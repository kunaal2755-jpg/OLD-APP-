package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires at the user's scheduled daily time even when the app is completely closed.
 * Creates cumulative backup (including tile images), writes to user's custom mobile folder,
 * uploads to Google Drive/Email webhook if configured, notifies user, and schedules next day.
 */
class ScheduledBackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val isOneMinTest = intent?.getBooleanExtra(BackgroundScheduler.EXTRA_IS_ONE_MIN_TEST, false) == true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                BackgroundScheduler.executeAutoBackupNow(
                    context = appContext,
                    forceRun = isOneMinTest,
                    sourceLabel = if (isOneMinTest) "ONE_MIN_TEST" else "AUTO_SCHEDULED"
                )
            } catch (_: Throwable) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
