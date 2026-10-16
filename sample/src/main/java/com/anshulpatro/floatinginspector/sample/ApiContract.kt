package com.anshulpatro.floatinginspector.sample

import android.content.Context
import android.os.Bundle
import com.anshulpatro.floatinginspector.DebugOverlay

/**
 * Not called anywhere. It exists so the compiler checks the public API from a caller's side:
 * the debug variant compiles it against the real library and the release variant against the
 * no-op, so both must accept every overload, including with named arguments.
 * (`checkNoopApiParity` compares signatures but cannot see parameter names.)
 */
@Suppress("unused")
internal fun debugOverlayApiContract(context: Context) {
    DebugOverlay.with(context = context)
        .log(msg = "message")
        .log(formattedMsg = "%s", parameters = arrayOf<Any?>("argument"))
        .log(event = "event", params = Bundle())
        .log(event = "event", params = null)
}
