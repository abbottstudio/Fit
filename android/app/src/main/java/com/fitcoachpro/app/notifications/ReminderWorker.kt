package com.fitcoachpro.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Fires the daily check-in notification. Enqueued as a periodic (~24h) job
 * by ReminderScheduler. Note this is an approximate-time reminder, not an
 * exact alarm - WorkManager periodic work can drift by several minutes
 * around the target time (battery-friendly by design). If you later need
 * exact-time delivery, that's an AlarmManager.setExactAndAllowWhileIdle
 * change with its own SCHEDULE_EXACT_ALARM permission flow - out of scope
 * for Phase 1 per IMPLEMENTATION_STEPS.md.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        NotificationHelper.showCheckInReminder(applicationContext)
        return Result.success()
    }
}
