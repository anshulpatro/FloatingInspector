# FloatingInspector

A lightweight, debug-only **floating overlay that shows your app's Firebase Analytics events live, on-device** — no Android Studio, no logcat scrolling, no remote DebugView. Add one dependency, flip one device switch, and every event you send to Firebase appears in a draggable bubble you can expand into a scrollable panel.

- **Tiny** — pure Android framework, zero third-party dependencies.
- **One line to add** — a normal Gradle `implementation` dependency. No Gradle plugin, no code changes.
- **Safe by default** — active only in **debuggable** builds; inert (and code-stripped) in release.
- **Zero-config capture** — auto-starts itself; you don't call an `init()`.

---

## Requirements

| | |
|---|---|
| `minSdk` | 23+ |
| Firebase Analytics | already integrated in your app |
| Build type | **debuggable** (e.g. `debug`) — the overlay does nothing in release builds |

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

That's the entire integration. There is **nothing to initialize in code** — FloatingInspector installs itself automatically (via a `ContentProvider`) and only wakes up in debuggable builds.

---

## Setup — two one-time steps on the device

Because Firebase exposes no public hook for an app's own events, FloatingInspector reads them from Firebase's **own debug log output**. That output is off by default, so enable it once:

### 1. Turn on Firebase verbose logging

```bash
adb shell setprop log.tag.FA VERBOSE
adb shell setprop log.tag.FA-SVC VERBOSE
```

> These properties reset on reboot — re-run them after restarting the device/emulator. This is Firebase's own [documented debug switch](https://firebase.google.com/docs/analytics/debugview); an app cannot set it itself.

### 2. Grant the "Display over other apps" permission

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

Once both are done, run your app and trigger some analytics — events appear immediately.

---

## Usage

### Automatic capture (the default)

Nothing to do. Every `FirebaseAnalytics.logEvent(...)` your app (or any library it uses) sends is shown in the overlay automatically.

### Manual logging (optional)

You can also push your own messages to the overlay — handy for non-Firebase debug signals:

```kotlin
import com.anshulpatro.floatinginspector.DebugOverlay

DebugOverlay.with(context).log("Checkout tapped")
DebugOverlay.with(context).log("cart size = %d", items.size)
DebugOverlay.with(context).log("add_to_cart", bundleOf("sku" to "ABC", "qty" to 2))
```

### Turn it off at runtime

```kotlin
import com.anshulpatro.floatinginspector.FloatingInspectorAnalytics

FloatingInspectorAnalytics.enabled = false   // stop forwarding events to the overlay
```

---

## What you'll see

- A **draggable floating bubble** with a badge counting unseen events.
- **Tap it** → a full-screen **"Analytics Events"** panel with the live, auto-scrolling log (event name in bold + timestamp, parameters as `key = value`). Keeps the most recent **200** events.
- **Minimize** returns to the bubble; **Close** stops the overlay.

---

## How it works

1. A merged `ContentProvider` captures the application `Context` at startup and — **only in debuggable builds** — starts a daemon thread.
2. That thread reads the app's **own** logcat (`logcat … FA:V FA-SVC:V`), parses Firebase's `Logging event (FE): <name>, Bundle[{…}]` lines, and forwards them to the overlay.
3. The overlay binds a small `Service` and draws the bubble/panel via `WindowManager`.

No `READ_LOGS` permission is needed — an app can always read its own process's log output.

---

## Troubleshooting

**Overlay shows nothing?** Check, in order:

1. The build is **debuggable** (`BuildConfig.DEBUG == true`). Release builds are intentionally inert.
2. You ran `adb shell setprop log.tag.FA VERBOSE` (and `FA-SVC`) **since the last reboot**.
3. The **"Display over other apps"** permission is granted (`Settings.canDrawOverlays(context)` returns `true`).
4. Firebase Analytics is actually initialized and you're triggering events.

---

## Notes & limitations

- **Debug-only by design.** The capture path is gated on `ApplicationInfo.FLAG_DEBUGGABLE`, so it never runs for your users.
- **Depends on Firebase's log format.** Capture parses Firebase's verbose log lines; a future Firebase SDK that changes that format may require a library update.
- **One device switch required.** The `setprop` step is unavoidable for a no-plugin, runtime-only approach — it's the only way to observe events without a public Firebase listener.

---

## License

[MIT](LICENSE) © Anshul Patro
