package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.model.ShopSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reschedules daily auto-backup and auto-attendance alarms after phone reboot or app update,
 * and catches up on missed runs if today's scheduled time already passed while phone was off.
 */
class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(appContext)
                val settings = db.shopSettingsDao().getSettingsSync() ?: ShopSettings()
                BackgroundScheduler.syncAllSchedules(appContext, settings)

                // Catch up auto-attendance if missed while phone was off
                val todayYyyyMmDd = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
                if (settings.autoAttendanceEnabled &&
                    settings.lastAutoAttendanceDate != todayYyyyMmDd &&
                    BackgroundScheduler.isCurrentTimeAtOrPast(settings.autoAttendanceTime, 9, 0)
                ) {
                    BackgroundScheduler.executeAutoAttendanceNow(appContext, forceRun = false)
                }

                // Catch up auto-backup if missed while phone was off
                val todayDdMmYyyy = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date())
                if (settings.autoBackupEnabled &&
                    settings.lastAutoBackupDate != todayDdMmYyyy &&
                    BackgroundScheduler.isCurrentTimeAtOrPast(settings.backupScheduledTime, 20, 0)
                ) {
                    BackgroundScheduler.executeAutoBackupNow(appContext, forceRun = false)
                }
            } catch (_: Throwable) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
