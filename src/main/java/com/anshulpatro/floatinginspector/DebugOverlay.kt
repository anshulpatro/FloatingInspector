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
                messageDispatcher.setService(service)
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

    private class MessageDispatcher {

        private var service: DebugOverlayService? = null
        private val messageQueue: Queue<String> = LinkedList()
        private val mainThreadHandler = Handler(Looper.getMainLooper())

        fun setService(service: DebugOverlayService) {
            this.service = service
            for (msg in messageQueue) {
                dispatchOnMainUiThread(msg)
            }
        }

        fun enqueueMessage(msg: String) {
            if (service != null) {
                dispatchOnMainUiThread(msg)
            } else {
                messageQueue.add(msg)
            }
        }

        private fun dispatchOnMainUiThread(message: String) {
            val service = this.service ?: throw NullPointerException(
                DebugOverlayService::class.java.simpleName + " is null, but this should never be the case"
            )
            if (Looper.myLooper() == Looper.getMainLooper()) {
                service.logMsg(message)
            } else {
                mainThreadHandler.post { service.logMsg(message) }
            }
        }
    }

    companion object {

        private var INSTANCE: DebugOverlay? = null

        @JvmStatic
        fun with(context: Context): DebugOverlay =
            INSTANCE ?: DebugOverlay(context.applicationContext).also { INSTANCE = it }
    }
}
