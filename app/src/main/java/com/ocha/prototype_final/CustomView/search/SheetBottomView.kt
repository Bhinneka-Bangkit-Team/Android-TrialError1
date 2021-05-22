package com.ocha.prototype_final.CustomView.search

import android.content.Context
import android.graphics.*
import android.media.ThumbnailUtils
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.common.base.Preconditions.checkArgument
import com.ocha.prototype_final.R

class SheetBottomView(context: Context, attrs:AttributeSet):View(context,attrs) {
    private val scrimPaint: Paint
    private val thumbnailPaint: Paint
    private val boxPaint: Paint
    private val thumbnailHeight: Int
    private val thumbnailMargin: Int
    private val boxCornerRadius: Int
    private var thumbnailBitmap:Bitmap? = null
    private var thumbnailRectF:RectF? = null
    private var downPercentInCollapsed:Float= 0f

    init {
        val resource = context.resources
        scrimPaint = Paint().apply {
            color = ContextCompat.getColor(context,R.color.dark)
        }

        boxPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = resource.getDimensionPixelOffset(R.dimen.object_thumbnail_stroke_width).toFloat()
            color = Color.WHITE

        }

        thumbnailPaint = Paint()

        boxCornerRadius = resource.getDimensionPixelOffset(R.dimen.bounding_box_radius)
        thumbnailHeight  = resource.getDimensionPixelOffset(R.dimen.thumbnail_height)
        thumbnailMargin = resource.getDimensionPixelOffset(R.dimen.thumbnail_margin)
    }

    companion object{
        private const val DOWN_PERCENT_TO_HIDE = 0.42f
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val bitmap = thumbnailBitmap?:return
        val react =thumbnailRectF ?:return
        canvas?.drawRect(0f,0f,width.toFloat(),height.toFloat(),scrimPaint)
        if (downPercentInCollapsed < DOWN_PERCENT_TO_HIDE){
            val alpha = ((1-downPercentInCollapsed/ DOWN_PERCENT_TO_HIDE)*255).toInt()
            thumbnailPaint.alpha = alpha
            canvas?.drawBitmap(bitmap,null,react,thumbnailPaint)
            boxPaint.alpha = alpha
            canvas?.drawRoundRect(react,boxCornerRadius.toFloat(),boxCornerRadius.toFloat(),boxPaint)
        }
    }

    fun updateWithThumbnailTranslate(
        thumbnailBitmap: Bitmap,
        collapsedStateHeight: Int,
        slideOffset: Float,
        bottomSheet:View
    ){
        this.thumbnailBitmap = thumbnailBitmap
        val currentSheetH:Float
        if(slideOffset < 0){
            downPercentInCollapsed = -slideOffset
            currentSheetH = collapsedStateHeight *(1+slideOffset)
        }else{
            downPercentInCollapsed = 0f
            currentSheetH = collapsedStateHeight +(bottomSheet.height - collapsedStateHeight) *slideOffset
        }

        thumbnailRectF = RectF().apply {
            val thumbnailWith =thumbnailBitmap.width.toFloat()/thumbnailBitmap.height.toFloat() * thumbnailHeight.toFloat()
            left =thumbnailMargin.toFloat()
            top =height.toFloat() - currentSheetH -thumbnailMargin.toFloat() - thumbnailHeight.toFloat()
            right =left - thumbnailWith
            bottom = top - thumbnailWith
        }

    }

    fun updateWithThumbnailTranslateAndScale( thumbnailBitmap: Bitmap, collapsedHeight:Int,slideOffset:Float, srcThumbnailRectF: RectF
    ){
        checkArgument(
            slideOffset <= 0,
            "Scale mode only works when the sheet is collapsed state"
        )

        this.thumbnailBitmap = thumbnailBitmap
        this.downPercentInCollapsed = 0f

        thumbnailRectF = RectF().apply {
            val dstX =thumbnailMargin.toFloat()
            val dstY =(height - collapsedHeight - thumbnailMargin - thumbnailHeight).toFloat()
            val dstHeight = thumbnailHeight.toFloat()
            val dstWidth = srcThumbnailRectF.width()/srcThumbnailRectF.height() * dstHeight
            val dstRect = RectF(dstX, dstY, dstX + dstWidth, dstY + dstHeight)
            val progressCollapsedState =slideOffset+1
            bottom =srcThumbnailRectF.bottom +  (dstRect.bottom - srcThumbnailRectF.bottom) * progressCollapsedState
            right = srcThumbnailRectF.right + (dstRect.right - srcThumbnailRectF.right) * progressCollapsedState
            left = srcThumbnailRectF.left + (dstRect.left - srcThumbnailRectF.left) * progressCollapsedState
            top = srcThumbnailRectF.top + (dstRect.top - srcThumbnailRectF.top) * progressCollapsedState


        }

        invalidate()
    }
}