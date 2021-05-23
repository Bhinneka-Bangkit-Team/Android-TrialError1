package com.ocha.prototype_final.settings

import android.content.Context
import android.preference.PreferenceManager
import androidx.annotation.StringRes
import com.google.android.gms.common.images.Size
import com.ocha.prototype_final.CustomView.camera.CameraPair
import com.ocha.prototype_final.R

object UtilsPreference {

    val  isAutoSearchEnable = true
    val isMultipleObjectsMode = false
    val isClassificationEnabled = true



    fun getUserSpecifiedPreviewSize(context: Context): CameraPair? {
        return try {
            val previewSizePrefKey = "rcpvs"
            val pictureSizePrefKey = "rcpts"
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            CameraPair(
                Size.parseSize(sharedPreferences.getString(previewSizePrefKey, null)),
                Size.parseSize(sharedPreferences.getString(pictureSizePrefKey, null)))
        } catch (e: Exception) {
            null
        }
    }

    fun saveStringPreference(context: Context, @StringRes prefKeyId: Int, value: String?){
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(context.getString(prefKeyId),value).apply()
    }

    fun getConfirmationTimeMs(): Int = when {
        isMultipleObjectsMode -> 300
        isAutoSearchEnable -> 1500
        else -> 500
    }



}