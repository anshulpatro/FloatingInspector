package com.anshulpatro.floatinginspector

import android.app.Application
import android.content.ContextWrapper
import android.os.Bundle
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoopDebugOverlayTest {

    private val app: Application = RuntimeEnvironment.getApplication()

    @Test
    fun withAlwaysReturnsTheSameInstance() {
        assertSame(DebugOverlay.with(app), DebugOverlay.with(app))
    }

    @Test
    fun everyLogOverloadReturnsTheSameInstanceForChaining() {
        val overlay = DebugOverlay.with(app)

        assertSame(overlay, overlay.log("message"))
        assertSame(overlay, overlay.log("count = %d", 3))
        assertSame(overlay, overlay.log("event", Bundle().apply { putString("key", "value") }))
        assertSame(overlay, overlay.log("event", null))
    }

    /** The real library calls String.format; the no-op must never crash a release build over a bad format string. */
    @Test
    fun malformedFormatStringsDoNotThrow() {
        DebugOverlay.with(app)
            .log("%d", "not a number")
            .log("%s and %s", "only one argument")
            .log("%q unknown conversion")
    }

    /** A ContextWrapper with no base context throws on almost any call, so this fails if the no-op touches the context. */
    @Test
    fun contextIsNeverUsed() {
        DebugOverlay.with(ContextWrapper(null)).log("message")
    }

    @Test
    fun neverBindsOrStartsAService() {
        DebugOverlay.with(app).log("message").log("event", Bundle())

        val shadowApp = shadowOf(app)
        assertTrue(shadowApp.boundServiceConnections.isEmpty())
        assertNull(shadowApp.nextStartedService)
    }
}
