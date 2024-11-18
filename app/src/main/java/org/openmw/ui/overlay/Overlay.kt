package org.openmw.ui.overlay

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.FrameLayout
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.sharp.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import kotlinx.coroutines.*
import org.openmw.ui.controls.ButtonState
import org.openmw.ui.controls.DynamicButtonManager
import org.openmw.ui.controls.MouseCursor
import org.openmw.ui.controls.ScaleView
import org.openmw.ui.controls.UIStateManager
import org.openmw.ui.controls.UIStateManager.configureControls
import org.openmw.ui.controls.UIStateManager.gridAlpha
import org.openmw.ui.controls.UIStateManager.gridSize
import org.openmw.ui.controls.UIStateManager.gridVisible
import org.openmw.ui.controls.UIStateManager.isCursorVisible
import org.openmw.ui.controls.UIStateManager.isScaleView
import org.openmw.utils.*
import kotlin.math.roundToInt

data class MemoryInfo(
    val totalMemory: String,
    val availableMemory: String,
    val usedMemory: String
)

@SuppressLint("RestrictedApi")
fun toggleScaleView(scaleView: ScaleView) {
    runOnUiThread {
        isScaleView = !isScaleView
        scaleView.visibility = if (isScaleView) View.VISIBLE else View.GONE
    }
}

@Composable
fun OverlayUI(
    context: Context,
    editMode: Boolean,
    createdButtons: SnapshotStateList<ButtonState>,
    scaleView: ScaleView,
    mouseCursor: MouseCursor?
) {
    var expanded by remember { mutableStateOf(false) }
    val visible = UIStateManager.visible
    val density = LocalDensity.current
    var isScaleView by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val logMessages = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            if (UIStateManager.isMemoryInfoEnabled) {
                val memoryInfo = getMemoryInfo(context)
                UIStateManager.memoryInfoText = "Total memory: ${memoryInfo.totalMemory}\n" +
                        "Available memory: ${memoryInfo.availableMemory}\n" +
                        "Used memory: ${memoryInfo.usedMemory}"
            } else {
                UIStateManager.memoryInfoText = ""
            }
            if (UIStateManager.isBatteryStatusEnabled) {
                UIStateManager.batteryStatus = getBatteryStatus(context)
            } else {
                UIStateManager.batteryStatus = ""
            }
            if (UIStateManager.isLoggingEnabled) {
                logMessages.value = getMessages().joinToString("\n")
                scrollState.scrollTo(scrollState.maxValue)
            } else {
                logMessages.value = ""
            }
            delay(1000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                },
                label = "size transform"
            ) { targetExpanded ->
                if (targetExpanded) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f))
                            .padding(5.dp)
                    ) {
                        PopUpWindow(
                            expanded = expanded,
                            context = context,
                            onClose = { expanded = false },
                            onToggleUI = {
                                UIStateManager.isUIHidden = !UIStateManager.isUIHidden
                                UIStateManager.visible = !UIStateManager.isUIHidden
                            },
                            onToggleVibration = { UIStateManager.isVibrationEnabled = !UIStateManager.isVibrationEnabled },
                            onToggleMemoryInfo = { UIStateManager.isMemoryInfoEnabled = !UIStateManager.isMemoryInfoEnabled },
                            onToggleBatteryStatus = { UIStateManager.isBatteryStatusEnabled = !UIStateManager.isBatteryStatusEnabled },
                            onToggleLogcat = { UIStateManager.isLoggingEnabled = !UIStateManager.isLoggingEnabled }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier
                                .padding(top = 10.dp, start = 20.dp)
                                .size(30.dp),
                            tint = Color.Black
                        )
                        AnimatedVisibility(
                            visible = visible,
                            enter = slideInVertically(
                                initialOffsetY = { with(density) { -20.dp.roundToPx() } },
                                animationSpec = tween(durationMillis = 1000)
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
                            DynamicButtonManager(
                                context = context,
                                onNewButtonAdded = { newButtonState ->
                                    createdButtons.add(newButtonState)
                                },
                                editMode = UIStateManager.editMode,
                                createdButtons = createdButtons
                            )
                            // IconButton to toggle zoom and move buttons
                            IconButton(
                                onClick = {
                                    isScaleView = !isScaleView
                                    scaleView.scaleSdlView(isScaleView)
                                    UIStateManager.isUIHidden = isScaleView
                                    UIStateManager.visible = !UIStateManager.isUIHidden
                                    toggleScaleView(scaleView)
                                    if (isScaleView) {
                                        // Add exit button to the view
                                        scaleView.addExitButton()
                                        //customCursorView.addCursorToggleButton()
                                    } else {
                                        // Remove exit button from the view
                                        scaleView.removeExitButton()
                                        //customCursorView.removeCursorToggleButton()
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Transparent
                                )
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "Toggle Zoom",
                                    modifier = Modifier.size(30.dp),
                                    tint = Color.Black
                                )
                            }
                            if (UIStateManager.editMode) {
                                Column {
                                    // Toggle Grid Visibility Icon
                                    IconButton(onClick = { gridVisible.value = !gridVisible.value },modifier = Modifier.padding(top = 40.dp)) {

                                        Icon(
                                            imageVector = if (gridVisible.value) Icons.Default.Edit else Icons.Default.Edit,
                                            contentDescription = null
                                        )
                                    }

                                    // Change Grid Size Icon
                                    IconButton(onClick = { gridSize.intValue = (gridSize.intValue % 100) + 10 }) {
                                        Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                                    }

                                    // Change Grid Alpha Icon
                                    IconButton(onClick = { gridAlpha.floatValue = if (gridAlpha.floatValue == 0.25f) 0.5f else 0.25f }) {
                                        Icon(imageVector = Icons.Default.Face, contentDescription = null)
                                    }
                                    // IconButton to enable/disable MouseCursor
                                    IconButton(
                                        onClick = {
                                            mouseCursor?.let {
                                                if (it.isCursorVisible) {
                                                    it.disableCursor()
                                                } else {
                                                    it.enableCursor()
                                                }
                                            }
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.Transparent
                                        )
                                    ) {
                                        Icon(
                                            if (isCursorVisible) Icons.Sharp.Send else Icons.Sharp.Send,
                                            contentDescription = "Toggle Mouse Cursor",
                                            modifier = Modifier.size(30.dp),
                                            tint = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
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
            if (UIStateManager.isMemoryInfoEnabled) {
                DraggableBox(editMode = UIStateManager.editMode) { fontSize ->
                    Text(
                        text = UIStateManager.memoryInfoText,
                        color = Color.White,
                        fontSize = fontSize.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            if (UIStateManager.isBatteryStatusEnabled) {
                DraggableBox(editMode = UIStateManager.editMode) { fontSize ->
                    Text(
                        text = UIStateManager.batteryStatus,
                        color = Color.White,
                        fontSize = fontSize.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            if (UIStateManager.isLoggingEnabled) {
                DraggableBox(editMode = UIStateManager.editMode) { fontSize ->
                    Text(
                        text = logMessages.value,
                        color = Color.White,
                        fontSize = fontSize.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun DraggableBox(
    editMode: Boolean,
    content: @Composable (Float) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var boxWidth by remember { mutableFloatStateOf(200f) }
    var boxHeight by remember { mutableFloatStateOf(100f) }
    var isDragging by remember { mutableStateOf(false) }
    var isResizing by remember { mutableStateOf(false) }
    var fontSize by remember { mutableFloatStateOf(10f) }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(width = boxWidth.dp, height = boxHeight.dp)
            .background(Color.Transparent)
            //.verticalScroll(scrollState)
            .then(
                if (UIStateManager.editMode) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDrag = { change, dragAmount ->
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            },
                            onDragEnd = { isDragging = false }
                        )
                    }
                } else Modifier
            )
            .border(2.dp, if (isDragging || isResizing) Color.Red else Color.Transparent)
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                content(fontSize)
            }
            if (UIStateManager.editMode) {
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 5f..30f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Red,
                        activeTrackColor = Color(alpha = .9f, red = 0f, green = 0f, blue = 0f),
                        inactiveTrackColor = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )

            }
        }
        if (UIStateManager.editMode) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .offset { IntOffset(boxWidth.roundToInt() - 16.dp.toPx().roundToInt() - 16, boxHeight.roundToInt() - 16.dp.toPx().roundToInt() - 16) }
                    .background(Color.Red)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isResizing = true },
                            onDrag = { change, dragAmount ->
                                boxWidth += dragAmount.x
                                boxHeight += dragAmount.y
                            },
                            onDragEnd = { isResizing = false }
                        )
                    }
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

// This is the settings window while in-game when you hit the gear icon. (top left)
@Composable
fun PopUpWindow(
    expanded: Boolean,
    context: Context,
    onClose: () -> Unit,
    onToggleUI: () -> Unit,
    onToggleVibration: () -> Unit,
    onToggleMemoryInfo: () -> Unit,
    onToggleBatteryStatus: () -> Unit,
    onToggleLogcat: () -> Unit
) {
    val gradientColors = listOf(Color(0xFF42A5F5), Color(0xFF478DE0), Color(0xFF3F76D2), Color(0xFF3B5FBA))
    if (expanded) {
        Dialog(onDismissRequest = onClose) {
            Column(
                modifier = Modifier
                    .background(Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f), shape = RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3), // Three columns per row
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    items(6) { index -> // Adjust the number of items as needed
                        val (text, enabled) = when (index) {
                            0 -> "Close" to false
                            1 -> "Hide UI" to UIStateManager.isUIHidden
                            2 -> "Enable Vibration" to UIStateManager.isVibrationEnabled
                            3 -> "Memory\n Info" to UIStateManager.isMemoryInfoEnabled
                            4 -> "Battery Status" to UIStateManager.isBatteryStatusEnabled
                            5 -> "Logcat" to UIStateManager.isLoggingEnabled
                            else -> "" to false
                        }

                        Card(
                            onClick = {
                                when (index) {
                                    0 -> onClose()
                                    1 -> onToggleUI()
                                    2 -> onToggleVibration()
                                    3 -> onToggleMemoryInfo()
                                    4 -> onToggleBatteryStatus()
                                    5 -> onToggleLogcat()
                                }
                            },
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                                .aspectRatio(1f) // Ensure circular shape
                                .border(1.dp, Color.Black, CircleShape)
                                .background(Brush.linearGradient(gradientColors), CircleShape),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Transparent // Override container color to transparent
                            ),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                                .background(Brush.linearGradient(gradientColors), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = text,
                                    color = if (enabled) Color.Green else Color.Black,
                                    fontSize = 17.sp,
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black,
                                            offset = Offset(2f, 2f),
                                            blurRadius = 4f
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
            // Conditionally display the "Exit to Launcher" button at the bottom center
            if (configureControls) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Button(
                        onClick = { (context as? Activity)?.finish() }
                    ) {
                        Text("Exit to Launcher")
                    }
                }
            }
        }
    }
}

// This is the snap to grid for the UI buttons.
@Composable
fun GridOverlay(gridSize: Int, snapX: Float?, snapY: Float?, alpha: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val lineColor = Color.LightGray.copy(alpha = alpha)
        val dotColor = Color.Red.copy(alpha = alpha)
        val highlightColor = Color.Red.copy(alpha = 1.0f)

        // Draw vertical lines
        for (x in 0 until width.toInt() step gridSize) {
            drawLine(
                color = lineColor,
                start = Offset(x.toFloat(), 0f),
                end = Offset(x.toFloat(), height),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw horizontal lines
        for (y in 0 until height.toInt() step gridSize) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y.toFloat()),
                end = Offset(width, y.toFloat()),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw dots at intersections
        for (x in 0 until width.toInt() step gridSize) {
            for (y in 0 until height.toInt() step gridSize) {
                val color = if (snapX == x.toFloat() && snapY == y.toFloat()) highlightColor else dotColor
                drawCircle(
                    color = color,
                    radius = 2.dp.toPx(),
                    center = Offset(x.toFloat(), y.toFloat())
                )
            }
        }
    }
}
