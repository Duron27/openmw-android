@file:OptIn(DelicateCoroutinesApi::class)

package org.openmw

import android.annotation.SuppressLint
import android.app.Activity
import android.app.LauncherActivity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import android.view.Choreographer
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.libsdl.app.SDLActivity
import org.openmw.navigation.NavmeshScreen
import org.openmw.ui.controls.BoxGrid2
import org.openmw.ui.controls.GamepadEmulatorScreen
import org.openmw.ui.controls.MouseIcon
import org.openmw.ui.controls.ResizableDraggableButton
import org.openmw.ui.controls.ResizableDraggableRightThumbstick
import org.openmw.ui.controls.ResizableDraggableThumbstick
import org.openmw.ui.controls.UIStateManager
import org.openmw.ui.controls.UIStateManager.LeftJoy
import org.openmw.ui.controls.UIStateManager.VirtualKB
import org.openmw.ui.controls.UIStateManager.configureControls
import org.openmw.ui.controls.UIStateManager.editMode
import org.openmw.ui.controls.UIStateManager.enableRightThumb
import org.openmw.ui.controls.UIStateManager.gridAlpha
import org.openmw.ui.controls.UIStateManager.gridVisible
import org.openmw.ui.controls.UIStateManager.isCursorVisible
import org.openmw.ui.controls.UIStateManager.launchedActivity
import org.openmw.ui.controls.UIStateManager.offsetXFlow
import org.openmw.ui.controls.UIStateManager.offsetYFlow
import org.openmw.ui.overlay.GridOverlay
import org.openmw.ui.overlay.OverlayUI
import org.openmw.utils.GameFilesPreferences
import org.openmw.utils.GameFilesPreferences.getCustomGLDriverPath
import org.openmw.utils.GameFilesPreferences.loadAutoMouseMode
import org.openmw.utils.enableLogcat
import java.io.File
import kotlin.math.roundToInt

@Suppress("DEPRECATION")
@InternalCoroutinesApi
class EngineActivity : SDLActivity() {
    private lateinit var sdlView: View

    init {
        setEnvironmentVariables(this@EngineActivity)
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    override fun getLibraries(): Array<String> {
        val customGlDriverPath = getCustomGlDriverPathSync(this)
        val libsPath = "${Constants.USER_FILE_STORAGE}/libs/"

        val jniLibsArray = if (UIStateManager.tempCodeGroup == "OpenMW") {
            arrayOf("SDL2", "c++_shared", "openal", "openmw")
        } else if (UIStateManager.tempCodeGroup == "UQM") {
            arrayOf("SDL2", "c++_shared", "openal", "uqm")
        } else {
            emptyArray()
        }

        jniLibsArray.forEach { libName ->
            val customLibFile = File(libsPath + "lib$libName.so")
            if (customLibFile.exists()) {
                Log.d("LibraryLoader", "Loading custom library: ${customLibFile.absolutePath}")
                System.load(customLibFile.absolutePath)
            } else {
                Log.d("LibraryLoader", "Custom library NOT FOUND: ${customLibFile.absolutePath}")
                Log.d("LibraryLoader", "Loading library from APK: lib$libName.so")
                System.loadLibrary(libName)
            }
        }

        if (customGlDriverPath != null) {
            Log.d("LibraryLoader", "Loading custom GL driver: $customGlDriverPath")
            System.load(customGlDriverPath)
        } else {
            Log.d("LibraryLoader", "Loading default GL library from APK: libGL.so")
            System.loadLibrary("ng_gl4es")
        }

        return jniLibsArray
    }


    override fun getMainSharedObject(): String {
        val libsPath = Constants.USER_FILE_STORAGE + "/libs"
        val sharedObjectName = when (UIStateManager.tempCodeGroup) {
            "OpenMW" -> OPENMW_MAIN_LIB
            "UQM" -> UQM_MAIN_LIB
            else -> ""
        }

        val customLibFile = File(libsPath + "lib$sharedObjectName.so")
        return if (customLibFile.exists()) {
            Log.d("SharedObjectLoader", "Loading shared object from custom directory: ${customLibFile.absolutePath}")
            customLibFile.absolutePath
        } else {
            Log.d("SharedObjectLoader", "Loading shared object from APK: $sharedObjectName")
            sharedObjectName
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        launchedActivity = true
        val controllerConnected = isControllerConnected(this)

        setContentView(R.layout.engine_activity)

        if (!configureControls) {
            sdlView = getContentView()
        } else if (configureControls) {
            sdlView = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        }

        hideSystemBars(this)

        // Add SDL view programmatically
        val sdlContainer = findViewById<FrameLayout>(R.id.sdl_container)
        (sdlView.parent as? ViewGroup)?.removeView(sdlView)
        sdlContainer.addView(sdlView) // Add SDL view to the sdl_container

        if (UIStateManager.tempCodeGroup == "OpenMW") {
            getPathToJni(filesDir.parent!!, Constants.USER_FILE_STORAGE)
        }

        if (UIStateManager.isLogcatEnabled) {
            enableLogcat()
        }

        // Setup Compose overlay for buttons
        val composeViewUI = findViewById<ComposeView>(R.id.compose_overlayUI)
        (composeViewUI.parent as? ViewGroup)?.removeView(composeViewUI)
        sdlContainer.addView(composeViewUI)

        // Adding a Global Layout Listener to get container dimensions
        sdlContainer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val containerWidth = sdlContainer.width.toFloat()
                val containerHeight = sdlContainer.height.toFloat()

                // Load button states into UIStateManager with container dimensions
                UIStateManager.loadButtonState(this@EngineActivity, containerWidth, containerHeight)

                if (!UIStateManager.useNavmesh) {

                    // Adds Overlay menu for buttons and edit mode
                    composeViewUI.setContent {
                        val autoMouseMode by loadAutoMouseMode(this@EngineActivity).collectAsState(initial = "Hybrid")

                        AutoMouseModeComposable()
                        val snapX = remember { mutableStateOf<Float?>(null) }
                        val snapY = remember { mutableStateOf<Float?>(null) }

                        if (configureControls) {
                            BackgroundAnimation()
                        }

                        if (editMode && gridVisible.value) {
                            GridOverlay(
                                gridSize = UIStateManager.gridSize.intValue,
                                snapX = snapX.value,
                                snapY = snapY.value,
                                alpha = gridAlpha.floatValue
                            )
                        }

                        if (isCursorVisible == 1) {
                            if (autoMouseMode != "None" || controllerConnected) {
                                MouseIcon()
                            }
                        }

                        OverlayUI(
                            context = this@EngineActivity,
                            onKeyEvent = { keyCode -> handleKeyEvent(keyCode) }
                        )

                        if (VirtualKB) {
                            BoxGrid2()
                        }

                        Buttons(context = this@EngineActivity, containerWidth = containerWidth, containerHeight = containerHeight, sdlView = sdlView)

                        if (enableRightThumb) {
                            ResizableDraggableRightThumbstick(
                                context = this@EngineActivity,
                                containerWidth = containerWidth,
                                containerHeight = containerHeight
                            )
                        }

                    }

                    val composeViewLeftThumb = findViewById<ComposeView>(R.id.compose_leftThumb)
                    (composeViewLeftThumb.parent as? ViewGroup)?.removeView(composeViewLeftThumb)

                    initializeOffsetsFromStateFlow()

                    composeViewLeftThumb.setContent {
                        if (LeftJoy) {
                            GamepadEmulatorScreen()
                        } else {
                            ResizableDraggableThumbstick(
                                context = this@EngineActivity,
                                containerWidth = containerWidth,
                                containerHeight = containerHeight
                            )
                        }
                    }

                    sdlContainer.addView(composeViewLeftThumb)
                    updateComposeViewPosition(composeViewLeftThumb)
                } else if (UIStateManager.useNavmesh) {
                    composeViewUI.setContent {
                        BackgroundAnimation()
                        NavmeshScreen(onComplete = { navigateToLauncher() })
                    }
                }
                // Remove the Global Layout Listener to prevent multiple calls
                sdlContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    private fun getCustomGlDriverPathSync(context: Context): String? {
        var customGlDriverPath: String? = null
        runBlocking {
            customGlDriverPath = getCustomGLDriverPath(context).first()
        }
        return customGlDriverPath
    }

    private fun navigateToLauncher() {
        val intent = Intent(this, LauncherActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun initializeOffsetsFromStateFlow() {
        lifecycleScope.launch {
            val buttonStates = UIStateManager.buttonStates.value
            buttonStates[99]?.let { state ->
                offsetXFlow.value = state.offsetX
                offsetYFlow.value = state.offsetY
            }
        }
    }

    private fun updateComposeViewPosition(composeViewLeftThumb: ComposeView) {
        lifecycleScope.launch {
            launch {
                offsetXFlow.collect { offsetX ->
                    val params = composeViewLeftThumb.layoutParams as FrameLayout.LayoutParams
                    params.leftMargin = offsetX.roundToInt()
                    composeViewLeftThumb.layoutParams = params
                }
            }
            launch {
                offsetYFlow.collect { offsetY ->
                    val params = composeViewLeftThumb.layoutParams as FrameLayout.LayoutParams
                    params.topMargin = offsetY.roundToInt()
                    composeViewLeftThumb.layoutParams = params
                }
            }
        }
    }

    private suspend fun getCommandLine(context: Context): String {
        return GameFilesPreferences.getCommandLine(context).first() ?: ""
    }

    private fun setEnvironmentVariables(context: Context) {
        if (UIStateManager.tempCodeGroup == "OpenMW") {
            try {
                Os.setenv("OSG_TEXT_SHADER_TECHNIQUE", "ALL", true)
                Os.setenv("OSG_VERTEX_BUFFER_HINT", "VBO", true)
                Os.setenv("OSG_GL_TEXTURE_STORAGE", "OFF", true)
                Os.setenv("LIBGL_SIMPLE_SHADERCONV", "1", true)
                Os.setenv("LIBGL_INSTANCING", "1", true)
                Os.setenv("LIBGL_DXTMIPMAP", "1", true)

                Os.setenv("OPENMW_GLES_VERSION", "3", true)
                Os.setenv("LIBGL_ES", "3", true)
            } catch (e: ErrnoException) {
                Log.e("OpenMW", "Failed setting environment variables.")
                e.printStackTrace()
            }

            //Os.setenv("OPENMW_USER_FILE_STORAGE", Constants.USER_FILE_STORAGE + "/", true)

            CoroutineScope(Dispatchers.IO).launch {
                val commandLine: String = getCommandLine(context).toString()
                withContext(Dispatchers.Main) {
                    if (commandLine.isNotEmpty()) {
                        val envs: List<String> = commandLine.split(" ", "\n")
                        var i = 0

                        repeat(envs.count())
                        {
                            val env: List<String> = envs[i].split("=")
                            if (env.count() == 2) Os.setenv(env[0], env[1], true)
                            i = i + 1
                        }
                    }
                }

                GameFilesPreferences.readAvoid16Bits(context).collect { avoid16bits ->
                    if (avoid16bits) {
                        Os.setenv("LIBGL_AVOID16BITS", "1", true)
                    } else {
                        Os.setenv("LIBGL_AVOID16BITS", "0", true)
                    }
                }
                // Set LIBGL_SHRINK based on preference
                GameFilesPreferences.readTextureShrinkingOption(context)
                    .collect { textureShrinkingOption ->
                        when (textureShrinkingOption) {
                            "low" -> Os.setenv("LIBGL_SHRINK", "2", true)
                            "medium" -> Os.setenv("LIBGL_SHRINK", "7", true)
                            "high" -> Os.setenv("LIBGL_SHRINK", "6", true)
                            else -> Os.setenv("LIBGL_SHRINK", "0", true)
                        }
                    }
            }
        } else {
            try {
                Os.setenv("LIBGL_ES", "2", true)
            } catch (e: ErrnoException) {
                Log.e("UQM", "Failed setting environment variables.")
                e.printStackTrace()
            }
        }

        Log.d("EngineActivity", "Environment variables set")
    }
    private external fun getPathToJni(path_global: String, path_user: String)

    private fun handleKeyEvent(keyCode: Int) {
        val keyEventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val keyEventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        dispatchKeyEvent(keyEventDown)
        dispatchKeyEvent(keyEventUp)
        Log.d(TAG, "Sent key event: $keyCode")
    }

    companion object {
        private const val TAG = "OpenMW-Launcher"

        var resolutionX = 0
        var resolutionY = 0
    }

    override fun getArguments(): Array<String> {
        val commandLineArgs = runBlocking {
            GameFilesPreferences.getCommandLine(this@EngineActivity).first() ?: ""
        }

        // Check if command-line arguments are valid
        if (commandLineArgs.isNullOrEmpty() || !commandLineArgs.contains("-")) {
            return super.getArguments()
        }

        return try {
            // Construct resource path
            val resourcesPath = "--resources " + Constants.USER_FILE_STORAGE + "/resources"
            val resourcesPathUQM = "-contentDir= " + Constants.USER_RESOURCES + "/libuqm"

            val combinedArgs = if (UIStateManager.tempCodeGroup == "OpenMW") {
                "$resourcesPath $commandLineArgs"
            } else {
                "$commandLineArgs $resourcesPathUQM"
            }

            // Parse the combined arguments
            val args = arrayListOf<String>()
            combinedArgs.split(" ".toRegex()).forEach {
                if (it.isNotEmpty()) {
                    args.add(it)
                }
            }
            args.toTypedArray()
        } catch (e: Exception) {
            super.getArguments()
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun AutoMouseModeComposable() {
    var isMouseShown by remember { mutableIntStateOf(SDLActivity.isMouseShown()) }
    // Launch a Choreographer callback to update isMouseShown in real-time
    DisposableEffect(Unit) {
        val choreographer = Choreographer.getInstance()
        val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                isMouseShown = SDLActivity.isMouseShown()
                isCursorVisible = isMouseShown
                choreographer.postFrameCallback(this)
            }
        }
        choreographer.postFrameCallback(frameCallback)

        onDispose {
            choreographer.removeFrameCallback(frameCallback)
        }
    }
}

@Composable
fun Buttons(context: Context, containerWidth: Float, containerHeight: Float, sdlView: View) {
    val buttonStates by UIStateManager.buttonStates.collectAsState()

    buttonStates.values
        .filter { it.id !in listOf(99, 98) }
        .forEach { button ->
            ResizableDraggableButton(
                context = context,
                id = button.id,
                keyCode = button.keyCode,
                containerWidth = containerWidth,
                containerHeight = containerHeight,
                sdlView = sdlView,
                onDelete = {
                    UIStateManager.removeButtonState(button.id, context, containerWidth, containerHeight)
                }
            )
        }
}

fun hideSystemBars(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.window.insetsController?.let {
            it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        @Suppress("DEPRECATION")
        activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }
}
