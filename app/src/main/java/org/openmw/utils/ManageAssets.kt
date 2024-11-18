package org.openmw.utils

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.content.Context
import android.util.Log
import org.openmw.Constants

/**
 * Helper class to handle copying assets to the storage
 * @param context Android context to use
 */
class ManageAssets(val context: Context) {

    /**
     * Copies assets recursively
     * @param src Source directory in assets
     * @param dst Destination directory on disk, absolute path
     */
    fun copy(src: String, dst: String) {
        val assetManager = context.assets
        try {
            val assets = assetManager.list(src) ?: return
            if (assets.isEmpty()) {
                copyFile(src, dst)
            } else {
                // Recurse into a subdirectory
                val dir = File(dst)
                if (!dir.exists())
                    dir.mkdirs()
                for (i in assets.indices) {
                    copy(src + "/" + assets[i], dst + "/" + assets[i])
                }
            }
        } catch (ex: IOException) {
            Log.e("ManageAssets", "Error copying assets from $src to $dst", ex)
        }
    }

    /**
     * Copies a single file from assets to disk
     * @param src Path of source file inside assets
     * @param dst Absolute path to destination file on disk
     */
    fun copyFile(src: String, dst: String) {
        try {
            val inp = context.assets.open(src)
            val out = FileOutputStream(dst)

            inp.copyTo(out)
            out.flush()

            inp.close()
            out.close()

            Log.d("ManageAssets", "Copied file from $src to $dst")
        } catch (e: IOException) {
            Log.e("ManageAssets", "Error copying file from $src to $dst", e)
        }
    }
}

class UserManageAssets(val context: Context) {

    val assetCopier = ManageAssets(context)

    fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory)
            for (child in fileOrDirectory.listFiles()!!)
                deleteRecursive(child)

        fileOrDirectory.delete()
    }

    /**
     * Removes old and creates new files located in private application directories
     * (i.e. under getFilesDir(), or /data/data/.../files)
     */
    fun reinstallStaticFiles() {
        // wipe old version first
        removeStaticFiles()

        Log.d("ManageAssets", "Copying resources to ${Constants.RESOURCES}")
        assetCopier.copy("libopenmw/resources", Constants.RESOURCES)

        Log.d("ManageAssets", "Copying global config to ${Constants.GLOBAL_CONFIG}")
        assetCopier.copy("libopenmw/openmw", Constants.GLOBAL_CONFIG)

        // set up user config (if not present)
        File(Constants.USER_CONFIG).mkdirs()
        if (!File(Constants.USER_OPENMW_CFG).exists()) {
            File(Constants.USER_OPENMW_CFG).writeText("# This is the user openmw.cfg. Feel free to modify it as you wish.\n")
        }
    }

    /**
     * Reset User Config
     */
    fun resetUserConfig() {
        // Check if the file exists and delete it if it does
        val settingsFile = File(Constants.SETTINGS_FILE)
        if (settingsFile.exists()) {
            Log.d("ManageAssets", "Deleting existing file: ${Constants.SETTINGS_FILE}")
            settingsFile.delete()
        }

        // Copy over settings.cfg
        Log.d("ManageAssets", "Copying settings.cfg to ${Constants.SETTINGS_FILE}")
        assetCopier.copy("libopenmw/openmw/settings.fallback.cfg", Constants.SETTINGS_FILE)
    }


    /**
     * Removes global static files, these include resources and config
     */
    fun removeStaticFiles() {
        // remove version stamp so that reinstallStaticFiles is called during game launch
        File(Constants.VERSION_STAMP).delete()

        deleteRecursive(File(Constants.GLOBAL_CONFIG))
        deleteRecursive(File(Constants.RESOURCES))
    }

    /**
     * First launch
     */
    fun onFirstLaunch() {
        if (!File(Constants.USER_FILE_STORAGE + "/resources/").isDirectory) {
            reinstallStaticFiles()

            val src = File(Constants.RESOURCES)
            val dst = File(Constants.USER_FILE_STORAGE + "/resources/")
            dst.mkdirs()
            src.copyRecursively(dst, true)
        }

        // set up user config (if not present)
        File(Constants.USER_CONFIG).mkdirs()
        if (!File(Constants.USER_OPENMW_CFG).exists()) {
            File(Constants.USER_OPENMW_CFG).writeText("# This is the user openmw.cfg. Feel free to modify it as you wish.\n")
        }

        // Create default UI
        val file = File("${Constants.USER_CONFIG}/UI.cfg")
        if (!file.exists()) {
            file.createNewFile()
            file.appendText("""
                ButtonID_1(60.0;2054.6936;18.942787;false;111;Color.Black;0.25)
                ButtonID_2(60.0;1805.0613;700.42505;false;54;Color.Black;0.25)
                ButtonID_3(60.0;1942.9843;561.5578;false;30;Color.Black;0.25)
                ButtonID_4(60.0;1805.0613;422.69055;false;33;Color.Black;0.25)
                ButtonID_5(60.0;1668.5325;561.5578;false;52;Color.Black;0.25)
                ButtonID_6(60.0;1335.1458;770.3131;false;62;Color.Black;0.25)
                ButtonID_7(60.0;750.73267;770.3131;false;66;Color.Black;0.25)
                ButtonID_98(160.0;1199.7069;216.80106;false;98;Color.Green;0.25)
                ButtonID_99(200.0;200.56776;281.6349;false;29;Color.Black;0.25)
            """.trimIndent())
        }

        // copy user settings file
        if (!File(Constants.SETTINGS_FILE).exists()) {
            Log.d("ManageAssets", "Copying resources to ${Constants.SETTINGS_FILE}")
            assetCopier.copy("libopenmw/openmw/settings.fallback.cfg", Constants.SETTINGS_FILE)
        }

        // copy fallback openmw_base.cfg
        if (!File(Constants.OPENMW_BASE_CFG).exists()) {
            Log.d("ManageAssets", "Copying base config to ${Constants.OPENMW_BASE_CFG}")
            assetCopier.copy("libopenmw/openmw/openmw.base.cfg", Constants.OPENMW_BASE_CFG)
        }

        // copy fallback openmw_fallback.cfg
        if (!File(Constants.OPENMW_FALLBACK_CFG).exists()) {
            Log.d("ManageAssets", "Copying fallback config to ${Constants.OPENMW_FALLBACK_CFG}")
            assetCopier.copy("libopenmw/openmw/openmw.fallback.cfg", Constants.OPENMW_FALLBACK_CFG)
        }
    }

    /**
     * Reset user resource files to default
     */
    fun removeResourceFiles() {
        reinstallStaticFiles()
        deleteRecursive(File(Constants.USER_FILE_STORAGE + "/resources/"))

        val src = File(Constants.RESOURCES)
        val dst = File(Constants.USER_FILE_STORAGE + "/resources/")
        dst.mkdirs()
        src.copyRecursively(dst, true)
    }

    /**
     * Resets user config to default values by removing it
     */
    fun removeUserConfig() {
        deleteRecursive(File(Constants.USER_CONFIG))
        File(Constants.USER_FILE_STORAGE + "/config").mkdirs()
        File(Constants.USER_OPENMW_CFG).writeText("# This is the user openmw.cfg. Feel free to modify it as you wish.\n")
    }

    companion object
}

