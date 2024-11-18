package org.openmw.ui.controls

import android.annotation.SuppressLint
import android.app.ActionBar.LayoutParams
import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import org.libsdl.app.SDLActivity
import org.openmw.R
import kotlin.math.roundToInt

class MouseCursor(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {
    var cursorX = SDLActivity.getMouseX().toFloat()
    var cursorY = SDLActivity.getMouseY().toFloat()
    private var offsetX = 0f
    private var offsetY = 0f
    var sdlView: View? = null
    private var cursorView: ImageView? = null
    private val cursorIcon = ContextCompat.getDrawable(context, R.drawable.pointer_icon)!!
    var isCursorVisible = false

    init {
        setupViews(context)
    }

    private fun setupViews(context: Context) {
        cursorView = ImageView(context).apply {
            setImageDrawable(cursorIcon)
            visibility = View.GONE
            layoutParams = LayoutParams(
                cursorIcon.intrinsicWidth,
                cursorIcon.intrinsicHeight
            )
        }
        (parent as? FrameLayout)?.addView(cursorView)

        // Container for the buttons
        val buttonContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        }

        // Button to perform mouse click
        val clickButton = Button(context).apply {
            text = "Click"
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                val event = MotionEvent.obtain(
                    SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_UP,
                    cursorX,
                    cursorY,
                    0
                )
                performMouseClick(event)
            }
        }
        buttonContainer.addView(clickButton)

        // Button to disable cursor
        val disableButton = Button(context).apply {
            text = "Disable"
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                disableCursor()
            }
        }
        buttonContainer.addView(disableButton)

        // Add the button container to the layout
        (parent as? FrameLayout)?.addView(buttonContainer)
    }

    fun enableCursor() {
        cursorView?.visibility = View.VISIBLE
        isCursorVisible = true
    }

    fun disableCursor() {
        cursorView?.visibility = View.GONE
        isCursorVisible = false
    }

    fun setCursorPosition(x: Float, y: Float) {
        cursorX = x.coerceIn(0f, (parent as View).width.toFloat() - cursorIcon.intrinsicWidth)
        cursorY = y.coerceIn(0f, (parent as View).height.toFloat() - cursorIcon.intrinsicHeight)
        cursorView?.layoutParams = (cursorView?.layoutParams as FrameLayout.LayoutParams).apply {
            leftMargin = cursorX.toInt()
            topMargin = cursorY.toInt()
        }
        cursorView?.requestLayout()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isCursorVisible) {
            val iconSize = 72
            cursorIcon.setBounds(cursorX.toInt(), cursorY.toInt(), cursorX.toInt() + iconSize, cursorY.toInt() + iconSize)
            cursorIcon.draw(canvas)
        }
    }

    fun performMouseClick(event: MotionEvent) {
        sdlView?.apply {
            val viewLocation = IntArray(2)
            getLocationOnScreen(viewLocation)

            val adjustedX = cursorX
            val adjustedY = cursorY

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

            dispatchTouchEvent(eventDown)
            dispatchTouchEvent(eventUp)
            eventDown.recycle()
            eventUp.recycle()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isCursorVisible) {
            cursorView?.let { cursor ->
                setOnTouchListener { _, motionEvent ->
                    when (motionEvent.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            offsetX = event.x - cursorX
                            offsetY = event.y - cursorY
                            cursor.visibility = View.VISIBLE
                            false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            setCursorPosition(event.x - offsetX, event.y - offsetY)
                            SDLActivity.sendRelativeMouseMotion(offsetX.roundToInt(), offsetY.roundToInt())
                        }
                        MotionEvent.ACTION_UP -> {
                            offsetX = event.x - cursorX
                            offsetY = event.y - cursorY
                            performMouseClick(event)
                        }
                    }
                    true
                }
            }
        }
        return false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cursorView = ImageView(context).apply {
            setImageDrawable(cursorIcon)
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                cursorIcon.intrinsicWidth,
                cursorIcon.intrinsicHeight
            )
        }
        (parent as? FrameLayout)?.addView(cursorView)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        (parent as? FrameLayout)?.removeView(cursorView)
    }
}

