package org.helllabs.android.xmp.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.helllabs.android.xmp.service.notifier.ModernNotifier
import org.helllabs.android.xmp.util.Log

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Action $action")
        when (action) {
            ModernNotifier.ACTION_STOP -> keyCode = STOP
            ModernNotifier.ACTION_PAUSE -> keyCode = PAUSE
            ModernNotifier.ACTION_NEXT -> keyCode = NEXT
            ModernNotifier.ACTION_PREV -> keyCode = PREV
        }
    }

    companion object {
        private const val TAG = "NotificationActionReceiver"
        const val NO_KEY = -1
        const val STOP = 1
        const val PAUSE = 2
        const val NEXT = 3
        const val PREV = 4
        private var keyCode = NO_KEY
        fun getKeyCode(): Int {
            return keyCode
        }

        fun setKeyCode(keyCode: Int) {
            Companion.keyCode = keyCode
        }
    }
}
