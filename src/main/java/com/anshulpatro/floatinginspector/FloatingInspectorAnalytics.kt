package com.anshulpatro.floatinginspector

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.provider.Settings

object FloatingInspectorAnalytics {

    @JvmStatic
    @Volatile
    var enabled: Boolean = true

    @JvmStatic
    fun capture(event: String, params: Bundle?) {
        if (!enabled) return
        val ctx = FloatingInspectorContextHolder.appContext ?: return
        if ((ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(ctx)) return
        try {
            DebugOverlay.with(ctx).log(event, params)
        } catch (ignored: Throwable) {
        }
    }
}

internal object FloatingInspectorContextHolder {
    @Volatile
    var appContext: Context? = null
}
