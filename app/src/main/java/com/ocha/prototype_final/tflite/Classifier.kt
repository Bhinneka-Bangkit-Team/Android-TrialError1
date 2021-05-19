package com.ocha.prototype_final.tflite

import android.graphics.Bitmap
import android.graphics.RectF

interface Classifier {
    val statString:String
    fun recognizeImage(bitmap: Bitmap)
    fun enableStatLogging(debug:Boolean)
    fun close()
    fun setNumThreads(numThread: Int)
    fun seUseNNAPI(isChecked:Boolean)


    class Recognition(
        val id:String?,
        val title:String?,
        val confidence:Float?,
        internal var location:RectF){

            fun getLocation():RectF{
                return RectF(location)
            }

            fun setLocation(location: RectF) {
                this.location = location
            }

        override fun toString(): String {
            var resultString = ""
            if (id != null) {
                resultString += "[$id] "
            }

            if (title != null) {
                resultString += "$title "
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f)
            }

            if (location != null) {
                resultString += location.toString() + " "
            }

            return resultString.trim { it <= ' ' }
        }
    }


}