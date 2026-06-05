package com.anshulpatro.floatinginspector

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class DebugOverlayService : Service() {

    private val binder = DebugOverlayServiceBinder(this)
    private var view: DebugOverlayView? = null

    override fun onBind(intent: Intent?): IBinder = binder

    fun logMsg(msg: String) {
        val current = view ?: DebugOverlayView(applicationContext).also {
            it.onClose = {
                destroyView()
                stopSelf()
            }
            view = it
        }
        current.addMessage(msg)
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyView()
    }

    private fun destroyView() {
        view?.hideView()
        view = null
    }

    class DebugOverlayServiceBinder(val service: DebugOverlayService) : Binder()
}
