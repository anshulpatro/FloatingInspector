package com.anshulpatro.floatinginspector.sample

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.anshulpatro.floatinginspector.DebugOverlay
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private var tapCount = 0
    private lateinit var permissionButton: Button

    private val sampleEvents = listOf(
        "screen_view", "add_to_cart", "remove_from_cart", "begin_checkout",
        "purchase", "login", "sign_up", "search", "share", "select_item",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(32), dp(24), dp(32))
        }

        content.addView(heading("FloatingInspector"))
        content.addView(
            paragraph(
                "1. Grant the overlay permission.\n" +
                    "2. Tap the buttons to push messages to the floating bubble.\n" +
                    "3. Open the bubble and try the 🔍 search — type to filter and highlight."
            )
        )

        permissionButton = button("Grant \"Display over other apps\"") { requestOverlayPermission() }
        content.addView(permissionButton)

        content.addView(sectionLabel("Log messages"))
        content.addView(button("Simple message") {
            DebugOverlay.with(this).log("Button tapped — count = ${++tapCount}")
        })
        content.addView(button("Formatted message") {
            DebugOverlay.with(this).log("user %s opened screen #%d", "alice", ++tapCount)
        })
        content.addView(button("Event with parameters") {
            DebugOverlay.with(this).log(
                "add_to_cart",
                Bundle().apply {
                    putString("sku", "SKU-${1000 + tapCount}")
                    putInt("qty", (tapCount % 3) + 1)
                    putString("currency", "USD")
                },
            )
            tapCount++
        })

        content.addView(sectionLabel("Populate for search"))
        content.addView(button("Log 30 sample events") { logSampleEvents(30) })

        setContentView(ScrollView(this).apply { addView(content) })
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionButton()
    }

    private fun logSampleEvents(count: Int) {
        val overlay = DebugOverlay.with(this)
        for (i in 0 until count) {
            val name = sampleEvents[i % sampleEvents.size]
            overlay.log(
                name,
                Bundle().apply {
                    putString("id", "item_$i")
                    putInt("index", i)
                    putString("source", if (i % 2 == 0) "home" else "feed")
                },
            )
        }
        toast("Logged $count events — open the bubble and search, e.g. \"purchase\" or \"item_1\".")
    }

    private fun requestOverlayPermission() {
        if (canDrawOverlays()) {
            toast("Permission already granted.")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"),
                ),
            )
        }
    }

    private fun refreshPermissionButton() {
        if (canDrawOverlays()) {
            permissionButton.text = "Overlay permission granted ✓"
            permissionButton.isEnabled = false
        } else {
            permissionButton.text = "Grant \"Display over other apps\""
            permissionButton.isEnabled = true
        }
    }

    private fun canDrawOverlays(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    // ---- tiny programmatic-UI helpers -------------------------------------------------

    private fun heading(text: String) = TextView(this).apply {
        this.text = text
        textSize = 24f
        setTextColor(Color.BLACK)
    }

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(0xFF888888.toInt())
        setPadding(0, dp(20), 0, dp(4))
    }

    private fun paragraph(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(0xFF444444.toInt())
        setPadding(0, dp(12), 0, dp(8))
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(8) }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}
