# FloatingInspector

A lightweight, debug-only **floating overlay for printing debug messages live, on-device** — no Android Studio, no logcat scrolling. Add one dependency and push your own messages to a draggable bubble you can expand into a scrollable panel.

- **Tiny** — pure Android framework, zero third-party dependencies.
- **One line to add** — a normal Gradle `implementation` dependency. No Gradle plugin.
- **Simple** — you decide what to show; call `DebugOverlay` and print.

---

## Requirements

| | |
|---|---|
| `minSdk` | 23+ |
| Build type | **debuggable** recommended — keep the overlay out of release builds |

---

## Installation

### 1. Add the repository

**JitPack** (once the library is published with a Git tag):

```groovy
// settings.gradle  →  dependencyResolutionManagement { repositories { … } }
maven { url 'https://jitpack.io' }
```

**Local builds** (consuming it from your machine via `publishToMavenLocal`):

```groovy
mavenLocal()
```

### 2. Add the dependency

```groovy
// app/build.gradle
dependencies {
    // From a Maven repo / mavenLocal:
    implementation 'com.anshulpatro:floatinginspector:0.1.0'

    // …or from JitPack once published:
    // implementation 'com.github.<your-github-user>:FloatingInspector:0.1.0'
}
```

---

## Setup — grant the "Display over other apps" permission

The overlay draws on top of your app using `SYSTEM_ALERT_WINDOW`. The library declares the permission; the user grants it once. Send them to the system screen from anywhere in your debug build:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
    startActivity(
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
    )
}
```

---

## Usage

Push your own messages to the overlay from anywhere you have a `Context`:

```kotlin
import com.anshulpatro.floatinginspector.DebugOverlay

DebugOverlay.with(context).log("Checkout tapped")
DebugOverlay.with(context).log("cart size = %d", items.size)
DebugOverlay.with(context).log("add_to_cart", bundleOf("sku" to "ABC", "qty" to 2))
```

> Want to surface Firebase Analytics events? Log them yourself at the call site, e.g.
> `firebaseAnalytics.logEvent(name, params); DebugOverlay.with(context).log(name, params)`.

Guard the calls behind your own debug check (e.g. `if (BuildConfig.DEBUG)`) if you want to keep them out of release builds.

---

## What you'll see

- A **draggable floating bubble** with a badge counting unseen messages.
- **Tap it** → a full-screen panel with the live, auto-scrolling log (message in bold + timestamp, parameters as `key = value`). Keeps the most recent **200** messages.
- **Minimize** returns to the bubble; **Close** stops the overlay.

---

## How it works

The library binds a small `Service` and draws the bubble/panel via `WindowManager`. Each `DebugOverlay.with(context).log(...)` call dispatches your message to that overlay on the main thread. There is no background capture, no logcat reading, and nothing starts until you make the first `log()` call.

---

## Troubleshooting

**Overlay shows nothing?** Check, in order:

1. The **"Display over other apps"** permission is granted (`Settings.canDrawOverlays(context)` returns `true`).
2. You are actually calling `DebugOverlay.with(context).log(...)`.

---

## License

[MIT](LICENSE) © Anshul Patro
