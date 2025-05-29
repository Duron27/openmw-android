package org.openmw.utils

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import org.openmw.Constants
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ManageAssets(private val context: Context) {

    fun copy(src: String, dst: String) {
        val assetManager = context.assets
        val assets = assetManager.list(src) ?: return

        if (assets.isEmpty()) {
            copyFile(src, dst)
        } else {
            File(dst).apply { mkdirs() }
            assets.forEach { asset ->
                copy("$src/$asset", "$dst/$asset")
            }
        }
    }

    private fun copyFile(src: String, dst: String) {
        val destinationFile = File(dst)
        if (destinationFile.exists()) {
            Log.d("ManageAssets", "File $dst already exists, skipping copy.")
            addCustomLog("ManageAssets, File $dst already exists, skipping copy.", textSize = 10, textColor = Color.White)
            return
        }
        try {
            context.assets.open(src).use { input ->
                FileOutputStream(dst).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("ManageAssets", "Copied file from $src to $dst")
            addCustomLog("ManageAssets Copied file from $src to $dst", textSize = 10, textColor = Color.White)
        } catch (e: IOException) {
            Log.e("ManageAssets", "Error copying file from $src to $dst", e)
        }
    }
}

class UserManageAssets(val context: Context) {
    val assetCopier = ManageAssets(context)

    fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach { deleteRecursive(it) }
        }
        fileOrDirectory.delete()
    }

    fun resetUserConfig() {
        val settingsFile = File(Constants.SETTINGS_FILE)
        if (settingsFile.exists()) {
            Log.d("ManageAssets", "Deleting existing file: ${Constants.SETTINGS_FILE}")
            settingsFile.delete()
        }
        assetCopier.copy("libopenmw/openmw/settings.fallback.cfg", Constants.SETTINGS_FILE)
    }

    private fun copyIfNotExists(source: String, destination: String) {
        if (!File(destination).exists()) {
            Log.d("ManageAssets", "Copying $source to $destination")
            assetCopier.copy(source, destination)
        }
    }

    private fun ensureDirectoryExists(directory: String) {
        val dir = File(directory)
        if (!dir.isDirectory) {
            dir.mkdirs()
            Log.d("ManageAssets", "Created directory at $directory")
        }
    }

    fun onFirstLaunch() {
        assetCopier.copy("libopenmw/openmw", Constants.GLOBAL_CONFIG)

        // Setup storage directories
        listOf("OpenMW/Override", "OpenMW/CACHE", "OpenMW/Mods", "OpenMW/Exports", "ui", "delta", "config", "libs").forEach { dir ->
            ensureDirectoryExists("${Constants.USER_FILE_STORAGE}/$dir")
        }

        if (!File(Constants.USER_OPENMW_CFG).exists()) {
            File(Constants.USER_OPENMW_CFG).writeText("# This is the user openmw.cfg. Feel free to modify it as you wish.\n")
        }

        assetCopier.copy("libopenmw/resources", Constants.USER_RESOURCES)
        assetCopier.copy("libopenmw/ui", Constants.USER_UI)
        copyIfNotExists("libopenmw/openmw/settings.fallback.cfg", Constants.SETTINGS_FILE)
        copyIfNotExists("libopenmw/openmw/UMOhelper.sh", Constants.USER_CONFIG + "/UMOhelper.sh")
        copyIfNotExists("libopenmw/ui/input_v3.xml", Constants.USER_CONFIG + "/input_v3.xml")
    }

    fun resetUI() {
        val uiDir = File(Constants.USER_UI)
        uiDir.deleteRecursively()
        if (uiDir.exists()) {
            uiDir.delete()
        }
        ensureDirectoryExists("${Constants.USER_FILE_STORAGE}/ui")
        assetCopier.copy("libopenmw/ui", Constants.USER_UI)
    }

    fun installUQMResourceFiles() {
        File(Constants.USER_CONFIG).mkdirs()
        if (!File(Constants.USER_FILE_STORAGE + "/resources/libuqm").isDirectory) {
            assetCopier.copy("libuqm", Constants.USER_RESOURCES + "/libuqm")
        }
    }
}
