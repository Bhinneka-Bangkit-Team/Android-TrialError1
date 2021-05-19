package com.ocha.prototype_final

import android.hardware.Camera
import android.media.ImageReader
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RadioGroup

class CameraActivity : AppCompatActivity(),ImageReader.OnImageAvailableListener,Camera.PreviewCallback,RadioGroup.OnCheckedChangeListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
    }

    override fun onImageAvailable(reader: ImageReader?) {
        TODO("Not yet implemented")
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        TODO("Not yet implemented")
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        TODO("Not yet implemented")
    }
}