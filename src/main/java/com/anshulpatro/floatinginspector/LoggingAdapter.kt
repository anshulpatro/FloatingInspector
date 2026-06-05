package com.anshulpatro.floatinginspector

import android.content.Context
import android.os.Build
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class LoggingAdapter(context: Context) : SimpleAdapter<String>(context) {

    override fun newView(type: Int, parent: ViewGroup): View =
        inflater.inflate(R.layout.debug_overlay_item_logmsg, parent, false)

    override fun bindView(position: Int, type: Int, view: View) {
        val tv = view as TextView
        tv.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(items[position], Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(items[position])
        }
    }
}
