package com.ocha.prototype_final.CustomView.objectDetected

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import com.ocha.prototype_final.CustomView.camera.GraphicOverlay
import com.ocha.prototype_final.R
import com.ocha.prototype_final.settings.UtilsPreference

class ObjectConfirmationGraphic internal constructor(overlay: GraphicOverlay,private val confirmationController: ObjectConfirmationController):GraphicOverlay.Graphic(overlay){
    private var outerRingFillRadius: Int = 0
    private var outerRingStrokeRadius: Int = 0
    private var innerRingStrokeRadius: Int = 0
    private val outerRingFillPaint: Paint
    private val outerRingStrokePaint: Paint
    private val innerRingPaint: Paint
    private val progressRingStrokePaint: Paint

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

        progressRingStrokePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = resources.getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_stroke_width).toFloat()
            strokeCap = Paint.Cap.ROUND
            color = ContextCompat.getColor(context, R.color.white)
        }

        innerRingPaint = Paint()
        if (UtilsPreference.isMultipleObjectsMode) {
            innerRingPaint.style = Paint.Style.FILL
            innerRingPaint.color = Color.WHITE
        } else {
            innerRingPaint.style = Paint.Style.STROKE
            innerRingPaint.strokeWidth =
                resources.getDimensionPixelOffset(R.dimen.object_reticle_inner_ring_stroke_width).toFloat()
            innerRingPaint.strokeCap = Paint.Cap.ROUND
            innerRingPaint.color = ContextCompat.getColor(context, R.color.white)
        }

        outerRingFillRadius = resources.getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_fill_radius)
        outerRingStrokeRadius = resources.getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_stroke_radius)
        innerRingStrokeRadius = resources.getDimensionPixelOffset(R.dimen.object_reticle_inner_ring_stroke_radius)
    }

    override fun draw(canvas: Canvas) {
        var x =canvas.width/2f
        var y = canvas.height/2f
        canvas.drawCircle(x, y, outerRingFillRadius.toFloat(), outerRingFillPaint)
        canvas.drawCircle(x, y, outerRingStrokeRadius.toFloat(), outerRingStrokePaint)
        canvas.drawCircle(x, y, innerRingStrokeRadius.toFloat(), innerRingPaint)

        val progressRect = RectF(
            x - outerRingStrokeRadius,
            y - outerRingStrokeRadius,
            x + outerRingStrokeRadius,
            y + outerRingStrokeRadius
        )
        val sweepAngle = confirmationController.progress * 360
        canvas.drawArc(
            progressRect,
           0f,
            sweepAngle,
           false,
            progressRingStrokePaint
        )
    }
}