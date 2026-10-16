package com.anshulpatro.floatinginspector

import android.content.Context
import android.os.Bundle

/**
 * No-op build of `DebugOverlay`, for release builds. It has the same public API as the real
 * one, but every call does nothing and returns immediately.
 *
 * Keep this file in step with the real `DebugOverlay`: same signatures *and* same parameter
 * names, since Kotlin callers may use named arguments. `./gradlew check` fails if the
 * signatures drift (checkNoopApiParity); the sample's `ApiContract.kt` covers parameter names.
 */
class DebugOverlay private constructor() {

    fun log(msg: String): DebugOverlay = this

    // Deliberately does not call String.format: a bad format string must never crash a release build.
    fun log(formattedMsg: String, vararg parameters: Any?): DebugOverlay = this

    fun log(event: String, params: Bundle?): DebugOverlay = this

    companion object {

        private val INSTANCE = DebugOverlay()

        /** Returns a shared instance. [context] is never used or retained. */
        @JvmStatic
        fun with(context: Context): DebugOverlay = INSTANCE
    }
}
