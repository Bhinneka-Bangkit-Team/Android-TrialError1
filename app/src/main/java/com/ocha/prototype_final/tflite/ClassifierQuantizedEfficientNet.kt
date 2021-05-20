package com.ocha.prototype_final.tflite

import android.app.Activity
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.ops.NormalizeOp

class ClassifierQuantizedEfficientNet(activity: Activity?,device: Device?,numThread: Int):Classifier(activity, device, numThread) {
    override val preprocessNormalizeOp: TensorOperator?
        get() = NormalizeOp(IMAGE_MEAN, IMAGE_STD)

    override val modelPath: String?
        get() = "efficientnet-lite0-int8.tflite"

    override val labelPath: String?
        get() = "labels_without_background.txt"

    override val postprocessNormalizeOp: TensorOperator?
        get() = NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)

    companion object{
        private const val IMAGE_MEAN =0.0f
        private const val IMAGE_STD = 1.0f

        private const val PROBABILITY_MEAN =0.0f
        private const val PROBABILITY_STD =255.0f
    }
}