package org.openmw

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.MediaStore
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.libsdl.app.SDLActivity
import org.openmw.ui.controls.MouseCursor
import org.openmw.ui.controls.ResizableDraggableButton
import org.openmw.ui.controls.ResizableDraggableRightThumbstick
import org.openmw.ui.controls.ResizableDraggableThumbstick
import org.openmw.ui.controls.ScaleView
import org.openmw.ui.controls.UIStateManager
import org.openmw.ui.controls.UIStateManager.REQUEST_CODE_PICK_IMAGE
import org.openmw.ui.controls.UIStateManager.createdButtons
import org.openmw.ui.controls.UIStateManager.editMode
import org.openmw.ui.controls.UIStateManager.gridAlpha
import org.openmw.ui.controls.UIStateManager.gridVisible
import org.openmw.ui.controls.UIStateManager.isCursorVisible
import org.openmw.ui.controls.loadButtonState
import org.openmw.ui.controls.saveButtonState
import org.openmw.ui.overlay.GridOverlay
import org.openmw.ui.overlay.OverlayUI
import org.openmw.utils.enableLogcat
import kotlin.Boolean

class EngineActivity : SDLActivity() {
    private var useNavmesh: Boolean = false
    private lateinit var scaleView: ScaleView
    private lateinit var sdlView: View
    private var isScaled = false
    private var gridSize = UIStateManager.gridSize

    init {
        setEnvironmentVariables()
    }

    companion object {
        var resolutionX = 0
        var resolutionY = 0
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
            val imageUri = data?.data
            if (imageUri != null) {
                val filePath = getRealPathFromURI(this, imageUri)
                Log.d("ImagePicker", "Selected Image URI: $imageUri")
                Log.d("ImagePicker", "Selected Image Path: $filePath")
            }
        }
    }

    fun getRealPathFromURI(context: Context, uri: Uri): String? {
        var result: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            if (idx >= 0) {
                result = cursor.getString(idx)
            }
            cursor.close()
        }
        return result
    }

    override fun getLibraries(): Array<String> {
        return if (useNavmesh) {
            jniLibsArrayNavmesh
        } else {
            try {
                Log.d("EngineActivity", "Loading libraries: ${jniLibsArray.joinToString(", ")}")
                jniLibsArray
            } catch (e: Exception) {
                Log.e("EngineActivity", "Error loading libraries", e)
                emptyArray()
            }
        }
    }

    override fun getMainSharedObject(): String {
        return if (useNavmesh) {
            OPENMW_NAVMESH_LIB
        } else {
            OPENMW_MAIN_LIB
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        UIStateManager.configureControls = false
        useNavmesh = intent.getBooleanExtra("USE_NAVMESH", false)

        if (useNavmesh) {
            setContentView(R.layout.engine_activity)
            sdlView = getContentView()
            val sdlContainer = findViewById<FrameLayout>(R.id.sdl_container)
            sdlContainer.addView(sdlView, 0)
            runNavmeshOperations()
        } else {
            setContentView(R.layout.engine_activity)
            sdlView = getContentView()
            scaleView = findViewById(R.id.scaleView)

            // Set sdlView in scaleView and scale it
            scaleView.sdlView = sdlView
            scaleView.scaleSdlView(isScaled)

            // Ensure the correct initial state of the cursor
            setupInitialScaleState()

            // Load UI saved buttons, 99 is the Thumbstick. Without these 3 lines the button loader will read 99
            // from the UI.cfg file and create a duplicate as a button
            val allButtons = loadButtonState(this@EngineActivity)
            val thumbstick = allButtons.find { it.id in listOf(99, 98) }
            createdButtons.addAll(allButtons.filter { it.id !in listOf(99, 98) })

            // Add SDL view programmatically
            val sdlContainer = findViewById<FrameLayout>(R.id.sdl_container)
            sdlContainer.addView(sdlView, 0)

            // Remove sdlView from its parent if necessary
            (sdlView.parent as? ViewGroup)?.removeView(sdlView)
            sdlContainer.addView(sdlView) // Add SDL view to the sdl_container
            (scaleView.parent as? ViewGroup)?.removeView(scaleView)
            sdlContainer.addView(scaleView)

            val mouseCursor = MouseCursor(this, null)
            sdlContainer.addView(mouseCursor)

            // Set MouseCursor View
            if (isCursorVisible) {
                mouseCursor.enableCursor()
            } else {
                mouseCursor.disableCursor()
            }

            WindowCompat.setDecorFitsSystemWindows(window, false)
            // Hide the system bars
            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            getPathToJni(filesDir.parent!!, Constants.USER_FILE_STORAGE)

            if (UIStateManager.isLogcatEnabled) {
                enableLogcat()
            }

            // Setup Compose overlay for buttons
            val composeViewUI = findViewById<ComposeView>(R.id.compose_overlayUI)
            (composeViewUI.parent as? ViewGroup)?.removeView(composeViewUI)
            sdlContainer.addView(composeViewUI, 2)

            // Adds Overlay menu for buttons and edit mode
            composeViewUI.setContent {
                val snapX = remember { mutableStateOf<Float?>(null) }
                val snapY = remember { mutableStateOf<Float?>(null) }

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
    }

    private fun deleteButton(buttonId: Int) {
        createdButtons.removeIf { it.id == buttonId }
        saveButtonState(this, createdButtons + loadButtonState(this).filter { it.id in listOf(99, 98) })
    }

    private fun setupInitialScaleState() {
        if (UIStateManager.isScaleView) {
            scaleView.visibility = View.VISIBLE
        } else {
            scaleView.visibility = View.GONE
        }
    }

    private fun runNavmeshOperations() {
        try {
            // Log the start of the Navmesh operations
            Log.d("EngineActivity", "Running Navmesh Operations")

            enableLogcat()

            // Run JNI path retrieval
            getPathToJni(filesDir.parent!!, Constants.USER_FILE_STORAGE)


        } catch (e: Exception) {
            Log.e("EngineActivity", "Error running Navmesh operations", e)
        }
    }

    private fun setEnvironmentVariables() {
        try {
            Os.setenv("OSG_TEXT_SHADER_TECHNIQUE", "ALL", true)
        } catch (e: ErrnoException) {
            Log.e("OpenMW", "Failed setting environment variables.")
            e.printStackTrace()
        }

        Os.setenv("OSG_VERTEX_BUFFER_HINT", "VBO", true)
        Os.setenv("OPENMW_USER_FILE_STORAGE", Constants.USER_FILE_STORAGE + "/", true)

        try {
            Os.setenv("OPENMW_GLES_VERSION", "2", true)
            Os.setenv("LIBGL_ES", "2", true)
        } catch (e: ErrnoException) {
            Log.e("OpenMW", "Failed setting environment variables.")
            e.printStackTrace()
        }
        Log.d("EngineActivity", "Environment variables set")
    }
    private external fun getPathToJni(path_global: String, path_user: String)

    public override fun onDestroy() {
        finish()
        Process.killProcess(Process.myPid())
        super.onDestroy()
    }
}
