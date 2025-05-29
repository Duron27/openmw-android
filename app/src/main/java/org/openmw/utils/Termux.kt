package org.openmw.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.IntentService
import android.app.PendingIntent
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.termux.shared.android.ProcessUtils.LOG_TAG
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openmw.Constants
import android.content.ClipboardManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.map
import org.openmw.MainActivity
import org.openmw.R
import org.openmw.utils.GameFilesPreferences.NEXUS_API_KEY
import java.io.File

@Suppress("OVERRIDE_DEPRECATION")
@SuppressLint("NewApi")
class PluginResultsService : IntentService("PluginResultsService") {

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        val executionId = intent.getIntExtra(EXTRA_EXECUTION_ID, -1)
        val resultBundle = intent.getBundleExtra("result")
        val result = resultBundle?.getString("result")

        Log.d(LOG_TAG, "Received result for execution id $executionId: $result")

        // Use coroutine to update the UI on the main thread
        CoroutineScope(Dispatchers.Main).launch {
            resultCallback?.invoke(result ?: "No output received")
            Toast.makeText(applicationContext, "Result: $result", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val EXTRA_EXECUTION_ID = "com.alpha3.launcher.EXTRA_EXECUTION_ID"
        private var executionIdCounter = 0
        var resultCallback: ((String) -> Unit)? = null

        fun getNextExecutionId(): Int {
            return executionIdCounter++
        }
    }
}

@SuppressLint("SdCardPath")
fun runShellCommand(context: Context, resultCallback: (String) -> Unit) {
    PluginResultsService.resultCallback = resultCallback

    val intent = Intent()
    intent.setClassName("com.termux", "com.termux.app.RunCommandService")
    intent.setAction(RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND)
    intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_COMMAND_PATH, "/data/data/com.termux/files/usr/bin/bash")
    intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_ARGUMENTS, arrayOf("${Constants.USER_CONFIG}/UMOhelper.sh"))
    intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_WORKDIR, "/data/data/com.termux/files/home")
    intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_BACKGROUND, false)
    intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_SESSION_ACTION, "0")
    intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_COMMAND_LABEL, "Install packages")
    intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_COMMAND_DESCRIPTION, "Installs necessary packages.")

    // Create the intent for the IntentService class that should be sent the result by TermuxService
    val pluginResultsServiceIntent = Intent(context, PluginResultsService::class.java)

    // Generate a unique execution id for this execution command
    val executionId = PluginResultsService.getNextExecutionId()

    // Put an extra that uniquely identifies the command internally for your app
    pluginResultsServiceIntent.putExtra(PluginResultsService.EXTRA_EXECUTION_ID, executionId)

    // Create the PendingIntent that will be used by TermuxService to send result of commands back to the IntentService
    val pendingIntent = PendingIntent.getService(
        context, executionId, pluginResultsServiceIntent,
        PendingIntent.FLAG_ONE_SHOT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
    )
    intent.putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pendingIntent)

    try {
        // Send command intent for execution
        Log.d(LOG_TAG, "Sending execution command with id $executionId")
        context.startService(intent)
    } catch (e: Exception) {
        Log.e(LOG_TAG, "Failed to start execution command with id $executionId: ${e.message}")
    }
}

@Composable
fun TermuxView(context: Context) {
    val termuxOutput = remember { mutableStateOf("No output yet") }
    val showDialog = remember { mutableStateOf(false) }
    val file = File(Constants.UMO_HELPER)
    val scope = rememberCoroutineScope()
    var apiKey by remember { mutableStateOf("") }
    var hasApiKey by remember { mutableStateOf(false) }
    val savedPath by GameFilesPreferences.gameFilesUri.collectAsState()
    val clipboardManager = LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // Load existing value from DataStore
    LaunchedEffect(Unit) {
        // Continue executing the remaining logic inside the coroutine
        PermissionHelper.getTermuxPermission(context as Activity)
        context.dataStore.data.map { preferences ->
            preferences[NEXUS_API_KEY] ?: ""
        }.collect { storedApiKey ->
            apiKey = storedApiKey
            hasApiKey = storedApiKey.isNotEmpty()
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            Button(onClick = { showDialog.value = true }) {
                Text("Instructions")
            }
            if (hasApiKey) {
                Button(onClick = {
                    Log.d(LOG_TAG, "Button clicked: Running shell command")
                    runShellCommand(context) { result ->
                        Log.d(LOG_TAG, "Shell command result: $result")
                        termuxOutput.value = result
                    }
                }) {
                    Text("Install required libraries")
                }
            }
        }


            if (hasApiKey) {
                Image(
                    painter = painterResource(id = R.drawable.approval_delegation_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                    contentDescription = "Checkmark",
                    colorFilter = ColorFilter.tint(Color.Green),
                    modifier = Modifier
                        .size(36.dp)
                )
                Button(
                    onClick = {
                        // Remove the API key and set it to an empty string
                        scope.launch {
                            context.dataStore.edit { preferences ->
                                preferences[NEXUS_API_KEY] = ""
                            }
                            hasApiKey = false
                            apiKey = ""
                        }
                    }
                ) {
                    Text("Remove API Key")
                }
            } else {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Nexus API Key") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        // Save the value to DataStore
                        scope.launch {
                            context.dataStore.edit { preferences ->
                                preferences[NEXUS_API_KEY] = apiKey
                            }
                            hasApiKey = apiKey.isNotEmpty()
                        }
                        if (file.exists()) {
                            val content = file.readText()
                            val updatedContent = content.replace("\"MORROWIND_DIR\": \"/storage/emulated/0/\"", "\"MORROWIND_DIR\": \"$savedPath\"")
                                .replace("\"NEXUS_API_KEY\": \"\"", "\"NEXUS_API_KEY\": \"$apiKey\"")
                            file.writeText(updatedContent)
                            println("Successfully updated Morrowind directory path!")
                        } else {
                            println("File not found.")
                        }
                    }
                ) {
                    Text("Save")
                }
            }

        BasicText(
            text = termuxOutput.value,
            modifier = Modifier.padding(top = 16.dp),
            style = TextStyle(color = Color.White)
        )

        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = { showDialog.value = false },
                text = { Text("1. Install Termux. \n\n2.In the app drawer go into Termux settings and enable -Appear on top \n\n3.Copy and paste the below SED command in termux \n\n4. After that come back and click on the Install required libraries button.") },
                confirmButton = {
                    Row {
                        Button(onClick = {
                            showDialog.value = false
                        }) {
                            Text("Close")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val clip = ClipData.newPlainText("Termux Command", "sed -i 's/# allow-external-apps = true/allow-external-apps = true/' /data/data/com.termux/files/home/.termux/termux.properties")
                            clipboardManager.setPrimaryClip(clip)
                        }) {
                            Text("Copy SED Command")
                        }
                    }
                }
            )
        }
    }
}