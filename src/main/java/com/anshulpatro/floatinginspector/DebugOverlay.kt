package com.anshulpatro.floatinginspector

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import java.util.TreeSet

class DebugOverlay private constructor(context: Context) {

    private val messageDispatcher = MessageDispatcher()

    init {
        val intent = Intent(context, DebugOverlayService::class.java)
        val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder) {
                val service = (binder as DebugOverlayService.DebugOverlayServiceBinder).service
                messageDispatcher.setSink(service::logMsg)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                INSTANCE = null
            }
        }

        val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        if (!bound) {
            throw RuntimeException(
                "Could not bind the Service " + DebugOverlayService::class.java.simpleName +
                    " -  Is Service declared in Android manifest and is Permission SYSTEM_ALERT_WINDOW granted?"
            )
        }
    }

    fun log(msg: String): DebugOverlay {
        messageDispatcher.enqueueMessage(msg)
        return this
    }

    fun log(formattedMsg: String, vararg parameters: Any?): DebugOverlay =
        log(String.format(formattedMsg, *parameters))

    @Suppress("DEPRECATION")
    fun log(event: String, params: Bundle?): DebugOverlay {
        val sb = StringBuilder()
        sb.append("<b><font color='#ffcccc'>").append(event).append("  ·  ")
            .append(SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date()))
            .append("</font></b>")
        if (params != null) {
            for (key in TreeSet(params.keySet())) {
                sb.append("<br>").append(key).append(" = ").append(params.get(key))
            }
        }
        return log(sb.toString())
    }

    internal class MessageDispatcher {

        // log() may be called from any thread while setSink() drains the queue on the
        // main thread, so all access to `sink` and `messageQueue` is guarded by `this`.
        private var sink: ((String) -> Unit)? = null
        private val messageQueue: Queue<String> = LinkedList()
        private val mainThreadHandler = Handler(Looper.getMainLooper())

        fun setSink(sink: (String) -> Unit) {
            val pending: List<String>
            synchronized(this) {
                this.sink = sink
                pending = ArrayList(messageQueue)
                messageQueue.clear()
            }
            for (msg in pending) {
                dispatchOnMainUiThread(sink, msg)
            }
        }

        fun enqueueMessage(msg: String) {
            val sink = synchronized(this) {
                sink ?: run {
                    messageQueue.add(msg)
                    return
                }
            }
            dispatchOnMainUiThread(sink, msg)
        }

        private fun dispatchOnMainUiThread(sink: (String) -> Unit, message: String) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                sink(message)
            } else {
                mainThreadHandler.post { sink(message) }
            }
        }
    }

    companion object {

        @Volatile
        private var INSTANCE: DebugOverlay? = null

        @JvmStatic
        fun with(context: Context): DebugOverlay =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: DebugOverlay(context.applicationContext).also { INSTANCE = it }
            }
    }
}
