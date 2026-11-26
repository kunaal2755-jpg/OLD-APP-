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

            val json = BackupRestoreHelper.exportDatabaseToJson(database)
            val now = System.currentTimeMillis()

            // 1. Update in local DB user entity
            try {
                database.appUserDao().saveCloudBackup(cleanPhone, json, now)
            } catch (_: Exception) {}

            // 2. Write to vault under phone-specific file and latest universal file
            writeVaultFile(context, "vault_${cleanPhone}.json", json)
            writeVaultFile(context, "vault_latest.json", json)

            // 3. Update users registry
            try {
                val allUsers = database.appUserDao().getAllUsersSync()
                if (allUsers.isNotEmpty()) {
                    saveUsersRegistry(context, allUsers)
                }
            } catch (_: Exception) {}

            // 4. Cloud sync for phone (survives OS Clear Data on any device!)
            try {
                val pasteUrl = uploadToPasteRs(json)
                if (!pasteUrl.isNullOrBlank()) {
                    publishKeyValToCloud("vault_${cleanPhone}", pasteUrl)
                }
            } catch (ce: Exception) {
                Log.w(TAG, "Cloud sync snapshot error", ce)
            }

            Log.i(TAG, "Shop snapshot saved successfully for phone: $cleanPhone (${json.length} bytes)")
            Result.success("सुरक्षित बैकअप वॉल्ट व क्लाउड में सेव हो गया!")
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
                val res = BackupRestoreHelper.restoreFromJsonString(database, json)
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
     * Publishes a key-value pair to cloud channels (cl1p.net and ntfy.sh)
     */
    fun publishKeyValToCloud(key: String, value: String) {
        val cleanKey = key.trim()

        // 1. ntfy.sh (Ultra-fast ~150ms sub-second broadcast)
        try {
            val url = URL("https://ntfy.sh/tm_cloud_vault_$cleanKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.setRequestProperty("User-Agent", "MouryaERP/2.0")
            conn.outputStream.use { it.write(value.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            Log.i(TAG, "ntfy publish for $cleanKey: HTTP $code")
        } catch (e: Exception) {
            Log.w(TAG, "ntfy publish failed for $cleanKey", e)
        }

        // 2. cl1p.net (Persistent fallback)
        try {
            val url = URL("https://cl1p.net/tm_cloud_vault_$cleanKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("User-Agent", "MouryaERP/2.0")
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            val postData = "content=" + URLEncoder.encode(value, "UTF-8")
            conn.outputStream.use { it.write(postData.toByteArray(Charsets.UTF_8)) }
            conn.responseCode
        } catch (e: Exception) {
            Log.w(TAG, "cl1p publish failed for $cleanKey", e)
        }
    }

    /**
     * Fetches value from cloud channels and resolves payload if it is a URL
     */
    fun fetchKeyValFromCloud(key: String): String? {
        val cleanKey = key.trim()
        var rawVal: String? = null

        // 1. Try cl1p.net first
        try {
            val url = URL("https://cl1p.net/tm_cloud_vault_$cleanKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 7000
            conn.readTimeout = 7000
            conn.setRequestProperty("User-Agent", "MouryaERP/2.0")
            if (conn.responseCode in 200..299) {
                val html = conn.inputStream.bufferedReader().readText()
                val regex = Regex("<textarea[^>]*>([\\s\\S]*?)</textarea>")
                val match = regex.find(html)
                if (match != null) {
                    val raw = match.groupValues[1].trim()
                    if (raw.isNotBlank()) {
                        rawVal = raw.replace("&quot;", "\"")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&#39;", "'")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "cl1p fetch failed for $cleanKey", e)
        }

        // 2. Try ntfy.sh if cl1p did not return
        if (rawVal.isNullOrBlank()) {
            try {
                val url = URL("https://ntfy.sh/tm_cloud_vault_$cleanKey/raw?poll=1")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("User-Agent", "MouryaERP/2.0")
                if (conn.responseCode in 200..299) {
                    val text = conn.inputStream.bufferedReader().readText().trim()
                    if (text.isNotBlank()) {
                        // In case of multiple historical updates on the ntfy channel, always take the latest update!
                        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
                        if (lines.isNotEmpty()) {
                            rawVal = lines.last()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "ntfy fetch failed for $cleanKey", e)
            }
        }

        // If rawVal is a URL (points to paste.rs or dpaste), fetch the actual content!
        if (!rawVal.isNullOrBlank() && (rawVal.startsWith("http://") || rawVal.startsWith("https://"))) {
            try {
                val fetchUrl = if (rawVal.contains("dpaste.org") && !rawVal.endsWith("/raw")) "$rawVal/raw" else rawVal
                val conn = URL(fetchUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("User-Agent", "MouryaERP/2.0")
                if (conn.responseCode in 200..299) {
                    return conn.inputStream.bufferedReader().readText().trim()
                }
            } catch (e: Exception) {
                Log.w(TAG, "failed fetching payload from $rawVal", e)
            }
        }

        return rawVal
    }

    /**
     * Publishes user deletion tombstone to cloud channels for instant auto-logout
     */
    suspend fun publishDeletedUser(phone: String, context: Context? = null) = withContext(Dispatchers.IO) {
        val clean = phone.trim()
        if (clean.isBlank()) return@withContext
        markPhoneDeleted(clean, context)
        try {
            val tombstone = JSONObject().apply {
                put("phone", clean)
                put("action", "FORCE_LOGOUT")
                put("status", "DELETED")
                put("approvalStatus", "DELETED")
                put("isDeleted", true)
                put("isActive", false)
                put("timestamp", System.currentTimeMillis())
            }
            val str = tombstone.toString()
            publishKeyValToCloud("user_$clean", str)
            publishKeyValToCloud("session_$clean", str)
            Log.i(TAG, "Published DELETED tombstone for $clean")
        } catch (e: Exception) {
            Log.w(TAG, "publishDeletedUser error", e)
        }
    }

    /**
     * Publishes user block/unblock state for instant auto-logout
     */
    suspend fun publishBlockedUser(phone: String, isBlocked: Boolean) = withContext(Dispatchers.IO) {
        val clean = phone.trim()
        if (clean.isBlank()) return@withContext
        try {
            val obj = JSONObject().apply {
                put("phone", clean)
                put("action", if (isBlocked) "FORCE_LOGOUT" else "UNBLOCKED")
                put("status", if (isBlocked) "BLOCKED" else "APPROVED")
                put("approvalStatus", if (isBlocked) "BLOCKED" else "APPROVED")
                put("isDeleted", false)
                put("isActive", !isBlocked)
                put("timestamp", System.currentTimeMillis())
            }
            val str = obj.toString()
            publishKeyValToCloud("user_$clean", str)
            publishKeyValToCloud("session_$clean", str)
            Log.i(TAG, "Published BLOCKED status ($isBlocked) for $clean")
        } catch (e: Exception) {
            Log.w(TAG, "publishBlockedUser error", e)
        }
    }

    /**
     * Fast sub-second session check against cloud (timeout: 2500ms)
     */
    fun checkCloudSessionRevocation(phone: String): CloudSessionState {
        val clean = phone.trim()
        if (clean.isBlank()) return CloudSessionState.UNKNOWN
        try {
            val url = URL("https://ntfy.sh/tm_cloud_vault_session_$clean/raw?poll=1")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 2500
            conn.readTimeout = 2500
            conn.setRequestProperty("User-Agent", "MouryaERP/2.0")
            if (conn.responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().readText().trim()
                if (text.isNotBlank()) {
                    val lastLine = text.lines().map { it.trim() }.lastOrNull { it.isNotBlank() } ?: ""
                    if (lastLine.contains("\"isDeleted\":true") || lastLine.contains("\"DELETED\"")) {
                        return CloudSessionState.DELETED
                    }
                    if (lastLine.contains("\"BLOCKED\"") || lastLine.contains("\"FORCE_LOGOUT\"") || lastLine.contains("\"REJECTED\"") || lastLine.contains("\"isActive\":false")) {
                        return CloudSessionState.BLOCKED
                    }
                    if (lastLine.contains("\"UNBLOCKED\"") || lastLine.contains("\"APPROVED\"")) {
                        return CloudSessionState.VALID
                    }
                }
            }
        } catch (_: Exception) {}
        return CloudSessionState.UNKNOWN
    }

    /**
     * Publishes a new user self-registration request to cloud channels so Admin App receives it instantly
     */
    suspend fun publishNewRegistration(user: AppUser) = withContext(Dispatchers.IO) {
        val clean = user.phone.trim()
        if (clean.isBlank()) return@withContext
        try {
            val obj = JSONObject().apply {
                put("name", user.name)
                put("shopName", user.shopName)
                put("phone", clean)
                put("password", user.password)
                put("role", "CLIENT")
                put("planDays", user.planDays)
                put("planName", user.planName)
                put("amountCharged", user.amountCharged)
                put("createdAt", user.createdAt)
                put("expiresAt", user.expiresAt)
                put("isActive", false)
                put("notes", user.notes)
                put("approvalStatus", "PENDING")
                put("address", user.address)
                put("timestamp", System.currentTimeMillis())
            }
            val str = obj.toString()

            // 1. Publish to the live registrations feed (ntfy topic)
            val url = URL("https://ntfy.sh/tm_cloud_vault_new_registrations")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "MouryaERP/2.0")
            conn.setRequestProperty("Title", "New Registration: ${user.name}")
            conn.outputStream.use { it.write(str.toByteArray(Charsets.UTF_8)) }
            conn.responseCode

            // 2. Also publish user's individual profile in cloud
            publishCloudUser(user.copy(approvalStatus = "PENDING", isActive = false))

            Log.i(TAG, "Published new self-registration for $clean to cloud")
        } catch (e: Exception) {
            Log.w(TAG, "publishNewRegistration failed", e)
        }
    }

    /**
     * Fetches all pending registration requests from the cloud feed
     */
    suspend fun fetchCloudPendingRegistrations(): List<AppUser> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AppUser>()
        try {
            val url = URL("https://ntfy.sh/tm_cloud_vault_new_registrations/raw?poll=1")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.setRequestProperty("User-Agent", "MouryaERP/2.0")
            if (conn.responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().readText().trim()
                if (text.isNotBlank()) {
                    val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
                    for (line in lines) {
                        try {
                            if (line.startsWith("{") && line.endsWith("}")) {
                                val obj = JSONObject(line)
                                val phone = obj.optString("phone", "").trim()
                                if (phone.isNotBlank() && !isPhoneDeleted(phone)) {
                                    list.add(
                                        AppUser(
                                            id = 0L,
                                            name = obj.optString("name", "दुकानदार"),
                                            shopName = obj.optString("shopName", "TILES AND MARBLES"),
                                            phone = phone,
                                            password = obj.optString("password", "123456"),
                                            role = "CLIENT",
                                            planDays = obj.optInt("planDays", 30),
                                            planName = obj.optString("planName", "30 Days Trial"),
                                            amountCharged = obj.optDouble("amountCharged", 0.0),
                                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                            expiresAt = obj.optLong("expiresAt", System.currentTimeMillis() + 30L * 86400000L),
                                            isActive = false,
                                            notes = obj.optString("notes", "सेल्फ-रजिस्टर किया गया"),
                                            approvalStatus = obj.optString("approvalStatus", "PENDING"),
                                            address = obj.optString("address", "")
                                        )
                                    )
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchCloudPendingRegistrations failed", e)
        }
        return@withContext list.distinctBy { it.phone }
    }

    /**
     * Publishes single user entity state (active/blocked, password, approvalStatus) to live cloud
     */
    suspend fun publishCloudUser(user: AppUser) = withContext(Dispatchers.IO) {
        val clean = user.phone.trim()
        if (clean.isBlank()) return@withContext
        try {
            val obj = JSONObject().apply {
                put("id", user.id)
                put("name", user.name)
                put("shopName", user.shopName)
                put("phone", clean)
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
            }
            publishKeyValToCloud("user_$clean", obj.toString())
            Log.i(TAG, "Published live cloud user for $clean: isActive=${user.isActive}, status=${user.approvalStatus}")
        } catch (e: Exception) {
            Log.w(TAG, "publishCloudUser failed", e)
        }
    }

    /**
     * Fetches live user entity state from cloud (authoritative across all phones!)
     */
    suspend fun fetchLiveCloudUser(phone: String): AppUser? = withContext(Dispatchers.IO) {
        val clean = phone.trim()
        if (clean.isBlank()) return@withContext null
        if (isPhoneDeleted(clean)) {
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
        try {
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

            val raw = fetchKeyValFromCloud("user_$clean")
            if (!raw.isNullOrBlank()) {
                val obj = JSONObject(raw)
                val isDeleted = obj.optBoolean("isDeleted", false) || obj.optString("approvalStatus") == "DELETED"
                if (isDeleted) {
                    markPhoneDeleted(clean)
                }
                return@withContext AppUser(
                    id = obj.optLong("id", 0L),
                    name = obj.optString("name", "दुकानदार"),
                    shopName = obj.optString("shopName", "TILES AND MARBLES"),
                    phone = obj.getString("phone"),
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
        } catch (e: Exception) {
            Log.w(TAG, "fetchLiveCloudUser failed for $phone", e)
        }
        return@withContext null
    }

    /**
     * Publishes admin config (including isMaintenanceMode emergency lock) to live cloud
     */
    suspend fun publishCloudAdminConfig(config: AdminGlobalConfig) = withContext(Dispatchers.IO) {
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
            publishKeyValToCloud("admin_global_config", obj.toString())
            Log.i(TAG, "Published cloud admin config: maintenanceMode=${config.isMaintenanceMode}")
        } catch (e: Exception) {
            Log.w(TAG, "publishCloudAdminConfig failed", e)
        }
    }

    /**
     * Fetches live admin config from cloud (checks emergency lock across all phones!)
     */
    suspend fun fetchLiveCloudAdminConfig(): AdminGlobalConfig? = withContext(Dispatchers.IO) {
        try {
            val raw = fetchKeyValFromCloud("admin_global_config")
            if (!raw.isNullOrBlank()) {
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
        cloudCodeOrUrl: String
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
                    BackupRestoreHelper.restoreFromJsonString(database, json)
                }
            } else {
                Result.failure(Exception("क्लाउड से डेटा नहीं मिला (HTTP $responseCode)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
