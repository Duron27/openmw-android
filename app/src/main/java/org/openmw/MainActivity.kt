package org.openmw

import android.content.Context
import android.content.Intent
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.InputDevice.SOURCE_GAMEPAD
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openmw.ui.controls.UIStateManager
import org.openmw.ui.controls.UIStateManager.firstLaunch
import org.openmw.ui.theme.OpenMWTheme
import org.openmw.utils.BouncingBackground
import org.openmw.utils.CaptureCrash
import org.openmw.utils.CircularBackground
import org.openmw.utils.ConfigFileObserver
import org.openmw.utils.GameFilesPreferences
import org.openmw.utils.GameFilesPreferences.readCodeGroup
import org.openmw.utils.LogRepository
import org.openmw.utils.LogsBox
import org.openmw.utils.ModValue
import org.openmw.utils.MultiPathFileObserver
import org.openmw.utils.MyAlertDialog
import org.openmw.utils.NoneBackground
import org.openmw.utils.PermissionHelper
import org.openmw.utils.PermissionHelper.getManageExternalStoragePermission
import org.openmw.utils.RotatingImageBackground
import org.openmw.utils.UserManageAssets
import org.openmw.utils.currentDeviceRealSize
import org.openmw.utils.initializePreferences
import org.openmw.utils.readModValues
import org.openmw.utils.updateResolutionInConfig

@InternalCoroutinesApi
@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private lateinit var configFileObserver: ConfigFileObserver
    private lateinit var fileObserver: MultiPathFileObserver

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @OptIn(DelicateCoroutinesApi::class)
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler(CaptureCrash())

        lifecycleScope.launch {
            val permissionGranted = getManageExternalStoragePermission(this@MainActivity)
            if (permissionGranted) {
                proceedWithNextSteps()
            }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @OptIn(ExperimentalFoundationApi::class)
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        PermissionHelper.handlePermissionResult(requestCode, intArrayOf(resultCode)) { granted ->
            if (granted) {
                proceedWithNextSteps()
            }
        }
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @OptIn(DelicateCoroutinesApi::class, ExperimentalFoundationApi::class)
    private fun proceedWithNextSteps() {
        lifecycleScope.launch {
            initializePreferences(this@MainActivity)

            firstLaunch = true
            withContext(Dispatchers.Default) {
                UserManageAssets(applicationContext).onFirstLaunch()
            }
            firstLaunch = false

            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val (width, height) = windowManager.currentDeviceRealSize()

            hideSystemBars(this@MainActivity)

            setContent {
                startObservingCodeGroup(this@MainActivity, uiScope)
                val configFilePath = Constants.SETTINGS_FILE
                configFileObserver = ConfigFileObserver(configFilePath)
                configFileObserver.startWatching()

                val pathsToWatch = listOf(
                    Constants.USER_FILE_STORAGE,
                    filesDir.absolutePath,
                    filesDir.parentFile.absolutePath,
                    Constants.SECOND_USER_FILE_STORAGE,
                    applicationInfo.nativeLibraryDir
                )
                fileObserver = MultiPathFileObserver(pathsToWatch)
                fileObserver.startWatching()

                var showDialog = remember { mutableStateOf(true) }
                val modValues = readModValues()
                val avoidInsertion by GameFilesPreferences.readResolutionInsertion(this@MainActivity).collectAsState(initial = false)
                val whatsNew by GameFilesPreferences.getWhatsNew(this@MainActivity).collectAsState(initial = false)

                if (!avoidInsertion) {
                    updateResolutionInConfig(width, height)
                }
                OpenMWTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (whatsNew) {
                            MyAlertDialog(showDialog = showDialog)
                        }
                        App(applicationContext, modValues)
                        if (UIStateManager.isAppLoggingEnabled) {
                            LogsBox(logs = LogRepository.logs, fontSize = 10f, boxWidth = 300f, boxHeight = 300f)
                        }
                    }
                }
            }
        }
    }

    public override fun onDestroy() {
        finish()
        configFileObserver.stopWatching()
        fileObserver.stopWatching()
        uiScope.cancel()
        super.onDestroy()
        Process.killProcess(Process.myPid())
    }
}

fun isControllerConnected(context: Context): Boolean {
    val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
    val deviceIds = inputManager.inputDeviceIds
    for (id in deviceIds) {
        val device = inputManager.getInputDevice(id)
        if (device?.sources?.and(SOURCE_GAMEPAD) == SOURCE_GAMEPAD) {
            return true
        }
    }
    return false
}

fun startObservingCodeGroup(context: Context, scope: CoroutineScope) {
    scope.launch {
        readCodeGroup(context).collect { codeGroup ->
            UIStateManager.tempCodeGroup = codeGroup
        }
    }
}

@Composable
fun BackgroundAnimation() {
    var selectedBackgroundAnimation by remember { mutableStateOf("BouncingBackground") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                GameFilesPreferences.getBackgroundAnimation(context)?.let { option ->
                    selectedBackgroundAnimation = option
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    when (selectedBackgroundAnimation) {
        "BouncingBackground" -> BouncingBackground()
        "RotatingImageBackground" -> RotatingImageBackground()
        "CircularBackground" -> CircularBackground()
        else -> NoneBackground()
    }
}

@InternalCoroutinesApi
@DelicateCoroutinesApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun App(context: Context, modValues: List<ModValue>) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Setting.route) {
            SettingScreen(context) {
                navController.navigate(Screen.Home.route)
            }
        }
        composable(Screen.Home.route) {
            HomeScreen(context, modValues) {
                navController.navigate(Screen.Setting.route)
            }
        }
    }
}

private object Route {
    const val SETTINGS = "setting"
    const val HOME = "home"
}

sealed class Screen(val route: String) {
    object Setting: Screen(Route.SETTINGS)
    object Home: Screen(Route.HOME)
}

