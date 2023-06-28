package org.helllabs.android.xmp.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class HeadsetPlugReceiver : BroadcastReceiver() {

    private var skip = true

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Timber.i("Action $action")
        if (intent.action == Intent.ACTION_HEADSET_PLUG) {
            if (skip) {
                skip = false
                return
            }
            val headsetState = intent.getIntExtra("state", -1)
            if (headsetState == 0) {
                Timber.i("Headset unplugged")
                state = HEADSET_UNPLUGGED
            } else if (headsetState == 1) {
                Timber.i("Headset plugged")
                state = HEADSET_PLUGGED
            }
        }
    }

    companion object {
        private const val TAG = "HeadsetPluginReceiver"
        const val HEADSET_UNPLUGGED = 0
        const val HEADSET_PLUGGED = 1
        const val NO_STATE = -1
        private var state = NO_STATE
        fun getState(): Int {
            return state
        }

        fun setState(state: Int) {
            Companion.state = state
        }
    }
}
