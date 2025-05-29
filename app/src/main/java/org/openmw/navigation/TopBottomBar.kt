package org.openmw.navigation

import android.content.Context
import android.content.Intent
import android.system.Os
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.InternalCoroutinesApi
import org.openmw.Constants
import org.openmw.Constants.SETTINGS_FILE
import org.openmw.EngineActivity
import org.openmw.R
import org.openmw.isControllerConnected
import org.openmw.ui.controls.UIStateManager
import org.openmw.ui.controls.UIStateManager.launchedActivity
import org.openmw.ui.controls.UIStateManager.transparentBlack
import org.openmw.utils.GameFilesPreferences
import org.openmw.utils.GameFilesPreferences.readCodeGroup
import org.openmw.utils.ProgressWithNavmesh
import org.openmw.utils.UserManageAssets
import java.io.File

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@Composable
fun MyTopBar(context: Context) {
    val controllerConnected = isControllerConnected(context)
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = transparentBlack,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Text(
                "Alpha-3 Launcher",
                maxLines = 1,
                color = Color.White,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            if (controllerConnected) {
                Image(
                    painter = painterResource(id = R.drawable.stadia_controller_24dp_e8eaed_fill0_wght400_grad0_opsz24), // Replace with your icon resource ID
                    contentDescription = "Icon",
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(Color.Green) // Tint the icon green
                )
            }
            BarOptions(context)
        },
        modifier = Modifier.height(60.dp)
    )
}

@Composable
fun NavmeshScreen(onComplete: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ProgressWithNavmesh(onComplete = onComplete)
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@Composable
fun MyFloatingActionButton() {
    val context = LocalContext.current
    val savedPath by GameFilesPreferences.getGameFilesUriState(context).collectAsState(initial = null)
    val codeGroupOption by readCodeGroup(context).collectAsState(initial = "OpenMW")
    val bypassGameCheck by GameFilesPreferences.loadBypassGameCheck(context).collectAsState(initial = false)
    val image: Painter = when (codeGroupOption) {
        "OpenMW" -> painterResource(id = R.drawable.ic_launcher_foreground)
        "UQM" -> painterResource(id = R.drawable.dreadnought_big_004)
        else -> painterResource(id = R.drawable.ic_launcher_foreground)
    }

    FloatingActionButton(
        onClick = {
            UIStateManager.configureControls = false
            val intent = Intent(context, EngineActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (!bypassGameCheck) {
                val uri = savedPath
                if (uri != null) {
                    if (codeGroupOption == "OpenMW") {
                        val morrowindIni = File(uri, "morrowind.ini")
                        if (morrowindIni.exists()) {
                            launchedActivity = true
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(
                                context,
                                "Morrowind folder not found. Please select game files.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else if (codeGroupOption == "UQM") {
                        // Additional logic for UQM can be added here if needed
                        launchedActivity = true
                        context.startActivity(intent)
                    }
                }
            } else {
                if (codeGroupOption == "OpenMW") {
                    launchedActivity = true
                    context.startActivity(intent)
                } else if (codeGroupOption == "UQM") {
                    // Additional logic for UQM can be added here if needed
                    launchedActivity = true
                    context.startActivity(intent)
                }
            }
        },
        containerColor = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f),
        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
    ) {
        Image(
            painter = image,
            contentDescription = "Launch"
        )
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@Composable
fun BarOptions (context: Context) {
    var expanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    val settingsFile = File(SETTINGS_FILE)
    val codeGroupOption by readCodeGroup(context).collectAsState(initial = "OpenMW")

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Filled.Menu,
            modifier = Modifier
                .border(
                    BorderStroke(width = 1.dp, color = Color.Black)
                ),
            contentDescription = "Localized description"
        )
    }
    DropdownMenu(
        modifier = Modifier
            .background(color = transparentBlack)
            .border(
                BorderStroke(width = 1.dp, color = Color.Black)
            ),
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        if (codeGroupOption == "OpenMW") {
            DropdownMenuItem(
                text = {
                    val navmeshFile = File("${Constants.USER_FILE_STORAGE}/navmesh.db")
                    if (navmeshFile.exists()) {
                        val fileSize = navmeshFile.length()
                        Text("Regenerate Navmesh \n(Size: ${fileSize / 1024} KB)", color = Color.White) // Display size in KB
                    } else {
                        Text("Generate Navmesh", color = Color.White)
                    }
                },
                onClick = {
                    // Delete the navmesh file if it exists
                    val navmeshFile = File("${Constants.USER_FILE_STORAGE}/navmesh.db")
                    if (navmeshFile.exists()) {
                        navmeshFile.delete()
                    }

                    // Set environment variable to start navmesh generation
                    Os.setenv("OPENMW_GENERATE_NAVMESH_CACHE", "1", true)

                    UIStateManager.useNavmesh = true

                    // Start the navmesh activity
                    val intent = Intent(context, EngineActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )
            DropdownMenuItem(
                text = { Text("Reset Settings", color = Color.White) },
                onClick = {
                    showDialog = true
                }
            )
        }
        DropdownMenuItem(
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Logcat", color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = UIStateManager.isLogcatEnabled,
                        onCheckedChange = {
                            UIStateManager.isLogcatEnabled = it
                        }
                    )
                }
            },
            onClick = { /* Handle click if necessary */ }
        )
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Confirm Reset") },
                text = { Text("Are you sure you want to reset the settings?") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (settingsFile.exists()) {
                                settingsFile.delete()
                                Log.d("ManageAssets", "Deleted existing file: $SETTINGS_FILE")
                                // Copy over settings.cfg
                                UserManageAssets(context).resetUserConfig()
                                expanded = false
                            }
                            Toast.makeText(context, "Settings file reset", Toast.LENGTH_SHORT).show()
                            showDialog = false
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
