package org.openmw

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.openmw.ui.theme.OpenMWTheme
import org.openmw.utils.CaptureCrash
import org.openmw.utils.ModValue
import org.openmw.utils.PermissionHelper
import org.openmw.utils.UserManageAssets
import org.openmw.utils.getScreenWidthAndHeight
import org.openmw.utils.readModValues
import org.openmw.utils.updateResolutionInConfig
import java.io.File

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_files_prefs")

object GameFilesPreferences {
    val GAME_FILES_URI_KEY = stringPreferencesKey("game_files_uri")
}

@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {

    @OptIn(DelicateCoroutinesApi::class)
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        PermissionHelper.getManageExternalStoragePermission(this@MainActivity)
        Thread.setDefaultUncaughtExceptionHandler(CaptureCrash())

        // Force the app to wait until this part is finished.
        runBlocking {
            launch(Dispatchers.IO) {

                val configDir = File(filesDir, "config")
                if (!configDir.exists()) {
                    configDir.mkdirs()
                }

                UserManageAssets(applicationContext).onFirstLaunch()
            }.join()
        }

        val (width, height) = getScreenWidthAndHeight(applicationContext)
        updateResolutionInConfig(width, height)
        val modValues = readModValues()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Hide the system bars
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            OpenMWTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App(applicationContext, modValues)
                }
            }
        }
    }
}

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
