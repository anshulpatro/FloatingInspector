# Changelog

Both artifacts, `floatinginspector` and `floatinginspector-noop`, are always released together at the same version.

## 0.2.1

No library or API changes since 0.2.0.

### Changed
- Built with Gradle 9.4.1 (was 8.13). The bytecode target (Java 8) and the public API are unchanged.

## 0.2.0

### Added
- **`com.anshulpatro:floatinginspector-noop`**: a no-op drop-in for release builds. It has the same `DebugOverlay` API, but every call returns immediately. It ships no service, no `SYSTEM_ALERT_WINDOW` permission, no resources and no dependencies. Recommended setup:
  ```kotlin
  debugImplementation("com.anshulpatro:floatinginspector:0.2.0")
  releaseImplementation("com.anshulpatro:floatinginspector-noop:0.2.0")
  ```
- The public API is now tracked with the Kotlin binary-compatibility validator (`api/*.api`), and the build fails if the two artifacts' APIs differ.

### Changed (breaking)
- `LoggingAdapter`, `SimpleAdapter` and `DebugOverlayService` (including `DebugOverlayService.DebugOverlayServiceBinder`) are now `internal`. They were never meant to be public, and `DebugOverlay` is now the whole public API. If you referenced them directly, use `DebugOverlay.with(context).log(...)` instead.

## 0.1.5 and earlier

See the git history.
