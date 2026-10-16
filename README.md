# FloatingInspector

[![Maven Central](https://img.shields.io/maven-central/v/com.anshulpatro/floatinginspector?label=Maven%20Central&color=blue)](https://central.sonatype.com/artifact/com.anshulpatro/floatinginspector)
[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg)](https://developer.android.com/about/versions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A tiny, debug-only **floating overlay for printing live debug messages on-device** — no Android Studio attached, no logcat scrolling. You push messages from your code and they appear in a draggable bubble you can expand into a scrollable panel, right on top of your running app.

> **Heads up:** as of `0.1.1` FloatingInspector is a **manual** overlay — *you* decide what to show by calling `DebugOverlay`. There is no background log scraping or automatic Firebase capture; nothing runs until your first `log()` call.

---

## ✨ Why use it

- **Zero dependencies** — pure Android framework, nothing transitive added to your app.
- **One line to integrate** — a normal Gradle `implementation`. No Gradle plugin, no `init()`.
- **See state on the device** — great for QA, on-device demos, physical devices without a cable, or any flow that's awkward to inspect in logcat.
- **Tiny & passive** — does nothing until you call it, and a **no-op artifact** keeps it out of release builds entirely.

---

## 📦 Installation

### 1. Make sure Maven Central is a repository

```kotlin
// settings.gradle.kts → dependencyResolutionManagement { repositories { … } }
mavenCentral()
```

### 2. Add the dependency

Use the real overlay in debug builds and the **no-op** artifact in release builds:

```kotlin
// app/build.gradle.kts
dependencies {
    debugImplementation("com.anshulpatro:floatinginspector:0.2.0")
    releaseImplementation("com.anshulpatro:floatinginspector-noop:0.2.0")
}
```

<details>
<summary>Groovy DSL</summary>

```groovy
// app/build.gradle
dependencies {
    debugImplementation 'com.anshulpatro:floatinginspector:0.2.0'
    releaseImplementation 'com.anshulpatro:floatinginspector-noop:0.2.0'
}
```
</details>

`floatinginspector-noop` has exactly the same API, so your `DebugOverlay` calls compile unchanged, but every call returns immediately. It ships **no service, no `SYSTEM_ALERT_WINDOW` permission, no resources and no dependencies**, so none of the overlay ends up in your release APK or its manifest. Always use the same version for both artifacts.

> Got more build types or flavors? Add the matching configuration for each one, e.g. `stagingImplementation(...)` with whichever artifact that build should get. If you'd rather ship one artifact everywhere, `implementation("com.anshulpatro:floatinginspector:0.2.0")` still works. Then guard your calls with `BuildConfig.DEBUG` (see below).

| | |
|---|---|
| **minSdk** | 23+ |
| **Dependencies** | none (only the Kotlin stdlib your app already has) |
| **Release builds** | `floatinginspector-noop` — same API, does nothing |

---

## 🔐 One-time setup: "Display over other apps"

The overlay draws on top of your app via `SYSTEM_ALERT_WINDOW`. The library declares the permission; the user grants it once. Send them to the system screen from anywhere in your app:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
    startActivity(
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        ),
    )
}
```

> If the permission isn't granted, the first `DebugOverlay.with(context)` call throws because it can't bind the overlay service.

---

## 🚀 Usage

Call `DebugOverlay.with(context)` from anywhere you have a `Context`, then `log(...)`:

```kotlin
import com.anshulpatro.floatinginspector.DebugOverlay

// plain message
DebugOverlay.with(context).log("Checkout tapped")

// printf-style formatting
DebugOverlay.with(context).log("cart size = %d", items.size)

// a named event with parameters (rendered as: bold name · HH:mm:ss.SSS, then key = value lines)
DebugOverlay.with(context).log("add_to_cart", bundleOf("sku" to "ABC", "qty" to 2))
```

Calls are chainable and **thread-safe** — `log()` always dispatches to the main thread, and messages sent before the overlay service finishes binding are queued rather than dropped.

### Keep it out of release builds

With the `debugImplementation` / `releaseImplementation` setup above, there's nothing to do: release builds get the no-op and your calls do nothing.

One thing the no-op can't skip is building the arguments at your call site. String templates and `bundleOf(...)` still run before the empty `log()` is called. That's negligible for occasional messages; for hot paths (per-frame, tight loops), or if you ship the real artifact in every build, guard the call:

```kotlin
if (BuildConfig.DEBUG) {
    DebugOverlay.with(context).log("payment_started", bundleOf("amount" to amount))
}
```

### Mirroring Firebase Analytics events

Want your analytics events on screen? Log them at the call site alongside the real call:

```kotlin
firebaseAnalytics.logEvent(name, params)
if (BuildConfig.DEBUG) DebugOverlay.with(context).log(name, params)
```

A small helper keeps it DRY:

```kotlin
fun Context.logEvent(name: String, params: Bundle? = null) {
    firebaseAnalytics.logEvent(name, params)
    if (BuildConfig.DEBUG) DebugOverlay.with(this).log(name, params)
}
```

---

## 📖 API reference

`DebugOverlay` — obtained via `DebugOverlay.with(context)` (returns a process-wide singleton).

| Method | Description |
|---|---|
| `with(context): DebugOverlay` | Get the overlay instance, binding its service on first use. |
| `log(msg: String): DebugOverlay` | Append a plain string. |
| `log(format: String, vararg args: Any?): DebugOverlay` | `String.format`-style message. |
| `log(event: String, params: Bundle?): DebugOverlay` | Bold event name + timestamp, with `key = value` lines for each bundle entry. |

All `log(...)` overloads return the same instance, so calls can be chained.

`floatinginspector-noop` exposes this exact API, down to parameter names. In the no-op, `with()` ignores the context and never throws, and `log(...)` never formats its arguments.

---

## 👀 What you'll see

- A **draggable floating bubble** with a badge counting unseen messages.
- **Tap it** → a scrollable, auto-scrolling panel showing each message (name in bold + `HH:mm:ss.SSS`, parameters as `key = value`). Keeps the most recent **200** messages.
- **🔍 Search** — tap the search icon in the panel to filter entries by any text (case-insensitive); matches are highlighted and a live count of matching entries is shown.
- **Minimize** returns to the bubble; **Close** stops the overlay. Pressing **Back** closes search, then collapses the panel.

<!-- Tip: drop a demo GIF/screenshot here, e.g. ![demo](docs/demo.gif) -->

---

## 🛠 How it works

The library binds a small `Service` and draws the bubble/panel via `WindowManager`. Each `DebugOverlay.with(context).log(...)` call dispatches your message to that overlay on the main thread. There is **no background thread, no logcat reading, and no automatic capture** — it stays completely idle until you call `log()`.

---

## 🧯 Troubleshooting

**Overlay shows nothing?** Check, in order:

1. You're running a build that uses `floatinginspector`, not `floatinginspector-noop` (with the recommended setup, that means a **debug** build).
2. The **"Display over other apps"** permission is granted (`Settings.canDrawOverlays(context)` returns `true`).
3. You're actually calling `DebugOverlay.with(context).log(...)` (and not behind a `BuildConfig.DEBUG` guard in a release build).

**Duplicate class `com.anshulpatro.floatinginspector.DebugOverlay`?** Both artifacts ended up in the same build variant. Each variant should get exactly one of them; check for a plain `implementation(...)` alongside the `debug`/`release` lines.

---

## 🧪 Sample app

A runnable sample lives in [`sample/`](sample) — a tiny app with buttons that push messages and events to the overlay so you can try everything, including search.

```bash
# install & launch on a connected device / emulator
./gradlew :sample:installDebug
```

In the app: grant the overlay permission, tap the buttons (e.g. **Log 30 sample events**), open the floating bubble, then tap 🔍 and type to filter/highlight (try `purchase` or `item_1`).

The sample depends on the library by source, so it always reflects your local changes — no publishing needed. It uses the recommended setup: the real library (`:`) in debug and the no-op (`:noop`) in release, so `./gradlew :sample:installRelease` shows that nothing appears in release builds.

---

## 🤝 Building & publishing (contributors)

The repo has two published modules: the real library at the root and the no-op in [`noop/`](noop).

```bash
# Everything CI runs: lint, unit tests, API checks, sample manifest checks
./gradlew check :sample:assembleDebug :sample:assembleRelease

# Build + run a local install of both artifacts into ~/.m2
./gradlew publishToMavenLocal

# Publish a release to Maven Central (maintainers; requires credentials + signing key)
./gradlew publishToMavenCentral --no-configuration-cache
```

**Changing the public API?** Make the same change in both `DebugOverlay.kt` files (root `src/` and `noop/src/`), then run `./gradlew apiDump` and commit the updated `api/*.api` files. `check` fails if a dump is stale (`apiCheck`), if the two modules' APIs differ (`checkNoopApiParity`), or if the sample's [`ApiContract.kt`](sample/src/main/java/com/anshulpatro/floatinginspector/sample/ApiContract.kt) stops compiling against either module. That last check catches renamed parameters.

The build is pinned to **JDK 17** via [`gradle/gradle-daemon-jvm.properties`](gradle/gradle-daemon-jvm.properties), so the Gradle daemon uses JDK 17 regardless of your shell's default JDK. CI runs on every push and pull request ([`ci.yml`](.github/workflows/ci.yml)) and publishes both artifacts when a `v*` tag is pushed ([`publish.yml`](.github/workflows/publish.yml)).

To cut a release: bump `VERSION_NAME` in [`gradle.properties`](gradle.properties) (it versions both artifacts), update [`CHANGELOG.md`](CHANGELOG.md), commit, then `git tag vX.Y.Z && git push --tags`. (Maven Central versions are immutable — always bump.)

---

## 📄 License

[MIT](LICENSE) © Anshul Patro
