package com.android.support

import android.app.Activity
import android.content.Context

object Main {

    // Load native library (name must match Android.mk)
    init {
        System.loadLibrary("clerx")
    }

    @JvmStatic private external fun CheckOverlayPermission(context: Context)

    /**
     * Starts the overlay without requesting the SYSTEM_ALERT_WINDOW permission
     * (use when the caller is already an Activity that owns its own window).
     */
    @JvmStatic
    fun StartWithoutPermission(context: Context) {
        CrashHandler.init(context)
        if (context is Activity) {
            val menu = Menu(context)
            menu.setWindowManagerActivity()
        } else {
            CheckOverlayPermission(context)
        }
    }

    /**
     * Normal entry point — asks for overlay permission via native helper,
     * which will then start [Launcher] once permission is granted.
     */
    @JvmStatic
    fun Start(context: Context) {
        CrashHandler.init(context)
        CheckOverlayPermission(context)
    }
}
