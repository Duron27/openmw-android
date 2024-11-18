package org.openmw.utils

import android.annotation.SuppressLint
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openmw.Constants
import java.io.File

private fun readIniValues(): Map<String, List<Triple<String, Any, String?>>> {
    val settings = mutableMapOf<String, MutableList<Triple<String, Any, String?>>>()
    val comments = mutableMapOf<String, String?>()
    val sections = mutableMapOf<String, MutableMap<String, String>>()
    var currentSection: String? = null
    var lastKey: String? = null

    // Define your blacklist here
    val blacklist = setOf("key1", "key2", "key3")

    File(Constants.SETTINGS_FILE).forEachLine { line ->
        val trimmedLine = line.trim()
        when {
            trimmedLine.startsWith("[") && trimmedLine.endsWith("]") -> {
                currentSection = trimmedLine.substring(1, trimmedLine.length - 1).trim()
                sections[currentSection!!] = mutableMapOf()
            }
            trimmedLine.startsWith("#") -> {
                val comment = trimmedLine.substring(1).trim()
                if (lastKey != null && lastKey !in blacklist) {
                    comments[lastKey!!] = comment
                }
            }
            "=" in trimmedLine -> {
                val (key, value) = trimmedLine.split("=", limit = 2).map { it.trim() }
                if (currentSection != null && key !in blacklist) {
                    sections[currentSection]!![key] = value
                    lastKey = key
                } else {
                    lastKey = null // Reset lastKey if the key is blacklisted
                }
            }
        }
    }
    sections.forEach { (section, properties) ->
        val sectionSettings = properties.mapNotNull { (key, value) ->
            if (key in blacklist) return@mapNotNull null
            val parsedValue: Any = when {
                value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true) -> value.toBoolean()
                value.toIntOrNull() != null -> value.toInt()
                value.toFloatOrNull() != null -> value.toFloat()
                else -> value
            }
            Triple(key, parsedValue, comments[key])
        }
        settings[section] = sectionSettings.toMutableList()
    }
    return settings
}


fun writeIniValue(section: String, key: String, value: Any, comment: String?) {
    val settingsFile = File(Constants.SETTINGS_FILE)
    val lines = settingsFile.readLines().toMutableList()
    var sectionFound = false
    var keyFound = false
    var sectionEndIndex = lines.size

    for (i in lines.indices) {
        val line = lines[i].trim()
        if (line.startsWith("[") && line.endsWith("]")) {
            if (sectionFound) {
                sectionEndIndex = i
                break
            }
            if (line.substring(1, line.length - 1).trim() == section) {
                sectionFound = true
            }
        } else if (sectionFound && line.startsWith(key)) {
            lines[i] = "${key.trim()} = ${value.toString().trim()}"
            keyFound = true
            if (comment != null) {
                val commentIndex = i + 1
                if (commentIndex < lines.size && lines[commentIndex].trim() != comment.trim()) {
                    lines.add(commentIndex, comment.trim())
                    lines.add(commentIndex + 1, "") // Add an empty line after the comment
                }
            }
            break
        }
    }

    if (!sectionFound) {
        lines.add("[${section.trim()}]")
        lines.add("${key.trim()} = ${value.toString().trim()}")
        if (comment != null) {
            lines.add(comment.trim())
            lines.add("") // Add an empty line after the comment
        }
    } else if (!keyFound) {
        lines.add(sectionEndIndex, "${key.trim()} = ${value.toString().trim()}")
        if (comment != null) {
            lines.add(sectionEndIndex + 1, comment.trim())
            lines.add(sectionEndIndex + 2, "") // Add an empty line after the comment
        }
    }
    settingsFile.writeText(lines.joinToString("\n"))
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun ReadAndDisplayIniValues() {
    val settings = remember { mutableStateOf(readIniValues()) }
    val transparentBlack = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
    val darkGray = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
    val lightGray = Color(alpha = 0.4f, red = 0f, green = 0f, blue = 0f)
    var isColumnExpanded by remember { mutableStateOf(false) }
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black)
            .background(color = transparentBlack)
            .clickable { isColumnExpanded = !isColumnExpanded }, // Toggle column expansion
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White
            )
            Icon(
                imageVector = if (isColumnExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isColumnExpanded) "Collapse" else "Expand"
            )
        }
        if (isColumnExpanded) {
            LazyColumn {
                items(settings.value.entries.toList()) { (section, sectionSettings) ->
                    var isExpanded by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .background(transparentBlack)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.Black)
                                .clickable { isExpanded = !isExpanded }, // Toggle expansion
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = section,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White,
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 2.dp
                                )
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 10.dp
                                )
                            )
                        }
                        if (isExpanded) {
                            sectionSettings.forEachIndexed { index, (propertyKey, value, comment) ->
                                val extractedPropertyKey =
                                    propertyKey.substringAfterLast('.')
                                val backgroundColor = if (index % 2 == 0) darkGray else lightGray
                                settings.value = readIniValues()

                                when (value) {
                                    is Boolean -> {
                                        var switchState by remember { mutableStateOf(value) }
                                        Column(
                                            modifier = Modifier
                                                .background(color = backgroundColor)
                                                .fillMaxWidth()
                                                .border(2.dp, Color.Black)
                                                .padding(bottom = 16.dp) // Add space between each iteration
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.Black)
                                                    .horizontalScroll(rememberScrollState()),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = extractedPropertyKey,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (value.toString() == "false") Color.Red else Color.Green,
                                                    fontSize = 16.sp,
                                                    modifier = Modifier.padding(start = 10.dp, top = 4.dp)
                                                )
                                                Spacer(modifier = Modifier.weight(1f))
                                                Switch(
                                                    checked = switchState,
                                                    onCheckedChange = {
                                                        switchState = it

                                                        writeIniValue(section, extractedPropertyKey, it, null)

                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                            view.performHapticFeedback(
                                                                HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
                                                        }

                                                        settings.value = readIniValues() // Reload settings
                                                    },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                                        uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                                                        uncheckedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    )
                                                )
                                            }
                                            if (comment != null) {
                                                Text(
                                                    text = comment,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    modifier = Modifier.padding(start = 10.dp, top = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    is Float -> {
                                        var floatValue by remember { mutableStateOf(value.toString()) }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.Black)
                                                .horizontalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = extractedPropertyKey,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Green,
                                                fontSize = 16.sp,
                                                modifier = Modifier.padding(
                                                    start = 10.dp,
                                                    top = 4.dp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            TextField(
                                                value = floatValue,
                                                onValueChange = {
                                                    floatValue = it
                                                    val floatVal =
                                                        it.toFloatOrNull() ?: 0.0f
                                                    writeIniValue(
                                                        section,
                                                        extractedPropertyKey,
                                                        floatVal,
                                                        null
                                                    )
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                        view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
                                                    }

                                                    settings.value = readIniValues() // Reload settings

                                                },
                                                label = { Text("Enter value") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            if (comment != null) {
                                                Text(
                                                    text = comment,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    modifier = Modifier.padding(start = 10.dp, top = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    is Int -> {
                                        var intValue by remember { mutableStateOf(value.toString()) }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.Black)
                                                .horizontalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = extractedPropertyKey,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Green,
                                                fontSize = 16.sp,
                                                modifier = Modifier.padding(
                                                    start = 10.dp,
                                                    top = 4.dp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            TextField(
                                                value = intValue,
                                                onValueChange = {
                                                    intValue = it
                                                    val intVal = it.toIntOrNull() ?: 0
                                                    writeIniValue(
                                                        section,
                                                        extractedPropertyKey,
                                                        intVal,
                                                        null
                                                    )

                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                        view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
                                                    }
                                                    settings.value = readIniValues() // Reload settings
                                                },
                                                label = { Text("Enter value") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            if (comment != null) {
                                                Text(
                                                    text = comment,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    modifier = Modifier.padding(start = 10.dp, top = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    else -> {
                                        Text(
                                            text = "$extractedPropertyKey = $value",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(
                                                start = 10.dp,
                                                top = 4.dp
                                            )
                                        )
                                        if (comment != null) {
                                            Text(
                                                text = comment,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                modifier = Modifier.padding(
                                                    start = 24.dp,
                                                    top = 4.dp
                                                )
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

@Composable
fun ExpandableBox(expanded: MutableState<Boolean>) {
    val settings = remember { mutableStateOf(readIniValues()) }
    val transparentBlack = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
    val darkGray = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
    val lightGray = Color(alpha = 0.4f, red = 0f, green = 0f, blue = 0f)
    val view = LocalView.current

    Box(
        modifier = Modifier
            .animateContentSize()
            .border(
                BorderStroke(width = 3.dp, color = Color.Black)
            )
            .fillMaxWidth()
            .background(color = transparentBlack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                expanded.value = !expanded.value
            }
    ) {
        if (expanded.value) {
            settings.value = readIniValues()
            LazyColumn {
                items(settings.value.entries.toList()) { (section, sectionSettings) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$section Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }
                    sectionSettings.forEachIndexed { index, (propertyKey, value, comment) ->
                        val extractedPropertyKey = propertyKey.substringAfterLast('.')
                        val backgroundColor = if (index % 2 == 0) darkGray else lightGray

                        Column(
                            modifier = Modifier
                                .background(color = backgroundColor)
                                .fillMaxWidth()
                                .border(2.dp, Color.Black)
                                .padding(bottom = 16.dp) // Add space between each iteration
                        ) {
                            Text(
                                text = "$extractedPropertyKey = $value",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (value.toString() == "false") Color.Red else Color.Green,
                                modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                thickness = 1.dp,
                                color = Color.Black
                            )

                            if (comment != null) {
                                Text(
                                    text = comment,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                                )
                            }
                        }
                    }
                }
                // New section and property fields
                item {
                    var displayText by remember { mutableStateOf("Choose Section to add Setting") }
                    var newExtractedPropertyKey by remember { mutableStateOf("") }
                    var newValue by remember { mutableStateOf("") }
                    var newComment by remember { mutableStateOf("") }
                    var expandedDropdown by remember { mutableStateOf(false) }
                    var selectedSection by remember { mutableStateOf(settings.value.keys.firstOrNull() ?: "") }

                    Column {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    BorderStroke(width = 1.dp, color = Color.White)
                                ),
                        ) {
                            Text(
                                text = displayText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedDropdown = true }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            settings.value.keys.forEach { section ->
                                DropdownMenuItem(
                                    text = { Text(section) },
                                    onClick = {
                                        selectedSection = section
                                        displayText = section
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = newExtractedPropertyKey,
                            onValueChange = { newExtractedPropertyKey = it },
                            label = { Text("New Setting Name") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White)
                        )
                        OutlinedTextField(
                            value = newValue,
                            onValueChange = { newValue = it },
                            label = { Text("New Value") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White)
                        )
                        OutlinedTextField(
                            value = newComment,
                            onValueChange = { newComment = it },
                            label = { Text("New Setting Description") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White)
                        )
                        Button(onClick = {
                            // Add new property to the selected section
                            if (newExtractedPropertyKey.isNotBlank() && newValue.isNotBlank() && selectedSection.isNotBlank()) {
                                val newBooleanValue = newValue.toBoolean()
                                val formattedComment = if (newComment.startsWith("#")) newComment else "# $newComment"

                                val updatedSettings = settings.value.toMutableMap().apply {
                                    val sectionSettings = this[selectedSection]?.toMutableList() ?: mutableListOf()
                                    sectionSettings.add(Triple(newExtractedPropertyKey, newBooleanValue, formattedComment))
                                    this[selectedSection] = sectionSettings
                                }
                                settings.value = updatedSettings
                                writeIniValue(selectedSection, newExtractedPropertyKey, newBooleanValue, formattedComment)

                                // Clear the text fields
                                newExtractedPropertyKey = ""
                                newValue = ""
                                newComment = ""
                            }
                        }) {
                            Text("Add")

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
                            }

                            settings.value = readIniValues()
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Manually Modify Settings.cfg",
                modifier = Modifier
                    .padding(16.dp)  // Maintain padding for visual appeal
                    .fillMaxWidth()   // Ensures horizontal centering
                    .wrapContentSize(Alignment.Center),  // Centers text horizontally
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp
            )
        }
    }
}
