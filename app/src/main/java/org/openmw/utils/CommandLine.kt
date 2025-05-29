package org.openmw.utils

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.openmw.Constants
import java.io.File

@Composable
fun CommandLineInputScreen(context: Context) {
    var commandLine by remember { mutableStateOf("") }
    val selectedOptions = remember { mutableStateListOf<String>() }
    var allOptions by remember { mutableStateOf(listOf(
        "--OSG_NOTIFY_LEVEL"
    )) }

    val optionDescriptions = mapOf(
        "OSG_NOTIFY_LEVEL" to "Sets the notification level for OpenSceneGraph."
    )

    var showDialog by remember { mutableStateOf(false) }
    val saveGameOptions by remember { mutableStateOf(emptyList<String>()) }
    val allOptionsCombined = allOptions + saveGameOptions

    Row(modifier = Modifier.fillMaxWidth()) {
        // Button styled as text field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small)
                .clickable { showDialog = true }
                .padding(16.dp)
        ) {
            Text(
                text = if (commandLine.isEmpty()) "Enter ENV Flags " else commandLine,
                style = MaterialTheme.typography.bodySmall,
                color = if (commandLine.isEmpty()) Color.Gray else Color.White
            )
        }

        // Scan for save games and add them to the options list
        LaunchedEffect(Unit) {
            val saveGames = scanSaveGames()
            val dynamicOptions = saveGames.map { "--load-savegame $it" }
            allOptions = allOptions + dynamicOptions
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "Enter Arguments") },
                text = {
                    Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Column {
                            OutlinedTextField(
                                value = commandLine,
                                onValueChange = {
                                    commandLine = it
                                    val parts = commandLine.split(" ").filter { it in allOptionsCombined }
                                    selectedOptions.clear()
                                    selectedOptions.addAll(parts)
                                },
                                label = { Text("Arguments") },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            allOptions.forEach { option ->
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = selectedOptions.contains(option),
                                            onCheckedChange = {
                                                if (selectedOptions.contains(option)) {
                                                    selectedOptions.remove(option)
                                                } else {
                                                    selectedOptions.add(option)
                                                }
                                                val textFieldParts = commandLine.split(" ").filter { it !in allOptions }
                                                commandLine = (textFieldParts + selectedOptions).distinct().joinToString(" ")
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = option,
                                            color = if (selectedOptions.contains(option)) Color.Green else Color.White
                                        )
                                        if (option !in optionDescriptions.keys) {
                                            IconButton(onClick = {
                                                runBlocking {
                                                    GameFilesPreferences.deleteUserOption(context, option)
                                                    allOptions = allOptions - option
                                                }
                                                // Also remove the option from the commandLine and selectedOptions
                                                selectedOptions.remove(option)
                                                val textFieldParts = commandLine.split(" ").filter { it != option }
                                                commandLine = textFieldParts.joinToString(" ")
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                                            }
                                        }
                                    }
                                    Text(
                                        text = optionDescriptions[option] ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(start = 32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val finalCommandLine = commandLine
                            runBlocking {
                                // Save command line
                                GameFilesPreferences.saveCommandLine(context, finalCommandLine)

                                // Extract new options without leading/trailing spaces and avoid empty entries
                                val newOptions = commandLine.split(" ").map { it.trim() }.filter { it.isNotEmpty() && it !in allOptions }.toSet()
                                if (newOptions.isNotEmpty()) {
                                    GameFilesPreferences.saveUserOptions(context, newOptions)
                                    allOptions = (allOptions + newOptions).distinct()
                                }
                            }
                            showDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

fun scanSaveGames(): List<String> {
    val saveGames = mutableListOf<String>()
    val savesDirectory = File(Constants.USER_SAVES)

    if (savesDirectory.exists() && savesDirectory.isDirectory) {
        savesDirectory.walk().forEach { file ->
            if (file.extension == "omwsave") {
                saveGames.add(file.absolutePath)
            }
        }
    }

    return saveGames.distinct() // Ensure the list contains unique paths
}



