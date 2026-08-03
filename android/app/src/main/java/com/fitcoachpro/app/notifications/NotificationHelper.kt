package com.fitcoachpro.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fitcoachpro.app.MainActivity
import com.fitcoachpro.app.R

const val CHECKIN_REMINDER_CHANNEL_ID = "checkin_reminder"
private const val CHECKIN_REMINDER_NOTIFICATION_ID = 1001

object NotificationHelper {

    /** Safe to call repeatedly - creating an existing channel is a no-op. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHECKIN_REMINDER_CHANNEL_ID,
            context.getString(R.string.checkin_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.checkin_reminder_channel_desc)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Shows the daily check-in reminder. Tapping it opens MainActivity, which
     * always starts on CheckInScreen (see MainActivity's nav host) - no
     * special deep link needed since check-in already is the home screen.
     */
    fun showCheckInReminder(context: Context) {
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHECKIN_REMINDER_CHANNEL_ID)
            // A real (if simple) vector notification icon, not a system
            // dialog icon - status-bar small icons get masked to a
            // monochrome silhouette at render time regardless, so
            // android.R.drawable.ic_dialog_info (a full-color system icon
            // never designed for this) rendered inconsistently across
            // versions. Swap for real app-branded art whenever you touch up
            // assets - see res/drawable/ic_notification.xml.
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time for your check-in")
            .setContentText("Log today's weight, sleep, and readiness for your coach.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // POST_NOTIFICATIONS permission is requested at launch in
        // MainActivity; if the user denied it, this call is a documented
        // no-op on API 33+ rather than a crash.
        NotificationManagerCompat.from(context).notify(CHECKIN_REMINDER_NOTIFICATION_ID, notification)
    }
}
