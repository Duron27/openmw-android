package org.openmw.utils

import android.os.Build
import android.util.Log
import org.openmw.Constants
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class CaptureCrash : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        saveCrashLog(throwable)

        // Terminate the app or perform any other necessary action
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(1)
    }

    private fun saveCrashLog(throwable: Throwable) {
        try {
            val logFile = File(Constants.CRASH_FILE)
            if (!logFile.exists()) {
                Log.d("LogFile", "File does not exist: ${logFile.path}")
                val created = logFile.createNewFile()
                if (!created) {
                    Log.d("LogFile", "File creation failed, using fallback: ${Constants.INTERNAL_CRASH_FILE}")
                    fallbackCrashLog(throwable)
                    return
                }
            }
            writeCrashLog(logFile, throwable)
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackCrashLog(throwable)
        }
    }

    private fun fallbackCrashLog(throwable: Throwable) {
        try {
            val fallbackFile = File(Constants.INTERNAL_CRASH_FILE)
            if (!fallbackFile.exists()) {
                fallbackFile.createNewFile()
            }
            writeCrashLog(fallbackFile, throwable)
        } catch (e: Exception) {
            Log.e("FallbackLog", "Failed to write crash log to fallback file: ${e.message}")
        }
    }

    private fun writeCrashLog(file: File, throwable: Throwable) {
        FileWriter(file, true).use { writer ->
            writer.append("Device: ${Build.MODEL} (API ${Build.VERSION.SDK_INT})\n")
            writer.append("${getCurrentDateTime()}:\t")
            printFullStackTrace(throwable, PrintWriter(writer))
        }
    }

    private fun printFullStackTrace(throwable: Throwable, printWriter: PrintWriter) {
        printWriter.println(throwable.toString())
        throwable.stackTrace.forEach { element ->
            printWriter.print("\t $element \n")
        }
        val cause = throwable.cause
        if (cause != null) {
            printWriter.print("Caused by:\t")
            printFullStackTrace(cause, printWriter)
        }
        printWriter.print("\n")
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
