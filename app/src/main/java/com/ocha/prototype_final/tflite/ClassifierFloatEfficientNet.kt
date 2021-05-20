package com.ocha.prototype_final.tflite

import android.app.Activity
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.ops.NormalizeOp

class ClassifierFloatEfficientNet(activity: Activity?, device: Device?, numThread: Int):Classifier(activity, device, numThread) {
    override val preprocessNormalizeOp: TensorOperator?
        get() = NormalizeOp(IMAGE_MEAN, IMAGE_STD)
    override val modelPath: String?
        get() = "efficientnet-lite0-fp32.tflite"
    override val labelPath: String?
        get() = "labels_without_background.txt"
    override val postprocessNormalizeOp: TensorOperator?
        get() = NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)

    companion object{
        private const val IMAGE_MEAN =127.0f
        private const val IMAGE_STD = 128.0f

        private const val PROBABILITY_MEAN =0.0f
        private const val PROBABILITY_STD =1.0f
    }
}