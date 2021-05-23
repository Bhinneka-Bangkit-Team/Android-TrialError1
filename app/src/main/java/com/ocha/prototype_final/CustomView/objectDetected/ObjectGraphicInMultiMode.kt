package com.ocha.prototype_final.CustomView.objectDetected

import android.graphics.*
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.ocha.prototype_final.CustomView.camera.GraphicOverlay
import com.ocha.prototype_final.CustomView.camera.GraphicOverlay.Graphic
import com.ocha.prototype_final.R

internal class ObjectGraphicInMultiMode(
    overlay: GraphicOverlay,
    private val detectedObject: DetectedObject,
    private val confirmationController: ObjectConfirmationController
) : GraphicOverlay.Graphic(overlay) {

    private val boxPaint: Paint
    private val scrimPaint: Paint
    private val eraserPaint: Paint
    @ColorInt
    private val boxGradientStartColor: Int
    @ColorInt
    private val boxGradientEndColor: Int
    private val boxCornerRadius: Int
    private val minBoxLen: Int

    init {
        val resources = context.resources
        boxPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = resources.getDimensionPixelOffset(
                if (confirmationController.isConfirmed) {
                    R.dimen.bounding_box_confirmed_stroke_width
                } else {
                    R.dimen.bounding_box_stroke_width
                }
            ).toFloat()
            color = Color.WHITE
        }

        boxGradientStartColor = Color.WHITE
        boxGradientEndColor = Color.WHITE
        boxCornerRadius = resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius)

        scrimPaint = Paint().apply {
            shader = LinearGradient(
                0f,
                0f,
                overlay.width.toFloat(),
                overlay.height.toFloat(),
                Color.WHITE,
                Color.WHITE,
                Shader.TileMode.MIRROR
            )
        }

        eraserPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        minBoxLen = resources.getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_stroke_radius) * 2
    }

    override fun draw(canvas: Canvas) {
        var rect = overlay.translateRect(detectedObject.boundingBox)

        val boxWidth = rect.width() * confirmationController.progress
        val boxHeight = rect.height() * confirmationController.progress
        if (boxWidth < minBoxLen || boxHeight < minBoxLen) {
            return
        }

        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2
        rect = RectF(
            cx - boxWidth / 2f,
            cy - boxHeight / 2f,
            cx + boxWidth / 2f,
            cy + boxHeight / 2f
        )

        if (confirmationController.isConfirmed) {
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), scrimPaint)
            canvas.drawRoundRect(rect, boxCornerRadius.toFloat(), boxCornerRadius.toFloat(), eraserPaint)
        }

        boxPaint.shader = if (confirmationController.isConfirmed) {
            null
        } else {
            LinearGradient(
                rect.left,
                rect.top,
                rect.left,
                rect.bottom,
                boxGradientStartColor,
                boxGradientEndColor,
                Shader.TileMode.MIRROR
            )
        }
        canvas.drawRoundRect(rect, boxCornerRadius.toFloat(), boxCornerRadius.toFloat(), boxPaint)
    }
}