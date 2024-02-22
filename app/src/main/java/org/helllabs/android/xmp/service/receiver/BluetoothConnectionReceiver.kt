package org.helllabs.android.xmp.service.receiver

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class BluetoothConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Timber.i("Action $action")
        if (intent.action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) {
            val bluetoothState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1)
            Timber.i("Extra state: $bluetoothState")
            if (bluetoothState == BluetoothProfile.STATE_DISCONNECTING ||
                bluetoothState == BluetoothProfile.STATE_DISCONNECTED
            ) {
                Timber.i("Bluetooth state changed to disconnected")
                state = DISCONNECTED
            } else if (bluetoothState == BluetoothProfile.STATE_CONNECTED) {
                Timber.i("Bluetooth state changed to connected")
                state = CONNECTED
            }
        }
    }

    companion object {
        private const val TAG = "BluetoothConnectionReceiver"
        const val DISCONNECTED = 0
        const val CONNECTED = 1
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
