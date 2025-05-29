package org.openmw.utils

import android.app.DownloadManager
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.openmw.Constants
import java.io.File

data class DownloadItem(
    val title: String,
    val size: String,
    val fileName: String,
    val url: String,
    var progress: MutableStateFlow<Int> = MutableStateFlow(0),
    var bytesDownloaded: MutableStateFlow<Long> = MutableStateFlow(0L),
    var totalBytes: MutableStateFlow<Long> = MutableStateFlow(0L),
    var isDownloadComplete: MutableStateFlow<Boolean> = MutableStateFlow(false)
)

@Composable
fun UqmDownloads() {
    val context = LocalContext.current
    val downloadManagerHelper = remember { DownloadManagerHelper(context) }
    val files = listOf(
        "mm-0.8.2-hd.uqm",
        "uqm-0.8.0-3DOMusicRemastered.uqm",
        "mm-0.8.2-3dovoice.uqm",
        "uqm-0.8.0-3dovideo.uqm",
        "mm-vols-remix.uqm",
        "mm-vols-space.uqm",
        "mm-rmx-utwig.uqm",
        "uqm-remix-disc1.uqm",
        "uqm-remix-disc2.uqm",
        "uqm-remix-disc3.uqm",
        "uqm-remix-disc4-1.uqm",
        "mm-remix-timing.uqm"
    )

    LazyColumn {
        files.forEach { file ->
            item {
                Row {
                    val url = "https://sourceforge.net/projects/uqm-mods/files/MegaMod/0.8.2/Content/$file/download"
                    val downloadItem = remember {
                        DownloadItem(
                            title = "File",
                            size = "Unknown",
                            fileName = file,
                            url = url
                        )
                    }
                    DownloadScreen(downloadManagerHelper, downloadItem)
                }
            }
        }
    }
}

suspend fun getFileSize(url: String): Long {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).head().build()
        client.newCall(request).execute().use { response ->
            val contentLength = response.header("Content-Length")?.toLongOrNull()
            contentLength ?: -1
        }
    }
}

class DownloadManagerHelper(private val context: Context) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun startDownload(item: DownloadItem) {
        val file = File(Constants.USER_FILE_STORAGE, item.fileName)
        if (file.exists()) {
            file.delete()
        }

        val request = DownloadManager.Request(Uri.parse(item.url))
            .setTitle(item.title)
            .setDescription("Downloading ${item.size}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Constants.USER_FILE_STORAGE, item.fileName)

        val downloadId = downloadManager.enqueue(request)
        observeDownloadProgress(downloadId, item)
    }

    private fun observeDownloadProgress(downloadId: Long, item: DownloadItem) {
        val query = DownloadManager.Query().setFilterById(downloadId)

        context.contentResolver.registerContentObserver(
            Uri.parse("content://downloads/my_downloads"),
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val bytesDownloaded =
                            cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                        if (totalBytes > 0) {
                            val progress = (bytesDownloaded * 100L / totalBytes).toInt()
                            item.progress.value = progress
                            item.bytesDownloaded.value = bytesDownloaded
                            item.totalBytes.value = totalBytes

                            val file = File(Constants.USER_FILE_STORAGE, item.fileName)
                            if (file.exists() && progress == 100) {
                                item.isDownloadComplete.value = true
                            } else {
                                item.isDownloadComplete.value = false
                            }
                        }
                        cursor.close()
                    }
                }
            })
    }

    fun reset(item: DownloadItem) {
        item.progress.value = 0
        item.bytesDownloaded.value = 0L
        item.totalBytes.value = 0L
        item.isDownloadComplete.value = false
    }
}

@Composable
fun DownloadScreen(downloadManagerHelper: DownloadManagerHelper, item: DownloadItem) {
    var fileSize by remember { mutableStateOf<Long?>(null) }
    val progress by item.progress.collectAsState()
    val bytesDownloaded by item.bytesDownloaded.collectAsState()
    val totalBytes by item.totalBytes.collectAsState()
    val isDownloadComplete by item.isDownloadComplete.collectAsState()

    val context = LocalContext.current
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), item.fileName)

    LaunchedEffect(file) {
        if (file.exists()) {
            downloadManagerHelper.reset(item)
            item.isDownloadComplete.value = true
        }
        fileSize = getFileSize(item.url)
    }

    Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (isDownloadComplete) {
            val totalMB = fileSize?.let { it / (1024 * 1024) } ?: "Unknown"
            Text(text = "File: ${item.fileName}\nSize: ${totalMB} MB", color = Color.Green)

            Spacer(modifier = Modifier.weight(1f))  // Pushes button to the right

            Button(
                onClick = {
                    if (file.exists()) {
                        file.delete()
                        downloadManagerHelper.reset(item)
                    }
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uninstall")
                }
            }
        } else {
            val downloadedMB = bytesDownloaded / (1024 * 1024)
            val totalMB = totalBytes / (1024 * 1024)
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "File: ${item.fileName}\nDownloaded: $downloadedMB MB / ${if (totalMB > 0) "$totalMB MB" else fileSize?.let { "${it / (1024 * 1024)} MB" } ?: "Unknown"}")
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = {
                        downloadManagerHelper.startDownload(item)
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Install")
                        }
                    }
                }
            }
        }
    }
}
