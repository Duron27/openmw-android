package org.openmw.utils

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.openmw.Constants
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.random.Random

fun importSpecificFile(context: Context, filePattern: String) {
    val downloadFolder = File(Constants.USER_FILE_STORAGE, "OpenMW/Exports")
    val regex = Regex(filePattern)

    try {
        val specificFile = downloadFolder.listFiles { _, name ->
            regex.containsMatchIn(name)
        }?.firstOrNull()

        if (specificFile != null) {
            when {
                specificFile.name == "settings.cfg" -> {
                    // Overwrite settings.cfg
                    specificFile.copyTo(File(Constants.SETTINGS_FILE), overwrite = true)
                    Toast.makeText(context, "File ${specificFile.name} imported successfully", Toast.LENGTH_SHORT).show()
                }
                specificFile.name.endsWith(".omwsave") -> {
                    // Import .omwsave file into a random folder
                    val randomFolderName = "_${Random.nextInt(10, 99)}"
                    val savesDirectory = File(Constants.USER_FILE_STORAGE, "saves")
                    if (!savesDirectory.exists()) {
                        savesDirectory.mkdirs()
                    }
                    val targetSaveFolder = File(savesDirectory, randomFolderName)
                    if (!targetSaveFolder.exists()) {
                        targetSaveFolder.mkdirs()
                    }
                    specificFile.copyTo(File(targetSaveFolder, specificFile.name), overwrite = true)
                    Toast.makeText(context, "Save file ${specificFile.name} imported successfully to $randomFolderName", Toast.LENGTH_SHORT).show()
                }
                specificFile.name == "UI.cfg" -> {
                    // Overwrite UI.cfg
                    specificFile.copyTo(File(Constants.USER_CONFIG, "UI.cfg"), overwrite = true)
                    Toast.makeText(context, "File ${specificFile.name} imported successfully", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(context, "File matching pattern $filePattern not found for import", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "File matching pattern $filePattern not found for import", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun exportCrashAndLogcatFiles(context: Context) {
    val crashFile = File(Constants.CRASH_FILE)
    val logcatFile = File(Constants.LOGCAT_FILE)
    val openmwlog = File(Constants.OPENMW_LOG)
    val downloadFolder = File(Constants.USER_FILE_STORAGE, "OpenMW/Exports")

    try {
        if (crashFile.exists()) {
            crashFile.copyTo(File(downloadFolder, crashFile.name), overwrite = true)
            Toast.makeText(context, "Crash file exported successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Crash file does not exist", Toast.LENGTH_SHORT).show()
        }
        if (crashFile.exists()) {
            crashFile.copyTo(File(downloadFolder, openmwlog.name), overwrite = true)
            Toast.makeText(context, "Openmw Log file exported successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Openmw Log does not exist", Toast.LENGTH_SHORT).show()
        }
        if (logcatFile.exists()) {
            logcatFile.copyTo(File(downloadFolder, logcatFile.name), overwrite = true)
            Toast.makeText(context, "Logcat file exported successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Logcat file does not exist", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ImportDialog(context: Context, onDismiss: () -> Unit) {
    var showDialog by remember { mutableStateOf(true) }
    var step by remember { mutableIntStateOf(1) }
    val backupFiles = File(Constants.USER_FILE_STORAGE, "OpenMW/Exports")
        .listFiles { _, name -> name.startsWith("openmw_") && name.endsWith(".zip") }
        ?.sortedByDescending { it.lastModified() }
        ?.toList() ?: emptyList()
    var selectedBackup by remember { mutableStateOf<File?>(null) }
    var directoriesToImport by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    val selectedDirectories = remember { mutableStateListOf<String>() }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                onDismiss()
            },
            title = { Text(text = if (step == 1) "Select Backup File" else "Select Directories to Import") },
            text = {
                if (step == 1) {
                    Column {
                        backupFiles.forEach { file ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedBackup == file,
                                    onClick = {
                                        selectedBackup = file
                                        // Load directories from the selected ZIP file
                                        selectedBackup?.let { backupFile ->
                                            directoriesToImport = loadDirectoriesFromZip(backupFile)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = file.name)
                            }
                        }
                    }
                } else {
                    Column {
                        directoriesToImport.forEach { (name, path) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedDirectories.contains(path),
                                    onCheckedChange = {
                                        if (selectedDirectories.contains(path)) {
                                            selectedDirectories.remove(path)
                                        } else {
                                            selectedDirectories.add(path)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (step == 1) {
                            step = 2
                        } else {
                            if (selectedBackup != null) {
                                val targetDirectory = File(Constants.USER_FILE_STORAGE)
                                try {
                                    ZipInputStream(FileInputStream(selectedBackup)).use { zis ->
                                        var entry: ZipEntry?
                                        while (zis.nextEntry.also { entry = it } != null) {
                                            val newFile = File(targetDirectory, entry!!.name)
                                            if (selectedDirectories.any { entry!!.name.startsWith(it) }) {
                                                if (entry!!.isDirectory) {
                                                    newFile.mkdirs()
                                                } else {
                                                    FileOutputStream(newFile).use { fos ->
                                                        val buffer = ByteArray(1024)
                                                        var len: Int
                                                        while (zis.read(buffer).also { len = it } > 0) {
                                                            fos.write(buffer, 0, len)
                                                        }
                                                    }
                                                }
                                            }
                                            zis.closeEntry()
                                        }
                                    }
                                    Toast.makeText(context, "Selected directories imported successfully", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Please select a backup file", Toast.LENGTH_SHORT).show()
                            }
                            showDialog = false
                            onDismiss()
                        }
                    }
                ) {
                    Text(if (step == 1) "Next" else "Import")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDialog = false
                    onDismiss()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExportDialog(context: Context, onDismiss: () -> Unit) {
    var showDialog by remember { mutableStateOf(true) }
    val directoriesToZip = listOf(
        "saves" to "${Constants.USER_FILE_STORAGE}/saves",
        "config" to "${Constants.USER_FILE_STORAGE}/config",
        "ui" to "${Constants.USER_FILE_STORAGE}/ui",
        "screenshots" to "${Constants.USER_FILE_STORAGE}/screenshots"
    )
    val selectedDirectories = remember { mutableStateListOf<String>() }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                onDismiss()
            },
            title = { Text(text = "Select Directories to Export") },
            text = {
                Column {
                    directoriesToZip.forEach { (name, path) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedDirectories.contains(path),
                                onCheckedChange = {
                                    if (selectedDirectories.contains(path)) {
                                        selectedDirectories.remove(path)
                                    } else {
                                        selectedDirectories.add(path)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = name)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rootDirectory = File(Constants.USER_FILE_STORAGE)
                        val zipFile = File(
                            File(Constants.USER_FILE_STORAGE, "OpenMW/Exports"),
                            "openmw_${SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())}.zip"
                        )
                        val directoriesToZipFiles = selectedDirectories.map { File(it) }
                        try {
                            zipFilesAndDirectories(rootDirectory, emptyList(), directoriesToZipFiles, zipFile)
                            Toast.makeText(context, "Selected directories zipped and exported to Download/OpenMW/Exports", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        showDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDialog = false
                    onDismiss()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun zipFilesAndDirectories(rootDirectory: File, filesToZip: List<File>, directoriesToZip: List<File>, zipFile: File) {
    try {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // Function to add a file to the zip
            fun addFileToZip(file: File) {
                FileInputStream(file).use { fis ->
                    val zipEntry = ZipEntry(file.relativeTo(rootDirectory).path)
                    zos.putNextEntry(zipEntry)
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (fis.read(buffer).also { length = it } > 0) {
                        zos.write(buffer, 0, length)
                    }
                    zos.closeEntry()
                }
            }

            // Add selected files to the zip
            filesToZip.forEach { file ->
                if (file.exists()) {
                    addFileToZip(file)
                } else {
                    println("File does not exist: ${file.absolutePath}")
                }
            }

            // Add selected directories to the zip
            directoriesToZip.forEach { dir ->
                if (dir.exists()) {
                    dir.walkTopDown().filter { it.isFile }.forEach { file ->
                        addFileToZip(file)
                    }
                } else {
                    println("Directory does not exist: ${dir.absolutePath}")
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadDirectoriesFromZip(backupFile: File): List<Pair<String, String>> {
    val directoriesInZip = mutableSetOf<String>()
    try {
        ZipInputStream(FileInputStream(backupFile)).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                val entryName = entry!!.name
                if (entry!!.isDirectory) {
                    directoriesInZip.add(entryName)
                } else {
                    val dirName = entryName.substringBefore("/")
                    if (dirName.isNotEmpty()) {
                        directoriesInZip.add("$dirName/")
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return directoriesInZip.map { dir ->
        when {
            dir.startsWith("saves") -> "Saves" to "saves"
            dir.startsWith("config") -> "Config" to "config"
            dir.startsWith("ui") -> "UI" to "ui"
            dir.startsWith("screenshots") -> "Screenshots" to "screenshots"
            else -> dir to dir // Default case for unknown directories
        }
    }
}