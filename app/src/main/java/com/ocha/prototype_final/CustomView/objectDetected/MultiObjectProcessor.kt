package com.ocha.prototype_final.CustomView.objectDetected

import android.graphics.PointF
import android.util.Log
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.core.util.set
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions
import com.ocha.prototype_final.CustomView.camera.CameraReticleAnimator
import com.ocha.prototype_final.CustomView.camera.FrameProcessorBase
import com.ocha.prototype_final.CustomView.camera.GraphicOverlay
import com.ocha.prototype_final.CustomView.camera.WorkFlow
import com.ocha.prototype_final.R
import com.ocha.prototype_final.settings.UtilsPreference
import java.io.IOException
import java.util.ArrayList
import kotlin.math.hypot

class MultiObjectProcessor(graphicOverlay: GraphicOverlay, private val workflowModel: WorkFlow):FrameProcessorBase<List<FirebaseVisionObject>>(){
    private val confirmationController: ObjectConfirmationController = ObjectConfirmationController(graphicOverlay)

    private val objectSelectionDistanceThreshold: Int = graphicOverlay
        .resources
        .getDimensionPixelOffset(R.dimen.object_selection_distance_threshold)
    private val detector: FirebaseVisionObjectDetector
    private val cameraReticleAnimator: CameraReticleAnimator = CameraReticleAnimator(graphicOverlay)
    private val objectDotAnimatorArray = SparseArray<ObjectAnimatorDot>()

    init {
        val optionsBuilder = FirebaseVisionObjectDetectorOptions.Builder()
            .setDetectorMode(FirebaseVisionObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
        if (UtilsPreference.isClassificationEnabled) {
            optionsBuilder.enableClassification()
        }
        this.detector = FirebaseVision.getInstance().getOnDeviceObjectDetector(optionsBuilder.build())
    }

    override fun detectInImage(image: FirebaseVisionImage): Task<MutableList<FirebaseVisionObject>> {
        return detector.processImage(image)
    }

    override fun onSuccess(
        image: FirebaseVisionImage,
        results: List<FirebaseVisionObject>,
        graphicOverlay: GraphicOverlay
    ) {
        var objects = results
        if (!workflowModel.cameraLive) {
            return
        }

        if (UtilsPreference.isClassificationEnabled) {
            val qualifiedObjects = ArrayList<FirebaseVisionObject>()
            for (result in objects) {
                if (result.classificationCategory != FirebaseVisionObject.CATEGORY_UNKNOWN) {
                    qualifiedObjects.add(result)
                }
            }
            objects = qualifiedObjects
        }

        removeAnimatorsFromUntrackedObjects(objects)
        graphicOverlay.clear()

        var selectedObject:DetectedObject? = null
        for (obj in objects.indices){
            val result = objects[obj]
            if (selectedObject  == null && shouldSelectObject(graphicOverlay, result)){
                selectedObject = DetectedObject(result, obj, image)

                confirmationController.confirming(result.trackingId)
                graphicOverlay.add(ObjectConfirmationGraphic(graphicOverlay, confirmationController))

                graphicOverlay.add(ObjectGraphicInMultiMode(graphicOverlay, selectedObject, confirmationController))
            }else{
                if (confirmationController.isConfirmed){
                    continue
                }
                val trackingId = result.trackingId ?: return
                val objectDotAnimator = objectDotAnimatorArray.get(trackingId) ?: let {
                    ObjectAnimatorDot(graphicOverlay).apply {
                        start()
                        objectDotAnimatorArray[trackingId] = this
                    }
                }
                graphicOverlay.add(
                    ObjectDotGraphic(
                        graphicOverlay, DetectedObject(result, obj, image), objectDotAnimator
                    )
                )
            }
        }


        if (selectedObject == null) {
            confirmationController.reset()
            graphicOverlay.add(ObjectRecticleGraph(graphicOverlay, cameraReticleAnimator))
            cameraReticleAnimator.start()
        } else {
            cameraReticleAnimator.cancel()
        }

        graphicOverlay.invalidate()

        if (selectedObject != null) {
            workflowModel.confirmingObject(selectedObject, confirmationController.progress)
        } else {
            workflowModel.setWorkState(
                if (objects.isEmpty()) {
                    WorkFlow.WorkflowState.DETECTING
                } else {
                    WorkFlow.WorkflowState.DETECTED
                }
            )
        }
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "onFailure: Failed to detected Object", )
    }

    override fun stop() {
        try {
            detector.close()
        }catch (e:IOException){
            Log.e(TAG, "stop: Failed to close object detector ",e )
        }
    }

    private fun removeAnimatorsFromUntrackedObjects(detectedObjects: List<FirebaseVisionObject>) {
        val trackingIds = detectedObjects.mapNotNull { it.trackingId }

        val removedTrackingIds = ArrayList<Int>()
        objectDotAnimatorArray.forEach { key, value ->
            if (!trackingIds.contains(key)) {
                value.cancel()
                removedTrackingIds.add(key)
            }
        }
        removedTrackingIds.forEach {
            objectDotAnimatorArray.remove(it)
        }
    }

    private fun shouldSelectObject(graphicOverlay: GraphicOverlay, visionObject: FirebaseVisionObject): Boolean {
        val box = graphicOverlay.translateRect(visionObject.boundingBox)
        val objectCenter = PointF((box.left + box.right) / 2f, (box.top + box.bottom) / 2f)
        val reticleCenter = PointF(graphicOverlay.width / 2f, graphicOverlay.height / 2f)
        val distance =
            hypot((objectCenter.x - reticleCenter.x).toDouble(), (objectCenter.y - reticleCenter.y).toDouble())
        return distance < objectSelectionDistanceThreshold
    }

    companion object{
        private const val TAG = "MultiObjectProcessor"
    }

}