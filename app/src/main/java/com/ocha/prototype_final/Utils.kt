package com.ocha.prototype_final

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration

object Utils {

    const val ASPECT_RATIO_TOLERANCE = 0.01f

    internal const val REQUEST_CODE_PHOTO_LIBRARY = 1

    private const val TAG = "Utils"


    internal fun openImage(activity:Activity){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        activity.startActivityForResult(intent, REQUEST_CODE_PHOTO_LIBRARY)
    }

    fun isPotraitMode(context: Context):Boolean = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

}