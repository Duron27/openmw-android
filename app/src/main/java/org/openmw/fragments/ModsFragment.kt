package org.openmw.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openmw.Constants
import org.openmw.getAbsolutePathFromUri
import org.openmw.utils.ModValue
import org.openmw.utils.writeModValuesToFile

class ModsFragment {

    fun findFilesWithExtensions(
        directory: DocumentFile?,
        extensions: Array<String>
    ): List<DocumentFile> {
        return directory?.listFiles()?.filter { file ->
            extensions.any { file.name?.endsWith(".$it") == true }
        } ?: emptyList()
    }

    fun modDocumentTreeSelection(context: Context, uri: Uri, onUriPersisted: (String?) -> Unit) {
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            CoroutineScope(Dispatchers.IO).launch {
                val ignoreList = listOf("Morrowind.bsa", "Tribunal.bsa", "Bloodmoon.bsa")
                val extensions = arrayOf("bsa", "esm", "esp", "omwaddon", "omwgame", "omwscripts")
                val selectedDirectory = DocumentFile.fromTreeUri(context, uri)
                val files = findFilesWithExtensions(selectedDirectory, extensions)
                val modPath = getAbsolutePathFromUri(context, uri)

                val modValues = files.mapIndexed { index, file ->
                    val fileName = file.name ?: ""
                    val nameWithoutExtension = fileName.substringBeforeLast(".")
                    val extension = fileName.substringAfterLast(".")
                    ModValue(index, "content", "$nameWithoutExtension.$extension", isChecked = true, originalIndex = index + 1) // Set isChecked as needed
                }.toMutableList()

                modValues.add(ModValue(modValues.size, "data", modPath ?: "", isChecked = true, originalIndex = modValues.size + 1)) // Set isChecked as needed

                writeModValuesToFile(modValues, Constants.USER_OPENMW_CFG, ignoreList)

                if (files.isNotEmpty()) {
                    onUriPersisted(modPath)
                } else {
                    onUriPersisted("")
                    showToast(context, "Please select a folder that contains Mods.")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onUriPersisted(null)
            showToast(context, "An error occurred while selecting the folder.")
        }
    }


    private fun showToast(context: Context, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}

fun LaunchDocumentTree(
    openDocumentTreeLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context,
    onPathSelected: (String?) -> Unit
) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
    openDocumentTreeLauncher.launch(intent)
}
