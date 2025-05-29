package org.openmw.ui.controls

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class ScaleView(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {

    private var offsetX = 0f
    private var offsetY = 0f
    private var scaleFactor = 1f
    var sdlView: View? = null
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private var centerX: Float = 0f
    private var centerY: Float = 0f

    fun scaleSdlView(scale: Boolean) {
        sdlView?.apply {
            scaleFactor = if (scale) 2f else 1f
            scaleX = scaleFactor
            scaleY = scaleFactor
            if (!scale) resetPosition()
        }
        logScaleAndPosition()
    }

    private fun resetPosition() {
        sdlView?.apply {
            translationX = (this@ScaleView.width - width * scaleFactor) / 2f
            translationY = (this@ScaleView.height - height * scaleFactor) / 2f
        }
        logScaleAndPosition()
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(1f, 3f) // Adjust the range as necessary
            sdlView?.scaleX = scaleFactor
            sdlView?.scaleY = scaleFactor
            logScaleAndPosition()
            invalidate()
            return true
        }
    }

    private fun logScaleAndPosition() {
        sdlView?.apply {
            centerX = width / 2f
            centerY = height / 2f
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                offsetX = event.x - (sdlView?.translationX ?: 0f)
                offsetY = event.y - (sdlView?.translationY ?: 0f)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val newX = event.x - offsetX
                val newY = event.y - offsetY
                sdlView?.translationX = newX
                sdlView?.translationY = newY
                logScaleAndPosition()
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                return true
            }
        }
        return false
    }
}
