package org.openmw.ui.controls

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import org.libsdl.app.SDLActivity
import org.openmw.ui.controls.UIStateManager.configureControls
import org.openmw.ui.controls.UIStateManager.logAllButtonStates
import org.openmw.ui.controls.UIStateManager.updateButtonState
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ResizableDraggableRightThumbstick(
    context: Context,
    id: Int,
    editMode: Boolean
) {
    var buttonState = UIStateManager.buttonStates.getOrPut(id) {
        mutableStateOf(ButtonState(id, 200f, 300f, 300f, false, 98, "Black", 0.25f))
    }.value
    var buttonSize by remember { mutableStateOf(buttonState.size.dp) }
    var buttonColor = remember { mutableStateOf(buttonState.color.toColor()) }
    var buttonAlpha by remember { mutableFloatStateOf(buttonState.alpha) }
    var offsetX by remember { mutableFloatStateOf(buttonState.offsetX) }
    var offsetY by remember { mutableFloatStateOf(buttonState.offsetY) }
    var offset by remember { mutableStateOf(IntOffset.Zero) }
    val visible = UIStateManager.visible
    val density = LocalDensity.current
    val radiusPx = with(density) { (buttonSize / 2).toPx() }
    var isDragging = remember { mutableStateOf(false) }
    val initialOffset = with(LocalDensity.current) { Offset(buttonSize.toPx() / 2, buttonSize.toPx() / 2) }
    var touchOffset by remember { mutableStateOf(initialOffset) }
    var showControlsPopup by remember { mutableStateOf(false) }
    val RightThumbColor = if (isDragging.value) Color.Red.copy(alpha = buttonAlpha) else buttonColor.value.copy(alpha = buttonAlpha)
    var saveState = {
        var updatedState = buttonState.copy(
            size = buttonSize.value,
            offsetX = offsetX,
            offsetY = offsetY,
            color = buttonColor.value.toColorString(),
            alpha = buttonAlpha
        )
        UIStateManager.buttonStates[id]?.value = updatedState
        // Update the global state
        updateButtonState(buttonState.id, updatedState)
        saveButtonState(context, UIStateManager.buttonStates.values.map { it.value })
        logAllButtonStates()
    }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(buttonSize)
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .background(Color.Transparent)
                    .then(
                        if (editMode) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { isDragging.value = true },
                                    onDrag = { change, dragAmount ->
                                        offsetX += dragAmount.x
                                        offsetY += dragAmount.y
                                    },
                                    onDragEnd = {
                                        isDragging.value = false
                                        saveState()
                                    }
                                )
                            }
                        } else Modifier
                    )
                    .border(2.dp, if (isDragging.value) Color.Red else RightThumbColor, shape = CircleShape)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, RightThumbColor, CircleShape)
                        .then(
                            if (!configureControls) {
                                Modifier.pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        val displayMetrics = context.resources.displayMetrics
                                        val screenWidth = displayMetrics.widthPixels.toFloat()
                                        val screenHeight = displayMetrics.heightPixels.toFloat()
                                        var startX = SDLActivity.getMouseX().toFloat()
                                        var startY = SDLActivity.getMouseY().toFloat()
                                        val mouseScalingFactor = 900f // This can be configurable
                                        var curX = startX
                                        var curY = startY
                                        var draggingStarted = false
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val down = event.changes.firstOrNull()?.pressed == true
                                            if (down) {
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
                                                        val movementX = (newX - curX) * mouseScalingFactor / screenWidth
                                                        val movementY = (newY - curY) * mouseScalingFactor / screenHeight
                                                        touchOffset = Offset(newX, newY)

                                                        // Call the native function with updated coordinates
                                                        SDLActivity.sendRelativeMouseMotion(
                                                            movementX.roundToInt().toInt(), movementY.roundToInt()
                                                                .toInt()
                                                        )
                                                        // Update current positions
                                                        curX = newX
                                                        curY = newY
                                                        Log.d("DragMovement", "movementX: $movementX, movementY: $movementY")
                                                    } else {
                                                        // Consider it a tap if dragging did not start
                                                        if (!draggingStarted) {
                                                            Log.d("TapEvent", "Tap detected at x: $startX, y: $startY")
                                                        }
                                                        // End the drag event if the pointer is released
                                                        draggingStarted = false
                                                        touchOffset = Offset(buttonSize.toPx() / 2, buttonSize.toPx() / 2)
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
                                val offsetX = ((touchOffset.x - (buttonSize.toPx() / 2)) / density).coerceIn(-radiusPx, radiusPx).dp.roundToPx()
                                val offsetY = ((touchOffset.y - (buttonSize.toPx() / 2)) / density).coerceIn(-radiusPx, radiusPx).dp.roundToPx()
                                IntOffset(offsetX, offsetY)
                            }
                            .background(
                                RightThumbColor,
                                shape = CircleShape
                            )
                    )
                }
                if (UIStateManager.editMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(30.dp)
                            .background(Color.Gray, shape = CircleShape)
                            .clickable { showControlsPopup = true }
                            .border(2.dp, Color.White, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                    if (showControlsPopup) {
                        Popup(
                            alignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(300.dp)
                                        .offset { offset }
                                        .background(Color.Black.copy(alpha = 0.8f))
                                        .border(2.dp, Color.White, shape = RoundedCornerShape(8.dp))
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
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(text = "ID: $id", color = Color.White)
                                        Text(text = "Size: ${buttonSize.value}", color = Color.White)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // + button
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .background(Color.Black, shape = CircleShape)
                                                    .clickable {
                                                        buttonSize += 20.dp
                                                        saveState()
                                                    }
                                                    .border(2.dp, Color.White, shape = CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = "+", color = Color.White, fontWeight = FontWeight.Bold)
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .background(Color.Black, shape = CircleShape)
                                                    .clickable {
                                                        buttonSize -= 20.dp
                                                        if (buttonSize < 50.dp) buttonSize = 50.dp
                                                        saveState()
                                                    }
                                                    .border(2.dp, Color.White, shape = CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = "-", color = Color.White, fontWeight = FontWeight.Bold)
                                            }

                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Pick a color and set alpha", color = Color.White)
                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(
                                                listOf(
                                                    Color.Black,
                                                    Color.Gray,
                                                    Color.White,
                                                    Color.Red,
                                                    Color.Green,
                                                    Color.Blue,
                                                    Color.Yellow,
                                                    Color.Magenta,
                                                    Color.Cyan
                                                )
                                            ) { color ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(50.dp)
                                                        .background(color, shape = CircleShape)
                                                        .clickable {
                                                            buttonColor.value = color
                                                        }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Slider(
                                            value = buttonAlpha,
                                            onValueChange = { alpha ->
                                                buttonAlpha = alpha
                                            },
                                            valueRange = 0f..1f,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = "Alpha: ${"%.2f".format(buttonAlpha)}",
                                            color = Color.White
                                        )
                                        Row {
                                            Button(onClick = {
                                                showControlsPopup = false
                                                saveState()
                                            }) {
                                                Text("OK", color = Color.White)
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Button(onClick = { showControlsPopup = false }) {
                                                Text("Cancel", color = Color.White)
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
