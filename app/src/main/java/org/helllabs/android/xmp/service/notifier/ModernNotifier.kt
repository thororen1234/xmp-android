package org.helllabs.android.xmp.service.notifier

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import org.helllabs.android.xmp.PrefManager
import androidx.media.app.NotificationCompat.MediaStyle as MediaStyle
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.service.receiver.NotificationActionReceiver
import org.helllabs.android.xmp.service.utils.QueueManager
import java.util.Locale

class ModernNotifier(private val service: PlayerService) {

    lateinit var queueManager: QueueManager

    private val icon = BitmapFactory.decodeResource(service.resources, R.drawable.icon)
    private val nextIntent = makePendingIntent(ACTION_NEXT)
    private val pauseIntent = makePendingIntent(ACTION_PAUSE)
    private val prevIntent = makePendingIntent(ACTION_PREV)
    private val stopIntent = makePendingIntent(ACTION_STOP)

    private val contentIntent: PendingIntent
        get() {
            val intent = Intent(service, PlayerActivity::class.java)
            return PendingIntent.getActivity(service, 0, intent, 0)
        }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = service.getString(R.string.notif_channel_name)
            val channelDescription = service.getString(R.string.notif_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
                description = channelDescription
            }

            (service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
                createNotificationChannel(channel)
            }
        }
    }

    private fun formatIndex(index: Int): String {
        return String.format(Locale.US, "%d/%d", index + 1, queueManager.size())
    }

    private fun makePendingIntent(action: String): PendingIntent {
        val intent = Intent(service, NotificationActionReceiver::class.java)
        intent.action = action
        return PendingIntent.getBroadcast(service, 0, intent, 0)
    }

    fun cancel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            service.stopForeground(true)
        }
    }

    fun notify(title: String, info: String, index: Int, type: Int) {
        val notification = NotificationCompat.Builder(service, "xmp-notification").apply {
            setContentTitle(
                title.trim().ifEmpty { "<untitled>" }
            )
            setContentText(
                if (type == TYPE_PAUSE) "(paused)" else info
            )
            setChannelId(CHANNEL_ID)
            setContentIntent(contentIntent)
            setLargeIcon(icon)
            setOngoing(true)
            setShowWhen(true)
            setSmallIcon(R.drawable.notification_icon)
            setSubText(formatIndex(index))
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            /** Ticker **/
            if (type == TYPE_TICKER) {
                setTicker("$title (${formatIndex(index)})")
            }

            /** Style **/
            val mediaStyle = MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setShowCancelButton(true)
                .setCancelButtonIntent(stopIntent)
            if (PrefManager.useMediaStyle) {
                mediaStyle.setMediaSession(service.mediaSession.sessionToken)
            }
            setStyle(mediaStyle)

            /** Actions **/
            addAction(R.drawable.ic_action_previous, "Previous", prevIntent)
            if (type == TYPE_PAUSE) {
                addAction(R.drawable.ic_action_play, "Play", pauseIntent)
            } else {
                addAction(R.drawable.ic_action_pause, "Pause", pauseIntent)
            }
            addAction(R.drawable.ic_action_next, "Next", nextIntent)
            addAction(R.drawable.ic_action_stop, "Stop", stopIntent)
        }

        service.startForeground(669, notification.build())
    }

    companion object {
        private const val CHANNEL_ID = "xmp"
        const val TYPE_TICKER = 1
        const val TYPE_PAUSE = 2
        const val ACTION_STOP = "org.helllabs.android.xmp.STOP"
        const val ACTION_PAUSE = "org.helllabs.android.xmp.PAUSE"
        const val ACTION_PREV = "org.helllabs.android.xmp.PREV"
        const val ACTION_NEXT = "org.helllabs.android.xmp.NEXT"
    }
}
