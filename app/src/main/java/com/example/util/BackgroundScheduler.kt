package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.model.ShopSettings
import com.example.data.repository.MouryaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import android.Manifest
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.core.content.ContextCompat

data class AutoBackupRunResult(
    val success: Boolean,
    val backupFile: File?,
    val mobileFolderMessage: String,
    val driveUploadMessage: String,
    val errorMessage: String? = null
)

data class AutoBackupDiagnostics(
    val isEnabled: Boolean,
    val scheduledTime: String,
    val isAlarmRegistered: Boolean,
    val nextTriggerDisplay: String,
    val isBatteryOptimizationIgnored: Boolean,
    val isNotificationPermissionGranted: Boolean,
    val isCustomFolderSet: Boolean,
    val folderSummary: String,
    val isDriveWebhookConfigured: Boolean,
    val targetEmail: String,
    val lastRunTimeDisplay: String,
    val lastRunSource: String,
    val lastRunSuccess: Boolean?,
    val lastRunMessage: String,
    val lastBackupFileName: String,
    val lastBackupSizeKb: Long,
    val issues: List<String>,
    val isHealthy: Boolean
)

object BackgroundScheduler {

    private const val TAG = "BackgroundScheduler"
    private const val CHANNEL_ID = "mourya_auto_tasks"
    private const val PREFS_DIAG = "auto_backup_diag_prefs"
    private const val REQ_CODE_AUTO_BACKUP = 41001
    private const val REQ_CODE_AUTO_ATTENDANCE = 41002
    private const val REQ_CODE_ONE_MIN_TEST_BACKUP = 41099
    private const val NOTIF_ID_BACKUP = 51001
    private const val NOTIF_ID_ATTENDANCE = 51002

    const val ACTION_SCHEDULED_BACKUP = "com.example.action.RUN_SCHEDULED_BACKUP"
    const val ACTION_SCHEDULED_ATTENDANCE = "com.example.action.RUN_SCHEDULED_ATTENDANCE"
    const val EXTRA_IS_ONE_MIN_TEST = "is_one_min_test"

    /**
     * Ready-to-deploy 15-line Google Apps Script so backups upload silently to Google Drive
     * AND email the configured Gmail even when the Android app is completely closed!
     */
    fun getGoogleDriveAppsScriptTemplate(folderName: String, targetEmail: String): String {
        val safeFolder = folderName.ifBlank { "TilesAndMarbles_Backups" }.replace("\"", "")
        val safeEmail = targetEmail.trim().replace("\"", "")
        return """
function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var fileName = data.fileName || "backup_data.json";
    var folderName = data.folderName || "$safeFolder";
    var emailTo = data.email || "$safeEmail";
    var content = data.backupJson || "";

    var folders = DriveApp.getFoldersByName(folderName);
    var folder = folders.hasNext() ? folders.next() : DriveApp.createFolder(folderName);
    var existing = folder.getFilesByName(fileName);
    while (existing.hasNext()) { existing.next().setTrashed(true); }
    var file = folder.createFile(fileName, content, MimeType.PLAIN_TEXT);

    if (emailTo && emailTo.indexOf("@") > 0) {
      MailApp.sendEmail({
        to: emailTo,
        subject: "Mourya Tiles Auto-Backup: " + fileName,
        body: "Automatic Backup saved in Google Drive folder '" + folderName + "'.\nLink: " + file.getUrl(),
        attachments: [file.getBlob()]
      });
    }
    return ContentService.createTextOutput(JSON.stringify({ok: true, url: file.getUrl()})).setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ok: false, error: err.toString()})).setMimeType(ContentService.MimeType.JSON);
  }
}
        """.trimIndent()
    }

    fun syncAllSchedules(context: Context, settings: ShopSettings) {
        if (settings.autoBackupEnabled) {
            scheduleDailyAutoBackup(context, settings.backupScheduledTime)
        } else {
            cancelScheduledAutoBackup(context)
        }

        if (settings.autoAttendanceEnabled) {
            scheduleDailyAutoAttendance(context, settings.autoAttendanceTime)
        } else {
            cancelScheduledAutoAttendance(context)
        }
    }

    fun scheduleDailyAutoBackup(context: Context, time24h: String) {
        val triggerAt = computeNextTriggerMillis(time24h, defaultHour = 20, defaultMinute = 0)
        val intent = Intent(context.applicationContext, ScheduledBackupReceiver::class.java).apply {
            action = ACTION_SCHEDULED_BACKUP
        }
        val ok = scheduleIdleSafeAlarm(context.applicationContext, REQ_CODE_AUTO_BACKUP, intent, triggerAt)
        context.applicationContext.getSharedPreferences(PREFS_DIAG, Context.MODE_PRIVATE).edit()
            .putLong("next_backup_trigger_ms", if (ok) triggerAt else 0L)
            .putString("scheduled_time_label", time24h)
            .apply()
    }

    /**
     * Schedules a real AlarmManager test firing in exactly 60 seconds so the user can
     * close the app completely and confirm that background auto-backup works on their phone!
     */
    fun scheduleOneMinuteTestBackup(context: Context): Long {
        val triggerAt = System.currentTimeMillis() + 60_000L
        val intent = Intent(context.applicationContext, ScheduledBackupReceiver::class.java).apply {
            action = ACTION_SCHEDULED_BACKUP
            putExtra(EXTRA_IS_ONE_MIN_TEST, true)
        }
        scheduleIdleSafeAlarm(context.applicationContext, REQ_CODE_ONE_MIN_TEST_BACKUP, intent, triggerAt)
        context.applicationContext.getSharedPreferences(PREFS_DIAG, Context.MODE_PRIVATE).edit()
            .putLong("one_min_test_trigger_ms", triggerAt)
            .apply()
        return triggerAt
    }

    fun cancelScheduledAutoBackup(context: Context) {
        val intent = Intent(context.applicationContext, ScheduledBackupReceiver::class.java).apply {
            action = ACTION_SCHEDULED_BACKUP
        }
        cancelAlarm(context.applicationContext, REQ_CODE_AUTO_BACKUP, intent)
        context.applicationContext.getSharedPreferences(PREFS_DIAG, Context.MODE_PRIVATE).edit()
            .putLong("next_backup_trigger_ms", 0L)
            .apply()
    }

    fun scheduleDailyAutoAttendance(context: Context, time24h: String) {
        val triggerAt = computeNextTriggerMillis(time24h, defaultHour = 9, defaultMinute = 0)
        val intent = Intent(context.applicationContext, AutoAttendanceReceiver::class.java).apply {
            action = ACTION_SCHEDULED_ATTENDANCE
        }
        scheduleIdleSafeAlarm(context.applicationContext, REQ_CODE_AUTO_ATTENDANCE, intent, triggerAt)
    }

    fun cancelScheduledAutoAttendance(context: Context) {
        val intent = Intent(context.applicationContext, AutoAttendanceReceiver::class.java).apply {
            action = ACTION_SCHEDULED_ATTENDANCE
        }
        cancelAlarm(context.applicationContext, REQ_CODE_AUTO_ATTENDANCE, intent)
    }

    private fun scheduleIdleSafeAlarm(
        context: Context,
        requestCode: Int,
        intent: Intent,
        triggerAtMillis: Long
    ): Boolean {
        return try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
            alarmManager.cancel(pendingIntent)

            // setAlarmClock fires on the exact minute even in Deep Doze / closed app on Android 12–15
            // and does NOT require restricted SCHEDULE_EXACT_ALARM permission!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    val showIntent = PendingIntent.getActivity(
                        context,
                        requestCode + 1000,
                        Intent(context, MainActivity::class.java).apply {
                            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        },
                        flags
                    )
                    val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
                    alarmManager.setAlarmClock(clockInfo, pendingIntent)
                    return true
                } catch (e: Throwable) {
                    Log.w(TAG, "setAlarmClock fallback to setAndAllowWhileIdle", e)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to schedule alarm $requestCode", e)
            false
        }
    }

    private fun cancelAlarm(context: Context, requestCode: Int, intent: Intent) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (_: Throwable) {}
    }

    fun getAutoBackupDiagnostics(context: Context, settings: ShopSettings): AutoBackupDiagnostics {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_DIAG, Context.MODE_PRIVATE)
        val checkIntent = Intent(appCtx, ScheduledBackupReceiver::class.java).apply {
            action = ACTION_SCHEDULED_BACKUP
        }
        val existingPi = PendingIntent.getBroadcast(
            appCtx,
            REQ_CODE_AUTO_BACKUP,
            checkIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        val isAlarmRegistered = existingPi != null

        val nextTriggerMs = prefs.getLong("next_backup_trigger_ms", 0L)
        val nextTriggerDisplay = if (settings.autoBackupEnabled && nextTriggerMs > System.currentTimeMillis() - 60_000L) {
            SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(Date(nextTriggerMs))
        } else if (settings.autoBackupEnabled) {
            val computed = computeNextTriggerMillis(settings.backupScheduledTime, 20, 0)
            SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(Date(computed))
        } else {
            "बंद है (Disabled)"
        }

        val batteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val pm = appCtx.getSystemService(Context.POWER_SERVICE) as? PowerManager
                pm?.isIgnoringBatteryOptimizations(appCtx.packageName) == true
            } catch (_: Throwable) { true }
        } else true

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(appCtx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        val safSet = settings.backupFolderUri.isNotBlank()
        val folderSummary = if (safSet) {
            "${getReadableSafFolderName(settings.backupFolderUri)} + Downloads/${sanitizeFolderName(settings.backupFolderName)}"
        } else {
            "Downloads/${sanitizeFolderName(settings.backupFolderName)}"
        }

        val lastRunMs = prefs.getLong("last_run_ms", 0L)
        val lastRunDisplay = if (lastRunMs > 0L) {
            SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.ENGLISH).format(Date(lastRunMs))
        } else {
            "अभी तक कोई बैकअप नहीं चला"
        }
        val lastSource = prefs.getString("last_run_source", "") ?: ""
        val hasLastRun = prefs.contains("last_run_success")
        val lastSuccess = if (hasLastRun) prefs.getBoolean("last_run_success", false) else null
        val lastMsg = prefs.getString("last_run_msg", "") ?: ""
        val lastFileName = prefs.getString("last_file_name", "") ?: ""
        val lastSizeKb = prefs.getLong("last_size_kb", 0L)

        val issues = mutableListOf<String>()
        if (!settings.autoBackupEnabled) {
            issues.add("ऑटो-बैकअप स्विच बंद (OFF) है — इसे चालू करें।")
        } else if (!isAlarmRegistered) {
            issues.add("अलार्म सिस्टम में रजिस्टर नहीं मिला — 'सेव व अलार्म री-रजिस्टर करें' दबाएं।")
        }
        if (!batteryIgnored) {
            issues.add("बैटरी ऑप्टिमाइजेशन चालू है — कुछ फोन (Xiaomi/Vivo/Realme) ऐप बंद होने पर बैकअप रोक सकते हैं। नीचे बटन से 'Unrestricted' करें।")
        }
        if (!notifGranted) {
            issues.add("नोटिफिकेशन अनुमति बंद है — बैकअप पूरा होने का अलर्ट नहीं दिखेगा।")
        }
        if (settings.backupTargetEmail.isNotBlank() && settings.driveWebhookUrl.isBlank()) {
            issues.add("ईमेल लिखा है पर Google Drive Script URL खाली है — बिना Script URL के बैकअप मोबाइल फ़ोल्डर में सेव होगा और Drive पर 1-टैप नोटिफिकेशन से भेजना होगा।")
        }

        val healthy = settings.autoBackupEnabled && isAlarmRegistered
        return AutoBackupDiagnostics(
            isEnabled = settings.autoBackupEnabled,
            scheduledTime = settings.backupScheduledTime,
            isAlarmRegistered = isAlarmRegistered,
            nextTriggerDisplay = nextTriggerDisplay,
            isBatteryOptimizationIgnored = batteryIgnored,
            isNotificationPermissionGranted = notifGranted,
            isCustomFolderSet = safSet,
            folderSummary = folderSummary,
            isDriveWebhookConfigured = settings.driveWebhookUrl.isNotBlank(),
            targetEmail = settings.backupTargetEmail,
            lastRunTimeDisplay = lastRunDisplay,
            lastRunSource = lastSource,
            lastRunSuccess = lastSuccess,
            lastRunMessage = lastMsg,
            lastBackupFileName = lastFileName,
            lastBackupSizeKb = lastSizeKb,
            issues = issues,
            isHealthy = healthy
        )
    }

    private fun saveBackupDiagnostic(
        context: Context,
        source: String,
        success: Boolean,
        message: String,
        fileName: String = "",
        sizeKb: Long = 0L
    ) {
        try {
            context.applicationContext.getSharedPreferences(PREFS_DIAG, Context.MODE_PRIVATE).edit()
                .putLong("last_run_ms", System.currentTimeMillis())
                .putString("last_run_source", source)
                .putBoolean("last_run_success", success)
                .putString("last_run_msg", message)
                .putString("last_file_name", fileName)
                .putLong("last_size_kb", sizeKb)
                .apply()
        } catch (_: Throwable) {}
    }

    fun parseHourMinute(time24h: String, defaultHour: Int, defaultMinute: Int): Pair<Int, Int> {
        return try {
            val cleaned = time24h.trim().replace(".", ":").replace(" ", "")
            val parts = cleaned.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: defaultHour
            val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: defaultMinute
            Pair(h, m)
        } catch (_: Exception) {
            Pair(defaultHour, defaultMinute)
        }
    }

    fun isCurrentTimeAtOrPast(time24h: String, defaultHour: Int, defaultMinute: Int): Boolean {
        val (h, m) = parseHourMinute(time24h, defaultHour, defaultMinute)
        val now = Calendar.getInstance()
        val nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val schedMins = h * 60 + m
        return nowMins >= schedMins
    }

    private fun computeNextTriggerMillis(time24h: String, defaultHour: Int, defaultMinute: Int): Long {
        val (h, m) = parseHourMinute(time24h, defaultHour, defaultMinute)
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If target time already passed today (or within next 30 seconds), schedule for tomorrow
        if (target.timeInMillis <= now.timeInMillis + 30_000L) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    /**
     * Executes full Auto-Backup (including tile photos!), saves into user's custom mobile folder
     * (SAF selected folder + Downloads/<customFolder>), uploads to Google Drive/Email webhook if configured,
     * sends Telegram alert, and posts a notification with 1-tap Drive/Email share action.
     * Works identically whether app is open or closed!
     */
    suspend fun executeAutoBackupNow(
        context: Context,
        forceRun: Boolean = false,
        sourceLabel: String = if (forceRun) "MANUAL_TEST" else "AUTO_SCHEDULED"
    ): AutoBackupRunResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        try {
            val database = AppDatabase.getInstance(appContext)
            val settings = database.shopSettingsDao().getSettingsSync() ?: ShopSettings()
            val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date())

            if (!forceRun) {
                if (!settings.autoBackupEnabled) {
                    saveBackupDiagnostic(appContext, sourceLabel, false, "ऑटो-बैकअप स्विच बंद था")
                    return@withContext AutoBackupRunResult(false, null, "", "", "ऑटो-बैकअप बंद है")
                }
                if (settings.lastAutoBackupDate == todayStr) {
                    syncAllSchedules(appContext, settings)
                    return@withContext AutoBackupRunResult(true, null, "आज का बैकअप पहले ही हो चुका है", "")
                }
            }

            // 1. Create full cumulative JSON backup with embedded tile photos
            val backupFile = BackupRestoreHelper.createCumulativeBackupFile(
                appContext,
                database,
                settings.backupFromDateString
            )

            // 2. Save to user's customizable mobile folder (SAF tree URI and/or Downloads/<backupFolderName>)
            val customFolderName = sanitizeFolderName(settings.backupFolderName)
            val mobileFolderMsg = saveBackupToCustomMobileFolder(
                context = appContext,
                backupFile = backupFile,
                safTreeUriString = settings.backupFolderUri,
                customFolderName = customFolderName
            )

            // 3. Also refresh permanent vault snapshot silently
            try {
                val ownerKey = settings.ownerPhone.ifBlank { "default" }
                PermanentVaultHelper.saveShopSnapshot(appContext, database, ownerKey)
            } catch (_: Throwable) {}

            // 4. If user configured a Google Drive Apps Script URL, upload directly to Google Drive + Email in background!
            val driveResultMsg = if (settings.driveWebhookUrl.isNotBlank()) {
                uploadBackupToDriveWebhook(
                    webhookUrl = settings.driveWebhookUrl.trim(),
                    targetEmail = settings.backupTargetEmail.trim(),
                    folderName = customFolderName,
                    backupFile = backupFile
                )
            } else if (settings.backupTargetEmail.isNotBlank()) {
                "ईमेल/ड्राइव (${settings.backupTargetEmail}) के लिए नोटिफिकेशन से 1-टैप शेयर तैयार"
            } else {
                ""
            }

            // 5. CRITICAL FIX: Only stamp lastAutoBackupDate on real scheduled runs (NOT manual/1-min tests!)
            // so testing or changing the backup time never causes today's scheduled backup to be skipped!
            val updatedSettings = if (!forceRun) {
                val next = settings.copy(lastAutoBackupDate = todayStr)
                database.shopSettingsDao().saveSettings(next)
                next
            } else {
                settings
            }
            syncAllSchedules(appContext, updatedSettings)

            val sizeKb = (backupFile.length() / 1024L).coerceAtLeast(1L)
            val diagSummary = buildString {
                append("सेव हुआ: $mobileFolderMsg")
                if (driveResultMsg.isNotBlank()) append(" | $driveResultMsg")
            }
            saveBackupDiagnostic(
                context = appContext,
                source = sourceLabel,
                success = true,
                message = diagSummary,
                fileName = backupFile.name,
                sizeKb = sizeKb
            )

            // 6. Send Telegram alert if configured
            try {
                val adminConfig = database.adminGlobalConfigDao().getConfigSync()
                val tgConfig = TelegramActiveConfig.resolve(adminConfig, updatedSettings)
                if (tgConfig.isReady) {
                    val nowTimeStr = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date())
                    val msg = buildString {
                        append("📦 <b>ऑटो-बैकअप सुरक्षित हुआ ($sourceLabel)</b>\n")
                        append("फ़ाइल: <code>${backupFile.name}</code> ($sizeKb KB)\n")
                        append("समय: $todayStr • $nowTimeStr\n")
                        append("मोबाइल फ़ोल्डर: $mobileFolderMsg\n")
                        if (updatedSettings.backupTargetEmail.isNotBlank()) {
                            append("Google Drive / Email: ${updatedSettings.backupTargetEmail}\n")
                        }
                        if (driveResultMsg.isNotBlank()) {
                            append("ड्राइव स्टेटस: $driveResultMsg")
                        }
                    }
                    TelegramNotifier.sendActiveMessage(tgConfig, msg)
                }
            } catch (_: Throwable) {}

            // 7. Show system notification with 1-tap Drive/Email share action
            postBackupCompletedNotification(
                context = appContext,
                backupFile = backupFile,
                folderSummary = mobileFolderMsg,
                driveStatus = driveResultMsg,
                targetEmail = updatedSettings.backupTargetEmail
            )

            AutoBackupRunResult(
                success = true,
                backupFile = backupFile,
                mobileFolderMessage = mobileFolderMsg,
                driveUploadMessage = driveResultMsg
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Auto-backup execution failed", t)
            val errMsg = t.localizedMessage ?: "बैकअप बनाने में त्रुटि"
            saveBackupDiagnostic(appContext, sourceLabel, false, "त्रुटि: $errMsg")
            AutoBackupRunResult(
                success = false,
                backupFile = null,
                mobileFolderMessage = "",
                driveUploadMessage = "",
                errorMessage = errMsg
            )
        }
    }

    /**
     * Executes automatic daily staff attendance (marks all unmarked active staff PRESENT
     * using exact yyyy-MM-dd format so it displays immediately in register & salary reports).
     * Works identically whether app is open or closed!
     */
    suspend fun executeAutoAttendanceNow(
        context: Context,
        forceRun: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        try {
            val database = AppDatabase.getInstance(appContext)
            val repository = MouryaRepository(database)
            val settings = database.shopSettingsDao().getSettingsSync() ?: ShopSettings()
            val todayYyyyMmDd = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

            if (!forceRun && !settings.autoAttendanceEnabled) {
                return@withContext 0
            }

            val markedCount = repository.autoMarkPresentForDate(todayYyyyMmDd, System.currentTimeMillis())

            val updatedSettings = settings.copy(lastAutoAttendanceDate = todayYyyyMmDd)
            database.shopSettingsDao().saveSettings(updatedSettings)
            syncAllSchedules(appContext, updatedSettings)

            if (markedCount > 0) {
                try {
                    val adminConfig = database.adminGlobalConfigDao().getConfigSync()
                    val tgConfig = TelegramActiveConfig.resolve(adminConfig, updatedSettings)
                    if (tgConfig.isReady && tgConfig.alertAttendance) {
                        val dispDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                        TelegramNotifier.sendActiveMessage(
                            tgConfig,
                            "⚡ <b>ऑटो-हाजिरी दर्ज हुई ($dispDate)</b>\n$markedCount सक्रिय कर्मचारियों को आज <b>PRESENT</b> मार्क कर दिया गया है।"
                        )
                    }
                } catch (_: Throwable) {}

                postAttendanceCompletedNotification(appContext, markedCount, todayYyyyMmDd)
            }

            markedCount
        } catch (t: Throwable) {
            Log.e(TAG, "Auto-attendance execution failed", t)
            0
        }
    }

    fun sanitizeFolderName(raw: String): String {
        val cleaned = raw.trim()
            .replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
            .replace(Regex("\\s+"), "_")
            .trim('_')
        return cleaned.ifBlank { "TilesAndMarbles_Backups" }
    }

    fun getReadableSafFolderName(uriString: String?): String {
        if (uriString.isNullOrBlank()) return ""
        return try {
            val uri = Uri.parse(uriString)
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val afterColon = docId.substringAfter(":", docId)
            afterColon.ifBlank { "Selected Folder" }
        } catch (_: Exception) {
            "कस्टम चयनित फ़ोल्डर"
        }
    }

    /**
     * Saves the backup file (with embedded tile images) into:
     * 1) User's SAF-chosen custom folder (if granted), AND
     * 2) Public mobile Downloads/<customFolderName>/ folder via MediaStore (zero storage permission needed on Android 10+!).
     */
    fun saveBackupToCustomMobileFolder(
        context: Context,
        backupFile: File,
        safTreeUriString: String,
        customFolderName: String
    ): String {
        val savedLocations = mutableListOf<String>()
        val safeName = backupFile.name

        // 1. Write to user-picked SAF folder (any folder on phone/SD card!)
        if (safTreeUriString.isNotBlank()) {
            try {
                val treeUri = Uri.parse(safTreeUriString)
                val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)

                // Delete existing file with same name in that SAF folder if present
                try {
                    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)
                    context.contentResolver.query(
                        childrenUri,
                        arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val name = cursor.getString(1)
                            if (name.equals(safeName, ignoreCase = true)) {
                                val docId = cursor.getString(0)
                                val existingUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                                DocumentsContract.deleteDocument(context.contentResolver, existingUri)
                            }
                        }
                    }
                } catch (_: Throwable) {}

                val newDocUri = DocumentsContract.createDocument(
                    context.contentResolver,
                    parentDocUri,
                    "application/json",
                    safeName
                )
                if (newDocUri != null) {
                    context.contentResolver.openOutputStream(newDocUri, "wt")?.use { out ->
                        FileInputStream(backupFile).use { input ->
                            input.copyTo(out)
                        }
                        out.flush()
                    }
                    savedLocations.add("कस्टम फ़ोल्डर: ${getReadableSafFolderName(safTreeUriString)}")
                }
            } catch (e: Throwable) {
                Log.w(TAG, "SAF folder write failed, falling back to Downloads", e)
            }
        }

        // 2. Also write into Downloads/<customFolderName>/ using MediaStore (Android 10+) or public Downloads (Android 9-)
        val cleanFolder = sanitizeFolderName(customFolderName)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$cleanFolder/"
                val resolver = context.contentResolver

                // Remove same-day duplicate if exists
                try {
                    resolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                        arrayOf(safeName, relativePath)
                    )
                } catch (_: Throwable) {}

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(backupFile).use { input ->
                            input.copyTo(out)
                        }
                        out.flush()
                    }
                    savedLocations.add("Downloads/$cleanFolder")
                }
            } else {
                @Suppress("DEPRECATION")
                val dlDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    cleanFolder
                )
                if (!dlDir.exists()) dlDir.mkdirs()
                val dest = File(dlDir, safeName)
                FileInputStream(backupFile).use { input ->
                    FileOutputStream(dest).use { out ->
                        input.copyTo(out)
                    }
                }
                savedLocations.add("Downloads/$cleanFolder")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Downloads folder write failed", e)
        }

        return if (savedLocations.isEmpty()) {
            "App Storage/backups/$safeName"
        } else {
            savedLocations.joinToString(" + ")
        }
    }

    /**
     * Uploads backup JSON silently to user's Google Apps Script Web App URL
     * so it lands straight in Google Drive & emails targetEmail even when app is closed.
     */
    private fun uploadBackupToDriveWebhook(
        webhookUrl: String,
        targetEmail: String,
        folderName: String,
        backupFile: File
    ): String {
        return try {
            if (!webhookUrl.startsWith("https://")) {
                return "Drive URL https:// से शुरू होना चाहिए"
            }
            val payload = JSONObject().apply {
                put("fileName", backupFile.name)
                put("folderName", folderName)
                put("email", targetEmail)
                put("backupJson", backupFile.readText(Charsets.UTF_8))
            }.toString()

            val conn = (URL(webhookUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = true
                connectTimeout = 20_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload)
                writer.flush()
            }
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..399) {
                "✔ Google Drive व Email ($targetEmail) पर सफलतापूर्वक अपलोड हो गया!"
            } else {
                "⚠️ Drive अपलोड कोड $code (मोबाइल फ़ोल्डर में बैकअप सेव है)"
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Drive webhook upload failed", e)
            "⚠️ इंटरनेट/Drive लिंक त्रुटि (मोबाइल फ़ोल्डर में बैकअप सुरक्षित है)"
        }
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto Backup & Auto Attendance",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily scheduled backup and automatic staff attendance alerts"
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun postBackupCompletedNotification(
        context: Context,
        backupFile: File,
        folderSummary: String,
        driveStatus: String,
        targetEmail: String
    ) {
        try {
            ensureNotificationChannel(context)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                61001,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val shareIntent = BackupRestoreHelper.createShareBackupIntent(context, backupFile, targetEmail)
            val chooserIntent = Intent.createChooser(shareIntent, "Google Drive / Email पर बैकअप भेजें").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val sharePending = PendingIntent.getActivity(
                context,
                61002,
                chooserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val body = buildString {
                append("फ़ोल्डर: $folderSummary")
                if (driveStatus.isNotBlank()) {
                    append("\n$driveStatus")
                }
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("📦 ऑटो-बैकअप सुरक्षित हुआ (${backupFile.length() / 1024} KB)")
                .setContentText("फ़ोल्डर: $folderSummary")
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(openPending)
                .addAction(
                    android.R.drawable.ic_menu_share,
                    if (targetEmail.isNotBlank()) "Drive / Email पर भेजें" else "Drive पर सेव करें",
                    sharePending
                )

            manager.notify(NOTIF_ID_BACKUP, builder.build())
        } catch (t: Throwable) {
            Log.w(TAG, "Notification skipped (permission not granted or unavailable)", t)
        }
    }

    private fun postAttendanceCompletedNotification(
        context: Context,
        markedCount: Int,
        dateYyyyMmDd: String
    ) {
        try {
            ensureNotificationChannel(context)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                61003,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                .setContentTitle("⚡ ऑटो-हाजिरी दर्ज हो गई ($dateYyyyMmDd)")
                .setContentText("$markedCount सक्रिय कर्मचारियों की आज की हाजिरी (PRESENT) स्वतः लग गई है।")
                .setAutoCancel(true)
                .setContentIntent(openPending)

            manager.notify(NOTIF_ID_ATTENDANCE, builder.build())
        } catch (t: Throwable) {
            Log.w(TAG, "Attendance notification skipped", t)
        }
    }
}
