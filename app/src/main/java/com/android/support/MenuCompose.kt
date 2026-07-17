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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.delay
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
//  ImGui-inspired colour palette
//  Flat, matte, dark — no gradients, no shadows, no Material bloat
// ─────────────────────────────────────────────────────────────────────────────
private val IBg          = Color(0xFF1A1A1A)   // main window bg
private val ITitleBg     = Color(0xFF242424)   // title bar
private val IFrameBg     = Color(0xFF0F0F0F)   // inset / frame background
private val IChildBg     = Color(0xFF161616)   // scroll area bg
private val IBorderMute  = Color(0xFF3A3A3A)   // neutral 1px borders
private val IText        = Color(0xFFDEDEDE)   // primary text
private val ITextDim     = Color(0xFF888888)   // secondary / dimmed text
private val IAccent      = Color(0xFF32CB00)   // green accent
private val IAccentDark  = Color(0xFF1B4500)   // dark green fill for ON buttons
private val ISep         = Color(0xFF2A2A2A)   // separator lines
private val ICatBg       = Color(0xFF1F1F1F)   // category row bg
private val IColBg       = Color(0xFF141414)   // collapse child bg
private val IButtonBg    = Color(0xFF252525)   // button face

private val MenuWidth  = 285.dp
private val MenuRadius = RoundedCornerShape(0.dp)   // ImGui is square

// ─────────────────────────────────────────────────────────────────────────────
//  Gesture tracking (plain class — mutations never trigger recomposition)
// ─────────────────────────────────────────────────────────────────────────────
private class GestureState {
    var startRawX  = 0f ; var startRawY  = 0f
    var initWinX   = 0f ; var initWinY   = 0f
    var isDragging = false
}

// ─────────────────────────────────────────────────────────────────────────────
//  Reusable clickable with NO ripple (ImGui feel)
// ─────────────────────────────────────────────────────────────────────────────
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = MutableInteractionSource(),
        indication        = null,
        onClick           = onClick,
    )

// ─────────────────────────────────────────────────────────────────────────────
//  Small ImGui-style checkbox  (replaces Material Switch)
//  A 15×15dp bordered square: empty when OFF, filled + "✓" when ON
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ImGuiCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(15.dp)
            .background(if (checked) IAccent else IFrameBg)
            .border(1.dp, if (checked) IAccent else IBorderMute)
            .noRippleClickable { onCheckedChange(!checked) },
    ) {
        if (checked) {
            Text(
                text       = "✓",
                color      = Color.Black,
                fontSize   = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 9.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Thin horizontal separator line
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ImGuiSeparator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ISep)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Root floating-overlay composable
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
fun FloatingMenu(
    overlay         : Menu,
    windowManager   : WindowManager,
    vmParams        : WindowManager.LayoutParams,
    composeView     : View,
    title           : String,   // still accepted (used for marquee subtitle fallback)
    subTitle        : String,
    overlayRequired : Boolean,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var menuAlpha      by remember { mutableStateOf(1f) }

    var posX by remember { mutableStateOf(vmParams.x.toFloat()) }
    var posY by remember { mutableStateOf(vmParams.y.toFloat()) }

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

    val iconGesture  = remember { GestureState() }
    val panelGesture = remember { GestureState() }

    fun applyDrag(state: GestureState, rawX: Float, rawY: Float) {
        posX = state.initWinX + (rawX - state.startRawX)
        posY = state.initWinY + (rawY - state.startRawY)
        vmParams.x = posX.toInt()
        vmParams.y = posY.toInt()
        windowManager.updateViewLayout(composeView, vmParams)
    }

    MaterialTheme {
    Box {

        // ── Floating icon (collapsed) ─────────────────────────────────────
        AnimatedVisibility(
            visible = !isMenuExpanded,
            enter   = fadeIn(tween(120)),
            exit    = fadeOut(tween(100)),
        ) {
            // Square icon, ImGui style: dark fill, green 1px border
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .alpha(menuAlpha)
                    .background(IBg)
                    .border(1.dp, IAccent)
                    .pointerInteropFilter { event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                iconGesture.startRawX  = event.rawX
                                iconGesture.startRawY  = event.rawY
                                iconGesture.initWinX   = posX
                                iconGesture.initWinY   = posY
                                iconGesture.isDragging = false
                                true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val dx = abs(event.rawX - iconGesture.startRawX)
                                val dy = abs(event.rawY - iconGesture.startRawY)
                                if (iconGesture.isDragging || dx > 10f || dy > 10f) {
                                    iconGesture.isDragging = true
                                    menuAlpha = 0.50f
                                    applyDrag(iconGesture, event.rawX, event.rawY)
                                }
                                true
                            }
                            MotionEvent.ACTION_UP -> {
                                menuAlpha = 1f
                                if (!iconGesture.isDragging) isMenuExpanded = true
                                iconGesture.isDragging = false
                                true
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                menuAlpha = 1f ; iconGesture.isDragging = false ; true
                            }
                            else -> false
                        }
                    }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // "CX" — abbreviated "Cler X"
                    Text(
                        text          = "CX",
                        color         = IAccent,
                        fontSize      = 18.sp,
                        fontWeight    = FontWeight.Bold,
                        fontFamily    = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    // Three pixel dots — drag indicator
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(3) {
                            Box(
                                Modifier
                                    .size(3.dp)
                                    .background(IAccent.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }

        // ── Expanded menu panel ───────────────────────────────────────────
        AnimatedVisibility(
            visible = isMenuExpanded,
            enter   = fadeIn(tween(120)) + expandVertically(tween(150)),
            exit    = fadeOut(tween(100)) + shrinkVertically(tween(120)),
        ) {
            Column(
                modifier = Modifier
                    .width(MenuWidth)
                    .wrapContentHeight()
                    .alpha(menuAlpha)
                    .background(IBg)
                    .border(1.dp, IBorderMute)
            ) {

                // ── Title bar ─────────────────────────────────────────────
                // Row: [drag area (weight=1): grip + "Cler X"] | [gear]
                // Gear is a sibling — outside the drag scope entirely.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ITitleBg)
                        .height(34.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Drag area with title — claims the gesture stream on DOWN
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pointerInteropFilter { event ->
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        panelGesture.startRawX  = event.rawX
                                        panelGesture.startRawY  = event.rawY
                                        panelGesture.initWinX   = posX
                                        panelGesture.initWinY   = posY
                                        panelGesture.isDragging = false
                                        true
                                    }
                                    MotionEvent.ACTION_MOVE -> {
                                        val dx = abs(event.rawX - panelGesture.startRawX)
                                        val dy = abs(event.rawY - panelGesture.startRawY)
                                        if (panelGesture.isDragging || dx > 6f || dy > 6f) {
                                            panelGesture.isDragging = true
                                            menuAlpha = 0.50f
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
                            },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Grip dots (2×3 grid)
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                repeat(2) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        repeat(3) {
                                            Box(
                                                Modifier
                                                    .size(2.dp)
                                                    .background(ITextDim)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.width(7.dp))
                            // "Cler X" — hardcoded per user request
                            Text(
                                text          = "Cler X",
                                color         = IAccent,
                                fontSize      = 13.sp,
                                fontWeight    = FontWeight.Bold,
                                fontFamily    = FontFamily.Monospace,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }

                    // Gear icon — independent sibling, never inside the drag scope
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(34.dp)
                            .noRippleClickable { isSettingsOpen = !isSettingsOpen }
                    ) {
                        Text(
                            text     = "⚙",
                            color    = if (isSettingsOpen) IAccent else ITextDim,
                            fontSize = 16.sp,
                        )
                    }
                }

                // Green 1px accent line under title
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(IAccent)
                )

                // Subtitle marquee
                AndroidView(
                    factory = { ctx ->
                        android.widget.TextView(ctx).apply {
                            ellipsize          = android.text.TextUtils.TruncateAt.MARQUEE
                            marqueeRepeatLimit = -1
                            isSingleLine       = true
                            isSelected         = true
                            textSize           = 9.5f
                            gravity            = android.view.Gravity.START
                            setPadding(10, 3, 10, 3)
                            setTextColor(android.graphics.Color.argb(180, 100, 100, 100))
                            setBackgroundColor(android.graphics.Color.parseColor("#1A1A1A"))
                            typeface = android.graphics.Typeface.MONOSPACE
                            text = subTitle.ifBlank { title }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                )

                ImGuiSeparator()

                // ── Feature / settings scroll area ────────────────────────
                val scrollMod = if (isScrollExpanded.value)
                    Modifier.weight(1f)
                else
                    Modifier.height(224.dp)

                Box(modifier = scrollMod.background(IChildBg)) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        if (!menuReady) {
                            IGuiCategoryRow(
                                "Waiting for game lib…\n" +
                                "Force load may not apply mods instantly."
                            )
                            ImGuiSeparator()
                            IGuiButtonRow(
                                name            = "Force Load Menu",
                                featNum         = -100,
                                onSpecialAction = { stopChecking = true },
                            )
                        } else {
                            val items = if (isSettingsOpen) settingItems else featureItems
                            items.forEach { item ->
                                IGuiFeatureRow(
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

                ImGuiSeparator()

                // ── Close bar ─────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ITitleBg)
                        .noRippleClickable { isMenuExpanded = false }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text          = "[ close ]",
                        color         = ITextDim,
                        fontSize      = 11.sp,
                        fontFamily    = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                    )
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
private fun IGuiFeatureRow(
    item             : FeatureItem,
    overlayRequired  : Boolean,
    isScrollExpanded : MutableState<Boolean>,
    onCloseSettings  : () -> Unit,
    onForceLoad      : () -> Unit,
) {
    when (item) {
        is FeatureItem.Toggle           -> IGuiToggleRow(item, isScrollExpanded)
        is FeatureItem.SeekBarItem      -> IGuiSeekBarRow(item)
        is FeatureItem.ButtonItem       -> IGuiButtonRow(
            name            = item.name,
            featNum         = item.featNum,
            onSpecialAction = onForceLoad,
            onCloseSettings = onCloseSettings,
        )
        is FeatureItem.ButtonOnOff      -> IGuiButtonOnOffRow(item)
        is FeatureItem.SpinnerItem      -> IGuiSpinnerRow(item)
        is FeatureItem.InputText        -> IGuiInputTextRow(item, overlayRequired)
        is FeatureItem.InputValue       -> IGuiInputValueRow(item, overlayRequired)
        is FeatureItem.CheckBoxItem     -> IGuiCheckBoxRow(item)
        is FeatureItem.RadioButtonGroup -> IGuiRadioButtonRow(item)
        is FeatureItem.CollapseSection  -> IGuiCollapseSectionRow(
            item             = item,
            overlayRequired  = overlayRequired,
            isScrollExpanded = isScrollExpanded,
            onCloseSettings  = onCloseSettings,
            onForceLoad      = onForceLoad,
        )
        is FeatureItem.ButtonLink    -> IGuiButtonLinkRow(item)
        is FeatureItem.CategoryLabel -> IGuiCategoryRow(item.text)
        is FeatureItem.RichTextLabel -> IGuiRichTextRow(item.text)
        is FeatureItem.RichWebLabel  -> IGuiRichWebRow(item.text)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Toggle row — ImGui checkbox + label
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IGuiToggleRow(
    item: FeatureItem.Toggle,
    isScrollExpanded: MutableState<Boolean>,
) {
    var checked by remember {
        mutableStateOf(Preferences.loadPrefBool(item.name, item.featNum, item.defaultOn))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable {
                val next = !checked
                checked = next
                when (item.featNum) {
                    -1 -> {
                        Preferences.with(Preferences.context!!).writeBoolean(-1, next)
                        if (!next) Preferences.with(Preferences.context!!).clear()
                    }
                    -3 -> {
                        Preferences.isExpanded = next
                        isScrollExpanded.value = next
                        Preferences.changeFeatureBool(item.name, item.featNum, next)
                    }
                    else -> Preferences.changeFeatureBool(item.name, item.featNum, next)
                }
            }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImGuiCheckbox(checked = checked, onCheckedChange = { next ->
            checked = next
            when (item.featNum) {
                -1 -> {
                    Preferences.with(Preferences.context!!).writeBoolean(-1, next)
                    if (!next) Preferences.with(Preferences.context!!).clear()
                }
                -3 -> {
                    Preferences.isExpanded = next
                    isScrollExpanded.value = next
                    Preferences.changeFeatureBool(item.name, item.featNum, next)
                }
                else -> Preferences.changeFeatureBool(item.name, item.featNum, next)
            }
        })
        Spacer(Modifier.width(8.dp))
        Text(
            text     = item.name,
            color    = if (checked) IText else ITextDim,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
        )
        if (checked) {
            Text(
                text       = "ON",
                color      = IAccent,
                fontSize   = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    ImGuiSeparator()
}

// ─────────────────────────────────────────────────────────────────────────────
//  SeekBar → flat Slider
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IGuiSeekBarRow(item: FeatureItem.SeekBarItem) {
    val loaded = Preferences.loadPrefInt(item.name, item.featNum)
    var value  by remember { mutableStateOf((if (loaded == 0) item.min else loaded).toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 2.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(item.name, color = ITextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(
                text       = value.toInt().toString(),
                color      = IAccent,
                fontSize   = 11.sp,
                fontFamily = FontFamily.Monospace,
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
                thumbColor         = IAccent,
                activeTrackColor   = IAccent,
                inactiveTrackColor = IBorderMute,
            ),
            modifier = Modifier.fillMaxWidth().height(28.dp),
        )
    }
    ImGuiSeparator()
}

// ─────────────────────────────────────────────────────────────────────────────
//  Button — flat, 1px border, monospace label
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IGuiButtonRow(
    name            : String,
    featNum         : Int,
    onSpecialAction : () -> Unit = {},
    onCloseSettings : () -> Unit = {},
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .background(IButtonBg)
            .border(1.dp, IBorderMute)
            .noRippleClickable {
                when (featNum) {
                    -6   -> onCloseSettings()
                    -100 -> onSpecialAction()
                }
                Preferences.changeFeatureInt(name, featNum, 0)
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text       = name,
            color      = ITextDim,
            fontSize   = 12.sp,
            fontFamily = FontFamily.Monospace,
            textAlign  = TextAlign.Center,
        )
    }
    ImGuiSeparator()
}

// ─────────────────────────────────────────────────────────────────────────────
//  ButtonOnOff — accent border when ON, muted when OFF
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IGuiButtonOnOffRow(item: FeatureItem.ButtonOnOff) {
    var isOn by remember {
        mutableStateOf(Preferences.loadPrefBool(item.name, item.featNum, item.defaultOn))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .background(if (isOn) IAccentDark else IButtonBg)
            .border(1.dp, if (isOn) IAccent else IBorderMute)
            .noRippleClickable {
                isOn = !isOn
                Preferences.changeFeatureBool(item.name, item.featNum, isOn)
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text       = item.name,
            color      = if (isOn) IText else ITextDim,
            fontSize   = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier   = Modifier.weight(1f),
        )
        Text(
            text       = if (isOn) "ON " else "OFF",
            color      = if (isOn) IAccent else ITextDim,
            fontSize   = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isOn) FontWeight.Bold else FontWeight.Normal,
        )
    }
    ImGuiSeparator()
}

// ─────────────────────────────────────────────────────────────────────────────
//  Spinner → simple dropdown
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IGuiSpinnerRow(item: FeatureItem.SpinnerItem) {
    var selectedIndex by remember {
        mutableStateOf(Preferences.loadPrefInt(item.name, item.featNum))
    }
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(item.name, color = ITextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 2.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value         = item.options.getOrElse(selectedIndex) { "" },
                onValueChange = {},
                readOnly      = true,
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedTextColor        = IText,
                    unfocusedTextColor      = ITextDim,
                    focusedBorderColor      = IAccent,
                    unfocusedBorderColor    = IBorderMute,
                    focusedContainerColor   = IFrameBg,
                    unfocusedContainerColor = IFrameBg,
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize   = 12.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                item.options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text    = { Text(option, color = ITextDim, fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace) },
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
    ImGuiSeparator()
}

// ─────────────────────────────────────────────────────────────────────────────
//  InputValue — number input
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IGuiInputValueRow(item: FeatureItem.InputValue, overlayRequired: Boolean) {
    val loaded     = Preferences.loadPrefInt(item.name, item.featNum)
    var current    by remember { mutableStateOf(if (loaded == 0) 1 else loaded) }
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .background(IButtonBg)
            .border(1.dp, IBorderMute)
            .noRippleClickable { showDialog = true }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(item.name, color = ITextDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f))
        Text("$current", color = IAccent, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold)
    }
    ImGuiSeparator()

    if (showDialog) {
        IGuiNumberInputDialog(
            hint            = if (item.maxValue != 0) "max: ${item.maxValue}" else "",
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
//  InputText — string input
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IGuiInputTextRow(item: FeatureItem.InputText, overlayRequired: Boolean) {
    var current    by remember { mutableStateOf(Preferences.loadPrefString(item.name, item.featNum)) }
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .background(IButtonBg)
            .border(1.dp, IBorderMute)
            .noRippleClickable { showDialog = true }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(item.name, color = ITextDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f))
        Text(
            text     = current.take(12),
            color    = IAccent,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
    ImGuiSeparator()

    if (showDialog) {
        IGuiTextInputDialog(
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
//  CheckBox row (uses ImGuiCheckbox)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IGuiCheckBoxRow(item: FeatureItem.CheckBoxItem) {
    var checked by remember {
        mutableStateOf(Preferences.loadPrefBool(item.name, item.featNum, item.defaultOn))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable {
                checked = !checked
                Preferences.changeFeatureBool(item.name, item.featNum, checked)
            }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImGuiCheckbox(checked = checked, onCheckedChange = { v ->
            checked = v
            Preferences.changeFeatureBool(item.name, item.featNum, v)
        })
        Spacer(Modifier.width(8.dp))
        Text(
            text       = item.name,
            color      = if (checked) IText else ITextDim,
            fontSize   = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
    ImGuiSeparator()
}

// ─────────────────────────────────────────────────────────────────────────────
//  RadioButton group — ImGui-style filled dot
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IGuiRadioButtonRow(item: FeatureItem.RadioButtonGroup) {
    var selectedIndex by remember {
        mutableStateOf(Preferences.loadPrefInt(item.name, item.featNum))
    }
    Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 2.dp)) {
        Text(
            text       = item.name,
            color      = ITextDim,
            fontSize   = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(bottom = 3.dp),
        )
        item.options.forEachIndexed { index, option ->
            val sel = selectedIndex == index
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .noRippleClickable {
                        selectedIndex = index
                        Preferences.changeFeatureInt(item.name, item.featNum, index)
                    }
                    .padding(vertical = 3.dp)
            ) {
                // Small circle dot
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(13.dp)
                        .border(1.dp, if (sel) IAccent else IBorderMute)
                        .background(if (sel) IAccent else IFrameBg)
                ) {
                    if (sel) {
                        Box(Modifier.size(5.dp).background(Color.Black))
                    }
                }
                Spacer(Modifier.width(7.dp))
                Text(option, color = if (sel) IText else ITextDim, fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace)
            }
        }
    }
    ImGuiSeparator()
}

// ─────────────────────────────────────────────────────────────────────────────
//  Collapse section — ImGui CollapsingHeader style
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IGuiCollapseSectionRow(
    item             : FeatureItem.CollapseSection,
    overlayRequired  : Boolean,
    isScrollExpanded : MutableState<Boolean>,
    onCloseSettings  : () -> Unit,
    onForceLoad      : () -> Unit,
) {
    var isOpen by remember { mutableStateOf(item.startExpanded) }

    // Header: left-aligned caret + text, no gradient, slight bg shift
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ITitleBg)
            .noRippleClickable { isOpen = !isOpen }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text       = if (isOpen) "▾" else "▸",
            color      = IAccent,
            fontSize   = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text       = item.text,
            color      = IText,
            fontSize   = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
        )
    }
    ImGuiSeparator()

    AnimatedVisibility(visible = isOpen) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(IColBg)
        ) {
            item.children.forEach { child ->
                IGuiFeatureRow(
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

// ─────────────────────────────────────────────────────────────────────────────
//  ButtonLink
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IGuiButtonLinkRow(item: FeatureItem.ButtonLink) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .background(IButtonBg)
            .border(1.dp, IAccent.copy(alpha = 0.35f))
            .noRippleClickable {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(item.url)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(item.name, color = IAccent, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text("→", color = IAccent, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
    ImGuiSeparator()
}

// ─────────────────────────────────────────────────────────────────────────────
//  Category header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IGuiCategoryRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ICatBg)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(2.dp).height(11.dp).background(IAccent))
        Spacer(Modifier.width(6.dp))
        Text(
            text       = text.uppercase(),
            color      = IAccent,
            fontSize   = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
    }
    ImGuiSeparator()
}

// ─────────────────────────────────────────────────────────────────────────────
//  Rich text label
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IGuiRichTextRow(text: String) {
    Text(
        text       = text,
        color      = ITextDim,
        fontSize   = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
    ImGuiSeparator()
}

// ─────────────────────────────────────────────────────────────────────────────
//  Rich WebView label
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IGuiRichWebRow(html: String) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(0x00000000)
                setPadding(8, 4, 8, 4)
                loadData(html, "text/html", "utf-8")
            }
        },
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
    )
    ImGuiSeparator()
}

// ─────────────────────────────────────────────────────────────────────────────
//  Dialog helpers
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OverlayAwareDialog(
    overlayRequired : Boolean,
    onDismiss       : () -> Unit,
    content         : @Composable () -> Unit,
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
private fun IGuiNumberInputDialog(
    hint            : String,
    overlayRequired : Boolean,
    onConfirm       : (String) -> Unit,
    onDismiss       : () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    OverlayAwareDialog(overlayRequired = overlayRequired, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .background(IBg)
                .border(1.dp, IBorderMute)
                .padding(16.dp)
        ) {
            Text("> enter value", color = IAccent, fontSize = 12.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value           = text,
                onValueChange   = { text = it },
                placeholder     = {
                    if (hint.isNotEmpty())
                        Text(hint, color = ITextDim, fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine      = true,
                textStyle       = LocalTextStyle.current.copy(
                    color      = IText,
                    fontSize   = 12.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = IAccent,
                    unfocusedBorderColor    = IBorderMute,
                    focusedContainerColor   = IFrameBg,
                    unfocusedContainerColor = IFrameBg,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(IButtonBg)
                        .border(1.dp, IBorderMute)
                        .noRippleClickable(onDismiss)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) { Text("cancel", color = ITextDim, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace) }
                Spacer(Modifier.width(8.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(IAccentDark)
                        .border(1.dp, IAccent)
                        .noRippleClickable { onConfirm(text) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) { Text("ok", color = IAccent, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun IGuiTextInputDialog(
    overlayRequired : Boolean,
    onConfirm       : (String) -> Unit,
    onDismiss       : () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    OverlayAwareDialog(overlayRequired = overlayRequired, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .background(IBg)
                .border(1.dp, IBorderMute)
                .padding(16.dp)
        ) {
            Text("> enter text", color = IAccent, fontSize = 12.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it },
                singleLine    = true,
                textStyle     = LocalTextStyle.current.copy(
                    color      = IText,
                    fontSize   = 12.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = IAccent,
                    unfocusedBorderColor    = IBorderMute,
                    focusedContainerColor   = IFrameBg,
                    unfocusedContainerColor = IFrameBg,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(IButtonBg)
                        .border(1.dp, IBorderMute)
                        .noRippleClickable(onDismiss)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) { Text("cancel", color = ITextDim, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace) }
                Spacer(Modifier.width(8.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(IAccentDark)
                        .border(1.dp, IAccent)
                        .noRippleClickable { onConfirm(text) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) { Text("ok", color = IAccent, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
