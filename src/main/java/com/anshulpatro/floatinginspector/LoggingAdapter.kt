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

    /** Current search query; empty means "show everything". */
    var query: String = ""
        private set

    /** The messages currently shown — all of [items], or only those matching [query]. */
    private val visible = ArrayList<String>()

    fun setQuery(value: String) {
        query = value.trim()
        refresh()
    }

    /** Recompute the visible list from [items] and [query]. Call after [items] changes. */
    fun refresh() {
        visible.clear()
        if (query.isEmpty()) {
            visible.addAll(items)
        } else {
            for (item in items) {
                if (plainText(item).contains(query, ignoreCase = true)) {
                    visible.add(item)
                }
            }
        }
        notifyDataSetChanged()
    }

    override fun getCount(): Int = visible.size

    override fun getItem(position: Int): String = visible[position]

    override fun newView(type: Int, parent: ViewGroup): View =
        inflater.inflate(R.layout.debug_overlay_item_logmsg, parent, false)

    override fun bindView(position: Int, type: Int, view: View) {
        val tv = view as TextView
        val spanned = fromHtml(visible[position])
        tv.text = if (query.isEmpty()) spanned else highlight(spanned, query)
    }

    private fun highlight(spanned: Spanned, needle: String): CharSequence {
        val builder = SpannableStringBuilder(spanned)
        val haystack = spanned.toString()
        var index = haystack.indexOf(needle, ignoreCase = true)
        while (index >= 0) {
            val end = index + needle.length
            builder.setSpan(BackgroundColorSpan(HIGHLIGHT_BG), index, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
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
        val HIGHLIGHT_BG = 0xFFFFEB3B.toInt()
        val HIGHLIGHT_FG = Color.BLACK
    }
}
