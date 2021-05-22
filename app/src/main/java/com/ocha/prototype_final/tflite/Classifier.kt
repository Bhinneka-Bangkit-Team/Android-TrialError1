package com.ocha.prototype_final.tflite

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.os.Trace
import com.ocha.prototype_final.Logger.v
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.MappedByteBuffer
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

abstract class Classifier protected constructor(activity: Activity?, device:Device?, numThread: Int) {

    private val TAG = "Classifier"

    //The runtime device type used for executing classification.
    enum class Device{
        CPU, NNAPI, GPU
    }

    //model type use for executing classification
    enum class Model{
        FLOAT_MOBILENET, QUANTIZED_MOBILENET, FLOAT_EFFICIENTNET, QUANTIZED_EFFICIENTNET
    }

    //tflite model
    private var tfLiteModel:MappedByteBuffer?

    //Image Size
    val imageSizeX:Int
    val imageSizeY:Int

    //optional if GPU or NNAPI delegate for acc
    private var gpuDelegate: GpuDelegate? = null
    private var apiDelegate:NnApiDelegate? = null

    private var tflite:Interpreter?

    private val tfliteOptions = Interpreter.Options()

    private val labels: List<String>

    private var inputImageBuffer: TensorImage

    private val outputProbabilityBuffer: TensorBuffer


    //process probability
    private val probabilityProcessor: TensorProcessor


    class Recognition(
        val id:String?,
        val title:String?,
        val confidence:Float?,
        private var location:RectF?
    ){
        fun setLocation(location: RectF?){
            this.location = location
        }

        override fun toString(): String {
            var resultString = ""
            if (id != null){
                resultString +="[$id]"
            }
            if (title != null){
                resultString +="[$title]"
            }
            if (confidence != null){
                resultString += String.format("(%.1f%%) ", confidence * 100.0f)
            }
            if (location != null){
                resultString += location.toString()+" "
            }

            return resultString.trim { it <= ' ' }
        }

        fun formatedString()="${title} - ${String.format("(%.1f%%) ", confidence!! * 100.0f)}"


    }

    fun recognizeImage(bitmap: Bitmap, sensorOrientation: Int):List<Recognition>{

        Trace.beginSection("recognizeImage")
        Trace.beginSection("loadImage")

        val startTimeForLoad = SystemClock.uptimeMillis()
        inputImageBuffer = loadImage(bitmap,sensorOrientation)
        val endTimeFoarLoad = SystemClock.uptimeMillis()
        Trace.endSection()
        v("Timecost to load the image: " + (endTimeFoarLoad - startTimeForLoad))

        Trace.beginSection("runInference")
        val startTimeForReference = SystemClock.uptimeMillis()
        tflite!!.run(inputImageBuffer.buffer,outputProbabilityBuffer.buffer.rewind())
        val endTimeForReference = SystemClock.uptimeMillis()
        Trace.endSection()

        v("Timecost to reference the image: " + (endTimeFoarLoad - startTimeForLoad))

        val labelProbability =TensorLabel(labels,probabilityProcessor.process(outputProbabilityBuffer)).mapWithFloatValue
        Trace.endSection()

        return getTopProbability(labelProbability)
    }

    private fun loadImage(bitmap: Bitmap, sensorOrientation: Int): TensorImage {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap)

        val cropSize = Math.min(bitmap.width, bitmap.height)
        val numRotation = sensorOrientation / 90

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(Rot90Op(numRotation))
            .add(preprocessNormalizeOp)
            .build()
        return imageProcessor.process(inputImageBuffer)
    }

    fun close(){
        if (tflite != null){
            tflite!!.close()
            tflite = null
        }
        if (gpuDelegate != null){
            gpuDelegate!!.close()
            gpuDelegate = null
        }
        if (apiDelegate != null){
            apiDelegate!!.close()
            apiDelegate = null
        }
        tfLiteModel = null
    }

    protected abstract val preprocessNormalizeOp:TensorOperator?
    protected abstract val modelPath: String?
    protected abstract val labelPath: String?


    protected abstract val postprocessNormalizeOp: TensorOperator?

    companion object{
        private const val MAX_RESULT = 3


        @Throws(IOException::class)
        fun create(activity: Activity?,model:Model,device: Device?,numThread: Int):Classifier{
            return if(model == Model.QUANTIZED_EFFICIENTNET){
                ClassifierQuantizedEfficientNet(activity,device,numThread)
            }else{
                throw UnsupportedOperationException()
            }
        }

        private fun getTopProbability(labelProb:Map<String,Float>):List<Recognition>{
            val pq =PriorityQueue(
                MAX_RESULT,
                Comparator<Recognition>{
                    lhs,rhs->
                    java.lang.Float.compare(rhs.confidence!!,lhs.confidence!!)
                }
            )
            for ((key,value) in labelProb){
                pq.add(Recognition(""+key,key,value,null))
            }
            val recognitions  = ArrayList<Recognition>()
            val recognitionsSize = Math.min(pq.size, MAX_RESULT)
            for (i in 0 until recognitionsSize){
                recognitions.add(pq.poll())
            }
            return recognitions
        }
    }

    init {
        tfLiteModel  = FileUtil.loadMappedFile(activity!!, modelPath!!)
        when(device){
            Device.NNAPI ->{
                apiDelegate = NnApiDelegate()
                tfliteOptions.addDelegate(apiDelegate)
            }
            Device.GPU->{
                gpuDelegate  = GpuDelegate()
                tfliteOptions.addDelegate(gpuDelegate)
            }
            Device.CPU->{

            }
        }

        tfliteOptions.setNumThreads(numThread)
        tflite = Interpreter(tfLiteModel!!,tfliteOptions)
        labels =FileUtil.loadLabels(activity,labelPath!!)
        
        val imageTensorIndex = 0
        val imageShape = tflite!!.getInputTensor(imageTensorIndex).shape()
        imageSizeX = imageShape[1]
        imageSizeY = imageShape[2]
        val imageDataType = tflite!!.getInputTensor(imageTensorIndex).dataType()

        val probablyTensorIndex = 0
        val probablyShape =tflite!!.getOutputTensor(probablyTensorIndex).shape()
        val probablyDataType =tflite!!.getOutputTensor(probablyTensorIndex).dataType()

        inputImageBuffer = TensorImage(imageDataType)

        outputProbabilityBuffer = TensorBuffer.createFixedSize(probablyShape,probablyDataType)

        probabilityProcessor  =TensorProcessor.Builder().add(postprocessNormalizeOp).build()

    }


}