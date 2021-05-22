package com.ocha.prototype_final

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.Camera
import android.util.Log
import com.ocha.prototype_final.CustomView.camera.CameraPair
import java.util.ArrayList
import kotlin.math.abs

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

    fun generateValidPreviewSizeList(camera: Camera): List<CameraPair> {
        val parameters = camera.parameters
        val supportedPreviewSizes = parameters.supportedPreviewSizes
        val supportedPictureSizes = parameters.supportedPictureSizes
        val validPreviewSizes = ArrayList<CameraPair>()
        for (previewSize in supportedPreviewSizes) {
            val previewAspectRatio = previewSize.width.toFloat() / previewSize.height.toFloat()


            for (pictureSize in supportedPictureSizes) {
                val pictureAspectRatio = pictureSize.width.toFloat() / pictureSize.height.toFloat()
                if (abs(previewAspectRatio - pictureAspectRatio) < ASPECT_RATIO_TOLERANCE) {
                    validPreviewSizes.add(CameraPair(previewSize, pictureSize))
                    break
                }
            }
        }
        if (validPreviewSizes.isEmpty()) {
            Log.w(TAG, "No preview sizes have a corresponding same-aspect-ratio picture size.")
            for (previewSize in supportedPreviewSizes) {

                validPreviewSizes.add(CameraPair(previewSize, null))
            }
        }

        return validPreviewSizes
    }
}