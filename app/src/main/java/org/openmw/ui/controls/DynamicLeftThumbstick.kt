package org.openmw.ui.controls

import android.content.Context
import android.util.Log
import android.view.KeyEvent
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
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.libsdl.app.SDLActivity.onNativeKeyDown
import org.libsdl.app.SDLActivity.onNativeKeyUp
import org.openmw.isControllerConnected
import org.openmw.ui.controls.UIStateManager.configureControls
import org.openmw.ui.controls.UIStateManager.customColor
import org.openmw.ui.controls.UIStateManager.editMode
import org.openmw.ui.controls.UIStateManager.globalColorChange
import org.openmw.ui.controls.UIStateManager.isCursorVisible
import org.openmw.ui.controls.UIStateManager.isThumbDragging
import org.openmw.ui.controls.UIStateManager.menuAlpha
import org.openmw.ui.controls.UIStateManager.menuColor
import org.openmw.ui.controls.UIStateManager.offsetXFlow
import org.openmw.ui.controls.UIStateManager.offsetYFlow
import org.openmw.ui.controls.UIStateManager.updateButtonState
import org.openmw.ui.overlay.getAnimations
import org.openmw.utils.ColorPickerWheel
import org.openmw.utils.GameFilesPreferences
import org.openmw.utils.GameFilesPreferences.getSelectedAnimation
import org.openmw.utils.GameFilesPreferences.getSelectedKeycodes
import org.openmw.utils.GameFilesPreferences.loadAutoMouseMode
import org.openmw.utils.fromHex
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ResizableDraggableThumbstick(
    context: Context,
    containerWidth: Float,
    containerHeight: Float,
) {
    val buttonStates by UIStateManager.buttonStates.collectAsState()
    val buttonState = buttonStates[99]
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
        var hexCode by remember { mutableStateOf(state.color) } // Set initial hex code directly from state
        var buttonColor by remember { mutableStateOf(Color.fromHex(hexCode)) }
        var buttonAlpha by remember { mutableFloatStateOf(state.alpha) }
        val offsetX by offsetXFlow.collectAsState()
        val offsetY by offsetYFlow.collectAsState()
        val scrollState = rememberScrollState()
        val stateSB = rememberScrollAreaState(scrollState)
        val selectedAnimation by getSelectedAnimation(context).collectAsState(initial = "None")
        val autoMouseMode by loadAutoMouseMode(context).collectAsState(initial = "Hybrid")
        var offset by remember { mutableStateOf(IntOffset.Zero) }
        val density = LocalDensity.current
        val radiusPx = with(density) { (buttonSize / 2).toPx() }
        val deadZone = 0.2f * radiusPx
        var touchState by remember { mutableStateOf(Offset(0f, 0f)) }
        var showControlsPopup by remember { mutableStateOf(false) }
        val isUIHidden by GameFilesPreferences.loadUIState(context).collectAsState(initial = false)
        val buttonGroupSwitch by GameFilesPreferences.getButtonGroupSwitch(context).collectAsState(initial = false)
        val controllerConnected = isControllerConnected(context)

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

        // Manage whats in the tabs here
        var options by remember { mutableStateOf(true) }
        var colors by remember { mutableStateOf(false) }
        var info by remember { mutableStateOf(false) }

        fun resetStates() {
            options = false
            colors = false
            info = false
        }

        val thumbColor =
            if (isThumbDragging) Color.Red.copy(alpha = buttonAlpha) else buttonColor.copy(
                alpha = buttonAlpha
            )

        menuColor = buttonColor
        menuAlpha = buttonAlpha

        val saveState = {
            val updatedState = state.copy(
                size = buttonSize.value,
                offsetX = offsetX,
                offsetY = offsetY,
                color = hexCode,
                alpha = buttonAlpha,
                group = 1
            )
            updateButtonState(buttonState.id, updatedState)
            UIStateManager.saveButtonState(containerWidth, containerHeight)
        }

        val animations = getAnimations().toMutableMap().apply { put("None", EnterTransition.None to ExitTransition.None) }
        val (currentEnterAnimation, currentExitAnimation) = animations[selectedAnimation] ?: (animations["Slide Vertically"] ?: error("Default animation not found"))

        val selectedKeycodesFlow = getSelectedKeycodes(context).map { keycodeString ->
            keycodeString.split(",").map { it.toInt() }
        }
        val selectedKeycodes by selectedKeycodesFlow.collectAsState(initial = emptyList())
        val isOnMouseScreen by derivedStateOf { 99 in selectedKeycodes }
        val visibility = when {
            isCursorVisible == 0 && autoMouseMode == "None" -> !isUIHidden // Set to isUIHidden if autoMouseMode is None
            isCursorVisible == 0 && autoMouseMode == "Hybrid" || 99 in selectedKeycodes -> !isUIHidden
            isCursorVisible == 0 && autoMouseMode == "Hybrid" || 99 !in selectedKeycodes -> isUIHidden
            isCursorVisible == 0 -> !isUIHidden
            else -> !isUIHidden
        }

        val visibilityFinal = if (controllerConnected) {
            isUIHidden
        } else {
            visibility
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
                    .then(
                        if (editMode) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        isThumbDragging = true
                                    },
                                    onDrag = { _, dragAmount ->
                                        updateOffsets(offsetX + dragAmount.x, offsetY + dragAmount.y)
                                    },
                                    onDragEnd = {
                                        isThumbDragging = false
                                        saveState()
                                    }
                                )
                            }
                        } else Modifier
                    ),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(buttonSize)
                        .background(Color.Transparent)
                        .border(2.dp, thumbColor, CircleShape)
                        .align(Alignment.Center)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .border(2.dp, thumbColor, CircleShape)
                            .then(
                                Modifier.pointerInput(Unit) {
                                    if (!configureControls) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val location = Offset(event.changes.first().position.x, event.changes.first().position.y)
                                                when (event.type) {
                                                    PointerEventType.Press, PointerEventType.Move -> {
                                                        val newX = (location.x - radiusPx).coerceIn(-radiusPx, radiusPx)
                                                        val newY = (location.y - radiusPx).coerceIn(-radiusPx, radiusPx)
                                                        touchState = Offset(newX, newY)

                                                        // Handle touch actions
                                                        onNativeKeyUp(KeyEvent.KEYCODE_W)
                                                        onNativeKeyUp(KeyEvent.KEYCODE_A)
                                                        onNativeKeyUp(KeyEvent.KEYCODE_S)
                                                        onNativeKeyUp(KeyEvent.KEYCODE_D)

                                                        val xRatio = touchState.x / radiusPx
                                                        val yRatio = touchState.y / radiusPx

                                                        when {
                                                            abs(yRatio) > abs(xRatio) -> {
                                                                if (touchState.y < -deadZone) onNativeKeyDown(KeyEvent.KEYCODE_W)
                                                                if (touchState.y > deadZone) onNativeKeyDown(KeyEvent.KEYCODE_S)
                                                                if (touchState.x < -deadZone) onNativeKeyDown(KeyEvent.KEYCODE_A)
                                                                if (touchState.x > deadZone) onNativeKeyDown(KeyEvent.KEYCODE_D)
                                                            }
                                                            abs(xRatio) > 0.9f -> {
                                                                if (touchState.y < -deadZone) onNativeKeyDown(KeyEvent.KEYCODE_W)
                                                                if (touchState.y > deadZone) onNativeKeyDown(KeyEvent.KEYCODE_S)
                                                                if (touchState.x < -deadZone) onNativeKeyDown(KeyEvent.KEYCODE_A)
                                                                if (touchState.x > deadZone) onNativeKeyDown(KeyEvent.KEYCODE_D)
                                                            }
                                                            else -> {
                                                                if (touchState.y < -deadZone) onNativeKeyDown(KeyEvent.KEYCODE_W)
                                                                if (touchState.y > deadZone) onNativeKeyDown(KeyEvent.KEYCODE_S)
                                                                if (touchState.x < -deadZone) onNativeKeyDown(KeyEvent.KEYCODE_A)
                                                                if (touchState.x > deadZone) onNativeKeyDown(KeyEvent.KEYCODE_D)
                                                            }
                                                        }
                                                    }
                                                    PointerEventType.Release, PointerEventType.Exit -> {
                                                        touchState = Offset.Zero
                                                        onNativeKeyUp(KeyEvent.KEYCODE_W)
                                                        onNativeKeyUp(KeyEvent.KEYCODE_A)
                                                        onNativeKeyUp(KeyEvent.KEYCODE_S)
                                                        onNativeKeyUp(KeyEvent.KEYCODE_D)
                                                    }
                                                    else -> Unit
                                                }
                                            }
                                        }
                                    }
                                }

                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(25.dp)
                                .offset {
                                    val offsetXx = (touchState.x / density.density).coerceIn(
                                        -radiusPx / density.density,
                                        radiusPx / density.density
                                    ).dp
                                    val offsetYy = (touchState.y / density.density).coerceIn(
                                        -radiusPx / density.density,
                                        radiusPx / density.density
                                    ).dp
                                    IntOffset(offsetXx.roundToPx(), offsetYy.roundToPx())
                                }
                                .background(
                                    thumbColor,
                                    shape = CircleShape
                                )
                                .border(2.dp, thumbColor, CircleShape)
                        )
                    }
                    // Small button at the top right outside the border, relative to buttonSize
                    if (buttonGroupSwitch) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(
                                    x = -(buttonSize / 20).value.dp,
                                    y = (buttonSize / 20).value.dp
                                )
                                .size(20.dp)
                                .background(Color.Transparent, shape = CircleShape)
                                .clickable {
                                    UIStateManager.buttonsGroup =
                                        if (UIStateManager.buttonsGroup == 1) 2 else 1
                                }
                                .border(1.dp, thumbColor, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${UIStateManager.buttonsGroup}",
                                color = Color.White,
                                fontSize = 8.sp,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
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
                                                    text = "ID: 99, Size: ${buttonSize.value}",
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

                                                        Text(text = "ID: 99", color = Color.White)
                                                        Text(
                                                            text = "Size: ${buttonSize.value}",
                                                            color = Color.White
                                                        )
                                                        Spacer(modifier = Modifier.height(16.dp))
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
                                                        }
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            modifier = Modifier.height(32.dp)
                                                        ) {
                                                            Text(
                                                                text = "Is this Thumbstick added to the mouse UI?",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            // Checkbox to add/remove keyCode from data store
                                                            Checkbox(
                                                                checked = isOnMouseScreen,
                                                                onCheckedChange = { checked ->
                                                                    val updatedKeycodes = if (checked) {
                                                                        selectedKeycodes + 99
                                                                    } else {
                                                                        selectedKeycodes - 99
                                                                    }.joinToString(",")

                                                                    CoroutineScope(Dispatchers.Main).launch {
                                                                        GameFilesPreferences.setSelectedKeycodes(context, updatedKeycodes)
                                                                    }
                                                                }
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
                                                            text = "offsetX: ${offsetX}, offsetY: ${offsetY}",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )
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

// Function to update offsets
fun updateOffsets(newOffsetX: Float, newOffsetY: Float) {
    offsetXFlow.value = newOffsetX
    offsetYFlow.value = newOffsetY
}