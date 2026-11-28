package com.example.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.AdminGlobalConfig
import com.example.data.model.AppUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Collections

enum class CloudSessionState {
    VALID,
    BLOCKED,
    DELETED,
    UNKNOWN
}

object PermanentVaultHelper {

    private const val TAG = "PermanentVaultHelper"
    private const val VAULT_DIR_NAME = "TilesAndMarbles_Vault"
    private const val REGISTRY_FILE = "users_registry.json"
    private const val DELETED_USERS_FILE = "deleted_users_blacklist.json"
    private const val ADMIN_CONFIG_FILE = "admin_config.json"

    private val memoryDeletedPhones = Collections.synchronizedSet(mutableSetOf<String>())

    /**
     * Writes content to MediaStore Downloads / Documents (Android 10+)
     * MediaStore files are NEVER wiped when user clicks "Clear Data" in Android settings!
     */
    private fun writeToMediaStore(context: Context, fileName: String, content: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(fileName)

                var existingUri: Uri? = null
                try {
                    resolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                            existingUri = ContentUris.withAppendedId(queryUri, id)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "MediaStore query error", e)
                }

                val targetUri = existingUri ?: run {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$VAULT_DIR_NAME")
                    }
                    resolver.insert(queryUri, values)
                }

                if (targetUri != null) {
                    resolver.openOutputStream(targetUri, "rwt")?.use { os ->
                        os.write(content.toByteArray(Charsets.UTF_8))
                        os.flush()
                    }
                    Log.i(TAG, "Successfully wrote $fileName to MediaStore Downloads")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed writing $fileName to MediaStore", e)
            }
        }
    }

    /**
     * Reads content from MediaStore Downloads / Documents
     */
    private fun readFromMediaStore(context: Context, fileName: String): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(fileName)

                resolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        val uri = ContentUris.withAppendedId(queryUri, id)
                        resolver.openInputStream(uri)?.use { stream ->
                            val text = stream.bufferedReader().readText()
                            if (text.isNotBlank()) {
                                Log.i(TAG, "Successfully read $fileName from MediaStore")
                                return text
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading $fileName from MediaStore", e)
            }
        }
        return null
    }

    /**
     * Gets all possible public & persistent directories on device.
     */
    private fun getPublicVaultDirectories(context: Context): List<File> {
        val dirs = mutableListOf<File>()

        // 1. Standard Downloads & Documents public folders (Survives Clear Data!)
        try {
            val d1 = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), VAULT_DIR_NAME)
            d1.mkdirs()
            dirs.add(d1)
        } catch (_: Exception) {}

        try {
            val d2 = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), VAULT_DIR_NAME)
            d2.mkdirs()
            dirs.add(d2)
        } catch (_: Exception) {}

        // 2. Direct absolute paths (mount points on Android devices)
        val directPaths = listOf(
            "/sdcard/Download/$VAULT_DIR_NAME",
            "/sdcard/Documents/$VAULT_DIR_NAME",
            "/storage/emulated/0/Download/$VAULT_DIR_NAME",
            "/storage/emulated/0/Documents/$VAULT_DIR_NAME",
            "/sdcard/$VAULT_DIR_NAME",
            "/storage/emulated/0/$VAULT_DIR_NAME"
        )
        for (p in directPaths) {
            try {
                val f = File(p)
                f.mkdirs()
                dirs.add(f)
            } catch (_: Exception) {}
        }

        // 3. Fallback app directories
        try {
            val ext = File(context.getExternalFilesDir(null), "PermanentVault")
            ext.mkdirs()
            dirs.add(ext)
        } catch (_: Exception) {}

        try {
            val internal = File(context.filesDir, "PermanentVault")
            internal.mkdirs()
            dirs.add(internal)
        } catch (_: Exception) {}

        return dirs.distinctBy { it.absolutePath }
    }

    /**
     * Universal writer to permanent vault (MediaStore + all public directories)
     */
    private fun writeVaultFile(context: Context, fileName: String, content: String) {
        // 1. Write to MediaStore
        writeToMediaStore(context, fileName, content)

        // 2. Write to all file directories
        val dirs = getPublicVaultDirectories(context)
        for (d in dirs) {
            try {
                if (!d.exists()) d.mkdirs()
                val targetFile = File(d, fileName)
                FileOutputStream(targetFile).use { it.write(content.toByteArray(Charsets.UTF_8)) }
            } catch (_: Exception) {}
        }
    }

    /**
     * Universal reader from permanent vault (Direct files first, then MediaStore)
     */
    private fun readVaultFile(context: Context, fileName: String): String? {
        // 1. Try direct files
        val dirs = getPublicVaultDirectories(context)
        for (d in dirs) {
            try {
                val targetFile = File(d, fileName)
                if (targetFile.exists() && targetFile.length() > 0) {
                    val text = FileInputStream(targetFile).use { it.bufferedReader().readText() }
                    if (text.isNotBlank()) return text
                }
            } catch (_: Exception) {}
        }

        // 2. Try MediaStore
        val mediaStoreText = readFromMediaStore(context, fileName)
        if (!mediaStoreText.isNullOrBlank()) return mediaStoreText

        return null
    }

    fun isPhoneDeleted(phone: String, context: Context? = null): Boolean {
        val clean = phone.trim()
        if (clean.isBlank()) return false
        if (memoryDeletedPhones.contains(clean)) return true
        if (context != null) {
            val prefs = context.getSharedPreferences("vault_blacklist_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("deleted_$clean", false)) {
                memoryDeletedPhones.add(clean)
                return true
            }
            val content = readVaultFile(context, DELETED_USERS_FILE)
            if (!content.isNullOrBlank()) {
                try {
                    val arr = JSONArray(content)
                    for (i in 0 until arr.length()) {
                        val p = arr.getString(i).trim()
                        if (p.isNotBlank()) memoryDeletedPhones.add(p)
                    }
                    if (memoryDeletedPhones.contains(clean)) return true
                } catch (_: Exception) {}
            }
        }
        return false
    }

    suspend fun markPhoneDeleted(phone: String, context: Context? = null) = withContext(Dispatchers.IO) {
        val clean = phone.trim()
        if (clean.isBlank()) return@withContext
        memoryDeletedPhones.add(clean)
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences("vault_blacklist_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("deleted_$clean", true).apply()

                val currentList = mutableSetOf<String>()
                val content = readVaultFile(context, DELETED_USERS_FILE)
                if (!content.isNullOrBlank()) {
                    try {
                        val arr = JSONArray(content)
                        for (i in 0 until arr.length()) currentList.add(arr.getString(i).trim())
                    } catch (_: Exception) {}
                }
                currentList.add(clean)
                val outArr = JSONArray()
                for (p in currentList) outArr.put(p)
                writeVaultFile(context, DELETED_USERS_FILE, outArr.toString(2))
            } catch (e: Exception) {
                Log.w(TAG, "Failed writing deleted phone to vault", e)
            }
        }
    }

    suspend fun unmarkPhoneDeleted(phone: String, context: Context? = null) = withContext(Dispatchers.IO) {
        val clean = phone.trim()
        if (clean.isBlank()) return@withContext
        memoryDeletedPhones.remove(clean)
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences("vault_blacklist_prefs", Context.MODE_PRIVATE)
                prefs.edit().remove("deleted_$clean").apply()

                val currentList = mutableSetOf<String>()
                val content = readVaultFile(context, DELETED_USERS_FILE)
                if (!content.isNullOrBlank()) {
                    try {
                        val arr = JSONArray(content)
                        for (i in 0 until arr.length()) {
                            val p = arr.getString(i).trim()
                            if (p != clean) currentList.add(p)
                        }
                        val outArr = JSONArray()
                        for (p in currentList) outArr.put(p)
                        writeVaultFile(context, DELETED_USERS_FILE, outArr.toString(2))
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Saves all registered user credentials to permanent vault.
     */
    suspend fun saveUsersRegistry(context: Context, users: List<AppUser>) = withContext(Dispatchers.IO) {
        try {
            val validUsers = users.filter { !isPhoneDeleted(it.phone, context) }
            val array = JSONArray()
            for (u in validUsers) {
                val obj = JSONObject().apply {
                    put("id", u.id)
                    put("name", u.name)
                    put("shopName", u.shopName)
                    put("phone", u.phone)
                    put("password", u.password)
                    put("role", u.role)
                    put("planDays", u.planDays)
                    put("planName", u.planName)
                    put("amountCharged", u.amountCharged)
                    put("createdAt", u.createdAt)
                    put("expiresAt", u.expiresAt)
                    put("isActive", u.isActive)
                    put("notes", u.notes)
                    put("approvalStatus", u.approvalStatus)
                    put("address", u.address)
                    put("lastCloudBackupAt", u.lastCloudBackupAt)
                    put("cloudBackupData", u.cloudBackupData ?: "")
                }
                array.put(obj)
            }
            val json = array.toString(2)
            writeVaultFile(context, REGISTRY_FILE, json)
            Log.i(TAG, "Saved users registry (${validUsers.size} users) to permanent vault")

            // 2. Cloud sync (survives device wipes & multi-device login)
            try {
                val pasteUrl = uploadToPasteRs(json)
                if (!pasteUrl.isNullOrBlank()) {
                    publishKeyValToCloud("users_registry", pasteUrl)
                }
            } catch (ce: Exception) {
                Log.w(TAG, "Cloud sync users registry error", ce)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving users registry", e)
        }
    }

    /**
     * Loads registered users from permanent vault.
     */
    suspend fun loadUsersRegistry(context: Context): List<AppUser> = withContext(Dispatchers.IO) {
        var json = readVaultFile(context, REGISTRY_FILE)
        if (json.isNullOrBlank()) {
            json = fetchKeyValFromCloud("users_registry")
            if (!json.isNullOrBlank()) {
                writeVaultFile(context, REGISTRY_FILE, json)
            }
        }

        if (!json.isNullOrBlank()) {
            try {
                val array = JSONArray(json)
                val list = mutableListOf<AppUser>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val phone = obj.getString("phone")
                    if (isPhoneDeleted(phone, context)) continue
                    list.add(
                        AppUser(
                            id = obj.optLong("id", 0L),
                            name = obj.optString("name", "दुकानदार"),
                            shopName = obj.optString("shopName", "TILES AND MARBLES"),
                            phone = phone,
                            password = obj.getString("password"),
                            role = obj.optString("role", "CLIENT"),
                            planDays = obj.optInt("planDays", 365),
                            planName = obj.optString("planName", "1 Year (1 वर्ष)"),
                            amountCharged = obj.optDouble("amountCharged", 0.0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            expiresAt = obj.optLong("expiresAt", System.currentTimeMillis() + 365L * 86400000L),
                            isActive = obj.optBoolean("isActive", true),
                            notes = obj.optString("notes", ""),
                            approvalStatus = obj.optString("approvalStatus", "APPROVED"),
                            address = obj.optString("address", ""),
                            lastCloudBackupAt = obj.optLong("lastCloudBackupAt", 0L),
                            cloudBackupData = obj.optString("cloudBackupData").ifEmpty { null }
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    Log.i(TAG, "Loaded ${list.size} users from permanent vault")
                    return@withContext list
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed parsing users registry", e)
            }
        }
        return@withContext emptyList()
    }

    /**
     * Saves complete shop snapshot (staff, attendance, payments, products, customers, orders, settings)
     * to persistent vault. Survives OS Clear Data!
     */
    suspend fun saveShopSnapshot(context: Context, database: AppDatabase, userPhone: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val cleanPhone = userPhone.trim()
            if (cleanPhone.isBlank()) return@withContext Result.failure(IllegalArgumentException("Phone required"))

            // Export full backup with tile images for persistent MediaStore Vault (survives Clear Data!)
            val fullVaultJson = BackupRestoreHelper.exportDatabaseToJson(
                database = database,
                context = context,
                includeImages = true,
                maxImageDim = 800,
                jpegQuality = 75
            )
            // Lightweight JSON without large base64 blobs for SQLite TEXT column (avoids 2MB CursorWindow limit)
            val lightJson = BackupRestoreHelper.exportDatabaseToJson(
                database = database,
                context = context,
                includeImages = false
            )
            val now = System.currentTimeMillis()

            // 1. Update in local DB user entity (size-safe)
            try {
                database.appUserDao().saveCloudBackup(cleanPhone, lightJson, now)
            } catch (_: Exception) {}

            // 2. Write full backup (including tile photos) to vault under phone-specific & latest universal file
            writeVaultFile(context, "vault_${cleanPhone}.json", fullVaultJson)
            writeVaultFile(context, "vault_latest.json", fullVaultJson)

            // 3. Update users registry
            try {
                val allUsers = database.appUserDao().getAllUsersSync()
                if (allUsers.isNotEmpty()) {
                    saveUsersRegistry(context, allUsers)
                }
            } catch (_: Exception) {}

            // 4. Cloud sync for phone (try compact images first, fallback to lightJson if too large)
            try {
                val compactCloudJson = BackupRestoreHelper.exportDatabaseToJson(
                    database = database,
                    context = context,
                    includeImages = true,
                    maxImageDim = 360,
                    jpegQuality = 55
                )
                val cloudPayload = if (compactCloudJson.length <= 450_000) compactCloudJson else lightJson
                val pasteUrl = uploadToPasteRs(cloudPayload) ?: if (cloudPayload !== lightJson) uploadToPasteRs(lightJson) else null
                if (!pasteUrl.isNullOrBlank()) {
                    publishKeyValToCloud("vault_${cleanPhone}", pasteUrl)
                }
            } catch (ce: Exception) {
                Log.w(TAG, "Cloud sync snapshot error", ce)
            }

            Log.i(TAG, "Shop snapshot saved successfully for phone: $cleanPhone (${fullVaultJson.length} bytes)")
            Result.success("सुरक्षित बैकअप (टाइल फोटो सहित) वॉल्ट व क्लाउड में सेव हो गया!")
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving shop snapshot", e)
            Result.failure(e)
        }
    }

    /**
     * Restores complete shop snapshot from persistent vault.
     * When forceCloud is true (e.g. login on new device or manual cloud sync), cloud is queried first!
     */
    suspend fun restoreShopSnapshot(
        context: Context,
        database: AppDatabase,
        userPhone: String,
        forceCloud: Boolean = false
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val cleanPhone = userPhone.trim()
            var json: String? = null

            // 1. If forceCloud is requested (e.g. logging into new phone), fetch latest cloud snapshot first!
            if (forceCloud) {
                try {
                    json = fetchKeyValFromCloud("vault_${cleanPhone}")
                    if (!json.isNullOrBlank()) {
                        writeVaultFile(context, "vault_${cleanPhone}.json", json)
                        Log.i(TAG, "Fetched shop snapshot from cloud for $cleanPhone (${json.length} bytes)")
                    }
                } catch (ce: Exception) {
                    Log.w(TAG, "Cloud fetch in restoreShopSnapshot failed", ce)
                }
            }

            // 2. Try phone-specific vault file (MediaStore & direct files)
            if (json.isNullOrBlank()) {
                json = readVaultFile(context, "vault_${cleanPhone}.json")
            }

            // 3. Try universal latest vault file
            if (json.isNullOrBlank()) {
                json = readVaultFile(context, "vault_latest.json")
            }

            // 4. Try DB user record
            if (json.isNullOrBlank()) {
                val user = database.appUserDao().getUserByPhone(cleanPhone)
                if (!user?.cloudBackupData.isNullOrBlank()) {
                    json = user?.cloudBackupData
                }
            }

            // 5. Try Cloud Vault if not attempted yet
            if (json.isNullOrBlank() && !forceCloud) {
                json = fetchKeyValFromCloud("vault_${cleanPhone}")
                if (!json.isNullOrBlank()) {
                    writeVaultFile(context, "vault_${cleanPhone}.json", json)
                }
            }

            if (!json.isNullOrBlank()) {
                val res = BackupRestoreHelper.restoreFromJsonString(database, json, context)
                if (res.isSuccess) {
                    Log.i(TAG, "Successfully restored shop snapshot for $cleanPhone")
                    Result.success(true)
                } else {
                    Result.failure(Exception(res.exceptionOrNull()?.message ?: "रीस्टोर विफल"))
                }
            } else {
                Log.w(TAG, "No vault snapshot found for phone: $cleanPhone")
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed restoring shop snapshot", e)
            Result.failure(e)
        }
    }

    /**
     * Saves admin config to vault
     */
    suspend fun saveAdminConfig(context: Context, config: AdminGlobalConfig) = withContext(Dispatchers.IO) {
        try {
            val obj = JSONObject().apply {
                put("isMaintenanceMode", config.isMaintenanceMode)
                put("maintenanceMessage", config.maintenanceMessage)
                put("allowUserSelfRegistration", config.allowUserSelfRegistration)
                put("requireAdminApproval", config.requireAdminApproval)
                put("defaultTrialDays", config.defaultTrialDays)
                put("allowInvoiceDeletion", config.allowInvoiceDeletion)
                put("allowBillingDiscounts", config.allowBillingDiscounts)
                put("enforceSingleDailyAttendance", config.enforceSingleDailyAttendance)
                put("allowSalaryAdvance", config.allowSalaryAdvance)
                put("lowStockThreshold", config.lowStockThreshold)
                put("broadcastAnnouncement", config.broadcastAnnouncement)
                put("showBroadcastAnnouncement", config.showBroadcastAnnouncement)
                put("customAdminPin", config.customAdminPin)
                put("adminPhone", config.adminPhone)
                put("adminWhatsapp", config.adminWhatsapp)
                put("adminEmail", config.adminEmail)
                put("recycleBinRetentionDays", config.recycleBinRetentionDays)
                put("brandAppName", config.brandAppName)
                put("latestAppVersionCode", config.latestAppVersionCode)
                put("latestAppVersionName", config.latestAppVersionName)
                put("latestApkDownloadUrl", config.latestApkDownloadUrl)
                put("updateChangelog", config.updateChangelog)
                put("isForceUpdate", config.isForceUpdate)
                put("telegramEnabled", config.telegramEnabled)
                put("telegramBotToken", config.telegramBotToken)
                put("telegramChatId", config.telegramChatId)
                put("telegramFormat", config.telegramFormat)
                put("telegramCustomTitle", config.telegramCustomTitle)
                put("telegramAlertNewSale", config.telegramAlertNewSale)
                put("telegramAlertPayment", config.telegramAlertPayment)
                put("telegramAlertLowStock", config.telegramAlertLowStock)
                put("telegramAlertAttendance", config.telegramAlertAttendance)
                put("telegramAlertSalaryAdvance", config.telegramAlertSalaryAdvance)
                put("telegramAlertNewProduct", config.telegramAlertNewProduct)
                put("telegramAlertInvoiceDeleted", config.telegramAlertInvoiceDeleted)
                put("telegramAlertUserAction", config.telegramAlertUserAction)
            }
            writeVaultFile(context, ADMIN_CONFIG_FILE, obj.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving admin config", e)
        }
    }

    /**
     * Restores admin config from vault
     */
    suspend fun loadAdminConfig(context: Context): AdminGlobalConfig? = withContext(Dispatchers.IO) {
        val json = readVaultFile(context, ADMIN_CONFIG_FILE)
        if (!json.isNullOrBlank()) {
            try {
                val obj = JSONObject(json)
                return@withContext AdminGlobalConfig(
                    isMaintenanceMode = obj.optBoolean("isMaintenanceMode", false),
                    maintenanceMessage = obj.optString("maintenanceMessage", "⚠️ सिस्टम में तकनीकी रखरखाव चल रहा है।"),
                    allowUserSelfRegistration = obj.optBoolean("allowUserSelfRegistration", true),
                    requireAdminApproval = obj.optBoolean("requireAdminApproval", true),
                    defaultTrialDays = obj.optInt("defaultTrialDays", 30),
                    allowInvoiceDeletion = obj.optBoolean("allowInvoiceDeletion", true),
                    allowBillingDiscounts = obj.optBoolean("allowBillingDiscounts", true),
                    enforceSingleDailyAttendance = obj.optBoolean("enforceSingleDailyAttendance", true),
                    allowSalaryAdvance = obj.optBoolean("allowSalaryAdvance", true),
                    lowStockThreshold = obj.optInt("lowStockThreshold", 10),
                    broadcastAnnouncement = obj.optString("broadcastAnnouncement", ""),
                    showBroadcastAnnouncement = obj.optBoolean("showBroadcastAnnouncement", false),
                    customAdminPin = obj.optString("customAdminPin", "8899"),
                    adminPhone = obj.optString("adminPhone", "+91 98765 43210"),
                    adminWhatsapp = obj.optString("adminWhatsapp", "9876543210"),
                    adminEmail = obj.optString("adminEmail", "cp622499@gmail.com"),
                    recycleBinRetentionDays = obj.optInt("recycleBinRetentionDays", 10),
                    brandAppName = obj.optString("brandAppName", "TILES AND MARBLES"),
                    latestAppVersionCode = obj.optInt("latestAppVersionCode", 1),
                    latestAppVersionName = obj.optString("latestAppVersionName", "1.0.0"),
                    latestApkDownloadUrl = obj.optString("latestApkDownloadUrl", ""),
                    updateChangelog = obj.optString("updateChangelog", "नई सुविधाएं व बग फिक्स"),
                    isForceUpdate = obj.optBoolean("isForceUpdate", false),
                    telegramEnabled = obj.optBoolean("telegramEnabled", false),
                    telegramBotToken = obj.optString("telegramBotToken", ""),
                    telegramChatId = obj.optString("telegramChatId", ""),
                    telegramFormat = obj.optString("telegramFormat", "HINDI_DETAILED"),
                    telegramCustomTitle = obj.optString("telegramCustomTitle", "TILES AND MARBLES"),
                    telegramAlertNewSale = obj.optBoolean("telegramAlertNewSale", true),
                    telegramAlertPayment = obj.optBoolean("telegramAlertPayment", true),
                    telegramAlertLowStock = obj.optBoolean("telegramAlertLowStock", true),
                    telegramAlertAttendance = obj.optBoolean("telegramAlertAttendance", true),
                    telegramAlertSalaryAdvance = obj.optBoolean("telegramAlertSalaryAdvance", true),
                    telegramAlertNewProduct = obj.optBoolean("telegramAlertNewProduct", true),
                    telegramAlertInvoiceDeleted = obj.optBoolean("telegramAlertInvoiceDeleted", true),
                    telegramAlertUserAction = obj.optBoolean("telegramAlertUserAction", true)
                )
            } catch (_: Exception) {}
        }
        return@withContext null
    }

    /**
     * Auto restore users and admin config on app launch after Clear Data
     */
    suspend fun autoRestoreOnLaunch(context: Context, database: AppDatabase) = withContext(Dispatchers.IO) {
        try {
            val localUsers = database.appUserDao().getAllUsersSync()
            if (localUsers.isEmpty()) {
                val vaultUsers = loadUsersRegistry(context)
                if (vaultUsers.isNotEmpty()) {
                    for (u in vaultUsers) {
                        if (!isPhoneDeleted(u.phone, context)) {
                            database.appUserDao().insert(u)
                        }
                    }
                    Log.i(TAG, "Restored users from vault after Clear Data")
                }
            }

            val localAdminConfig = database.adminGlobalConfigDao().getConfigSync()
            if (localAdminConfig == null) {
                val vaultConfig = loadAdminConfig(context)
                if (vaultConfig != null) {
                    database.adminGlobalConfigDao().saveConfig(vaultConfig)
                } else {
                    database.adminGlobalConfigDao().saveConfig(AdminGlobalConfig())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed autoRestoreOnLaunch", e)
        }
    }

    /**
     * Uploads content to paste.rs (free permanent raw text storage) with dpaste.org fallback
     */
    fun uploadToPasteRs(content: String): String? {
        // 1. Try paste.rs first
        try {
            val url = URL("https://paste.rs")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "MouryaERP/2.0")
            conn.outputStream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode in 200..299) {
                val pasteUrl = conn.inputStream.bufferedReader().readText().trim()
                if (pasteUrl.startsWith("http")) {
                    return pasteUrl
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "uploadToPasteRs failed", e)
        }

        // 2. Fallback to dpaste.org
        try {
            val url = URL("https://dpaste.org/api/")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "MouryaERP/2.0")
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            val postData = "content=" + URLEncoder.encode(content, "UTF-8") + "&expiry_days=365"
            conn.outputStream.use { it.write(postData.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode in 200..299) {
                val codeOrUrl = conn.inputStream.bufferedReader().readText().replace("\"", "").trim()
                return if (codeOrUrl.startsWith("http")) {
                    if (codeOrUrl.endsWith("/raw")) codeOrUrl else "$codeOrUrl/raw"
                } else {
                    "https://dpaste.org/$codeOrUrl/raw"
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "dpaste fallback failed", e)
        }

        return null
    }

    /**
     * Normalizes phone numbers so "+91 98765-43210", "09876543210", and "9876543210"
     * always hit the exact same cloud key across Admin App and User App.
     */
    fun normalizePhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.length > 10 && digits.startsWith("91") -> digits.takeLast(10)
            digits.length > 10 && digits.startsWith("0") -> digits.takeLast(10)
            digits.isNotBlank() -> digits
            else -> phone.trim().replace("[^a-zA-Z0-9_]".toRegex(), "")
        }
    }

    private fun sanitizeCloudKey(key: String): String {
        return key.trim().replace("[^a-zA-Z0-9_-]".toRegex(), "_")
    }

    private fun encodeCloudEnvelope(value: String, timestamp: Long = System.currentTimeMillis()): String {
        val env = JSONObject().apply {
            put("_ts", timestamp)
            put("_v", value)
        }
        return android.util.Base64.encodeToString(
            env.toString().toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP
        )
    }

    private fun decodeCloudEnvelope(raw: String?): Pair<Long, String>? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        // 1. Try Base64 envelope
        try {
            val bytes = android.util.Base64.decode(trimmed, android.util.Base64.DEFAULT)
            val decoded = String(bytes, Charsets.UTF_8).trim()
            if (decoded.startsWith("{")) {
                val obj = JSONObject(decoded)
                if (obj.has("_v")) {
                    return Pair(obj.optLong("_ts", 0L), obj.optString("_v", ""))
                }
                return Pair(obj.optLong("timestamp", obj.optLong("_syncTs", 0L)), decoded)
            }
        } catch (_: Exception) {}
        // 2. Fallback: raw JSON or URL
        if (trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.startsWith("http")) {
            val ts = try {
                if (trimmed.startsWith("{")) JSONObject(trimmed).optLong("timestamp", 0L) else 0L
            } catch (_: Exception) { 0L }
            return Pair(ts, trimmed)
        }
        return null
    }

    /**
     * Publishes a key-value pair to Dual Persistent Cloud KV Stores (textdb.dev + textdb.online)
     * Both support unlimited multi-reads, instant overwrites, and full UTF-8/Hindi via Base64.
     */
    fun publishKeyValToCloud(key: String, value: String): Boolean {
        val safeKey = "tm_erp_v3_" + sanitizeCloudKey(key)
        val encoded = encodeCloudEnvelope(value, System.currentTimeMillis())
        var anySuccess = false

        // 1. Primary: textdb.dev (instant persistent KV, supports overwrite & multi-read)
        try {
            val url = URL("https://textdb.dev/api/data/$safeKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.setRequestProperty("Content-Type", "text/plain")
            conn.outputStream.use { it.write(encoded.toByteArray(Charsets.US_ASCII)) }
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299) {
                anySuccess = true
                Log.i(TAG, "textdb.dev publish OK for $safeKey (HTTP $code)")
            } else {
                Log.w(TAG, "textdb.dev publish HTTP $code for $safeKey")
            }
        } catch (e: Exception) {
            Log.w(TAG, "textdb.dev publish failed for $safeKey", e)
        }

        // 2. Mirror/Backup: textdb.online (instant persistent KV, supports overwrite & multi-read)
        try {
            val url = URL("https://textdb.online/update/?key=$safeKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            val body = "value=" + URLEncoder.encode(encoded, "UTF-8")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.US_ASCII)) }
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299) {
                anySuccess = true
                Log.i(TAG, "textdb.online publish OK for $safeKey (HTTP $code)")
            } else {
                Log.w(TAG, "textdb.online publish HTTP $code for $safeKey")
            }
        } catch (e: Exception) {
            Log.w(TAG, "textdb.online publish failed for $safeKey", e)
        }

        return anySuccess
    }

    private fun fetchRawFromTextDbDev(safeKey: String, timeoutMs: Int = 4500): String? {
        return try {
            val url = URL("https://textdb.dev/api/data/$safeKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.useCaches = false
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.setRequestProperty("Cache-Control", "no-cache")
            val code = conn.responseCode
            val result = if (code in 200..299) {
                conn.inputStream.bufferedReader().readText().trim().ifBlank { null }
            } else null
            conn.disconnect()
            result
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchRawFromTextDbOnline(safeKey: String, timeoutMs: Int = 4500): String? {
        return try {
            val url = URL("https://textdb.online/$safeKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.useCaches = false
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.setRequestProperty("Cache-Control", "no-cache")
            val code = conn.responseCode
            val result = if (code in 200..299) {
                val text = conn.inputStream.bufferedReader().readText().trim()
                if (text.isBlank() || text.startsWith("<!DOCTYPE", ignoreCase = true) || text.startsWith("<html", ignoreCase = true)) {
                    null
                } else text
            } else null
            conn.disconnect()
            result
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fetches latest value from Dual Persistent Cloud KV Stores.
     * If primary succeeds, returns immediately; if empty/failed, seamlessly reads mirror.
     * Set checkBoth = true to compare timestamps from both stores and guarantee freshest write.
     */
    fun fetchKeyValFromCloud(key: String, checkBoth: Boolean = false): String? {
        val safeKey = "tm_erp_v3_" + sanitizeCloudKey(key)

        val devDecoded = decodeCloudEnvelope(fetchRawFromTextDbDev(safeKey))
        val onlineDecoded = if (devDecoded == null || checkBoth) {
            decodeCloudEnvelope(fetchRawFromTextDbOnline(safeKey))
        } else null

        val bestValue = when {
            devDecoded != null && onlineDecoded != null -> {
                if (onlineDecoded.first > devDecoded.first) onlineDecoded.second else devDecoded.second
            }
            devDecoded != null -> devDecoded.second
            onlineDecoded != null -> onlineDecoded.second
            else -> null
        } ?: return null

        // If value is a URL (points to paste.rs or dpaste), fetch the actual content!
        if (bestValue.startsWith("http://") || bestValue.startsWith("https://")) {
            try {
                val fetchUrl = if (bestValue.contains("dpaste.org") && !bestValue.endsWith("/raw")) "$bestValue/raw" else bestValue
                val conn = URL(fetchUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                if (conn.responseCode in 200..299) {
                    return conn.inputStream.bufferedReader().readText().trim()
                }
            } catch (e: Exception) {
                Log.w(TAG, "failed fetching payload from $bestValue", e)
            }
        }

        return bestValue
    }

    private fun userToJsonObject(user: AppUser, timestamp: Long = System.currentTimeMillis()): JSONObject {
        val norm = normalizePhone(user.phone)
        return JSONObject().apply {
            put("id", user.id)
            put("name", user.name)
            put("shopName", user.shopName)
            put("phone", norm.ifBlank { user.phone.trim() })
            put("password", user.password)
            put("role", user.role)
            put("planDays", user.planDays)
            put("planName", user.planName)
            put("amountCharged", user.amountCharged)
            put("createdAt", user.createdAt)
            put("expiresAt", user.expiresAt)
            put("isActive", user.isActive)
            put("notes", user.notes)
            put("approvalStatus", user.approvalStatus)
            put("address", user.address)
            put("isDeleted", user.approvalStatus == "DELETED")
            put("timestamp", timestamp)
        }
    }

    private fun jsonObjectToUser(obj: JSONObject): AppUser? {
        val rawPhone = obj.optString("phone", "").trim()
        val norm = normalizePhone(rawPhone)
        if (norm.isBlank()) return null
        val isDeleted = obj.optBoolean("isDeleted", false) || obj.optString("approvalStatus") == "DELETED"
        return AppUser(
            id = obj.optLong("id", 0L),
            name = obj.optString("name", "दुकानदार"),
            shopName = obj.optString("shopName", "TILES AND MARBLES"),
            phone = norm,
            password = obj.optString("password", ""),
            role = obj.optString("role", "CLIENT"),
            planDays = obj.optInt("planDays", 365),
            planName = obj.optString("planName", "1 Year"),
            amountCharged = obj.optDouble("amountCharged", 0.0),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            expiresAt = obj.optLong("expiresAt", Long.MAX_VALUE),
            isActive = if (isDeleted) false else obj.optBoolean("isActive", true),
            notes = obj.optString("notes", ""),
            approvalStatus = if (isDeleted) "DELETED" else obj.optString("approvalStatus", "APPROVED"),
            address = obj.optString("address", "")
        )
    }

    /**
     * Merges a user entry into the shared cloud users directory so Admin App can see
     * all registered & active users across separate devices.
     * When overwriteExisting = false (used by User App heartbeat), admin-controlled fields
     * (isActive, approvalStatus, expiresAt, password) are NEVER overwritten by the User App.
     */
    private fun upsertUserInCloudDirectory(user: AppUser, overwriteExisting: Boolean) {
        try {
            val norm = normalizePhone(user.phone)
            if (norm.isBlank()) return
            val existingRaw = fetchKeyValFromCloud("users_directory")
            val arr = if (!existingRaw.isNullOrBlank() && existingRaw.trim().startsWith("[")) {
                JSONArray(existingRaw)
            } else {
                JSONArray()
            }

            val map = LinkedHashMap<String, JSONObject>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val p = normalizePhone(o.optString("phone", ""))
                if (p.isNotBlank()) map[p] = o
            }

            val existing = map[norm]
            if (existing == null || overwriteExisting) {
                map[norm] = userToJsonObject(user)
            } else {
                // Keep admin's authoritative status; only update empty profile info if needed
                if (existing.optString("name").isBlank() && user.name.isNotBlank()) {
                    existing.put("name", user.name)
                }
                if (existing.optString("shopName").isBlank() && user.shopName.isNotBlank()) {
                    existing.put("shopName", user.shopName)
                }
                map[norm] = existing
            }

            val out = JSONArray()
            for (v in map.values) out.put(v)
            publishKeyValToCloud("users_directory", out.toString())
        } catch (e: Exception) {
            Log.w(TAG, "upsertUserInCloudDirectory error", e)
        }
    }

    /**
     * Publishes user presence from User App so Admin App on another phone discovers this user.
     */
    suspend fun publishUserPresence(user: AppUser) = withContext(Dispatchers.IO) {
        upsertUserInCloudDirectory(user, overwriteExisting = false)
    }

    /**
     * Publishes user deletion tombstone to cloud channels for instant auto-logout
     */
    suspend fun publishDeletedUser(phone: String, context: Context? = null): Boolean = withContext(Dispatchers.IO) {
        val clean = normalizePhone(phone)
        if (clean.isBlank()) return@withContext false
        markPhoneDeleted(clean, context)
        if (phone.trim() != clean) markPhoneDeleted(phone.trim(), context)
        try {
            val now = System.currentTimeMillis()
            val tombstone = JSONObject().apply {
                put("phone", clean)
                put("action", "FORCE_LOGOUT")
                put("status", "DELETED")
                put("approvalStatus", "DELETED")
                put("isDeleted", true)
                put("isActive", false)
                put("timestamp", now)
            }
            val str = tombstone.toString()
            val ok1 = publishKeyValToCloud("session_$clean", str)
            val ok2 = publishKeyValToCloud("user_$clean", str)
            upsertUserInCloudDirectory(
                AppUser(
                    name = "",
                    shopName = "",
                    phone = clean,
                    password = "",
                    isActive = false,
                    approvalStatus = "DELETED"
                ),
                overwriteExisting = true
            )
            Log.i(TAG, "Published DELETED tombstone for $clean (ok=${ok1 || ok2})")
            ok1 || ok2
        } catch (e: Exception) {
            Log.w(TAG, "publishDeletedUser error", e)
            false
        }
    }

    /**
     * Publishes user block/unblock state for instant auto-logout
     */
    suspend fun publishBlockedUser(phone: String, isBlocked: Boolean): Boolean = withContext(Dispatchers.IO) {
        val clean = normalizePhone(phone)
        if (clean.isBlank()) return@withContext false
        if (!isBlocked) {
            unmarkPhoneDeleted(clean, null)
            if (phone.trim() != clean) unmarkPhoneDeleted(phone.trim(), null)
        }
        try {
            val now = System.currentTimeMillis()
            val obj = JSONObject().apply {
                put("phone", clean)
                put("action", if (isBlocked) "FORCE_LOGOUT" else "UNBLOCKED")
                put("status", if (isBlocked) "BLOCKED" else "APPROVED")
                put("approvalStatus", if (isBlocked) "BLOCKED" else "APPROVED")
                put("isDeleted", false)
                put("isActive", !isBlocked)
                put("timestamp", now)
            }
            val ok = publishKeyValToCloud("session_$clean", obj.toString())
            Log.i(TAG, "Published BLOCKED status ($isBlocked) for $clean (ok=$ok)")
            ok
        } catch (e: Exception) {
            Log.w(TAG, "publishBlockedUser error", e)
            false
        }
    }

    /**
     * Live session check against persistent cloud KV (textdb.dev + textdb.online)
     */
    fun checkCloudSessionRevocation(phone: String): CloudSessionState {
        val clean = normalizePhone(phone)
        if (clean.isBlank()) return CloudSessionState.UNKNOWN
        try {
            val raw = fetchKeyValFromCloud("session_$clean") ?: return CloudSessionState.UNKNOWN
            if (raw.startsWith("{")) {
                val obj = JSONObject(raw)
                val status = obj.optString("status", obj.optString("approvalStatus", ""))
                val action = obj.optString("action", "")
                val isDeleted = obj.optBoolean("isDeleted", false) || status == "DELETED"
                val isActive = obj.optBoolean("isActive", true)
                if (isDeleted) return CloudSessionState.DELETED
                if (status == "BLOCKED" || status == "REJECTED" || action == "FORCE_LOGOUT" || !isActive) {
                    return CloudSessionState.BLOCKED
                }
                if (action == "UNBLOCKED" || status == "APPROVED" || isActive) {
                    return CloudSessionState.VALID
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "checkCloudSessionRevocation error for $clean", e)
        }
        return CloudSessionState.UNKNOWN
    }

    /**
     * Publishes a new user self-registration request to cloud channels so Admin App receives it instantly
     */
    suspend fun publishNewRegistration(user: AppUser): Boolean = withContext(Dispatchers.IO) {
        val clean = normalizePhone(user.phone)
        if (clean.isBlank()) return@withContext false
        try {
            val normalizedUser = user.copy(phone = clean)
            val ok1 = publishCloudUser(normalizedUser)
            upsertUserInCloudDirectory(normalizedUser, overwriteExisting = true)
            Log.i(TAG, "Published new self-registration for $clean to cloud (ok=$ok1)")
            ok1
        } catch (e: Exception) {
            Log.w(TAG, "publishNewRegistration failed", e)
            false
        }
    }

    /**
     * Fetches all known users & pending registration requests from the shared cloud directory
     */
    suspend fun fetchCloudPendingRegistrations(): List<AppUser> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AppUser>()
        try {
            val raw = fetchKeyValFromCloud("users_directory")
            if (!raw.isNullOrBlank() && raw.trim().startsWith("[")) {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val u = jsonObjectToUser(obj) ?: continue
                    if (u.approvalStatus != "DELETED" && !isPhoneDeleted(u.phone)) {
                        list.add(u)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchCloudPendingRegistrations failed", e)
        }
        return@withContext list.distinctBy { normalizePhone(it.phone) }
    }

    /**
     * Publishes single user entity state (active/blocked, password, approvalStatus, expiry) to live cloud
     * Also keeps session_<phone> and users_directory in sync!
     */
    suspend fun publishCloudUser(user: AppUser): Boolean = withContext(Dispatchers.IO) {
        val clean = normalizePhone(user.phone)
        if (clean.isBlank()) return@withContext false
        try {
            val now = System.currentTimeMillis()
            val normalizedUser = user.copy(phone = clean)
            if (normalizedUser.approvalStatus != "DELETED") {
                unmarkPhoneDeleted(clean, null)
            }
            val obj = userToJsonObject(normalizedUser, now)
            val okUser = publishKeyValToCloud("user_$clean", obj.toString())

            val sessionObj = JSONObject().apply {
                put("phone", clean)
                put("action", when {
                    normalizedUser.approvalStatus == "DELETED" -> "FORCE_LOGOUT"
                    !normalizedUser.isActive || normalizedUser.approvalStatus == "BLOCKED" || normalizedUser.approvalStatus == "REJECTED" -> "FORCE_LOGOUT"
                    else -> "UNBLOCKED"
                })
                put("status", if (!normalizedUser.isActive && normalizedUser.approvalStatus == "APPROVED") "BLOCKED" else normalizedUser.approvalStatus)
                put("approvalStatus", normalizedUser.approvalStatus)
                put("isDeleted", normalizedUser.approvalStatus == "DELETED")
                put("isActive", normalizedUser.isActive)
                put("timestamp", now)
            }
            val okSession = publishKeyValToCloud("session_$clean", sessionObj.toString())
            upsertUserInCloudDirectory(normalizedUser, overwriteExisting = true)
            Log.i(TAG, "Published live cloud user for $clean: isActive=${user.isActive}, status=${user.approvalStatus} (ok=${okUser || okSession})")
            okUser || okSession
        } catch (e: Exception) {
            Log.w(TAG, "publishCloudUser failed", e)
            false
        }
    }

    /**
     * Fetches live user entity state from cloud (authoritative across all phones!)
     */
    suspend fun fetchLiveCloudUser(phone: String): AppUser? = withContext(Dispatchers.IO) {
        val clean = normalizePhone(phone)
        if (clean.isBlank()) return@withContext null
        try {
            val raw = fetchKeyValFromCloud("user_$clean")
            val rev = checkCloudSessionRevocation(clean)

            if (rev == CloudSessionState.DELETED) {
                markPhoneDeleted(clean)
                return@withContext AppUser(
                    id = -1L,
                    name = "",
                    shopName = "",
                    phone = clean,
                    password = "",
                    approvalStatus = "DELETED",
                    isActive = false
                )
            }

            if (!raw.isNullOrBlank() && raw.trim().startsWith("{")) {
                val obj = JSONObject(raw)
                val parsed = jsonObjectToUser(obj) ?: return@withContext null
                if (parsed.approvalStatus == "DELETED") {
                    markPhoneDeleted(clean)
                    return@withContext parsed
                }
                // If session key says BLOCKED, honor it even if user payload was stale
                val effectiveUser = when (rev) {
                    CloudSessionState.BLOCKED -> parsed.copy(isActive = false, approvalStatus = "BLOCKED")
                    CloudSessionState.VALID -> {
                        // Unmark any old local tombstone if Admin re-created or unblocked this user!
                        unmarkPhoneDeleted(clean, null)
                        parsed
                    }
                    else -> parsed
                }
                return@withContext effectiveUser
            }

            // If user_<phone> wasn't found yet but session explicitly says BLOCKED
            if (rev == CloudSessionState.BLOCKED) {
                return@withContext AppUser(
                    id = 0L,
                    name = "",
                    shopName = "",
                    phone = clean,
                    password = "",
                    approvalStatus = "BLOCKED",
                    isActive = false
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchLiveCloudUser failed for $phone", e)
        }
        return@withContext null
    }

    /**
     * Publishes admin config (maintenance mode, feature switches, broadcast banner, telegram, update) to live cloud
     */
    suspend fun publishCloudAdminConfig(config: AdminGlobalConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            val obj = JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("isMaintenanceMode", config.isMaintenanceMode)
                put("maintenanceMessage", config.maintenanceMessage)
                put("allowUserSelfRegistration", config.allowUserSelfRegistration)
                put("requireAdminApproval", config.requireAdminApproval)
                put("defaultTrialDays", config.defaultTrialDays)
                put("allowInvoiceDeletion", config.allowInvoiceDeletion)
                put("allowBillingDiscounts", config.allowBillingDiscounts)
                put("enforceSingleDailyAttendance", config.enforceSingleDailyAttendance)
                put("allowSalaryAdvance", config.allowSalaryAdvance)
                put("lowStockThreshold", config.lowStockThreshold)
                put("broadcastAnnouncement", config.broadcastAnnouncement)
                put("showBroadcastAnnouncement", config.showBroadcastAnnouncement)
                put("customAdminPin", config.customAdminPin)
                put("adminPhone", config.adminPhone)
                put("adminWhatsapp", config.adminWhatsapp)
                put("adminEmail", config.adminEmail)
                put("recycleBinRetentionDays", config.recycleBinRetentionDays)
                put("brandAppName", config.brandAppName)
                put("latestAppVersionCode", config.latestAppVersionCode)
                put("latestAppVersionName", config.latestAppVersionName)
                put("latestApkDownloadUrl", config.latestApkDownloadUrl)
                put("updateChangelog", config.updateChangelog)
                put("isForceUpdate", config.isForceUpdate)
                put("telegramEnabled", config.telegramEnabled)
                put("telegramBotToken", config.telegramBotToken)
                put("telegramChatId", config.telegramChatId)
                put("telegramFormat", config.telegramFormat)
                put("telegramCustomTitle", config.telegramCustomTitle)
                put("telegramAlertNewSale", config.telegramAlertNewSale)
                put("telegramAlertPayment", config.telegramAlertPayment)
                put("telegramAlertLowStock", config.telegramAlertLowStock)
                put("telegramAlertAttendance", config.telegramAlertAttendance)
                put("telegramAlertSalaryAdvance", config.telegramAlertSalaryAdvance)
                put("telegramAlertNewProduct", config.telegramAlertNewProduct)
                put("telegramAlertInvoiceDeleted", config.telegramAlertInvoiceDeleted)
                put("telegramAlertUserAction", config.telegramAlertUserAction)
            }
            val ok = publishKeyValToCloud("admin_global_config", obj.toString())
            Log.i(TAG, "Published cloud admin config: maintenanceMode=${config.isMaintenanceMode}, ok=$ok")
            ok
        } catch (e: Exception) {
            Log.w(TAG, "publishCloudAdminConfig failed", e)
            false
        }
    }

    /**
     * Fetches live admin config from cloud (checks emergency lock & feature controls across all phones!)
     */
    suspend fun fetchLiveCloudAdminConfig(): AdminGlobalConfig? = withContext(Dispatchers.IO) {
        try {
            val raw = fetchKeyValFromCloud("admin_global_config")
            if (!raw.isNullOrBlank() && raw.trim().startsWith("{")) {
                val obj = JSONObject(raw)
                return@withContext AdminGlobalConfig(
                    isMaintenanceMode = obj.optBoolean("isMaintenanceMode", false),
                    maintenanceMessage = obj.optString("maintenanceMessage", "⚠️ सिस्टम में तकनीकी रखरखाव चल रहा है।"),
                    allowUserSelfRegistration = obj.optBoolean("allowUserSelfRegistration", true),
                    requireAdminApproval = obj.optBoolean("requireAdminApproval", true),
                    defaultTrialDays = obj.optInt("defaultTrialDays", 30),
                    allowInvoiceDeletion = obj.optBoolean("allowInvoiceDeletion", true),
                    allowBillingDiscounts = obj.optBoolean("allowBillingDiscounts", true),
                    enforceSingleDailyAttendance = obj.optBoolean("enforceSingleDailyAttendance", true),
                    allowSalaryAdvance = obj.optBoolean("allowSalaryAdvance", true),
                    lowStockThreshold = obj.optInt("lowStockThreshold", 10),
                    broadcastAnnouncement = obj.optString("broadcastAnnouncement", ""),
                    showBroadcastAnnouncement = obj.optBoolean("showBroadcastAnnouncement", false),
                    customAdminPin = obj.optString("customAdminPin", "8899"),
                    adminPhone = obj.optString("adminPhone", "+91 98765 43210"),
                    adminWhatsapp = obj.optString("adminWhatsapp", "9876543210"),
                    adminEmail = obj.optString("adminEmail", "cp622499@gmail.com"),
                    recycleBinRetentionDays = obj.optInt("recycleBinRetentionDays", 10),
                    brandAppName = obj.optString("brandAppName", "TILES AND MARBLES"),
                    latestAppVersionCode = obj.optInt("latestAppVersionCode", 1),
                    latestAppVersionName = obj.optString("latestAppVersionName", "1.0.0"),
                    latestApkDownloadUrl = obj.optString("latestApkDownloadUrl", ""),
                    updateChangelog = obj.optString("updateChangelog", "नई सुविधाएं व बग फिक्स"),
                    isForceUpdate = obj.optBoolean("isForceUpdate", false),
                    telegramEnabled = obj.optBoolean("telegramEnabled", false),
                    telegramBotToken = obj.optString("telegramBotToken", ""),
                    telegramChatId = obj.optString("telegramChatId", ""),
                    telegramFormat = obj.optString("telegramFormat", "HINDI_DETAILED"),
                    telegramCustomTitle = obj.optString("telegramCustomTitle", "TILES AND MARBLES"),
                    telegramAlertNewSale = obj.optBoolean("telegramAlertNewSale", true),
                    telegramAlertPayment = obj.optBoolean("telegramAlertPayment", true),
                    telegramAlertLowStock = obj.optBoolean("telegramAlertLowStock", true),
                    telegramAlertAttendance = obj.optBoolean("telegramAlertAttendance", true),
                    telegramAlertSalaryAdvance = obj.optBoolean("telegramAlertSalaryAdvance", true),
                    telegramAlertNewProduct = obj.optBoolean("telegramAlertNewProduct", true),
                    telegramAlertInvoiceDeleted = obj.optBoolean("telegramAlertInvoiceDeleted", true),
                    telegramAlertUserAction = obj.optBoolean("telegramAlertUserAction", true)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchLiveCloudAdminConfig failed", e)
        }
        return@withContext null
    }

    /**
     * Uploads backup to free cloud server (dpaste.org, 365 days)
     */
    suspend fun uploadCloudBackup(json: String, expiryDays: Int = 365): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = URL("https://dpaste.org/api/")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val postData = "content=" + URLEncoder.encode(json, "UTF-8") +
                    "&expiry_days=" + expiryDays.toString()

            OutputStreamWriter(conn.outputStream, "UTF-8").use {
                it.write(postData)
                it.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText().trim() }
                val cleaned = responseText.replace("\"", "").trim()
                val code = cleaned.substringAfterLast("/")
                Result.success(code)
            } else {
                Result.failure(Exception("Cloud Upload Failed: HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads backup from cloud using short code or URL and restores it
     */
    suspend fun downloadAndRestoreCloudBackup(
        database: AppDatabase,
        cloudCodeOrUrl: String,
        context: Context? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val input = cloudCodeOrUrl.trim()
            val fetchUrl = if (input.startsWith("http://") || input.startsWith("https://")) {
                if (input.endsWith("/raw")) input else "$input/raw"
            } else {
                "https://dpaste.org/$input/raw"
            }

            val url = URL(fetchUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val json = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                if (json.isBlank()) {
                    Result.failure(Exception("क्लाउड बैकअप खाली है"))
                } else {
                    BackupRestoreHelper.restoreFromJsonString(database, json, context)
                }
            } else {
                Result.failure(Exception("क्लाउड से डेटा नहीं मिला (HTTP $responseCode)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
