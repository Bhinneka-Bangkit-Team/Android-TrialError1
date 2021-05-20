package com.ocha.prototype_final

import android.util.Log

object Logger {
    private const val TAG = "protype_final"

    @JvmStatic
    fun v(log:String){
        Log.v(TAG,log)
    }

    @JvmStatic
    fun e(log:String){
        Log.e(TAG,log)
    }

    @JvmStatic
    fun d(log:String){
        Log.d(TAG,log)
    }
}