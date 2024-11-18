package org.openmw.ui.controls

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import coil.compose.rememberImagePainter
import org.libsdl.app.SDLActivity
import org.libsdl.app.SDLActivity.onNativeKeyDown
import org.libsdl.app.SDLActivity.onNativeKeyUp
import org.openmw.ui.controls.UIStateManager.REQUEST_CODE_PICK_IMAGE
import org.openmw.ui.controls.UIStateManager.configureControls
import org.openmw.ui.controls.UIStateManager.logAllButtonStates
import org.openmw.ui.controls.UIStateManager.selectedImageUri
import org.openmw.utils.vibrate
import kotlin.math.roundToInt

fun pickImage(activity: Activity, requestCode: Int) {
    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    activity.startActivityForResult(intent, requestCode)
}

@Composable
fun CustomIconPickerButton(context: Context, activity: Activity) {
    Button(onClick = {
        pickImage(activity, REQUEST_CODE_PICK_IMAGE)
    }) {
        Text("Choose Icon", color = Color.White)
    }
}

@Suppress("DEPRECATION")
@Composable
fun ImagePreview(uri: Uri?) {
    if (uri != null) {
        val painter = rememberImagePainter(uri)
        Image(
            painter = painter,
            contentDescription = "Selected Image",
            modifier = Modifier
                .size(100.dp)
                .padding(8.dp)
        )
    }
}

@Suppress("DEPRECATION")
@Composable
fun ResizableDraggableButton(
    context: Context,
    id: Int,
    keyCode: Int,
    editMode: Boolean,
    onDelete: (Int) -> Unit
) {
    val buttonState = UIStateManager.buttonStates.getOrPut(id) {
        mutableStateOf(ButtonState(id, 60f, 0f, 0f, false, keyCode, "Black", 0.25f))
    }.value

    var buttonSize = remember { mutableStateOf(buttonState.size.dp) }
    var offset by remember { mutableStateOf(IntOffset.Zero) }
    var isPressed by remember { mutableStateOf(false) }
    val buttonColor = remember { mutableStateOf(buttonState.color.toColor()) }
    var buttonAlpha by remember { mutableFloatStateOf(buttonState.alpha) }
    var showControlsPopup by remember { mutableStateOf(false) }
    var iconUri by remember { mutableStateOf<Uri?>(null) }
    val density = LocalDensity.current
    val visible = UIStateManager.visible
    var isDragging = remember { mutableStateOf(false) }
    var offsetX = remember { mutableFloatStateOf(buttonState.offsetX) }
    var offsetY = remember { mutableFloatStateOf(buttonState.offsetY) }
    var snapX = remember { mutableStateOf<Float?>(null) }
    var snapY = remember { mutableStateOf<Float?>(null) }
    val painter: Painter? = selectedImageUri?.let { rememberImagePainter(it) }

    val saveState = {
        val updatedState = buttonState.copy(
            size = buttonSize.value.value,  // Adjust for .dp value
            offsetX = offsetX.floatValue,
            offsetY = offsetY.floatValue,
            color = buttonColor.value.toColorString(),
            alpha = buttonAlpha
        )
        UIStateManager.buttonStates[id]?.value = updatedState
        // Update the global state
        UIStateManager.updateButtonState(buttonState.id, updatedState)
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
                    .size(buttonSize.value)
                    .background(Color.Transparent)

            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                offsetX.floatValue.roundToInt(),
                                offsetY.floatValue.roundToInt()
                            )
                        }
                        .border(
                            2.dp,
                            if (isDragging.value) Color.Red else Color.Black,
                            shape = CircleShape
                        )
                        .size(buttonSize.value)
                        .then(
                            if (UIStateManager.editMode) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { isDragging.value = true },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            offsetX.floatValue += dragAmount.x
                                            offsetY.floatValue += dragAmount.y

                                            // Calculate snap points with current grid size
                                            val currentGridSize = UIStateManager.gridSize.intValue
                                            snapX.value =
                                                ((offsetX.floatValue / currentGridSize).roundToInt() * currentGridSize).toFloat()
                                            snapY.value =
                                                ((offsetY.floatValue / currentGridSize).roundToInt() * currentGridSize).toFloat()
                                            saveState()
                                        },
                                        onDragEnd = {
                                            isDragging.value = false
                                            // Snap to nearest grid
                                            val currentGridSize = UIStateManager.gridSize.intValue
                                            offsetX.floatValue =
                                                ((offsetX.floatValue / currentGridSize).roundToInt() * currentGridSize).toFloat()
                                            offsetY.floatValue =
                                                ((offsetY.floatValue / currentGridSize).roundToInt() * currentGridSize).toFloat()
                                            saveState()
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
                                    if (isPressed) buttonColor.value.copy(alpha = 1.0f) else buttonColor.value.copy(
                                        alpha = buttonAlpha
                                    )
                                },
                                shape = CircleShape
                            )
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    if (!configureControls) {
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

                                                // Handle the onPress event
                                                if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT ||
                                                    keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT
                                                ) {
                                                    isPressed = !isPressed
                                                    if (isPressed) {
                                                        onNativeKeyDown(keyCode)
                                                    } else {
                                                        onNativeKeyUp(keyCode)
                                                    }
                                                } else {
                                                    onNativeKeyDown(keyCode)
                                                    onNativeKeyUp(keyCode)
                                                }
                                                if (keyCode == KeyEvent.KEYCODE_Z) {
                                                    onNativeKeyDown(keyCode)
                                                    onNativeKeyDown(KeyEvent.KEYCODE_ENTER)
                                                    if (UIStateManager.isScaleView) {
                                                        TODO()
                                                    } else {
                                                        onNativeKeyDown(KeyEvent.KEYCODE_ENTER)
                                                    }
                                                    if (UIStateManager.isVibrationEnabled) {
                                                        vibrate(context)
                                                    }
                                                } else if (keyCode == KeyEvent.KEYCODE_E) {
                                                    onNativeKeyDown(keyCode)
                                                    if (UIStateManager.isVibrationEnabled) {
                                                        vibrate(context)
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

                                                        val movementX =
                                                            (newX - curX) * mouseScalingFactor / screenWidth
                                                        val movementY =
                                                            (newY - curY) * mouseScalingFactor / screenHeight

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
                                                        // Consider it a tap if dragging did not start
                                                        if (!draggingStarted) {
                                                            Log.d(
                                                                "TapEvent",
                                                                "Tap detected at x: $startX, y: $startY"
                                                            )
                                                        }

                                                        // End the drag event if the pointer is released
                                                        draggingStarted = false
                                                        break
                                                    }
                                                }

                                                // End the press event
                                                if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT ||
                                                    keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT
                                                ) {
                                                    // Do nothing for SHIFT or ALT keys as they toggle
                                                } else {
                                                    onNativeKeyUp(keyCode)
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
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = keyCodeToChar(keyCode),
                                color = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        if (UIStateManager.editMode) {
                            Text(
                                text = "ID: $id, Key: ${keyCodeToChar(keyCode)}",
                                color = Color.White
                            )
                        } else {
                            Text(text = keyCodeToChar(keyCode), color = Color.White)
                        }
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
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.White
                            )
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
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(text = "ID: $id", color = Color.White)
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
                                                Spacer(modifier = Modifier.width(8.dp))
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
                                                                50.dp // Minimum size
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
                                                Spacer(modifier = Modifier.width(60.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .background(Color.Red, shape = CircleShape)
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
                                            //Spacer(modifier = Modifier.height(16.dp))
                                            //Text("Select Custom Icon", color = Color.White)
                                            //CustomIconPickerButton(context, context as Activity)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            if (iconUri != null) {
                                                Text("Icon Selected: $iconUri", color = Color.White)
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
        else -> (keyCode - KeyEvent.KEYCODE_A + 'A'.code).toChar().toString()
    }
}
