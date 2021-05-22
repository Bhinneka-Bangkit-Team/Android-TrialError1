package com.ocha.prototype_final.CustomView.camera

import android.hardware.Camera
import com.google.android.gms.common.images.Size

class CameraPair {
    val preview: Size
    val picture: Size?

    constructor(previewSize: Camera.Size, pictureSize: Camera.Size?) {
        preview = Size(previewSize.width, previewSize.height)
        picture = pictureSize?.let { Size(it.width, it.height) }
    }

    constructor(previewSize: Size, pictureSize: Size?) {
        preview = previewSize
        picture = pictureSize
    }
}