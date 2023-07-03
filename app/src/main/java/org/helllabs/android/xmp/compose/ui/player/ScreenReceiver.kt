package org.helllabs.android.xmp.compose.ui.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/*
 * From "Handling Screen OFF and Screen ON Intents" by jwei512
 * http://thinkandroid.wordpress.com/2010/01/24/handling-screen-off-and-screen-on-intents/
 */
class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        wasScreenOn = when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> false
            Intent.ACTION_SCREEN_ON -> true
            else -> return // Do nothing
        }
    }

    companion object {
        var wasScreenOn = true
    }
}
