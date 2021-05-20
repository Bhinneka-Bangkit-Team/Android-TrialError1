package com.ocha.prototype_final

import android.app.Activity
import android.content.Intent

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


}