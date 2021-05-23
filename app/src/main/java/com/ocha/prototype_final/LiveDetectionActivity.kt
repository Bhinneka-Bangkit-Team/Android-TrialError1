package com.ocha.prototype_final

import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.ocha.prototype_final.CustomView.camera.CameraSource
import com.ocha.prototype_final.CustomView.camera.CameraSourcePreview
import com.ocha.prototype_final.CustomView.camera.GraphicOverlay
import com.ocha.prototype_final.CustomView.camera.WorkFlow
import com.ocha.prototype_final.CustomView.search.SheetBottomView
import com.ocha.prototype_final.databinding.ActivityLiveDetectionBinding
import com.ocha.prototype_final.databinding.BottomSheetBinding
import com.ocha.prototype_final.databinding.CameraOverlayBinding
import com.ocha.prototype_final.databinding.TopWidgetLiveDetectionBinding
import com.ocha.prototype_final.settings.UtilsPreference
import com.ocha.prototype_final.tflite.Classifier
import java.io.IOException
import java.util.*

class LiveDetectionActivity : AppCompatActivity(), View.OnClickListener {

    private var cameraSource: CameraSource? = null
    private var workflowModel: WorkFlow? = null
    private var currentWorkflowState: WorkFlow.WorkflowState? = null

    private var objectThumbnailForBottomSheet: Bitmap? = null
    private var slidingSheetUpFromHiddenState: Boolean = false
    private lateinit var viewBinding:ActivityLiveDetectionBinding

    private val classifier by lazy{
        ClassifierHelper(this, ClassifierSpec(
            Classifier.Model.QUANTIZED_EFFICIENTNET,
            Classifier.Device.GPU,
            1
        ))
    }


    private val preview by lazy { findViewById<CameraSourcePreview>(R.id.preview_camera) }
    private val graphicOverlay:GraphicOverlay  = findViewById(R.id.camera_preview_grapic_overlay)
    private val searchButton:ExtendedFloatingActionButton = findViewById(R.id.product_search_button)
    private val progress by lazy { findViewById<ProgressBar>(R.id.search_progress) }
    private var bottomSheetBehavior:BottomSheetBehavior<View>? = null
    private var bottomSheetScrimView:SheetBottomView? = null

    private var searchButtonAnimatior:AnimatorSet? = null
    private var prompChipAnimator:AnimatorSet? =null
    private val prompChip by lazy { findViewById<Chip>(R.id.bottom_prompt_chip)  }

    private val bottomSheetCaptionView by lazy { findViewById<TextView>(R.id.bottom_sheet_caption)  }
    private val bottomSheetBestView by lazy { findViewById<TextView>(R.id.bottom_sheet_best_match)}

    private val closeButton by lazy { findViewById<ImageView>(R.id.close_button) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityLiveDetectionBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        graphicOverlay.apply {
            setOnClickListener(this@LiveDetectionActivity)
            cameraSource = CameraSource(this)
        }
        setUpBottomSheet()
        searchButton.setOnClickListener(this@LiveDetectionActivity)
        closeButton.setOnClickListener(this@LiveDetectionActivity)

        setWorkflowModel()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onClick(v: View?) {
            TODO("Not yet implemented")
        }

    private fun setUpBottomSheet(){
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))
        bottomSheetBehavior?.setBottomSheetCallback(
            object :BottomSheetBehavior.BottomSheetCallback(){
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    bottomSheetScrimView?.visibility = if(newState == BottomSheetBehavior.STATE_HIDDEN) View.GONE else View.VISIBLE
                    graphicOverlay.clear()

                    when(newState){
                        BottomSheetBehavior.STATE_HIDDEN ->workflowModel?.setWorkState(WorkFlow.WorkflowState.DETECTING)
                        BottomSheetBehavior.STATE_COLLAPSED,
                        BottomSheetBehavior.STATE_EXPANDED,
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> slidingSheetUpFromHiddenState = false
                        BottomSheetBehavior.STATE_DRAGGING,
                        BottomSheetBehavior.STATE_SETTLING ->{

                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    val searchedObject = workflowModel!!.searchedObject.value
                    if (searchedObject == null || java.lang.Float.isNaN(slideOffset)) {
                        return
                    }
                    val bottomSheetBehavior = bottomSheetBehavior ?: return
                    val collapsedStateHeight = bottomSheetBehavior.peekHeight.coerceAtMost(bottomSheet.height)
                    val bottomBitmap = objectThumbnailForBottomSheet ?: return
                    if (slidingSheetUpFromHiddenState) {
                        val thumbnailSrcRect = graphicOverlay.translateRect(searchedObject.boundingBox)
                        bottomSheetScrimView?.updateWithThumbnailTranslateAndScale(
                            bottomBitmap,
                            collapsedStateHeight,
                            slideOffset,
                            thumbnailSrcRect)
                    } else {
                        bottomSheetScrimView?.updateWithThumbnailTranslate(
                            bottomBitmap, collapsedStateHeight, slideOffset, bottomSheet)
                    }
                }

            }
        )
    }

    private fun setWorkflowModel(){
        workflowModel = ViewModelProviders.of(this).get(WorkFlow::class.java).apply {
            workflowState.observe(this@LiveDetectionActivity, Observer {
                if(it == null || Objects.equals(currentWorkflowState,workflowState)){
                    return@Observer
                }
                currentWorkflowState = it
                if (UtilsPreference.isAutoSearchEnable){
                    stateChangeInAuto(it)
                }else{
                    stateChangeInManualSearchMode(it)
                }
            })

            objectToSearch.observe(this@LiveDetectionActivity, Observer { detectObject ->
                Logger.d("Detect object: $detectObject")
                val capturedBitmap = detectObject.getBitmap()
                classifier.execute(
                    bitmap = capturedBitmap,
                    onError = {
                        Toast.makeText(
                            this@LiveDetectionActivity,
                            it.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onResult = {
                        showResult(it, capturedBitmap)
                    }
                )
            })
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showResult(result: List<Classifier.Recognition>, bitmap: Bitmap){
        progress.visibility = View.GONE
        objectThumbnailForBottomSheet = bitmap
        slidingSheetUpFromHiddenState = true

        if (result.size > 1) {
            val resultString = result
                .subList(1, result.size)
                .foldIndexed("") { index, acc, recognition ->
                    "${acc}${index + 2}. ${recognition.formatedString()}\n"
                }
            bottomSheetCaptionView.text = resultString
        }

        bottomSheetBestView.text = "1. ${result.first().formatedString()}"
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun stateChangeInAuto(workFlowState: WorkFlow.WorkflowState){
        val wasPromChipGone = prompChip.visibility == View.GONE
        searchButton.visibility = View.GONE
        progress.visibility = View.GONE

        when(workFlowState){
            WorkFlow.WorkflowState.DETECTING, WorkFlow.WorkflowState.DETECTED, WorkFlow.WorkflowState.CONFIRMING ->{
                prompChip.visibility = View.VISIBLE
                prompChip.setText(
                    if (workFlowState == WorkFlow.WorkflowState.CONFIRMING)
                        "Keep camera still for a moment"
                    else
                        "Point your camera at a barcode")
                startCameraPreview()
            }
            WorkFlow.WorkflowState.CONFIRMED->{
                prompChip.visibility = View.VISIBLE
                prompChip.setText("Processing...")
                stopCameraPreview()
            }
            WorkFlow.WorkflowState.SEARCHING ->{
                progress.visibility = View.VISIBLE
                prompChip.visibility = View.VISIBLE
                prompChip.setText("Processing...")
                stopCameraPreview()
            }
            WorkFlow.WorkflowState.SEARCHED -> {
                prompChip.visibility = View.GONE
                stopCameraPreview()
            }
            else -> prompChip.visibility = View.GONE
        }

    }

    private fun stateChangeInManualSearchMode(workFlowState: WorkFlow.WorkflowState){
        val wasPromChipGone = prompChip.visibility == View.GONE
        val wasSearchButtonGone = searchButton.visibility == View.GONE
        progress.visibility = View.GONE

        when(workFlowState){
            WorkFlow.WorkflowState.DETECTING, WorkFlow.WorkflowState.DETECTED, WorkFlow.WorkflowState.CONFIRMING ->{
                prompChip.visibility = View.VISIBLE
                prompChip.setText("Point your camera at an object")
                searchButton.visibility = View.GONE
                startCameraPreview()
            }
            WorkFlow.WorkflowState.CONFIRMED->{
                prompChip.visibility = View.GONE
                searchButton.visibility = View.VISIBLE
                searchButton.isEnabled = true
                searchButton.setBackgroundColor(Color.WHITE)
                startCameraPreview()
            }
            WorkFlow.WorkflowState.SEARCHING ->{
                prompChip.visibility = View.GONE
                searchButton.visibility = View.VISIBLE
                searchButton.isEnabled = false
                searchButton.setBackgroundColor(Color.GRAY)
                progress.visibility = View.VISIBLE
                stopCameraPreview()
            }
            WorkFlow.WorkflowState.SEARCHED -> {
                prompChip.visibility = View.GONE
                searchButton.visibility = View.GONE
                stopCameraPreview()
            }
            else -> {
                prompChip.visibility = View.GONE
                searchButton.visibility = View.GONE
            }
        }

    }

    private fun startCameraPreview() {
        val cameraSource = this.cameraSource ?: return
        val workflowModel = this.workflowModel ?: return
        if (!workflowModel.cameraLive) {
            try {
                workflowModel.markCameraLive()
                preview?.start(cameraSource)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to start camera preview!", e)
                cameraSource.release()
                this.cameraSource = null
            }
        }
    }

    private fun stopCameraPreview() {
        if (workflowModel?.cameraLive == true) {
            workflowModel!!.markCameraFrozen()
            preview?.stop()
        }
    }

    companion object{
        private const val TAG = "LiveDetectionActivity"
    }


}