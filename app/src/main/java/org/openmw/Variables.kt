package org.openmw

import android.app.Application
import android.os.Environment
import java.io.File

val jniLibsArray = arrayOf("GL", "SDL2", "c++_shared", "openal", "openmw")
val jniLibsArrayNavmesh = arrayOf("SDL2", "c++_shared", "libopenmw-navmeshtool.so")
const val OPENMW_MAIN_LIB = "libopenmw.so"
const val OPENMW_NAVMESH_LIB = "libopenmw-navmeshtool.so"

// Openmw Path Variables
object Constants {
    const val APP_PREFERENCES = "settings"

    var USER_FILE_STORAGE = ""
    var DEFAULTS_BIN = ""
    var OPENMW_CFG = ""
    var SETTINGS_FILE = ""
    var LOGCAT_FILE = ""
    var OPENMW_LOG = ""
    var OPENMW_BASE_CFG = ""
    var OPENMW_FALLBACK_CFG = ""
    var RESOURCES = ""
    var GLOBAL_CONFIG = ""
    var USER_CONFIG = ""
    var USER_SAVES = ""
    var USER_DELTA = ""
    var USER_OPENMW_CFG = ""
    var VERSION_STAMP = ""
    var CRASH_FILE = ""

}

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        app = this

        // Set up global paths
        //Constants.USER_FILE_STORAGE = applicationContext.getExternalFilesDir(null)?.absolutePath ?: ""
        Constants.USER_FILE_STORAGE = Environment.getExternalStorageDirectory().toString() + "/OMW/"
        Constants.USER_CONFIG = "${Constants.USER_FILE_STORAGE}/config"
        Constants.USER_SAVES = "${Constants.USER_FILE_STORAGE}/saves"
        Constants.USER_DELTA = "${Constants.USER_FILE_STORAGE}/delta"
        Constants.USER_OPENMW_CFG = "${Constants.USER_CONFIG}/openmw.cfg"
        Constants.SETTINGS_FILE = "${Constants.USER_CONFIG}/settings.cfg"
        Constants.LOGCAT_FILE = "${Constants.USER_CONFIG}/openmw_logcat.txt"
        Constants.OPENMW_LOG = "${Constants.USER_CONFIG}/openmw.log"
        Constants.CRASH_FILE = "${Constants.USER_CONFIG}/crash.log"
        Constants.DEFAULTS_BIN = File(filesDir, "config/defaults.bin").toString()
        Constants.OPENMW_CFG = File(filesDir, "config/openmw.cfg").absolutePath
        Constants.OPENMW_BASE_CFG = File(filesDir, "config/openmw.base.cfg").absolutePath
        Constants.OPENMW_FALLBACK_CFG = File(filesDir, "config/openmw.fallback.cfg").absolutePath
        Constants.RESOURCES = File(filesDir, "resources").absolutePath
        Constants.GLOBAL_CONFIG = File(filesDir, "config").absolutePath
        Constants.VERSION_STAMP = File(filesDir, "stamp").absolutePath


    }

    companion object {
        lateinit var app: MyApp
    }
}
