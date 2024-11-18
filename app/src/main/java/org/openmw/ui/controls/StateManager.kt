package org.openmw.ui.controls

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.openmw.Constants
import java.io.File

data class ButtonState(
    var id: Int,
    var size: Float,
    var offsetX: Float,
    var offsetY: Float,
    var isLocked: Boolean,
    var keyCode: Int,
    var color: String,
    var alpha: Float
)

object UIStateManager {
    var isUIHidden by mutableStateOf(false)
    var visible by mutableStateOf(true)
    var isVibrationEnabled by mutableStateOf(true)
    var isScaleView by mutableStateOf(false)
    var isCursorVisible by mutableStateOf(false)

    // Add the shared states
    var memoryInfoText by mutableStateOf("")
    var batteryStatus by mutableStateOf("")
    var isMemoryInfoEnabled by mutableStateOf(false)
    var isBatteryStatusEnabled by mutableStateOf(false)
    var isLoggingEnabled by mutableStateOf(false)
    var isLogcatEnabled by mutableStateOf(false)
    var editMode by mutableStateOf(false)
    var isThumbDragging by mutableStateOf(false)
    var buttonStates = mutableMapOf<Int, MutableState<ButtonState>>()
    var useNavmesh by mutableStateOf(false)
    val gridSize = mutableIntStateOf(50)
    val gridVisible = mutableStateOf(false)
    val gridAlpha = mutableFloatStateOf(0.25f)
    val createdButtons = mutableStateListOf<ButtonState>()
    const val REQUEST_CODE_PICK_IMAGE = 1001
    var selectedImageUri: Uri? by mutableStateOf(null)
    var configureControls by mutableStateOf(false)

    fun updateButtonState(id: Int, state: ButtonState) {
        if (false) {
            buttonStates.remove(id)
        } else {
            // Update the state in the map
            buttonStates[id]?.value = state
        }
        // Log the state update for debugging
        Log.d("UpdateButtonState", "Updated state for button ID: $id, State: $state")
    }

    fun logAllButtonStates() {
        buttonStates.forEach { (id, state) ->
            Log.d("ButtonState", "Button ID: $id, State: ${state.value}")
        }
    }
}

fun saveButtonState(context: Context, state: List<ButtonState>) {
    val file = File("${Constants.USER_CONFIG}/UI.cfg")
    if (!file.exists()) {
        file.createNewFile()
    }

    val thumbsticks = state.filter { it.id in listOf(99, 98) }
    val existingStates = state.filter { it.id !in listOf(99, 98) }.toMutableList()
    // Add all elements of thumbsticks to existingStates
    existingStates.addAll(thumbsticks)

    file.printWriter().use { out ->
        existingStates.forEach { button ->
            out.println("ButtonID_${button.id}(${button.size};${button.offsetX};${button.offsetY};${button.isLocked};${button.keyCode};Color.${button.color};${button.alpha})")
        }
    }
}

fun loadButtonState(context: Context): List<ButtonState> {
    val file = File("${Constants.USER_CONFIG}/UI.cfg")
    if (!file.exists()) {
        println("File does not exist: ${file.absolutePath}")
        return emptyList()
    }

    val lines = file.readLines()
    println("File content: $lines")
    if (lines.isEmpty()) {
        println("File is empty")
        return emptyList()
    }

    return lines.mapNotNull { line ->
        val regex = """ButtonID_(\d+)\(([\d.]+);([\d.]+);([\d.]+);(true|false);(\d+);Color\.(\w+);([\d.]+)\)""".toRegex()
        val matchResult = regex.find(line)
        println("Processing line: $line")
        if (matchResult == null) {
            println("No match for line: $line")
            return@mapNotNull null
        }

        matchResult.let {
            val buttonState = ButtonState(
                id = it.groupValues[1].toInt(),
                size = it.groupValues[2].toFloat(),
                offsetX = it.groupValues[3].toFloat(),
                offsetY = it.groupValues[4].toFloat(),
                isLocked = it.groupValues[5].toBoolean(),
                keyCode = it.groupValues[6].toInt(),
                color = it.groupValues[7],  // Parse color as string
                alpha = it.groupValues[8].toFloat()  // Parse alpha
            )

            println("Loaded button state: $buttonState")

            // Update UIStateManager with the loaded button state
            UIStateManager.buttonStates[buttonState.id] = mutableStateOf(buttonState)
            buttonState
        }
    }
}

@Composable
fun KeySelectionMenu(onKeySelected: (Int) -> Unit, usedKeys: List<Int>, editMode: Boolean) {
    // Add A, S, D, and W to usedKeys
    val reservedKeys = listOf(
        KeyEvent.KEYCODE_A,
        KeyEvent.KEYCODE_S,
        KeyEvent.KEYCODE_D,
        KeyEvent.KEYCODE_W
    )
    val allUsedKeys = usedKeys + reservedKeys

    val letterKeys = ('A'..'Z').toList().filter { key ->
        val keyCode = KeyEvent.KEYCODE_A + key.minus('A')
        keyCode !in allUsedKeys
    }
    val fKeys = (KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12).filter { keyCode ->
        keyCode !in allUsedKeys
    }
    val additionalKeys = listOf(
        KeyEvent.KEYCODE_SHIFT_LEFT,
        KeyEvent.KEYCODE_SHIFT_RIGHT,
        KeyEvent.KEYCODE_CTRL_LEFT,
        KeyEvent.KEYCODE_CTRL_RIGHT,
        KeyEvent.KEYCODE_ALT_LEFT,
        KeyEvent.KEYCODE_ALT_RIGHT,
        KeyEvent.KEYCODE_SPACE,
        KeyEvent.KEYCODE_ESCAPE,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_GRAVE
    ).filter { keyCode -> keyCode !in allUsedKeys }

    var showDialog by remember { mutableStateOf(false) }
    IconButton(onClick = {
        showDialog = true
        UIStateManager.editMode = true
    }) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add Button",
            modifier = Modifier.size(36.dp), // Adjust the icon size here
            tint = Color.Red // Change the color here
        )
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .widthIn(min = 300.dp, max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Select a Key",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    letterKeys.chunked(6).forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { key ->
                                val keyCode = KeyEvent.KEYCODE_A + key.minus('A')
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.LightGray, shape = CircleShape)
                                        .clickable {
                                            onKeySelected(keyCode)
                                            showDialog = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color.White, thickness = 1.dp, modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = "Select a Function Key",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    fKeys.chunked(6).forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { keyCode ->
                                val key = "F${keyCode - KeyEvent.KEYCODE_F1 + 1}"
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.LightGray, shape = CircleShape)
                                        .clickable {
                                            onKeySelected(keyCode)
                                            showDialog = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color.White, thickness = 1.dp, modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = "Select a Unique Key, The shift and alt keys toggle.",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    additionalKeys.chunked(4).forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { keyCode ->
                                val key = when (keyCode) {
                                    KeyEvent.KEYCODE_SHIFT_LEFT -> "Shift-L"
                                    KeyEvent.KEYCODE_SHIFT_RIGHT -> "Shift-R"
                                    KeyEvent.KEYCODE_CTRL_LEFT -> "Ctrl-L"
                                    KeyEvent.KEYCODE_CTRL_RIGHT -> "Ctrl-R"
                                    KeyEvent.KEYCODE_ALT_LEFT -> "Alt-L"
                                    KeyEvent.KEYCODE_ALT_RIGHT -> "Alt-R"
                                    KeyEvent.KEYCODE_SPACE -> "Space"
                                    KeyEvent.KEYCODE_ESCAPE -> "Escape"
                                    KeyEvent.KEYCODE_ENTER -> "Enter"
                                    KeyEvent.KEYCODE_GRAVE -> "`"
                                    else -> keyCode.toString()
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.LightGray, shape = CircleShape)
                                        .clickable {
                                            onKeySelected(keyCode)
                                            showDialog = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showDialog = false
                            UIStateManager.editMode = true
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicButtonManager(
    context: Context,
    onNewButtonAdded: (ButtonState) -> Unit,
    editMode: Boolean,
    createdButtons: List<ButtonState>
) {
    var showDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.padding(start = 40.dp) // Add padding to space from the left
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                showDialog = !showDialog
                UIStateManager.editMode = showDialog
            }) {
                Icon(Icons.Default.Build, contentDescription = "Button Menu")
            }
        }
        if (showDialog) {
            KeySelectionMenu(
                onKeySelected = { keyCode ->
                    val allButtons = loadButtonState(context)
                    val thumbsticks = allButtons.filter { it.id in listOf(99, 98) }
                    val otherButtons = allButtons.filter { it.id !in listOf(99, 98) }
                    val maxExistingId = otherButtons.maxOfOrNull { it.id } ?: 0
                    val newId = maxExistingId + 1
                    val newButtonState = ButtonState(
                        id = newId,
                        size = 60f,
                        offsetX = 100f,
                        offsetY = 100f,
                        isLocked = false,
                        keyCode = keyCode,
                        color = "Black",
                        alpha = 0.25f
                    )
                    val updatedButtons = otherButtons + newButtonState
                    val finalUpdatedButtons = updatedButtons + thumbsticks
                    saveButtonState(context, finalUpdatedButtons)
                    onNewButtonAdded(newButtonState)
                    showDialog = false
                    UIStateManager.editMode = true
                },
                usedKeys = createdButtons.map { it.keyCode },
                editMode = UIStateManager.editMode
            )
        }
    }
}

// Helper function to convert string to Color
fun String.toColor(): Color = when (this) {
    "Black" -> Color.Black
    "Gray" -> Color.Gray
    "White" -> Color.White
    "Red" -> Color.Red
    "Green" -> Color.Green
    "Blue" -> Color.Blue
    "Yellow" -> Color.Yellow
    "Magenta" -> Color.Magenta
    "Cyan" -> Color.Cyan
    else -> Color.Gray
}

// Convert Color object to string representation
fun Color.toColorString(): String = when (this) {
    Color.Black -> "Black"
    Color.Gray -> "Gray"
    Color.White -> "White"
    Color.Red -> "Red"
    Color.Green -> "Green"
    Color.Blue -> "Blue"
    Color.Yellow -> "Yellow"
    Color.Magenta -> "Magenta"
    Color.Cyan -> "Cyan"
    else -> "Gray"
}
