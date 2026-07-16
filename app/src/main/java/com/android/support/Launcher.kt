package com.android.support

import android.app.ActivityManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View

class Launcher : Service() {

    private lateinit var menu: Menu

    override fun onCreate() {
        super.onCreate()

        menu = Menu(this)
        menu.setWindowManagerWindowService()

        // Poll every second: hide menu when the game loses foreground
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                checkGameForeground()
                handler.postDelayed(this, 1000)
            }
        })
    }

    override fun onBind(intent: Intent): IBinder? = null

    /** Returns true when the game process is no longer in the foreground. */
    private fun isNotInGame(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    private fun checkGameForeground() {
        menu.setVisibility(if (isNotInGame()) View.INVISIBLE else View.VISIBLE)
    }

    override fun onDestroy() {
        super.onDestroy()
        menu.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        try {
            Thread.sleep(100)
        } catch (_: InterruptedException) {}
        stopSelf()
    }

    /** START_NOT_STICKY: don't restart the service if it's killed. */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_NOT_STICKY
}
