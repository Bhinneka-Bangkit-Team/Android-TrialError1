package com.ocha.prototype_final.CustomView

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView
import java.lang.IllegalArgumentException

class AutoFitTextureView @JvmOverloads
    constructor(context: Context, attrs:AttributeSet? = null, styleDef:Int = 0)
    :TextureView(context,attrs,styleDef) {

    private var ratioWidth = 0
    private var ratioHeight = 0

    fun setRatio(width:Int, height:Int){
        if (width<0 || height<0){
            throw IllegalArgumentException("Size object are negative")
        }
        ratioHeight = height
        ratioWidth = width
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val height =MeasureSpec.getSize(heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        if (0==ratioWidth||0==ratioHeight){
            setMeasuredDimension(width,height)
        }else{
            if (width<height* ratioWidth/ratioHeight){
                setMeasuredDimension(width,width * ratioHeight/ratioWidth)
            }else{
                setMeasuredDimension(height * ratioWidth/ratioHeight,height)
            }
        }
    }
}