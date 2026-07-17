package com.android.support

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Kotlin replacement for Menu.java.
 *
 * Class name MUST remain "Menu" — the native library's JNI_OnLoad registers
 * its methods against "com/android/support/Menu" via RegisterNatives.
 *
 * Manages the WindowManager overlay window and hosts the Jetpack Compose
 * floating menu inside a [ComposeView].  All native method declarations
 * are identical to the originals so the C++ side is unchanged.
 */
class Menu(private val context: Context) {

    companion object {
        @JvmField var SCREEN_WIDTH  = 0
        @JvmField var SCREEN_HEIGHT = 0

        // Initial position of the floating icon
        private const val INIT_POS_X = 0
        private const val INIT_POS_Y = 100
    }

    // ── Native method declarations (unchanged from Menu.java) ──────────────
    external fun Init(context: Context, title: TextView, subTitle: TextView)
    external fun Icon(): String
    external fun IconWebViewData(): String
    external fun GetFeatureList(): Array<String>
    external fun SettingsList(): Array<String>
    external fun IsGameLibLoaded(): Boolean

    // ── Internal state ─────────────────────────────────────────────────────
    private var windowManager: WindowManager? = null
    private var vmParams: WindowManager.LayoutParams? = null
    private var composeView: ComposeView?    = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    /** True when the window was added via [setWindowManagerWindowService]. */
    var overlayRequired = false
        private set

    // Title / subtitle are written by native Init() once, then held as state
    private val titleState    = mutableStateOf("")
    private val subTitleState = mutableStateOf("")

    init {
        Preferences.context = context
    }

    // ── Public setup API ───────────────────────────────────────────────────

    /**
     * Sets up a TYPE_APPLICATION_OVERLAY window (used from [Launcher] service).
     * Builds and attaches the Compose view immediately.
     */
    @SuppressLint("WrongConstant")
    fun setWindowManagerWindowService() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        vmParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = INIT_POS_X
            y = INIT_POS_Y
        }

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayRequired = true
        buildAndAttachView()
    }

    /**
     * Sets up a TYPE_APPLICATION window (used when the caller is an Activity).
     * Builds and attaches the Compose view immediately.
     */
    @Suppress("DEPRECATION")
    fun setWindowManagerActivity() {
        vmParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
            PixelFormat.TRANSPARENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = INIT_POS_X
            y = INIT_POS_Y
        }

        windowManager = (context as Activity).windowManager
        buildAndAttachView()
    }

    // ── Lifecycle helpers (called from Launcher) ───────────────────────────

    /** Show / hide the entire overlay (e.g. when the game loses foreground). */
    fun setVisibility(visibility: Int) {
        composeView?.visibility = visibility
    }

    /** Remove the overlay from the screen and release resources. */
    fun onDestroy() {
        lifecycleOwner?.onDestroy()
        composeView?.let { v ->
            try { windowManager?.removeView(v) } catch (_: Exception) {}
        }
        composeView    = null
        lifecycleOwner = null
    }

    // ── Private ────────────────────────────────────────────────────────────

    /**
     * Creates the [ComposeView], wires up the [OverlayLifecycleOwner], calls
     * native [Init] to obtain title/subtitle strings, then adds the view to
     * the WindowManager.  The Compose tree starts rendering after this call.
     */
    private fun buildAndAttachView() {
        // Wrap context with the app theme so ComposeView and Material3 can
        // resolve theme attributes (Service context has no UI theme by default)
        val themedContext = ContextThemeWrapper(context, R.style.AppTheme)

        // Let native code set title and subtitle on temporary TextViews
        val titleTv    = TextView(themedContext)
        val subTitleTv = TextView(themedContext)
        Init(context, titleTv, subTitleTv)
        titleState.value    = titleTv.text.toString()
        subTitleState.value = subTitleTv.text.toString()

        // Lifecycle owner required for Compose outside of an Activity
        val lifecycle = OverlayLifecycleOwner().also { lifecycleOwner = it }
        lifecycle.onCreate()

        val view = ComposeView(themedContext).also { composeView = it }

        // Wire tree-level owners so Compose internals can find them
        view.setViewTreeLifecycleOwner(lifecycle)
        view.setViewTreeViewModelStoreOwner(lifecycle)
        view.setViewTreeSavedStateRegistryOwner(lifecycle)

        view.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )

        // Capture locals for the Compose lambda (avoid leaking `this` reference)
        val wm      = windowManager!!
        val params  = vmParams!!
        val overlay = this

        view.setContent {
            FloatingMenu(
                overlay         = overlay,
                windowManager   = wm,
                vmParams        = params,
                composeView     = view,
                title           = titleState.value,
                subTitle        = subTitleState.value,
                overlayRequired = overlayRequired,
            )
        }

        wm.addView(view, params)

        // Advance lifecycle so Compose recomposition and coroutines run
        lifecycle.onStart()
        lifecycle.onResume()
    }
}
