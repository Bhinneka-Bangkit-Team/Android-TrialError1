package com.ocha.prototype_final.settings

import android.content.Context
import android.preference.PreferenceManager
import androidx.annotation.StringRes
import com.google.android.gms.common.images.Size
import com.ocha.prototype_final.CustomView.camera.CameraPair
import com.ocha.prototype_final.R

object UtilsPreference {

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

}