package org.openmw.fragments

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.openmw.Constants
import org.openmw.utils.IniConverter
import org.openmw.utils.dataStore
import java.io.File

private val job = Job()
private val scope = CoroutineScope(Dispatchers.IO + job)

private fun findFilesWithExtensions(directory: DocumentFile?, extensions: Array<String>): List<DocumentFile> {
    return directory?.listFiles()?.filter { file ->
        extensions.any { file.name?.endsWith(".$it") == true }
    } ?: emptyList()
}

fun processSelectedFolder(context: Context, folder: File, onUriPersisted: (String?) -> Unit) {
    val dataStoreKey = stringPreferencesKey("game_files_uri")
    try {
        scope.launch {
            val savedPath = folder.absolutePath
            val selectedDirectory = DocumentFile.fromFile(folder)
            val iniFile = selectedDirectory.findFile("Morrowind.ini")
            val dataFilesFolder = selectedDirectory.findFile("Data Files")
            val extensions = arrayOf("esm", "bsa")
            val modDirectory = dataFilesFolder ?: selectedDirectory
            val files = findFilesWithExtensions(modDirectory, extensions)
            val fileName = Constants.OPENMW_CFG
            val file = File(fileName)
            val regexData = Regex("""^data\s*=\s*".*?"""")
            val replacementStringData = """data="${savedPath}/Data Files""""
            val overridePath = Constants.USER_FILE_STORAGE + "/OpenMW/Override"

            if (iniFile != null && dataFilesFolder != null && dataFilesFolder.isDirectory) {
                // The path is already absolute, no need for Uri conversion
                val uriString = context.dataStore.data.map { preferences ->
                    preferences[dataStoreKey]
                }.first()
                onUriPersisted(uriString)

                // Prepare content lines
                val orderedEsmFiles = listOf("Morrowind.esm", "Tribunal.esm", "Bloodmoon.esm")
                val esmContentLines = orderedEsmFiles.filter { name ->
                    files.any { file -> file.name == name }
                }.map { name ->
                    "content=$name"
                }

                // Prepare fallback-archive lines
                val orderedBsaFiles = listOf("Morrowind.bsa", "Tribunal.bsa", "Bloodmoon.bsa")
                val fallbackArchiveLines = orderedBsaFiles.filter { name ->
                    files.any { file -> file.name == name }
                }.map { name ->
                    "fallback-archive=$name"
                }

                // Read and replace lines in the file
                val lines = file.readLines().map { line ->
                    var modifiedLine = line
                    if (line.contains(regexData)) {
                        modifiedLine = modifiedLine.replace(regexData, replacementStringData)
                    }
                    if (line.contains("resources=./resources")) {
                        modifiedLine = modifiedLine.replace(
                            "resources=./resources",
                            "resources=${Constants.USER_RESOURCES}"
                        )
                    }
                    // Add more replacements if needed
                    modifiedLine
                }

                // Write modified lines back to the file
                file.bufferedWriter().use { writer ->
                    lines.forEach { line ->
                        writer.write(line)
                        writer.newLine()
                    }

                    // Append fallback-archive lines
                    fallbackArchiveLines.forEach { line ->
                        writer.write(line)
                        writer.newLine()
                    }
                }

                // Write .esm content lines to a separate file
                val esmFile = File(Constants.USER_OPENMW_CFG)
                esmFile.bufferedWriter().use { writer ->
                    // Write replacementStringData at the top
                    writer.write(replacementStringData)
                    writer.newLine()

                    // Write esm content lines
                    esmContentLines.forEach { line ->
                        writer.write(line)
                        writer.newLine()
                    }
                }

                // Fix Calender
                val file2 = File(Constants.OPENMW_CFG)
                val lineToAdd = "data=${Constants.USER_RESOURCES}/vfs-mw\n"
                file2.appendText(lineToAdd)

                val iniData = iniFile.uri.let { uri ->
                    context.contentResolver.openInputStream(uri)?.bufferedReader()
                    .use { it?.readText() }
                }
                val converter = IniConverter(iniData ?: "")
                val convertedData = converter.convert()

                listOf(
                    "openmw.cfg",
                    "openmw.base.cfg",
                    "openmw.fallback.cfg"
                ).forEach { outputFileName ->
                    val outputFile = File(Constants.GLOBAL_CONFIG, outputFileName)
                    val currentContent = outputFile.readText()

                    // Append convertedData if not already in the file
                    if (!currentContent.contains(convertedData)) {
                        outputFile.appendText(convertedData + "\n")
                    }

                    // Append "data-local=$overridePath\n" if not already in the file
                    val dataLocalString = "data-local=$overridePath\n"
                    if (!currentContent.contains(dataLocalString)) {
                        outputFile.appendText(dataLocalString)
                    }
                }
            } else {
                context.dataStore.edit { preferences ->
                    preferences[dataStoreKey] = ""
                }
                onUriPersisted("")
                //Toast.makeText(context, "Please select a folder with Morrowind.ini.", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onUriPersisted(null)
        Toast.makeText(context, "An error occurred while selecting the folder.", Toast.LENGTH_SHORT).show()
    }
}
