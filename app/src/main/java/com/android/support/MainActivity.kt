package com.android.support

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

class MainActivity : Activity() {

    /** Fully-qualified class name of the game's main Activity. */
    private val gameActivity = "com.unity3d.player.UnityPlayerActivity"
    private var hasLaunched  = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasLaunched) {
            hasLaunched = true
            try {
                // Launch the game's Activity, then start the mod menu
                startActivity(Intent(this, Class.forName(gameActivity)))
                Main.Start(this)
                return
            } catch (e: ClassNotFoundException) {
                Log.e("Mod_menu", "Error. Game's main activity does not exist")
                // Uncomment below for METHOD 2 (no game activity):
                // Toast.makeText(this, "Error. Game's main activity does not exist", Toast.LENGTH_LONG).show()
            }
        }

        // Fallback — launch menu without game Activity
        Main.Start(this)
    }
}
