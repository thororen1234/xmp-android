package org.helllabs.android.xmp.service.notifier

import android.app.Service
import androidx.core.app.NotificationCompat
import org.helllabs.android.xmp.R

class LegacyNotifier(service: Service) : Notifier(service) {

    private val time: Long = System.currentTimeMillis()

    override fun notify(notifyTitle: String?, notifyInfo: String?, index: Int, type: Int) {
        var title = notifyTitle
        if (title != null && title.trim().isEmpty()) {
            title = "<untitled>"
        }
        val indexText = formatIndex(index)
        val builder = NotificationCompat.Builder(service)
            .setContentTitle(title)
            .setContentText(notifyInfo)
            .setContentInfo(indexText)
            .setContentIntent(contentIntent)
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(icon)
            .setOngoing(true)
            .setWhen(time)
            .addAction(R.drawable.ic_action_stop, "Stop", stopIntent)
        if (type == TYPE_PAUSE) {
            builder.addAction(R.drawable.ic_action_play, "Play", pauseIntent)
            builder.setContentText("(paused)")
        } else {
            builder.addAction(R.drawable.ic_action_pause, "Pause", pauseIntent)
        }
        builder.addAction(R.drawable.ic_action_next, "Next", nextIntent)
        if (type == TYPE_TICKER) {
            if (queueManager!!.size() > 1) {
                builder.setTicker("$title ($indexText)")
            } else {
                builder.setTicker(title)
            }
        }
        service.startForeground(NOTIFY_ID, builder.build())
    }
}
