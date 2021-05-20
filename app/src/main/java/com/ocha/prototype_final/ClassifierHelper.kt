package com.ocha.prototype_final

import android.app.Activity
import android.graphics.Bitmap
import com.ocha.prototype_final.tflite.Classifier
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis


data class ClassifierSpec(
    val model: Classifier.Model,
    val device: Classifier.Device,
    val numThreads: Int
)

class ClassifierHelper(
    private var activity:Activity,
    private var spec:ClassifierSpec
) {
    private var classifier: Classifier? = null
    private val executors = Executors.newSingleThreadExecutor()

    fun execute(
        bitmap: Bitmap,
        onError: (Exception) -> Unit,
        onResult: (List<Classifier.Recognition>) -> Unit){
        val mainOnError = { e: Exception -> activity.runOnUiThread { onError(e) } }
        val mainOnResult = { r: List<Classifier.Recognition> -> activity.runOnUiThread { onResult(r) } }

        executors.execute {
            createClassifier(mainOnError)
            processImage(bitmap, mainOnResult)
        }
    }

    private fun createClassifier(onError: (Exception) -> Unit){
        if(classifier!=null)return

        val (model,device,numThread) = spec

        if (device === Classifier.Device.GPU  && (model === Classifier.Model.QUANTIZED_MOBILENET || model === Classifier.Model.QUANTIZED_EFFICIENTNET)){
            Logger.d("Not creating classifier: GPU doesn't support quantized models.")
            onError(IllegalStateException("Error regarding GPU support for Quant models[CHAR_LIMIT=60]"))
            return
        }

        try {
            Logger.d("Creating classifier (model=$model, device=$device, numThreads=$numThread)")
            classifier = Classifier.create(activity, model, device, numThread)
        }catch (e:IOException){
            Logger.d("Creating classifier (model=$model, device=$device, numThreads=$numThread)")
            classifier = Classifier.create(activity, model, device, numThread)
        }
    }

    private fun processImage(bitmap: Bitmap,onResult: (List<Classifier.Recognition>) -> Unit){
        val currentClassifier = classifier?:throw java.lang.IllegalStateException("Classifier not ready!")
        measureTimeMillis {
            val results = currentClassifier.recognizeImage(bitmap, 0)
            onResult(results)
            Logger.d("Result ready: $results")
        }.also {
            Logger.v("Detect: $it ms")
        }
    }
}