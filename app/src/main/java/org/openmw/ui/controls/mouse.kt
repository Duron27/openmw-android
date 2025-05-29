@file:OptIn(InternalCoroutinesApi::class)

package org.openmw.ui.controls

import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import org.libsdl.app.SDLActivity
import org.openmw.Constants
import org.openmw.EngineActivity
import org.openmw.utils.GameFilesPreferences.getOffsetXMouse
import org.openmw.utils.GameFilesPreferences.getOffsetYMouse
import org.openmw.utils.currentDeviceRealSize
import kotlin.math.roundToInt

@Composable
fun MouseIcon() {
    val context = LocalContext.current
    var iconOffset by remember { mutableStateOf(IntOffset.Zero) }
    val offsetXMouse by getOffsetXMouse(context).collectAsState(initial = 0f)
    val offsetYMouse by getOffsetYMouse(context).collectAsState(initial = 0f)
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val (screenWidth, screenHeight) = windowManager.currentDeviceRealSize()
    val sdlWidth = EngineActivity.resolutionX.toFloat()
    val sdlHeight = EngineActivity.resolutionY.toFloat()

    LaunchedEffect(Unit) {
        while (true) {
            val x = (SDLActivity.getMouseX().toFloat() + (offsetXMouse ?: 0f)) * (screenWidth / sdlWidth)
            val y = (SDLActivity.getMouseY().toFloat() + (offsetYMouse ?: 0f)) * (screenHeight / sdlHeight)
            iconOffset = IntOffset(x.roundToInt(), y.roundToInt())
            delay(16L)
        }
    }

    Box {
        Image(
            painter = rememberAsyncImagePainter(model = "file:${Constants.USER_UI}/pointer_arrow.png"),
            contentDescription = "Pointer Icon",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .offset { iconOffset }
                .size(32.dp)
        )
    }
}
