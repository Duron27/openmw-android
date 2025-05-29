package org.openmw.utils

import android.util.Log
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.openmw.ui.controls.UIStateManager
import org.openmw.ui.controls.UIStateManager.menuAlpha
import org.openmw.ui.controls.UIStateManager.menuColor
import org.openmw.utils.GameFilesPreferences.ICON_GLOW_KEY

// Declare a lambda for updating console output and handling key events
lateinit var updateConsoleOutput: (String) -> Unit
lateinit var sendKeyEvent: (Int) -> Unit

@Composable
fun TravelMenuPopup(
    onKeyEvent: (Int) -> Unit,
    blurRadius: Dp = 6.dp,
    shadowColor: Color = Color.White.copy(alpha = 0.6f),
    scaleFactor:Float = 1.2f,
) {
    var popupVisible by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var selectedText by remember { mutableStateOf("Travel to destination ") }
    val matchIconColorChecked by GameFilesPreferences.loadMatchIconColorState(context).collectAsState(initial = false)
    val iconGlow by produceState(initialValue = false) {
        val preferences = context.dataStore.data.first()
        value = preferences[ICON_GLOW_KEY] ?: false
    }
    val options = listOf(
        "Balmora", "Suran", "Vivec", "Ald'ruhn", "Caldera", "Ebonheart", "Ghostgate", "Gnisis",
        "Maar Gan", "Molag Mar", "Pelagiad", "Tel Aruhn", "Tel Branora", "Tel Mora",
        "Ald Velothi", "Dagon Fel", "Gnaar Mok", "Hla Oad", "Khuul", "Seyda Neen",
        "Tel Fyr", "Tel Vos", "Vos"
    )

    Column(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(onClick = { popupVisible = true }) {
            if (iconGlow) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Open Travel Menu",
                    modifier = Modifier
                        .size(30.dp)
                        .scale(scaleFactor)
                        .blur(blurRadius),
                    tint = shadowColor
                )
            }
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Open Travel Menu",
                modifier = Modifier.size(24.dp),
                tint = if (matchIconColorChecked) {
                    menuColor.copy(alpha = menuAlpha)
                } else {
                    Color.Black
                }
            )
        }
        if (popupVisible) {
            Popup(
                onDismissRequest = {
                    popupVisible = false
                    sendKeyEvent(KeyEvent.KEYCODE_ESCAPE) },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.Black)
                        .padding(16.dp)
                        .width(300.dp)
                ) {
                    // Dropdown Menu Button
                    Box(modifier = Modifier.clickable { dropdownExpanded = true }) {
                        Row(modifier = Modifier.padding(8.dp).background(Color.Black)) {
                            Text(
                                text = selectedText,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start
                            )
                            Icon(
                                imageVector = if (dropdownExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }

                    if (dropdownExpanded) {
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(text = option, color = Color.White) },
                                    modifier = Modifier.padding(8.dp).background(Color.Black),
                                    onClick = {
                                        selectedText = option
                                        dropdownExpanded = false
                                        val command = when (option) {
                                            "Balmora" -> "coc balmora"
                                            "Suran" -> "coc suran"
                                            "Vivec" -> "coc vivec"
                                            "Ald'ruhn" -> "coc aldrhuun"
                                            "Caldera" -> "coc caldera"
                                            "Ebonheart" -> "coc ebonheart"
                                            "Ghostgate" -> "coc ghostgate"
                                            "Gnisis" -> "coc gnisis"
                                            "Maar Gan" -> "coc maar_gan"
                                            "Molag Mar" -> "coc molag_mar"
                                            "Pelagiad" -> "coc pelagiad"
                                            "Tel Aruhn" -> "coc tel_aruhn"
                                            "Tel Branora" -> "coc tel_branora"
                                            "Tel Mora" -> "coc tel_mora"
                                            "Ald Velothi" -> "coc ald_velothi"
                                            "Dagon Fel" -> "coc dagon_fel"
                                            "Gnaar Mok" -> "coc gnaar_mok"
                                            "Hla Oad" -> "coc hla_oad"
                                            "Khuul" -> "coc khuul"
                                            "Seyda Neen" -> "coc seyda_neen"
                                            "Tel Fyr" -> "coc tel_fyr"
                                            "Tel Vos" -> "coc tel_vos"
                                            "Vos" -> "coc vos"
                                            else -> ""
                                        }
                                        Log.d("Overlay", "Run Commands with $command")
                                        automateCommands(command)
                                    }
                                )
                            }
                        }
                    }

                    // Additional Buttons (Placeholders)
                    Spacer(modifier = Modifier.height(1.dp))
                    Button(onClick = { automateCommands("tcl") }) {
                        Text("Enable / Disable Clipping")
                    }
                    Button(onClick = { automateCommands("tgm") }) {
                        Text("Enable / Disable Godmode")
                    }
                    /*
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { automateCommands("player->setspeed 400") }) {
                        Text("Speed Increase")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { automateCommands("player->setspeed 100") }) {
                        Text("Default Speed")
                    }

                     */
                }
            }
        }
    }
}

fun runCommand(command: String, delayBetweenKeys: Long = 10L) {
    Log.d("CommandUtils", "Executing command: $command")
    command.forEach { char ->
        sendKeyEvent(char.toKeyCode())
        // Add a delay between each key press
        Thread.sleep(delayBetweenKeys)
    }
    updateConsoleOutput("$command\n")
}

fun automateCommands(command: String) {
    CoroutineScope(Dispatchers.Main).launch {
        // Send the grave key first
        sendKeyEvent(KeyEvent.KEYCODE_GRAVE)
        delay(100) // Delay for 100 ms

        // Send each character in the provided command
        runCommand(command, 10L)
        delay(100) // Delay for 100 ms

        // Send the enter key
        sendKeyEvent(KeyEvent.KEYCODE_ENTER)
        //delay(1000) // Delay for 1 second

        // Send the escape key
        sendKeyEvent(KeyEvent.KEYCODE_ESCAPE)
    }
}

private fun Char.toKeyCode(): Int {
    return when (this) {
        'a' -> KeyEvent.KEYCODE_A
        'b' -> KeyEvent.KEYCODE_B
        'c' -> KeyEvent.KEYCODE_C
        'd' -> KeyEvent.KEYCODE_D
        'e' -> KeyEvent.KEYCODE_E
        'f' -> KeyEvent.KEYCODE_F
        'g' -> KeyEvent.KEYCODE_G
        'h' -> KeyEvent.KEYCODE_H
        'i' -> KeyEvent.KEYCODE_I
        'j' -> KeyEvent.KEYCODE_J
        'k' -> KeyEvent.KEYCODE_K
        'l' -> KeyEvent.KEYCODE_L
        'm' -> KeyEvent.KEYCODE_M
        'n' -> KeyEvent.KEYCODE_N
        'o' -> KeyEvent.KEYCODE_O
        'p' -> KeyEvent.KEYCODE_P
        'q' -> KeyEvent.KEYCODE_Q
        'r' -> KeyEvent.KEYCODE_R
        's' -> KeyEvent.KEYCODE_S
        't' -> KeyEvent.KEYCODE_T
        'u' -> KeyEvent.KEYCODE_U
        'v' -> KeyEvent.KEYCODE_V
        'w' -> KeyEvent.KEYCODE_W
        'x' -> KeyEvent.KEYCODE_X
        'y' -> KeyEvent.KEYCODE_Y
        'z' -> KeyEvent.KEYCODE_Z
        '0' -> KeyEvent.KEYCODE_0
        '1' -> KeyEvent.KEYCODE_1
        '2' -> KeyEvent.KEYCODE_2
        '3' -> KeyEvent.KEYCODE_3
        '4' -> KeyEvent.KEYCODE_4
        '5' -> KeyEvent.KEYCODE_5
        '6' -> KeyEvent.KEYCODE_6
        '7' -> KeyEvent.KEYCODE_7
        '8' -> KeyEvent.KEYCODE_8
        '9' -> KeyEvent.KEYCODE_9
        ' ' -> KeyEvent.KEYCODE_SPACE
        '-' -> KeyEvent.KEYCODE_MINUS
        '=' -> KeyEvent.KEYCODE_EQUALS
        '[' -> KeyEvent.KEYCODE_LEFT_BRACKET
        ']' -> KeyEvent.KEYCODE_RIGHT_BRACKET
        '\\' -> KeyEvent.KEYCODE_BACKSLASH
        ';' -> KeyEvent.KEYCODE_SEMICOLON
        '\'' -> KeyEvent.KEYCODE_APOSTROPHE
        '/' -> KeyEvent.KEYCODE_SLASH
        ',' -> KeyEvent.KEYCODE_COMMA
        '.' -> KeyEvent.KEYCODE_PERIOD
        '<' -> KeyEvent.KEYCODE_SHIFT_LEFT
        '>' -> KeyEvent.KEYCODE_SHIFT_RIGHT
        '@' -> KeyEvent.KEYCODE_AT
        '#' -> KeyEvent.KEYCODE_POUND
        '+' -> KeyEvent.KEYCODE_PLUS
        else -> KeyEvent.KEYCODE_UNKNOWN
    }
}

