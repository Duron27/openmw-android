package org.openmw.utils

import android.os.Environment
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

fun containsMorrowindFolder(zipFilePath: String): Boolean {
    ZipInputStream(FileInputStream(zipFilePath)).use { zipIn ->
        var entry: ZipEntry? = zipIn.nextEntry
        while (entry != null) {
            if (entry.name.startsWith("Morrowind/")) {
                return true
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
    }
    return false
}

@Composable
fun UnzipWithProgress(onComplete: () -> Unit) {
    val progressFlow = remember { MutableStateFlow(0f) }
    val zipFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/Morrowind.zip"
    val destDirectory = LocalContext.current.getExternalFilesDir(null)?.absolutePath ?: ""

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val progress by progressFlow.collectAsState()

        if (progress < 1f) {
            CircularProgressIndicator(
                progress = {
                    progress
                },
                trackColor = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Unzipping... ${(progress * 100).toInt()}%")
        } else {
            Text(text = "Unzipping complete!")
            onComplete()
        }
    }

    LaunchedEffect(Unit) {
        Log.d("UnzipWithProgress", "LaunchedEffect triggered")
        // Launch a coroutine in the IO dispatcher
        CoroutineScope(Dispatchers.IO).launch {
            unzip(zipFilePath, destDirectory) { extractedSize, totalSize ->
                Log.d("UnzipWithProgress", "Extracted: $extractedSize, Total: $totalSize")
                // Switch to the main thread to update the progressFlow

                    progressFlow.value = extractedSize / totalSize.toFloat()

            }
        }
    }
}

fun unzip(zipFilePath: String, destDirectory: String, onProgress: (Long, Long) -> Unit) {
    val destDir = File(destDirectory)
    if (!destDir.exists()) {
        destDir.mkdirs()
    }
    ZipInputStream(FileInputStream(zipFilePath)).use { zipIn ->
        val totalSize = File(zipFilePath).length()
        var extractedSize: Long = 0
        var entry: ZipEntry? = zipIn.nextEntry
        while (entry != null) {
            val filePath = destDirectory + File.separator + entry.name
            if (!entry.isDirectory) {
                extractedSize += extractFile(zipIn, filePath)
                onProgress(extractedSize, totalSize)
            } else {
                val dir = File(filePath)
                dir.mkdirs()
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
    }
}

private fun extractFile(zipIn: ZipInputStream, filePath: String): Long {
    var totalBytes: Long = 0
    File(filePath).outputStream().use { fos ->
        val bytesIn = ByteArray(4096)
        var read: Int
        while (zipIn.read(bytesIn).also { read = it } != -1) {
            fos.write(bytesIn, 0, read)
            totalBytes += read
        }
    }
    return totalBytes
}
