package com.ocha.prototype_final.CustomView.objectDetected

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import com.ocha.prototype_final.R

class SearchedObject( private val detectedObject: DetectedObject,resources: Resources) {
    private var objectThumb: Bitmap? = null
    private val objectThumbRadius:Int  =resources.getDimensionPixelOffset(R.dimen.bounding_box_radius)
    val objectIndex:Int
        get() =detectedObject.indexObject

    val boundingBox: Rect
        get() = detectedObject.boundingBox


}