package com.ocha.prototype_final.CustomView.objectDetected

import android.os.CountDownTimer
import com.ocha.prototype_final.CustomView.camera.GraphicOverlay
import com.ocha.prototype_final.settings.UtilsPreference

internal class ObjectConfirmationController (graphicOverlay: GraphicOverlay) {
    private val countDownTimer: CountDownTimer

    private var objectId: Int? = null
    var progress = 0f
        private set

    val isConfirmed: Boolean
        get() = progress.compareTo(1f) == 0

    init {
        val confirmationTimeMs = UtilsPreference.getConfirmationTimeMs().toLong()
        countDownTimer = object : CountDownTimer(confirmationTimeMs, /* countDownInterval= */ 20) {
            override fun onTick(millisUntilFinished: Long) {
                progress = (confirmationTimeMs - millisUntilFinished).toFloat() / confirmationTimeMs
                graphicOverlay.invalidate()
            }

            override fun onFinish() {
                progress = 1f
            }
        }
    }

    fun confirming(objectId: Int?) {
        if (objectId == this.objectId) {
            // Do nothing if it's already in confirming.
            return
        }

        reset()
        this.objectId = objectId
        countDownTimer.start()
    }

    fun reset() {
        countDownTimer.cancel()
        objectId = null
        progress = 0f
    }

}