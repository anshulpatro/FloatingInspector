package com.anshulpatro.floatinginspector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
internal class DebugOverlayView(context: Context) : FrameLayout(context) {

    var onClose: (() -> Unit)? = null

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val adapter = LoggingAdapter(context)

    private val bubbleSize = dp(52)
    private val bubbleBox = bubbleSize + dp(8)
    private val edgeInset = dp(16)
    private val statusBarHeight = statusBarHeight()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val screenWidth: Int
    private val screenHeight: Int

    private val listView: ListView
    private val searchInput: EditText
    private val matchCount: TextView
    private val searchRow: LinearLayout
    private val badge: TextView
    private val bubble: FrameLayout
    private val panel: LinearLayout
    private val windowParams: WindowManager.LayoutParams

    private var minimised = true
    private var searchOpen = false
    private var unseenCount = 0
    private var bubbleX: Int
    private var bubbleY: Int

    init {
        val size = Point()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getSize(size)
        screenWidth = size.x
        screenHeight = size.y
        bubbleX = screenWidth - bubbleBox - edgeInset
        bubbleY = screenHeight - bubbleBox - edgeInset

        listView = buildListView()
        searchInput = buildSearchInput()
        matchCount = buildMatchCount()
        searchRow = buildSearchRow(searchInput, matchCount)
        panel = buildPanel(listView, searchRow)
        badge = buildBadge()
        bubble = buildBubble(badge)

        addView(panel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(bubble, LayoutParams(bubbleBox, bubbleBox))
        panel.visibility = View.GONE

        windowParams = buildWindowParams()
        windowManager.addView(this, windowParams)
    }

    fun addMessage(msg: String) {
        val items = adapter.items
        items.add(msg)
        while (items.size > MAX_MESSAGES) {
            items.removeAt(0)
        }
        adapter.refresh()
        updateMatchCount()
        scrollToBottom()
        if (minimised) {
            unseenCount++
            updateBadge()
        }
    }

    fun hideView() {
        windowManager.removeView(this)
    }

    /** Closes search first, then collapses the panel, when the user presses Back. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            when {
                searchOpen -> {
                    closeSearch()
                    return true
                }
                !minimised -> {
                    collapse()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun expand() {
        if (!minimised) return
        minimised = false
        unseenCount = 0
        updateBadge()
        bubble.visibility = View.GONE
        panel.visibility = View.VISIBLE
        windowParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        windowParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        windowParams.x = 0
        windowParams.y = 0
        windowParams.flags = overlayFlags(focusable = true)
        windowManager.updateViewLayout(this, windowParams)
        scrollToBottom()
    }

    private fun collapse() {
        if (minimised) return
        minimised = true
        if (searchOpen) closeSearch()
        panel.visibility = View.GONE
        bubble.visibility = View.VISIBLE
        windowParams.width = bubbleBox
        windowParams.height = bubbleBox
        windowParams.x = bubbleX
        windowParams.y = bubbleY
        windowParams.flags = overlayFlags(focusable = false)
        windowManager.updateViewLayout(this, windowParams)
    }

    private fun toggleSearch() {
        if (searchOpen) closeSearch() else openSearch()
    }

    private fun openSearch() {
        searchOpen = true
        searchRow.visibility = View.VISIBLE
        searchInput.requestFocus()
        post { showKeyboard(searchInput) }
    }

    private fun closeSearch() {
        searchOpen = false
        searchInput.setText("")     // clears the query via the TextWatcher
        searchRow.visibility = View.GONE
        hideKeyboard()
    }

    private fun applySearch(query: String) {
        adapter.setQuery(query)
        updateMatchCount()
        scrollToBottom()
    }

    private fun updateMatchCount() {
        if (adapter.query.isEmpty()) {
            matchCount.visibility = View.GONE
        } else {
            matchCount.visibility = View.VISIBLE
            matchCount.text = adapter.count.toString()
        }
    }

    @Suppress("DEPRECATION")
    private fun buildWindowParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            bubbleBox, bubbleBox, type,
            overlayFlags(focusable = false),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bubbleX
            y = bubbleY
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
    }

    /**
     * The bubble must stay non-focusable so touches pass through to the app behind it; the
     * expanded panel must be focusable so the search field can receive the soft keyboard.
     */
    private fun overlayFlags(focusable: Boolean): Int {
        val base = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        return if (focusable) base else base or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    }

    private fun buildListView(): ListView = ListView(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
        cacheColorHint = Color.TRANSPARENT
        divider = ColorDrawable(COLOR_DIVIDER)
        dividerHeight = dp(1)
        isVerticalScrollBarEnabled = false
        transcriptMode = AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL
        isStackFromBottom = true
        adapter = this@DebugOverlayView.adapter
    }

    private fun buildPanel(list: ListView, search: LinearLayout): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_PANEL)
            setPadding(0, statusBarHeight, 0, 0)
            addView(
                buildTopBar(),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44))
            )
            addView(
                search,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                list,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            )
        }

    private fun buildTopBar(): LinearLayout {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(context).apply {
            text = "Debug Logs"
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
        }
        val titleParams =
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12)
            }
        bar.addView(title, titleParams)

        val search = buildBarIcon(R.drawable.debug_overlay_ic_search)
        search.setOnClickListener { toggleSearch() }
        bar.addView(search, barIconParams(dp(2)))

        val minimise = buildBarIcon(R.drawable.debug_overlay_ic_arrow_down)
        minimise.setOnClickListener { collapse() }
        bar.addView(minimise, barIconParams(dp(2)))

        val close = buildBarIcon(R.drawable.debug_overlay_ic_close)
        close.setOnClickListener { onClose?.invoke() }
        bar.addView(close, barIconParams(dp(12)))

        return bar
    }

    private fun buildSearchInput(): EditText = EditText(context).apply {
        hint = "Search logs"
        setHintTextColor(0x80FFFFFF.toInt())
        setTextColor(Color.WHITE)
        textSize = 13f
        setSingleLine()
        setBackgroundColor(Color.TRANSPARENT)
        val padV = dp(6)
        setPadding(0, padV, 0, padV)
        imeOptions = EditorInfo.IME_ACTION_SEARCH
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else {
                false
            }
        }
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applySearch(s?.toString().orEmpty())
            }
        })
    }

    private fun buildMatchCount(): TextView = TextView(context).apply {
        setTextColor(0xB3FFFFFF.toInt())
        textSize = 12f
        visibility = View.GONE
    }

    private fun buildSearchRow(input: EditText, count: TextView): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), dp(4))
            visibility = View.GONE
            addView(
                input,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
            addView(
                count,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dp(12) }
            )
        }

    private fun buildBarIcon(resId: Int): ImageView = ImageView(context).apply {
        setImageResource(resId)
        setColorFilter(Color.WHITE)
        scaleType = ImageView.ScaleType.FIT_CENTER
        val pad = dp(4)
        setPadding(pad, pad, pad, pad)
    }

    private fun barIconParams(endMargin: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(dp(30), dp(30)).apply { marginEnd = endMargin }

    private fun buildBadge(): TextView = TextView(context).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(COLOR_BADGE)
        }
        setTextColor(Color.WHITE)
        textSize = 10f
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER
        minWidth = dp(18)
        val padH = dp(4)
        setPadding(padH, 0, padH, 0)
        visibility = View.GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildBubble(badgeView: TextView): FrameLayout {
        val box = FrameLayout(context)

        val circle = ImageView(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(COLOR_BUBBLE)
            }
            setImageResource(R.drawable.debug_overlay_ic_list)
            setColorFilter(Color.WHITE)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val pad = dp(15)
            setPadding(pad, pad, pad, pad)
        }
        box.addView(circle, LayoutParams(bubbleSize, bubbleSize, Gravity.CENTER))
        box.addView(
            badgeView,
            LayoutParams(LayoutParams.WRAP_CONTENT, dp(18), Gravity.TOP or Gravity.END)
        )

        box.setOnTouchListener(dragListener())
        return box
    }

    private fun dragListener() = object : View.OnTouchListener {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var dragging = false

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = bubbleX
                    startY = bubbleY
                    dragging = false
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    if (!dragging && abs(dx) < touchSlop && abs(dy) < touchSlop) {
                        return true
                    }
                    dragging = true
                    bubbleX = clamp(startX + dx, 0, screenWidth - bubbleBox)
                    bubbleY = clamp(startY + dy, statusBarHeight, screenHeight - bubbleBox)
                    windowParams.x = bubbleX
                    windowParams.y = bubbleY
                    windowManager.updateViewLayout(this@DebugOverlayView, windowParams)
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        v.performClick()
                        expand()
                    }
                    return true
                }
            }
            return false
        }
    }

    private fun showKeyboard(view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun updateBadge() {
        if (unseenCount <= 0) {
            badge.visibility = View.GONE
        } else {
            badge.visibility = View.VISIBLE
            badge.text = if (unseenCount > 99) "99+" else unseenCount.toString()
        }
    }

    private fun scrollToBottom() {
        val count = adapter.count
        if (count > 0) {
            listView.setSelection(count - 1)
        }
    }

    private fun clamp(value: Int, min: Int, max: Int): Int = value.coerceIn(min, max)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    private fun statusBarHeight(): Int {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
    }

    companion object {
        private val COLOR_PANEL = 0xD9000000.toInt()
        private val COLOR_BUBBLE = 0xCC000000.toInt()
        private const val COLOR_DIVIDER = 0x26FFFFFF
        private val COLOR_BADGE = 0xFFFF3B30.toInt()
        private const val MAX_MESSAGES = 200
    }
}
