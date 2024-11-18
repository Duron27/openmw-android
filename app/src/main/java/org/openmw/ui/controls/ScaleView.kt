package org.openmw.ui.controls

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color

class ScaleView(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {

    private var offsetX = 0f
    private var offsetY = 0f
    private var scaleFactor = 1f
    var sdlView: View? = null
    private var exitButtonView: ComposeView? = null
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private var centerX: Float = 0f
    private var centerY: Float = 0f

    fun scaleSdlView(scale: Boolean) {
        sdlView?.apply {
            scaleFactor = if (scale) 2f else 1f
            scaleX = scaleFactor
            scaleY = scaleFactor
            if (!scale) resetPosition() // Reset position when unscaled
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

    fun addExitButton() {
        exitButtonView = ComposeView(context).apply {
            setContent {
                Box(
                    contentAlignment = Alignment.TopEnd,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            scaleSdlView(false)
                            UIStateManager.isUIHidden = false
                            UIStateManager.visible = true
                            removeExitButton()
                        },
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color.Red, shape = CircleShape),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text("X", color = Color.White)
                    }
                }
            }
        }
        (parent as? FrameLayout)?.addView(exitButtonView)
    }

    fun removeExitButton() {
        (parent as? FrameLayout)?.removeView(exitButtonView)
        exitButtonView = null
    }

    private fun performMouseClick(event: MotionEvent) {
        sdlView?.apply {
            // Calculate the offset of sdlView in relation to its parent
            val viewLocation = IntArray(2)
            getLocationOnScreen(viewLocation)

            val adjustedX = (event.x - viewLocation[0]) / scaleFactor
            val adjustedY = (event.y - viewLocation[1]) / scaleFactor

            val eventDown = MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN,
                adjustedX,
                adjustedY,
                0
            )
            val eventUp = MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP,
                adjustedX,
                adjustedY,
                0
            )

            Log.d("CustomCursorView", "Performing click at X: $adjustedX, Y: $adjustedY")

            dispatchTouchEvent(eventDown)
            dispatchTouchEvent(eventUp)
            eventDown.recycle()
            eventUp.recycle()
        }
    }

    private fun logScaleAndPosition() {
        sdlView?.apply {
            centerX = width / 2f
            centerY = height / 2f
            Log.d("CustomCursorView", "Scale Factor: $scaleFactor, Center: X=$centerX, Y=$centerY, Position: X=$translationX, Y=$translationY")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                offsetX = event.x - (sdlView?.translationX ?: 0f)
                offsetY = event.y - (sdlView?.translationY ?: 0f)
                Log.d("CustomCursorView", "Touch Down at X: ${event.x}, Y: ${event.y}")
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val newX = event.x - offsetX
                val newY = event.y - offsetY
                sdlView?.translationX = newX
                sdlView?.translationY = newY
                logScaleAndPosition()
                invalidate() // Redraw the view
                Log.d("CustomCursorView", "Touch Move at X: ${event.x}, Y: ${event.y}")
                return true
            }
            MotionEvent.ACTION_UP -> {
                Log.d("CustomCursorView", "Touch Released at X: ${event.x}, Y: ${event.y}")
                performMouseClick(event)
                return true
            }
        }
        return false
    }
}
