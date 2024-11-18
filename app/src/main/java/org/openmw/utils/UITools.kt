package org.openmw.utils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.VibrationEffect
import android.os.Vibrator
import android.system.Os
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.openmw.Constants
import org.openmw.EngineActivity
import org.openmw.R
import org.openmw.ui.overlay.MemoryInfo
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.math.ln
import kotlin.math.pow

@Suppress("DEPRECATION")
fun vibrate(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (vibrator.hasVibrator()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}

fun getAvailableStorageSpace(context: Context): String {
    val storageDirectory = Environment.getExternalStorageDirectory()
    val stat = StatFs(storageDirectory.toString())
    val availableBytes = stat.availableBytes
    return humanReadableByteCountBin(availableBytes)
}

// Get storage space
//val availableSpace = getAvailableStorageSpace(this)
//println("Available storage space: $availableSpace bytes")

fun getMemoryInfo(context: Context): MemoryInfo {
    val memoryInfo = ActivityManager.MemoryInfo()
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    activityManager.getMemoryInfo(memoryInfo)
    val totalMemory = humanReadableByteCountBin(memoryInfo.totalMem)
    val availableMemory = humanReadableByteCountBin(memoryInfo.availMem)
    val usedMemory = humanReadableByteCountBin(memoryInfo.totalMem - memoryInfo.availMem)

    return MemoryInfo(totalMemory, availableMemory, usedMemory)
}

@SuppressLint("DefaultLocale")
fun humanReadableByteCountBin(bytes: Long): String {
    val unit = 1024
    if (bytes < unit) return "$bytes B"
    val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
    val pre = "KMGTPE"[exp - 1] + "i"
    return String.format("%.1f %sB", bytes / unit.toDouble().pow(exp.toDouble()), pre)
}

fun getBatteryStatus(context: Context): String {
    val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val isCharging = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0
    val batteryPct = (level / scale.toFloat()) * 100

    return "Battery: ${batteryPct.toInt()}%${if (isCharging) " (Charging)" else ""}"
}

fun getMessages(): List<String> {
    val logMessages = mutableListOf<String>()
    try {
        val process = Runtime.getRuntime().exec("logcat -d")
        val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))

        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            logMessages.add(line!!)
        }
        process.waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return logMessages
}

fun enableLogcat() {
    val logcatFile = File(Constants.USER_CONFIG + "/openmw_logcat.txt")
    if (logcatFile.exists()) {
        logcatFile.delete()
    }

    val processBuilder = ProcessBuilder()
    val commandToExecute = arrayOf("/system/bin/sh", "-c", "logcat *:W -d -f ${Constants.USER_CONFIG}/openmw_logcat.txt")
    processBuilder.command(*commandToExecute)
    processBuilder.redirectErrorStream(true)
    processBuilder.start()
}

fun updateResolutionInConfig(width: Int, height: Int) {
    // Ensure the larger value is assigned to width
    val (adjustedWidth, adjustedHeight) = if (width > height) width to height else height to width

    val file = File(Constants.SETTINGS_FILE)
    val lines = file.readLines().map { line ->
        when {
            // Update lines based on the adjusted width and height
            line.startsWith("# Width of screen") -> "# Width recommended for your device = $adjustedWidth"
            line.startsWith("# Height of screen") -> "# Height recommended for your device = $adjustedHeight"
            line.startsWith("resolution y = 0") -> "resolution y = $adjustedHeight"
            line.startsWith("resolution x = 0") -> "resolution x = $adjustedWidth"
            else -> line
        }
    }
    file.writeText(lines.joinToString("\n"))

    // Update the companion object values
    EngineActivity.resolutionX = adjustedWidth
    EngineActivity.resolutionY = adjustedHeight
}

fun getScreenWidthAndHeight(context: Context): Pair<Int, Int> {
    val windowMetrics: WindowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context)
    val bounds = windowMetrics.bounds
    var width = bounds.width()
    var height = bounds.height()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        val windowInsets: WindowInsets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.windowInsets
        } else {
            TODO("VERSION.SDK_INT < R")
        }
        val displayCutout = windowInsets.displayCutout
        if (displayCutout != null) {
            width += displayCutout.safeInsetLeft + displayCutout.safeInsetRight
            height += displayCutout.safeInsetTop + displayCutout.safeInsetBottom
        }
    }
    return Pair(width, height)
}

@Composable
fun ProgressWithNavmesh(onComplete: () -> Unit) {
    val progressFlow = remember { MutableStateFlow(0f) }
    val navmeshStatus = remember { MutableStateFlow("0.0") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val progress by progressFlow.collectAsState()
        val status by navmeshStatus.collectAsState()

        if (progress < 1f) {
            CircularProgressIndicator(
                progress = progress,
                trackColor = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Processing... ${(progress * 100).toInt()}%")
        } else {
            Text(text = "Processing complete!")
            onComplete()
        }
    }

    LaunchedEffect(Unit) {
        Log.d("ProgressWithNavmesh", "LaunchedEffect triggered")
        CoroutineScope(Dispatchers.IO).launch {
            while (navmeshStatus.value != "Done") {
                val statusMessage = Os.getenv("NAVMESHTOOL_MESSAGE")
                if (statusMessage != null) {
                    navmeshStatus.value = statusMessage
                    progressFlow.value = statusMessage.toFloat() / 100.0f
                }
                delay(50) // Using delay from kotlinx.coroutines
            }
            progressFlow.value = 1f
        }
    }
}

@Composable
fun BouncingBackground() {
    val image: Painter = painterResource(id = R.drawable.backgroundbouncebw)
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp * configuration.densityDpi / 160
    val screenHeight = configuration.screenHeightDp * configuration.densityDpi / 160

    val imageWidth = 2000 // Replace with your image width
    val imageHeight = 2337 // Replace with your image height

    var offset: Offset by remember { mutableStateOf(Offset.Zero) }
    val xDirection by remember { mutableFloatStateOf(1f) }
    val yDirection by remember { mutableFloatStateOf(1f) }

    // Adjust this value to increase the distance
    val stepSize = 1f

    LaunchedEffect(Unit) {
        while (true) {
            offset = Offset(
                x = (offset.x + xDirection * stepSize) % screenWidth,
                y = (offset.y + yDirection * stepSize) % screenHeight
            )

            delay(16L) // Update every frame (approx 60fps)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = image,
            contentDescription = null,
            modifier = Modifier
                .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                .size(imageWidth.dp, imageHeight.dp) // Convert Int to Dp
                .scale(6f) // Scale the image up by a factor of 6
                .background(color = Color.LightGray))
    }
}
