package com.fitcoachpro.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fitcoachpro.app.data.PrefsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Defense-in-depth: WorkManager persists periodic work across reboots on its
 * own via its internal boot receiver, but some OEM battery-optimization
 * layers (notably aggressive ones on certain Android skins) are known to
 * interfere with background rescheduling after reboot. This receiver
 * re-enqueues the reminder from saved prefs as a belt-and-suspenders check -
 * see IMPLEMENTATION_STEPS.md Phase 7's explicit reboot-survival test note.
 */
class BootRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val prefsRepository = PrefsRepository(context.applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = prefsRepository.snapshotReminderEnabled()
                if (enabled) {
                    val hour = prefsRepository.snapshotReminderHour()
                    val minute = prefsRepository.snapshotReminderMinute()
                    ReminderScheduler.schedule(context.applicationContext, hour, minute)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
