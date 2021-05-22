package com.ocha.prototype_final.CustomView.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.util.ArrayList

class GraphicOverlay(context: Context, attrs: AttributeSet): View(context,attrs) {
    private val lock = Any()
    private val previewWidth:Int = 0
    private var widthScaleFactor = 1.0f
    private var previewHeight: Int = 0
    private var heightScaleFactor = 1.0f
    private val graphics = ArrayList<Graphic>()

    abstract class Graphic protected constructor(protected val overlay: GraphicOverlay){
        protected val context:Context = overlay.context
        abstract fun draw(canvas: Canvas
        )
    }

    fun clear(){
        synchronized(lock){
            graphics.clear()
        }
    }

    fun add(graphic: Graphic){
        synchronized(lock){
            graphics.add(graphic)
        }
    }

    fun setCameraInfo(cameraSource:CameraSource){

    }
}