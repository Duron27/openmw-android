package org.openmw.ui.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.libsdl.app.SDLControllerManager
import org.openmw.utils.addCustomLog
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

@Composable
fun GamepadEmulatorScreen() {
    var registered by remember { mutableStateOf(false) }
    val deviceId = 1384510555
    var stickId = 0

    fun updateStick(stickId: Int, x: Float, y: Float) {
        if (!registered) {
            registered = true
            SDLControllerManager.nativeAddJoystick(deviceId, "Virtual", "Virtual",
                0xbad, 0xf00d,
                false, -0x1,
                4, 0, 0, 0)
            addCustomLog("Joystick registered: $deviceId", textSize = 10, textColor = Color.Blue)
        }

        SDLControllerManager.onNativeJoy(deviceId, stickId * 2, x)
        SDLControllerManager.onNativeJoy(deviceId, stickId * 2 + 1, y)
        addCustomLog("Stick $stickId updated: x=$x, y=$y", textSize = 10, textColor = Color.Yellow)
    }

    Joystick(
        stickId = stickId,
        onUpdateStick = ::updateStick
    )
}

@Composable
fun Joystick(
    stickId: Int,
    onUpdateStick: (Int, Float, Float) -> Unit,
) {
    var initialX by remember { mutableFloatStateOf(0f) }
    var initialY by remember { mutableFloatStateOf(0f) }
    var currentX by remember { mutableFloatStateOf(-1f) }
    var currentY by remember { mutableFloatStateOf(-1f) }
    var down by remember { mutableStateOf(false) }

    Canvas(
        modifier = Modifier
            .size(200.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        initialX = it.x
                        initialY = it.y
                        currentX = initialX
                        currentY = initialY
                        down = true
                    },
                    onDrag = { change, _ ->
                        currentX = change.position.x
                        currentY = change.position.y
                    },
                    onDragEnd = {
                        down = false
                        currentX = -1f
                        currentY = -1f
                    }
                )
            }
    ) {
        val strokeWidth2 = with(density) { 2.dp.toPx() }
        val paint = Paint().apply {
            style = PaintingStyle.Stroke
            strokeWidth = strokeWidth2
        }

        if (down) {
            // Draw initial touch
            drawCircle(
                color = Color.Gray,
                radius = size.minDimension / 10f,
                center = Offset(initialX, initialY),
                style = Stroke(width = paint.strokeWidth)
            )

            // Draw current stick position
            drawCircle(
                color = Color.Gray,
                radius = size.minDimension / 5f,
                center = Offset(currentX, currentY),
                style = Stroke(width = paint.strokeWidth)
            )
        } else {
            // Draw the outline
            drawCircle(
                color = Color.Gray,
                radius = size.minDimension / 2f - paint.strokeWidth,
                center = center,
                style = Stroke(width = paint.strokeWidth)
            )
        }

        if (down) {
            val w = size.minDimension / 3f
            var diffX = currentX - initialX
            var diffY = currentY - initialY

            val bias = 0.3f

            if (abs(diffX) > abs(diffY)) {
                diffY = sign(diffY) * max(0f, abs(diffY) - bias * abs(diffX))
            } else {
                diffX = sign(diffX) * max(0f, abs(diffX) - bias * abs(diffY))
            }

            val dx = diffX / w + 0.2f * sign(diffX)
            val dy = diffY / w + 0.2f * sign(diffY)
            onUpdateStick(stickId, dx.coerceIn(-1f, 1f), dy.coerceIn(-1f, 1f))
        } else {
            onUpdateStick(stickId, 0f, 0f)
        }
    }
}
