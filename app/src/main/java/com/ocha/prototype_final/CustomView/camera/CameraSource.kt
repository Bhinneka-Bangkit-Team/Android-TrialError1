package com.ocha.prototype_final.CustomView.camera

import android.content.Context
import android.graphics.Camera
import android.graphics.ImageFormat
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import com.google.android.gms.common.images.Size
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.ocha.prototype_final.R
import com.ocha.prototype_final.Utils
import com.ocha.prototype_final.settings.UtilsPreference
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import kotlin.jvm.Throws
import kotlin.math.abs
import kotlin.math.ceil

@Suppress("DEPRECATION")
class CameraSource(private val graphicOverlay: GraphicOverlay) {
    private var camera:android.hardware.Camera? =null
    @FirebaseVisionImageMetadata.Rotation
    private var rotation:Int = 0

    internal var previewSize: Size? = null
        private set

    private var processingThread: Thread? = null
    private val processingRunnable = ProcessingFrameRunnable()

    private val processorLock = Object()
    private var frameProcessor: ProcessorFrame? = null

    private val bytesToBuffer = IdentityHashMap<ByteArray,ByteBuffer>()
    private val context: Context = graphicOverlay.context

    @Synchronized
    @Throws(IOException::class)
    internal fun start(surfaceHolder:SurfaceHolder){
        if (camera != null) return

        camera = createCamera().apply {
            setPreviewDisplay(surfaceHolder)
            startPreview()
        }

        processingThread = Thread(processingRunnable).apply {
            processingRunnable.setAct(true)
            start()
        }
    }

    @Synchronized
    internal fun stop() {
        processingRunnable.setAct(false)
        processingThread?.let {
            try {
                it.join()
            } catch (e: InterruptedException) {
                Log.e(TAG, "Frame processing thread interrupted on stop.")
            }
            processingThread = null
        }

        camera?.let {
            it.stopPreview()
            it.setPreviewCallbackWithBuffer(null)
            try {
                it.setPreviewDisplay(null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clearing the preview: $e")
            }
            it.release()
            camera = null
        }

        // Release the reference to any image buffers, since these will no longer be in use.
        bytesToBuffer.clear()
    }

    fun release() {
        graphicOverlay.clear()
        synchronized(processorLock) {
            stop()
            frameProcessor?.stop()
        }
    }

    fun setFrameProcessor(processor: ProcessorFrame) {
        graphicOverlay.clear()
        synchronized(processorLock) {
            frameProcessor?.stop()
            frameProcessor = processor
        }
    }

    fun updateFlashMode(flashMode: String) {
        val parameters = camera?.parameters
        parameters?.flashMode = flashMode
        camera?.parameters = parameters
    }

    @Throws(IOException::class)
    private fun createCamera(): android.hardware.Camera {
        val camera = android.hardware.Camera.open() ?: throw IOException("There is no back-facing camera.")
        val parameters = camera.parameters
        setPreviewAndPictureSize(camera, parameters)
        setRotation(camera, parameters)

        val previewFpsRange = selectPreviewFps(camera)
            ?: throw IOException("Could not find suitable preview frames per second range.")
        parameters.setPreviewFpsRange(
            previewFpsRange[android.hardware.Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
            previewFpsRange[android.hardware.Camera.Parameters.PREVIEW_FPS_MAX_INDEX]
        )

        parameters.previewFormat = IMAGE_FORMAT

        if (parameters.supportedFocusModes.contains(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.focusMode = android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        } else {
            Log.i(TAG, "Camera auto focus is not supported on this device.")
        }

        camera.parameters = parameters

        camera.setPreviewCallbackWithBuffer(processingRunnable::setNextFrame)
        previewSize?.let {
            camera.addCallbackBuffer(createPreviewBuffer(it))
            camera.addCallbackBuffer(createPreviewBuffer(it))
            camera.addCallbackBuffer(createPreviewBuffer(it))
            camera.addCallbackBuffer(createPreviewBuffer(it))
        }

        return camera
    }

    private fun createPreviewBuffer(previewSize: Size): ByteArray {
        val bitsPerPixel = ImageFormat.getBitsPerPixel(IMAGE_FORMAT)
        val sizeInBits = previewSize.height.toLong() * previewSize.width.toLong() * bitsPerPixel.toLong()
        val bufferSize = ceil(sizeInBits / 8.0).toInt() + 1

        val byteArray = ByteArray(bufferSize)
        val byteBuffer = ByteBuffer.wrap(byteArray)
        check(!(!byteBuffer.hasArray() || !byteBuffer.array()!!.contentEquals(byteArray))) {
            "Failed to create valid buffer for camera source."
        }

        bytesToBuffer[byteArray] = byteBuffer
        return byteArray
    }

    private fun setRotation(camera: android.hardware.Camera, parameters: android.hardware.Camera.Parameters) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val degrees = when (val deviceRotation = windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> {
                Log.e(TAG, "Bad device rotation value: $deviceRotation")
                0
            }
        }

        val cameraInfo = android.hardware.Camera.CameraInfo()
        android.hardware.Camera.getCameraInfo(CAMERA_BACK, cameraInfo)
        val angle = (cameraInfo.orientation - degrees + 360) % 360

        this.rotation = angle / 90
        camera.setDisplayOrientation(angle)
        parameters.setRotation(angle)
    }

    @Throws(IOException::class)
    private fun setPreviewAndPictureSize(camera: android.hardware.Camera, parameters: android.hardware.Camera.Parameters) {

        // Gives priority to the preview size specified by the user if exists.
        val sizePair: CameraPair = UtilsPreference.getUserSpecifiedPreviewSize(context)?: kotlin.run {
            val displayAspectRatio:Float =
                if (Utils.isPotraitMode(graphicOverlay.context)) {
                    graphicOverlay.height.toFloat() / graphicOverlay.width
                } else {
                graphicOverlay.width.toFloat() / graphicOverlay.height
                }
            selectPair(camera,displayAspectRatio)
        }?: throw IOException("Could not find suitable preview size.")

        previewSize = sizePair.preview.also {
            Log.v(TAG, "Camera preview size: $it")
            parameters.setPreviewSize(it.width, it.height)
            UtilsPreference.saveStringPreference(context, R.string.pref_key_rear_camera_preview_size, it.toString())
        }

        sizePair.picture?.let { pictureSize ->
            Log.v(TAG, "Camera picture size: $pictureSize")
            parameters.setPictureSize(pictureSize.width, pictureSize.height)
            UtilsPreference.saveStringPreference(
                context, R.string.pref_key_rear_camera_picture_size, pictureSize.toString())
        }
    }

    private fun selectPreviewFps(camera: android.hardware.Camera): IntArray? {
        val desiredPreviewFpsScaled = (REQ_CAMERA_FPS * 1000f).toInt()

        var selectedFpsRange: IntArray? = null
        var minDiff = Integer.MAX_VALUE
        for (range in camera.parameters.supportedPreviewFpsRange) {
            val deltaMin = desiredPreviewFpsScaled - range[android.hardware.Camera.Parameters.PREVIEW_FPS_MIN_INDEX]
            val deltaMax = desiredPreviewFpsScaled - range[android.hardware.Camera.Parameters.PREVIEW_FPS_MAX_INDEX]
            val diff = abs(deltaMin) + abs(deltaMax)
            if (diff < minDiff) {
                selectedFpsRange = range
                minDiff = diff
            }
        }
        return selectedFpsRange
    }

    private inner class ProcessingFrameRunnable internal constructor():Runnable{
        private var lock = Object()
        private var active = true

        private var pendingFrame:ByteBuffer? =null
        internal fun setAct(active:Boolean){
            synchronized(lock){
                this.active = active
                lock.notifyAll()
            }
        }
        internal fun setNextFrame(data: ByteArray, camera: android.hardware.Camera) {
            synchronized(lock) {
                pendingFrame?.let {
                    camera.addCallbackBuffer(it.array())
                    pendingFrame = null
                }

                if (!bytesToBuffer.containsKey(data)) {
                    Log.d(
                        TAG,
                        "Skipping frame. Could not find ByteBuffer associated with the image data from the camera."
                    )
                    return
                }
                pendingFrame = bytesToBuffer[data]
                lock.notifyAll()
            }
        }

        override fun run() {
            var data: ByteBuffer?
            while (true){
                    synchronized(lock){
                        while (active && pendingFrame == null) {
                            try {

                                lock.wait()
                            } catch (e: InterruptedException) {
                                Log.e(TAG, "Frame processing loop terminated.", e)
                                return
                            }
                        }
                        if(!active) return
                        data = pendingFrame
                        pendingFrame = null
                    }
                    try {
                        synchronized(processorLock){
                            val frameMetadata =FrameMetadata(previewSize!!.width, previewSize!!.height,rotation)
                            data?.let {
                                frameProcessor?.process(it,frameMetadata,graphicOverlay)
                            }
                        }
                    }catch (e:Exception){
                        Log.e(TAG, "Exception thrown from receiver.", e)
                    }finally {
                        data?.let {
                            camera?.addCallbackBuffer(it.array())
                        }
                    }
            }
        }
    }
    companion object{
        private const val TAG="CameraSource"
        private const val REQ_CAMERA_FPS = 30.0f
        const val CAMERA_BACK = android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK
        private const val IMAGE_FORMAT = ImageFormat.NV21
        private const val MIN_CAMERA_PREVIEW_WIDTH = 400
        private const val MAX_CAMERA_PREVIEW_WIDTH = 1300
        private const val DEFAULT_REQUESTED_CAMERA_PREVIEW_WIDTH = 640
        private const val DEFAULT_REQUESTED_CAMERA_PREVIEW_HEIGHT = 360


        private fun selectPair(camera: android.hardware.Camera, displayAspectRatioInLandscape: Float):CameraPair?{
            val validPreviewSizes = Utils.generateValidPreviewSizeList(camera)

            var selectedPair: CameraPair? = null
            // Picks the preview size that has closest aspect ratio to display view.
            var minAspectRatioDiff = Float.MAX_VALUE

            for (sizePair in validPreviewSizes) {
                val previewSize = sizePair.preview
                if (previewSize.width < MIN_CAMERA_PREVIEW_WIDTH || previewSize.width > MAX_CAMERA_PREVIEW_WIDTH) {
                    continue
                }

                val previewAspectRatio = previewSize.width.toFloat() / previewSize.height.toFloat()
                val aspectRatioDiff = abs(displayAspectRatioInLandscape - previewAspectRatio)
                if (abs(aspectRatioDiff - minAspectRatioDiff) < Utils.ASPECT_RATIO_TOLERANCE) {
                    if (selectedPair == null || selectedPair.preview.width < sizePair.preview.width) {
                        selectedPair = sizePair
                    }
                } else if (aspectRatioDiff < minAspectRatioDiff) {
                    minAspectRatioDiff = aspectRatioDiff
                    selectedPair = sizePair
                }
            }

            if (selectedPair == null) {
                var minDiff = Integer.MAX_VALUE
                for (sizePair in validPreviewSizes) {
                    val size = sizePair.preview
                    val diff =
                        abs(size.width - DEFAULT_REQUESTED_CAMERA_PREVIEW_WIDTH) +
                                abs(size.height - DEFAULT_REQUESTED_CAMERA_PREVIEW_HEIGHT)
                    if (diff < minDiff) {
                        selectedPair = sizePair
                        minDiff = diff
                    }
                }
            }
            return selectedPair
        }
    }
}