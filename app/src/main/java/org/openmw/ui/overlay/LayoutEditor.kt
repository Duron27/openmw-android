package org.openmw.ui.overlay

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.openmw.R
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStream
import kotlin.math.roundToInt

data class Widget(
    val type: String,
    val position: MutableState<String>,
    val name: String,
    val properties: Map<String, String>
)

data class WidgetInfo(
    val paddingRight: Float = 0f,
    val paddingBottom: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
)

class WidgetViewModel : ViewModel() {
    private val _widgetInfo = MutableStateFlow(WidgetInfo())
    val widgetInfo: StateFlow<WidgetInfo> get() = _widgetInfo

    fun updateWidgetInfo(paddingRight: Float, paddingBottom: Float, width: Float, height: Float) {
        viewModelScope.launch {
            _widgetInfo.value = WidgetInfo(paddingRight, paddingBottom, width, height)
        }
    }
}

@Composable
fun DraggableBox(
    initialPosition: Offset,
    initialSize: Size,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(initialPosition.x) }
    var offsetY by remember { mutableFloatStateOf(initialPosition.y) }
    var width by remember { mutableFloatStateOf(initialSize.width) }
    var height by remember { mutableFloatStateOf(initialSize.height) }
    var isDragging by remember { mutableStateOf(false) }
    var isResizing by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(width.dp, height.dp)
            .background(Color.Transparent)
            .border(2.dp, if (isDragging || isResizing) Color.Red else Color.Gray)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDrag = { _, dragAmount ->
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    },
                    onDragEnd = { isDragging = false }
                )
            }
    ) {
        content()
        Image(
            painter = painterResource(id = R.drawable.aspect_ratio_24dp_e8eaed_fill0_wght400_grad0_opsz24),
            contentDescription = "Resize handle",
            colorFilter = ColorFilter.tint(Color.Red),
            modifier = Modifier
                .size(32.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isResizing = true },
                        onDrag = { _, dragAmount ->
                            width += dragAmount.x.toDp().value
                            height += dragAmount.y.toDp().value
                        },
                        onDragEnd = { isResizing = false }
                    )
                }
                .align(Alignment.BottomEnd)
        )
    }
}


fun getLayoutFiles(folderPath: String): List<File> {
    val folder = File(folderPath)
    return folder.listFiles { _, name -> name.endsWith(".layout") }?.toList() ?: emptyList()
}

@Composable
fun LayoutFileDropdown(
    files: List<File>,
    onFileSelected: (File) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<File?>(null) }

    Box {
        Button(onClick = { expanded = true }) {
            Text(text = selectedFile?.name ?: "Select Layout File")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            files.forEach { file ->
                DropdownMenuItem(
                    text = { Text(file.name, color = Color.Black) },
                    onClick = {
                    selectedFile = file
                    onFileSelected(file)
                    expanded = false
                })
            }
        }
    }
}

fun parseLayout(inputStream: InputStream): List<Widget> {
    val widgets = mutableListOf<Widget>()
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()
    parser.setInput(inputStream, null)

    try {
        var eventType = parser.eventType
        var currentWidget: Widget? = null
        val widgetStack = mutableListOf<Widget>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "Widget") {
                        val name = parser.getAttributeValue(null, "name") ?: "Unnamed"
                        if (name == "_Main") {
                            parser.nextTag() // Skip _Main widget and its children
                            continue
                        }

                        val type = parser.getAttributeValue(null, "type") ?: "Unknown"
                        val position = mutableStateOf(parser.getAttributeValue(null, "position") ?: "0 0 100 100")
                        val properties = mutableMapOf<String, String>()

                        for (i in 0 until parser.attributeCount) {
                            properties[parser.getAttributeName(i)] = parser.getAttributeValue(i)
                        }

                        currentWidget = Widget(type, position, name, properties)

                        if (widgetStack.isNotEmpty()) {
                            widgetStack.add(currentWidget)
                        } else {
                            widgets.add(currentWidget)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "Widget" && widgetStack.isNotEmpty()) {
                        currentWidget = widgetStack.removeAt(widgetStack.size - 1)
                    }
                }
            }
            eventType = parser.next()
        }
    } catch (e: Exception) {
        Log.e("ParseLayout", "Error parsing layout: ${e.message}")
    }

    return widgets.filter { it.name != "_Main" }
}


@Composable
fun DisplayWidgetsFromLayout(widgets: List<Widget>, viewModel: WidgetViewModel = viewModel()) {
    val widgetInfo by viewModel.widgetInfo.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        for (widget in widgets) {
            if (widget.name == "MiniMap") {
                val (paddingRight, paddingBottom, width, height) = widget.position.value.split(" ").map { it.toFloat() }

                // Update ViewModel with widget info
                viewModel.updateWidgetInfo(paddingRight, paddingBottom, width, height)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingBottom.dp, end = paddingRight.dp)
                        .wrapContentSize(align = Alignment.BottomEnd)
                        .size(width.dp, height.dp)
                ) {
                    DraggableBox(
                        initialPosition = Offset(0f, 0f), // Reset the initial position for this example
                        initialSize = Size(width, height)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(text = widget.name, color = Color.White) // Text color set to white
                        }
                    }
                }
            }
        }

        // Display additional information anywhere on the screen
        Box(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart) // Display information at the top left
        ) {
            Column {
                Text(text = "Widget Info:", color = Color.White)
                Text(text = "Padding Right: ${widgetInfo.paddingRight}", color = Color.White)
                Text(text = "Padding Bottom: ${widgetInfo.paddingBottom}", color = Color.White)
                Text(text = "Width: ${widgetInfo.width}", color = Color.White)
                Text(text = "Height: ${widgetInfo.height}", color = Color.White)
            }
        }
    }
}


