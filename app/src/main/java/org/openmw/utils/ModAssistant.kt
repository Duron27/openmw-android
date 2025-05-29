package org.openmw.utils

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Environment
import android.system.Os
import android.util.Log
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.documentfile.provider.DocumentFile
import com.composables.core.ScrollArea
import com.composables.core.Thumb
import com.composables.core.ThumbVisibility
import com.composables.core.VerticalScrollbar
import com.composables.core.rememberScrollAreaState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openmw.Constants
import org.openmw.Constants.SETTINGS_FILE
import org.openmw.ControlsInsert
import org.openmw.DevInsert
import org.openmw.EngineActivity
import org.openmw.FeaturesSwitches
import org.openmw.ImportExportInsert
import org.openmw.Logs
import org.openmw.R
import org.openmw.navigation.BarOptions
import org.openmw.ui.controls.UIStateManager
import org.openmw.ui.controls.UIStateManager.customColor
import org.openmw.ui.controls.UIStateManager.transparentBlack
import org.openmw.utils.GameFilesPreferences.readCodeGroup
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.File
import java.util.Locale
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.seconds

data class ModValue(
    var id: Int,
    val category: String,
    val value: String,
    val isChecked: Boolean,
    var originalIndex: Int
) {
    companion object {
        fun updateUIFromModValues(categories: List<String>): List<List<ModValue>> {
            val newModValues = readModValues()
            return newModValues.categorizeModValues(categories)
        }
    }
}

fun List<ModValue>.categorizeModValues(categories: List<String>): List<List<ModValue>> {
    return categories.map { category ->
        this.filter { it.category == category }
    }
}

@Composable
fun PathCorrectorDialog(
    changes: Map<String, String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "Confirm Changes")
            }
        },
        text = {
            if (changes.isNotEmpty()) {
                Column {
                    Text(text = "The following changes will be made:")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn {
                        items(changes.entries.toList()) { (oldLine, newLine) ->
                            Text(text = "$oldLine -> $newLine")
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text(text = "No changes required", color = Color.Green, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Icon(Icons.Default.CheckCircle, contentDescription = "Checkmark", tint = Color.Green, modifier = Modifier.size(48.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = changes.isNotEmpty()) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

suspend fun pathCorrector(): Map<String, String> {
    val configFilePath = Constants.USER_OPENMW_CFG
    val configFile = File(configFilePath)
    val results = mutableMapOf<String, String>()

    withContext(Dispatchers.IO) {
        configFile.readLines().filter { it.startsWith("data=") }.forEach { line ->
            val dataPath = line.substringAfter("data=")
            val dataDirectory = File(dataPath)

            // Check if the path is valid
            if (!dataDirectory.exists() || !dataDirectory.isDirectory) {
                val fileName = dataDirectory.name
                val searchResult = searchFile(Environment.getExternalStorageDirectory(), fileName)

                if (searchResult != null) {
                    val correctedLine = "data=$searchResult"
                    results[line] = correctedLine
                }
            }
        }
    }

    return results
}

fun applyChanges(changes: Map<String, String>) {
    val configFilePath = Constants.USER_OPENMW_CFG
    val configFile = File(configFilePath)
    val existingLines = configFile.readLines().toMutableList()

    val updatedLines = existingLines.map { line ->
        changes[line] ?: line
    }

    configFile.writeText(updatedLines.joinToString("\n"))
}

fun searchFile(directory: File, fileName: String): String? {
    directory.listFiles()?.forEach { file ->
        if (file.isDirectory) {
            if (file.name.equals(fileName, ignoreCase = true)) {
                return file.absolutePath
            } else {
                val result = searchFile(file, fileName)
                if (result != null) {
                    return result
                }
            }
        }
    }
    return null
}


suspend fun startSearch(updateStatus: (String) -> Unit): List<String> {
    val basePath = Environment.getExternalStorageDirectory().toString()
    val baseDirectory = File(basePath)
    val extensions = arrayOf("bsa", "esm", "esp", "esl", "omwaddon", "omwgame", "omwscripts")
    val filterList = listOf("Morrowind.bsa", "Bloodmoon.bsa", "Tribunal.bsa") // Add your filters here
    val results = mutableListOf<String>()

    withContext(Dispatchers.IO) {
        searchFiles(baseDirectory, extensions, results, updateStatus, filterList)
    }
    return results
}

suspend fun searchFiles(directory: File, extensions: Array<String>, results: MutableList<String>, updateStatus: (String) -> Unit, filterList: List<String>) {
    directory.listFiles()?.forEach { file ->
        if (file.isDirectory && filterList.none { file.name.contains(it, ignoreCase = true) }) {
            searchFiles(file, extensions, results, updateStatus, filterList)
        } else if (!file.isDirectory && filterList.none { file.name.contains(it, ignoreCase = true) }) {
            val fileName = file.name.lowercase()
            if (extensions.any { fileName.endsWith(it) }) {
                results.add(file.absolutePath)

                withContext(Dispatchers.Main) {
                    updateStatus(file.absolutePath)
                }
            }
        }
    }
}

fun updateConfigFile(files: List<String>) {
    val configFilePath = Constants.USER_OPENMW_CFG
    val configFile = File(configFilePath)
    val existingLines = configFile.readLines().toMutableList()
    val newLines = mutableListOf<String>()

    val dataLines = existingLines.filter { it.startsWith("data=") }.toMutableList()
    val contentLines = existingLines.filter { it.startsWith("content=") }.toMutableList()
    val groundcoverLines = existingLines.filter { it.startsWith("groundcover=") }.toMutableList()

    val groundcoverPattern = Pattern.compile("Rem_.*", Pattern.CASE_INSENSITIVE) // Modify this regex to match your pattern

    files.forEach { filePath ->
        val file = File(filePath)
        val directoryPath = file.parent
        val fileName = file.name

        val dataLine = "data=$directoryPath"
        val categoryLine = if (groundcoverPattern.matcher(fileName).matches()) {
            "groundcover=$fileName"
        } else {
            "content=$fileName"
        }

        if (!dataLines.contains(dataLine)) {
            dataLines.add(dataLine)
        }

        if (categoryLine.startsWith("groundcover=") && !groundcoverLines.contains(categoryLine)) {
            groundcoverLines.add(categoryLine)
        } else if (categoryLine.startsWith("content=") && !contentLines.contains(categoryLine)) {
            contentLines.add(categoryLine)
        }
    }

    newLines.addAll(dataLines)
    newLines.addAll(contentLines)
    newLines.addAll(groundcoverLines)

    configFile.writeText(newLines.joinToString("\n"))
}

fun modPathSelection(context: Context, directory: File, onPathPersisted: (String?) -> Unit) {
    try {
        CoroutineScope(Dispatchers.IO).launch {
            val ignoreList = listOf("Morrowind.bsa", "Tribunal.bsa", "Bloodmoon.bsa")
            val extensions = arrayOf("bsa", "esm", "esp", "esl", "omwaddon", "omwgame", "omwscripts")
            val selectedDirectory = DocumentFile.fromFile(directory)
            val files = findFilesWithExtensions(selectedDirectory, extensions)
            val modPath = directory.absolutePath

            val modValues = files.mapIndexed { index, file ->
                val fileName = file.name ?: ""
                val nameWithoutExtension = fileName.substringBeforeLast(".")
                val extension = fileName.substringAfterLast(".")
                ModValue(index, "content", "$nameWithoutExtension.$extension", isChecked = true, originalIndex = index + 1) // Set isChecked as needed
            }.toMutableList()

            modValues.add(ModValue(modValues.size, "data", modPath ?: "", isChecked = true, originalIndex = modValues.size + 1)) // Set isChecked as needed

            writeModValuesToFile(modValues, Constants.USER_OPENMW_CFG, ignoreList)

            if (files.isNotEmpty()) {
                onPathPersisted(modPath)
            } else {
                onPathPersisted("")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onPathPersisted(null)
    }
}

fun readModValues(): List<ModValue> {
    val values = mutableListOf<ModValue>()
    val validCategories = setOf("data", "content", "groundcover")

    // Check if the file exists
    val configFile = File(Constants.USER_OPENMW_CFG)
    if (!configFile.exists()) {
        return values // Return an empty list if the file doesn't exist
    }

    configFile.forEachLine { line ->
        val trimmedLine = line.trim()
        if ("=" in trimmedLine) {
            val isChecked = !trimmedLine.startsWith("#")
            val (category, value) = trimmedLine.removePrefix("#").split("=", limit = 2).map { it.trim() }
            if (category in validCategories) {
                values.add(ModValue(values.size, category, value, isChecked, values.size + 1))
            }
        }
    }
    return values
}


fun writeModValuesToFile(modValues: List<ModValue>, filePath: String, ignoreList: List<String>) {
    val file = File(filePath)

    // Read existing lines while maintaining order and categories
    val existingLines = mutableListOf<String>().apply {
        if (file.exists()) {
            file.forEachLine { line ->
                if (line.trim().isNotEmpty()) {
                    add(line.trim())
                }
            }
        }
    }

    val sortedModValues = modValues.sortedWith(compareBy({ it.category }, { it.originalIndex }))
    val categorizedModValues = sortedModValues.groupBy { it.category }
    val orderedCategories = listOf("data", "content", "groundcover")

    // Create a map to track already present lines per category
    val existingCategoryLines = orderedCategories.associateWith { category ->
        existingLines.filter { it.startsWith(category) }.toMutableList()
    }

    // Append new lines to existing category lines
    orderedCategories.forEach { category ->
        categorizedModValues[category]?.sortedBy { it.originalIndex }?.forEach { modValue ->
            val line = "${modValue.category}=${modValue.value}" // Ignore isChecked
            val lineWithoutPrefix = line.removePrefix("#")
            val duplicates = existingCategoryLines[category]?.map { it.removePrefix("#") } ?: emptyList()
            if (!duplicates.contains(lineWithoutPrefix) && !ignoreList.contains(modValue.value)) { // Add only if it's not a duplicate and not in ignore list
                existingCategoryLines[category]!!.add(line)
            }
        }
    }

    // Combine final lines preserving the order
    val finalLines = mutableListOf<String>()
    orderedCategories.forEach { category ->
        finalLines.addAll(existingCategoryLines[category]!!)
        finalLines.add("") // Add a newline between categories
    }

    // Write all lines back to the file
    file.writeText(finalLines.joinToString("\n"))
}

fun searchMods(query: String, modValues: List<ModValue>, category: String?): List<ModValue> {
    return modValues.filter {
        (category == null || it.category == category) && it.value.contains(query, ignoreCase = true)
    }
}

fun navigateToMod(
    modValue: ModValue,
    categorizedModValues: List<List<ModValue>>,
    setSelectedTabIndex: (Int) -> Unit,
    lazyListState: LazyListState,
    coroutineScope: CoroutineScope
) {
    val tabIndex = categorizedModValues.indexOfFirst { categoryList -> categoryList.any { it.id == modValue.id } }
    if (tabIndex != -1) {
        setSelectedTabIndex(tabIndex)
        val itemIndex = categorizedModValues[tabIndex].indexOfFirst { it.id == modValue.id }
        if (itemIndex != -1) {
            coroutineScope.launch {
                try {
                    addCustomLog("Scrolling to ${modValue.id} at index: $itemIndex", textSize = 14, textColor = Color.Magenta)
                    lazyListState.scrollToItem(itemIndex)  // Use scrollToItem for testing
                } catch (e: Exception) {
                    addCustomLog("Error scrolling: ${e.message}", textSize = 14, textColor = Color.Red)
                }
            }
        }
    }
}

@InternalCoroutinesApi
@OptIn(ExperimentalMaterial3Api::class)
@DelicateCoroutinesApi
@ExperimentalFoundationApi
@Composable
fun ModValuesList(modValues: List<ModValue>) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showFileBrowser by remember { mutableStateOf(false) }

    var selectedModPath by remember { mutableStateOf<String?>(null) }
    val categories = listOf("data", "content", "groundcover")
    var categorizedModValues by remember {
        mutableStateOf(categories.map { category ->
            modValues.filter { it.category == category }
        })
    }
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showDialogPC by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf(emptyList<ModValue>()) }
    var searchInProgress by remember { mutableStateOf(false) }
    var pathCorrectorInProgress by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Status") }
    var changes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val foundFiles = remember { mutableStateListOf<String>() }
    val orientation = LocalConfiguration.current.orientation
    val newModValues = readModValues()
    val stateSB = rememberScrollAreaState(lazyListState)
    var expanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    val settingsFile = File(SETTINGS_FILE)

    // Launch the app
    var launchFloatingActionButton by remember { mutableStateOf(false) }

    // Manage whats in the tabs here
    var showMods by remember { mutableStateOf(true) }
    var showNav by remember { mutableStateOf(false) }
    var showDelta by remember { mutableStateOf(false) }
    var autoMods by remember { mutableStateOf(false) }
    var downloader by remember { mutableStateOf(false) }
    var s3LightsDialog by remember { mutableStateOf(false) }
    var showDialogSettings by remember { mutableStateOf(false) }

    val savedPath by GameFilesPreferences.getGameFilesUriState(context).collectAsState(initial = null)

    val codeGroupOption by readCodeGroup(context).collectAsState(initial = "OpenMW")
    val image: Painter = when (codeGroupOption) {
        "OpenMW" -> painterResource(id = R.drawable.ic_launcher_foreground)
        "UQM" -> painterResource(id = R.drawable.dreadnought_big_004)
        else -> painterResource(id = R.drawable.ic_launcher_foreground)
    }

    fun resetStates() {
        autoMods = false
        downloader = false
        showMods = false
        showNav = false
        s3LightsDialog = false
        showDelta = false
        showDialogSettings = false
        showSearchDialog = false
        launchFloatingActionButton = false
    }

    val coroutineScope = rememberCoroutineScope()
    val newFeatureEnabledChecked by GameFilesPreferences.loadNewFeatureEnabledState(context)
        .collectAsState(initial = false)
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val currentList = categorizedModValues[selectedTabIndex].toMutableList()
        val movedItem = currentList.removeAt(from.index)
        currentList.add(to.index, movedItem)

        categorizedModValues = categorizedModValues.toMutableList().apply {
            this[selectedTabIndex] = currentList
        }
    }

    // Reload the mod values and update the UI
    categorizedModValues = ModValue.updateUIFromModValues(categories)

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 500
            ),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Column {
        ScrollableTabRow(selectedTabIndex = selectedTabIndex) { // , edgePadding = 1.dp,
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Tab(
                    selected = false,
                    onClick = {
                        resetStates()
                        UIStateManager.configureControls = false
                        val uri = savedPath
                        if (uri != null) {
                            val intent = Intent(context, EngineActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            if (codeGroupOption == "OpenMW") {
                                val morrowindIni = File(uri, "morrowind.ini")
                                if (morrowindIni.exists()) {
                                    UIStateManager.launchedActivity = true
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
                                UIStateManager.launchedActivity = true
                                context.startActivity(intent)
                            }
                        }
                    },
                    icon = {
                        Image(
                            painter = image,
                            contentDescription = "Launch"
                        )
                    }
                )
            }
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        resetStates()
                        showMods = true
                        selectedTabIndex = index

                    },
                    text = { Text(category.replaceFirstChar { it.titlecase(Locale.getDefault()) }) }
                )
            }
            Tab(
                selected = selectedTabIndex == categories.size,
                onClick = { showFileBrowser = true },
                text = { Text("Add Mod") }
            )
            if (newFeatureEnabledChecked) {
                Tab(
                    selected = false,
                    onClick = {
                        resetStates()
                        autoMods = true
                    },
                    text = { Text("Search / Repair") }
                )
            }
            Tab(
                selected = false,
                onClick = {
                    resetStates()
                    downloader = true
                },
                text = { Text("Mod Downloader") }
            )
            Tab(
                selected = false,
                onClick = {
                    resetStates()
                    s3LightsDialog = true
                },
                text = { Text("S3LightFixes") }
            )
            Tab(
                selected = false,
                onClick = {
                    resetStates()
                    showDelta = true
                },

                text = { Text("Delta_Plugin") }
            )
            Tab(
                selected = false,
                onClick = {
                    resetStates()
                    showSearchDialog = true
                },
                icon = { Icon(Icons.Default.Search, contentDescription = "Search Mods") }
            )
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Tab(
                    selected = false,
                    onClick = {
                        resetStates()
                        showDialogSettings = true
                    },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
                )
                Tab(
                    selected = false,
                    onClick = {
                        expanded = true
                    },
                    icon = { Icon(Icons.Filled.Menu, contentDescription = "Top Menu") }
                )
                CommandLineInputScreen(context)
                DropdownMenu(
                    modifier = Modifier
                        .background(color = transparentBlack)
                        .border(BorderStroke(width = 1.dp, color = Color.Black)),
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
                                showDialogSettings = true
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
                }
            }
        }
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
        if (showFileBrowser) {
            FileBrowserPopup(
                onDismiss = { showFileBrowser = false },
                onFolderSelected = { folder ->
                    modPathSelection(context, folder) { modPath ->
                        selectedModPath = modPath

                        // Reload the mod values
                        categorizedModValues = categories.map { category ->
                            newModValues.filter { it.category == category }
                        }
                    }
                    showFileBrowser = false
                },
                mode = FileBrowserMode.FOLDER
            )
        }
        fun handleSearchResult(modValue: ModValue) {
            navigateToMod(
                modValue,
                categorizedModValues,
                setSelectedTabIndex = { selectedTabIndex = it },
                lazyListState = lazyListState,
                coroutineScope = coroutineScope  // Pass the CoroutineScope
            )
            showSearchDialog = false
            showMods = true
        }
        if (showSearchDialog) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize(),  // Increased height for filter options
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Search Mods", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Category Filter Checkboxes
                    categories.forEach { category ->
                        var isChecked by remember { mutableStateOf(false) }
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                isChecked = checked
                                selectedCategory = if (checked) category else null
                                searchResults = if (selectedCategory != null) {
                                    searchMods(searchQuery, modValues, selectedCategory)
                                } else {
                                    searchMods(searchQuery, modValues, null)
                                }
                            }
                        )
                        Text(category)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        searchResults = searchMods(it, modValues, selectedCategory)
                    },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(color = Color.Black)
                ) {
                    items(searchResults) { result ->
                        Text(
                            text = result.value,
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable {
                                    handleSearchResult(result)
                                }
                        )
                    }
                }

            }
        }
        if (showNav) {
            BarOptions(context)
        }
        if (s3LightsDialog) {
            S3LightFixes()
        }
        if (downloader) {
            if (hasInternetPermission(context)) {
                //DownloadAndRunApp()
                TermuxView(context)
                //ModFilterAndDisplay()
            } else {
                Text("Enable internet for the options here.", color = Color.White)
            }
        }
        if (showDelta) {
            DeltaScreen()
        }
        if (autoMods) {
            Text(
                text = "This is extremely experimental!!",
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        pathCorrectorInProgress = true
                        foundFiles.clear() // Clear the list
                        coroutineScope.launch {
                            val newChanges = pathCorrector()
                            changes = newChanges
                            showDialogPC = true
                            pathCorrectorInProgress = false
                        }
                    },
                    enabled = !pathCorrectorInProgress
                ) {
                    Text(
                        text = "Path Corrector",
                        style = TextStyle(fontSize = TextUnit.Unspecified) // Auto-sizing text
                    )
                }
                Button(
                    onClick = {
                        searchInProgress = true
                        statusMessage = "Searching..."
                        foundFiles.clear() // Clear the list
                        coroutineScope.launch {
                            val searchResult = startSearch { path ->
                                statusMessage = "Found: $path"
                                foundFiles.add(path)
                            }
                            updateConfigFile(searchResult)
                            searchInProgress = false
                            statusMessage =
                                "Search completed. Found ${searchResult.size} files."
                        }
                    },
                    enabled = !searchInProgress
                ) {
                    Text(
                        text = "Search",
                        style = TextStyle(fontSize = TextUnit.Unspecified) // Auto-sizing text
                    )
                }
            }

            // Display progress indicator if any task is in progress
            if (searchInProgress || pathCorrectorInProgress) {
                CircularProgressIndicator()
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "$statusMessage\n")

            // PathCorrectorDialog inside the Dialog
            if (showDialogPC) {
                PathCorrectorDialog(
                    changes = changes,
                    onConfirm = {
                        coroutineScope.launch {
                            applyChanges(changes)
                            showDialogPC = false
                            statusMessage = "Paths corrected and updated."
                        }
                    },
                    onDismiss = { showDialogPC = false }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(color = Color.Black), // = MaterialTheme.colorScheme.background

            ) {
                items(foundFiles) { file ->
                    Text(text = file, fontSize = 12.sp)
                }
            }
        }

        // Manage whats in the tabs here
        var switches by remember { mutableStateOf(false) }
        var iniValues by remember { mutableStateOf(false) }
        var controls by remember { mutableStateOf(false) }
        var importexport by remember { mutableStateOf(false) }
        var logs by remember { mutableStateOf(false) }
        var dev by remember { mutableStateOf(false) }

        fun resetStates() {
            switches = false
            iniValues = false
            controls = false
            importexport = false
            logs = false
            dev = false
        }

        // Show dialog when showDialog is true
        if (showDialogSettings) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(4.dp)
                        .background(color = customColor)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            resetStates()
                            switches = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = customColor
                        )
                    ) {
                        Text(
                            text = "Launcher Settings",
                            color = if (switches) Color.Green else Color.White
                        )
                    }
                    Button(
                        onClick = {
                            resetStates()
                            iniValues = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = customColor
                        )
                    ) {
                        Text(
                            text = "Settings.cfg",
                            color = if (iniValues) Color.Green else Color.White
                        )
                    }
                    Button(
                        onClick = {
                            resetStates()
                            controls = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = customColor
                        )
                    ) {
                        Text(
                            text = "Controls",
                            color = if (controls) Color.Green else Color.White
                        )
                    }
                    Button(
                        onClick = {
                            resetStates()
                            importexport = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = customColor
                        )
                    ) {
                        Text(
                            text = "Import / Export",
                            color = if (importexport) Color.Green else Color.White
                        )
                    }
                    Button(
                        onClick = {
                            resetStates()
                            logs = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = customColor
                        )
                    ) {
                        Text(
                            text = "Logs",
                            color = if (logs) Color.Green else Color.White
                        )
                    }
                    if (newFeatureEnabledChecked) {
                        Button(
                            onClick = {
                                resetStates()
                                dev = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = customColor
                            )
                        ) {
                            Text(
                                text = "Developer Menu",
                                color = if (dev) Color.Green else Color.White
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if (switches) {
                        LazyColumn(
                            modifier = Modifier
                                .padding(4.dp)
                                .background(color = customColor),
                        ) {
                            item {
                                FeaturesSwitches(context)
                            }
                        }
                    }
                    if (iniValues) {
                        IniSettings()
                    }
                        if (controls) {
                            LazyColumn(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .background(color = customColor),
                            ) {
                                item {
                                    ControlsInsert()
                                }
                            }
                        }
                        if (importexport) {
                            LazyColumn(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .background(color = customColor),
                            ) {
                                item {
                                    ImportExportInsert()
                                }
                            }
                        }
                        if (dev) {
                            LazyColumn(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .background(color = customColor),
                            ) {
                                item {
                                    DevInsert()
                                }
                            }
                        }
                        if (logs) {
                            LazyColumn(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .background(color = customColor),
                            ) {
                                item {
                                    Logs()
                                }
                            }
                        }
                    }
                }
            } else {
                // Update UI after dialog closes
                categorizedModValues = ModValue.updateUIFromModValues(categories)

            }

            ScrollArea(state = stateSB) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    state = lazyListState,
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (showMods) {
                        items(
                            categorizedModValues[selectedTabIndex],
                            key = { it.originalIndex }) { modValue ->
                            ReorderableItem(reorderableLazyListState, key = modValue.originalIndex) {
                                var isDragging by remember { mutableStateOf(false) }
                                var showPopup by remember { mutableStateOf(false) }
                                var showDialog2 by remember { mutableStateOf(false) }
                                var isChecked by remember { mutableStateOf(modValue.isChecked) }
                                var showDeleteConfirmation by remember { mutableStateOf(false) }
                                var showMoveDialog by remember { mutableStateOf(false) }

                                val backgroundColor by animateColorAsState(
                                    when {
                                        isDragging -> Color(0xFF8BC34A) // Color during drag
                                        modValue.isChecked -> Color.DarkGray
                                        else -> MaterialTheme.colorScheme.surface
                                    }, label = ""
                                )
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = backgroundColor,
                                    ),
                                    modifier = Modifier.then(
                                        if (isDragging) Modifier.graphicsLayer(
                                            scaleX = pulse,
                                            scaleY = pulse
                                        ) else Modifier
                                    ),
                                    elevation = CardDefaults.cardElevation(4.dp),
                                    onClick = { showPopup = true },
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                isChecked = checked
                                                val currentList =
                                                    categorizedModValues[selectedTabIndex].toMutableList()
                                                val index =
                                                    currentList.indexOfFirst { it.originalIndex == modValue.originalIndex }
                                                if (index != -1) {
                                                    currentList[index] =
                                                        currentList[index].copy(isChecked = checked)
                                                }
                                                categorizedModValues =
                                                    categorizedModValues.toMutableList().apply {
                                                        this[selectedTabIndex] = currentList
                                                    }

                                                // Update file
                                                val file = File(Constants.USER_OPENMW_CFG)
                                                val existingLines = mutableListOf<String>()
                                                if (file.exists()) {
                                                    file.forEachLine { line ->
                                                        if (line.trim().isNotEmpty()) {
                                                            existingLines.add(line.trim())
                                                        }
                                                    }
                                                }
                                                val updatedLines = existingLines.map { line ->
                                                    if (line.contains(modValue.value)) {
                                                        if (checked) {
                                                            modValue.category + "=" + modValue.value
                                                        } else {
                                                            "#" + modValue.category + "=" + modValue.value
                                                        }
                                                    } else {
                                                        line
                                                    }
                                                }
                                                file.writeText(updatedLines.joinToString("\n") + "\n")

                                                // Reload the mod values and update the UI
                                                categorizedModValues = ModValue.updateUIFromModValues(categories)

                                            }
                                        )
                                        Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                                            Text(
                                                text = modValue.value,
                                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                                            )
                                            Text(
                                                text = "Load Order: ${modValue.originalIndex}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier,
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            IconButton(
                                                modifier = Modifier.draggableHandle(
                                                    onDragStarted = {
                                                        isDragging = true
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                            vibrate(context)
                                                        }
                                                    },
                                                    onDragStopped = {
                                                        isDragging = false
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                            vibrate(context)
                                                        }

                                                        val currentList =
                                                            categorizedModValues[selectedTabIndex].toMutableList()

                                                        // Update the original indexes based on current list order
                                                        currentList.forEachIndexed { index, item ->
                                                            currentList[index] =
                                                                item.copy(originalIndex = index + 1)
                                                        }

                                                        categorizedModValues =
                                                            categorizedModValues.toMutableList().apply {
                                                                this[selectedTabIndex] = currentList
                                                            }

                                                        // Update the file with the new order
                                                        val file = File(Constants.USER_OPENMW_CFG)
                                                        val finalLines = categorizedModValues.flatten()
                                                            .sortedWith(
                                                                compareBy(
                                                                    { categories.indexOf(it.category) },
                                                                    { it.originalIndex })
                                                            )
                                                            .map { modValue ->
                                                                if (modValue.isChecked) {
                                                                    "${modValue.category}=${modValue.value}"
                                                                } else {
                                                                    "#${modValue.category}=${modValue.value}"
                                                                }
                                                            }
                                                        file.writeText(finalLines.joinToString("\n") + "\n")

                                                        // Reload the mod values and update the UI
                                                        categorizedModValues = ModValue.updateUIFromModValues(categories)
                                                    }
                                                ),
                                                onClick = { showDialog2 = true },
                                            ) {
                                                Icon(Icons.Rounded.Menu, contentDescription = "Reorder")
                                            }

                                        }
                                    }
                                    if (showPopup) {
                                        Popup(
                                            alignment = Alignment.Center,
                                            onDismissRequest = {
                                                showPopup = false
                                            } // Hide the popup when dismissed
                                        ) {
                                            Surface(
                                                modifier = Modifier.padding(16.dp),
                                                shape = MaterialTheme.shapes.medium,
                                                color = MaterialTheme.colorScheme.background
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = "Choose an action for the selected mod.",
                                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "Name: ${modValue.value}",
                                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                                                    )
                                                    Text(
                                                        text = "Category: ${modValue.category}",
                                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Button(
                                                        onClick = {
                                                            // Handle the switch category action
                                                            val updatedList =
                                                                categorizedModValues[selectedTabIndex].toMutableList()
                                                            val index =
                                                                updatedList.indexOfFirst { it.originalIndex == modValue.originalIndex }
                                                            if (index != -1) {
                                                                val newCategory =
                                                                    if (modValue.category == "content") "groundcover" else "content"
                                                                updatedList[index] =
                                                                    updatedList[index].copy(category = newCategory)
                                                                categorizedModValues =
                                                                    categorizedModValues.toMutableList()
                                                                        .apply {
                                                                            this[selectedTabIndex] =
                                                                                updatedList
                                                                        }

                                                                // Update the file directly with the new category values
                                                                val file = File(Constants.USER_OPENMW_CFG)
                                                                val existingLines = mutableListOf<String>()
                                                                if (file.exists()) {
                                                                    file.forEachLine { line ->
                                                                        if (line.trim().isNotEmpty()) {
                                                                            existingLines.add(line.trim())
                                                                        }
                                                                    }
                                                                }

                                                                // Update file with new categories
                                                                val finalLines =
                                                                    categorizedModValues.flatten()
                                                                        .map { modValue ->
                                                                            "${modValue.category}=${modValue.value}"
                                                                        }
                                                                file.writeText(finalLines.joinToString("\n") + "\n")

                                                                // Reload the mod values and update the UI
                                                                categorizedModValues = ModValue.updateUIFromModValues(categories)
                                                            }
                                                            showPopup = false
                                                        }
                                                    ) {
                                                        Text(
                                                            text = "Switch to ${if (modValue.category == "content") "groundcover" else "content"}",
                                                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Button(
                                                        onClick = {
                                                            // Show the delete confirmation dialog
                                                            showDeleteConfirmation = true
                                                        }
                                                    ) {
                                                        Text(
                                                            text = "Delete",
                                                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Button(
                                                        onClick = {
                                                            // Show the move dialog
                                                            showMoveDialog = true
                                                        }
                                                    ) {
                                                        Text(
                                                            text = "Move",
                                                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Button(onClick = { showPopup = false }) {
                                                        Text(
                                                            text = "Cancel",
                                                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (showDeleteConfirmation) {
                                        AlertDialog(
                                            onDismissRequest = { showDeleteConfirmation = false },
                                            title = { Text("Confirm Deletion") },
                                            text = { Text("Are you sure you want to delete this mod?") },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        // Handle the delete action
                                                        val updatedList =
                                                            categorizedModValues[selectedTabIndex].toMutableList()
                                                        val index =
                                                            updatedList.indexOfFirst { it.originalIndex == modValue.originalIndex }
                                                        if (index != -1) {
                                                            updatedList.removeAt(index)
                                                            categorizedModValues =
                                                                categorizedModValues.toMutableList().apply {
                                                                    this[selectedTabIndex] = updatedList
                                                                }

                                                            // Update the file after deletion
                                                            val file = File(Constants.USER_OPENMW_CFG)
                                                            val finalLines =
                                                                categorizedModValues.flatten()
                                                                    .map { modValue ->
                                                                        "${modValue.category}=${modValue.value}"
                                                                    }
                                                            file.writeText(finalLines.joinToString("\n") + "\n")

                                                            // Reload the mod values and update the UI
                                                            categorizedModValues = ModValue.updateUIFromModValues(categories)
                                                        }
                                                        showDeleteConfirmation = false
                                                        showPopup = false
                                                    }
                                                ) {
                                                    Text("Yes")
                                                }
                                            },
                                            dismissButton = {
                                                Button(onClick = { showDeleteConfirmation = false }) {
                                                    Text("No")
                                                }
                                            }
                                        )
                                    }
                                    if (showMoveDialog) {
                                        // Get the initial index of the mod
                                        val initialIndex =
                                            categorizedModValues[selectedTabIndex].indexOfFirst { it.originalIndex == modValue.originalIndex }
                                        selectedIndex = initialIndex

                                        AlertDialog(
                                            onDismissRequest = { showMoveDialog = false },
                                            title = {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "Mod Mover",
                                                        color = Color.Green,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            },
                                            text = {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Select the new position for the mod:")
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {

                                                        AndroidView(factory = {
                                                            NumberPicker(context).apply {
                                                                minValue = 0
                                                                maxValue =
                                                                    categorizedModValues[selectedTabIndex].size - 1
                                                                value =
                                                                    initialIndex  // Set the initial value
                                                                wrapSelectorWheel = true
                                                                // Custom formatter to display index and mod value
                                                                setFormatter { index ->
                                                                    val modValues =
                                                                        categorizedModValues[selectedTabIndex].getOrNull(
                                                                            index
                                                                        )?.value ?: ""
                                                                    "$index = $modValues"
                                                                }
                                                                setOnValueChangedListener { _, _, newVal ->
                                                                    selectedIndex = newVal
                                                                }
                                                            }.also { numberPicker ->
                                                                // Set the layout parameters to make the NumberPicker wider
                                                                numberPicker.layoutParams =
                                                                    LinearLayout.LayoutParams(
                                                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                                                    ).apply {
                                                                        weight = 1f
                                                                    }

                                                                // Remove the grayed-out effect
                                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                                    numberPicker.setSelectionDividerHeight(0)
                                                                }
                                                            }
                                                        })

                                                    }

                                                    // Keep the current mod value as the one you clicked on
                                                    val currentModValue = modValue.value

                                                    // Display the mod value above the selected index
                                                    val aboveModValue =
                                                        categorizedModValues[selectedTabIndex].getOrNull(
                                                            selectedIndex - 1
                                                        )?.value
                                                    if (aboveModValue != null) {
                                                        Text("Above Mod: $aboveModValue")
                                                    }
                                                    Text(
                                                        text = "Selected Mod: $currentModValue",
                                                        color = Color.Green,
                                                        textDecoration = TextDecoration.Underline,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            },
                                            confirmButton = {
                                                val isSamePosition = selectedIndex == initialIndex
                                                Button(
                                                    onClick = {
                                                        // Handle the move action if the positions are different
                                                        if (!isSamePosition) {
                                                            val updatedList =
                                                                categorizedModValues[selectedTabIndex].toMutableList()
                                                            val currentIndex =
                                                                updatedList.indexOfFirst { it.originalIndex == modValue.originalIndex }

                                                            if (currentIndex != -1) {
                                                                // Implement the logic to move the mod to the new position
                                                                val newPosition = selectedIndex
                                                                Log.d(
                                                                    "MoveDialog",
                                                                    "Moving mod from $currentIndex to $newPosition"
                                                                )
                                                                val movedMod =
                                                                    updatedList.removeAt(currentIndex)
                                                                updatedList.add(newPosition, movedMod)

                                                                // Update originalIndex for all mods
                                                                updatedList.forEachIndexed { index, modValue ->
                                                                    modValue.originalIndex = index
                                                                    Log.d(
                                                                        "MoveDialog",
                                                                        "Mod at index $index has originalIndex ${modValue.originalIndex}"
                                                                    )
                                                                }

                                                                // Update the mod values
                                                                categorizedModValues =
                                                                    categorizedModValues.toMutableList()
                                                                        .apply {
                                                                            this[selectedTabIndex] =
                                                                                updatedList
                                                                        }

                                                                // Update the file with the new positions
                                                                val file = File(Constants.USER_OPENMW_CFG)
                                                                val finalLines =
                                                                    categorizedModValues.flatten()
                                                                        .map { modValue ->
                                                                            "${modValue.category}=${modValue.value}"
                                                                        }
                                                                Log.d(
                                                                    "MoveDialog",
                                                                    "Writing to file: $finalLines"
                                                                )
                                                                file.writeText(finalLines.joinToString("\n") + "\n")

                                                                // Reload the mod values and update the UI
                                                                categorizedModValues = ModValue.updateUIFromModValues(categories)
                                                                Log.d("MoveDialog", "Mod values reloaded")
                                                            } else {
                                                                Log.d(
                                                                    "MoveDialog",
                                                                    "Mod with value not found."
                                                                )
                                                            }
                                                            showMoveDialog = false
                                                            showPopup = false
                                                        } else {
                                                            Log.d(
                                                                "MoveDialog",
                                                                "Selected index is the same as the current index."
                                                            )
                                                        }
                                                    },
                                                    enabled = !isSamePosition
                                                ) {
                                                    Text("Move")
                                                }
                                            },
                                            dismissButton = {
                                                Button(onClick = { showMoveDialog = false }) {
                                                    Text("Cancel")
                                                }
                                            }
                                        )
                                    }

                                    if (showDialog2) {
                                        AlertDialog(
                                            onDismissRequest = { showDialog2 = false },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        val updatedList =
                                                            categorizedModValues[selectedTabIndex].toMutableList()
                                                        val index =
                                                            updatedList.indexOfFirst { it.originalIndex == modValue.originalIndex }
                                                        if (index != -1) {
                                                            val newCategory =
                                                                if (modValue.category == "content") "groundcover" else "content"
                                                            updatedList[index] =
                                                                updatedList[index].copy(category = newCategory)
                                                            categorizedModValues =
                                                                categorizedModValues.toMutableList().apply {
                                                                    this[selectedTabIndex] = updatedList
                                                                }

                                                            // Update the file directly with the new category values
                                                            val file = File(Constants.USER_OPENMW_CFG)
                                                            val existingLines = mutableListOf<String>()
                                                            if (file.exists()) {
                                                                file.forEachLine { line ->
                                                                    if (line.trim().isNotEmpty()) {
                                                                        existingLines.add(line.trim())
                                                                    }
                                                                }
                                                            }

                                                            // Update file with new categories
                                                            val finalLines =
                                                                categorizedModValues.flatten()
                                                                    .map { modValue ->
                                                                        "${modValue.category}=${modValue.value}"
                                                                    }
                                                            file.writeText(finalLines.joinToString("\n") + "\n")

                                                            // Reload the mod values and update the UI
                                                            categorizedModValues = ModValue.updateUIFromModValues(categories)
                                                        }
                                                        showDialog2 = false
                                                    }
                                                ) {
                                                    Text("Yes")
                                                }
                                            },
                                            dismissButton = {
                                                Button(onClick = { showDialog2 = false }) {
                                                    Text("No")
                                                }
                                            },
                                            title = { Text("Confirm Action") },
                                            text = { Text("Are you sure you want to switch the category to ${if (modValue.category == "content") "groundcover" else "content"}?") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight()
                        .width(10.dp)
                ) {
                    Thumb(
                        modifier = Modifier.background(Color.Black.copy(0.3f), RoundedCornerShape(100)),
                        thumbVisibility = ThumbVisibility.HideWhileIdle(
                            enter = fadeIn(),
                            exit = fadeOut(),
                            hideDelay = 0.5.seconds
                        )
                    )
                }
            }
        }
    }


