package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires at the user's scheduled daily auto-attendance time even when the app is completely closed.
 * Marks all unmarked active staff PRESENT for today (yyyy-MM-dd) and reschedules tomorrow's alarm.
 */
class AutoAttendanceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                BackgroundScheduler.executeAutoAttendanceNow(appContext, forceRun = false)
            } catch (_: Throwable) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
