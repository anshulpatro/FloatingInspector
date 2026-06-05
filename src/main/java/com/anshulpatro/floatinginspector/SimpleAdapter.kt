package com.anshulpatro.floatinginspector

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

abstract class SimpleAdapter<D>(protected val context: Context) : BaseAdapter() {

    protected var inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    var items: MutableList<D> = ArrayList()

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): D = items[position]

    override fun getItemId(position: Int): Long = 0

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val type = getItemViewType(position)
        val view = convertView ?: newView(type, parent)
        bindView(position, type, view)
        return view
    }

    abstract fun newView(type: Int, parent: ViewGroup): View

    abstract fun bindView(position: Int, type: Int, view: View)
}
