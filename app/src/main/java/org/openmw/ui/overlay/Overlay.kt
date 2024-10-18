package org.openmw.ui.overlay

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import org.libsdl.app.SDLActivity
import org.openmw.utils.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class MemoryInfo(
    val totalMemory: String,
    val availableMemory: String,
    val usedMemory: String
)

object UIStateManager {
    var isUIHidden by mutableStateOf(false)
    var visible by mutableStateOf(true)
}

@Composable
fun OverlayUI() {
    val context = LocalContext.current
    var memoryInfoText by remember { mutableStateOf("") }
    var batteryStatus by remember { mutableStateOf("") }
    var logMessages by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    var isMemoryInfoEnabled by remember { mutableStateOf(false) }
    var isBatteryStatusEnabled by remember { mutableStateOf(false) }
    var isLoggingEnabled by remember { mutableStateOf(false) }
    var isVibrationEnabled by remember { mutableStateOf(true) }
    val isUIHidden = UIStateManager.isUIHidden
    var isRunEnabled by remember { mutableStateOf(false) }
    var isF10Enabled by remember { mutableStateOf(false) } // New state for F10
    var isBacktickEnabled by remember { mutableStateOf(false) }

LaunchedEffect(Unit) {
        getMessages() // Ensure logcat is enabled

        while (true) {
            if (isMemoryInfoEnabled) {
                val memoryInfo = getMemoryInfo(context)
                memoryInfoText = "Total memory: ${memoryInfo.totalMemory}\n" +
                        "Available memory: ${memoryInfo.availableMemory}\n" +
                        "Used memory: ${memoryInfo.usedMemory}"
            } else {
                memoryInfoText = ""
            }

            if (isBatteryStatusEnabled) {
                batteryStatus = getBatteryStatus(context)
            } else {
                batteryStatus = ""
            }

            if (isLoggingEnabled) {
                logMessages = getMessages().joinToString("\n")
            } else {
                logMessages = ""
            }
            scrollState.animateScrollTo(scrollState.maxValue)
            delay(1000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        var expanded by remember { mutableStateOf(false) }

        Surface(
            color = Color.Transparent,
            onClick = { expanded = !expanded }
        ) {
            AnimatedContent(
                targetState = expanded,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150, 150)) togetherWith
                            fadeOut(animationSpec = tween(150)) using
                            SizeTransform { initialSize, targetSize ->
                                if (targetState) {
                                    keyframes {
                                        // Expand horizontally first.
                                        IntSize(targetSize.width, initialSize.height) at 150
                                        durationMillis = 600
                                    }
                                } else {
                                    keyframes {
                                        // Shrink vertically first.
                                        IntSize(initialSize.width, targetSize.height) at 150
                                        durationMillis = 600
                                    }
                                }
                            }
                }, label = "size transform"
            ) { targetExpanded ->
                if (targetExpanded) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f))
                            .padding(5.dp)
                    ) {
                        Text(
                            text = "Show Memory Info",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                        Switch(
                            checked = isMemoryInfoEnabled,
                            onCheckedChange = { isMemoryInfoEnabled = it }
                        )
                        Text(
                            text = "Show Battery Status",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                        Switch(
                            checked = isBatteryStatusEnabled,
                            onCheckedChange = { isBatteryStatusEnabled = it }
                        )
                        Text(
                            text = "Show Logcat",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                        Switch(
                            checked = isLoggingEnabled,
                            onCheckedChange = { isLoggingEnabled = it }
                        )
                        Text(text = "Enable Vibration", color = Color.White, fontSize = 10.sp)
                        Switch(
                            checked = isVibrationEnabled,
                            onCheckedChange = { isVibrationEnabled = it }
                        )
                        Text(text = "Hide UI", color = Color.White, fontSize = 10.sp)
                        Switch(checked = isUIHidden, onCheckedChange = {
                            UIStateManager.isUIHidden = it
                            UIStateManager.visible = !it //
                        })
                        // Run/Walk Toggle Switch
                        Text(text = "Run/Walk", color = Color.White, fontSize = 10.sp)
                        Switch(checked = isRunEnabled, onCheckedChange = {
                            isRunEnabled = it
                            if (it) SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT)
                            else SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT)
                        })
                        // F10 Toggle Switch
                        Text(text = "Press F10", color = Color.White, fontSize = 10.sp)
                        Switch(checked = isF10Enabled, onCheckedChange = {
                            isF10Enabled = it
                            if (it) {
                                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_F10)
                                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_F10)
                            } else {
                                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_F10)
                                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_F10)
                            }
                        })
                        // Backtick Toggle Switch
                        Text(text = "Console", color = Color.White, fontSize = 10.sp)
                        Switch(checked = isBacktickEnabled, onCheckedChange = {
                            isBacktickEnabled = it
                            if (it) {
                                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_GRAVE)
                                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_GRAVE)
                            } else {
                                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_GRAVE)
                                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_GRAVE)
                            }
                        })
                    }
                } else {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                }
            }
        }
        // Information display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isMemoryInfoEnabled) {
                Text(
                    text = memoryInfoText,
                    color = Color.White,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isBatteryStatusEnabled) {
                Text(
                    text = batteryStatus,
                    color = Color.White,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isLoggingEnabled) {
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .height(200.dp)
                        .padding(vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        // Your text content here
                        Text(
                            text = logMessages,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }

}

@Suppress("DEPRECATION")
fun vibrate(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (vibrator.hasVibrator()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}

@Composable
fun Thumbstick(
    onWClick: () -> Unit,
    onAClick: () -> Unit,
    onSClick: () -> Unit,
    onDClick: () -> Unit,
    onRelease: () -> Unit
) {
    val density = LocalDensity.current
    val radiusPx = with(density) { 75.dp.toPx() }
    val deadZone = 0.2f * radiusPx // Adjust deadzone as needed
    var touchState by remember { mutableStateOf(Offset(0f, 0f)) }
    val visible = UIStateManager.visible

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { with(density) { -20.dp.roundToPx() } },
            animationSpec = tween(durationMillis = 1000) // Adjust the duration as needed
        ) + expandVertically(
            expandFrom = Alignment.Bottom,
            animationSpec = tween(durationMillis = 1000)
        ) + fadeIn(
            initialAlpha = 0.3f,
            animationSpec = tween(durationMillis = 1000)
        ),
        exit = slideOutVertically(
            targetOffsetY = { with(density) { -20.dp.roundToPx() } },
            animationSpec = tween(durationMillis = 1000)
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = 1000)
        ) + fadeOut(
            animationSpec = tween(durationMillis = 1000)
        )
    ) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .padding(20.dp)
                .align(Alignment.BottomStart)
                .border(2.dp, Color.Black, shape = CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            touchState = Offset.Zero // Reset to center
                            onRelease() // Invoke release callback
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newX = touchState.x + dragAmount.x
                            val newY = touchState.y + dragAmount.y
                            if (hypot(newX, newY) <= radiusPx) {
                                touchState = Offset(newX, newY)
                            } else {
                                val angle = atan2(newY, newX)
                                touchState = Offset(
                                    radiusPx * cos(angle),
                                    radiusPx * sin(angle)
                                )
                            }
                            // Determine direction with overlapping zones and deadzone
                            SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_W)
                            SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_A)
                            SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_S)
                            SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_D)
                            val xRatio = touchState.x / radiusPx
                            val yRatio = touchState.y / radiusPx
                            when {
                                abs(yRatio) > abs(xRatio) -> {
                                    if (abs(yRatio) > 0.95f) {
                                        if (touchState.y < 0) onWClick() else onSClick()
                                    } else {
                                        if (touchState.y < -deadZone) onWClick()
                                        if (touchState.y > deadZone) onSClick()
                                        if (touchState.x < -deadZone) onAClick()
                                        if (touchState.x > deadZone) onDClick()
                                    }
                                }

                                abs(xRatio) > 0.95f -> {
                                    if (touchState.x < 0) onAClick() else onDClick()
                                }

                                else -> {
                                    if (touchState.y < -deadZone) onWClick()
                                    if (touchState.y > deadZone) onSClick()
                                    if (touchState.x < -deadZone) onAClick()
                                    if (touchState.x > deadZone) onDClick()
                                }
                            }
                        }
                    )
                }
        ) {
                Box(
                    modifier = Modifier
                        .size(25.dp)
                        .offset(
                            x = (touchState.x / density.density).dp,
                            y = (touchState.y / density.density).dp
                        )
                        .background(
                            Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun GameControllerButtons(
    isVibrationEnabled: Boolean
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val visible = UIStateManager.visible

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { with(density) { -20.dp.roundToPx() } },
            animationSpec = tween(durationMillis = 1000) // Adjust the duration as needed
        ) + expandVertically(
            expandFrom = Alignment.Bottom,
            animationSpec = tween(durationMillis = 1000)
        ) + fadeIn(
            initialAlpha = 0.3f,
            animationSpec = tween(durationMillis = 1000)
        ),
        exit = slideOutVertically(
            targetOffsetY = { with(density) { -20.dp.roundToPx() } },
            animationSpec = tween(durationMillis = 1000)
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = 1000)
        ) + fadeOut(
            animationSpec = tween(durationMillis = 1000)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Button(onClick = {
                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_E)
                    sendKeyEvent(KeyEvent.KEYCODE_E)
                    if (isVibrationEnabled) {
                        vibrate(context)
                    }
                }, modifier = Modifier.size(50.dp), shape = CircleShape) {
                    Text(
                        text = "Y",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.width(200.dp) // Sets the fixed width for the row
                ) {
                    Button(onClick = {
                        SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_SPACE)
                        sendKeyEvent(KeyEvent.KEYCODE_SPACE)
                        if (isVibrationEnabled) {
                            vibrate(context)
                        }
                    }, modifier = Modifier.size(50.dp), shape = CircleShape) {
                        Text(
                            text = "X",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(onClick = {
                        SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ESCAPE)
                        sendKeyEvent(KeyEvent.KEYCODE_ESCAPE)
                    }, modifier = Modifier.size(50.dp), shape = CircleShape) {
                        Text(
                            text = "B",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Button(onClick = {
                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ENTER)
                    sendKeyEvent(KeyEvent.KEYCODE_ENTER)
                }, modifier = Modifier.size(50.dp), shape = CircleShape) {
                    Text(
                        text = "A",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun sendKeyEvent(keyCode: Int) {
    SDLActivity.onNativeKeyDown(keyCode)
    SDLActivity.onNativeKeyUp(keyCode)
}
