package org.openmw.utils

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.openmw.Constants
import java.io.File

val controlMappings = listOf(
    7 to "Forward",
    8 to "Back",
    5 to "Left",
    6 to "Right",
    30 to "Toggle POV",
    48 to "Zoom Camera In",
    49 to "Zoom Camera Out",
    17 to "Run",
    23 to "Always Run",
    24 to "Sneak",
    9 to "Use",
    28 to "Ready Weapon",
    29 to "Ready Magic",
    18 to "Previous Spell",
    19 to "Next Spell",
    20 to "Previous Weapon",
    21 to "Next Weapon",
    12 to "Auto Run",
    11 to "Jump",
    3 to "Inventory",
    14 to "Journal",
    13 to "Rest",
    4 to "Console",
    25 to "Quick Save",
    26 to "Quick Load",
    42 to "Toggle HUD",
    2 to "Screenshot",
    41 to "Quick Menu",
    31 to "Quick 1",
    32 to "Quick 2",
    33 to "Quick 3",
    34 to "Quick 4",
    35 to "Quick 5",
    36 to "Quick 6",
    37 to "Quick 7",
    38 to "Quick 8",
    39 to "Quick 9",
    40 to "Quick 10",
    50 to "Toggle Post Processor HUD"
)

val keyCodeToNameMap = mapOf(
    4 to "A", 5 to "B", 6 to "C", 7 to "D", 8 to "E", 9 to "F", 10 to "G", 11 to "H", 12 to "I", 13 to "J",
    14 to "K", 15 to "L", 16 to "M", 17 to "N", 18 to "O", 19 to "P", 20 to "Q", 21 to "R", 22 to "S", 23 to "T",
    24 to "U", 25 to "V", 26 to "W", 27 to "X", 28 to "Y", 29 to "Z",
    30 to "1", 31 to "2", 32 to "3", 33 to "4", 34 to "5", 35 to "6", 36 to "7", 37 to "8", 38 to "9", 39 to "0",
    40 to "ENTER", 41 to "ESCAPE", 42 to "BACKSPACE", 43 to "TAB", 44 to "SPACE",
    45 to "MINUS", 46 to "EQUALS", 47 to "LEFTBRACKET", 48 to "RIGHTBRACKET", 49 to "BACKSLASH",
    50 to "NONUSHASH", 51 to "SEMICOLON", 52 to "APOSTROPHE", 53 to "GRAVE", 54 to "COMMA", 55 to "PERIOD",
    56 to "SLASH", 57 to "CAPSLOCK",
    58 to "F1", 59 to "F2", 60 to "F3", 61 to "F4", 62 to "F5", 63 to "F6", 64 to "F7", 65 to "F8", 66 to "F9",
    67 to "F10", 68 to "F11", 69 to "F12",
    70 to "PRINTSCREEN", 71 to "SCROLLLOCK", 72 to "PAUSE", 73 to "INSERT", 74 to "HOME", 75 to "PAGEUP",
    76 to "DELETE", 77 to "END", 78 to "PAGEDOWN", 79 to "RIGHT", 80 to "LEFT", 81 to "DOWN", 82 to "UP",
    83 to "NUMLOCKCLEAR", 84 to "KP_DIVIDE", 85 to "KP_MULTIPLY", 86 to "KP_MINUS", 87 to "KP_PLUS",
    88 to "KP_ENTER", 89 to "KP_1", 90 to "KP_2", 91 to "KP_3", 92 to "KP_4", 93 to "KP_5", 94 to "KP_6",
    95 to "KP_7", 96 to "KP_8", 97 to "KP_9", 98 to "KP_0", 99 to "KP_PERIOD",
    100 to "NONUSBACKSLASH", 101 to "APPLICATION", 102 to "POWER", 103 to "KP_EQUALS", 104 to "F13",
    105 to "F14", 106 to "F15", 107 to "F16", 108 to "F17", 109 to "F18", 110 to "F19", 111 to "F20",
    112 to "F21", 113 to "F22", 114 to "F23", 115 to "F24", 116 to "EXECUTE",
    224 to "LEFT CTRL", 225 to "LEFT SHIFT", 226 to "LEFT ALT", 227 to "LIGHT GUI", 228 to "RIGHT CTRL", 229 to "RIGHT SHIFT",
    230 to "RIGHT ALT", 231 to "RIGHT GUI"
)

data class Control(val name: String, val keyCode: Int?, val keyName: String)

@Composable
fun KeyBindings(context: Context) {
    var controls by remember { mutableStateOf<List<Control>>(emptyList()) }
    var selectedControl by remember { mutableStateOf<Control?>(null) }
    var showKeySelection by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val xmlFile = File("${Constants.USER_CONFIG}/input_v3.xml")
        val xmlContent = xmlFile.readText()
        controls = parseControls(xmlContent)
    }

    // Create a map for quick lookups
    val controlMappingsMap = controlMappings.toMap()

    // Sort controls by controlMappings order
    val sortedControls = remember(controls) {
        controls.filter { it.name != "Unknown" }
            .sortedBy { control ->
                controlMappingsMap.entries.indexOfFirst { it.value == control.name }
            }
        controls.sortedBy { control ->
            controlMappingsMap.entries.indexOfFirst { it.value == control.name }
        }
    }

    val scrollState = rememberScrollState()

    Column(modifier = Modifier.verticalScroll(scrollState).padding(16.dp)) {
        sortedControls.forEach { control ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(control.name, modifier = Modifier.weight(1f).align(Alignment.CenterVertically))
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    selectedControl = control
                    showKeySelection = true
                }) {
                    Text(control.keyName)
                }
            }
        }

        if (showKeySelection && selectedControl != null) {
            DropdownMenu(
                expanded = showKeySelection,
                onDismissRequest = { showKeySelection = false }
            ) {
                keyCodeToNameMap.forEach { (keyCode, keyName) ->
                    DropdownMenuItem(
                        text = { Text(keyName, color = Color.White) },
                        onClick = {
                            selectedControl?.keyCode?.let { currentKeyCode ->
                                updateXmlWithSelectedKey(context, currentKeyCode, keyCode)
                            }
                            selectedControl = null
                            showKeySelection = false
                            // Reload controls after updating XML
                            val xmlFile = File("${Constants.USER_CONFIG}/input_v3.xml")
                            val xmlContent = xmlFile.readText()
                            controls = parseControls(xmlContent)
                        }
                    )
                }
            }
        }
    }
}

fun updateXmlWithSelectedKey(context: Context, currentKeyCode: Int, selectedKey: Int) {
    val xmlFile = File("${Constants.USER_CONFIG}/input_v3.xml")
    val xmlContent = xmlFile.readText()
    val newXmlContent = xmlContent.replace("""key="$currentKeyCode"""", """key="$selectedKey"""")

    xmlFile.writeText(newXmlContent)
    println("XML after update: \n$newXmlContent") // Debugging
}

fun parseControls(xmlContent: String): List<Control> {
    val controls = mutableListOf<Control>()
    val regex = """<Control name="(\d+)"[^>]*>\s*<KeyBinder key="(\d+)""".toRegex()

    regex.findAll(xmlContent).forEach { matchResult ->
        val nameIndex = matchResult.groupValues[1].toInt()
        val keyCode = matchResult.groupValues[2].toInt()
        val name = controlMappings.find { it.first == nameIndex }?.second ?: "Unknown"
        val keyName = keyCodeToNameMap[keyCode] ?: "Not Set"

        controls.add(Control(name, keyCode, keyName))
    }

    // Create a map for quick lookups
    val controlMappingsMap = controlMappings.toMap()

    // Filter out controls with the name "Unknown" and sort the remaining controls by controlMappings order
    return controls
        .filter { it.name != "Unknown" }
        .sortedBy { control ->
            controlMappingsMap.entries.indexOfFirst { it.value == control.name }
        }

}
