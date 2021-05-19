package com.ocha.prototype_final.CustomView

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import java.util.*

class OverlayView(context: Context,attributeSet: AttributeSet):View(context,attributeSet) {
    private val callbacks =LinkedList<DrawCallback>()
    fun addCalback(callback: DrawCallback){
        callbacks.add(callback)
    }

    @SuppressLint("MissingSuperCall")
    @Synchronized
    override fun draw(canvas: Canvas) {
        for (callback in callbacks){
            callback.drawCallback(canvas)
        }
    }

    interface DrawCallback {
        fun drawCallback(canvas: Canvas)
    }
}