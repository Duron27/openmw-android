@file:OptIn(InternalCoroutinesApi::class)

package org.openmw

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import org.openmw.navigation.MyFloatingActionButton
import org.openmw.navigation.MyTopBar
import org.openmw.ui.controls.UIStateManager
import org.openmw.ui.controls.UIStateManager.configureControls
import org.openmw.ui.controls.UIStateManager.customColor
import org.openmw.ui.controls.UIStateManager.launchedActivity
import org.openmw.ui.controls.UIStateManager.transparentBlack
import org.openmw.ui.overlay.AnimationSettings
import org.openmw.ui.theme.gradientColors
import org.openmw.utils.BouncingBackground
import org.openmw.utils.CircularBackground
import org.openmw.utils.CommandLineInputScreen
import org.openmw.utils.ExportDialog
import org.openmw.utils.FileBrowserMode
import org.openmw.utils.FileBrowserPopup
import org.openmw.utils.GameFilesPreferences
import org.openmw.utils.GameFilesPreferences.clearCustomGLDriverPath
import org.openmw.utils.GameFilesPreferences.getBackgroundAnimationFlow
import org.openmw.utils.GameFilesPreferences.getCustomGLDriverPath
import org.openmw.utils.GameFilesPreferences.loadAutoMouseMode
import org.openmw.utils.GameFilesPreferences.readCodeGroup
import org.openmw.utils.GameFilesPreferences.readTextureShrinkingOption
import org.openmw.utils.GameFilesPreferences.saveAutoMouseMode
import org.openmw.utils.GameFilesPreferences.saveIconGlow
import org.openmw.utils.GameFilesPreferences.setCustomGLDriverPath
import org.openmw.utils.ImportDialog
import org.openmw.utils.InitialDirectorySelection
import org.openmw.utils.KeyBindings
import org.openmw.utils.NoneBackground
import org.openmw.utils.ReadAndDisplayIniValues
import org.openmw.utils.RotatingImageBackground
import org.openmw.utils.UserManageAssets
import org.openmw.utils.exportCrashAndLogcatFiles
import org.openmw.utils.importSpecificFile
import java.io.File

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SettingScreen(context: Context, navigateToHome: () -> Unit) {
    val selectedBackgroundAnimation by getBackgroundAnimationFlow(context).collectAsState(initial = "BouncingBackground")
    val newFeatureEnabledChecked by GameFilesPreferences.loadNewFeatureEnabledState(context).collectAsState(initial = false)
    val orientation = LocalConfiguration.current.orientation
    val codeGroupOption by readCodeGroup(context).collectAsState(initial = "OpenMW")

    Scaffold(
        topBar = {
            MyTopBar(context)
        },
        content = @Composable {
            when (selectedBackgroundAnimation) {
                "BouncingBackground" -> BouncingBackground()
                "RotatingImageBackground" -> RotatingImageBackground()
                "CircularBackground" -> CircularBackground()
                else -> NoneBackground()
            }
            Box(
                modifier = Modifier
                    .background(customColor)
                    .padding(top = 65.dp, bottom = 45.dp),
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 40.dp)
                ) {
                    if (codeGroupOption == "OpenMW" && orientation != Configuration.ORIENTATION_LANDSCAPE) {
                        ToggleFeatureSwitch(context)
                        ReadAndDisplayIniValues()
                        ControlsMenu()
                        ImportAndExport()
                        InspectLogs()
                        if (newFeatureEnabledChecked) {
                            DeveloperMenu()
                        }
                    } else if (codeGroupOption == "OpenMW") {
                        navigateToHome()
                    }
                    else if (codeGroupOption == "UQM") {
                        UQM()
                        ToggleFeatureSwitch(context)
                        ControlsMenu()
                        if (newFeatureEnabledChecked) {
                            DeveloperMenu()
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
                BottomAppBar(
                    containerColor = transparentBlack,
                    actions = {
                        Button(
                            onClick = { navigateToHome() },
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color.Transparent),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Settings",
                                modifier = Modifier.size(80.dp),
                                tint = Color.White
                            )
                        }
                        CommandLineInputScreen(context)
                    },
                    floatingActionButton = {
                        MyFloatingActionButton()
                    }
                )
            }
        }
    )
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun InspectLogs() {
    var isColumnExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black)
            .background(color = customColor)
            .clickable { isColumnExpanded = !isColumnExpanded },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.horizontalGradient(colors = gradientColors))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Inspect Logs",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White
            )
            Icon(
                imageVector = if (isColumnExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isColumnExpanded) "Collapse" else "Expand"
            )
        }
        if (isColumnExpanded) {
            Logs()
        }
    }
}

@Composable
fun Logs() {
    var showDialogOpenMW by remember { mutableStateOf(false) }
    val logContent = remember { mutableStateOf("") }
    var showDialogLC by remember { mutableStateOf(false) }
    val logContentLC = remember { mutableStateOf("") }
    var showDialogCrash by remember { mutableStateOf(false) }
    val logContentCrash = remember { mutableStateOf("") }
    val context = LocalContext.current
    Button(
        onClick = {
            showDialogLC = true
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        )
    ) {
        Text("Open Logcat Log", color = Color.White)
    }
    HorizontalDivider(color = Color.White, thickness = 1.dp)
    Button(
        onClick = {
            showDialogOpenMW = true
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        )
    ) {
        Text("Open OpenMW Log", color = Color.White)
    }
    HorizontalDivider(color = Color.White, thickness = 1.dp)
    Button(
        onClick = {
            showDialogCrash = true
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        )
    ) {
        Text("Open Crash Log", color = Color.White)
    }

    if (showDialogCrash) {
        logContentCrash.value = readLogFile(Constants.CRASH_FILE)
        AlertDialog(
            onDismissRequest = { showDialogCrash = false },
            title = { Text("Crash Log") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Text(
                        text = logContentCrash.value,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Row {
                    Button(onClick = { showDialogCrash = false }) {
                        Text("OK")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            copyToClipboard(context, logContentCrash.value)
                        }) {
                        Text("Copy")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        clearLogFile(context)
                        showDialogCrash = false
                    }) {
                        Text("Clear Log")
                    }
                }
            }
        )
    }

    if (showDialogOpenMW) {
        logContent.value = readLogFile(Constants.OPENMW_LOG)
        AlertDialog(
            onDismissRequest = { showDialogOpenMW = false },
            title = { Text("OpenMW Log") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Text(
                        text = logContent.value,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Row {
                    Button(onClick = { showDialogOpenMW = false }) {
                        Text("OK")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        copyToClipboard(context, logContent.value)
                    }) {
                        Text("Copy")
                    }
                }
            }
        )
    }

    if (showDialogLC) {
        logContentLC.value = readLogFile(Constants.LOGCAT_FILE)
        AlertDialog(
            onDismissRequest = { showDialogLC = false },
            title = { Text("Logcat Log") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Text(
                        text = logContentLC.value,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Row {
                    Button(
                        onClick = { showDialogLC = false }
                    ) {
                        Text("OK")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        copyToClipboard(context, logContentLC.value)
                    }) {
                        Text("Copy")
                    }
                }
            }
        )
    }
}

fun clearLogFile(context: Context) {
    val logFile = File(Constants.CRASH_FILE)
    if (logFile.exists()) {
        logFile.delete()
        Toast.makeText(context, "Log file deleted", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "Log file does not exist", Toast.LENGTH_SHORT).show()
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Log Content", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

fun readLogFile(logFilePath: String): String {
    return try {
        File(logFilePath).readText()
    } catch (e: Exception) {
        "Failed to read log file: ${e.message}"
    }
}


@InternalCoroutinesApi
@ExperimentalMaterial3Api
@Composable
fun DeveloperMenu() {
    var isColumnExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black)
            .background(color = customColor)
            .clickable { isColumnExpanded = !isColumnExpanded },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.horizontalGradient(colors = gradientColors))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Developer Tools",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White
            )
            Icon(
                imageVector = if (isColumnExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isColumnExpanded) "Collapse" else "Expand"
            )
        }
        if (isColumnExpanded) {
            DevInsert()
        }
    }
}

@Composable
fun DevInsert() {
    var showPopup by remember { mutableStateOf(false) }
    var showFileBrowser by remember { mutableStateOf(false) }
    var showFileBrowserGL by remember { mutableStateOf(false) }
    var selectedInitialDirectory by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bypassGameCheck by GameFilesPreferences.loadBypassGameCheck(context).collectAsState(initial = false)
    val customGlDriverPath by getCustomGLDriverPath(context).collectAsState(initial = "None")
    val avoidInsertion by GameFilesPreferences.readResolutionInsertion(context).collectAsState(initial = false)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "App Logging",
                    color = Color.White
                )
                Text(
                    text = "This feature will enable a custom logging system in the UI",
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                )
            }

            Switch(
                checked = UIStateManager.isAppLoggingEnabled,
                onCheckedChange = { checked ->
                    UIStateManager.isAppLoggingEnabled = checked
                }
            )
        }
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Don't inject resolution settings automatically",
                    color = Color.White
                )
                Text(
                    text = "This enables / disables the launcher overwriting resolution settings in settings.cfg on launch",
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                )
            }

            Switch(
                checked = avoidInsertion,
                onCheckedChange = { checked ->
                    scope.launch {
                        GameFilesPreferences.writeResolutionInsertion(context, !avoidInsertion)
                    }
                }
            )
        }
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Bypass the game files check for the launcher button",
                    color = Color.White
                )
                Text(
                    text = "This enables / disables the launcher files check on game launch. Odd bug with Andfroid 15?",
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                )
            }

            Switch(
                checked = bypassGameCheck,
                onCheckedChange = { checked ->
                    scope.launch {
                        GameFilesPreferences.saveBypassGameCheck(context, !bypassGameCheck)
                    }
                }
            )
        }
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        Button(
            onClick = { showPopup = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            )
        ) {
            Text("Show DataStore Content", color = Color.White)
        }
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        Button(
            onClick = {
                selectedInitialDirectory = null
                showFileBrowser = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            )
        ) {
            Text("Open File Browser", color = Color.White)
        }
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        // Custom GL Driver Selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (customGlDriverPath != null) {
                        val displayPath = if (customGlDriverPath!!.length > 30) {
                            customGlDriverPath!!.take(10) + "..." + customGlDriverPath!!.takeLast(10)
                        } else {
                            customGlDriverPath!!
                        }
                        displayPath
                    } else {
                        "Select Custom GL Driver"
                    },
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Button(onClick = {
                Toast.makeText(context, "This may be broken, Clear driver to make work again", Toast.LENGTH_SHORT).show()
                if (customGlDriverPath != null) {
                    // Clear the custom GL driver
                    scope.launch {
                        clearCustomGLDriverPath(context)
                    }
                } else {
                    // Show the file browser popup
                    showFileBrowserGL = true
                }
            }) {
                Text(if (customGlDriverPath != null) "Clear Driver" else "Select Driver")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Test Virtual Keyboard",
                    color = Color.White
                )
                Text(
                    text = "Test Virtual Keyboard.",
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                )
            }

            Switch(
                checked = UIStateManager.VirtualKB,
                onCheckedChange = { checked ->
                    UIStateManager.VirtualKB = checked
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Test Virtual Left Joystick",
                    color = Color.White
                )
                Text(
                    text = "Test Virtual Left Joystick.",
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                )
            }

            Switch(
                checked = UIStateManager.LeftJoy,
                onCheckedChange = { checked ->
                    UIStateManager.LeftJoy = checked
                }
            )
        }
    }
    // File Browser Popup
    if (showFileBrowserGL) {
        FileBrowserPopup(
            initialDirectory = File(Environment.getExternalStorageDirectory().toString()),
            onDismiss = { showFileBrowserGL = false },
            onFileSelected = { file ->
                showFileBrowserGL = false
                scope.launch {
                    setCustomGLDriverPath(context, customGlDriverPath!!)
                }
            },
            mode = FileBrowserMode.FILE
        )
    }


    if (showPopup) {
        DataStoreContentPopup(
            context = context,
            onDismiss = { showPopup = false })
    }

    if (showFileBrowser && selectedInitialDirectory == null) {
        InitialDirectorySelection(
            onSelect = { directory ->
                selectedInitialDirectory = directory
                showFileBrowser = true
            },
            onDismiss = { showFileBrowser = false }
        )
    }

    if (showFileBrowser && selectedInitialDirectory != null) {
        FileBrowserPopup(
            initialDirectory = selectedInitialDirectory!!,
            onDismiss = { showFileBrowser = false }

        )
    }
}
@SuppressLint("MutableCollectionMutableState")
@Composable
fun DataStoreContentPopup(context: Context, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var dataStoreContent by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        scope.launch {
            dataStoreContent = GameFilesPreferences.getAllPreferences(context)
        }
    }

    var updatedDataStoreContent by remember { mutableStateOf(dataStoreContent.toMutableMap()) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
                title = { Text("DataStore Content") },
                text = {
                    Column (
                        modifier = Modifier
                        .fillMaxHeight(0.6f) // Adjust the height as needed
                        .verticalScroll(rememberScrollState())
                    ) {
                        dataStoreContent.forEach { (key, value) ->
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = key)
                            var textFieldValue by remember { mutableStateOf(value) }
                            TextField(
                                value = textFieldValue,
                                onValueChange = { newValue ->
                                    textFieldValue = newValue
                                    updatedDataStoreContent[key] = newValue
                                },
                                modifier = Modifier.fillMaxWidth().wrapContentHeight()
                            )
                            HorizontalDivider(
                                color = Color.Gray,
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            updatedDataStoreContent.forEach { (key, value) ->
                                when (key) {
                                    GameFilesPreferences.GAME_FILES_URI_KEY.name -> GameFilesPreferences.storeGameFilesPath(context, value)
                                    GameFilesPreferences.UI_HIDDEN_STATE_KEY.name -> GameFilesPreferences.saveUIState(context, value.toBoolean())
                                    GameFilesPreferences.MATCH_ICON_COLOR_KEY.name -> GameFilesPreferences.saveMatchIconColorState(context, value.toBoolean())
                                    GameFilesPreferences.RESOLUTION_X_KEY.name -> GameFilesPreferences.saveResolutionX(context, value.toInt())
                                    GameFilesPreferences.RESOLUTION_Y_KEY.name -> GameFilesPreferences.saveResolutionY(context, value.toInt())
                                    GameFilesPreferences.ICON_GLOW_KEY.name -> saveIconGlow(context, value.toBoolean())
                                    GameFilesPreferences.COMMAND_LINE_KEY.name -> GameFilesPreferences.saveCommandLine(context, value)
                                    GameFilesPreferences.USER_OPTIONS_KEY.name -> GameFilesPreferences.saveUserOptions(context, value.split(",").toSet())
                                    GameFilesPreferences.AUTO_MOUSE_MODE_KEY.name -> saveAutoMouseMode(context, value)
                                    GameFilesPreferences.AVOID_16_BITS_KEY.name -> GameFilesPreferences.writeAvoid16Bits(context, value.toBoolean())
                                    GameFilesPreferences.TEXTURE_SHRINKING_KEY.name -> GameFilesPreferences.writeTextureShrinkingOption(context, value)
                                    GameFilesPreferences.CUSTOM_GL_DRIVER_KEY.name -> setCustomGLDriverPath(context, value)
                                    GameFilesPreferences.SELECTED_MOUSE_KEYS.name -> GameFilesPreferences.setSelectedKeycodes(context, value)
                                    GameFilesPreferences.ALLOWED_TO_TEXT_EDITOR.name -> GameFilesPreferences.setExtensionAllowedToEdit(context, value)
                                    GameFilesPreferences.NEW_FEATURE_ENABLED_KEY.name -> GameFilesPreferences.saveUIState(context, value.toBoolean())
                                    GameFilesPreferences.WHATS_NEW_KEY.name -> GameFilesPreferences.setWhatsNew(context, value.toBoolean())
                                    GameFilesPreferences.TUTORIAL_KEY.name -> GameFilesPreferences.setTutorial(context, value.toBoolean())
                                }
                            }
                            onDismiss()
                        }
                    }, modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = customColor
                        )) {
                        Text("Save", color = Color.White)
                    }
                },
                dismissButton = {
                    Button(onClick = { onDismiss() }, colors = ButtonDefaults.buttonColors(Color.Transparent)) {
                        Text("Cancel", color = Color.White)
                    }
                }
    )
}

@Composable
fun ToggleFeatureSwitch(context: Context) {
    var isColumnExpanded by remember { mutableStateOf(false) }
    if (launchedActivity) {
        isColumnExpanded = true
    }

    Column(
        modifier = Modifier
            .border(1.dp, Color.Black)
            .background(color = customColor)
            .verticalScroll(rememberScrollState())
            .clickable { isColumnExpanded = !isColumnExpanded },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!launchedActivity) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.horizontalGradient(colors = gradientColors))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Launcher Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White
            )
            Icon(
                imageVector = if (isColumnExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isColumnExpanded) "Collapse" else "Expand"
            )
        }
        }
        if (isColumnExpanded) {
            FeaturesSwitches(context)
        }
    }
}

@Composable
fun FeaturesSwitches(context: Context) {
    val scope = rememberCoroutineScope()
    val isUIHidden by GameFilesPreferences.loadUIState(context).collectAsState(initial = false)
    val isVibrationOn by GameFilesPreferences.loadVibrationState(context).collectAsState(initial = true)
    val matchIconColorChecked by GameFilesPreferences.loadMatchIconColorState(context).collectAsState(initial = false)
    val avoid16BitsChecked by GameFilesPreferences.readAvoid16Bits(context).collectAsState(initial = false)
    val buttonGroupSwitch by GameFilesPreferences.getButtonGroupSwitch(context).collectAsState(initial = true)
    val iconGlowChecked by GameFilesPreferences.loadIconGlow(context).collectAsState(initial = true)
    var showDialog by remember { mutableStateOf(false) }
    val controllerConnected = isControllerConnected(context)
    val newFeatureEnabledChecked by GameFilesPreferences.loadNewFeatureEnabledState(context).collectAsState(initial = false)
    Column {
        if (!launchedActivity) {
            OpenMW()
        }
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        if (launchedActivity) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Display Memory Info",
                        color = Color.White
                    )
                    Text(
                        text = "Create a window showing Memory Info.",
                        color = Color.White,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    )
                }

                Switch(
                    checked = UIStateManager.isMemoryInfoEnabled,
                    onCheckedChange = { checked ->
                        UIStateManager.isMemoryInfoEnabled = checked
                    }
                )
            }
            HorizontalDivider(color = Color.White, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Display Battery Info",
                        color = Color.White
                    )
                    Text(
                        text = "Create a window showing Battery Info.",
                        color = Color.White,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    )
                }

                Switch(
                    checked = UIStateManager.isBatteryStatusEnabled,
                    onCheckedChange = { checked ->
                        UIStateManager.isBatteryStatusEnabled = checked
                    }
                )
            }
            HorizontalDivider(color = Color.White, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Logcat",
                        color = Color.White
                    )
                    Text(
                        text = "Create a window showing Logcat Info.",
                        color = Color.White,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    )
                }

                Switch(
                    checked = UIStateManager.isLoggingEnabled,
                    onCheckedChange = { checked ->
                        UIStateManager.isLoggingEnabled = checked
                    }
                )
            }
            HorizontalDivider(color = Color.White, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Custom Log Output",
                        color = Color.White
                    )
                    Text(
                        text = "Create a window showing Custom Log Output.",
                        color = Color.White,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    )
                }

                Switch(
                    checked = UIStateManager.isAppLoggingEnabled,
                    onCheckedChange = { checked ->
                        UIStateManager.isAppLoggingEnabled = checked
                    }
                )
            }
            HorizontalDivider(color = Color.White, thickness = 1.dp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Column to stack the two Text components on the left
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f) // This ensures the Column takes up available space on the left
            ) {
                Text(
                    text = if (controllerConnected || !isUIHidden) {
                        "UI is Visible"
                    } else {
                        "UI is Hidden"
                    },
                    color = Color.White
                )
                Text(
                    text = "This feature will set the UI visible state at system start",
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                )
            }
            Switch(
                checked = isUIHidden,
                onCheckedChange = { checked ->
                    scope.launch {
                        GameFilesPreferences.saveUIState(context, !isUIHidden)
                    }
                }
            )
        }
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Column to stack the two Text components on the left
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (!isVibrationOn) "Vibration Disabled" else "Vibration Enabled",
                    color = Color.White
                )
                Text(
                    text = "This feature will set the Vibration state at system start",
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                )
            }
            Switch(
                checked = isVibrationOn,
                onCheckedChange = { checked ->
                    scope.launch {
                        GameFilesPreferences.saveVibrationState(context, !isVibrationOn)
                    }
                }
            )
        }
        if (!launchedActivity) {
            HorizontalDivider(color = Color.White, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Avoid 16 Bits",
                        color = Color.White
                    )
                    Text(
                        text = "This feature helps with devices that have less memory",
                        color = Color.White,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    )
                }

                Switch(
                    checked = avoid16BitsChecked,
                    onCheckedChange = { checked ->
                        scope.launch {
                            GameFilesPreferences.writeAvoid16Bits(context, !avoid16BitsChecked)
                        }
                    }
                )
            }
            HorizontalDivider(color = Color.White, thickness = 1.dp)
            if (newFeatureEnabledChecked) {
                CodeGroupOptionSelector()
            }
            HorizontalDivider(color = Color.White, thickness = 1.dp)
            BackgroundAnimationOptionSelector()
            HorizontalDivider(color = Color.White, thickness = 1.dp)
            TextureShrinkingOptionSelector()
            HorizontalDivider(color = Color.White, thickness = 1.dp)

            CharsetDropdownMenu { newEncoding ->
                updateCharset(newEncoding)
            }
            HorizontalDivider(color = Color.White, thickness = 1.dp)
        }
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        AnimationSettings(context, scope)
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Match Icon Color to Thumbstick",
                    color = Color.White
                )
                Text(
                    text = "This feature makes the thumbstick share its color with all the icons in the top left of the UI in game.",
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                )
            }

            Switch(
                checked = matchIconColorChecked,
                onCheckedChange = { checked ->
                    scope.launch {
                        GameFilesPreferences.saveMatchIconColorState(context, !matchIconColorChecked)
                    }
                }
            )
        }

        HorizontalDivider(color = Color.White, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Icon Glow",
                    color = Color.White
                )
                Text(
                    text = "This feature adds a glow to all the icons in the top left of the UI in game.",
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                )
            }

            Switch(
                checked = iconGlowChecked,
                onCheckedChange = { checked ->
                    scope.launch {
                        saveIconGlow(context, !iconGlowChecked)
                    }
                }
            )
        }
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        AutoMouseModeOptionSelector()
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Allow Button Groups",
                    color = Color.White
                )
                Text(
                    text = "This feature adds a second UI that you can place buttons in, hiding them from the main controls screen.",
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                )
            }

            Switch(
                checked = buttonGroupSwitch,
                onCheckedChange = { checked ->
                    scope.launch {
                        GameFilesPreferences.setButtonGroupSwitch(context, !buttonGroupSwitch)
                    }
                }
            )
        }
        // Conditionally display the "Exit to Launcher" button at the bottom center
        if (configureControls) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Button(
                    onClick = {
                        launchedActivity = false
                        configureControls = false
                        (context as? Activity)?.finish()
                    }
                ) {
                    Text("Return to Launcher")
                }
            }
        }
        if (!launchedActivity) {
            HorizontalDivider(color = Color.White, thickness = 1.dp)
            // Add the new feature enabled switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Enable Developer Options",
                        color = Color.White
                    )
                    Text(
                        text = "This enables features not ready for main use, or too advanced for beginners.",
                        color = Color.White,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    )
                }

                Switch(
                    checked = newFeatureEnabledChecked,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showDialog = true
                        } else {
                            scope.launch {
                                GameFilesPreferences.saveNewFeatureEnabledState(context, false)
                            }
                        }
                    }
                )
            }

            // Alert Dialog
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showDialog = false
                    },
                    title = {
                        Text(text = "Warning")
                    },
                    text = {
                        Text("Are you sure you want to enable this feature?\n\nThis will open up features I'm working on that could\nbreak things. Only use if you know what you are doing\nor don't mind reinstalling.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    GameFilesPreferences.saveNewFeatureEnabledState(context, true)
                                }
                                showDialog = false
                            }
                        ) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                showDialog = false
                            }
                        ) {
                            Text("No")
                        }
                    }
                )
            }
        }
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun TextureShrinkingOptionSelector() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val textureShrinkingOption by readTextureShrinkingOption(context).collectAsState(initial = "None")
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
            .fillMaxWidth()
            .background(color = customColor)
            .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Texture Shrinking Option", color = Color.White)
            Box(
                modifier = Modifier
                .clickable { expanded = true }
            ) {
                Text(textureShrinkingOption, modifier = Modifier.padding(8.dp), color = Color.White)
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .border(1.dp, Color.Black)
                ) {
                    DropdownMenuItem(
                        text = { Text("None", color = Color.White) },
                                     onClick = {
                                         scope.launch {
                                             try {
                                                 GameFilesPreferences.writeTextureShrinkingOption(context, "None")
                                             } catch (e: Exception) {
                                                 e.printStackTrace()
                                             }
                                         }
                                         expanded = false
                                     })
                    DropdownMenuItem(
                        text = { Text("Low", color = Color.White) },
                                     onClick = {
                                         scope.launch {
                                             try {
                                                 GameFilesPreferences.writeTextureShrinkingOption(context, "low")
                                             } catch (e: Exception) {
                                                 e.printStackTrace()
                                             }
                                         }
                                         expanded = false
                                     })
                    DropdownMenuItem(
                        text = { Text("Medium", color = Color.White) },
                                     onClick = {
                                         scope.launch {
                                             try {
                                                 GameFilesPreferences.writeTextureShrinkingOption(context, "medium")
                                             } catch (e: Exception) {
                                                 e.printStackTrace()
                                             }
                                         }
                                         expanded = false
                                     })
                    DropdownMenuItem(
                        text = { Text("High", color = Color.White) },
                        onClick = {
                                scope.launch {
                                    try {
                                        GameFilesPreferences.writeTextureShrinkingOption(context, "high")
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                expanded = false
                        })
                }
            }
        }
    }
}

@Composable
fun BackgroundAnimationOptionSelector() {
    val context = LocalContext.current
    val backgroundAnimation by getBackgroundAnimationFlow(context).collectAsState(initial = "BouncingBackground")
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = customColor)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Background Animations", color = Color.White)
            Box(
                modifier = Modifier
                    .clickable { expanded = true }
            ) {
                Text(backgroundAnimation, modifier = Modifier.padding(8.dp), color = Color.White)
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .border(1.dp, Color.Black)
                ) {
                    DropdownMenuItem(
                        text = { Text("None", color = Color.White) },
                        onClick = {
                            scope.launch {
                                try {
                                    GameFilesPreferences.setBackgroundAnimation(context, "None")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            expanded = false
                        })
                    DropdownMenuItem(
                        text = { Text("Bouncing Background", color = Color.White) },
                        onClick = {
                            scope.launch {
                                try {
                                    GameFilesPreferences.setBackgroundAnimation(context, "BouncingBackground")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            expanded = false
                        })
                    DropdownMenuItem(
                        text = { Text("Rotating Image Background", color = Color.White) },
                        onClick = {
                            scope.launch {
                                try {
                                    GameFilesPreferences.setBackgroundAnimation(context, "RotatingImageBackground")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            expanded = false
                        })
                    DropdownMenuItem(
                        text = { Text("Circular Background", color = Color.White) },
                        onClick = {
                            scope.launch {
                                try {
                                    GameFilesPreferences.setBackgroundAnimation(context, "CircularBackground")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            expanded = false
                        })
                }
            }
        }
    }
}

@Composable
fun CodeGroupOptionSelector() {
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val codeGroupOption by readCodeGroup(context).collectAsState(initial = "OpenMW")

    Column {
        Row(
            modifier = Modifier
            .fillMaxWidth()
            .background(color = customColor)
            .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Select Game", color = Color.White)
            Box(
                modifier = Modifier
                .clickable { expanded = true }
            ) {
                Text(codeGroupOption, modifier = Modifier.padding(8.dp), color = Color.White)
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .border(1.dp, Color.Black)
                ) {
                    DropdownMenuItem(
                        text = { Text("OpenMW", color = Color.White) },
                        onClick = {
                            scope.launch {
                                try {
                                    GameFilesPreferences.setCodeGroup(context, "OpenMW")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        expanded = false
                        })
                    DropdownMenuItem(
                        text = { Text("UQM", color = Color.White) },
                        onClick = {
                            scope.launch {
                                try {
                                    GameFilesPreferences.setCodeGroup(context, "UQM")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            expanded = false
                        })
                }
            }
        }
    }
}

@Composable
fun AutoMouseModeOptionSelector() {
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val autoMouseMode by loadAutoMouseMode(context).collectAsState(initial = "Hybrid")

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = customColor)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f).background(color = customColor)
            ) {
                Text(
                    text = "Mouse Mode",
                    color = Color.White
                )
                Text(
                    text = "\n\nNone: no mouse cursor, you just click with your fingers. (the UI will be hidden)\n\nHybrid: like Auto except the UI will be visable",
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                )
            }
            Box(
                modifier = Modifier
                    .clickable { expanded = true }
            ) {
                Text(autoMouseMode, modifier = Modifier.padding(8.dp), color = Color.White)
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .border(1.dp, Color.Black).background(color = customColor)
                ) {
                    DropdownMenuItem(
                        text = { Text("None", color = Color.White) },
                        onClick = {
                            scope.launch {
                                try {
                                    saveAutoMouseMode(context, "None")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            expanded = false
                        })
                    DropdownMenuItem(
                        text = { Text("Hybrid", color = Color.White) },
                        onClick = {
                            scope.launch {
                                try {
                                    saveAutoMouseMode(context, "Hybrid")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            expanded = false
                        })
                }
            }
        }
    }
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun ImportAndExport() {
    var isColumnExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black)
            .background(color = customColor)
            .clickable { isColumnExpanded = !isColumnExpanded },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.horizontalGradient(colors = gradientColors))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Import / Export",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White
            )
            Icon(
                imageVector = if (isColumnExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isColumnExpanded) "Collapse" else "Expand"
            )
        }
        if (isColumnExpanded) {
            ImportExportInsert()
        }
    }
}

@Composable
fun ImportExportInsert() {
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    Button(
        onClick = { exportCrashAndLogcatFiles(context) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        )
    ) {
        Text(text = "Export all logs", color = Color.White)
    }
    HorizontalDivider(color = Color.White, thickness = 1.dp)
    Button(
        onClick = { importSpecificFile(context, """.*\.omwsave$""") },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        )
    )
    {
        Text(text = "Import save game", color = Color.White)
    }
    HorizontalDivider(color = Color.White, thickness = 1.dp)
    Button(
        onClick = { showExportDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        )
    ) {
        Text(text = "Backup files and directories", color = Color.White)
    }
    HorizontalDivider(color = Color.White, thickness = 1.dp)
    Button(
        onClick = { showImportDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        )
    ) {
        Text(text = "Restore files and directories", color = Color.White)
    }
    HorizontalDivider(color = Color.White, thickness = 1.dp)
    Button(
        onClick = { importSpecificFile(context, "settings.cfg") },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        )
    )
    {
        Text(text = "Import settings.cfg", color = Color.White)
    }

    if (showExportDialog) {
        ExportDialog(context = context, onDismiss = { showExportDialog = false })
    }
    if (showImportDialog) {
        ImportDialog(context = context, onDismiss = { showImportDialog = false })
    }
}

@Composable
fun CharsetDropdownMenu(updateCharset: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val charsets = listOf("utf-8", "utf-16", "us-ascii", "iso-8859-1", "shift_jis", "euc-jp", "win1250", "win1251", "windows-1252")
    var selectedCharset by remember { mutableStateOf(readCurrentCharset()) }
    var feedbackMessage by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
        .padding(16.dp)
        .background(color = customColor)
        .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Select Charset", modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = { expanded = true }) {
            Text(selectedCharset)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .border(1.dp, Color.Black)
        ) {
            charsets.forEach { charset ->
                DropdownMenuItem(
                    text = { Text(charset, color = Color.White) },
                    onClick = {
                        selectedCharset = charset
                        expanded = false
                        updateCharset(charset)
                        feedbackMessage = "Charset changed to $charset"
                    }
                )
            }
        }
    }
    if (feedbackMessage.isNotEmpty()) {
        Text(feedbackMessage, color = Color.Green, modifier = Modifier.padding(8.dp))
    }
}

fun readCurrentCharset(): String {
    val configFilePath = Constants.OPENMW_CFG
    val file = File(configFilePath)
    var encodingLine = file.readLines().find { it.startsWith("encoding=") }
    if (encodingLine == null) {
        encodingLine = "encoding=win1252"
        file.appendText("\n$encodingLine")
    }
    return encodingLine.split("=")[1]
}

fun updateCharset(newEncoding: String) {
    val configFilePath = Constants.OPENMW_CFG
    val file = File(configFilePath)
    val updatedLines = file.readLines().map { line ->
        if (line.startsWith("encoding=")) {
            "encoding=$newEncoding"
        } else {
            line
        }
    }
    file.writeText(updatedLines.joinToString("\n"))
    println("Charset changed to $newEncoding")
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@Composable
fun ControlsMenu() {
    var isColumnExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black)
            .background(color = customColor)
            .clickable { isColumnExpanded = !isColumnExpanded },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.horizontalGradient(colors = gradientColors))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Customize Controls",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White
            )
            Icon(
                imageVector = if (isColumnExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isColumnExpanded) "Collapse" else "Expand"
            )
        }
        if (isColumnExpanded) {
            ControlsInsert()
        }
    }
}

@Composable
fun ControlsInsert() {
    val context = LocalContext.current
    var showKeyBindings by remember { mutableStateOf(false) }
    var showDialog2 by remember { mutableStateOf(false) }
    HorizontalDivider(color = Color.White, thickness = 1.dp)
    Button(
        onClick = {
            configureControls = true
            val intent = Intent(context, EngineActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        )
    ) {
        Text("Configure Controls", color = Color.White)
    }
    HorizontalDivider(color = Color.White, thickness = 1.dp)
    Button(
        onClick = {
            showDialog2 = true
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        )
    ) {
        Text("Reset Controls", color = Color.White)
    }
    HorizontalDivider(color = Color.White, thickness = 1.dp)
    Button(
        onClick = {
            showKeyBindings = true
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        )
    ) {
        Text("Key Bindings", color = Color.White)
    }


    if (showKeyBindings) {
        KeyBindings(context)
    }
    if (showDialog2) {
        AlertDialog(
            onDismissRequest = {
                showDialog2 = false
            },
            title = {
                Text(text = "Confirm Reset", color = Color.White)
            },
            text = {
                Text(
                    "Are you sure you want to reset the controls and icons? This action cannot be undone.",
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        UserManageAssets(context).resetUI()
                        showDialog2 = false
                    }, colors = ButtonDefaults.buttonColors(Color.Transparent)
                ) {
                    Text("Confirm", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog2 = false
                    }, colors = ButtonDefaults.buttonColors(Color.Transparent)
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}



@Composable
fun UQM() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        CodeGroupOptionSelector()
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        Button(onClick = {
            UserManageAssets(context).installUQMResourceFiles()
        }, colors = ButtonDefaults.buttonColors(Color.Transparent)) {
            Text("Install UQM base files.", color = Color.White)
        }
    }
}