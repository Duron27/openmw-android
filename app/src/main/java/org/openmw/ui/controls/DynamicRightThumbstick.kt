package org.openmw.ui.controls

import android.content.Context
import android.util.Log
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.composables.core.ScrollArea
import com.composables.core.Thumb
import com.composables.core.VerticalScrollbar
import com.composables.core.rememberScrollAreaState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import org.libsdl.app.SDLActivity
import org.openmw.EngineActivity
import org.openmw.isControllerConnected
import org.openmw.ui.controls.UIStateManager.configureControls
import org.openmw.ui.controls.UIStateManager.customColor
import org.openmw.ui.controls.UIStateManager.editMode
import org.openmw.ui.controls.UIStateManager.enableRightThumb
import org.openmw.ui.controls.UIStateManager.globalColorChange
import org.openmw.ui.controls.UIStateManager.isCursorVisible
import org.openmw.ui.controls.UIStateManager.updateButtonState
import org.openmw.ui.overlay.getAnimations
import org.openmw.utils.ColorPickerWheel
import org.openmw.utils.GameFilesPreferences
import org.openmw.utils.GameFilesPreferences.getSelectedAnimation
import org.openmw.utils.GameFilesPreferences.getSensitivityMouse
import org.openmw.utils.GameFilesPreferences.getSensitivityRT
import org.openmw.utils.GameFilesPreferences.loadAutoMouseMode
import org.openmw.utils.GameFilesPreferences.setSensitivityMouse
import org.openmw.utils.GameFilesPreferences.setSensitivityRT
import org.openmw.utils.currentDeviceRealSize
import org.openmw.utils.fromHex
import kotlin.math.roundToInt

@OptIn(InternalCoroutinesApi::class)
@Composable
fun ResizableDraggableRightThumbstick(
    context: Context,
    containerWidth: Float,
    containerHeight: Float
) {
    val buttonStates by UIStateManager.buttonStates.collectAsState()
    val buttonState = buttonStates[98]
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )

    buttonState?.let { state ->
        var buttonSize by remember { mutableStateOf(state.size.dp) }
        var hexCode by remember { mutableStateOf(state.color) }
        var buttonColor by remember { mutableStateOf(Color.fromHex(hexCode)) }
        var buttonAlpha by remember { mutableFloatStateOf(state.alpha) }
        val offsetX = remember { mutableFloatStateOf(state.offsetX) }
        val offsetY = remember { mutableFloatStateOf(state.offsetY) }
        val isUIHidden by GameFilesPreferences.loadUIState(context).collectAsState(initial = false)
        var offset by remember { mutableStateOf(IntOffset.Zero) }
        val density = LocalDensity.current
        val radiusPx = with(density) { (buttonSize / 2).toPx() }
        val isDragging = remember { mutableStateOf(false) }
        val scrollState = rememberScrollState()
        val stateSB = rememberScrollAreaState(scrollState)
        val autoMouseMode by loadAutoMouseMode(context).collectAsState(initial = "Hybrid")
        val selectedAnimation by getSelectedAnimation(context).collectAsState(initial = "None")
        val sensitivityMouseFlow = getSensitivityMouse(context).collectAsState(initial = 5000f)
        var sensitivityMouse by remember { mutableFloatStateOf(sensitivityMouseFlow.value ?: 5000f) }
        val sensitivityRTFlow = getSensitivityRT(context).collectAsState(initial = 2500f)
        var sensitivityRT by remember { mutableFloatStateOf(sensitivityRTFlow.value ?: 2500f) }
        val initialOffset =
            with(LocalDensity.current) { Offset(buttonSize.toPx() / 2, buttonSize.toPx() / 2) }
        var touchOffset by remember { mutableStateOf(initialOffset) }
        var showControlsPopup by remember { mutableStateOf(false) }
        val controllerConnected = isControllerConnected(context)
        val rightThumbColor =
            if (isDragging.value) Color.Red.copy(alpha = buttonAlpha) else buttonColor.copy(
                alpha = buttonAlpha
            )

        val saveState = {
            val updatedState = state.copy(
                size = buttonSize.value,
                offsetX = offsetX.floatValue,
                offsetY = offsetY.floatValue,
                color = hexCode,
                alpha = buttonAlpha,
                group = 1
            )
            updateButtonState(98, updatedState)
            UIStateManager.saveButtonState(containerWidth, containerHeight)
        }

        // Manage whats in the tabs here
        var options by remember { mutableStateOf(true) }
        var colors by remember { mutableStateOf(false) }
        var info by remember { mutableStateOf(false) }

        fun resetStates() {
            options = false
            colors = false
            info = false
        }

        LaunchedEffect(sensitivityMouseFlow.value) {
            sensitivityMouse = sensitivityMouseFlow.value ?: 5000f
        }
        LaunchedEffect(sensitivityRTFlow.value) {
            sensitivityRT = sensitivityRTFlow.value ?: 2500f
        }

        // Update hexCode, buttonColor, and buttonAlpha to use the global color and alpha if globalColorChange is enabled
        LaunchedEffect(globalColorChange) {
            if (globalColorChange) {
                hexCode = UIStateManager.globalColor
                buttonColor = Color.fromHex(hexCode)
                buttonAlpha = UIStateManager.globalAlpha
            } else {
                hexCode = state.color
                buttonColor = Color.fromHex(hexCode)
                buttonAlpha = state.alpha
            }
        }

        val animations = getAnimations().toMutableMap().apply { put("None", EnterTransition.None to ExitTransition.None) }
        val (currentEnterAnimation, currentExitAnimation) = animations[selectedAnimation] ?: (animations["Slide Vertically"] ?: error("Default animation not found"))

        val visibilityState = if (isCursorVisible != 0 && autoMouseMode == "None") {
            isUIHidden
        } else {
            !isUIHidden
        }
        val visibilityFinal = if (controllerConnected) {
            isUIHidden
        } else {
            visibilityState
        }

        AnimatedVisibility(
            visible = visibilityFinal,
            enter = currentEnterAnimation,
            exit = currentExitAnimation
        ) {
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .background(Color.Transparent)
                    //.border(2.dp, Color.Red)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(buttonSize)
                        .offset {
                            IntOffset(
                                offsetX.floatValue.roundToInt(),
                                offsetY.floatValue.roundToInt()
                            )
                        }
                        .background(Color.Transparent)
                        .then(
                            if (editMode) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            isDragging.value = true
                                        },
                                        onDrag = { change, dragAmount ->
                                            offsetX.floatValue += dragAmount.x
                                            offsetY.floatValue += dragAmount.y
                                        },
                                        onDragEnd = {
                                            isDragging.value = false
                                            saveState()
                                        }
                                    )
                                }
                            } else Modifier
                        )
                        .border(
                            2.dp,
                            if (isDragging.value) Color.Red else rightThumbColor,
                            shape = CircleShape
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .border(2.dp, rightThumbColor, CircleShape)
                            .then(
                                if (!configureControls && !editMode) {
                                    Modifier.pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                                            val (screenWidth, screenHeight) = windowManager.currentDeviceRealSize()
                                            val sdlWidth = EngineActivity.resolutionX.toFloat()
                                            val sdlHeight = EngineActivity.resolutionY.toFloat()

                                            val startX = SDLActivity.getMouseX().toFloat() * (screenWidth / sdlWidth)
                                            val startY = SDLActivity.getMouseY().toFloat() * (screenHeight / sdlHeight)

                                            var curX = startX
                                            var curY = startY
                                            var draggingStarted = false
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val down =
                                                    event.changes.firstOrNull()?.pressed == true
                                                if (down) {
                                                    while (true) {
                                                        val dragEvent = awaitPointerEvent()
                                                        val dragChange =
                                                            dragEvent.changes.firstOrNull()
                                                        if (dragChange?.pressed == true) {
                                                            val newX = dragChange.position.x
                                                            val newY = dragChange.position.y
                                                            if (!draggingStarted) {
                                                                curX = newX
                                                                curY = newY
                                                                draggingStarted = true
                                                            }
                                                            val movementX = if (isCursorVisible != 0) {
                                                                (newX - curX) * sensitivityMouse / screenWidth
                                                            } else {
                                                                (newX - curX) * sensitivityRT / screenWidth
                                                            }
                                                            val movementY = if (isCursorVisible != 0) {
                                                                (newY - curY) * sensitivityMouse / screenWidth
                                                            } else {
                                                                (newY - curY) * sensitivityRT / screenWidth
                                                            }

                                                            touchOffset = Offset(newX, newY)

                                                            // Call the native function with updated coordinates
                                                            SDLActivity.sendRelativeMouseMotion(
                                                                movementX.roundToInt().toInt(),
                                                                movementY.roundToInt()
                                                                    .toInt()
                                                            )
                                                            // Update current positions
                                                            curX = newX
                                                            curY = newY
                                                            //Log.d("DragMovement", "movementX: $movementX, movementY: $movementY")
                                                        } else {
                                                            draggingStarted = false
                                                            touchOffset = Offset(
                                                                buttonSize.toPx() / 2,
                                                                buttonSize.toPx() / 2
                                                            )
                                                            break
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                    ) {
                        val density = LocalDensity.current.density
                        Box(
                            modifier = Modifier
                                .size(25.dp)
                                .offset {
                                    val offsetX =
                                        ((touchOffset.x - (buttonSize.toPx() / 2)) / density).coerceIn(
                                            -radiusPx,
                                            radiusPx
                                        ).dp.roundToPx()
                                    val offsetY =
                                        ((touchOffset.y - (buttonSize.toPx() / 2)) / density).coerceIn(
                                            -radiusPx,
                                            radiusPx
                                        ).dp.roundToPx()
                                    IntOffset(offsetX, offsetY)
                                }
                                .background(
                                    rightThumbColor,
                                    shape = CircleShape
                                )
                                .border(2.dp, if (isDragging.value) Color.Red else rightThumbColor, CircleShape)
                        )
                    }
                    if (editMode) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(30.dp)
                                .background(Color.Black, shape = CircleShape)
                                .clickable { showControlsPopup = true }
                                .border(2.dp, Color.DarkGray, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.Red,
                                modifier = Modifier.graphicsLayer(
                                    rotationZ = rotation
                                )
                            )
                        }
                        if (showControlsPopup) {
                            Popup(
                                alignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Transparent),
                                    //.verticalScroll(rememberScrollState())
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(300.dp)
                                            .offset { offset }
                                            .padding(top = 5.dp)
                                            .background(Color.Black.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
                                            .border(
                                                2.dp,
                                                Color.White,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .pointerInput(Unit) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    offset = IntOffset(
                                                        offset.x + dragAmount.x.roundToInt(),
                                                        offset.y + dragAmount.y.roundToInt()
                                                    )
                                                }
                                            }
                                    ) {
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.height(32.dp).padding(start = 20.dp)
                                            ) {
                                                Text(
                                                    text = "ID: 98, Size: ${buttonSize.value}",
                                                    color = Color.White
                                                )
                                            }
                                            ScrollableTabRow(
                                                selectedTabIndex = when {
                                                    options -> 0
                                                    colors -> 1
                                                    info -> 2
                                                    else -> 0
                                                },
                                                edgePadding = 1.dp,
                                                contentColor = Color.White,
                                                containerColor = customColor
                                            ) {
                                                Tab(
                                                    selected = options,
                                                    onClick = {
                                                        resetStates()
                                                        options = true
                                                    },
                                                    text = { Text("Options") }
                                                )
                                                Tab(
                                                    selected = colors,
                                                    onClick = {
                                                        resetStates()
                                                        colors = true
                                                    },
                                                    text = { Text("Colors") }
                                                )
                                                Tab(
                                                    selected = info,
                                                    onClick = {
                                                        resetStates()
                                                        info = true
                                                    },
                                                    text = { Text("Info") }
                                                )
                                            }

                                            ScrollArea(state = stateSB) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp).verticalScroll(scrollState)
                                                ) {
                                                    if (options) {

                                                        Text(text = "ID: 98", color = Color.White)
                                                        Text(
                                                            text = "Size: ${buttonSize.value}",
                                                            color = Color.White
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            // + button
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(30.dp)
                                                                    .background(
                                                                        Color.Black,
                                                                        shape = CircleShape
                                                                    )
                                                                    .clickable {
                                                                        buttonSize += 20.dp
                                                                        saveState()
                                                                    }
                                                                    .border(
                                                                        2.dp,
                                                                        Color.White,
                                                                        shape = CircleShape
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = "+",
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }

                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(30.dp)
                                                                    .background(
                                                                        Color.Black,
                                                                        shape = CircleShape
                                                                    )
                                                                    .clickable {
                                                                        buttonSize -= 20.dp
                                                                        if (buttonSize < 50.dp) buttonSize =
                                                                            50.dp
                                                                        saveState()
                                                                    }
                                                                    .border(
                                                                        2.dp,
                                                                        Color.White,
                                                                        shape = CircleShape
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = "-",
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(10.dp))
                                                            Text(
                                                                text = "Disable Right Thumbstick",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Spacer(modifier = Modifier.width(10.dp))
                                                            Switch(
                                                                checked = enableRightThumb,
                                                                onCheckedChange = { isChecked ->
                                                                    enableRightThumb = isChecked
                                                                    if (isChecked) {
                                                                        // Logic to add ButtonID_98
                                                                        val newButtonState = ButtonState(
                                                                            id = 98,
                                                                            size = 160f,
                                                                            offsetX = 1199.7069f,
                                                                            offsetY = 216.80106f,
                                                                            isLocked = false,
                                                                            keyCode = 98,
                                                                            color = "aafdfffe",
                                                                            alpha = 0.25f,
                                                                            uri = null,
                                                                            group = state.group
                                                                        )

                                                                        // Update UIStateManager with the new button state
                                                                        updateButtonState(newButtonState.id, newButtonState)
                                                                    } else {

                                                                        // Logic to remove ButtonID_98
                                                                        UIStateManager.removeButtonState(98, context, containerWidth, containerHeight)
                                                                    }

                                                                    // Save the updated button states to the file
                                                                    UIStateManager.saveButtonState(containerWidth, containerHeight)
                                                                },
                                                                colors = SwitchDefaults.colors(
                                                                    checkedThumbColor = Color.Green,
                                                                    uncheckedThumbColor = Color.Gray
                                                                )
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        Text("Adjust Sensitivity", color = Color.White)
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Slider(
                                                                value = sensitivityRT,
                                                                onValueChange = { newValue ->
                                                                    sensitivityRT = newValue
                                                                    CoroutineScope(Dispatchers.Main).launch {
                                                                        setSensitivityRT(context, newValue)
                                                                    }
                                                                },
                                                                valueRange = 1000f..5000f,
                                                                steps = 49,
                                                                modifier = Modifier.width(150.dp),
                                                                colors = SliderDefaults.colors(
                                                                    thumbColor = Color.White, // Color of the thumb
                                                                    activeTrackColor = Color.Black, // Color of the active track
                                                                    inactiveTrackColor = Color.Black, // Color of the inactive track
                                                                    activeTickColor = Color.Red, // Color of the active ticks
                                                                    inactiveTickColor = Color.Red // Color of the inactive ticks
                                                                )
                                                            )
                                                            Text(
                                                                text = "${sensitivityRT.roundToInt()}",
                                                                fontSize = 24.sp,
                                                                color = Color.White
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(16.dp))

                                                        Row {
                                                            Button(
                                                                onClick = {
                                                                    UIStateManager.changeAllButtonColorsAndAlpha(hexCode, buttonAlpha)
                                                                    // Force a recomposition by toggling the state
                                                                    globalColorChange = !globalColorChange
                                                                },
                                                                modifier = Modifier.width(IntrinsicSize.Max) // Adjust to the button's intrinsic width
                                                            ) {
                                                                Text(
                                                                    text = "Apply Globally",
                                                                    color = Color.White,
                                                                    fontSize = 12.sp // Smaller font size to fit the text
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.weight(1f))
                                                            Button(onClick = {
                                                                showControlsPopup = false
                                                                saveState()
                                                            }) {
                                                                Text("Close", color = Color.White)
                                                            }

                                                        }
                                                    }
                                                    if (colors) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        ColorPickerWheel(
                                                            initialColor = buttonColor,
                                                            onColorSelected = { color, hex, alphaValue ->
                                                                hexCode = hex
                                                                buttonAlpha = alphaValue
                                                                buttonColor = color
                                                                saveState()  // Save state whenever color changes
                                                                Log.d("ColorWheel", "Selected Color: $color, Hex: $hex, Alpha: $alphaValue")
                                                            }
                                                        )
                                                    }
                                                    if (info) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "offsetX: ${offsetX.floatValue}, offsetY: ${offsetY.floatValue}",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        if (buttonState.uri != null) {
                                                            Text(
                                                                "Icon Selected: ${buttonState.uri}",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                                VerticalScrollbar(
                                                    modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().width(10.dp)
                                                ) {
                                                    Thumb(
                                                        modifier = Modifier.background(
                                                            Color.Red.copy(0.3f), RoundedCornerShape(100)
                                                        ),
                                                    )
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
