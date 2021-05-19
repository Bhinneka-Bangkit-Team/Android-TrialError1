package com.ocha.prototype_final.CustomView

import com.ocha.prototype_final.tflite.Classifier

interface ResultView{
    fun setResult(result: List<Classifier.Recognition>)
}