@file:Suppress("DEPRECATION")

package org.openmw.internet

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openmw.Constants
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

fun getFolderSize(folder: File): Long {
    var totalSize: Long = 0
    if (folder.isDirectory) {
        folder.listFiles()?.forEach { file ->
            totalSize += if (file.isFile) {
                file.length()
            } else {
                getFolderSize(file)
            }
        }
    }
    return totalSize
}

fun formatSize(size: Long): String {
    val kb = size / 1024
    val mb = kb / 1024
    val gb = mb / 1024
    return when {
        gb > 0 -> "$gb GB"
        mb > 0 -> "$mb MB"
        kb > 0 -> "$kb KB"
        else -> "$size B"
    }
}

suspend fun getFileSize(url: String): Long = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connect()
    val size = connection.contentLengthLong
    connection.disconnect()
    size
}

fun extractZip(zipFilePath: String, destDirPath: String) {
    val destDir = File(destDirPath)
    if (!destDir.exists()) {
        destDir.mkdirs()
    }

    try {
        ZipInputStream(File(zipFilePath).inputStream()).use { zipInputStream ->
            var entry: ZipEntry?
            while (zipInputStream.nextEntry.also { entry = it } != null) {
                entry?.let { zipEntry ->
                    val newFile = File(destDir, zipEntry.name)
                    if (zipEntry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        // Create parent directories if they don't exist
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { output ->
                            zipInputStream.copyTo(output)
                        }
                    }
                }
            }
        }
    } catch (e: ZipException) {
        Log.e("extractZip", "Error extracting ZIP file", e)
    } catch (e: IOException) {
        Log.e("extractZip", "I/O error during extraction", e)
    }
}

@Composable
fun DownloadButton() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var progress by remember { mutableIntStateOf(0) }
    var isDownloading by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var fileExists by remember { mutableStateOf(false) }
    var folderSize by remember { mutableStateOf(0L) }
    var zipFileSize by remember { mutableStateOf(0L) }

    val filePath = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "game-icons.zip"
    )

    val folderPath = File("${Constants.USER_FILE_STORAGE}/icons")
    val zipUrl = "https://game-icons.net/archives/png/zip/ffffff/000000/game-icons.net.png.zip"

    LaunchedEffect(Unit) {
        zipFileSize = getFileSize(zipUrl)
        fileExists = filePath.exists()
        folderSize = if (folderPath.exists()) getFolderSize(folderPath) else 0L
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            if (fileExists) {
                coroutineScope.launch(Dispatchers.IO) {
                    isDeleting = true
                    deleteFolderWithProgress(folderPath) {
                        progress = it
                        if (it == 100) {
                            isDeleting = false
                            fileExists = false
                            folderSize = 0L
                        }
                    }
                    filePath.delete()
                }
            } else {
                showDialog = true
            }
        }) {
            Text(if (fileExists) "Remove Icons" else "Download File")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isDownloading) {
            CircularProgressIndicator(progress = progress / 100f)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Downloading: $progress%")
        }

        if (isDeleting) {
            CircularProgressIndicator(progress = progress / 100f)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Deleting: $progress%")
        }

        if (folderSize > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Icon Folder Size: ${formatSize(folderSize)}")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Download Icon Pack?") },
                text = { Text("The ZIP file size is ${formatSize(zipFileSize)}. Do you want to download it?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            coroutineScope.launch(Dispatchers.IO) {
                                isDownloading = true
                                downloadFile(context) {
                                    progress = it
                                    if (it == 100) {
                                        isDownloading = false
                                        fileExists = true
                                        folderSize = getFolderSize(folderPath)
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Download")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

fun downloadFile(context: Context, progressCallback: (Int) -> Unit) {
    val url = URL("https://game-icons.net/archives/png/zip/ffffff/000000/game-icons.net.png.zip")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.doOutput = true
    connection.connect()

    val fileLength = connection.contentLength

    val file = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "game-icons.zip"
    )

    var count: Int
    val buffer = ByteArray(1024)
    var total: Long = 0

    BufferedInputStream(connection.inputStream).use { input ->
        FileOutputStream(file).use { output ->
            while (input.read(buffer).also { count = it } != -1) {
                total += count
                output.write(buffer, 0, count)
                // Calculate progress
                if (fileLength > 0) {
                    val progress = (total * 100 / fileLength).toInt()
                    progressCallback(progress)
                }
            }
        }
    }

    // Extract the ZIP file after downloading
    if (file.exists()) {
        extractZip(file.absolutePath, "${Constants.USER_FILE_STORAGE}/icons")
    }
}

fun deleteFolderWithProgress(folder: File, progressCallback: (Int) -> Unit) {
    val totalSize = getFolderSize(folder)
    var deletedSize: Long = 0

    fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                deleteRecursively(it)
            }
        }
        val fileSize = file.length()
        if (file.delete()) {
            deletedSize += fileSize
            val progress = (deletedSize * 100 / totalSize).toInt()
            progressCallback(progress)
        }
    }

    deleteRecursively(folder)
}
