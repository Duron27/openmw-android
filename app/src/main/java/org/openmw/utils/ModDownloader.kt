/*
@file:Suppress("DEPRECATION")

package org.openmw.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.openmw.Constants
import org.openmw.R
import org.openmw.utils.GameFilesPreferences.NEXUS_API_KEY
import org.openmw.utils.GameFilesPreferences.getCookies
import org.openmw.utils.GameFilesPreferences.saveCookies
import java.io.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@Serializable
data class ModArchive(
    val name: String,
    val for_mod: String,
    val on_lists: List<String>,
    val url: String,
    val author: String?,
    val description: String?,
    val category: String?,
    val dl_url: String?,
    val usage_notes: String?,
    val compat: Int?,
    val dir: String?,
    val slug: String?,
    val date_added: String?,
    val date_updated: String?,
    val tags: List<String>?,
    val data_paths: List<String>?,
    val nexus_file_id: String?,
    val actions: List<String>,
    val file_name: String?,
    var extract_to: String? = null // Make this mutable to set later
)

// Function to extract mod ID from URL
fun extractModIdFromUrl(url: String): String {
    return url.substringAfter("mods/").substringBefore("?")
}

fun getInitialDirectory(dataPath: String): String {
    return dataPath.split("/").firstOrNull() ?: "DefaultDirectory"
}

// Function to fetch HTTP response body
suspend fun fetchResponseBody(url: String): String? {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            return@withContext if (!response.isSuccessful) null else response.body?.string()
        }
    }
}

// Function to get archived Nexus file IDs
suspend fun getArchivedNexusFileIds(modId: String): List<String>? {
    val searchUrl = "https://www.nexusmods.com/morrowind/mods/$modId?tab=files"
    val versionsResponse = fetchResponseBody(
        "http://web.archive.org/cdx/search/cdx?url=$searchUrl"
    ) ?: return null
    val pattern = Pattern.compile("[0-9]{14}")
    val matcher = pattern.matcher(versionsResponse)
    if (matcher.find()) {
        val cacheDate = matcher.group(0)
        val archivedPage = fetchResponseBody(
            "https://web.archive.org/web/$cacheDate/$searchUrl"
        ) ?: return null
        val doc = Jsoup.parse(archivedPage)
        return doc.select("a[href*='file_id']").map { it.attr("href").split("file_id=")[1] }
    }
    return null
}

suspend fun fetchAndParseModData(context: Context): Map<String, List<ModArchive>> {
    return withContext(Dispatchers.IO) {
        val urlToTotalOverhaul = "https://modding-openmw.com/lists/total-overhaul/json"
        val file = downloadAndSaveJson(urlToTotalOverhaul, context)
        file?.let {
            val json = it.readText()
            println("Raw JSON data: $json") // Print raw JSON data for debugging
            try {
                // Parse ModArchive without download_info
                val modArchivesType = object : TypeToken<List<ModArchive>>() {}
                val modArchivesData = Gson().fromJson<List<ModArchive>>(json, modArchivesType.type) ?: emptyList()
                // Extract the listsMap for dropdown menu
                val listsMap = mutableMapOf<String, MutableList<ModArchive>>()
                modArchivesData.forEach { archive ->
                    archive.on_lists.forEach { listName ->
                        listsMap.computeIfAbsent(listName) { mutableListOf() }.add(archive)
                    }
                }
                // Create a map of name to extract_to from download_info
                val extractToMap = modArchivesData.associateBy({ it.file_name }, { it.extract_to })
                // Merge extract_to into ModArchive instances
                modArchivesData.forEach { archive ->
                    archive.extract_to = extractToMap[archive.name]
                }
                mapOf("modArchives" to modArchivesData)
            } catch (e: Exception) {
                println("Failed to parse JSON: ${e.message}")
                e.printStackTrace()
                emptyMap<String, List<ModArchive>>()
            }
        } ?: emptyMap()
    }
}

fun downloadAndSaveJson(url: String, context: Context): File? {
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()
    return try {
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e("ModParse", "Failed to download: $url")
            throw IOException("Unexpected code $response")
        }
        val json = response.body?.string()
        if (json != null) {
            val file = File(context.cacheDir, url.substringAfterLast("/"))

            file.writeText(json)
            Log.d("ModParse", "Saved JSON to ${file.absolutePath}")
            file
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun ModFilterAndDisplay() {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var modData by remember { mutableStateOf<Map<String, List<*>>?>(null) }
    var selectedList by remember { mutableStateOf<String?>(null) }
    var expandedList by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showWebView by remember { mutableStateOf(false) }
    var webViewUrl by remember { mutableStateOf("") }
    var modId by remember { mutableStateOf("") }
    var modName by remember { mutableStateOf("") }
    var dataPath by remember { mutableStateOf("") }
    var downloadStatus by remember { mutableStateOf("") }
    var unzipStatus by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val progress = remember { mutableIntStateOf(0) }
    val isDownloading = remember { mutableStateOf(false) }
    val downloadResult = remember { mutableStateOf<Boolean?>(null) }
    val configFile = File(Constants.USER_OPENMW_CFG)
    val readableSize = remember { MutableStateFlow("") }
    val readableSizeState by readableSize.collectAsState()
    var apiKey by remember { mutableStateOf("") }
    var hasApiKey by remember { mutableStateOf(false) }
    val outputLog = MutableStateFlow("")
    val modMessages by outputLog.collectAsState()

    val blockedModNames = listOf(
        "Buy and Install Morrowind",
        "Install OpenMW",
        "Set your Site Settings",
        "Folder Deploy Script",
        "How to Install Mods",
        "S3LightFixes"
    )

    // Load existing value from DataStore
    LaunchedEffect(Unit) {
        context.dataStore.data.map { preferences ->
            preferences[NEXUS_API_KEY] ?: ""
        }.collect { storedApiKey ->
            apiKey = storedApiKey
            hasApiKey = storedApiKey.isNotEmpty()
        }
    }
    if (showWebView) {
        WebViewPopup(
            url = webViewUrl,
            modName = modName
        )
    }
    Column(
        modifier = Modifier
            .padding(2.dp)
            .fillMaxSize()

    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = {
                isLoading = true
                coroutineScope.launch {
                    modData = fetchAndParseModData(context)
                    isLoading = false
                }
            }) {
                Text("Fetch Lists")
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
                    label = { Text("Nexus API Key") },
                    modifier = Modifier.weight(1f)
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
                    }
                ) {
                    Text("Save")
                }
            }
            if (showWebView) {
                Button(onClick = {
                    showWebView = false
                }) {
                    Text("Close WebView")
                }
            }
        }
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            val regex = Pattern.compile(".*-wip.*", Pattern.CASE_INSENSITIVE)

            val listCounts = modData?.flatMap { (_, entries) ->
                entries.filterIsInstance<ModArchive>().flatMap { it.on_lists }
            }?.filter { !regex.matcher(it).matches() }
                ?.groupingBy { it }?.eachCount() ?: emptyMap()
            // Dropdown menu for selecting list
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { expandedList = true }) {
                        Text(text = selectedList ?: "Select Mod Pack", color = Color.White)
                    }
                    DropdownMenu(
                        expanded = expandedList,
                        onDismissRequest = { expandedList = false }
                    ) {
                        listCounts.forEach { (listName, count) ->
                            DropdownMenuItem(
                                text = { Text("$listName ($count)", color = Color.White) },
                                onClick = {
                                    selectedList = listName
                                    expandedList = false
                                }
                            )
                        }
                    }
                    /*
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        // Iterate through each item in filteredModData
                        val filteredModData = modData?.mapValues { (_, entries) ->
                            entries.filterIsInstance<ModArchive>()
                                .filter { it.on_lists.contains(selectedList) && it.name !in blockedModNames }
                        }?.filterValues { it.isNotEmpty() } ?: emptyMap()

                        coroutineScope.launch {
                            filteredModData.forEach { (key, items) ->
                                items.forEach { item ->
                                    // Perform actions for each item, like downloading and extracting
                                    val fileName = item.url.substringAfterLast("/")
                                    val outputFile = File(Constants.CACHE_DIR, fileName)
                                    val extractionProgress = MutableStateFlow(0f)
                                    val dataPath = item.data_paths?.joinToString() ?: "Data"
                                    val initialDirectory = getInitialDirectory(dataPath)
                                    val extractionDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "OpenMW/Mods/$initialDirectory/")

                                    if (outputFile.exists()) {
                                        extractArchive(outputFile, extractionDir) { extractedSize, totalSize ->
                                            val progress = ((extractedSize * 100) / totalSize).toInt()
                                            extractionProgress.value = extractedSize / totalSize.toFloat()
                                            withContext(Dispatchers.Main) {
                                                // Update UI with extraction progress
                                                outputLog.value += "Extraction progress: $progress%\n"
                                            }
                                        }
                                    } else {
                                        downloadFile(item.url, Constants.CACHE_DIR, { progress ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                // Update UI with download progress
                                                downloadStatus = "Downloading: $progress%"
                                            }
                                        }) { success ->
                                            if (success) {
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    extractArchive(outputFile, extractionDir) { extractedSize, totalSize ->
                                                        val progress = ((extractedSize * 100) / totalSize).toInt()
                                                        extractionProgress.value = extractedSize / totalSize.toFloat()
                                                        withContext(Dispatchers.Main) {
                                                            // Update UI with extraction progress
                                                            outputLog.value += "Extraction progress: $progress%\n"
                                                        }
                                                    }
                                                }
                                            } else {
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    // Update UI with download failure status
                                                    downloadStatus = "Download failed"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }) {
                        Text("Install")
                    }

                     */
                }
            }
            if (selectedList != null) {
                val filteredModData = modData?.mapValues { (_, entries) ->
                    entries.filterIsInstance<ModArchive>()
                        .filter { it.on_lists.contains(selectedList) && it.name !in blockedModNames }
                }?.filterValues { it.isNotEmpty() } ?: emptyMap()
                LazyColumn(
                    modifier = Modifier
                        .padding(2.dp)
                        .background(Color.Black)
                        .weight(1f)
                ) {
                    filteredModData.forEach { (key, items) ->
                        item {
                            HorizontalDivider(color = Color.White, thickness = 1.dp)
                            //Text(text = "$key:", modifier = Modifier.padding(8.dp))
                            items.map { item ->
                                var expanded by remember { mutableStateOf(false) }
                                var expanded2 by remember { mutableStateOf(false) }
                                Column(
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .background(Color.Black)
                                        .clickable { expanded = !expanded }
                                ) {
                                    HorizontalDivider(color = Color.White, thickness = 1.dp)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(8.dp)

                                    ) {
                                        Text(
                                            text = item.name,
                                            color = if (expanded) Color.Green else Color.White
                                        )
                                        // Display the download status
                                        if (downloadStatus.isNotEmpty()) {
                                            Text(
                                                text = "Downloading: $downloadStatus",
                                                color = Color.Yellow
                                            )
                                        }
                                        // Display the unzip status
                                        if (unzipStatus.isNotEmpty()) {
                                            Text(
                                                text = "Unzipping: $unzipStatus",
                                                color = Color.Cyan
                                            )
                                        }
                                    }
                                    if (expanded) {
                                        Column(
                                            modifier = Modifier
                                                .padding(2.dp)
                                                .fillMaxWidth()
                                                .background(Color.DarkGray)
                                        ) {
                                            item.category?.let { Text(text = "Category: $it\n") }
                                            Text(
                                                text = item.url,
                                                modifier = Modifier.clickable {
                                                    webViewUrl = item.url
                                                    showWebView = true
                                                    modId = extractModIdFromUrl(item.url)
                                                    modName = item.name
                                                },
                                                color = Color.White
                                            )
                                            item.description?.let { Text(text = "\nDescription:\n $it\n") }
                                            Column {
                                                val fileName = item.url.substringAfterLast("/")
                                                val outputFile = File(Constants.CACHE_DIR, fileName)
                                                val fileExists = outputFile.exists()
                                                val extractionProgress = remember { MutableStateFlow(0f) }
                                                val isExtracting = remember { mutableStateOf(false) }
                                                val dataPath = item.data_paths?.joinToString() ?: "Data"
                                                val uncompressProgress by extractionProgress.collectAsState()
                                                val initialDirectory = getInitialDirectory(dataPath)
                                                val extractionDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "OpenMW/Mods/$initialDirectory/")
                                                var totalStorageUsed = getDirectorySize(extractionDir)
                                                var updatedReadableSize = humanReadableByteCountBin(totalStorageUsed)
                                                val addModDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "OpenMW/Mods/")
                                                val extensions = arrayOf("bsa", "esm", "esp", "esl", "omwaddon", "omwgame", "omwscripts")
                                                val scrollState = rememberScrollState()
                                                LaunchedEffect(modMessages) {
                                                    scrollState.animateScrollTo(scrollState.maxValue)
                                                }
                                                LaunchedEffect(Unit) {
                                                    // Update readableSize
                                                    readableSize.value = updatedReadableSize
                                                }

                                                when {
                                                    item.url.contains("github", ignoreCase = true) -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .wrapContentHeight()
                                                                .heightIn(max = 200.dp)
                                                                .background(Color.White)
                                                                .padding(8.dp)
                                                                .verticalScroll(scrollState)
                                                        ) {
                                                            Text(
                                                                text = modMessages,
                                                                color = Color.Black,
                                                                fontSize = 14.sp,
                                                                modifier = Modifier.padding(4.dp)
                                                            )
                                                        }
                                                        Button(onClick = {
                                                            if (fileExists) {
                                                                // Handle file removal
                                                                outputFile.delete()
                                                                extractionDir.deleteRecursively()

                                                                // Update config file to reverse changes
                                                                if (configFile.exists()) {

                                                                    // Handle file removal
                                                                    outputFile.delete()
                                                                    extractionDir.deleteRecursively()

                                                                    // Update config file to reverse changes
                                                                    if (configFile.exists()) {


                                                                        fun searchFilesWithExtensions(directory: File, extensions: Array<String>): List<String> {
                                                                            val files = mutableListOf<String>()

                                                                            // Debug logs
                                                                            outputLog.value += "Searching in directory: ${directory.absolutePath}\n"
                                                                            outputLog.value += "Using extensions: ${extensions.joinToString(", ")}\n"

                                                                            directory.walkTopDown().forEach { file ->
                                                                                if (file.isFile && extensions.any { ext -> file.extension.equals(ext, ignoreCase = true) }) {
                                                                                    files.add(file.absolutePath) // Use file.absolutePath to get the full path
                                                                                    outputLog.value += "File found: ${file.absolutePath}\n" // Log the found file
                                                                                } else {
                                                                                    outputLog.value += "Skipping: ${file.absolutePath}\n"
                                                                                }
                                                                            }
                                                                            return files
                                                                        }

                                                                        val configFileContent = configFile.readLines()

                                                                        // Collect data lines to be removed
                                                                        val dataLinesToRemove = item.data_paths?.map { "data=$addModDir/$it" }.orEmpty()

                                                                        // Collect content lines to be removed
                                                                        val contentLinesToRemove = item.data_paths?.flatMap { dataPath ->
                                                                            val dataPathDir = File(addModDir, dataPath)
                                                                            searchFilesWithExtensions(dataPathDir, extensions).map { "content=$it" }
                                                                        }.orEmpty()

                                                                        // Combine both sets of lines to remove
                                                                        val linesToRemove = dataLinesToRemove + contentLinesToRemove

                                                                        val newContent = configFileContent.filterNot { line ->
                                                                            linesToRemove.any { lineToRemove -> line.equals(lineToRemove, ignoreCase = true) }
                                                                        }.joinToString("\n")

                                                                        configFile.writeText(newContent)
                                                                    }

                                                                }
                                                                // Update config file to reverse changes

                                                                downloadResult.value =
                                                                    null  // Reset download result
                                                            } else {
                                                                scope.launch {
                                                                    isDownloading.value = true
                                                                    downloadFile(
                                                                        item.url,
                                                                        Constants.CACHE_DIR,
                                                                        onProgress = {
                                                                            progress.intValue = it
                                                                        },
                                                                        onResult = {
                                                                            isDownloading.value =
                                                                                false
                                                                            downloadResult.value =
                                                                                it
                                                                        })
                                                                }
                                                            }
                                                        }) {
                                                            Text(if (isDownloading.value) "Downloading...${progress.intValue}%" else if (fileExists) "Remove ${item.name}" else "Download ${item.name}")
                                                        }

                                                        HorizontalDivider(thickness = 1.dp)

                                                        downloadResult.value?.let {
                                                            if (it) {
                                                                Text("Download complete!")
                                                                if (!extractionDir.exists()) {
                                                                    Button(onClick = {
                                                                        CoroutineScope(Dispatchers.IO).launch {
                                                                            isExtracting.value =
                                                                                true
                                                                            un7zip(
                                                                                outputFile.path,
                                                                                extractionDir.path
                                                                            ) { extractedSize, totalSize ->
                                                                                extractionProgress.value =
                                                                                    extractedSize / totalSize.toFloat()
                                                                            }
                                                                            outputFile.delete()

                                                                            isExtracting.value = false

                                                                            val configFileContent = configFile.readLines()
                                                                            val uniqueLines = mutableSetOf<String>()
                                                                            val existingContentLines = configFileContent.filter { it.startsWith("content=") }
                                                                            uniqueLines.addAll(existingContentLines)
                                                                            val dataPathsContentLines = mutableSetOf<String>()
                                                                            fun searchFilesWithExtensions(directory: File, extensions: Array<String>): List<String> {
                                                                                val files = mutableListOf<String>()
                                                                                // Debug logs
                                                                                CoroutineScope(Dispatchers.Main).launch {
                                                                                    outputLog.value += "Searching in directory: ${directory.absolutePath}\n"
                                                                                    outputLog.value += "Using extensions: ${extensions.joinToString(", ")}\n"
                                                                                }
                                                                                directory.walkTopDown().forEach { file ->
                                                                                    if (file.isFile && extensions.any { ext -> file.extension.equals(ext, ignoreCase = true) }) {
                                                                                        files.add(file.absolutePath) // Use file.absolutePath to get the full path
                                                                                        CoroutineScope(Dispatchers.Main).launch {
                                                                                            outputLog.value += "File found: ${file.absolutePath}\n"
                                                                                        }
                                                                                    } else {
                                                                                        CoroutineScope(Dispatchers.Main).launch {
                                                                                            outputLog.value += "Skipping: ${file.absolutePath}\n"
                                                                                        }
                                                                                    }
                                                                                }
                                                                                return files
                                                                            }

                                                                            val newContent = buildString {
                                                                                item.data_paths?.forEach { dataPath ->
                                                                                    val dataLine = "data=$addModDir/$dataPath"
                                                                                    if (uniqueLines.add(dataLine)) {
                                                                                        append("$dataLine\n")
                                                                                        CoroutineScope(Dispatchers.Main).launch {
                                                                                            outputLog.value += "Data path added: $dataLine\n"
                                                                                        }
                                                                                    }
                                                                                    // Search for files with specific extensions in the data path
                                                                                    val dataPathDir = File(addModDir, dataPath)
                                                                                    val modFiles = searchFilesWithExtensions(dataPathDir, extensions)
                                                                                    modFiles.forEach { modFile ->
                                                                                        val fileNameC = modFile.substringAfterLast('/')
                                                                                        val contentLine = "content=$fileNameC"
                                                                                        if (uniqueLines.add(contentLine)) {
                                                                                            dataPathsContentLines.add(contentLine)
                                                                                            CoroutineScope(Dispatchers.Main).launch {
                                                                                                outputLog.value += "Content file added: $contentLine\n"
                                                                                            }
                                                                                        }
                                                                                    }

                                                                                }
                                                                                // Append the rest of the configFile content, excluding lines matching any data_path
                                                                                append(configFileContent.filterNot { line ->
                                                                                    item.data_paths?.any { dataPath -> line == "data=$addModDir/$dataPath" } ?: false
                                                                                }.joinToString("\n")
                                                                                )

                                                                                    // Append new content lines found in data paths without duplicates
                                                                                    if (dataPathsContentLines.isNotEmpty()) {
                                                                                        append("\n")
                                                                                        dataPathsContentLines.forEach { line ->
                                                                                            append(line).append("\n")
                                                                                        }
                                                                                    }
                                                                                }

                                                                                configFile.writeText(newContent)


                                                                            // Update readableSize
                                                                            readableSize.value = updatedReadableSize
                                                                        }
                                                                    }) {
                                                                        Text(
                                                                            when {
                                                                                isExtracting.value -> "Installing...${(uncompressProgress * 100).toInt()}%"
                                                                                extractionDir.exists() -> "Installed!"
                                                                                else -> "Install ${item.name}"
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        Text("Total storage used by ${item.name}: $readableSizeState")
                                                    }

                                                    item.url.contains("nexus", ignoreCase = true) -> {

                                                        Column {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .wrapContentHeight()
                                                                    .heightIn(max = 200.dp)
                                                                    .background(Color.White)
                                                                    .padding(8.dp)
                                                                    .verticalScroll(scrollState)
                                                            ) {
                                                                Text(
                                                                    text = modMessages,
                                                                    color = Color.Black,
                                                                    fontSize = 14.sp,
                                                                    modifier = Modifier.padding(4.dp)
                                                                )
                                                            }
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                            ) {
                                                                Button(onClick = {
                                                                    outputLog.value = ""
                                                                    if (apiKey.isNotEmpty()) {
                                                                        scope.launch(Dispatchers.IO) {
                                                                            val outputFile = File(Constants.CACHE_DIR, fileName)
                                                                            val client = OkHttpClient()

                                                                            val url = "https://api.nexusmods.com/v1/games/Morrowind/mods/${extractModIdFromUrl(item.url)}/files.json"
                                                                            val request = Request.Builder()
                                                                                .url(url)
                                                                                .header("User-Agent", "Mozilla/5.0 (Windows NT 6.0) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.97 Safari/537.11")
                                                                                .header("accept", "application/json")
                                                                                .header("apikey", apiKey)
                                                                                .build()

                                                                            client.newCall(request).execute().use { response ->
                                                                                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                                                                                response.body?.byteStream()?.use { inputStream ->
                                                                                    val outputStream = FileOutputStream(outputFile)
                                                                                    val buffer = ByteArray(1024)
                                                                                    var bytesRead: Int

                                                                                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                                                                        outputStream.write(buffer, 0, bytesRead)
                                                                                    }
                                                                                    outputStream.flush()
                                                                                }

                                                                                // Log the contents of the file
                                                                                val reader = outputFile.bufferedReader()
                                                                                var line: String?
                                                                                while (reader.readLine().also { line = it } != null) {
                                                                                    withContext(Dispatchers.Main) {
                                                                                        outputLog.value += "$line\n"
                                                                                    }
                                                                                }

                                                                                // Parse the JSON file
                                                                                val jsonString = outputFile.readText()
                                                                                val jsonObject = JSONObject(jsonString)
                                                                                val filesArray = jsonObject.getJSONArray("files")
                                                                                var fileId: Long? = null
                                                                                var fileName: String? = null
                                                                                var largestSize: Long = 0
                                                                                for (i in 0 until filesArray.length()) {
                                                                                    val fileObject = filesArray.getJSONObject(i)
                                                                                    val fileSize = fileObject.getLong("size")
                                                                                    if (fileSize > largestSize) {
                                                                                        largestSize = fileSize
                                                                                        fileId = fileObject.getLong("file_id")
                                                                                        fileName = fileObject.getString("file_name")
                                                                                    }
                                                                                }

                                                                                // Optionally, log the largest file details
                                                                                withContext(Dispatchers.Main) {
                                                                                    outputLog.value += "Largest file ID: $fileId, Name: $fileName, Size: $largestSize\n"
                                                                                }
                                                                                if (fileId != null) {
                                                                                    scope.launch(Dispatchers.IO) {
                                                                                        val downloadLinkOutputFile = File(Constants.CACHE_DIR, "download_link.json")
                                                                                        val client = OkHttpClient()

                                                                                        val url =
                                                                                            "https://api.nexusmods.com/v1/games/Morrowind/mods/${extractModIdFromUrl(item.url)}/files/$fileId/download_link.json"
                                                                                        val request = Request.Builder()
                                                                                            .url(url)
                                                                                            .header(
                                                                                                "User-Agent",
                                                                                                "Mozilla/5.0 (Windows NT 6.0) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.97 Safari/537.11"
                                                                                            )
                                                                                            .header("accept", "application/json")
                                                                                            .header("apikey", apiKey)
                                                                                            .build()

                                                                                        client.newCall(request).execute().use { response ->
                                                                                            if (!response.isSuccessful) throw IOException("Unexpected code $response")

                                                                                            response.body?.byteStream()?.use { inputStream ->
                                                                                                val outputStream = FileOutputStream(downloadLinkOutputFile)
                                                                                                val buffer = ByteArray(1024)
                                                                                                var bytesRead: Int
                                                                                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                                                                                    outputStream.write(buffer, 0, bytesRead)
                                                                                                }
                                                                                                outputStream.flush()
                                                                                            }
                                                                                        }


                                                                                        // After downloading, log the file contents
                                                                                        withContext(Dispatchers.Main) {
                                                                                            val reader = downloadLinkOutputFile.bufferedReader()
                                                                                            var line: String?
                                                                                            while (reader.readLine().also { line = it } != null) {
                                                                                                outputLog.value += "$line\n"
                                                                                            }
                                                                                        }


                                                                                        // Reading the file content and logging
                                                                                        val downloadLinkReader = downloadLinkOutputFile.bufferedReader()
                                                                                        var downloadLinkLine: String?
                                                                                        while (withContext(Dispatchers.IO) {
                                                                                                downloadLinkReader.readLine()
                                                                                            }.also { downloadLinkLine = it } != null) {
                                                                                            outputLog.value += "$downloadLinkLine\n"
                                                                                        }
                                                                                        // Log the downloaded link data to Logcat
                                                                                        val downloadLinkJsonString = downloadLinkOutputFile.readText()
                                                                                        // Parse the download link JSON file and extract the URI links
                                                                                        val uriList = mutableListOf<String>()
                                                                                        val downloadLinkArray = JSONArray(downloadLinkJsonString)
                                                                                        for (i in 0 until downloadLinkArray.length()) {
                                                                                            val linkObject = downloadLinkArray.getJSONObject(i)
                                                                                            val uri = linkObject.getString("URI")
                                                                                            uriList.add(uri)
                                                                                        }
                                                                                        // Log the extracted URIs to Logcat
                                                                                        uriList.forEach { uri ->
                                                                                            outputLog.value += "Download URI: $uri\n"
                                                                                        }

                                                                                        // Use one of the URI links to download the actual file (example using the first URI)
                                                                                        if (uriList.isNotEmpty()) {
                                                                                            val downloadUri = uriList[0]
                                                                                            // Encode the downloadUri to replace spaces with %20
                                                                                            val encodedDownloadUri = downloadUri.replace(" ", "%20")

                                                                                            val actualFileOutput = File(Constants.CACHE_DIR, fileName ?: "downloaded_file")
                                                                                            val client = OkHttpClient()

                                                                                            val request = Request.Builder()
                                                                                                .url(encodedDownloadUri)
                                                                                                .header(
                                                                                                    "User-Agent",
                                                                                                    "Mozilla/5.0 (Windows NT 6.0) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.97 Safari/537.11"
                                                                                                )
                                                                                                .header("accept", "application/json")
                                                                                                .header("apikey", System.getenv("apiKey") ?: "")
                                                                                                .build()

                                                                                            withContext(Dispatchers.IO) {
                                                                                                client.newCall(request).execute().use { response ->
                                                                                                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                                                                                                    val fileLength = response.body?.contentLength() ?: -1L
                                                                                                    response.body?.byteStream()?.use { inputStream ->
                                                                                                        val outputStream = FileOutputStream(actualFileOutput)
                                                                                                        val buffer = ByteArray(32768)
                                                                                                        var totalBytesRead: Long = 0
                                                                                                        var bytesRead: Int
                                                                                                        var lastProgress = 0
                                                                                                        var lastUpdateTime = System.currentTimeMillis()
                                                                                                        var lastBytesRead = 0L

                                                                                                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                                                                                            outputStream.write(buffer, 0, bytesRead)
                                                                                                            totalBytesRead += bytesRead

                                                                                                            val currentTime = System.currentTimeMillis()
                                                                                                            if (currentTime - lastUpdateTime >= 1000) { // Update every second
                                                                                                                val bytesReadInLastSecond = totalBytesRead - lastBytesRead
                                                                                                                val speedInBytesPerSecond = bytesReadInLastSecond / ((currentTime - lastUpdateTime) / 1000.0)
                                                                                                                val speedInKbps = speedInBytesPerSecond / 1024.0
                                                                                                                lastUpdateTime = currentTime
                                                                                                                lastBytesRead = totalBytesRead

                                                                                                                withContext(Dispatchers.Main) {
                                                                                                                    outputLog.value += "Download speed: %.2f KB/s\n".format(speedInKbps)
                                                                                                                }
                                                                                                            }

                                                                                                            if (fileLength > 0) {
                                                                                                                val progress = ((totalBytesRead * 100) / fileLength).toInt()
                                                                                                                if (progress != lastProgress) {
                                                                                                                    lastProgress = progress
                                                                                                                    withContext(Dispatchers.Main) {
                                                                                                                        outputLog.value += "Download progress: $progress%\n"
                                                                                                                    }
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                        outputStream.flush()
                                                                                                    }
                                                                                                }
                                                                                            }


                                                                                            // Delete the two files in the cache directory
                                                                                            val downloadLinkFile = File(Constants.CACHE_DIR, "download_link.json")
                                                                                            val modFile = File(Constants.CACHE_DIR, extractModIdFromUrl(item.url))
                                                                                            if (downloadLinkFile.exists()) {
                                                                                                downloadLinkFile.delete()
                                                                                            }
                                                                                            if (modFile.exists()) {
                                                                                                modFile.delete()
                                                                                            }


                                                                                            CoroutineScope(Dispatchers.IO).launch {
                                                                                                val extractionJob = when (actualFileOutput.extension) {
                                                                                                    "7z" -> async {
                                                                                                        un7zip(
                                                                                                            actualFileOutput.absolutePath,
                                                                                                            extractionDir.path
                                                                                                        ) { extractedSize, totalSize ->
                                                                                                            val progress = ((extractedSize * 100) / totalSize).toInt()
                                                                                                            extractionProgress.value = extractedSize / totalSize.toFloat()
                                                                                                            CoroutineScope(Dispatchers.Main).launch {
                                                                                                                outputLog.value += "Extraction progress: $progress%\n"
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                    // Add cases for other file types here using the previously shared code
                                                                                                    else -> async {
                                                                                                        extractFile(
                                                                                                            actualFileOutput,
                                                                                                            extractionDir
                                                                                                        ) { extractedSize, totalSize ->
                                                                                                            val progress = ((extractedSize * 100) / totalSize).toInt()
                                                                                                            extractionProgress.value = extractedSize / totalSize.toFloat()
                                                                                                            CoroutineScope(Dispatchers.Main).launch {
                                                                                                                outputLog.value += "Extraction progress: $progress%\n"
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }


                                                                                                // Wait for extraction to complete
                                                                                                extractionJob.await()

                                                                                                val configFileContent = configFile.readLines()
                                                                                                val uniqueLines = mutableSetOf<String>()
                                                                                                val existingContentLines = configFileContent.filter { it.startsWith("content=") }
                                                                                                uniqueLines.addAll(existingContentLines)
                                                                                                val dataPathsContentLines = mutableSetOf<String>()
                                                                                                fun searchFilesWithExtensions(directory: File, extensions: Array<String>): List<String> {
                                                                                                    val files = mutableListOf<String>()
                                                                                                    // Debug logs
                                                                                                    CoroutineScope(Dispatchers.Main).launch {
                                                                                                        outputLog.value += "Searching in directory: ${directory.absolutePath}\n"
                                                                                                        outputLog.value += "Using extensions: ${extensions.joinToString(", ")}\n"
                                                                                                    }
                                                                                                    directory.walkTopDown().forEach { file ->
                                                                                                        if (file.isFile && extensions.any { ext -> file.extension.equals(ext, ignoreCase = true) }) {
                                                                                                            files.add(file.absolutePath) // Use file.absolutePath to get the full path
                                                                                                            CoroutineScope(Dispatchers.Main).launch {
                                                                                                                outputLog.value += "File found: ${file.absolutePath}\n"
                                                                                                            }
                                                                                                        } else {
                                                                                                            CoroutineScope(Dispatchers.Main).launch {
                                                                                                                outputLog.value += "Skipping: ${file.absolutePath}\n"
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                    return files
                                                                                                }

                                                                                                val newContent = buildString {
                                                                                                    item.data_paths?.forEach { dataPath ->
                                                                                                        val dataLine = "data=$addModDir/$dataPath"
                                                                                                        if (uniqueLines.add(dataLine)) {
                                                                                                            append("$dataLine\n")
                                                                                                            CoroutineScope(Dispatchers.Main).launch {
                                                                                                                outputLog.value += "Data path added: $dataLine\n"
                                                                                                            }
                                                                                                        }
                                                                                                        // Search for files with specific extensions in the data path
                                                                                                        val dataPathDir = File(addModDir, dataPath)
                                                                                                        val modFiles = searchFilesWithExtensions(dataPathDir, extensions)
                                                                                                        modFiles.forEach { modFile ->
                                                                                                            val fileNameC = modFile.substringAfterLast('/')
                                                                                                            val contentLine = "content=$fileNameC"
                                                                                                            if (uniqueLines.add(contentLine)) {
                                                                                                                dataPathsContentLines.add(contentLine)
                                                                                                                CoroutineScope(Dispatchers.Main).launch {
                                                                                                                    outputLog.value += "Content file added: $contentLine\n"
                                                                                                                }
                                                                                                            }
                                                                                                        }

                                                                                                    }
                                                                                                    // Append the rest of the configFile content, excluding lines matching any data_path
                                                                                                    append(configFileContent.filterNot { line ->
                                                                                                        item.data_paths?.any { dataPath -> line == "data=$addModDir/$dataPath" } ?: false
                                                                                                    }.joinToString("\n")
                                                                                                    )
                                                                                                    // Append new content lines found in data paths without duplicates
                                                                                                    if (dataPathsContentLines.isNotEmpty()) {
                                                                                                        append("\n")
                                                                                                        dataPathsContentLines.forEach { line ->
                                                                                                            append(line).append("\n")
                                                                                                            CoroutineScope(Dispatchers.Main).launch {
                                                                                                                outputLog.value += "Final content line: $line\n"
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                                configFile.writeText(newContent)
                                                                                                // Log the completion of the update
                                                                                                CoroutineScope(Dispatchers.Main).launch {
                                                                                                    outputLog.value += "Config file updated with new content.\n"
                                                                                                    outputLog.value += "File downloaded to: ${actualFileOutput.absolutePath}\n"
                                                                                                    outputLog.value += "Extraction directory: ${extractionDir.absolutePath}\n"
                                                                                                    outputLog.value += "Total storage used: $updatedReadableSize\n"
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }) {
                                                                    Text("Download and Install")
                                                                }
                                                                Button(onClick = {
                                                                    if (fileExists) {
                                                                        // Handle file removal
                                                                        outputFile.delete()
                                                                        extractionDir.deleteRecursively()
                                                                        // Update config file to reverse changes
                                                                        if (configFile.exists()) {
                                                                            val configFileContent = configFile.readLines()
                                                                            fun searchFilesWithExtensions(directory: File, extensions: Array<String>): List<String> {
                                                                                val files = mutableListOf<String>()
                                                                                // Debug logs
                                                                                outputLog.value += "Searching in directory: ${directory.absolutePath}\n"
                                                                                outputLog.value += "Using extensions: ${extensions.joinToString(", ")}\n"
                                                                                directory.walkTopDown().forEach { file ->
                                                                                    if (file.isFile && extensions.any { ext -> file.extension.equals(ext, ignoreCase = true) }) {
                                                                                        files.add(file.absolutePath) // Use file.absolutePath to get the full path
                                                                                        outputLog.value += "File found: ${file.absolutePath}\n" // Log the found file
                                                                                    } else {
                                                                                        outputLog.value += "Skipping: ${file.absolutePath}\n"
                                                                                    }
                                                                                }
                                                                                return files
                                                                            }
                                                                            // Collect data lines to be removed
                                                                            val dataLinesToRemove = item.data_paths?.map { "data=$addModDir/$it" }.orEmpty()
                                                                            // Collect content lines to be removed
                                                                            val contentLinesToRemove = item.data_paths?.flatMap { dataPath ->

                                                                                val dataPathDir = File(addModDir, dataPath)
                                                                                searchFilesWithExtensions(dataPathDir, extensions).map { "content=$it" }
                                                                            }.orEmpty()
                                                                            // Combine both sets of lines to remove
                                                                            val linesToRemove = dataLinesToRemove + contentLinesToRemove
                                                                            val newContent = configFileContent.filterNot { line ->
                                                                                linesToRemove.any { lineToRemove -> line.equals(lineToRemove, ignoreCase = true) }
                                                                            }.joinToString("\n")
                                                                            configFile.writeText(newContent)
                                                                        }
                                                                    }
                                                                }) {
                                                                    Text("Remove")
                                                                }
                                                            }
                                                            HorizontalDivider(thickness = 1.dp)
                                                        }
                                                    }
                                                    else -> {
                                                        Text("Working on it!")
                                                    }
                                                }
                                            }
                                            HorizontalDivider(thickness = 1.dp)
                                            // "More Info" clickable text
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            ) {
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text(
                                                    text = if (expanded2) "Less Info" else "More Info",
                                                    color = Color.White,
                                                    modifier = Modifier.clickable { expanded2 = !expanded2 }
                                                )
                                            }
                                            // Expanded section
                                            if (expanded2) {
                                                Column(
                                                    modifier = Modifier
                                                        .padding(2.dp)
                                                        .fillMaxWidth()
                                                ) {
                                                    item.tags?.let { Text(text = "Tags: ${it.joinToString()}\n") }
                                                    item.compat?.let { Text(text = "Compatibility: $it") }
                                                    item.dir?.let { Text(text = "Directory: $it\n") }
                                                    item.date_added?.let { Text(text = "Date Added: $it") }
                                                    item.date_updated?.let { Text(text = "Date Updated: $it\n") }
                                                    item.data_paths?.let { Text(text = "Data Paths: ${it.joinToString()}") }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
@Composable
fun WebViewPopup(
    url: String,
    modName: String
) {
    var completeUrl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Update completeUrl based on the input URL
    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            if (url.contains("https://www.nexusmods.com/morrowind/mods/")) {
                val modId = extractModIdFromUrl(url)
                val fileId = getArchivedNexusFileIds(modId)?.firstOrNull()
                if (fileId != null) {
                    completeUrl = "https://www.nexusmods.com/morrowind/mods/$modId?tab=files&file_id=$fileId&nmm=1"
                } else {
                    completeUrl = url // Fallback to the original URL if fileId is not found
                }
            } else {
                completeUrl = url // Keep other URLs untouched
            }
        }
    }

    completeUrl?.let {
        Box(
            modifier = Modifier
                .padding(1.dp)
                .fillMaxWidth()
                .height(400.dp)
        ) {
            Column {
                Text(modName, style = MaterialTheme.typography.bodySmall)
                Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                AndroidView(factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                view?.loadUrl(request?.url.toString())
                                return true
                            }
                        }
                        // Enable cookies
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        // Load cookies from DataStore
                        scope.launch {
                            val cookies = getCookies(context)
                            cookies.forEach { cookie ->
                                cookieManager.setCookie("https://www.nexusmods.com", cookie)
                            }
                        }

                        // Save cookies when the WebView is loaded
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val newCookies = cookieManager.getCookie(url)
                                val cookieSet = newCookies?.split(";")?.toSet() ?: emptySet()
                                scope.launch {
                                    saveCookies(context, cookieSet)
                                }
                            }
                        }

                        loadUrl(it)
                    }
                })
            }
        }
    }
}

suspend fun downloadFile(url: String, directory: File, onProgress: (Int) -> Unit, onResult: (Boolean) -> Unit) {
    withContext(Dispatchers.IO) {
        val fileName = url.substringAfterLast("/")
        val outputFile = File(directory, fileName)
        if (outputFile.exists()) {
            onResult(false)  // Indicates the file already exists
            return@withContext
        }
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connect()
        val fileLength = connection.contentLengthLong
        val inputStream: InputStream = connection.inputStream
        val outputStream = FileOutputStream(outputFile)
        var totalBytesRead: Long = 0
        val buffer = ByteArray(65536)
        inputStream.use { input ->
            outputStream.use { output ->
                while (true) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) break
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (fileLength > 0) {
                        val progress = ((totalBytesRead * 100) / fileLength).toInt()
                        onProgress(progress)
                    }

                }
            }
        }
        onResult(true)  // Indicates the file was downloaded successfully
    }
}

fun un7zip(
    sevenZFilePath: String,
    destDirectory: String,
    onProgress: (Long, Long) -> Unit
) {
    val destDir = File(destDirectory)
    if (!destDir.exists()) {
        destDir.mkdirs()
    }
    try {
        val sevenZFile = SevenZFile(File(sevenZFilePath))
        val entries = mutableListOf<SevenZArchiveEntry>()
        var totalSize: Long = 0
        while (true) {
            val entry = sevenZFile.nextEntry ?: break
            entries.add(entry)
            totalSize += entry.size
        }
        var extractedSize: Long = 0
        for (entry in entries) {
            val filePath = destDirectory + File.separator + entry.name
            try {
                if (entry.isDirectory) {
                    File(filePath).mkdirs()
                } else {
                    val entryFile = File(filePath)
                    entryFile.parentFile?.mkdirs()  // Ensure the parent directories exist
                    entryFile.outputStream().use { outputStream ->
                        sevenZFile.getInputStream(entry).copyTo(outputStream)
                    }
                    extractedSize += entry.size
                    onProgress(extractedSize, totalSize)
                }
            } catch (e: IOException) {
                Log.e("un7zip", "Error extracting entry: ${entry.name}, path: $filePath: ${e.message}")
            }
        }
        sevenZFile.close()
    } catch (e: IOException) {
        Log.e("un7zip", "Error opening 7z file: ${e.message}")
    }
}

suspend fun extractFile(
    inputFile: File,
    outputDir: File,
    progressCallback: (Long, Long) -> Unit
) = withContext(Dispatchers.IO) {
    val buffer = ByteArray(65536)
    val totalSize = inputFile.length()
    var extractedSize = 0L

    if (inputFile.extension == "zip") {
        // Handle ZIP file extraction
        ZipInputStream(FileInputStream(inputFile)).use { zipIn ->
            var entry: ZipEntry? = zipIn.nextEntry
            while (entry != null) {
                val filePath = File(outputDir, entry.name)
                if (entry.isDirectory) {
                    filePath.mkdirs()
                } else {
                    BufferedOutputStream(FileOutputStream(filePath)).use { bos ->
                        var len: Int
                        while (zipIn.read(buffer).also { len = it } != -1) {
                            bos.write(buffer, 0, len)
                            extractedSize += len
                            withContext(Dispatchers.Main) {
                                progressCallback(extractedSize, totalSize)
                            }
                        }
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    } else {
        // Handle other types of archives
        BufferedInputStream(FileInputStream(inputFile)).use { fis ->
            val inputStream = when (val ext = inputFile.extension) {
                "gz", "bz2", "xz", "lzma" -> CompressorStreamFactory().createCompressorInputStream(fis)
                else -> fis
            }
            val archiveStream = ArchiveStreamFactory().createArchiveInputStream(inputStream)
            var entry: ArchiveEntry?

            while (archiveStream.nextEntry.also { entry = it } != null) {
                val outFile = File(outputDir, entry!!.name)

                if (entry!!.isDirectory) {
                    if (!outFile.exists()) {
                        outFile.mkdirs()
                    }
                } else {
                    BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
                        var len: Int
                        while (archiveStream.read(buffer).also { len = it } != -1) {
                            bos.write(buffer, 0, len)
                            extractedSize += len
                            withContext(Dispatchers.Main) {
                                progressCallback(extractedSize, totalSize)
                            }
                        }
                    }
                }
            }
        }
    }
}


 */