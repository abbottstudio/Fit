package com.fitcoachpro.app.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val REMINDER_WORK_NAME = "daily_checkin_reminder"

/**
 * Schedules/cancels the daily reminder. Uses a unique periodic work name so
 * re-saving Settings with a new time replaces the existing job rather than
 * stacking duplicates.
 *
 * Deliberately uses ExistingPeriodicWorkPolicy.REPLACE, not UPDATE: UPDATE
 * (added in WorkManager 2.8) preserves the ALREADY-SCHEDULED next run time
 * of existing periodic work and does not honor a new initial delay on an
 * update - so if you enqueue at 7:00am and then change to 8:00am, UPDATE can
 * leave the next fire at the original 7:00am. REPLACE cancels the existing
 * work and enqueues a fresh one, which does honor the new initial delay -
 * what "the user changed the reminder time" actually needs. REPLACE is
 * deprecated in favor of UPDATE for the general case, but UPDATE's specific
 * behavior around initial delay is exactly wrong for this use case.
 */
object ReminderScheduler {

    fun schedule(context: Context, hour: Int, minute: Int) {
        val initialDelayMillis = millisUntilNext(hour, minute)

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK_NAME)
    }

    private fun millisUntilNext(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
