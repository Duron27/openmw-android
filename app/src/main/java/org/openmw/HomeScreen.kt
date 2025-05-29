package org.openmw

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import org.openmw.fragments.processSelectedFolder
import org.openmw.navigation.MyFloatingActionButton
import org.openmw.navigation.MyTopBar
import org.openmw.ui.controls.UIStateManager.transparentBlack
import org.openmw.utils.CommandLineInputScreen
import org.openmw.utils.FileBrowserMode
import org.openmw.utils.FileBrowserPopup
import org.openmw.utils.GameFilesPreferences
import org.openmw.utils.GameFilesPreferences.readCodeGroup
import org.openmw.utils.GameFilesPreferences.storeGameFilesPath
import org.openmw.utils.ModValue
import org.openmw.utils.ModValuesList
import org.openmw.utils.UqmDownloads
import org.openmw.utils.UserManageAssets
import java.io.File

@InternalCoroutinesApi
@DelicateCoroutinesApi
@ExperimentalFoundationApi // ModValuesList
@OptIn(ExperimentalMaterial3Api::class) // MyFloatingActionButton && MyTopBar
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(context: Context, modValues: List<ModValue>, navigateToSettings: () -> Unit) {
    val orientation = LocalConfiguration.current.orientation
    val paddingModifier = if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
        Modifier.padding(top = 65.dp, bottom = 45.dp).fillMaxSize()
    } else {
        Modifier.fillMaxSize()
    }
    val savedPath by GameFilesPreferences.getGameFilesUriState(context).collectAsState(initial = null)
    val codeGroupOption by readCodeGroup(context).collectAsState(initial = "OpenMW")

    Scaffold(
        topBar = {
            if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
                MyTopBar(context)
            }
        }, content = @Composable {
            BackgroundAnimation()
                Column(
                    modifier = paddingModifier,
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (codeGroupOption) {
                        "OpenMW" -> {
                            if (orientation != Configuration.ORIENTATION_LANDSCAPE && (savedPath.isNullOrEmpty() || savedPath == "Game Files: ")) {
                                OpenMW()
                            }
                            ModValuesList(modValues)
                        }
                        /*
                        else -> UqmDownloads()
                         */
                    }
                }

        }, bottomBar = {
            if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
                BottomAppBar(
                    containerColor = transparentBlack,
                    actions = {
                        Button(
                            onClick = { navigateToSettings() },
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color.Transparent),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
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

@Composable
fun OpenMW() {
    var showFileBrowser by remember { mutableStateOf(false) }
    var selectedFolderPath by remember { mutableStateOf<String?>(null) }
    var persistedPath by remember { mutableStateOf<String?>(null) }
    val infiniteTransition = rememberInfiniteTransition(label = "")
    var lastProcessedFolder by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val savedPath by GameFilesPreferences.getGameFilesUriState(context).collectAsState(initial = null)

    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 500
            ),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Column {
        Button(
            onClick = {
                showFileBrowser = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp)
                .height(56.dp)
                .let {
                    if (savedPath.isNullOrEmpty() || savedPath == "Game Files: ") {
                        it.graphicsLayer(scaleX = pulse, scaleY = pulse)
                    } else {
                        it
                    }
                },
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = transparentBlack
            )
        ) {
            Text(
                text = if (savedPath.isNullOrEmpty() || savedPath == "Game Files:  ") {
                    "Select Games Files"
                } else {
                    "Game Files: $savedPath"
                },
                color = Color.White
            )
        }

        selectedFolderPath?.let { path ->
            if (path != lastProcessedFolder) {
                lastProcessedFolder = path
                processSelectedFolder(context, File(path), onUriPersisted = { newPath ->
                    persistedPath = newPath
                })
            }
        }

        if (showFileBrowser) {
            FileBrowserPopup(
                onDismiss = { showFileBrowser = false },
                onFolderSelected = { folder ->
                    val selectedDirectory = DocumentFile.fromFile(folder)
                    val iniFile = selectedDirectory.findFile("Morrowind.ini")
                    val dataFilesFolder = selectedDirectory.findFile("Data Files")

                    if (iniFile != null && dataFilesFolder != null && dataFilesFolder.isDirectory) {
                        CoroutineScope(Dispatchers.IO).launch {
                            storeGameFilesPath(context, folder.absolutePath)
                        }
                        showFileBrowser = false
                        Toast.makeText(
                            context,
                            "Selected Folder: ${folder.name}",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Save to DataStore
                        CoroutineScope(Dispatchers.IO).launch {
                            storeGameFilesPath(context, folder.absolutePath)
                        }

                        // Re-trigger processing
                        processSelectedFolder(
                            context,
                            folder,
                            onUriPersisted = { persistedPath = it })

                    } else {
                        CoroutineScope(Dispatchers.IO).launch {
                            storeGameFilesPath(context, "")
                        }
                        Toast.makeText(
                            context,
                            "Please select a folder with Morrowind.ini and Data Files.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                mode = FileBrowserMode.FOLDER
            )
        }
    }
}
