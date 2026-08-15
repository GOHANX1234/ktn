# S3 Hacks — Android Mod Menu: Complete Project Analysis

> **Project:** S3 Hacks Floating Mod Menu  
> **Base:** LGLTeam/Android-Mod-Menu (forked & heavily customized)  
> **Target Game:** Call of Duty: Mobile (COD:M) — `Assembly-CSharp.dll`, `libil2cpp.so`  
> **Build Tool:** Android Studio / Codemagic CI + Android NDK  
> **License:** GNU General Public License v3  
> **Android Layer:** Fully rewritten from Java to **Kotlin + Jetpack Compose**

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Languages & Technologies](#2-languages--technologies)
3. [Build Configuration](#3-build-configuration)
4. [Full Directory Structure](#4-full-directory-structure)
5. [Kotlin Layer — Detailed Breakdown](#5-kotlin-layer--detailed-breakdown)
6. [Native C++ Layer — Detailed Breakdown](#6-native-cc-layer--detailed-breakdown)
7. [Third-Party Libraries](#7-third-party-libraries)
8. [Feature List (Hack Capabilities)](#8-feature-list-hack-capabilities)
9. [JNI Bridge — How Kotlin Talks to C++](#9-jni-bridge--how-kotlin-talks-to-c)
10. [Hook System — How the Cheats Work](#10-hook-system--how-the-cheats-work)
11. [Menu UI System (Jetpack Compose)](#11-menu-ui-system-jetpack-compose)
12. [Preferences & Persistence System](#12-preferences--persistence-system)
13. [Security & Anti-Detection Mechanisms](#13-security--anti-detection-mechanisms)
14. [Memory System (KittyMemory)](#14-memory-system-kittymemory)
15. [Il2Cpp Reflection System](#15-il2cpp-reflection-system)
16. [Chams / Visual Hacks (OpenGL ES)](#16-chams--visual-hacks-opengl-es)
17. [Android Manifest & Permissions](#17-android-manifest--permissions)
18. [App Lifecycle & Startup Flow](#18-app-lifecycle--startup-flow)
19. [Crash Handler](#19-crash-handler)
20. [Assets & Resources](#20-assets--resources)
21. [How to Build (Android Studio / Codemagic)](#21-how-to-build-android-studio--codemagic)
22. [How to Inject / Use](#22-how-to-inject--use)
23. [Known Issues & Limitations](#23-known-issues--limitations)

---

## 1. Project Overview

This is a **floating overlay mod menu** injected into Android Unity/il2cpp games — specifically customized for **Call of Duty: Mobile**. It compiles into a shared native library (`libclerx.so`) which is loaded via `System.loadLibrary()` at runtime. The mod menu floats on top of the game window using Android's `SYSTEM_ALERT_WINDOW` overlay permission.

The Android application layer has been **fully rewritten from Java to Kotlin**, and the floating UI has been rebuilt with **Jetpack Compose** (Material 3) instead of the original programmatic View system. The native C++ layer is unchanged.

### What it Does
- Injects a draggable floating overlay button on top of any running app
- Hooks game functions at runtime using Shadowhook / Dobby
- Reads/writes game memory using KittyMemory
- Exposes gameplay cheats through a touch-driven Compose menu: aimbot, ESP, chams, fly map, guest reset, etc.
- Persists feature settings across sessions using Android SharedPreferences

### Architecture at a Glance
```
Kotlin (Android Activity/Service)
        │
        │  JNI (Java Native Interface)
        ▼
C++ Shared Library  ──►  Hook Engine (Shadowhook/Dobby)
        │                       │
        │                       ▼
        │              Game Functions (il2cpp)
        ▼
KittyMemory  ──►  Direct Memory R/W
        │
        ▼
Il2Cpp Reflection  ──►  Class/Method/Field Lookup at Runtime
```

---

## 2. Languages & Technologies

| Layer | Language / Technology |
|---|---|
| Android UI & lifecycle | **Kotlin** |
| Floating overlay UI | **Jetpack Compose (Material 3)** |
| Native hacks & hooks | C++ (C++17) |
| Dynamic library | C (xDL — dynamic linker) |
| Build system (top-level) | Gradle (Groovy DSL) + Kotlin plugin |
| Native build system | Android NDK Build (`ndk-build`, `Android.mk`) |
| OpenGL hooks (chams) | OpenGL ES 2.0 via `libGLESv2.so` |
| String protection | AY Obfuscator (compile-time XOR cipher, C++14) |
| Memory patching | KittyMemory + Keystone assembler |
| Function hooking | Shadowhook + Dobby |
| Il2Cpp introspection | Custom Il2Cpp runtime reflection (`Il2Cpp.h` / `Il2Cpp.cpp`) |
| Coroutines | `kotlinx-coroutines-android` (used in Compose `LaunchedEffect`) |

> **Migration note:** All `.java` files under `app/src/main/java/com/android/support/` have been replaced with `.kt` equivalents. The C++ JNI side is **completely unchanged** — native method signatures, class paths, and method tables remain identical.

---

## 3. Build Configuration

### Top-Level `build.gradle`
```
AGP (Android Gradle Plugin): 7.4.2
Kotlin version: 1.8.22
Repositories: google(), mavenCentral()
```

### App-Level `app/build.gradle`
| Setting | Value |
|---|---|
| `compileSdk` | 33 (Android 13) |
| `minSdk` | 21 (Android 5.0 Lollipop — raised from 19 for Compose) |
| `targetSdk` | 33 (Android 13) |
| `applicationId` | `uk.lgl` |
| `versionName` | `2.9` |
| `versionCode` | `1` |
| ABI Filters | `arm64-v8a` only (armeabi-v7a and x86 dropped) |
| NDK Version | `25.2.9519653` (r25c — stable Apple Silicon host support) |
| Native Build | NDK Build via `Android.mk` |
| Signing | `signingConfigs.debug` (debug key) |
| ProGuard | Disabled (`minifyEnabled false`) |
| Compose | Enabled (`buildFeatures { compose true }`) |
| Compose Compiler | `1.4.8` (matches Kotlin 1.8.22) |
| JVM Target | `1.8` |

### Kotlin / Compose Dependencies
| Dependency | Purpose |
|---|---|
| `kotlin-stdlib` | Kotlin standard library |
| `compose-bom:2023.06.01` | Pins all Compose library versions |
| `compose.ui`, `compose.ui:ui-graphics` | Compose core UI |
| `material3` | Material You design components |
| `foundation`, `animation`, `runtime` | Compose building blocks |
| `lifecycle-runtime-ktx:2.6.1` | Lifecycle integration for Compose |
| `lifecycle-viewmodel-ktx:2.6.1` | ViewModel support in overlay |
| `savedstate:1.2.1` | SavedState registry for overlay `LifecycleOwner` |
| `kotlinx-coroutines-android:1.7.1` | Coroutines for `LaunchedEffect` / `delay` |

### `Application.mk` (NDK)
| Setting | Value |
|---|---|
| `APP_ABI` | `arm64-v8a` |
| `APP_STL` | `c++_static` |
| `APP_OPTIM` | `release` |
| `APP_THIN_ARCHIVE` | `true` |
| `APP_PIE` | `true` |

### `Android.mk` (NDK)
The main module name is **`clerx`** (maps to `libclerx.so`).

**Compiler flags:**
```
-w -s -Wno-error=format-security -fvisibility=hidden
-fpermissive -fexceptions -std=c++17
-Wno-error=c++11-narrowing -Wall
```

**Linker flags:**
```
-Wl,--gc-sections,--strip-all,-llog
System libs: -llog -landroid -lEGL -lGLESv2
```

**Static libraries linked:**
- `Keystone` (assembler, for KittyMemory)
- `Dobby` (function hooking)
- `Shadowhook` (function hooking — arm64 only)

**Source files compiled:**
```
Main.cpp
Includes/Utils.cpp
KittyMemory/KittyArm64.cpp
KittyMemory/KittyMemory.cpp
KittyMemory/KittyScanner.cpp
KittyMemory/KittyUtils.cpp
KittyMemory/MemoryBackup.cpp
KittyMemory/MemoryPatch.cpp
S3HACKS/Tools.cpp
S3HACKS/Il2Cpp.cpp
```

---

## 4. Full Directory Structure

```
Android-Mod-Menu/
├── build.gradle                        # Top-level Gradle config (AGP 7.4.2, Kotlin 1.8.22)
├── settings.gradle                     # Module: ':app'
├── gradle.properties
├── gradlew / gradlew.bat               # Gradle wrapper scripts
├── gradle/wrapper/
│   └── gradle-wrapper.properties       # Gradle version config
├── codemagic.yaml                      # Codemagic CI/CD build pipeline
├── LICENSE                             # GNU GPL v3
│
└── app/
    ├── build.gradle                    # App module config (SDK 33, NDK r25c, Compose)
    ├── proguard-rules.pro              # ProGuard rules (not enabled)
    │
    └── src/main/
        ├── AndroidManifest.xml         # App manifest, permissions, components
        ├── ic_launcher-playstore.png   # Play Store icon
        │
        ├── assets/
        │   └── clerx.ttf              # Custom font asset
        │
        ├── java/com/android/support/   # (package path kept; all files are .kt)
        │   ├── MainActivity.kt         # Entry point Activity; launches game + menu
        │   ├── Main.kt                 # Kotlin object; loads native lib, starts flow
        │   ├── Launcher.kt             # Foreground Service; hosts the menu overlay
        │   ├── MenuOverlay.kt          # WindowManager setup + ComposeView host
        │   ├── MenuCompose.kt          # Entire floating UI in Jetpack Compose
        │   ├── FeatureItem.kt          # Sealed class hierarchy + parseFeatureList()
        │   ├── OverlayLifecycleOwner.kt # LifecycleOwner bridge for Compose-in-overlay
        │   ├── Preferences.kt          # SharedPreferences wrapper (read/write all types)
        │   └── CrashHandler.kt         # Kotlin object; uncaught exception handler
        │
        ├── jni/
        │   ├── Android.mk              # NDK Makefile — source files, flags, linked libs
        │   ├── Application.mk          # NDK application settings (ABI, STL, optimisation)
        │   ├── Main.cpp                # JNI_OnLoad + feature list + Changes() dispatcher
        │   ├── hook.h                  # All game hooks (aimbot, ESP, fly map, etc.)
        │   ├── class.h                 # Game class/method wrappers (il2cpp call helpers)
        │   │
        │   ├── Menu/
        │   │   ├── Menu.h              # Icon (base64), SettingsList(), Init() native side
        │   │   ├── Setup.h             # Overlay permission check, service launcher, dialogs
        │   │   └── get_device_api_level_inlines.h  # Runtime API level detection
        │   │
        │   ├── Includes/
        │   │   ├── Logger.h            # LOGD/LOGE/LOGI/LOGW macros → __android_log_print
        │   │   ├── obfuscate.h         # AY compile-time XOR string obfuscator (C++14)
        │   │   ├── Macros.h            # General-purpose macros
        │   │   ├── Utils.h / Utils.hpp / Utils.cpp  # Utility functions
        │   │
        │   ├── S3HACKS/
        │   │   ├── Il2Cpp.h / Il2Cpp.cpp  # Il2Cpp runtime reflection engine
        │   │   ├── Tools.h / Tools.cpp    # Memory R/W, base address, pattern scan
        │   │   ├── Includes.h             # Common includes for S3HACKS modules
        │   │   ├── AutoUpdate.h           # Field<T>/LoadClass — il2cpp field access helpers
        │   │   ├── chams.h                # OpenGL ES chams (wallhack, wireframe, glow, etc.)
        │   │   ├── fake_dlfcn.h / .cpp    # Custom dlopen/dlsym (bypasses linker restrictions)
        │   │   ├── Struct.h               # Large game struct definitions (COD:M classes)
        │   │   ├── Quaternion.h / .hpp    # 3D rotation math
        │   │   ├── Vector2.h / .hpp       # 2D vector math
        │   │   ├── Vector3.hpp            # 3D vector math (Distance, Angle, Lerp, etc.)
        │   │   └── Rect.h                 # Rectangle struct
        │   │
        │   ├── KittyMemory/
        │   │   ├── KittyMemory.h / .cpp   # Core memory R/W with mprotect support
        │   │   ├── KittyArm64.h / .cpp    # ARM64-specific patching helpers
        │   │   ├── KittyScanner.h / .cpp  # Pattern/signature scanning in memory
        │   │   ├── KittyUtils.h / .cpp    # Utilities (hex strings, maps, etc.)
        │   │   ├── MemoryPatch.h / .cpp   # High-level patch objects (store + restore bytes)
        │   │   ├── MemoryBackup.h / .cpp  # Byte-level backup before patching
        │   │   └── Deps/Keystone/
        │   │       ├── includes/          # Keystone assembler headers (ARM, ARM64, x86...)
        │   │       └── libs-android/
        │   │           ├── arm64-v8a/libkeystone.a
        │   │           ├── armeabi-v7a/libkeystone.a
        │   │           ├── x86/libkeystone.a
        │   │           └── x86_64/libkeystone.a
        │   │
        │   ├── Dobby/
        │   │   ├── dobby.h                # Dobby hook API header
        │   │   ├── README.md
        │   │   ├── arm64-v8a/libdobby.a
        │   │   ├── armeabi-v7a/libdobby.a
        │   │   ├── x86/libdobby.a
        │   │   └── x86_64/libdobby.a
        │   │
        │   ├── Shadowhook/
        │   │   ├── Shadowhook.h           # Shadowhook API header
        │   │   └── libraries/arm64-v8a/libShadowhook.a
        │   │
        │   └── xDL/
        │       ├── xdl.h / xdl.c          # Custom dynamic linker (dlopen/dlsym substitute)
        │       ├── xdl_iterate.h / .c     # Library map iteration
        │       ├── xdl_linker.h / .c      # Linker internals access
        │       ├── xdl_lzma.h / .c        # LZMA decompression (for packed libs)
        │       ├── xdl_util.h / .c        # Utilities
        │       └── xdl.map.txt            # Symbol map
        │
        └── res/
            ├── drawable/
            │   └── ic_launcher_foreground.xml   # Adaptive icon foreground
            ├── layout/
            │   └── activity_main.xml            # Main activity layout
            ├── mipmap-{hdpi,mdpi,xhdpi,xxhdpi,xxxhdpi}/
            │   ├── ic_launcher.png
            │   └── ic_launcher_round.png
            ├── mipmap-anydpi-v26/
            │   ├── ic_launcher.xml
            │   └── ic_launcher_round.xml
            └── values/
                ├── strings.xml          # app_name = "LGL Mod Menu"
                ├── colors.xml
                ├── styles.xml
                └── ic_launcher_background.xml
```

---

## 5. Kotlin Layer — Detailed Breakdown

> **All Android-side source files are now Kotlin (`.kt`).** The package path `com/android/support/` is unchanged, and all JNI-registered class/method names remain the same so the C++ side requires no modification.

---

### `MainActivity.kt`
**Role:** The app's entry point (launcher Activity).

**What it does:**
1. On `onCreate()`, attempts to launch the target game's Activity (`com.unity3d.player.UnityPlayerActivity`)
2. Simultaneously calls `Main.Start(this)` to begin the mod menu initialization
3. If the game Activity is not found (`ClassNotFoundException`), still calls `Main.Start(this)` as fallback
4. Uses a `hasLaunched` guard to prevent double-invocation on configuration changes

**Key field:**
- `gameActivity` — `val` storing the fully-qualified class name of the game to launch; change this to target a different game

**Kotlin notes:** Class inherits `Activity` directly (no `AppCompatActivity` needed). `hasLaunched` is a `var` property rather than a static field.

---

### `Main.kt`
**Role:** Bootstrapper that loads the native library and starts the permission + service flow.

**Kotlin type:** `object` (singleton — no instantiation needed, replaces static methods in Java).

**What it does:**
- `init { }` block: `System.loadLibrary("clerx")` — loads `libclerx.so` on first access
- `Start(context)`: Calls `CrashHandler.init(context, false)` then the native `CheckOverlayPermission(context)`
- `StartWithoutPermission(context)`: Alternative start path — if already in an Activity context, creates `MenuOverlay` directly and calls `setWindowManagerActivity()` instead of going through the Service

**Native method declared:**
```kotlin
@JvmStatic private external fun CheckOverlayPermission(context: Context)
```

**Kotlin notes:** `@JvmStatic` preserves the static-call ABI the JNI C++ side expects. The `CrashHandler` reference is now a direct `object` reference rather than a `new` instantiation.

---

### `Launcher.kt`
**Role:** Android `Service` that hosts the floating mod menu overlay.

**What it does:**
1. `onCreate()`: Creates a `MenuOverlay(this)` and calls `setWindowManagerWindowService()` to attach to WindowManager (overlay type)
2. Runs a `Handler` loop every 1 second calling `checkGameForeground()` which hides/shows the menu based on whether the game is in the foreground
3. `onDestroy()`: Calls `menu.onDestroy()` to clean up the overlay and lifecycle
4. `onTaskRemoved()`: Sleeps 100 ms then calls `stopSelf()` when the task is swiped from recents
5. `onStartCommand()` returns `START_NOT_STICKY` — the service will not auto-restart if killed
6. `onBind()` returns `null` (not a bound service)

**Foreground detection:**
```kotlin
info.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
```

**Kotlin notes:** `lateinit var menu: MenuOverlay` replaces the Java field with a null-safety guarantee. `onBind` uses a single-expression `= null` return.

---

### `MenuOverlay.kt`
**Role:** Kotlin replacement for `Menu.java`. Manages the WindowManager overlay window and hosts Jetpack Compose inside a `ComposeView`. All native method declarations are identical to the originals.

**Key responsibilities:**
- Declares all native methods (`Init`, `Icon`, `IconWebViewData`, `GetFeatureList`, `SettingsList`, `IsGameLibLoaded`)
- Manages `WindowManager.LayoutParams` and the `ComposeView` instance
- Sets up the `OverlayLifecycleOwner` required for Compose to work outside an Activity
- Exposes `setWindowManagerWindowService()` (service mode) and `setWindowManagerActivity()` (activity mode)
- Exposes `setVisibility()` and `onDestroy()` for `Launcher`

**`buildAndAttachView()` — internal setup:**
1. Calls native `Init(context, titleTv, subTitleTv)` on temporary `TextView`s to receive title/subtitle strings
2. Reads the text back into `mutableStateOf` — Compose observes these as state
3. Creates an `OverlayLifecycleOwner` and advances it through `onCreate → onStart → onResume`
4. Creates a `ComposeView`, wires tree owners (`ViewTreeLifecycleOwner`, `ViewTreeViewModelStoreOwner`, `ViewTreeSavedStateRegistryOwner`)
5. Calls `view.setContent { FloatingMenu(...) }` to mount the Compose tree
6. Adds the view to `WindowManager` with the prepared `LayoutParams`

**Native methods declared:**
```kotlin
external fun Init(context: Context, title: TextView, subTitle: TextView)
external fun Icon(): String
external fun IconWebViewData(): String
external fun GetFeatureList(): Array<String>
external fun SettingsList(): Array<String>
external fun IsGameLibLoaded(): Boolean
```

**WindowManager setup:**
- **Service mode** (`setWindowManagerWindowService`): Uses `TYPE_APPLICATION_OVERLAY` (API ≥ 26) or `TYPE_PHONE` (older). Flags: `FLAG_NOT_FOCUSABLE`.
- **Activity mode** (`setWindowManagerActivity`): Uses `TYPE_APPLICATION` with additional layout flags (`FLAG_LAYOUT_IN_OVERSCAN`, `FLAG_LAYOUT_IN_SCREEN`, `FLAG_SPLIT_TOUCH`).

---

### `MenuCompose.kt`
**Role:** The entire floating overlay UI, implemented as Jetpack Compose composables. This replaces the 1091-line `Menu.java` programmatic View system entirely.

#### Colour Palette (identical values to the original Java)
| Constant | Hex | Purpose |
|---|---|---|
| `CText` | `#FFFFFF` | Pure white text |
| `CTextSub` | `#E0E0E0` | Light gray secondary text |
| `CBtnBg` | `#1A1A2E` | Dark navy button background |
| `CMenuBg` | `#CC0F0F1A` | Semi-transparent dark menu background |
| `CFeatureBg` | `#1A1A2E` | Feature list background |
| `CToggleOn` | `#00FFAA` | Neon green (active state) |
| `CToggleOff` | `#3D3D5C` | Muted purple-gray (inactive state) |
| `CBtnOn` | `#00ACC1` | Cyan (button ON) |
| `CBtnOff` | `#2C2C44` | Dark muted blue (button OFF) |
| `CCategoryBg` | `#252540` | Dark slate category header |
| `CCollapseBg` | `#222D38` | Collapse section background |
| `CBorder` | `#32CB00` | Green border on collapsed icon and menu panel |

Layout constants: `MenuWidth = 290.dp`, `MenuScrollH = 210.dp`, `MenuCorner = 12.dp`.

#### Root Composable: `FloatingMenu`
The single entry point for the entire overlay UI. Holds all top-level state:
- `isMenuExpanded` — collapsed icon vs. full menu panel
- `isSettingsOpen` — feature list vs. settings panel
- `menuAlpha` — dims to 0.5 while dragging
- `posX / posY` — current window position (mirrored into `WindowManager.LayoutParams`)
- `featureItems / settingItems` — typed `List<FeatureItem>` parsed from native strings
- `menuReady` — false while waiting for `libil2cpp.so` to load

**Game lib polling** is done with `LaunchedEffect(Unit)` + `delay(600)` coroutine loop, replacing the original `Handler` / thread approach.

**Drag** is implemented with `pointerInput` + `awaitPointerEventScope` on both the collapsed icon and the expanded panel. A movement < 10 px is treated as a tap (opens the menu); larger movements drag the window via `windowManager.updateViewLayout()`.

#### Overlay States
- **Collapsed:** A 65 dp circle with `S3` text (28 sp bold), neon green `#32CB00` 2 dp border. Uses `AnimatedVisibility(fadeIn/fadeOut)`.
- **Expanded:** A rounded `Column` (290 dp wide) with title bar, scrollable feature/settings list, and a CLOSE button. Enters via `fadeIn() + expandVertically()`, exits via `fadeOut() + shrinkVertically()`.

#### Feature Widget Composables
Every item from `GetFeatureList()` / `SettingsList()` is dispatched by `FeatureRow` to its typed composable:

| Widget Type | Composable | Compose Component Used |
|---|---|---|
| `Toggle` | `ToggleRow` | Material 3 `Switch` |
| `ButtonOnOff` | `ButtonOnOffRow` | `Button` with `animateColorAsState` + `animateFloatAsState` scale |
| `Button` | `ButtonRow` | `Button` |
| `ButtonLink` | `ButtonLinkRow` | `Button` that fires an `Intent.ACTION_VIEW` |
| `SeekBar` | `SeekBarRow` | Material 3 `Slider` |
| `Spinner` | `SpinnerRow` | `ExposedDropdownMenuBox` + `OutlinedTextField` |
| `InputText` | `InputTextRow` | `Button` that opens a string `AlertDialog` |
| `InputValue` | `InputValueRow` | `Button` that opens a numeric `AlertDialog` |
| `CheckBox` | `CheckBoxRow` | `Checkbox` + `Row` |
| `RadioButton` | `RadioButtonRow` | `RadioButton` group in a `Column` |
| `Collapse` | `CollapseSectionRow` | `AnimatedVisibility` expandable section |
| `Category` | `CategoryRow` | `Text` with `CCategoryBg` background |
| `RichTextView` | `RichTextRow` | `AndroidView` wrapping a `TextView` with `Html.fromHtml` |
| `RichWebView` | `RichWebRow` | `AndroidView` wrapping a `WebView` |

The marquee scrolling subtitle uses `AndroidView { TextView(...) }` with `TruncateAt.MARQUEE` — a real `TextView` embedded inside Compose for compatibility.

---

### `FeatureItem.kt`
**Role:** Type-safe data model for feature list items. Replaces the raw string-parsing logic that was inlined in `Menu.java`'s `featureList()` method.

**`sealed class FeatureItem`** — one subtype per widget:
```kotlin
sealed class FeatureItem {
    abstract val featNum: Int

    data class Toggle(featNum, name, defaultOn)
    data class SeekBarItem(featNum, name, min, max)
    data class ButtonItem(featNum, name)
    data class ButtonOnOff(featNum, name, defaultOn)
    data class SpinnerItem(featNum, name, options: List<String>)
    data class InputText(featNum, name)
    data class InputValue(featNum, name, maxValue)
    data class CheckBoxItem(featNum, name, defaultOn)
    data class RadioButtonGroup(featNum, name, options: List<String>)
    data class CollapseSection(text, startExpanded, children: List<FeatureItem>)
    data class ButtonLink(name, url)
    data class CategoryLabel(text)
    data class RichTextLabel(text)
    data class RichWebLabel(text)
}
```

**`parseFeatureList(listFT: Array<String>): List<FeatureItem>`** — top-level function that parses the raw native string array into the typed hierarchy. Handles:
- `_True` suffix → `defaultOn = true`
- `CollapseAdd_` prefix → child item routed into the active `CollapseSection`
- Numeric first token → explicit `featNum`; otherwise auto-incrementing index

---

### `OverlayLifecycleOwner.kt`
**Role:** A minimal `LifecycleOwner` / `ViewModelStoreOwner` / `SavedStateRegistryOwner` that allows Jetpack Compose to run inside a `WindowManager` overlay (a `Service` context that has no Activity lifecycle).

**Why it's needed:** Compose's internals (recomposition, coroutines, `remember`, state restoration) depend on a `LifecycleOwner` being attached to the view tree via `ViewTreeLifecycleOwner`. Inside a `Service`, no such owner exists — this class provides one.

**Lifecycle advancement** is driven explicitly by `MenuOverlay.buildAndAttachView()`:
```
onCreate() → onStart() → onResume()   (when overlay is created)
onDestroy()                            (when Launcher.onDestroy() is called)
```

---

### `Preferences.kt`
**Role:** Full SharedPreferences wrapper — read and write all Kotlin/Java primitive types plus string sets.

**Kotlin type:** Regular `class` with a `companion object` that holds static-equivalent members and the native `Changes` declaration.

- Preference file: `<packageName>_preferences`
- Keys: feature numbers stored as `featureNum.toString()`
- Every call to `changeFeatureBool/Int/String` also calls the native `Changes()` method to propagate the value into C++ immediately
- `@JvmField` on `context`, `loadPref`, `isExpanded` ensures Java/JNI can access them as fields

**Special feature numbers:**
| Number | Behaviour |
|---|---|
| `-1` | "Save feature preferences" toggle — enables/disables SharedPreferences persistence; clearing on disable |
| `-3` | "Auto size vertically" toggle — switches menu scroll area to `weight(1f)` expand mode |
| `-6` | "Close settings" button — handled in `ButtonRow` composable |
| `-100` | "Force load menu" button — sets `stopChecking = true` in `FloatingMenu` state, bypassing the game-lib wait |

---

### `CrashHandler.kt`
**Role:** Global uncaught exception handler.

**Kotlin type:** `object` (singleton — replaces `static` methods).

**On crash:**
1. Captures timestamp (`SimpleDateFormat`), device info (`Build.*`), app version (`packageManager.getPackageInfo`)
2. Converts stack trace to string via `StringWriter` + `PrintWriter`
3. Builds a formatted crash report string using Kotlin `buildString { appendLine(...) }`
4. Writes crash log to:
   - Android 11+ (API 30): `/storage/emulated/0/Documents/mod_menu_crash_<timestamp>.txt`
   - Older: `context.getExternalFilesDir(null)`
5. Shows two Toast messages: "Game has crashed unexpectedly" + log file path
6. Calls `System.exit(2)`

**`init(app, overlayRequired)`** — takes an `overlayRequired: Boolean` parameter (used to distinguish service vs. activity context, though both paths currently share the same handler logic).

---

## 6. Native C++ Layer — Detailed Breakdown

> **The C++ layer is completely unchanged from the original Java version.** All JNI class names, method names, and signatures remain the same. Everything in sections 6 through 16 (C++ logic) is identical to the original project. Only the Java class names in JNI registration comments now refer to their Kotlin equivalents.

### `Main.cpp` — The Core Entry Point

#### `JNI_OnLoad(JavaVM *vm, void *reserved)`
Called automatically when `System.loadLibrary()` is executed. This is the true entry point of the native library.

**What it does:**
1. Gets `JNIEnv` from the VM
2. Stores `JavaVM*` as `publicVM` (used later for attaching threads)
3. Spawns `hack_thread` on a new pthread — this thread waits for the game library to load, then hooks game functions
4. Registers 3 JNI native method tables:
   - `RegisterMenu(env)` → binds `MenuOverlay.kt` native methods
   - `RegisterPreferences(env)` → binds `Preferences.kt` native methods
   - `RegisterMain(env)` → binds `Main.kt` native methods

#### `GetFeatureList()` — Feature Definition
Returns an array of strings defining every button in the menu. Format: `"<id>_<type>_<name>"`.

Current features defined:
```
S3 HACKS (title)
├── Category: Aim
│   ├── 8_ButtonOnOff_Enable Aim
│   ├── 11_ButtonOnOff_Aim Fire
│   ├── 12_ButtonOnOff_Aim Auto
│   └── 0_ButtonOnOff_Aim Silent
├── Category: Esp
│   ├── 80_ButtonOnOff_Enable ESP
│   ├── 9_ButtonOnOff_Esp Fire
│   ├── 10_ButtonOnOff_Esp Fire Blue
│   └── 13_ButtonOnOff_Esp Alert
├── Category: Chams
│   ├── 1_ButtonOnOff_Enable chams
│   ├── 3_ButtonOnOff_Enable shading
│   ├── 4_ButtonOnOff_Enable wireframe
│   ├── 5_ButtonOnOff_Enable glow
│   ├── 6_ButtonOnOff_Enable outline
│   └── 7_ButtonOnOff_Enable rainbow
├── Category: Fly Map
│   └── 14_ButtonOnOff_Fly Map
└── Category: Reset Guest
    └── 2_ButtonOnOff_Reset Guest
```

#### `Changes()` — Feature Toggle Dispatcher
Called from Kotlin (`Preferences.kt`) every time a feature is toggled/adjusted. Routes feature numbers to C++ boolean/float globals:

| Feature # | Variable | Hack |
|---|---|---|
| 0 | `SilentAimv222` | Silent Aim |
| 1 | `chams` | Chams (wallhack render) |
| 2 | `Guest` | Reset Guest |
| 3 | `shading` | Chams shading |
| 4 | `wireframe` | Chams wireframe |
| 5 | `glow` | Chams glow |
| 6 | `outline` | Chams outline |
| 7 | `rainbow` | Rainbow chams |
| 8 | `EnableAim` | Aimbot enable |
| 9 | `EspFire` | ESP fire line (red) |
| 10 | `EspFireBlue` | ESP fire line (blue) |
| 11 | `AimFire` | Aim while firing |
| 12 | `AimAuto` | Auto aim (always) |
| 13 | `EspAlert` | ESP alert (enemy info HUD) |

---

### `hook.h` — Game Hook Implementations

This is where all the actual gameplay cheating logic lives.

#### Global State Variables
```cpp
bool SilentAimv222 = false;     // Silent aim toggle
bool ignoreEnemyBot = false;    // Whether to skip bots in targeting
bool ignoreKnocked = true;      // Skip knocked players
int aimPosition = 0;            // 0=Head, 1=Neck, 2=Mid, 3=Hip
float NECK_OFFSET = -0.15f;     // Y offset for neck aim
bool checkVisible2 = true;      // Visibility check before aiming
bool Headshot = false;          // Force headshot damage
bool EnableAim = false;         // Master aimbot switch
float Fov_Aim = 330.0f;         // Aimbot FOV angle (100x multiplier)
bool AimVisible = true;         // Only aim at visible enemies
bool AimFire / AimScope / AimAuto  // Aim trigger modes
bool EspFire / EspFireBlue / EspAlert / EspCount  // ESP modes
bool Guest = false;             // Reset guest mode hack
bool ghostmapcs = false;        // Fly/ghost map mode (internal)
```

#### Key Functions

**`EnemySlientAim()`** — Silent Aim Target Selection
- Iterates the game's player dictionary (`ListPlayer` field offset from il2cpp)
- Filters: skip bots (if enabled), skip team, skip knocked, skip dead, skip max-HP=0, skip invisible
- Optionally checks line-of-sight via `Visible_Check()` (raycasting)
- Returns the closest enemy by world-space distance

**`EnemyVisible(void* match)`** — FOV Aimbot Target Selection
- Similar filtering as above
- Additionally checks angle to camera forward vector: only targets within `Fov_Aim` (multiplied by 100 in storage)
- Returns the enemy with the smallest angle from screen center

**`GetAdjustedPosition(void* enemy)`** — Aim Position Calculator
- `aimPosition == 0`: Head
- `aimPosition == 1`: Neck (head Y - 0.15)
- `aimPosition == 2 / 3`: Hip

**`isEnemyInRangeWeapon(player, enemy, weapon)`** — Range Check
- Computes distance between player head and enemy head
- Compares against the weapon's max range (`get_Range`)

**`Visible_Check(void* enemy)`** — Raycast Visibility
- Casts a ray from camera to enemy head collider
- Uses `Physics_Raycast` (layer 12); returns `true` if not blocked

---

#### Hooked Game Functions (via Shadowhook)

| Hook | Target | Purpose |
|---|---|---|
| `hook_PlayerNetwork_TakeDamage` | `COW.GamePlay.PlayerNetwork.TakeDamage` (overload 9) | Modifies fire/hit position to force headshots when `Headshot=true`; repositions hit to enemy head |
| `BLAGCMCGEJG1` | `COW.GamePlay.GPBDEDFKJNA.BLAGCMCGEJG` (overload 1) | Silent aim — intercepts hit-scan result and redirects hit location to closest enemy's head |
| `_LateUpdate` | `COW.GamePlay.Player.UpdateBehavior` (overload 2) | Frame-update hook: runs aimbot rotation, ESP fire lines, ESP alert HUD display |
| `_ResetGuest` | `COW.GameConfig.get_ResetGuest` (overload 0) | Returns `true` always when `Guest=true`, bypassing guest mode restriction |
| `hook_BEV_Jump_onExecute` | `COW.GamePlay.OMNMBMOKLOH.onExecute` (overload 1) | Increases vertical position on jump (fly-related) |
| `hook_isground` | `UnityEngine.CharacterController.get_isGrounded` (overload 0) | Returns `-1` (not grounded) when `ghostmapcs=true` — enables no-clip movement |
| `GarenaMSDKMgr` | `GarenaMSDK.GarenaMSDKMgr.Update` (overload 0) | Fly map — locks Y position to a saved value, ignoring gravity |

---

#### `_LateUpdate` — Per-Frame Logic
This is the most complex hook, running every game frame:

1. **Aimbot rotation** — if `EnableAim` and a target exists:
   - Computes `Quaternion.LookRotation` from player camera to enemy head
   - Applies via `Player.JPNJCAONHME()` (obfuscated set_aim method)
   - Respects mode: `AimFire` (only while firing), `AimScope` (only while scoped), `AimAuto` (always)

2. **ESP Fire Lines** — iterates all enemies:
   - `EspFire`: draws a red line from local player center to enemy center (`GPBDEDFKJNA.CPBCGAKODII overload 4`)
   - `EspFireBlue`: draws blue line (`UGCLevelMiniSentry.CPBCGAKODII overload 2`)

3. **ESP Alert** — enemy proximity HUD:
   - Gets `CurrentInGameUIScene()`
   - Finds closest visible enemy
   - Displays name + distance + HP + bot/real status via `UIInGameScene.ShowAssistantText()`

---

### `class.h` — Il2Cpp Wrapper Functions

All game method calls are wrapped in typed inline functions using macros that resolve offsets via `Il2CppGetMethodOffset()` and `Il2CppGetFieldOffset()` at runtime. All strings passed to the resolver are XOR-obfuscated at compile time.

#### Data Structures Defined
```cpp
struct UnityArray<T>        // Unity's managed array (klass, monitor, bounds, max_length, vector[])
struct DictionaryEntry      // Single entry in a Unity Dictionary (hashCode, next, key, value)
struct MyDictionary         // Unity Dictionary (buckets, entries, count)
struct monoArray<T>         // Mono runtime array wrapper
struct monoList<T>          // Mono runtime List<T> wrapper
struct monoString           // Mono runtime string (klass, monitor, length, chars[])
class Vvector3              // Alternate Vector3 (for set_position_Injected ABI)
```

#### Game Method Wrappers

| Function | Il2Cpp Target | Returns |
|---|---|---|
| `Curent_Match()` | `COW.GameFacade.CurrentMatch` | Active match object |
| `GetLocalPlayer(match)` | `COW.UIHudDetectorController.GetLocalPlayer` | Local player object |
| `GetHeadPosition(player)` | `COW.GamePlay.Player.GetHeadTF` + `Transform.get_position` | Vector3 head world pos |
| `GetHipPosition(player)` | `COW.GamePlay.Player.GetHipTF` + `Transform.get_position` | Vector3 hip world pos |
| `getPosition(player)` | `UnityEngine.Component.get_transform` + `get_position` | Vector3 world pos |
| `Component_GetTransform(c)` | `UnityEngine.Component.get_transform` | Transform object |
| `Transform_GetPosition(t)` | `UnityEngine.Transform.get_position_Injected` | Vector3 |
| `Camera_main()` | `UnityEngine.Camera.get_main` | Main camera object |
| `CameraMain(player)` | Field: `Player.MainCameraTransform` + position | Camera world pos (Vector3) |
| `get_IsClientBot(player)` | Field: `Player.IsClientBot` | bool |
| `get_isLocalTeam(player)` | `COW.GamePlay.Player.IsLocalTeammate` | bool |
| `get_IsDieing(player)` | `COW.GamePlay.Player.get_IsDieing` | bool |
| `GetHp(player)` | `COW.GamePlay.Player.get_CurHP` | int |
| `get_MaxHP(player)` | `COW.GamePlay.Player.get_MaxHP` | int |
| `get_isVisible(player)` | `COW.GamePlay.Player.IsVisible` | bool |
| `get_isGod(player)` | `COW.GamePlay.Player.get_IsGod` | bool |
| `get_IsSighting(player)` | `COW.GamePlay.Player.get_IsSighting` | bool (scoped) |
| `get_IsFiring(player)` | `COW.GamePlay.Player.IsFiring` | bool |
| `get_NickName(player)` | `COW.GamePlay.Player.get_NickName` | monoString* |
| `GetWeaponOnHand(player)` | `COW.GamePlay.Player.GetWeaponOnHand` | Weapon object |
| `get_Range(weapon)` | `COW.GamePlay.GPBDEDFKJNA.JDGGIFMKIKF` | float weapon range |
| `GetAttackableCenterWS(p)` | `COW.GamePlay.Player.GetAttackableCenterWS` | Vector3 |
| `get_imo(player)` | `COW.GamePlay.Player.GetActiveWeapon` | Weapon object |
| `Player_GetHeadCollider(p)` | `COW.GamePlay.Player.get_HeadCollider` | Head collider |
| `Physics_Raycast(...)` | `COW.GamePlay.JEAGCMACNNC.PLDCHDBCOBF` (overload 4) | bool (hit) |
| `set_aim(player, quat)` | `COW.GamePlay.Player.JPNJCAONHME` (overload 1) | void |
| `get_gameObject(comp)` | `UnityEngine.Component.get_gameObject` | GameObject |
| `GetForward(transform)` | `UnityEngine.Transform.get_forward` | Vector3 |
| `WorldToScreenPoint(cam, pos)` | `UnityEngine.Camera.WorldToScreenPoint` (overload 1) | Vector3 screen pos |
| `CurrentInGameUIScene()` | `COW.GameFacade.CurrentInGameUIScene` | UI scene object |
| `ShowAssistantText(ui, n, l)` | `COW.UIInGameScene.ShowAssistantText` (overload 2) | void |
| `GetNameFromPlayer(player)` | `COW.GamePlay.Player.get_NickName` + UTF-16→UTF-8 | std::string |

---

### `Menu/Menu.h` — Native Menu Initialization

**`Icon()`** — Returns a base64-encoded PNG string (the menu icon). The icon data is stored as a single long `OBFUSCATE()`-wrapped string constant in code, not on disk.

**`IconWebViewData()`** — Returns `NULL` (WebView icon disabled). Can be set to an imgur URL or base64 data URI to use an animated GIF instead.

**`SettingsList()`** — Returns the settings panel items:
```
Category_Settings
-1_Toggle_Save feature preferences   (feature -1 = SharedPreferences persistence)
-3_Toggle_Auto size vertically        (feature -3 = expand scroll view)
Category_Menu
-6_Button_<font color='red'>Close settings</font>
```

**`Init(env, ctx, title, subtitle)`** — Called once when menu is shown:
- Sets title to: `<b>S3 HACKS</b>`
- Sets subtitle to: scrolling marquee with `Modded by S3 Hacks | @s3hacks`
- Shows Toast: "Welcome To S3 Hacks"

---

### `Menu/Setup.h` — Overlay Permission & Service

**`CheckOverlayPermission(env, thiz, ctx)`** (JNI static method for `com.android.support.Main`):
1. Gets device API level via `api_level()`
2. If API ≥ 23 (Android 6.0+): checks `Settings.canDrawOverlays(ctx)`
   - If not granted: shows toast, opens `MANAGE_OVERLAY_PERMISSION` settings, spawns exit thread (kills app in 5 seconds)
   - If granted: falls through to `startService()`
3. `startService()`: Creates an Intent for `com.android.support.Launcher` and calls `context.startService()`

**`setDialog()`** — Creates an `AlertDialog.Builder` dialog via JNI (not currently used in main flow, available for custom dialogs).

**`Toast()`** — Shows Android Toast via JNI (used for welcome message and permission warnings).

---

## 7. Third-Party Libraries

### Dobby
- **Type:** Inline function hooking library
- **Supported ABIs:** arm64-v8a, armeabi-v7a, x86, x86_64
- **Distribution:** Pre-compiled static libraries (`libdobby.a`)
- **Usage:** Available but Shadowhook is preferred in the active hooks

### Shadowhook
- **Type:** Android inline hook library (ByteDance)
- **Supported ABIs:** arm64-v8a only in this project
- **Mode used:** `SHADOWHOOK_MODE_UNIQUE`
- **All active hooks use Shadowhook** (`shadowhook_hook_func_addr`)
- **Init:** `shadowhook_init(SHADOWHOOK_MODE_UNIQUE, false)` called in `hack_thread`

### KittyMemory
- **Author:** MJx0 (Ruit)
- **Purpose:** Process memory read/write with page protection handling (`mprotect`)
- **Components:**
  - `KittyMemory.cpp` — core `memWrite`, `memRead`, `read2HexStr`, `getAllMaps`
  - `KittyArm64.cpp` — ARM64-specific helpers
  - `KittyScanner.cpp` — byte-pattern scanning in memory regions
  - `KittyUtils.cpp` — helpers (hex conversion, library maps)
  - `MemoryPatch.cpp` — high-level patch object (saves original bytes, applies patch, can restore)
  - `MemoryBackup.cpp` — raw byte backup before modifying memory
- **Keystone assembler** — bundled as static libs; used by KittyMemory for assembling patches from mnemonics

### Keystone Assembler
- **Purpose:** Assemble ARM/ARM64/x86 instructions from mnemonics at runtime for memory patching
- **Supported ABIs:** arm64-v8a, armeabi-v7a, x86, x86_64
- **Distribution:** Pre-compiled `libkeystone.a`

### xDL (xDynamicLinker)
- **Purpose:** Alternative to standard `dlopen`/`dlsym` that bypasses Android linker namespace restrictions
- **Source files:** `xdl.c`, `xdl_iterate.c`, `xdl_linker.c`, `xdl_lzma.c`, `xdl_util.c`
- **Usage:** Used by the project to reliably resolve symbols in protected system libraries

### AY Obfuscator (`obfuscate.h`)
- **Author:** Adam Yaxley
- **License:** Public Domain (Unlicense)
- **Mechanism:** Compile-time XOR cipher using MurmurHash3-derived key seeded by `__LINE__`
- **Effect:** All strings wrapped in `OBFUSCATE("...")` are stored encrypted in the `.so` binary; they decrypt in-memory only when accessed at runtime
- **Macro:**
  ```cpp
  OBFUSCATE("string")          // Uses line-number-derived key
  OBFUSCATE_KEY("string", key) // Custom 64-bit key
  ```

### Jetpack Compose BOM (2023.06.01)
- **Purpose:** Pins all Compose library versions consistently on the Kotlin/Android layer
- **Components used:** `compose.ui`, `ui-graphics`, `material3`, `foundation`, `animation`, `runtime`

### Coroutines (`kotlinx-coroutines-android:1.7.1`)
- **Purpose:** Powers `LaunchedEffect` + `delay` loops in `FloatingMenu` (game lib polling)
- **Replaces:** The original `Handler` + `Thread.sleep` polling approach in `Menu.java`

---

## 8. Feature List (Hack Capabilities)

### Aim Category
| Feature | ID | Description |
|---|---|---|
| Enable Aim | 8 | Master aimbot toggle. Enables the `_LateUpdate` aimbot logic |
| Aim Fire | 11 | Aimbot only activates while the fire button is held |
| Aim Auto | 12 | Aimbot runs every frame regardless of fire state |
| Aim Silent | 0 | Silent aim — bullets auto-redirect to enemy head without camera movement |

### ESP Category
| Feature | ID | Description |
|---|---|---|
| Enable ESP | 80 | (Listed but ESP drawing is integrated into LateUpdate via EspFire/EspFireBlue) |
| Esp Fire | 9 | Draws a red line from local player to visible enemies (weapon fire trace) |
| Esp Fire Blue | 10 | Same as above but blue line variant |
| Esp Alert | 13 | Overlays enemy name, distance (meters), HP, and bot/real status on the in-game HUD |

### Chams Category (OpenGL ES hooks)
| Feature | ID | Description |
|---|---|---|
| Enable chams | 1 | Draws enemies through walls using GL blending (wallhack) |
| Enable shading | 3 | Custom depth-override shading effect on enemy models |
| Enable wireframe | 4 | Renders enemy geometry as wireframe lines |
| Enable glow | 5 | Glow/bloom render pass around enemy geometry |
| Enable outline | 6 | Outline silhouette around enemies |
| Enable rainbow | 7 | Cycling RGB color on the selected chams effect |

### Fly Map Category
| Feature | ID | Description |
|---|---|---|
| Fly Map | 14 | (maps to `ghostmapcs`) Disables gravity check (`get_isGrounded` always returns false) + locks Y position via `GarenaMSDKMgr.Update` hook — allows floating/flying |

### Reset Guest Category
| Feature | ID | Description |
|---|---|---|
| Reset Guest | 2 | Hooks `GameConfig.get_ResetGuest` — always returns `true`, bypassing guest-mode restrictions |

---

## 9. JNI Bridge — How Kotlin Talks to C++

All communication between Kotlin and C++ goes through **JNI (Java Native Interface)**. Because Kotlin compiles to the same JVM bytecode as Java, the JNI layer is identical — the C++ side does not need to distinguish between Kotlin and Java callers.

### Registration (in `Main.cpp :: JNI_OnLoad`)

Three JNI method tables are registered manually (not using the standard `Java_<pkg>_<class>_<method>` naming convention — this is intentional for obfuscation):

```cpp
// MenuOverlay.kt ← native methods
{ "Icon",            "()Ljava/lang/String;",    Icon             }
{ "IconWebViewData", "()Ljava/lang/String;",    IconWebViewData  }
{ "IsGameLibLoaded", "()Z",                     isGameLibLoaded  }
{ "Init",            "(Context,TextView,TextView)V", Init        }
{ "SettingsList",    "()[Ljava/lang/String;",   SettingsList     }
{ "GetFeatureList",  "()[Ljava/lang/String;",   GetFeatureList   }

// Preferences.kt ← native methods
{ "Changes", "(Context;I;String;I;Z;String;)V", Changes }

// Main.kt ← native methods
{ "CheckOverlayPermission", "(Context;)V", CheckOverlayPermission }
```

### Kotlin → C++ (UI events)
When a user taps a `ButtonOnOff`, the call chain is:
```
User tap → ButtonOnOffRow composable (onClick)
         → Preferences.changeFeatureBool(name, featNum, bool)
           → Preferences.with(ctx).writeBoolean(featNum, bool)
           → Preferences.Changes(ctx, featNum, name, 0, bool, null)   ← JNI call
           → C++ Changes() in Main.cpp
           → Sets C++ boolean global (e.g. SilentAimv222 = bool)
```

### C++ → Kotlin (reading SharedPreferences on load)
```
FloatingMenu LaunchedEffect → overlay.GetFeatureList() → parseFeatureList()
         → FeatureItem widgets call Preferences.loadPrefBool(name, featNum, default)
         → reads sharedPreferences.getBoolean("8", false)  → returns true
         → JNI: Preferences.Changes(ctx, 8, name, 0, true, null)   ← re-syncs C++
         → Widget renders as ON
```

### Kotlin `external` vs Java `native`
Kotlin uses the `external` keyword where Java uses `native`. The compiled bytecode is identical; the JNI C++ side sees no difference.

---

## 10. Hook System — How the Cheats Work

### Step 1: Library Load Detection (`hack_thread`)
```cpp
void *hack_thread(void *) {
    while (!il2cpp) {
        il2cpp = GetBaseAddress("libil2cpp.so");
        unity  = GetBaseAddress("libunity.so");
        // ... other libs
        sleep(4);  // Poll every 4 seconds until game loads
    }
    Il2CppAttach();                          // Init il2cpp reflection
    shadowhook_init(SHADOWHOOK_MODE_UNIQUE, false);  // Init hook engine
    // ... install all hooks
}
```

On the Kotlin/Compose side, `FloatingMenu` polls `overlay.IsGameLibLoaded()` every 600 ms via a `LaunchedEffect` coroutine. The UI shows a "Waiting for game lib..." message and a "Force load menu" button until either the lib loads or the user forces it.

### Step 2: Il2Cpp Attachment
`Il2CppAttach()` — resolves all necessary il2cpp API function pointers from `libil2cpp.so` via `dlopen`/`dlsym`, enabling runtime reflection (class/method/field lookup by name).

### Step 3: Hook Installation
Each hook uses `shadowhook_hook_func_addr()`:
```cpp
shadowhook_hook_func_addr(
    (void*) Il2CppGetMethodOffset("Assembly", "Namespace", "Class", "Method", overloadIndex),
    (void*) myHookFunction,
    (void**) &originalFunction
);
```
- The address is obtained by searching il2cpp metadata at runtime (by DLL name, namespace, class name, method name, overload index)
- The original function pointer is stored for calling through (trampoline pattern)

### Step 4: Hook Execution
Inside each hooked function, the C++ code either:
- **Modifies arguments** before calling the original (e.g., `hook_PlayerNetwork_TakeDamage` changes `firePos`/`hitPos`)
- **Replaces behavior entirely** based on toggle state (e.g., `_ResetGuest` bypasses the original)
- **Adds behavior** on top of the original call (e.g., `_LateUpdate` runs aimbot code then calls original)

---

## 11. Menu UI System (Jetpack Compose)

The menu UI has been fully rebuilt in **Jetpack Compose (Material 3)**, replacing the original 1091-line programmatic View system in `Menu.java`.

### View Hierarchy (Compose tree)
```
FloatingMenu()                           ← root composable in ComposeView
└── Box
    ├── AnimatedVisibility (collapsed)
    │   └── Box (65dp circle, "S3" icon)  ← pointerInput for drag + tap-to-open
    │
    └── AnimatedVisibility (expanded)
        └── Column (290dp wide)
            ├── MenuTitleBar()
            │   ├── Box (title centred + settings ⚙ gear at right)
            │   └── AndroidView { TextView (marquee subtitle) }
            ├── Box (scrollable body)
            │   └── Column (verticalScroll)
            │       └── FeatureRow() × N   ← one per item in GetFeatureList() / SettingsList()
            └── Row (CLOSE button)
```

### Key Compose Patterns Used
- **`AnimatedVisibility`** — fade + slide animations for collapsed↔expanded transitions
- **`LaunchedEffect` + `delay()`** — coroutine-based game lib polling (replaces Handler loops)
- **`mutableStateOf` / `remember`** — reactive UI state for each widget's on/off value
- **`animateColorAsState` / `animateFloatAsState`** — smooth ButtonOnOff color and scale transitions
- **`pointerInput` / `awaitPointerEventScope`** — low-level touch handling for the drag gesture
- **`AndroidView`** — embeds a classic `TextView` (marquee subtitle) and `WebView` (RichWebView) inside Compose
- **`ExposedDropdownMenuBox`** — Material 3 Spinner/dropdown
- **`Dialog` with `DialogWindowProvider`** — number/text input dialogs that work within the overlay window

### Collapsed Icon
A 65 dp `Box` clipped to `CircleShape` with a 2 dp `CBorder` (#32CB00) border. The "S3" text is rendered directly as a Compose `Text` at 28 sp Bold. No bitmap or asset is used.

---

## 12. Preferences & Persistence System

Feature states are saved in Android SharedPreferences under the key `featureNum.toString()`.

### Save/Load Flow
```
User toggles feature #8 (Enable Aim) to ON
→ ButtonOnOffRow.onClick → isOn = true
→ Preferences.changeFeatureBool("Enable Aim", 8, true)
  → sharedPreferences.edit().putBoolean("8", true).apply()
  → JNI: Changes(ctx, 8, "Enable Aim", 0, true, null)
     → C++: EnableAim = true

App restarts → FloatingMenu LaunchedEffect → parseFeatureList()
→ ButtonOnOffRow initial state:
    Preferences.loadPrefBool("Enable Aim", 8, false)
  → reads sharedPreferences.getBoolean("8", false)  → returns true
  → JNI: Changes(ctx, 8, "Enable Aim", 0, true, null)   ← re-syncs C++
  → Widget renders as ON
```

Persistence is only active when feature `-1` ("Save feature preferences") is enabled. When disabled, `loadPref = false` and reads always return defaults.

Clearing preferences: when feature `-1` is toggled OFF, `Preferences.with(ctx).clear()` wipes all saved values.

### Special Toggle `-3` ("Auto size vertically")
When enabled, the scroll area height switches from the fixed `Modifier.height(210.dp)` to `Modifier.weight(1f)`, making the menu expand to fill available vertical space. This state is tracked in `Preferences.isExpanded` (`@JvmField`) and reflected into `isScrollExpanded` Compose state in `ToggleRow`.

---

## 13. Security & Anti-Detection Mechanisms

### 1. Compile-Time String Obfuscation (AY Obfuscator)
Every string literal (class names, method names, library names, log tags) is wrapped in `OBFUSCATE("...")`. These are XOR-encrypted in the compiled `.so` binary. Static analysis of the binary will not reveal plain text strings.

The key is derived from `MurmurHash3` seeded by `__LINE__`, so each string has a different key.

### 2. Hidden Symbols (`-fvisibility=hidden`)
The compiler flag `-fvisibility=hidden` ensures no C++ symbols are exported from the library unless explicitly marked. This makes the library harder to analyze with tools like `readelf` or `nm`.

### 3. Symbol Stripping (`-s`, `--strip-all`)
Linker flags strip all debug symbols from the final `.so`, reducing binary size and removing function names.

### 4. Manual JNI Registration (non-standard naming)
Normally JNI methods follow `Java_<package>_<class>_<method>` naming. This project uses `RegisterNatives()` with obfuscated method name strings — meaning the native functions do not have the standard JNI names in the binary.

### 5. Package Name Masking
The app uses `package="com.android.support"` in the manifest — impersonating a common Android support library namespace.

### 6. Dead Code Sections
Several features are commented out in `hack_thread`:
```cpp
/*
if (mlovinit()) {
    setShader("_AlphaMask");
    LogShaders();
    Wallhack();
}
*/
```
The chams system exists in code but is not connected to any hook in the active flow (the OpenGL hooks are prepared but not installed in the final `hack_thread`).

---

## 14. Memory System (KittyMemory)

KittyMemory provides safe memory access that handles page protection:

### `memWrite(address, buffer, len)`
1. Gets page start address and length for the target range
2. Calls `mprotect(page, length, PROT_READ | PROT_WRITE | PROT_EXEC)` to make memory writable
3. Copies buffer with `memcpy`
4. Restores original protection
5. Flushes instruction cache (`__builtin___clear_cache`)

### `MemoryPatch` Object
```cpp
MemoryPatch patch;
patch = MemoryPatch::createWithHex(address, "90 90 90 90");  // NOP bytes
patch.Modify();    // Apply patch (saves original bytes)
patch.Restore();   // Restore original bytes
```

### `KittyScanner`
Scans loaded library regions for byte patterns. Useful for finding game functions when offsets change between game updates.

### `ProcMap`
Parses `/proc/self/maps` to get address ranges, permissions, and file names of all memory-mapped libraries. Used to find base addresses of `libil2cpp.so`, `libunity.so`, etc.

---

## 15. Il2Cpp Reflection System

### `Il2CppAttach(name)`
Opens `libil2cpp.so` via `dlopen` and resolves the following internal il2cpp API functions:
- `il2cpp_domain_get`
- `il2cpp_domain_get_assemblies`
- `il2cpp_assembly_get_image`
- `il2cpp_class_from_name`
- `il2cpp_class_get_method_from_name`
- `il2cpp_class_get_field_from_name`
- `il2cpp_field_get_offset`

### `Il2CppGetMethodOffset(image, namespace, class, method, argsCount)`
1. Gets the image (DLL) by name
2. Gets the class from the image by namespace + class name
3. Iterates method list, matching by name and parameter count
4. Returns the method's `methodPointer` — the actual function pointer to hook or call

### `Il2CppGetFieldOffset(image, namespace, class, field)`
Returns the byte offset of a field within the class's object layout. Used with pointer arithmetic to directly read/write fields: `*(bool*)((uintptr_t)obj + offset)`.

### `LoadClass` / `Field<T>` (AutoUpdate.h)
Higher-level wrappers:
```cpp
LoadClass playerClass("COW.GamePlay", "Player");
Field<bool> isBot = playerClass.GetFieldByName<bool>("IsClientBot");
isBot.clazz = playerInstance;
bool val = isBot.get();   // reads *(bool*)((uintptr_t)player + offset)
isBot.set(false);
```

---

## 16. Chams / Visual Hacks (OpenGL ES)

`chams.h` hooks into OpenGL ES 2.0 rendering by intercepting:
- `glGetUniformLocation` — used to identify the current shader program by looking for a named uniform
- `glDrawElements` — intercepts all triangle draw calls

### Detection Logic
Only processes draw calls where `mode == GL_TRIANGLES && count >= 1000` AND the current shader program contains the target uniform `shaderName` (default: `"_AlphaMask"`).

### Visual Effects (all applied in `new_glDrawElements`)

| Effect | Toggle | GL Operations |
|---|---|---|
| **Wallhack (Chams)** | `chams` | `glBlendColor`, `glBlendFunc(GL_CONSTANT_ALPHA, GL_CONSTANT_COLOR)` — makes enemies visible through geometry |
| **Wireframe** | `wireframe` | `glDepthRangef(1, 0.5)`, `glDrawElements(GL_LINE_LOOP, ...)` |
| **Shading** | `shading` | Depth override + custom blend equation for a shaded look |
| **Glow** | `glow` | Two-pass: lines + filled triangles with bloom colors |
| **Outline** | `outline` | Lines pass with black+color blending |
| **Rainbow** | `rainbow` | Animates RGB values in a cycle, applies via `glBlendColor` |
| **Clean Outline** | `cleanOutline` | Two-pass: depth mask + front-face culled colored triangles |

> **Note:** The chams hooks (`mlovinit()`, `LogShaders()`, `Wallhack()`) are commented out in `hack_thread` and not active in the current build. The code is present and functional but not connected.

---

## 17. Android Manifest & Permissions

**Package name:** `com.android.support`  
**Launcher Activity:** `com.android.support.MainActivity`  
**Service:** `com.android.support.Launcher` (`stopWithTask="true"` — stops when game is swiped from recents)

### Permissions Requested
| Permission | Purpose |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw the floating overlay on top of other apps |
| `INTERNET` | Network access (for potential online icon/update loading) |
| `WRITE_EXTERNAL_STORAGE` | Save crash logs to external storage |
| `READ_EXTERNAL_STORAGE` | Read from external storage |
| `ACCESS_NETWORK_STATE` | Check network connectivity |

---

## 18. App Lifecycle & Startup Flow

```
User taps app icon
        │
        ▼
MainActivity.onCreate()   [Kotlin Activity]
        │
        ├──► startActivity(UnityPlayerActivity)  ← launches the target game
        │
        └──► Main.Start(this)                    [Kotlin object]
                │
                └──► CrashHandler.init(context, false)   [Kotlin object]
                        │
                        └──► [JNI] CheckOverlayPermission(context)
                                │
                                ├── API < 23: → startService(Launcher)
                                │
                                └── API >= 23:
                                    ├── canDrawOverlays? YES → startService(Launcher)
                                    └── NO → Toast + open Settings + exit in 5s

                                         startService(Launcher)
                                                │
                                        Launcher.onCreate()         [Kotlin Service]
                                                │
                                        MenuOverlay(context)        [Kotlin class]
                                                │
                                        setWindowManagerWindowService()
                                                │
                                        buildAndAttachView()
                                          ├── Init() via JNI → titleState / subTitleState
                                          ├── OverlayLifecycleOwner.onCreate/Start/Resume
                                          ├── ComposeView.setContent { FloatingMenu(...) }
                                          └── windowManager.addView(composeView, params)
                                                │
                                        FloatingMenu (Compose)
                                          └── LaunchedEffect: polls IsGameLibLoaded() every 600ms
                                              → when loaded: parseFeatureList() → render widgets
                                                │
                                        Handler loop (1s) → checkGameForeground() → hide/show

Meanwhile (on a pthread):
        hack_thread()
                │
                ├── Poll until il2cpp + unity libs loaded (sleep 4s each poll)
                │
                ├── Il2CppAttach()
                ├── shadowhook_init()
                │
                └── Install all function hooks
```

---

## 19. Crash Handler

`CrashHandler.kt` (Kotlin `object`) registers a global `UncaughtExceptionHandler`. On any unhandled exception:

1. Generates timestamped filename: `mod_menu_crash_YYYY_MM_DD-HH_mm_ss.txt`
2. Writes structured crash report using Kotlin `buildString { appendLine(...) }`:
   ```
   ************* Crash Head ****************
   Time Of Crash      : 2026_07_16-13_45_00
   Device Manufacturer: samsung
   Device Model       : SM-G998B
   Android Version    : 12
   Android SDK        : 31
   App VersionName    : 2.9
   App VersionCode    : 1
   ************* Crash Head ****************
   
   <full Kotlin/Java stack trace>
   ```
3. Shows two Toast messages: "Game has crashed unexpectedly" + log file path (with `/storage/emulated/0/` stripped for readability)
4. Calls `System.exit(2)`

Save location:
- Android 11+ (API 30+): `/storage/emulated/0/Documents/`
- Older: `context.getExternalFilesDir(null)`

**`versionCode`** is read with `longVersionCode` on API 28+ and the deprecated `versionCode.toLong()` otherwise (guarded with `@Suppress("DEPRECATION")`).

---

## 20. Assets & Resources

### Font: `clerx.ttf`
A custom TrueType font stored in `assets/` under the name `clerx.ttf`. Available to load with `Typeface.createFromAsset()` in an `AndroidView` composable if needed for branding. The current Compose UI does not load it (default fonts are used).

### Icons: `ic_launcher` (mipmap densities)
Standard Android adaptive icons. The round and square variants exist in hdpi, mdpi, xhdpi, xxhdpi, xxxhdpi, plus an anydpi-v26 adaptive version.

### `ic_launcher_foreground.xml`
Vector drawable for the adaptive icon foreground layer.

### `strings.xml`
```xml
<string name="app_name">LGL Mod Menu</string>
```

### `styles.xml` / `colors.xml`
Standard Android theme/color resources. The overlay UI ignores these and uses hardcoded Compose `Color(0xFF...)` values.

---

## 21. How to Build (Android Studio / Codemagic)

### Building with Android Studio (PC — Recommended)

1. Open the project root in Android Studio **Flamingo or newer** (required for AGP 7.4.2)
2. Sync Gradle
3. Ensure NDK **r25c (`25.2.9519653`)** is installed via SDK Manager → SDK Tools → NDK (Specific version)
4. Ensure **Kotlin 1.8.22** and the **Compose compiler extension 1.4.8** are resolved (handled automatically by `build.gradle`)
5. **Build → Build APK(s)**

**SDK requirements:**
- `compileSdk 33` — Android 13 SDK required
- `minSdk 21` — device must run Android 5.0 or higher (Compose requirement; was 19 in the original Java version)
- `targetSdk 33`

### Building with Codemagic (CI/CD)
A `codemagic.yaml` is included in the project root. Push to the connected repository to trigger a cloud build. The pipeline handles NDK installation, Gradle sync, and APK signing automatically.

### Changing the Library Name
The native library is named `clerx` in `Android.mk` and loaded with `System.loadLibrary("clerx")` in `Main.kt`.
Both must match exactly.

### Adding a New Feature
1. Add a new entry to the `features[]` array in `GetFeatureList()` in `Main.cpp`:
   ```cpp
   OBFUSCATE("15_ButtonOnOff_My New Feature"),
   ```
2. Add the corresponding `case 15:` in `Changes()` in `Main.cpp`:
   ```cpp
   case 15:
       myNewFeatureBool = boolean;
       break;
   ```
3. Declare the global bool in `hook.h` or `Main.cpp`:
   ```cpp
   bool myNewFeatureBool = false;
   ```
4. Use `myNewFeatureBool` in any hook or `_LateUpdate` logic

The Kotlin/Compose UI is fully data-driven from `GetFeatureList()` — no Kotlin changes are needed to add new buttons.

---

## 22. How to Inject / Use

This mod menu is designed to be used as a **replacement app** — it wraps around the target game:

1. Uninstall the original game (or use a cloner app)
2. Install this APK on a rooted or patched device
3. Launch this app — it auto-launches the Unity game and starts the floating menu service
4. Grant overlay permission when prompted
5. The floating "S3" circle button appears — tap it to open the Compose menu
6. Toggle features on/off; they take effect immediately via the C++ hook layer

> **Note:** The target game's `libil2cpp.so` must be in the same process. This works when the APK is merged/repacked with the game, not as a standalone separate app.

---

## 23. Known Issues & Limitations

| Issue | Details |
|---|---|
| Shadowhook only has arm64 lib | `Shadowhook/libraries/` only contains `arm64-v8a/libShadowhook.a`. The ABI filter in `app/build.gradle` is now also `arm64-v8a` only, so this is consistent; builds for other ABIs are not supported |
| Chams system is commented out | `mlovinit()`, `LogShaders()`, `Wallhack()` in `hack_thread` are inside a comment block — chams UI buttons exist but do nothing |
| Fly Map (`case 14`) unmapped | Feature 14 is defined in `GetFeatureList()` but `case 14:` is missing from the `Changes()` switch — fly map can never be activated via UI |
| `MainCam` macro defined twice | `class.h` defines `#define MainCam` twice — the second definition wins, but the compiler may warn |
| Aimbot FOV stored × 100 | `Fov_Aim = 330.0f` but the comparison is `angle <= maxAngle` where `angle` is also multiplied by 100 — actual FOV is 3.3 degrees, which is very narrow |
| Hardcoded game class names | All offsets are resolved by string name from `Assembly-CSharp.dll`. If the game updates and renames obfuscated class names (e.g., `GPBDEDFKJNA`, `BLAGCMCGEJG`), hooks will silently fail |
| `hack_thread` spins on `sleep(4)` | If il2cpp never loads (wrong game), the thread loops forever consuming CPU |
| Rainbow chams bug | `if (getRainbow1Enabled)` — missing parentheses; `getRainbow1Enabled` is a function pointer, so this is always truthy regardless of the `enableRainbow1` flag value |
| Compose in overlay requires lifecycle owner | `OverlayLifecycleOwner` must be advanced manually through `onCreate → onStart → onResume`; forgetting any step will cause Compose recomposition or coroutines to silently stall |
| `minSdk` raised from 19 to 21 | Jetpack Compose requires API 21+. Devices running Android 4.4 KitKat (API 19–20) are no longer supported |
