package com.anshulpatro.floatinginspector

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class LoggingAdapter(context: Context) : SimpleAdapter<String>(context) {

    /** Current search query; empty means "no search active". */
    var query: String = ""
        private set

    /** Positions in [items] whose text contains [query]. */
    private val matches = ArrayList<Int>()

    /** Index into [matches] of the focused match, or -1 when there are none. */
    private var currentMatch = -1

    /** Total number of matching entries. */
    val matchTotal: Int get() = matches.size

    /** 1-based ordinal of the focused match, or 0 when there are none. */
    val currentOrdinal: Int get() = if (currentMatch < 0) 0 else currentMatch + 1

    /** Position in [items] of the focused match, or -1 when there are none. */
    val currentMatchPosition: Int
        get() = if (currentMatch in matches.indices) matches[currentMatch] else -1

    fun setQuery(value: String) {
        query = value.trim()
        recomputeMatches(resetPosition = true)
    }

    /** Recompute matches after [items] changed; keeps the focused match when possible. */
    fun refresh() = recomputeMatches(resetPosition = false)

    /** Advances to the next match (wrapping) and returns its position in [items], or -1. */
    fun nextMatch(): Int {
        if (matches.isEmpty()) return -1
        currentMatch = (currentMatch + 1) % matches.size
        notifyDataSetChanged()
        return currentMatchPosition
    }

    /** Steps to the previous match (wrapping) and returns its position in [items], or -1. */
    fun previousMatch(): Int {
        if (matches.isEmpty()) return -1
        currentMatch = (currentMatch - 1 + matches.size) % matches.size
        notifyDataSetChanged()
        return currentMatchPosition
    }

    private fun recomputeMatches(resetPosition: Boolean) {
        matches.clear()
        if (query.isNotEmpty()) {
            items.forEachIndexed { index, item ->
                if (plainText(item).contains(query, ignoreCase = true)) matches.add(index)
            }
        }
        currentMatch = when {
            matches.isEmpty() -> -1
            resetPosition -> 0
            else -> currentMatch.coerceIn(0, matches.size - 1)
        }
        notifyDataSetChanged()
    }

    override fun newView(type: Int, parent: ViewGroup): View =
        inflater.inflate(R.layout.debug_overlay_item_logmsg, parent, false)

    override fun bindView(position: Int, type: Int, view: View) {
        val tv = view as TextView
        val spanned = fromHtml(items[position])
        tv.text = if (query.isEmpty()) {
            spanned
        } else {
            highlight(spanned, query, isCurrent = position == currentMatchPosition)
        }
    }

    private fun highlight(spanned: Spanned, needle: String, isCurrent: Boolean): CharSequence {
        val builder = SpannableStringBuilder(spanned)
        val haystack = spanned.toString()
        val background = if (isCurrent) HIGHLIGHT_CURRENT else HIGHLIGHT_BG
        var index = haystack.indexOf(needle, ignoreCase = true)
        while (index >= 0) {
            val end = index + needle.length
            builder.setSpan(BackgroundColorSpan(background), index, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(ForegroundColorSpan(HIGHLIGHT_FG), index, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            index = haystack.indexOf(needle, end, ignoreCase = true)
        }
        return builder
    }

    private fun plainText(html: String): String = fromHtml(html).toString()

    private fun fromHtml(html: String): Spanned =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }

    private companion object {
        val HIGHLIGHT_BG = 0xFFFFEB3B.toInt()       // every match — yellow
        val HIGHLIGHT_CURRENT = 0xFFFF9800.toInt()  // focused match — orange
        val HIGHLIGHT_FG = Color.BLACK
    }
}
