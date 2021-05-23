package com.ocha.prototype_final.CustomView.objectDetected

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.ocha.prototype_final.CustomView.camera.GraphicOverlay

class ObjectAnimatorDot(graphicOverlay: GraphicOverlay) {
    private val animatorSet: AnimatorSet

    var radiusScale = 0f
        private set
    var alphaScale = 0f
        private set

    init {
        val dotScaleUpAnimator = ValueAnimator.ofFloat(0f, 1.3f)
            .setDuration(DURATION_DOT_SCALE_UP_MS)
        dotScaleUpAnimator.interpolator = FastOutSlowInInterpolator()
        dotScaleUpAnimator.addUpdateListener { animation ->
            radiusScale = animation.animatedValue as Float
            graphicOverlay.postInvalidate()
        }

        val dotScaleDownAnimator = ValueAnimator.ofFloat(1.3f, 1f)
            .setDuration(DURATION_DOT_SCALE_DOWN_MS)
        dotScaleDownAnimator.startDelay = START_DELAY_DOT_SCALE_DOWN_MS
        dotScaleDownAnimator.interpolator = PathInterpolatorCompat
            .create(0.4f, 0f, 0f, 1f)
        dotScaleDownAnimator.addUpdateListener { animation ->
            radiusScale = animation.animatedValue as Float
            graphicOverlay.postInvalidate()
        }

        val dotFadeInAnimator = ValueAnimator.ofFloat(0f, 1f)
            .setDuration(DURATION_DOT_FADE_IN_MS)
        dotFadeInAnimator.addUpdateListener { animation ->
            alphaScale = animation.animatedValue as Float
            graphicOverlay.postInvalidate()
        }

        animatorSet = AnimatorSet()
        animatorSet.playTogether(dotScaleUpAnimator, dotScaleDownAnimator, dotFadeInAnimator)
    }

    fun start() {
        if (!animatorSet.isRunning) {
            animatorSet.start()
        }
    }

    fun cancel() {
        animatorSet.cancel()
        radiusScale = 0f
        alphaScale = 0f
    }

    companion object {
        // All these time constants are in millisecond unit.
        private const val DURATION_DOT_SCALE_UP_MS: Long = 217
        private const val DURATION_DOT_SCALE_DOWN_MS: Long = 783
        private const val DURATION_DOT_FADE_IN_MS: Long = 150
        private const val START_DELAY_DOT_SCALE_DOWN_MS: Long = 217
    }
}