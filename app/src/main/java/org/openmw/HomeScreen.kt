package org.openmw

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.openmw.fragments.SettingsFragment
import org.openmw.navigation.MyFloatingActionButton
import org.openmw.navigation.MyTopBar
import org.openmw.utils.BouncingBackground
import org.openmw.utils.ModValue
import org.openmw.utils.ModValuesList

@DelicateCoroutinesApi
@ExperimentalFoundationApi
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(context: Context, modValues: List<ModValue>, navigateToSettings: () -> Unit) {
    val transparentBlack = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
    val currentContext = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 500
            ),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )
    var savedPath by remember { mutableStateOf<String?>(null) }
    val buttonText = if (savedPath.isNullOrEmpty() || savedPath == "Game Files: ") {
        "Select Games Files"
    } else {
        "Game Files: $savedPath"
    }

    LaunchedEffect(Unit) {
        val dataStoreKey = stringPreferencesKey("game_files_uri")
        val uriString = context.dataStore.data.map { preferences ->
            preferences[dataStoreKey]
        }.first()
        savedPath = uriString?.let { getAbsolutePathFromUri(context, Uri.parse(it)) }
    }

    val openDocumentTreeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                val settingsFragment = SettingsFragment()
                settingsFragment.handleDocumentTreeSelection(currentContext, uri) { newPath ->
                    savedPath = newPath
                }
            }
        }
    }
    Scaffold(
        topBar = {
            MyTopBar(context)
        }, content = @Composable {
            BouncingBackground()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp, bottom = 80.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            }
                            openDocumentTreeLauncher.launch(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .height(56.dp)
                            .let {
                                if (savedPath.isNullOrEmpty() || savedPath == "Game Files: ") {
                                    it.graphicsLayer(scaleX = pulse, scaleY = pulse)
                                } else {
                                    it
                                }
                            },
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
                        )
                    ) {
                        Text(text = buttonText, color = Color.White)
                    }

                    ModValuesList(modValues)
                }
            }
        }, bottomBar = {
            BottomAppBar(
                containerColor = transparentBlack,
                actions = {
                    Button(
                        onClick = { navigateToSettings() },
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.Transparent),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings, // Make sure you have the correct icon import
                            contentDescription = "Settings",
                            modifier = Modifier.size(80.dp),
                            tint = Color.White
                        )
                    }

                },
                floatingActionButton = {
                    MyFloatingActionButton(context)
                }
            )
        }
    )
}

suspend fun storeGameFilesUri(context: Context, uri: Uri) {
    context.dataStore.edit { preferences ->
        preferences[GameFilesPreferences.GAME_FILES_URI_KEY] = uri.toString()
    }
}

fun getAbsolutePathFromUri(context: Context, uri: Uri): String? {
    val documentFile = DocumentFile.fromTreeUri(context, uri)
    val path = documentFile?.uri?.path?.replace("/tree/primary:", "/storage/emulated/0/")
    return path?.substringBeforeLast("document")
}
