package org.openmw.ui.controls

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import coil.compose.rememberImagePainter
import com.composables.core.ScrollArea
import com.composables.core.Thumb
import com.composables.core.VerticalScrollbar
import com.composables.core.rememberScrollAreaState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.libsdl.app.SDLActivity
import org.libsdl.app.SDLActivity.isMouseShown
import org.libsdl.app.SDLActivity.onNativeKeyDown
import org.libsdl.app.SDLActivity.onNativeKeyUp
import org.openmw.EngineActivity
import org.openmw.isControllerConnected
import org.openmw.ui.controls.UIStateManager.configureControls
import org.openmw.ui.controls.UIStateManager.customColor
import org.openmw.ui.controls.UIStateManager.editMode
import org.openmw.ui.controls.UIStateManager.globalColorChange
import org.openmw.ui.controls.UIStateManager.highlightStep
import org.openmw.ui.controls.UIStateManager.isCursorVisible
import org.openmw.ui.controls.UIStateManager.updateButtonState
import org.openmw.ui.overlay.getAnimations
import org.openmw.utils.ColorPickerWheel
import org.openmw.utils.CustomIconPickerButton
import org.openmw.utils.GameFilesPreferences
import org.openmw.utils.GameFilesPreferences.getSelectedAnimation
import org.openmw.utils.GameFilesPreferences.getSelectedKeycodes
import org.openmw.utils.GameFilesPreferences.getSensitivityMouse
import org.openmw.utils.GameFilesPreferences.getSensitivityRT
import org.openmw.utils.GameFilesPreferences.loadAutoMouseMode
import org.openmw.utils.GameFilesPreferences.setTutorial
import org.openmw.utils.GameFilesPreferences.setWhatsNew
import org.openmw.utils.currentDeviceRealSize
import org.openmw.utils.fromHex
import org.openmw.utils.vibrate
import kotlin.collections.joinToString
import kotlin.math.roundToInt

@OptIn(InternalCoroutinesApi::class)
@Suppress("DEPRECATION")
@Composable
fun ResizableDraggableButton(
    context: Context,
    id: Int,
    keyCode: Int,
    containerWidth: Float,
    containerHeight: Float,
    sdlView: View,
    onDelete: (Int) -> Unit
) {
    val buttonStates by UIStateManager.buttonStates.collectAsState()
    val buttonState = buttonStates[id]
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
        val buttonSize = remember { mutableStateOf(state.size.dp) }
        var offset by remember { mutableStateOf(IntOffset.Zero) }
        var isPressed by remember { mutableStateOf(false) }
        var showControlsPopup by remember { mutableStateOf(false) }
        val isUIHidden by GameFilesPreferences.loadUIState(context).collectAsState(initial = false)
        val isDragging = remember { mutableStateOf(false) }
        val isToggle = remember { mutableStateOf(false) }
        val offsetX = remember { mutableFloatStateOf(state.offsetX) }
        val offsetY = remember { mutableFloatStateOf(state.offsetY) }
        val snapX = remember { mutableStateOf<Float?>(null) }
        val snapY = remember { mutableStateOf<Float?>(null) }
        val painter: Painter? = state.uri?.let { rememberImagePainter(it) }
        val buttonUri = remember { mutableStateOf(state.uri) } // Handling URI state
        var hexCode by remember { mutableStateOf(state.color) } // Set initial hex code directly from state
        var buttonColor by remember { mutableStateOf(Color.fromHex(hexCode)) }
        var buttonAlpha by remember { mutableFloatStateOf(state.alpha) }
        val scrollState = rememberScrollState()
        val stateSB = rememberScrollAreaState(scrollState)
        val autoMouseMode by loadAutoMouseMode(context).collectAsState(initial = "Hybrid")
        val selectedAnimation by getSelectedAnimation(context).collectAsState(initial = "None")
        val sensitivityMouseFlow = getSensitivityMouse(context).collectAsState(initial = 5000f)
        var sensitivityMouse by remember { mutableFloatStateOf(sensitivityMouseFlow.value ?: 5000f) }
        val sensitivityRTFlow = getSensitivityRT(context).collectAsState(initial = 5000f)
        var sensitivityRT by remember { mutableFloatStateOf(sensitivityRTFlow.value ?: 5000f) }
        val tutorial by GameFilesPreferences.getTutorial(context).collectAsState(initial = false)
        val isVibrationOn by GameFilesPreferences.loadVibrationState(context).collectAsState(initial = true)
        val buttonGroupSwitch by GameFilesPreferences.getButtonGroupSwitch(context).collectAsState(initial = true)
        val animations = getAnimations().toMutableMap().apply { put("None", EnterTransition.None to ExitTransition.None) }
        val (currentEnterAnimation, currentExitAnimation) = animations[selectedAnimation] ?: (animations["None"] ?: error("Default animation not found"))
        val controllerConnected = isControllerConnected(context)
        val scope = rememberCoroutineScope()

        val animatedOffsetX = remember { Animatable(offsetX.floatValue) }
        val animatedOffsetY = remember { Animatable(offsetY.floatValue) }

        // Get the selected key codes from the data store as a flow of lists of integers
        val selectedKeycodesFlow = getSelectedKeycodes(context).map { keycodeString ->
            keycodeString.split(",").map { it.toInt() }
        }

        val selectedKeycodes by selectedKeycodesFlow.collectAsState(initial = emptyList())

        val isOnMouseScreen by derivedStateOf { keyCode in selectedKeycodes }

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

        LaunchedEffect(sensitivityMouseFlow.value) {
            sensitivityMouse = sensitivityMouseFlow.value ?: 5000f
        }
        LaunchedEffect(sensitivityRTFlow.value) {
            sensitivityRT = sensitivityRTFlow.value ?: 5000f
        }

        if (configureControls && tutorial && id == 999) {
            if (highlightStep == 4) {
                LaunchedEffect(Unit) {
                    // Define target values for the animation (e.g., move 100 pixels in each direction)
                    val targetX = offsetX.floatValue + 300f
                    val targetY = offsetY.floatValue + 150f

                    // Animate to target values over 3 seconds
                    launch {
                        animatedOffsetX.animateTo(
                            targetValue = targetX,
                            animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                        )
                        // After animation, update offsetX to the final value
                        offsetX.floatValue = animatedOffsetX.value
                    }
                    launch {
                        animatedOffsetY.animateTo(
                            targetValue = targetY,
                            animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                        )
                        // After animation, update offsetY to the final value
                        offsetY.floatValue = animatedOffsetY.value
                        highlightStep++
                    }
                }
            }
        }

        val saveState = {
            val updatedState = state.copy(
                size = buttonSize.value.value,
                offsetX = offsetX.floatValue,
                offsetY = offsetY.floatValue,
                color = hexCode, // Use the hex string conversion
                alpha = buttonAlpha,
                uri = buttonUri.value,
                group = state.group
            )
            updateButtonState(buttonState.id, updatedState)
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

        // Separate function to save group changes
        val saveGroup = { group: Int ->
            val updatedState = state.copy(group = group)
            updateButtonState(buttonState.id, updatedState)
            UIStateManager.saveButtonState(containerWidth, containerHeight)
        }

        // Final visibility state
        val isVisible = when {
            isCursorVisible != 0 && autoMouseMode == "None" && state.group == UIStateManager.buttonsGroup && keyCode == 111 -> !isUIHidden
            isCursorVisible == 0 && autoMouseMode == "None" && state.group == UIStateManager.buttonsGroup -> !isUIHidden
            isCursorVisible != 0 && autoMouseMode != "None" && state.group == UIStateManager.buttonsGroup -> !isUIHidden && keyCode in selectedKeycodes
            isCursorVisible != 0 && autoMouseMode != "None" && state.group != UIStateManager.buttonsGroup -> !isUIHidden && keyCode in selectedKeycodes
            isCursorVisible == 0 && autoMouseMode != "None" && state.group == UIStateManager.buttonsGroup -> !isUIHidden
            else -> false
        }
        val visibilityFinal = if (controllerConnected) {
            isUIHidden
        } else {
            isVisible
        }

        AnimatedVisibility(
            visible = visibilityFinal,
            enter = currentEnterAnimation,
            exit = currentExitAnimation
        ) {

            Box(
                modifier = Modifier
                    .size(buttonSize.value)
                    .background(Color.Transparent)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(buttonSize.value)
                        .background(Color.Transparent)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (if (highlightStep == 4 && id == 999) animatedOffsetX.value else offsetX.floatValue).roundToInt(),
                                    (if (highlightStep == 4 && id == 999) animatedOffsetY.value else offsetY.floatValue).roundToInt()
                                )
                            }
                            .border(
                                2.dp,
                                if (isDragging.value && painter == null) Color.Red else if (painter == null) Color.Black else Color.Transparent,
                                shape = CircleShape
                            )
                            .size(buttonSize.value)
                            .then(
                                if (editMode) {
                                    Modifier.pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = {
                                                isDragging.value = true
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                offsetX.floatValue += dragAmount.x
                                                offsetY.floatValue += dragAmount.y

                                                // Calculate snap points with current grid size
                                                val currentGridSize = UIStateManager.gridSize.intValue
                                                snapX.value = ((offsetX.floatValue / currentGridSize).roundToInt() * currentGridSize).toFloat()
                                                snapY.value = ((offsetY.floatValue / currentGridSize).roundToInt() * currentGridSize).toFloat()

                                            },
                                            onDragEnd = {
                                                isDragging.value = false
                                                // Snap to nearest grid
                                                val currentGridSize = UIStateManager.gridSize.intValue
                                                offsetX.floatValue = ((offsetX.floatValue / currentGridSize).roundToInt() * currentGridSize).toFloat()
                                                offsetY.floatValue = ((offsetY.floatValue / currentGridSize).roundToInt() * currentGridSize).toFloat()

                                                saveState()
                                                if (configureControls && tutorial && id == 999 && highlightStep == 5) {
                                                    highlightStep++
                                                }
                                            },
                                            onDragCancel = {
                                                isDragging.value = false
                                            }
                                        )
                                    }
                                } else Modifier
                            )
                    ) {
                        // Main button
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (painter != null) {
                                        Color.Transparent
                                    } else {
                                        if (isPressed) buttonColor.copy(alpha = 1.0f) else buttonColor.copy(
                                            alpha = buttonAlpha
                                        )
                                    },
                                    shape = CircleShape
                                )
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        if (!configureControls) {
                                            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                                            val (screenWidth, screenHeight) = windowManager.currentDeviceRealSize()
                                            val sdlWidth = EngineActivity.resolutionX.toFloat()
                                            val sdlHeight = EngineActivity.resolutionY.toFloat()

                                            val startX = SDLActivity.getMouseX().toFloat() * (screenWidth / sdlWidth)
                                            val startY = SDLActivity.getMouseY().toFloat() * (screenHeight / sdlHeight)

                                            var curX = startX
                                            var curY = startY
                                            var draggingStarted = false
                                            var downFrom = 0L

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val down = event.changes.firstOrNull()?.pressed == true

                                                if (down) {
                                                    downFrom = System.currentTimeMillis()
                                                    // Handle the onPress event
                                                    if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT ||
                                                        keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT ||
                                                        keyCode == KeyEvent.KEYCODE_CTRL_LEFT || keyCode == KeyEvent.KEYCODE_CTRL_RIGHT
                                                    ) {
                                                        isPressed = !isPressed
                                                        if (isPressed) {
                                                            onNativeKeyDown(keyCode)
                                                            isToggle.value = true
                                                        } else {
                                                            isToggle.value = false
                                                            onNativeKeyUp(keyCode)
                                                        }
                                                    } else {
                                                        when (keyCode) {
                                                            KeyEvent.KEYCODE_Z -> {
                                                                if (isCursorVisible == 1) {
                                                                    SDLActivity.sendMouseButton(1, 1)
                                                                } else {
                                                                    if (isVibrationOn) {
                                                                        vibrate(context)
                                                                    }
                                                                }
                                                            }
                                                            KeyEvent.KEYCODE_E -> {
                                                                if (isVibrationOn) {
                                                                    vibrate(context)
                                                                }
                                                            }
                                                        }
                                                        onNativeKeyDown(keyCode)
                                                        if (keyCode == KeyEvent.KEYCODE_B) {
                                                            onNativeKeyUp(keyCode)
                                                            if (isCursorVisible == 1) {
                                                                //sendMouseClick(sdlView)
                                                                SDLActivity.sendMouseButton(1, 2)
                                                            }
                                                        }
                                                    }
                                                    while (true) {
                                                        val dragEvent = awaitPointerEvent()
                                                        val dragChange = dragEvent.changes.firstOrNull()

                                                        if (dragChange?.pressed == true) {
                                                            val newX = dragChange.position.x
                                                            val newY = dragChange.position.y

                                                            if (!draggingStarted) {
                                                                curX = newX
                                                                curY = newY
                                                                draggingStarted = true
                                                            }

                                                            if (System.currentTimeMillis() - downFrom > 100) {
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

                                                                // Call the native function with updated coordinates
                                                                SDLActivity.sendRelativeMouseMotion(
                                                                    movementX.roundToInt(),
                                                                    movementY.roundToInt()
                                                                )

                                                                // Update current positions
                                                                curX = newX
                                                                curY = newY
                                                            }
                                                        } else {
                                                            if (!draggingStarted) {
                                                                // No action required
                                                            }
                                                            draggingStarted = false
                                                            break
                                                        }
                                                    }
                                                }
                                                // End the press event
                                                if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT ||
                                                    keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT ||
                                                    keyCode == KeyEvent.KEYCODE_CTRL_LEFT || keyCode == KeyEvent.KEYCODE_CTRL_RIGHT
                                                ) {
                                                    // Do nothing for SHIFT or ALT keys as they toggle
                                                } else {
                                                    onNativeKeyUp(keyCode)
                                                    if (isMouseShown() == 1) {
                                                        SDLActivity.sendMouseButton(0, 1)
                                                        SDLActivity.sendMouseButton(0, 2)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },

                            contentAlignment = Alignment.Center
                        ) {
                            if (painter != null) {
                                Image(
                                    painter = painter,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(buttonAlpha),
                                    colorFilter = if (isToggle.value) {
                                        ColorFilter.tint(buttonColor.copy(alpha = 1.0f))
                                    } else {
                                        ColorFilter.tint(buttonColor.copy(alpha = buttonAlpha))
                                    }
                                )
                            } else {
                                Text(
                                    text = keyCodeToChar(keyCode),
                                    color = Color.White,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            if (editMode) {
                                Text(
                                    text = "ID: $id, Key: ${keyCodeToChar(keyCode)}",
                                    color = Color.White
                                )
                            }
                        }
                        if (editMode) {
                            val iconTint = if (configureControls && tutorial && highlightStep == 6 && id == 999) {
                                Color.Yellow
                            } else {
                                Color.Red
                            }
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
                                    tint = iconTint,
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
                                                        text = "ID: $id, Key: ${keyCodeToChar(keyCode)}, Size: ${buttonSize.value}",
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
                                                        if (highlightStep == 6 && id == 999) {
                                                            Text(
                                                                text = "This is where you can edit each button / thumbstick.\nScroll to the bottom of this box and\npress close to finish the tutorial.",
                                                                style = TextStyle(
                                                                    color = White,
                                                                    fontSize = 20.sp
                                                                )
                                                            )
                                                        }
                                                        if (options) {
                                                            var showPicker by remember { mutableStateOf(false) }
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                modifier = Modifier.height(32.dp)
                                                            ) {
                                                                Text(
                                                                    text = "Is this button ($keyCode) added to the mouse UI?",
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                                // Checkbox to add/remove keyCode from data store
                                                                Checkbox(
                                                                    checked = isOnMouseScreen,
                                                                    onCheckedChange = { checked ->
                                                                        val updatedKeycodes = if (checked) {
                                                                            selectedKeycodes + keyCode
                                                                        } else {
                                                                            selectedKeycodes - keyCode
                                                                        }.joinToString(",")

                                                                        CoroutineScope(Dispatchers.Main).launch {
                                                                            GameFilesPreferences.setSelectedKeycodes(context, updatedKeycodes)
                                                                        }
                                                                    }
                                                                )
                                                            }
                                                            HorizontalDivider(color = Color.White, thickness = 1.dp)
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = "Increase / Decrease Size",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.height(32.dp)
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
                                                                            buttonSize.value += 10.dp
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
                                                                Spacer(modifier = Modifier.width(10.dp))

                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(30.dp)
                                                                        .background(
                                                                            Color.Black,
                                                                            shape = CircleShape
                                                                        )
                                                                        .clickable {
                                                                            buttonSize.value -= 10.dp
                                                                            if (buttonSize.value < 50.dp) buttonSize.value =
                                                                                30.dp // Minimum size
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
                                                                Spacer(modifier = Modifier.weight(1f))
                                                                Text(
                                                                    text = "Delete Button",
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(30.dp)
                                                                        .background(
                                                                            Color.Red,
                                                                            shape = CircleShape
                                                                        )
                                                                        .clickable {
                                                                            onDelete(id)
                                                                        }
                                                                        .border(
                                                                            2.dp,
                                                                            Color.White,
                                                                            shape = CircleShape
                                                                        ),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        text = "X",
                                                                        color = Color.White,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                            if (buttonGroupSwitch) {
                                                                Spacer(modifier = Modifier.height(10.dp))
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp), // Reduced spacing
                                                                    modifier = Modifier.height(32.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "Select Button Group:",
                                                                        color = Color.White
                                                                    )
                                                                    Spacer(modifier = Modifier.weight(1f))
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.spacedBy(4.dp) // Reduced spacing between radio buttons and texts
                                                                    ) {
                                                                        RadioButton(
                                                                            selected = state.group == 1,
                                                                            onClick = {
                                                                                saveGroup(1)
                                                                            },
                                                                            colors = RadioButtonDefaults.colors(
                                                                                selectedColor = Color.White
                                                                            )
                                                                        )
                                                                        Text(text = "1", color = Color.White)
                                                                        RadioButton(
                                                                            selected = state.group == 2,
                                                                            onClick = {
                                                                                saveGroup(2)
                                                                            },
                                                                            colors = RadioButtonDefaults.colors(
                                                                                selectedColor = Color.White
                                                                            )
                                                                        )
                                                                        Text(text = "2", color = Color.White)
                                                                    }
                                                                }
                                                                HorizontalDivider(color = Color.White, thickness = 1.dp)
                                                            }
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Button(
                                                                onClick = { showPicker = true },
                                                            ) {
                                                                Text("Enable Pick Icon Button")
                                                            }
                                                            if (showPicker) {
                                                                CustomIconPickerButton(
                                                                    context = context,
                                                                    buttonId = id,
                                                                    buttonUri = buttonUri,
                                                                    containerWidth = containerWidth,
                                                                    containerHeight = containerHeight
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(2.dp))
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
                                                                Button(onClick = {
                                                                    showControlsPopup = false
                                                                    saveState()
                                                                    if (configureControls && tutorial && id == 999) {
                                                                        highlightStep++
                                                                        onDelete(id)
                                                                        scope.launch {
                                                                            setWhatsNew(context, false)
                                                                            setTutorial(context, false)
                                                                        }
                                                                    }
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
                                                                text = "offsetX: ${offsetX.floatValue}\noffsetY: ${offsetY.floatValue}",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "button keycode = ($keyCode)",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "Belongs to button Group = (${state.group})",
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
}

fun keyCodeToChar(keyCode: Int): String {
    return when (keyCode) {
        in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 -> "F${keyCode - KeyEvent.KEYCODE_F1 + 1}"
        KeyEvent.KEYCODE_SHIFT_LEFT -> "Shift-L"
        KeyEvent.KEYCODE_SHIFT_RIGHT -> "Shift-R"
        KeyEvent.KEYCODE_CTRL_LEFT -> "Ctrl-L"
        KeyEvent.KEYCODE_CTRL_RIGHT -> "Ctrl-R"
        KeyEvent.KEYCODE_ALT_LEFT -> "Alt-L"
        KeyEvent.KEYCODE_ALT_RIGHT -> "Alt-R"
        KeyEvent.KEYCODE_SPACE -> "Space"
        KeyEvent.KEYCODE_ESCAPE -> "Escape"
        KeyEvent.KEYCODE_ENTER -> "Enter"
        KeyEvent.KEYCODE_GRAVE -> "Grave"
        KeyEvent.KEYCODE_TAB -> "Tab"
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> "${keyCode - KeyEvent.KEYCODE_0}"
        else -> (keyCode - KeyEvent.KEYCODE_A + 'A'.code).toChar().toString()
    }
}
