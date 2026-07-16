package com.android.support

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date

object CrashHandler {

    val DEFAULT_UNCAUGHT_EXCEPTION_HANDLER: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    @JvmStatic
    fun init(app: Context, overlayRequired: Boolean) {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AppCrash", "Error just launched")
            try {
                handleUncaughtException(app, throwable)
            } catch (e: Throwable) {
                e.printStackTrace()
                DEFAULT_UNCAUGHT_EXCEPTION_HANDLER?.uncaughtException(thread, throwable)
                    ?: System.exit(2)
            }
        }
    }

    private fun handleUncaughtException(app: Context, throwable: Throwable) {
        Log.e("AppCrash", "Try saving log")

        val time = SimpleDateFormat("yyyy_MM_dd-HH_mm_ss").format(Date())
        val fileName = "mod_menu_crash_$time.txt"

        val dirName = if (Build.VERSION.SDK_INT >= 30) {
            "/storage/emulated/0/Documents/"
        } else {
            app.getExternalFilesDir(null).toString()
        }

        val crashFile = File(dirName, fileName)

        var versionName = "unknown"
        var versionCode = 0L
        try {
            val packageInfo = app.packageManager.getPackageInfo(app.packageName, 0)
            versionName = packageInfo.versionName
            versionCode = if (Build.VERSION.SDK_INT >= 28)
                packageInfo.longVersionCode
            else
                @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
        } catch (_: Exception) {}

        val fullStackTrace = StringWriter().also { sw ->
            throwable.printStackTrace(PrintWriter(sw))
        }.toString()

        val errorLog = buildString {
            appendLine("************* Crash Head ****************")
            appendLine("Time Of Crash      : $time")
            appendLine("Device Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Device Model       : ${Build.MODEL}")
            appendLine("Android Version    : ${Build.VERSION.RELEASE}")
            appendLine("Android SDK        : ${Build.VERSION.SDK_INT}")
            appendLine("App VersionName    : $versionName")
            appendLine("App VersionCode    : $versionCode")
            appendLine("************* Crash Head ****************")
            appendLine()
            append(fullStackTrace)
        }

        try {
            writeFile(crashFile, errorLog)
        } catch (_: IOException) {}

        Toast.makeText(app, "Game has crashed unexpectedly", Toast.LENGTH_LONG).show()
        Toast.makeText(
            app,
            "Log saved to: ${crashFile.toString().replace("/storage/emulated/0/", "")}",
            Toast.LENGTH_LONG
        ).show()

        Log.e("AppCrash", "Done")
        System.exit(2)
    }

    private fun writeFile(file: File, content: String) {
        file.parentFile?.mkdirs()
        file.createNewFile()
        FileOutputStream(file).use { it.write(content.toByteArray()) }
    }
}
