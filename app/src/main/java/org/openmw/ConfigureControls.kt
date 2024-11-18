package org.openmw

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.openmw.ui.controls.MouseCursor
import org.openmw.ui.controls.ResizableDraggableButton
import org.openmw.ui.controls.ResizableDraggableRightThumbstick
import org.openmw.ui.controls.ResizableDraggableThumbstick
import org.openmw.ui.controls.ScaleView
import org.openmw.ui.controls.UIStateManager
import org.openmw.ui.controls.UIStateManager.createdButtons
import org.openmw.ui.controls.UIStateManager.editMode
import org.openmw.ui.controls.UIStateManager.gridAlpha
import org.openmw.ui.controls.UIStateManager.gridVisible
import org.openmw.ui.controls.loadButtonState
import org.openmw.ui.controls.saveButtonState
import org.openmw.ui.overlay.GridOverlay
import org.openmw.ui.overlay.OverlayUI
import org.openmw.utils.BouncingBackground

class ConfigureControls : AppCompatActivity() { // Use AppCompatActivity for simplicity
    private lateinit var sdlView: View
    private lateinit var scaleView: ScaleView
    private var isScaled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        UIStateManager.configureControls = true
        setContentView(R.layout.engine_activity)
        // Initialize placeholder SDL View
        sdlView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Hide the system bars
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val mouseCursor: MouseCursor? = null

        // Reference the sdlContainer and scaleView
        val sdlContainer = findViewById<FrameLayout>(R.id.sdl_container)
        // Remove sdlView from its parent if necessary
        (sdlView.parent as? ViewGroup)?.removeView(sdlView)
        sdlContainer.addView(sdlView, 0)

        scaleView = findViewById(R.id.scaleView)

        // Set sdlView in scaleView and scale it
        scaleView.sdlView = sdlView
        scaleView.scaleSdlView(isScaled)

        // Load UI saved buttons, 99 is the Thumbstick. Without these 3 lines the button loader will read 99
        // from the UI.cfg file and create a duplicate as a button
        val allButtons = loadButtonState(this@ConfigureControls)
        val thumbstick = allButtons.find { it.id in listOf(99, 98) }
        createdButtons.addAll(allButtons.filter { it.id !in listOf(99, 98) })

        // Setup Compose overlay for buttons
        val composeViewUI = findViewById<ComposeView>(R.id.compose_overlayUI)
        (composeViewUI.parent as? ViewGroup)?.removeView(composeViewUI)
        (scaleView.parent as? ViewGroup)?.removeView(scaleView)
        sdlContainer.addView(scaleView)
        sdlContainer.addView(composeViewUI, 2)

        // Add your buttons or other UI elements
        composeViewUI.setContent {
            val snapX = remember { mutableStateOf<Float?>(null) }
            val snapY = remember { mutableStateOf<Float?>(null) }

            BouncingBackground()

            if (editMode && gridVisible.value) {
                GridOverlay(gridSize = UIStateManager.gridSize.intValue, snapX = snapX.value, snapY = snapY.value, alpha = gridAlpha.floatValue)
            }

            OverlayUI(
                context = this,
                editMode = editMode,
                createdButtons = createdButtons,
                scaleView = scaleView,
                mouseCursor = mouseCursor
            )

            createdButtons.forEach { button ->
                ResizableDraggableButton(
                    context = this,
                    id = button.id,
                    keyCode = button.keyCode,
                    editMode = editMode,
                    onDelete = { deleteButton(button.id) },
                )
            }

            thumbstick?.let {
                ResizableDraggableThumbstick(
                    context = this,
                    id = 99,
                    keyCode = it.keyCode,
                    editMode = editMode
                )
            }

            ResizableDraggableRightThumbstick(
                context = this,
                id = 98,
                editMode = editMode
            )
        }
    }

    private fun deleteButton(buttonId: Int) {
        createdButtons.removeIf { it.id == buttonId }
        saveButtonState(this, createdButtons + loadButtonState(this).filter { it.id == 99 }) // Ensure thumbstick is not removed
    }
}
