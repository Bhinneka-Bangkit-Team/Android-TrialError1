package com.ocha.prototype_final.CustomView.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.ocha.prototype_final.Utils
import java.util.ArrayList

class GraphicOverlay(context: Context, attrs: AttributeSet): View(context,attrs) {
    private val lock = Any()
    private var previewWidth:Int = 0
    private var widthScaleFactor = 1.0f
    private var previewHeight: Int = 0
    private var heightScaleFactor = 1.0f
    private val graphics = ArrayList<Graphic>()

    abstract class Graphic protected constructor(protected val overlay: GraphicOverlay){
        protected val context:Context = overlay.context
        abstract fun draw(canvas: Canvas
        )
    }

    fun translateRect(rect: Rect) = RectF(
        translateX(rect.left.toFloat()),
        translateY(rect.top.toFloat()),
        translateX(rect.right.toFloat()),
        translateY(rect.bottom.toFloat())
    )

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

    fun translateX(x: Float): Float = x * widthScaleFactor
    fun translateY(y: Float): Float = y * heightScaleFactor

    fun setCameraInfo(cameraSource:CameraSource){
        val previewSize = cameraSource.previewSize ?: return
        if (Utils.isPotraitMode(context)) {
            // Swap width and height when in portrait, since camera's natural orientation is landscape.
            previewWidth = previewSize.height
            previewHeight = previewSize.width
        } else {
            previewWidth = previewSize.width
            previewHeight = previewSize.height
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (previewWidth > 0 && previewHeight > 0) {
            widthScaleFactor = width.toFloat() / previewWidth
            heightScaleFactor = height.toFloat() / previewHeight
        }
        synchronized(lock) {
            graphics.forEach {
                if (canvas != null) {
                    it?.draw(canvas)
                }
            }
        }
    }
}