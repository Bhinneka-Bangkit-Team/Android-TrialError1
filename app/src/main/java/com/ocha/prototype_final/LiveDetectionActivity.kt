package com.ocha.prototype_final

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ocha.prototype_final.tflite.Classifier

class LiveDetectionActivity : AppCompatActivity() {

    private var objectThumbnailForBottomSheet: Bitmap? = null
    private var slidingSheetUpFromHiddenState: Boolean = false

    private val classifier by lazy{
        ClassifierHelper(this, ClassifierSpec(
            Classifier.Model.QUANTIZED_EFFICIENTNET,
            Classifier.Device.GPU,
            1
        )
        )
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_detection)
    }




}