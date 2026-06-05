package com.anshulpatro.floatinginspector

import android.content.Context
import android.os.Bundle
import java.io.BufferedReader
import java.io.InputStreamReader

internal object FirebaseLogcatTap {

    @Volatile
    private var started = false

    private val eventLine =
        Regex("""Logging event(?:\s*\([^)]*\))?:\s*([^,]+?),\s*Bundle\[\{(.*)\}\]""")

    @Synchronized
    fun start(context: Context) {
        if (started) return
        started = true
        val appContext = context.applicationContext
        Thread({ pump(appContext) }, "FloatingInspector-FA").apply {
            isDaemon = true
            start()
        }
    }

    private fun pump(context: Context) {
        try {
            val process = ProcessBuilder("logcat", "-v", "tag", "FA:V", "FA-SVC:V", "*:S")
                .redirectErrorStream(true)
                .start()
            BufferedReader(InputStreamReader(process.inputStream)).forEachLine { line ->
                val match = eventLine.find(line) ?: return@forEachLine
                FloatingInspectorAnalytics.capture(
                    match.groupValues[1].trim(),
                    parseParams(match.groupValues[2])
                )
            }
        } catch (ignored: Throwable) {
        }
    }

    private fun parseParams(body: String): Bundle {
        val bundle = Bundle()
        if (body.isBlank()) return bundle
        for (pair in body.split(", ")) {
            val separator = pair.indexOf('=')
            if (separator > 0) {
                bundle.putString(pair.substring(0, separator).trim(), pair.substring(separator + 1).trim())
            }
        }
        return bundle
    }
}
