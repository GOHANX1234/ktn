package com.android.support

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.*
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
//  Colour palette — matches the original Java values exactly
// ─────────────────────────────────────────────────────────────────────────────
private val CText        = Color(0xFFFFFFFF)
private val CTextSub     = Color(0xFFE0E0E0)
private val CBtnBg       = Color(0xFF1A1A2E)
private val CMenuBg      = Color(0xCC0F0F1AL)   // semi-transparent dark
private val CFeatureBg   = Color(0xFF1A1A2E)
private val CToggleOn    = Color(0xFF00FFAA)
private val CToggleOff   = Color(0xFF3D3D5C)
private val CBtnOn       = Color(0xFF00ACC1)
private val CBtnOff      = Color(0xFF2C2C44)
private val CCategoryBg  = Color(0xFF252540)
private val CCollapseBg  = Color(0xFF222D38)
private val CBorder      = Color(0xFF32CB00)

private val MenuCorner  = 12.dp
private val MenuWidth   = 290.dp
private val MenuScrollH = 210.dp   // fixed scroll-area height when not "expanded"

// ─────────────────────────────────────────────────────────────────────────────
//  Root composable — the complete floating overlay
// ─────────────────────────────────────────────────────────────────────────────
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
    // ── Local UI state ────────────────────────────────────────────────────
    var isMenuExpanded  by remember { mutableStateOf(false) }
    var isSettingsOpen  by remember { mutableStateOf(false) }
    var menuAlpha       by remember { mutableStateOf(1f) }

    // Window position (mirrors vmParams.x / vmParams.y for drag)
    var posX by remember { mutableStateOf(vmParams.x.toFloat()) }
    var posY by remember { mutableStateOf(vmParams.y.toFloat()) }

    // Feature list loading state
    var featureItems by remember { mutableStateOf<List<FeatureItem>>(emptyList()) }
    var settingItems by remember { mutableStateOf<List<FeatureItem>>(emptyList()) }
    var menuReady    by remember { mutableStateOf(false) }
    var stopChecking by remember { mutableStateOf(false) }

    // Expanded scroll-area preference (special toggle -3)
    val isScrollExpanded = remember { mutableStateOf(Preferences.isExpanded) }

    // Poll until the game lib loads, then populate feature lists
    LaunchedEffect(Unit) {
        if (Preferences.loadPref && !overlay.IsGameLibLoaded()) {
            while (!overlay.IsGameLibLoaded() && !stopChecking) {
                delay(600)
            }
        }
        featureItems = parseFeatureList(overlay.GetFeatureList())
        settingItems = parseFeatureList(overlay.SettingsList())
        menuReady    = true
    }

    // Shared drag-handler: moves the WindowManager window
    fun applyDrag(dx: Float, dy: Float) {
        posX += dx; posY += dy
        vmParams.x = posX.toInt(); vmParams.y = posY.toInt()
        windowManager.updateViewLayout(composeView, vmParams)
    }

    MaterialTheme {
    Box {
        // ── Collapsed "S3" icon ───────────────────────────────────────────
        AnimatedVisibility(
            visible = !isMenuExpanded,
            enter = fadeIn(),
            exit  = fadeOut(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(65.dp)
                    .alpha(0.9f)
                    .clip(CircleShape)
                    .background(CMenuBg)
                    .border(2.dp, CToggleOn, CircleShape)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var startX = down.position.x
                                var startY = down.position.y
                                var initX  = posX
                                var initY  = posY
                                var moved  = false

                                do {
                                    val evt = awaitPointerEvent()
                                    val ch  = evt.changes.firstOrNull() ?: break
                                    val dx  = ch.position.x - startX
                                    val dy  = ch.position.y - startY
                                    if (abs(dx) >= 10f || abs(dy) >= 10f) {
                                        moved   = true
                                        menuAlpha = 0.5f
                                        posX    = initX + dx
                                        posY    = initY + dy
                                        vmParams.x = posX.toInt()
                                        vmParams.y = posY.toInt()
                                        windowManager.updateViewLayout(composeView, vmParams)
                                        ch.consume()
                                    }
                                    if (ch.changedToUp()) {
                                        menuAlpha = 1f
                                        if (!moved) isMenuExpanded = true
                                        break
                                    }
                                } while (true)
                            }
                        }
                    }
            ) {
                Text("S3", color = CText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ── Expanded menu panel ───────────────────────────────────────────
        AnimatedVisibility(
            visible = isMenuExpanded,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .width(MenuWidth)
                    .wrapContentHeight()
                    .alpha(menuAlpha)
                    .clip(RoundedCornerShape(MenuCorner))
                    .background(CMenuBg)
                    .border(1.dp, CBorder, RoundedCornerShape(MenuCorner))
                    // Drag the whole panel by touching anywhere
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var startX = down.position.x
                                var startY = down.position.y
                                var initX  = posX
                                var initY  = posY

                                do {
                                    val evt = awaitPointerEvent()
                                    val ch  = evt.changes.firstOrNull() ?: break
                                    val dx  = ch.position.x - startX
                                    val dy  = ch.position.y - startY
                                    if (abs(dx) >= 5f || abs(dy) >= 5f) {
                                        menuAlpha = 0.5f
                                        posX = initX + dx; posY = initY + dy
                                        vmParams.x = posX.toInt(); vmParams.y = posY.toInt()
                                        windowManager.updateViewLayout(composeView, vmParams)
                                        ch.consume()
                                    }
                                    if (ch.changedToUp()) { menuAlpha = 1f; break }
                                } while (true)
                            }
                        }
                    }
            ) {
                // Title bar
                MenuTitleBar(
                    title          = title,
                    subTitle       = subTitle,
                    isSettingsOpen = isSettingsOpen,
                    onToggleSettings = { isSettingsOpen = !isSettingsOpen },
                )

                // Scrollable feature / settings list
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
                    ) {
                        if (!menuReady) {
                            // Waiting for game lib screen
                            CategoryRow(
                                "Save preferences was been enabled. " +
                                "Waiting for game lib to be loaded...\n\n" +
                                "Force load menu may not apply mods instantly. " +
                                "You would need to reactivate them again"
                            )
                            Spacer(Modifier.height(4.dp))
                            ButtonRow(
                                name             = "Force load menu",
                                featNum          = -100,
                                onSpecialAction  = { stopChecking = true },
                                onCloseSettings  = {},
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

                // Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    TextButton(
                        onClick = { isMenuExpanded = false },
                        colors  = ButtonDefaults.textButtonColors(contentColor = CText),
                    ) {
                        Text("CLOSE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
    } // end MaterialTheme
}

// ─────────────────────────────────────────────────────────────────────────────
//  Title bar (title centred, settings gear at right, marquee subtitle below)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MenuTitleBar(
    title: String,
    subTitle: String,
    isSettingsOpen: Boolean,
    onToggleSettings: () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text       = title,
                color      = CText,
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.align(Alignment.Center),
            )
            // Settings gear — top-right
            Text(
                text     = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) "⚙" else "\uD83D\uDD27",
                color    = CText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(onClick = onToggleSettings),
            )
        }
        // Marquee-scrolling subtitle — reuse a real TextView for compatibility
        AndroidView(
            factory = { ctx ->
                android.widget.TextView(ctx).apply {
                    ellipsize            = android.text.TextUtils.TruncateAt.MARQUEE
                    marqueeRepeatLimit   = -1
                    isSingleLine         = true
                    isSelected           = true
                    textSize             = 10f
                    gravity              = android.view.Gravity.CENTER
                    setPadding(0, 0, 0, 5)
                    setTextColor(android.graphics.Color.WHITE)
                    text = subTitle
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        )
    }
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
        is FeatureItem.Toggle -> ToggleRow(
            item             = item,
            isScrollExpanded = isScrollExpanded,
        )
        is FeatureItem.SeekBarItem     -> SeekBarRow(item)
        is FeatureItem.ButtonItem      -> ButtonRow(
            name            = item.name,
            featNum         = item.featNum,
            onSpecialAction = onForceLoad,
            onCloseSettings = onCloseSettings,
        )
        is FeatureItem.ButtonOnOff     -> ButtonOnOffRow(item)
        is FeatureItem.SpinnerItem     -> SpinnerRow(item)
        is FeatureItem.InputText       -> InputTextRow(item, overlayRequired)
        is FeatureItem.InputValue      -> InputValueRow(item, overlayRequired)
        is FeatureItem.CheckBoxItem    -> CheckBoxRow(item)
        is FeatureItem.RadioButtonGroup -> RadioButtonRow(item)
        is FeatureItem.CollapseSection -> CollapseSectionRow(
            item             = item,
            overlayRequired  = overlayRequired,
            isScrollExpanded = isScrollExpanded,
            onCloseSettings  = onCloseSettings,
            onForceLoad      = onForceLoad,
        )
        is FeatureItem.ButtonLink   -> ButtonLinkRow(item)
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
            .padding(start = 10.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = item.name,
            color    = CTextSub,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = { value ->
                checked = value
                when (item.featNum) {
                    -1 -> {
                        // "Save preferences" master switch
                        Preferences.with(Preferences.context!!).writeBoolean(-1, value)
                        if (!value) Preferences.with(Preferences.context!!).clear()
                    }
                    -3 -> {
                        // "Expanded" scroll area toggle
                        Preferences.isExpanded = value
                        isScrollExpanded.value = value
                        Preferences.changeFeatureBool(item.name, item.featNum, value)
                    }
                    else -> Preferences.changeFeatureBool(item.name, item.featNum, value)
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor   = CToggleOn,
                checkedTrackColor   = CToggleOn.copy(alpha = 0.35f),
                uncheckedThumbColor = CToggleOff,
                uncheckedTrackColor = CToggleOff.copy(alpha = 0.35f),
            ),
            modifier = Modifier.padding(end = 6.dp),
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
            .padding(start = 10.dp, top = 4.dp, bottom = 4.dp, end = 8.dp)
    ) {
        Text(
            text     = "${item.name}: ${value.toInt()}",
            color    = CTextSub,
            fontSize = 14.sp,
        )
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
                inactiveTrackColor = CToggleOn.copy(alpha = 0.25f),
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
    Button(
        onClick = {
            when (featNum) {
                -6   -> onCloseSettings()
                -100 -> onSpecialAction()
            }
            Preferences.changeFeatureInt(name, featNum, 0)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 7.dp, vertical = 5.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CBtnBg),
        shape  = RoundedCornerShape(4.dp),
    ) {
        Text(text = name, color = CTextSub, fontSize = 14.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ButtonOnOff — toggle button with animated color + scale
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ButtonOnOffRow(item: FeatureItem.ButtonOnOff) {
    var isOn by remember {
        mutableStateOf(Preferences.loadPrefBool(item.name, item.featNum, item.defaultOn))
    }
    val animColor by animateColorAsState(
        targetValue   = if (isOn) CBtnOn else CBtnOff,
        animationSpec = tween(200),
        label         = "btnOnOffColor",
    )
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.95f else 1f,
        animationSpec = tween(100),
        label         = "btnScale",
    )

    Button(
        onClick = {
            pressed = true
            isOn    = !isOn
            Preferences.changeFeatureBool(item.name, item.featNum, isOn)
            pressed = false
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 7.dp, vertical = 5.dp)
            .scale(scale),
        colors = ButtonDefaults.buttonColors(containerColor = animColor),
        shape  = RoundedCornerShape(4.dp),
    ) {
        Text(
            text     = "${item.name}: ${if (isOn) "ON" else "OFF"}",
            color    = CTextSub,
            fontSize = 14.sp,
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

    Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)) {
        Text(
            text     = item.name,
            color    = CTextSub,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
        )
        ExposedDropdownMenuBox(
            expanded        = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value     = item.options.getOrElse(selectedIndex) { "" },
                onValueChange = {},
                readOnly  = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors    = OutlinedTextFieldDefaults.colors(
                    focusedTextColor        = CTextSub,
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
                expanded        = expanded,
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

    Button(
        onClick  = { showDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 7.dp, vertical = 5.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = CBtnBg),
        shape    = RoundedCornerShape(4.dp),
    ) {
        Text("${item.name}: $current", color = CTextSub, fontSize = 14.sp)
    }

    if (showDialog) {
        NumberInputDialog(
            hint            = if (item.maxValue != 0) "Max value: ${item.maxValue}" else "",
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

    Button(
        onClick  = { showDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 7.dp, vertical = 5.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = CBtnBg),
        shape    = RoundedCornerShape(4.dp),
    ) {
        Text("${item.name}: $current", color = CTextSub, fontSize = 14.sp)
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
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked   = checked,
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
        Text(text = item.name, color = CTextSub, fontSize = 14.sp)
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
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
        val label = item.options.getOrElse(selectedIndex) { "" }
        Text(
            text     = if (label.isNotEmpty()) "${item.name}: $label" else "${item.name}:",
            color    = CTextSub,
            fontSize = 14.sp,
        )
        item.options.forEachIndexed { index, option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
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
                .padding(vertical = 20.dp),
        ) {
            Text(
                text       = if (isOpen) "△ ${item.text} △" else "▽ ${item.text} ▽",
                color      = CTextSub,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                fontSize   = 14.sp,
            )
        }
        AnimatedVisibility(visible = isOpen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CCollapseBg)
                    .padding(vertical = 5.dp)
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
    Button(
        onClick = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(item.url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 7.dp, vertical = 5.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = CBtnBg),
        shape    = RoundedCornerShape(4.dp),
    ) {
        Text(text = item.name, color = CTextSub, fontSize = 14.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Category header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryRow(text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(CCategoryBg)
            .padding(vertical = 5.dp, horizontal = 8.dp),
    ) {
        Text(
            text       = text,
            color      = CTextSub,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            fontSize   = 13.sp,
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
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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

/** Wraps a Dialog and sets the correct window type for overlay mode. */
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
            shape = RoundedCornerShape(12.dp),
            color = CBtnBg,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Input number", color = CText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value         = text,
                    onValueChange = { text = it },
                    placeholder   = {
                        if (hint.isNotEmpty())
                            Text(hint, color = CTextSub.copy(alpha = 0.55f), fontSize = 13.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor     = CText,
                        unfocusedTextColor   = CText,
                        focusedBorderColor   = CToggleOn,
                        unfocusedBorderColor = CTextSub.copy(alpha = 0.35f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = CTextSub) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(text) },
                        colors  = ButtonDefaults.buttonColors(containerColor = CToggleOn),
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
            shape    = RoundedCornerShape(12.dp),
            color    = CBtnBg,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Input text", color = CText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value         = text,
                    onValueChange = { text = it },
                    singleLine    = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor     = CText,
                        unfocusedTextColor   = CText,
                        focusedBorderColor   = CToggleOn,
                        unfocusedBorderColor = CTextSub.copy(alpha = 0.35f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = CTextSub) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(text) },
                        colors  = ButtonDefaults.buttonColors(containerColor = CToggleOn),
                    ) {
                        Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
