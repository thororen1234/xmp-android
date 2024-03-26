package org.helllabs.android.xmp.service.utils

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class Watchdog(private val timeout: Int) : Runnable {

    private var executor: ScheduledExecutorService? = null
    private var listener: OnTimeoutListener? = null
    private var running = false
    private var timer = timeout

    fun interface OnTimeoutListener {
        fun onTimeout()
    }

    fun setOnTimeoutListener(listener: OnTimeoutListener?) {
        this.listener = listener
    }

    override fun run() {
        if (--timer <= 0) {
            listener?.onTimeout()
            stop()
        }
    }

    fun start() {
        if (executor == null) {
            executor = Executors.newScheduledThreadPool(1)
        }

        running = true
        executor?.scheduleWithFixedDelay(this, 0, 1, TimeUnit.SECONDS)
    }

    fun stop() {
        running = false
        executor?.shutdown()
        executor = null
    }

    fun refresh() {
        timer = timeout
    }
}
