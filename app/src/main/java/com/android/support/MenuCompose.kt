package com.android.support

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.delay
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
//  Colour palette
// ─────────────────────────────────────────────────────────────────────────────
private val CText        = Color(0xFFFFFFFF)
private val CTextSub     = Color(0xFFB0B0C8)
private val CBtnBg       = Color(0xFF1E1E32)
private val CMenuBg      = Color(0xF20D0D1A)   // nearly-opaque dark
private val CFeatureBg   = Color(0xFF111120)
private val CToggleOn    = Color(0xFF32CB00)
private val CToggleOff   = Color(0xFF2A2A3E)
private val CBtnOn       = Color(0xFF1A5200)
private val CBtnOff      = Color(0xFF1E1E32)
private val CCategoryBg  = Color(0xFF1A1A2E)
private val CCollapseBg  = Color(0xFF161626)
private val CBorder      = Color(0xFF32CB00)
private val CHeaderBg    = Color(0xFF090914)

private val MenuCorner  = 14.dp
private val MenuWidth   = 290.dp
private val MenuScrollH = 220.dp

// ─────────────────────────────────────────────────────────────────────────────
//  Gesture state — plain (non-Compose-state) object so mutations never
//  trigger recomposition and survive recompositions via remember {}
// ─────────────────────────────────────────────────────────────────────────────
private class GestureState {
    var startRawX  = 0f
    var startRawY  = 0f
    var initWinX   = 0f
    var initWinY   = 0f
    var isDragging = false
}

// ─────────────────────────────────────────────────────────────────────────────
//  Root composable — the complete floating overlay
//
//  BUG FIXES vs. the original implementation:
//
//  1. Drag now uses MotionEvent.rawX/rawY (absolute screen coordinates) via
//     pointerInteropFilter. The original used local Compose coordinates; when
//     updateViewLayout() moved the window, the next event's local coords were
//     shifted by the opposite amount, causing oscillation/shaking.
//
//  2. Each drag handler returns true on ACTION_DOWN to reliably CLAIM the
//     gesture stream. Without claiming, subsequent MOVE/UP delivery is not
//     guaranteed by the Android event-dispatch model.
//
//  3. The title-bar drag handler is scoped only to the title+dots Box inside
//     a Row; the settings gear lives as a sibling outside that Box and retains
//     its own independent clickable — no conflicts or blocked touches.
//
//  4. The scroll area (verticalScroll Column) has no drag modifier at all, so
//     scroll gestures are delivered to it unimpeded.
//
//  5. Tap detection on the icon is handled explicitly in ACTION_UP: if
//     isDragging is still false at UP time, the menu opens.
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FloatingMenu(
    overlay: Menu,
    windowManager: WindowManager,
    vmParams: WindowManager.LayoutParams,
    composeView: View,
    title: String,
    subTitle: String,
    overlayRequired: Boolean,
) {
    // ── Compose UI state ──────────────────────────────────────────────────
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var menuAlpha      by remember { mutableStateOf(1f) }

    // Window position — kept in sync with vmParams for the drag calculations
    var posX by remember { mutableStateOf(vmParams.x.toFloat()) }
    var posY by remember { mutableStateOf(vmParams.y.toFloat()) }

    // Feature / settings loading
    var featureItems by remember { mutableStateOf<List<FeatureItem>>(emptyList()) }
    var settingItems by remember { mutableStateOf<List<FeatureItem>>(emptyList()) }
    var menuReady    by remember { mutableStateOf(false) }
    var stopChecking by remember { mutableStateOf(false) }

    val isScrollExpanded = remember { mutableStateOf(Preferences.isExpanded) }

    LaunchedEffect(Unit) {
        if (Preferences.loadPref && !overlay.IsGameLibLoaded()) {
            while (!overlay.IsGameLibLoaded() && !stopChecking) { delay(600) }
        }
        featureItems = parseFeatureList(overlay.GetFeatureList())
        settingItems = parseFeatureList(overlay.SettingsList())
        menuReady    = true
    }

    // ── Gesture state objects (remembered, mutated without triggering recompose)
    val iconGesture  = remember { GestureState() }
    val panelGesture = remember { GestureState() }

    // Shared helper: apply raw-coordinate drag delta to the window
    fun applyDrag(state: GestureState, rawX: Float, rawY: Float) {
        posX = state.initWinX + (rawX - state.startRawX)
        posY = state.initWinY + (rawY - state.startRawY)
        vmParams.x = posX.toInt()
        vmParams.y = posY.toInt()
        windowManager.updateViewLayout(composeView, vmParams)
    }

    MaterialTheme {
    Box {

        // ── Collapsed floating icon ───────────────────────────────────────
        AnimatedVisibility(
            visible = !isMenuExpanded,
            enter   = fadeIn(tween(200)) + scaleIn(tween(200)),
            exit    = fadeOut(tween(150)) + scaleOut(tween(150)),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .alpha(menuAlpha)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF151528), Color(0xFF0A0A16))
                        )
                    )
                    .border(2.dp, CBorder, RoundedCornerShape(16.dp))
                    // ── Gesture handler ────────────────────────────────────
                    // Returns true on ACTION_DOWN to CLAIM the gesture stream
                    // so all subsequent MOVE/UP events are guaranteed to arrive.
                    // The tap-to-open is handled explicitly in ACTION_UP.
                    .pointerInteropFilter { event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                iconGesture.startRawX  = event.rawX
                                iconGesture.startRawY  = event.rawY
                                iconGesture.initWinX   = posX
                                iconGesture.initWinY   = posY
                                iconGesture.isDragging = false
                                true   // ← claim the stream
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val dx = abs(event.rawX - iconGesture.startRawX)
                                val dy = abs(event.rawY - iconGesture.startRawY)
                                if (iconGesture.isDragging || dx > 10f || dy > 10f) {
                                    iconGesture.isDragging = true
                                    menuAlpha = 0.55f
                                    applyDrag(iconGesture, event.rawX, event.rawY)
                                }
                                true
                            }
                            MotionEvent.ACTION_UP -> {
                                menuAlpha = 1f
                                if (!iconGesture.isDragging) {
                                    // No movement → treat as a tap, open the menu
                                    isMenuExpanded = true
                                }
                                iconGesture.isDragging = false
                                true
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                menuAlpha = 1f
                                iconGesture.isDragging = false
                                true
                            }
                            else -> false
                        }
                    }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text          = "S3",
                        color         = CToggleOn,
                        fontSize      = 22.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                    )
                    // Three-dot drag indicator
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(CToggleOn.copy(alpha = 0.6f))
                            )
                        }
                    }
                }
            }
        }

        // ── Expanded menu panel ───────────────────────────────────────────
        AnimatedVisibility(
            visible = isMenuExpanded,
            enter   = fadeIn(tween(200)) + expandVertically(tween(250)),
            exit    = fadeOut(tween(150)) + shrinkVertically(tween(200)),
        ) {
            Column(
                modifier = Modifier
                    .width(MenuWidth)
                    .wrapContentHeight()
                    .alpha(menuAlpha)
                    .clip(RoundedCornerShape(MenuCorner))
                    .background(CMenuBg)
                    .border(1.5.dp, CBorder, RoundedCornerShape(MenuCorner))
                // NOTE: NO drag modifier on the Column itself.
                // Drag is confined to the title-bar Row so that the
                // verticalScroll area below receives its gestures freely.
            ) {
                // ── Title bar ─────────────────────────────────────────────
                // Structure: Row { [drag area: dots + title (weight=1)] | [gear] }
                //
                // The drag pointerInteropFilter is on the weight=1 Box (center),
                // which is a SIBLING of the gear Text in the Row.
                // This means the gear's clickable is completely independent — it
                // is never inside the drag handler's touch scope, so returning
                // true on ACTION_DOWN does not block gear taps.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CHeaderBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Drag handle: dots + title — takes all space except the gear
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .pointerInteropFilter { event ->
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        panelGesture.startRawX  = event.rawX
                                        panelGesture.startRawY  = event.rawY
                                        panelGesture.initWinX   = posX
                                        panelGesture.initWinY   = posY
                                        panelGesture.isDragging = false
                                        true  // ← claim the stream
                                    }
                                    MotionEvent.ACTION_MOVE -> {
                                        val dx = abs(event.rawX - panelGesture.startRawX)
                                        val dy = abs(event.rawY - panelGesture.startRawY)
                                        if (panelGesture.isDragging || dx > 6f || dy > 6f) {
                                            panelGesture.isDragging = true
                                            menuAlpha = 0.55f
                                            applyDrag(panelGesture, event.rawX, event.rawY)
                                        }
                                        true
                                    }
                                    MotionEvent.ACTION_UP,
                                    MotionEvent.ACTION_CANCEL -> {
                                        menuAlpha = 1f
                                        panelGesture.isDragging = false
                                        true
                                    }
                                    else -> false
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            // Six-dot drag-grip indicator
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                repeat(2) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        repeat(3) {
                                            Box(
                                                modifier = Modifier
                                                    .size(2.5.dp)
                                                    .clip(CircleShape)
                                                    .background(CBorder.copy(alpha = 0.5f))
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text          = title,
                                color         = CToggleOn,
                                fontSize      = 16.sp,
                                fontWeight    = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Settings gear — sibling of the drag Box, NOT nested inside it.
                    // Has its own independent clickable; drag events never reach here.
                    val gearColor = if (isSettingsOpen) CToggleOn else CTextSub
                    Text(
                        text       = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) "⚙" else "\uD83D\uDD27",
                        color      = gearColor,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.clickable { isSettingsOpen = !isSettingsOpen },
                    )
                }

                // Marquee subtitle
                AndroidView(
                    factory = { ctx ->
                        android.widget.TextView(ctx).apply {
                            ellipsize          = android.text.TextUtils.TruncateAt.MARQUEE
                            marqueeRepeatLimit = -1
                            isSingleLine       = true
                            isSelected         = true
                            textSize           = 10f
                            gravity            = android.view.Gravity.CENTER
                            setPadding(12, 0, 12, 4)
                            setTextColor(android.graphics.Color.argb(180, 180, 180, 200))
                            text = subTitle
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(CHeaderBg)
                )

                // Green accent divider under header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(CBorder)
                )

                // ── Feature / settings list ───────────────────────────────
                // verticalScroll works here because there is NO drag modifier
                // on this Box or its Column — drag is isolated to the title bar.
                val scrollMod = if (isScrollExpanded.value)
                    Modifier.weight(1f)
                else
                    Modifier.height(MenuScrollH)

                Box(modifier = scrollMod.background(CFeatureBg)) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(vertical = 4.dp)
                    ) {
                        if (!menuReady) {
                            CategoryRow(
                                "Save preferences enabled.\n" +
                                "Waiting for game lib to load…\n\n" +
                                "Force load may not apply mods instantly. " +
                                "Reactivate features after force loading."
                            )
                            Spacer(Modifier.height(6.dp))
                            ButtonRow(
                                name            = "Force Load Menu",
                                featNum         = -100,
                                onSpecialAction = { stopChecking = true },
                                onCloseSettings = {},
                            )
                        } else {
                            val items = if (isSettingsOpen) settingItems else featureItems
                            items.forEach { item ->
                                FeatureRow(
                                    item             = item,
                                    overlayRequired  = overlayRequired,
                                    isScrollExpanded = isScrollExpanded,
                                    onCloseSettings  = { isSettingsOpen = false },
                                    onForceLoad      = { stopChecking = true },
                                )
                            }
                        }
                    }
                }

                // ── Close button ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CHeaderBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    TextButton(
                        onClick  = { isMenuExpanded = false },
                        colors   = ButtonDefaults.textButtonColors(contentColor = CToggleOn),
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Text(
                            "✕  CLOSE",
                            fontWeight    = FontWeight.Bold,
                            fontSize      = 12.sp,
                            letterSpacing = 1.sp,
                        )
                    }
                }
            }
        }

    }
    } // MaterialTheme
}

// ─────────────────────────────────────────────────────────────────────────────
//  Feature row dispatcher
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FeatureRow(
    item: FeatureItem,
    overlayRequired: Boolean,
    isScrollExpanded: MutableState<Boolean>,
    onCloseSettings: () -> Unit,
    onForceLoad: () -> Unit,
) {
    when (item) {
        is FeatureItem.Toggle           -> ToggleRow(item, isScrollExpanded)
        is FeatureItem.SeekBarItem      -> SeekBarRow(item)
        is FeatureItem.ButtonItem       -> ButtonRow(
            name            = item.name,
            featNum         = item.featNum,
            onSpecialAction = onForceLoad,
            onCloseSettings = onCloseSettings,
        )
        is FeatureItem.ButtonOnOff      -> ButtonOnOffRow(item)
        is FeatureItem.SpinnerItem      -> SpinnerRow(item)
        is FeatureItem.InputText        -> InputTextRow(item, overlayRequired)
        is FeatureItem.InputValue       -> InputValueRow(item, overlayRequired)
        is FeatureItem.CheckBoxItem     -> CheckBoxRow(item)
        is FeatureItem.RadioButtonGroup -> RadioButtonRow(item)
        is FeatureItem.CollapseSection  -> CollapseSectionRow(
            item             = item,
            overlayRequired  = overlayRequired,
            isScrollExpanded = isScrollExpanded,
            onCloseSettings  = onCloseSettings,
            onForceLoad      = onForceLoad,
        )
        is FeatureItem.ButtonLink    -> ButtonLinkRow(item)
        is FeatureItem.CategoryLabel -> CategoryRow(item.text)
        is FeatureItem.RichTextLabel -> RichTextRow(item.text)
        is FeatureItem.RichWebLabel  -> RichWebRow(item.text)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Toggle (Switch)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ToggleRow(
    item: FeatureItem.Toggle,
    isScrollExpanded: MutableState<Boolean>,
) {
    var checked by remember {
        mutableStateOf(Preferences.loadPrefBool(item.name, item.featNum, item.defaultOn))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (checked) CBtnOn.copy(alpha = 0.18f) else Color.Transparent)
            .padding(start = 12.dp, top = 3.dp, bottom = 3.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Green left-edge indicator when ON
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (checked) CToggleOn else Color.Transparent)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text     = item.name,
            color    = if (checked) CText else CTextSub,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked         = checked,
            onCheckedChange = { value ->
                checked = value
                when (item.featNum) {
                    -1 -> {
                        Preferences.with(Preferences.context!!).writeBoolean(-1, value)
                        if (!value) Preferences.with(Preferences.context!!).clear()
                    }
                    -3 -> {
                        Preferences.isExpanded = value
                        isScrollExpanded.value = value
                        Preferences.changeFeatureBool(item.name, item.featNum, value)
                    }
                    else -> Preferences.changeFeatureBool(item.name, item.featNum, value)
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor   = CToggleOn,
                checkedTrackColor   = CToggleOn.copy(alpha = 0.30f),
                uncheckedThumbColor = CToggleOff,
                uncheckedTrackColor = CToggleOff.copy(alpha = 0.30f),
            ),
            modifier = Modifier.padding(end = 4.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SeekBar → Slider
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SeekBarRow(item: FeatureItem.SeekBarItem) {
    val loaded = Preferences.loadPrefInt(item.name, item.featNum)
    var value  by remember { mutableStateOf((if (loaded == 0) item.min else loaded).toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 6.dp, bottom = 4.dp, end = 10.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(item.name, color = CTextSub, fontSize = 13.sp)
            Text(
                text       = value.toInt().toString(),
                color      = CToggleOn,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Slider(
            value         = value,
            onValueChange = { v ->
                value = v.coerceAtLeast(item.min.toFloat())
                Preferences.changeFeatureInt(item.name, item.featNum, value.toInt())
            },
            valueRange = item.min.toFloat()..item.max.toFloat(),
            colors     = SliderDefaults.colors(
                thumbColor         = CToggleOn,
                activeTrackColor   = CToggleOn,
                inactiveTrackColor = CToggleOn.copy(alpha = 0.20f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Button
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ButtonRow(
    name: String,
    featNum: Int,
    onSpecialAction: () -> Unit = {},
    onCloseSettings: () -> Unit = {},
) {
    OutlinedButton(
        onClick = {
            when (featNum) {
                -6   -> onCloseSettings()
                -100 -> onSpecialAction()
            }
            Preferences.changeFeatureInt(name, featNum, 0)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CTextSub),
        border = BorderStroke(1.dp, CBorder.copy(alpha = 0.5f)),
        shape  = RoundedCornerShape(6.dp),
    ) {
        Text(text = name, fontSize = 13.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ButtonOnOff — animated ON/OFF toggle button
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ButtonOnOffRow(item: FeatureItem.ButtonOnOff) {
    var isOn by remember {
        mutableStateOf(Preferences.loadPrefBool(item.name, item.featNum, item.defaultOn))
    }
    val animColor by animateColorAsState(
        targetValue   = if (isOn) CToggleOn.copy(alpha = 0.20f) else CBtnOff,
        animationSpec = tween(200),
        label         = "btnOnOffColor",
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isOn) CToggleOn else CBorder.copy(alpha = 0.3f),
        animationSpec = tween(200),
        label         = "btnOnOffBorder",
    )
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.96f else 1f,
        animationSpec = tween(80),
        label         = "btnScale",
    )

    Button(
        onClick = {
            pressed = true
            isOn    = !isOn
            Preferences.changeFeatureBool(item.name, item.featNum, isOn)
            pressed = false
        },
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .scale(scale),
        colors    = ButtonDefaults.buttonColors(containerColor = animColor),
        border    = BorderStroke(1.dp, borderColor),
        shape     = RoundedCornerShape(6.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp),
    ) {
        Text(
            text       = "${item.name}: ${if (isOn) "ON" else "OFF"}",
            color      = if (isOn) CToggleOn else CTextSub,
            fontSize   = 13.sp,
            fontWeight = if (isOn) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Spinner → ExposedDropdownMenuBox
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpinnerRow(item: FeatureItem.SpinnerItem) {
    var selectedIndex by remember {
        mutableStateOf(Preferences.loadPrefInt(item.name, item.featNum))
    }
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(
            text     = item.name,
            color    = CTextSub,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
        )
        ExposedDropdownMenuBox(
            expanded         = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value         = item.options.getOrElse(selectedIndex) { "" },
                onValueChange = {},
                readOnly      = true,
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedTextColor        = CText,
                    unfocusedTextColor      = CTextSub,
                    focusedBorderColor      = CToggleOn,
                    unfocusedBorderColor    = CBtnBg,
                    focusedContainerColor   = CBtnBg,
                    unfocusedContainerColor = CBtnBg,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false },
            ) {
                item.options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text    = { Text(option, color = CTextSub) },
                        onClick = {
                            selectedIndex = index
                            expanded      = false
                            Preferences.changeFeatureInt(option, item.featNum, index)
                        },
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  InputValue — number input with AlertDialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun InputValueRow(item: FeatureItem.InputValue, overlayRequired: Boolean) {
    val loaded     = Preferences.loadPrefInt(item.name, item.featNum)
    var current    by remember { mutableStateOf(if (loaded == 0) 1 else loaded) }
    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick  = { showDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CTextSub),
        border = BorderStroke(1.dp, CBorder.copy(alpha = 0.4f)),
        shape  = RoundedCornerShape(6.dp),
    ) {
        Text("${item.name}: $current", fontSize = 13.sp)
    }

    if (showDialog) {
        NumberInputDialog(
            hint            = if (item.maxValue != 0) "Max: ${item.maxValue}" else "",
            overlayRequired = overlayRequired,
            onConfirm       = { raw ->
                val num = try {
                    val p = if (raw.isBlank()) 0 else raw.toInt()
                    if (item.maxValue != 0 && p >= item.maxValue) item.maxValue else p
                } catch (_: NumberFormatException) {
                    if (item.maxValue != 0) item.maxValue else Int.MAX_VALUE - 7
                }
                current = num
                Preferences.changeFeatureInt(item.name, item.featNum, num)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  InputText — string input with AlertDialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun InputTextRow(item: FeatureItem.InputText, overlayRequired: Boolean) {
    var current    by remember { mutableStateOf(Preferences.loadPrefString(item.name, item.featNum)) }
    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick  = { showDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CTextSub),
        border = BorderStroke(1.dp, CBorder.copy(alpha = 0.4f)),
        shape  = RoundedCornerShape(6.dp),
    ) {
        Text("${item.name}: $current", fontSize = 13.sp)
    }

    if (showDialog) {
        TextInputDialog(
            overlayRequired = overlayRequired,
            onConfirm       = { str ->
                current = str
                Preferences.changeFeatureString(item.name, item.featNum, str)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CheckBox
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CheckBoxRow(item: FeatureItem.CheckBoxItem) {
    var checked by remember {
        mutableStateOf(Preferences.loadPrefBool(item.name, item.featNum, item.defaultOn))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                checked = !checked
                Preferences.changeFeatureBool(item.name, item.featNum, checked)
            }
            .background(if (checked) CBtnOn.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked         = checked,
            onCheckedChange = { value ->
                checked = value
                Preferences.changeFeatureBool(item.name, item.featNum, value)
            },
            colors = CheckboxDefaults.colors(
                checkedColor   = CToggleOn,
                uncheckedColor = CTextSub,
                checkmarkColor = Color.Black,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text     = item.name,
            color    = if (checked) CText else CTextSub,
            fontSize = 13.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  RadioButton group
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RadioButtonRow(item: FeatureItem.RadioButtonGroup) {
    var selectedIndex by remember {
        mutableStateOf(Preferences.loadPrefInt(item.name, item.featNum))
    }
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        val label = item.options.getOrElse(selectedIndex) { "" }
        Text(
            text       = if (label.isNotEmpty()) "${item.name}: $label" else "${item.name}:",
            color      = CTextSub,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        item.options.forEachIndexed { index, option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedIndex = index
                        Preferences.changeFeatureInt(item.name, item.featNum, index)
                    }
            ) {
                RadioButton(
                    selected = selectedIndex == index,
                    onClick  = {
                        selectedIndex = index
                        Preferences.changeFeatureInt(item.name, item.featNum, index)
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = CToggleOn),
                )
                Text(text = option, color = CTextSub, fontSize = 13.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Collapse section
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CollapseSectionRow(
    item: FeatureItem.CollapseSection,
    overlayRequired: Boolean,
    isScrollExpanded: MutableState<Boolean>,
    onCloseSettings: () -> Unit,
    onForceLoad: () -> Unit,
) {
    var isOpen by remember { mutableStateOf(item.startExpanded) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(CCategoryBg)
                .clickable { isOpen = !isOpen }
                .padding(vertical = 12.dp),
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text  = if (isOpen) "▲ " else "▼ ",
                    color = CToggleOn, fontSize = 11.sp,
                )
                Text(
                    text       = item.text,
                    color      = CText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                )
            }
        }
        AnimatedVisibility(visible = isOpen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CCollapseBg)
                    .padding(vertical = 4.dp)
            ) {
                item.children.forEach { child ->
                    FeatureRow(
                        item             = child,
                        overlayRequired  = overlayRequired,
                        isScrollExpanded = isScrollExpanded,
                        onCloseSettings  = onCloseSettings,
                        onForceLoad      = onForceLoad,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ButtonLink
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ButtonLinkRow(item: FeatureItem.ButtonLink) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(item.url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CToggleOn),
        border = BorderStroke(1.dp, CToggleOn.copy(alpha = 0.6f)),
        shape  = RoundedCornerShape(6.dp),
    ) {
        Text("🔗 ${item.name}", fontSize = 13.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Category header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CCategoryBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(CToggleOn)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text       = text,
            color      = CText,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 12.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  RichText plain label
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RichTextRow(text: String) {
    Text(
        text     = text,
        color    = CTextSub,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  RichWebView — HTML rendered via AndroidView + WebView
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RichWebRow(html: String) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(0x00000000)
                setPadding(0, 5, 0, 5)
                loadData(html, "text/html", "utf-8")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Dialog helpers — set TYPE_APPLICATION_OVERLAY when in overlay mode
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OverlayAwareDialog(
    overlayRequired: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        if (overlayRequired) {
            val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
            LaunchedEffect(Unit) {
                @Suppress("DEPRECATION")
                dialogWindow?.setType(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        WindowManager.LayoutParams.TYPE_PHONE
                )
            }
        }
        content()
    }
}

@Composable
private fun NumberInputDialog(
    hint: String,
    overlayRequired: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    OverlayAwareDialog(overlayRequired = overlayRequired, onDismiss = onDismiss) {
        Surface(
            shape    = RoundedCornerShape(14.dp),
            color    = CMenuBg,
            border   = BorderStroke(1.dp, CBorder.copy(alpha = 0.6f)),
            modifier = Modifier.padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Enter number", color = CText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value           = text,
                    onValueChange   = { text = it },
                    placeholder     = {
                        if (hint.isNotEmpty())
                            Text(hint, color = CTextSub.copy(alpha = 0.55f), fontSize = 12.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine      = true,
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedTextColor     = CText,
                        unfocusedTextColor   = CText,
                        focusedBorderColor   = CToggleOn,
                        unfocusedBorderColor = CTextSub.copy(alpha = 0.30f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier              = Modifier.fillMaxWidth(),
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = CTextSub) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(text) },
                        colors  = ButtonDefaults.buttonColors(containerColor = CToggleOn),
                        shape   = RoundedCornerShape(8.dp),
                    ) {
                        Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TextInputDialog(
    overlayRequired: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    OverlayAwareDialog(overlayRequired = overlayRequired, onDismiss = onDismiss) {
        Surface(
            shape    = RoundedCornerShape(14.dp),
            color    = CMenuBg,
            border   = BorderStroke(1.dp, CBorder.copy(alpha = 0.6f)),
            modifier = Modifier.padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Enter text", color = CText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value         = text,
                    onValueChange = { text = it },
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedTextColor     = CText,
                        unfocusedTextColor   = CText,
                        focusedBorderColor   = CToggleOn,
                        unfocusedBorderColor = CTextSub.copy(alpha = 0.30f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier              = Modifier.fillMaxWidth(),
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = CTextSub) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(text) },
                        colors  = ButtonDefaults.buttonColors(containerColor = CToggleOn),
                        shape   = RoundedCornerShape(8.dp),
                    ) {
                        Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
