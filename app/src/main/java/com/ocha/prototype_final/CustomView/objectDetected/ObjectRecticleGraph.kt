package com.ocha.prototype_final.CustomView.objectDetected

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.ContextCompat
import com.ocha.prototype_final.CustomView.camera.CameraReticleAnimator
import com.ocha.prototype_final.CustomView.camera.GraphicOverlay
import com.ocha.prototype_final.R

class ObjectRecticleGraph(overlay: GraphicOverlay, private val animator: CameraReticleAnimator):GraphicOverlay.Graphic(overlay) {
    private var outerRingFillRadius: Int = 0
    private var outerRingStrokeRadius: Int = 0
    private var innerRingStrokeRadius: Int = 0
    private var rippleSizeOffset: Int = 0
    private var rippleStrokeWidth: Int = 0
    private var rippleAlpha: Int =  0
    private val outerRingFillPaint: Paint
    private val outerRingStrokePaint: Paint
    private val innerRingStrokePaint: Paint
    private val ripplePaint: Paint

    init {

        val resources = overlay.resources
        outerRingFillPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }

        outerRingStrokePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = resources.getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_stroke_width).toFloat()
            strokeCap = Paint.Cap.ROUND
            color = Color.WHITE
        }

        innerRingStrokePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = resources.getDimensionPixelOffset(R.dimen.object_reticle_inner_ring_stroke_width).toFloat()
            strokeCap = Paint.Cap.ROUND
            color = ContextCompat.getColor(context, R.color.white)
        }

        ripplePaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
        }

        outerRingFillRadius = resources.getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_fill_radius)
        outerRingStrokeRadius = resources.getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_stroke_radius)
        innerRingStrokeRadius = resources.getDimensionPixelOffset(R.dimen.object_reticle_inner_ring_stroke_radius)
        rippleSizeOffset = resources.getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_stroke_radius)
        rippleStrokeWidth = resources.getDimensionPixelOffset(R.dimen.object_reticle_ripple_stroke_width)
        rippleAlpha = ripplePaint.alpha
    }
    override fun draw(canvas: Canvas) {
        var x =canvas.width/2f
        var y = canvas.height/2f
        canvas.drawCircle(x, y, outerRingFillRadius.toFloat(), outerRingFillPaint)
        canvas.drawCircle(x, y, outerRingStrokeRadius.toFloat(), outerRingStrokePaint)
        canvas.drawCircle(x, y, innerRingStrokeRadius.toFloat(), innerRingStrokePaint)
        ripplePaint.alpha = (rippleAlpha * animator.rippleAlphaScale).toInt()
        ripplePaint.strokeWidth = rippleStrokeWidth * animator.rippleStrokeWidthScale
        val radius = outerRingStrokeRadius + rippleSizeOffset * animator.rippleSizeScale
        canvas.drawCircle(x, y, radius, ripplePaint)
    }

}