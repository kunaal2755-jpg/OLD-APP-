package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.OrderWithItems
import com.example.data.model.ShopSettings
import com.example.data.model.SizePricing
import com.example.data.model.TileProduct
import com.example.data.repository.MouryaRepository
import com.example.util.BackupRestoreHelper
import com.example.util.PdfGenerator
import com.example.util.ShareHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class Screen {
    object Dashboard : Screen()
    object Inventory : Screen()
    object NewSale : Screen()
    data class EditOrder(val orderId: Long) : Screen()
    object Orders : Screen()
    data class OrderDetail(val orderId: Long) : Screen()
    object Customers : Screen()
    object Reports : Screen()
    object Settings : Screen()
    object Staff : Screen()
    data class StaffDetail(val staffId: Long) : Screen()
    object Login : Screen()
}

data class DashboardStats(
    val totalProducts: Int = 0,
    val totalCustomers: Int = 0,
    val availableBoxes: Int = 0,
    val availableLoosePieces: Int = 0,
    val totalAvailablePieces: Int = 0,
    val pendingOrdersCount: Int = 0,
    val pendingOrdersValue: Double = 0.0,
    val confirmedSalesCount: Int = 0,
    val confirmedSalesValue: Double = 0.0,
    val totalBoxesSold: Int = 0,
    val lowStockCount: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = MouryaRepository(database)

    private val isInitialLoggedIn = com.example.data.security.UserSessionManager.isLoggedIn(application)
    private val initialScreen = if (isInitialLoggedIn) Screen.Dashboard else Screen.Login

    // Navigation State
    private val _currentScreen = MutableStateFlow<Screen>(initialScreen)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _screenStack = mutableListOf<Screen>(initialScreen)

    private val _forcedLogoutReason = MutableStateFlow<String?>(null)
    val forcedLogoutReason: StateFlow<String?> = _forcedLogoutReason.asStateFlow()

    fun clearForcedLogoutAlert() {
        _forcedLogoutReason.value = null
    }

    private suspend fun triggerForcedLogout(reason: String) {
        kotlinx.coroutines.withContext(Dispatchers.Main) {
            com.example.data.security.UserSessionManager.logout(getApplication())
            _screenStack.clear()
            _screenStack.add(Screen.Login)
            _currentScreen.value = Screen.Login
            _forcedLogoutReason.value = reason
            showMessage(reason)
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val sessionUser = com.example.data.security.UserSessionManager.getCurrentUser(getApplication())
            if (sessionUser != null) {
                val cleanPhone = sessionUser.phone.trim()
                if (com.example.util.PermanentVaultHelper.isPhoneDeleted(cleanPhone, getApplication())) {
                    triggerForcedLogout("⚠️ आपका खाता (User ID: $cleanPhone) एडमिन द्वारा हटा दिया गया है।")
                }
            }
            repository.ensureDefaultUserExists()
            repository.purgeOldRecycleBinItems()
            if (sessionUser != null) {
                val dbUser = repository.getUserById(sessionUser.id) ?: repository.getUserByPhone(sessionUser.phone)
                if (dbUser != null) {
                    com.example.data.security.UserSessionManager.saveSession(getApplication(), dbUser)
                    val s = repository.getCurrentShopSettings()
                    if (s.shopName.isBlank() || s.ownerPhone.isBlank() || s.shopName.contains("Mourya", ignoreCase = true)) {
                        val updated = s.copy(
                            shopName = dbUser.shopName,
                            ownerName = dbUser.name,
                            ownerPhone = dbUser.phone
                        )
                        repository.saveSettings(updated)
                    }
                }
            }

            // Continuous sub-second session monitor: Auto-logout within 1 sec if deleted or blocked
            var tickCount = 0
            while (isActive) {
                kotlinx.coroutines.delay(600)
                tickCount++
                try {
                    val currentSession = com.example.data.security.UserSessionManager.getCurrentUser(getApplication())
                    if (currentSession != null && _currentScreen.value !is Screen.Login) {
                        val sessionPhone = currentSession.phone.trim()

                        // 1. Instant local blacklist check (0ms)
                        if (com.example.util.PermanentVaultHelper.isPhoneDeleted(sessionPhone, getApplication())) {
                            repository.appUserDao.deleteByPhone(sessionPhone)
                            triggerForcedLogout("⚠️ आपका खाता (User ID: $sessionPhone) एडमिन द्वारा हटा (Delete) दिया गया है। आप तुरंत लॉगआउट कर दिए गए हैं।")
                            continue
                        }

                        // 2. Check local database record
                        val localUser = repository.getUserByPhone(sessionPhone)
                        if (localUser != null) {
                            if (!localUser.isActive || localUser.approvalStatus == "BLOCKED" || localUser.approvalStatus == "REJECTED") {
                                triggerForcedLogout("⚠️ आपका अकाउंट एडमिन द्वारा ब्लॉक (Blocked) कर दिया गया है। आप तुरंत लॉगआउट कर दिए गए हैं।")
                                continue
                            }
                            if (localUser.approvalStatus == "DELETED") {
                                repository.appUserDao.deleteByPhone(sessionPhone)
                                triggerForcedLogout("⚠️ आपका खाता एडमिन द्वारा हटा (Delete) दिया गया है।")
                                continue
                            }
                        }

                        // 3. Fast sub-second cloud session revocation check (via ntfy session topic)
                        val revState = com.example.util.PermanentVaultHelper.checkCloudSessionRevocation(sessionPhone)
                        if (revState == com.example.util.CloudSessionState.DELETED) {
                            com.example.util.PermanentVaultHelper.markPhoneDeleted(sessionPhone, getApplication())
                            repository.appUserDao.deleteByPhone(sessionPhone)
                            triggerForcedLogout("⚠️ आपका खाता एडमिन द्वारा हटा (Delete) दिया गया है। आप तुरंत लॉगआउट कर दिए गए हैं।")
                            continue
                        } else if (revState == com.example.util.CloudSessionState.BLOCKED) {
                            if (localUser != null) {
                                repository.appUserDao.update(localUser.copy(isActive = false, approvalStatus = "BLOCKED"))
                            }
                            triggerForcedLogout("⚠️ आपका अकाउंट एडमिन द्वारा ब्लॉक (Blocked) कर दिया गया है। कृपया एडमिन से संपर्क करें।")
                            continue
                        }

                        // 4. Periodically (every ~15s) check Admin Maintenance Mode and Subscription Expiry
                        if (tickCount % 10 == 0) {
                            val liveConfig = com.example.util.PermanentVaultHelper.fetchLiveCloudAdminConfig()
                            if (liveConfig != null) {
                                repository.adminGlobalConfigDao.saveConfig(liveConfig)
                                checkForAppUpdate(liveConfig)
                                if (liveConfig.isMaintenanceMode && currentSession.role != "ADMIN") {
                                    triggerForcedLogout(liveConfig.maintenanceMessage.ifBlank { "⚠️ एडमिन द्वारा इमरजेंसी मोड चालू किया गया है। ऐप तुरंत बंद हो रहा है।" })
                                    continue
                                }
                            }

                            val now = System.currentTimeMillis()
                            if (currentSession.expiresAt in 1..now) {
                                triggerForcedLogout("⚠️ आपका सब्सक्रिप्शन समाप्त (Expired) हो गया है। कृपया रिन्यू कराएं।")
                                continue
                            }
                        }

                        // 5. Evening Scheduled Auto-Backup & Auto-Attendance Check (every ~30s)
                        if (tickCount % 30 == 0) {
                            try {
                                val s = repository.getCurrentShopSettings()
                                val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date())

                                if (s.autoAttendanceEnabled) {
                                    repository.autoMarkPresentForDate(todayStr)
                                }

                                if (s.autoBackupEnabled) {
                                    val nowTimeStr = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date())
                                    val schedTime = s.backupScheduledTime.ifBlank { "20:00" }
                                    if (nowTimeStr >= schedTime && s.lastAutoBackupDate != todayStr) {
                                        val backupFile = BackupRestoreHelper.createCumulativeBackupFile(
                                            getApplication(),
                                            repository.database,
                                            s.backupFromDateString
                                        )
                                        repository.saveSettings(s.copy(lastAutoBackupDate = todayStr))
                                        try {
                                            val tgConfig = getActiveTelegramConfig()
                                            com.example.util.TelegramNotifier.sendActiveMessage(
                                                tgConfig,
                                                "📦 *दैनिक शाम का ऑटो-बैकअप सुरक्षित हुआ*\n" +
                                                "फ़ाइल नाम: `${backupFile.name}`\n" +
                                                "तारीख: $todayStr (शाम $nowTimeStr)\n" +
                                                if (s.backupTargetEmail.isNotBlank()) "ईमेल / ड्राइव: ${s.backupTargetEmail}\n" else "" +
                                                "कस्टम बैकअप डेटा सेव कर दिया गया है।"
                                            )
                                        } catch (_: Exception) {}
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _screenStack.add(screen)
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        if (_screenStack.size > 1) {
            _screenStack.removeAt(_screenStack.size - 1)
            _currentScreen.value = _screenStack.last()
            return true
        }
        return false
    }

    val adminGlobalConfig: StateFlow<com.example.data.model.AdminGlobalConfig?> = repository.adminGlobalConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _updatePromptConfig = MutableStateFlow<com.example.data.model.AdminGlobalConfig?>(null)
    val updatePromptConfig: StateFlow<com.example.data.model.AdminGlobalConfig?> = _updatePromptConfig.asStateFlow()

    fun dismissUpdatePrompt() {
        _updatePromptConfig.value = null
    }

    fun checkForAppUpdate(config: com.example.data.model.AdminGlobalConfig?) {
        if (config == null) return
        val currentCode = com.example.BuildConfig.VERSION_CODE
        if (config.latestAppVersionCode > currentCode && config.latestApkDownloadUrl.isNotBlank()) {
            _updatePromptConfig.value = config
        }
    }

    fun launchUpdateDownload(context: Context, url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showMessage("अपडेट लिंक खोलने में विफल: ${e.localizedMessage}")
        }
    }

    suspend fun getActiveTelegramConfig(): com.example.util.TelegramActiveConfig {
        val adminConfig = adminGlobalConfig.value ?: repository.adminGlobalConfigDao.getConfigSync()
        val shopSettings = repository.getCurrentShopSettings()
        return com.example.util.TelegramActiveConfig.resolve(adminConfig, shopSettings)
    }

    fun triggerAutoVaultSave() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUser = com.example.data.security.UserSessionManager.getCurrentUser(getApplication())
                val phone = currentUser?.phone?.ifBlank { null }
                    ?: repository.getUserByPhone("9876543210")?.phone
                    ?: "9876543210"
                com.example.util.PermanentVaultHelper.saveShopSnapshot(getApplication(), repository.database, phone)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Auto-vault save error", e)
            }
        }
    }

    fun uploadToCloudSync(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = com.example.data.security.UserSessionManager.getCurrentUser(getApplication())
            val phone = user?.phone ?: "9876543210"
            com.example.util.PermanentVaultHelper.saveShopSnapshot(getApplication(), repository.database, phone)
            val compactWithImages = com.example.util.BackupRestoreHelper.exportDatabaseToJson(
                database = repository.database,
                context = getApplication(),
                includeImages = true,
                maxImageDim = 360,
                jpegQuality = 55
            )
            val payload = if (compactWithImages.length <= 450_000) {
                compactWithImages
            } else {
                com.example.util.BackupRestoreHelper.exportDatabaseToJson(
                    database = repository.database,
                    context = getApplication(),
                    includeImages = false
                )
            }
            var res = com.example.util.PermanentVaultHelper.uploadCloudBackup(payload, expiryDays = 365)
            if (res.isFailure && payload === compactWithImages) {
                val lightFallback = com.example.util.BackupRestoreHelper.exportDatabaseToJson(
                    database = repository.database,
                    context = getApplication(),
                    includeImages = false
                )
                res = com.example.util.PermanentVaultHelper.uploadCloudBackup(lightFallback, expiryDays = 365)
            }
            res.fold(
                onSuccess = { code ->
                    showMessage("क्लाउड सिंक कोड: $code")
                    onSuccess(code)
                },
                onFailure = { err ->
                    onError(err.message ?: "क्लाउड अपलोड विफल रहा")
                }
            )
        }
    }

    fun restoreFromCloudSyncCode(code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = com.example.util.PermanentVaultHelper.downloadAndRestoreCloudBackup(
                database = repository.database,
                cloudCodeOrUrl = code,
                context = getApplication()
            )
            res.fold(
                onSuccess = { msg ->
                    showMessage(msg.ifBlank { "डेटा सफलतापूर्वक क्लाउड से रीस्टोर हुआ!" })
                    onSuccess()
                },
                onFailure = { err ->
                    onError(err.message ?: "क्लाउड रीस्टोर विफल रहा")
                }
            )
        }
    }

    fun loginClientUser(phone: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanPhone = phone.trim()

            // 0. Check local deletion blacklist (Instant, 0ms)
            if (com.example.util.PermanentVaultHelper.isPhoneDeleted(cleanPhone, getApplication())) {
                onError("❌ यह ID और पासवर्ड मौजूद नहीं है। यह खाता नहीं बना है या एडमिन द्वारा हटा दिया गया है।")
                return@launch
            }

            // 1. Instant Cloud Session Revocation Check (via ntfy)
            val cloudRev = com.example.util.PermanentVaultHelper.checkCloudSessionRevocation(cleanPhone)
            if (cloudRev == com.example.util.CloudSessionState.DELETED) {
                com.example.util.PermanentVaultHelper.markPhoneDeleted(cleanPhone, getApplication())
                repository.appUserDao.deleteByPhone(cleanPhone)
                onError("❌ यह ID और पासवर्ड मौजूद नहीं है। यह खाता नहीं बना है या एडमिन द्वारा हटा दिया गया है।")
                return@launch
            } else if (cloudRev == com.example.util.CloudSessionState.BLOCKED) {
                onError("⚠️ आपका अकाउंट एडमिन द्वारा ब्लॉक (Blocked) कर दिया गया है। सहायता के लिए एडमिन से संपर्क करें।")
                return@launch
            }

            // 2. Live Cloud Admin Check: First check if Admin enabled Emergency Maintenance Mode
            val liveCloudConfig = com.example.util.PermanentVaultHelper.fetchLiveCloudAdminConfig()
            if (liveCloudConfig != null) {
                repository.adminGlobalConfigDao.saveConfig(liveCloudConfig)
                if (liveCloudConfig.isMaintenanceMode) {
                    onError("⚠️ " + (liveCloudConfig.maintenanceMessage.ifBlank { "सिस्टम में इमरजेंसी मोड चालू है। एडमिन द्वारा ऐप बंद किया गया है।" }))
                    return@launch
                }
            } else {
                val localAdminConfig = repository.adminGlobalConfigDao.getConfigSync()
                if (localAdminConfig != null && localAdminConfig.isMaintenanceMode) {
                    onError(localAdminConfig.maintenanceMessage.ifBlank { "⚠️ सिस्टम में तकनीकी रखरखाव चल रहा है। कृपया बाद में प्रयास करें।" })
                    return@launch
                }
            }

            // 3. Fetch live cloud user state (authoritative across all physical devices!)
            val liveCloudUser = com.example.util.PermanentVaultHelper.fetchLiveCloudUser(cleanPhone)
            if (liveCloudUser != null) {
                if (liveCloudUser.approvalStatus == "DELETED") {
                    com.example.util.PermanentVaultHelper.markPhoneDeleted(cleanPhone, getApplication())
                    repository.appUserDao.deleteByPhone(cleanPhone)
                    onError("❌ यह ID और पासवर्ड मौजूद नहीं है। यह खाता नहीं बना है या एडमिन द्वारा हटा दिया गया है।")
                    return@launch
                }
                val existingLocal = repository.getUserByPhone(cleanPhone)
                if (existingLocal != null) {
                    repository.appUserDao.update(liveCloudUser.copy(id = existingLocal.id))
                } else {
                    repository.appUserDao.insert(liveCloudUser)
                }
            }

            var user = repository.getUserByPhone(cleanPhone) ?: liveCloudUser
            // If user not in local DB (e.g. fresh install or Clear Data), check Permanent Vault!
            if (user == null) {
                com.example.util.PermanentVaultHelper.autoRestoreOnLaunch(getApplication(), repository.database)
                user = repository.getUserByPhone(cleanPhone)
            }

            if (user == null || user.approvalStatus == "DELETED") {
                onError("❌ यह ID और पासवर्ड मौजूद नहीं है। यह खाता नहीं बना है या एडमिन द्वारा हटा दिया गया है।")
                return@launch
            }

            // 4. Password validation
            if (user.password.trim() != password.trim()) {
                onError("❌ गलत पासवर्ड! कृपया सही पासवर्ड दर्ज करें।")
                return@launch
            }

            // 5. Admin Block & Status checks
            if (user.approvalStatus == "PENDING") {
                onError("⏳ आपका खाता अभी एडमिन की स्वीकृति के लिए प्रतीक्षारत (Pending) है। एडमिन द्वारा स्वीकार (Approve) किए जाने के बाद आप लॉगिन कर सकेंगे।")
                return@launch
            }
            if (user.approvalStatus == "REJECTED" || user.approvalStatus == "BLOCKED" || !user.isActive) {
                onError("⚠️ आपका अकाउंट एडमिन द्वारा ब्लॉक (Blocked) कर दिया गया है। कृपया एडमिन से संपर्क करें।")
                return@launch
            }
            val now = System.currentTimeMillis()
            if (user.expiresAt in 1..now) {
                onError("⚠️ आपका सब्सक्रिप्शन समाप्त (Expired) हो चुका है। कृपया रिन्यू कराने के लिए एडमिन से संपर्क करें।")
                return@launch
            }

            // 6. Successful credentials & status check - Save Session
            com.example.data.security.UserSessionManager.saveSession(getApplication(), user)

            // 6. MULTI-DEVICE DATA SYNC (Phone A -> Phone B):
            // Automatically download and restore the user's complete shop snapshot from cloud
            // so staff, advance salary, products, attendance, and orders appear on this new phone!
            try {
                val res = com.example.util.PermanentVaultHelper.restoreShopSnapshot(
                    getApplication(),
                    repository.database,
                    user.phone,
                    forceCloud = true
                )
                if (res.getOrDefault(false)) {
                    showMessage("✅ क्लाउड से आपका संपूर्ण डेटा (स्टाफ, पेरोल, बिल) लोड हो गया!")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Auto-restore error on login", e)
            }

            _screenStack.clear()
            _screenStack.add(Screen.Dashboard)
            _currentScreen.value = Screen.Dashboard
            showMessage("लॉगिन सफल! स्वागत है ${user.name}")

            try {
                val liveCfg = com.example.util.PermanentVaultHelper.fetchLiveCloudAdminConfig()
                val tgCfg = com.example.util.TelegramActiveConfig.resolve(
                    repository.adminGlobalConfigDao.getConfigSync(),
                    repository.getCurrentShopSettings(),
                    liveCfg
                )
                com.example.util.TelegramNotifier.notifyUserAction(
                    tgCfg,
                    user.name,
                    user.phone,
                    "🔑 सफल यूजर लॉगिन: ${user.name} (${user.shopName}) ने ऐप में प्रवेश किया"
                )
            } catch (_: Exception) {}

            onSuccess()
        }
    }

    fun registerClientUser(
        name: String,
        shopName: String,
        phone: String,
        password: String,
        address: String,
        onSuccess: (com.example.data.model.AppUser) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.registerClientUser(getApplication(), name, shopName, phone, password, address)
            res.fold(
                onSuccess = { created ->
                    onSuccess(created)
                },
                onFailure = { err ->
                    onError(err.message ?: "रजिस्ट्रेशन विफल रहा")
                }
            )
        }
    }

    // Recycle Bin / Trash
    val recycleItems = repository.allRecycleItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recycleCount = repository.recycleBinCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun restoreRecycleItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.restoreRecycleItem(id)
            res.fold(
                onSuccess = { msg -> showMessage(msg) },
                onFailure = { err -> showMessage("रीस्टोर विफल: ${err.message}") }
            )
        }
    }

    fun restoreAllRecycleItems() {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.restoreAllRecycleItems()
            res.fold(
                onSuccess = { count -> showMessage("कुल $count आइटम अपनी जगह रीस्टोर कर दिए गए!") },
                onFailure = { err -> showMessage("रीस्टोर विफल: ${err.message}") }
            )
        }
    }

    fun deleteRecycleItemPermanently(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRecycleItemPermanently(id)
            showMessage("आइटम हमेशा के लिए हटा दिया गया")
        }
    }

    fun clearRecycleBin() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearRecycleBin()
            showMessage("रीसायकल बिन खाली कर दिया गया")
        }
    }

    // Cloud Backup & Multi-device Sync
    fun syncCloudBackup(onDone: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = com.example.data.security.UserSessionManager.getCurrentUser(getApplication())
            val phone = user?.phone ?: repository.getCurrentShopSettings().ownerPhone
            if (phone.isBlank()) {
                onError("कृपया पहले सेटिंग्स में मोबाइल नंबर सेव करें")
                return@launch
            }
            val res = repository.syncCloudBackupForUser(getApplication(), phone)
            res.fold(
                onSuccess = {
                    showMessage("☁️ क्लाउड बैकअप सुरक्षित रूप से सहेज लिया गया!")
                    onDone()
                },
                onFailure = { err ->
                    onError(err.message ?: "क्लाउड सिंक विफल")
                }
            )
        }
    }

    fun restoreCloudBackup(onDone: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = com.example.data.security.UserSessionManager.getCurrentUser(getApplication())
            val phone = user?.phone ?: repository.getCurrentShopSettings().ownerPhone
            if (phone.isBlank()) {
                onError("कृपया पहले मोबाइल नंबर दर्ज करें")
                return@launch
            }
            val res = repository.restoreCloudBackupForUser(getApplication(), phone)
            res.fold(
                onSuccess = { found ->
                    if (found) {
                        showMessage("✅ क्लाउड से आपका सारा डेटा वापस लोड हो गया!")
                        onDone()
                    } else {
                        onError("इस नंबर के लिए कोई पुराना क्लाउड बैकअप नहीं मिला")
                    }
                },
                onFailure = { err ->
                    onError(err.message ?: "रीस्टोर विफल")
                }
            )
        }
    }

    fun logoutClientUser() {
        com.example.data.security.UserSessionManager.logout(getApplication())
        _screenStack.clear()
        _screenStack.add(Screen.Login)
        _currentScreen.value = Screen.Login
        showMessage("लॉगआउट हो गया")
    }

    fun changeCurrentUserPassword(
        oldPass: String,
        newPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val sessionUser = com.example.data.security.UserSessionManager.getCurrentUser(getApplication())
            if (sessionUser == null) {
                onError("लॉगिन सत्र नहीं मिला")
                return@launch
            }
            val dbUser = repository.getUserById(sessionUser.id) ?: repository.getUserByPhone(sessionUser.phone)
            if (dbUser == null) {
                onError("यूजर डेटाबेस में नहीं मिला")
                return@launch
            }
            if (dbUser.password.trim() != oldPass.trim()) {
                onError("वर्तमान पासवर्ड गलत है!")
                return@launch
            }
            if (newPass.trim().length < 4) {
                onError("नया पासवर्ड कम से कम 4 अक्षरों का होना चाहिए!")
                return@launch
            }
            repository.updateUserPassword(dbUser.id, newPass.trim(), getApplication())
            com.example.data.security.UserSessionManager.updatePassword(getApplication(), newPass.trim())
            showMessage("पासवर्ड सफलतापूर्वक बदल दिया गया!")
            onSuccess()
        }
    }

    // Messages
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun showMessage(msg: String) {
        viewModelScope.launch { _userMessage.emit(msg) }
    }

    // Repository Flows
    val shopSettings: StateFlow<ShopSettings> = repository.shopSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShopSettings())

    val sizePricings: StateFlow<List<SizePricing>> = repository.sizePricings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<TileProduct>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<OrderEntity>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val staffMembers: StateFlow<List<com.example.data.model.StaffMember>> = repository.allStaff
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeStaff: StateFlow<List<com.example.data.model.StaffMember>> = repository.activeStaff
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentStaffPayments: StateFlow<List<com.example.data.model.StaffPayment>> = repository.recentStaffPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayDateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayAttendance: StateFlow<List<com.example.data.model.StaffAttendance>> = repository.getAttendanceForDate(todayDateString)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard Statistics
    val dashboardStats: StateFlow<DashboardStats> = combine(
        products,
        customers,
        orders
    ) { prodList, custList, ordList ->
        var boxes = 0
        var loose = 0
        var totalPieces = 0
        var lowStock = 0
        for (p in prodList) {
            boxes += p.fullBoxes
            loose += p.loosePieces
            totalPieces += p.stockPieces
            if (p.isLowStock) lowStock++
        }

        var pendingCount = 0
        var pendingVal = 0.0
        var confirmedCount = 0
        var confirmedVal = 0.0

        for (o in ordList) {
            if (o.status == "CONFIRMED") {
                confirmedCount++
                confirmedVal += o.grandTotal
            } else {
                pendingCount++
                pendingVal += o.grandTotal
            }
        }

        DashboardStats(
            totalProducts = prodList.size,
            totalCustomers = custList.size,
            availableBoxes = boxes,
            availableLoosePieces = loose,
            totalAvailablePieces = totalPieces,
            pendingOrdersCount = pendingCount,
            pendingOrdersValue = pendingVal,
            confirmedSalesCount = confirmedCount,
            confirmedSalesValue = confirmedVal,
            totalBoxesSold = 0,
            lowStockCount = lowStock
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    val stockMovements: StateFlow<List<com.example.data.model.StockMovement>> = repository.stockMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // ------------------------------------------------------------------------
    // Product Operations
    // ------------------------------------------------------------------------
    fun saveProduct(
        id: Long,
        code: String,
        name: String,
        size: String,
        photoUri: String?,
        description: String,
        piecesPerBox: Int,
        stockPieces: Int,
        defaultSellingPrice: Double,
        purchaseCost: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val product = TileProduct(
                id = id,
                code = code.trim().uppercase(Locale.getDefault()),
                name = name.trim(),
                size = size.trim(),
                photoUri = photoUri,
                description = description.trim(),
                piecesPerBox = piecesPerBox.coerceAtLeast(1),
                stockPieces = stockPieces.coerceAtLeast(0),
                defaultSellingPrice = defaultSellingPrice.coerceAtLeast(0.0),
                purchaseCost = purchaseCost.coerceAtLeast(0.0)
            )

            val result = if (id == 0L) {
                repository.addProduct(product)
            } else {
                repository.updateProduct(product)
            }

            if (result.isSuccess) {
                triggerAutoVaultSave()
                try {
                    val tgConfig = getActiveTelegramConfig()
                    com.example.util.TelegramNotifier.notifyNewProductOrStock(
                        tgConfig,
                        if (id == 0L) "नया उत्पाद जोड़ा गया (New Product)" else "उत्पाद अपडेट हुआ (Product Updated)",
                        product.name,
                        product.code,
                        product.size,
                        product.stockDisplay,
                        product.defaultSellingPrice
                    )
                } catch (_: Exception) {}
                showMessage(if (id == 0L) "Product added successfully!" else "Product updated successfully!")
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Error saving product")
            }
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProduct(id)
            triggerAutoVaultSave()
            showMessage("Product deleted.")
        }
    }

    fun adjustProductStock(productId: Long, newStockPieces: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.adjustDirectStock(productId, newStockPieces)
            triggerAutoVaultSave()
            try {
                val prod = repository.getProductById(productId)
                if (prod != null) {
                    val tgConfig = getActiveTelegramConfig()
                    com.example.util.TelegramNotifier.notifyNewProductOrStock(
                        tgConfig,
                        "स्टॉक समायोजन (Stock Adjusted)",
                        prod.name,
                        prod.code,
                        prod.size,
                        prod.stockDisplay,
                        prod.defaultSellingPrice
                    )
                    val threshold = adminGlobalConfig.value?.lowStockThreshold ?: 10
                    if (prod.fullBoxes <= threshold) {
                        com.example.util.TelegramNotifier.notifyLowStock(
                            tgConfig,
                            prod,
                            prod.fullBoxes,
                            threshold
                        )
                    }
                }
            } catch (_: Exception) {}
            showMessage("Stock updated.")
        }
    }

    // ------------------------------------------------------------------------
    // Customer Operations
    // ------------------------------------------------------------------------
    fun saveCustomer(
        id: Long,
        name: String,
        phone: String,
        address: String,
        notes: String,
        outstandingBalance: Double = 0.0,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val customer = Customer(
                id = id,
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                notes = notes.trim(),
                outstandingBalance = outstandingBalance
            )
            val newId = repository.saveCustomer(customer)
            triggerAutoVaultSave()
            showMessage(if (id == 0L) "Customer added." else "Customer updated.")
            onSuccess(newId)
        }
    }

    fun deleteCustomer(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomer(id)
            triggerAutoVaultSave()
            showMessage("Customer deleted.")
        }
    }

    // ------------------------------------------------------------------------
    // Order Operations
    // ------------------------------------------------------------------------
    fun createSaleOrder(
        invoiceNumber: String,
        customerId: Long?,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        status: String,
        showPriceOnPdf: Boolean,
        subtotal: Double,
        discount: Double,
        grandTotal: Double,
        paidAmount: Double = 0.0,
        notes: String,
        items: List<OrderItemEntity>,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val validPaid = paidAmount.coerceIn(0.0, grandTotal)
            val balance = (grandTotal - validPaid).coerceAtLeast(0.0)
            val historyJson = if (validPaid > 0) {
                val arr = org.json.JSONArray()
                arr.put(org.json.JSONObject().apply {
                    put("amount", validPaid)
                    put("date", SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date()))
                    put("mode", "ADVANCE_AT_BILLING")
                    put("note", "बिल बनाते समय अग्रिम जमा (Initial Advance)")
                    put("balanceAfter", balance)
                })
                arr.toString()
            } else ""

            val order = OrderEntity(
                invoiceNumber = invoiceNumber.trim(),
                customerId = customerId,
                customerName = customerName.trim().ifBlank { "Cash Customer" },
                customerPhone = customerPhone.trim(),
                customerAddress = customerAddress.trim(),
                status = status,
                showPriceOnPdf = showPriceOnPdf,
                subtotal = subtotal,
                discount = discount,
                grandTotal = grandTotal,
                paidAmount = validPaid,
                balanceAmount = balance,
                paymentHistoryJson = historyJson,
                notes = notes.trim()
            )

            val result = repository.createOrder(order, items)
            if (result.isSuccess) {
                val orderId = result.getOrThrow()
                triggerAutoVaultSave()
                try {
                    val tgConfig = getActiveTelegramConfig()
                    com.example.util.TelegramNotifier.notifyNewSale(
                        tgConfig,
                        com.example.data.model.OrderWithItems(order.copy(id = orderId), items)
                    )
                    // Low stock alert for items
                    val threshold = adminGlobalConfig.value?.lowStockThreshold ?: 10
                    for (item in items) {
                        val prod = repository.getProductById(item.productId)
                        if (prod != null && prod.fullBoxes <= threshold) {
                            com.example.util.TelegramNotifier.notifyLowStock(
                                tgConfig,
                                prod,
                                prod.fullBoxes,
                                threshold
                            )
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MainViewModel", "Telegram notify sale error", e)
                }
                showMessage(if (status == "CONFIRMED") "Sale confirmed & stock deducted!" else "Order saved as Pending!")
                onSuccess(orderId)
            } else {
                onError(result.exceptionOrNull()?.message ?: "Failed to create order")
            }
        }
    }

    fun confirmPendingOrder(orderId: Long, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.confirmPendingOrder(orderId)
            if (result.isSuccess) {
                triggerAutoVaultSave()
                showMessage("Order confirmed & inventory stock updated!")
                onDone(true, null)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Failed to confirm order"
                showMessage("Error: $error")
                onDone(false, error)
            }
        }
    }

    fun updateOrder(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.updateOrder(order, items)
            if (result.isSuccess) {
                triggerAutoVaultSave()
                showMessage("Order updated successfully!")
                onSuccess()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to update order"
                onError(err)
            }
        }
    }

    fun returnTileFromOrder(
        orderId: Long,
        orderItemId: Long,
        returnBoxes: Int,
        returnLoose: Int,
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.processTileReturn(orderId, orderItemId, returnBoxes, returnLoose, reason)
            if (res.isSuccess) {
                triggerAutoVaultSave()
                showMessage("टाइल सफलतापूर्वक वापस हो गई और स्टॉक में जुड़ गई!")
                onSuccess()
            } else {
                val err = res.exceptionOrNull()?.message ?: "टाइल वापसी असफल रही"
                onError(err)
            }
        }
    }

    fun addOrderPayment(
        orderId: Long,
        additionalAmount: Double,
        paymentMode: String,
        note: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.addOrderPayment(orderId, additionalAmount, paymentMode, note)
            if (result.isSuccess) {
                val newBal = result.getOrThrow()
                triggerAutoVaultSave()
                val order = repository.getOrderById(orderId)
                if (order != null) {
                    try {
                        val tgConfig = getActiveTelegramConfig()
                        val text = "💰 *भुगतान जमा (Payment Received)*\n" +
                                "बिल क्र.: #${order.invoiceNumber}\n" +
                                "ग्राहक: ${order.customerName}\n" +
                                "जमा राशि: ₹${additionalAmount.toInt()} ($paymentMode)\n" +
                                "शेष बकाया: ₹${newBal.toInt()}\n" +
                                if (note.isNotBlank()) "नोट: $note\n" else ""
                        com.example.util.TelegramNotifier.sendActiveMessage(tgConfig, text)
                    } catch (_: Exception) {}
                }
                showMessage("₹${additionalAmount.toInt()} राशि जमा हो गई! शेष बकाया: ₹${newBal.toInt()}")
                onSuccess()
            } else {
                val err = result.exceptionOrNull()?.message ?: "भुगतान जमा करने में त्रुटि"
                showMessage(err)
                onError(err)
            }
        }
    }

    fun deleteOrder(orderId: Long, restoreStock: Boolean, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminConfig = adminGlobalConfig.value ?: repository.adminGlobalConfigDao.getConfigSync()
            if (adminConfig != null && !adminConfig.allowInvoiceDeletion) {
                showMessage("⚠️ एडमिन द्वारा इनवॉइस डिलीट करना प्रतिबंधित (Disabled) है।")
                onDone()
                return@launch
            }
            val orderToDelete = repository.getOrderWithItemsSync(orderId)?.order
            val result = repository.deleteOrder(orderId, restoreStock)
            if (result.isSuccess) {
                triggerAutoVaultSave()
                try {
                    val tgConfig = getActiveTelegramConfig()
                    if (orderToDelete != null) {
                        com.example.util.TelegramNotifier.notifyInvoiceDeleted(
                            tgConfig,
                            orderToDelete.invoiceNumber,
                            orderToDelete.customerName,
                            orderToDelete.grandTotal,
                            if (restoreStock) "आर्डर हटाया गया एवं स्टॉक पुनः जोड़ा गया" else "आर्डर हटाया गया"
                        )
                    }
                } catch (_: Exception) {}
                showMessage(if (restoreStock) "Order deleted and stock restored!" else "Order deleted without stock change.")
                onDone()
            } else {
                showMessage("Failed to delete order: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun toggleShowPriceOnPdf(orderId: Long, showPrice: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePdfPriceVisibility(orderId, showPrice)
            showMessage(if (showPrice) "Prices will appear on PDF" else "Prices will be hidden on PDF (Quantity Slip)")
        }
    }

    // ------------------------------------------------------------------------
    // PDF & Sharing
    // ------------------------------------------------------------------------
    fun generateOrderPdf(
        context: Context,
        orderId: Long,
        onGenerated: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val orderWithItems = repository.getOrderWithItemsSync(orderId)
                if (orderWithItems == null) {
                    onError("Order not found")
                    return@launch
                }
                val settings = repository.getSettingsSync() ?: ShopSettings()
                val pdfFile = PdfGenerator.generateOrderPdf(context, orderWithItems, settings)
                onGenerated(pdfFile)
            } catch (e: Exception) {
                onError("Failed to generate PDF: ${e.localizedMessage}")
            }
        }
    }

    // ------------------------------------------------------------------------
    // Shop Settings & Size Pricing
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Shop Settings & Size Pricing
    // ------------------------------------------------------------------------
    fun saveSettings(
        shopName: String,
        subtitle: String,
        ownerName: String = "",
        ownerPhone: String,
        altPhone: String = "",
        address: String,
        logoUri: String?,
        gstNumber: String = "",
        panNumber: String = "",
        bankName: String = "",
        bankAccountNumber: String = "",
        bankIfsc: String = "",
        bankBranch: String = "",
        invoicePrefix: String = "INV-",
        defaultTaxPercent: Double = 0.0,
        showQrOnInvoice: Boolean = true,
        showBankOnInvoice: Boolean = true,
        invoiceFooterNotes: String = "",
        termsNotes: String,
        appTheme: String = "NAVY_GOLD",
        upiId: String = "",
        // Telegram Bot parameters
        telegramBotToken: String = "",
        telegramChatId: String = "",
        telegramEnabled: Boolean = false,
        telegramFormat: String = "DETAILED_HINDI",
        telegramCustomTitle: String = "TILES AND MARBLES",
        notifyNewSale: Boolean = true,
        notifyPaymentReceived: Boolean = true,
        notifyLowStock: Boolean = true,
        notifyStaffAttendance: Boolean = true,
        notifyDailySummary: Boolean = true,
        autoBackupEnabled: Boolean = false,
        backupTargetEmail: String = "",
        backupScheduledTime: String = "20:00",
        backupFromDateString: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSettings = repository.getCurrentShopSettings()
            val s = ShopSettings(
                shopName = shopName.trim(),
                subtitle = subtitle.trim(),
                ownerName = ownerName.trim(),
                ownerPhone = ownerPhone.trim(),
                altPhone = altPhone.trim(),
                address = address.trim(),
                logoUri = logoUri,
                gstNumber = gstNumber.trim(),
                panNumber = panNumber.trim(),
                bankName = bankName.trim(),
                bankAccountNumber = bankAccountNumber.trim(),
                bankIfsc = bankIfsc.trim(),
                bankBranch = bankBranch.trim(),
                invoicePrefix = invoicePrefix.trim().ifBlank { "INV-" },
                defaultTaxPercent = defaultTaxPercent,
                showQrOnInvoice = showQrOnInvoice,
                showBankOnInvoice = showBankOnInvoice,
                invoiceFooterNotes = invoiceFooterNotes.trim(),
                termsNotes = termsNotes.trim(),
                appTheme = appTheme.trim(),
                upiId = upiId.trim(),
                telegramBotToken = telegramBotToken.trim(),
                telegramChatId = telegramChatId.trim(),
                telegramEnabled = telegramEnabled,
                telegramFormat = telegramFormat.trim(),
                telegramCustomTitle = telegramCustomTitle.trim(),
                notifyNewSale = notifyNewSale,
                notifyPaymentReceived = notifyPaymentReceived,
                notifyLowStock = notifyLowStock,
                notifyStaffAttendance = notifyStaffAttendance,
                notifyDailySummary = notifyDailySummary,
                autoAttendanceEnabled = currentSettings.autoAttendanceEnabled,
                autoBackupEnabled = autoBackupEnabled,
                backupTargetEmail = backupTargetEmail.trim(),
                backupScheduledTime = backupScheduledTime.trim().ifBlank { "20:00" },
                backupFromDateString = backupFromDateString.trim(),
                lastAutoBackupDate = currentSettings.lastAutoBackupDate,
                pdfSizeDisplayConfigJson = currentSettings.pdfSizeDisplayConfigJson
            )
            repository.saveSettings(s)

            // Bi-directional sync with AppUser so that user app settings update the registered user
            val sessionUser = com.example.data.security.UserSessionManager.getCurrentUser(getApplication())
            if (sessionUser != null) {
                val dbUser = repository.getUserById(sessionUser.id) ?: repository.getUserByPhone(sessionUser.phone)
                if (dbUser != null) {
                    val updated = dbUser.copy(
                        name = ownerName.trim().ifBlank { dbUser.name },
                        shopName = shopName.trim().ifBlank { dbUser.shopName },
                        phone = ownerPhone.trim().ifBlank { dbUser.phone }
                    )
                    repository.updateUser(updated, getApplication())
                    com.example.data.security.UserSessionManager.saveSession(getApplication(), updated)
                }
            }

            triggerAutoVaultSave()
            showMessage("दुकान व सेटिंग्स सुरक्षित हो गईं!")
        }
    }

    fun savePdfSizeDisplayConfig(configJson: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSettings = repository.getCurrentShopSettings()
            repository.saveSettings(currentSettings.copy(pdfSizeDisplayConfigJson = configJson))
            triggerAutoVaultSave()
            showMessage("PDF टाइल साइज़ डिस्प्ले सेटिंग सेव हो गई!")
        }
    }

    fun saveShopSettings(settings: ShopSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSettings(settings)
            triggerAutoVaultSave()
        }
    }

    fun testTelegramBot(
        botToken: String,
        chatId: String,
        format: String,
        customTitle: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = repository.getCurrentShopSettings()
            val result = com.example.util.TelegramNotifier.sendTestMessage(
                settings = settings,
                botToken = botToken,
                chatId = chatId,
                format = format,
                customTitle = customTitle
            )
            onResult(result.first, result.second)
        }
    }

    fun saveSizePricing(sizeName: String, pricePerBox: Double, piecesPerBox: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSizePricing(sizeName, pricePerBox, piecesPerBox)
            showMessage("Pricing & packing for $sizeName saved!")
        }
    }

    fun updateSizePricingAndProducts(
        sizeName: String,
        pricePerBox: Double,
        piecesPerBox: Int,
        updateExistingProducts: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSizePricing(sizeName, pricePerBox, piecesPerBox)
            if (updateExistingProducts) {
                val count = repository.updateProductsOfSize(sizeName, pricePerBox, piecesPerBox)
                showMessage("रेट सेव हुआ और $sizeName की $count टाइल्स भी अपडेट हो गईं!")
            } else {
                showMessage("Pricing & packing for $sizeName saved!")
            }
            triggerAutoVaultSave()
        }
    }

    fun deleteSizePricing(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSizePricing(id)
            showMessage("Size pricing deleted.")
        }
    }

    // Copy selected image from Gallery Uri to internal app storage
    fun persistImageLocally(context: Context, sourceUri: Uri): String? {
        return try {
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) imagesDir.mkdirs()

            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val destFile = File(imagesDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    // ------------------------------------------------------------------------
    // Backup & Restore
    // ------------------------------------------------------------------------
    fun createBackup(context: Context, onDone: (File) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = BackupRestoreHelper.createBackup(context, database)
                showMessage("बैकअप (टाइल फोटो सहित) तैयार: ${file.name}")
                onDone(file)
            } catch (e: Exception) {
                onError("Backup failed: ${e.localizedMessage}")
            }
        }
    }

    fun restoreBackup(context: Context, backupFile: File, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = BackupRestoreHelper.restoreBackup(context, database, backupFile)
            if (result.isSuccess) {
                showMessage(result.getOrNull() ?: "Backup restored successfully!")
                onDone()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Failed to restore backup")
            }
        }
    }

    fun generateNextInvoiceNumber(): String {
        val sdf = SimpleDateFormat("yyMMdd", Locale.getDefault())
        val datePart = sdf.format(Date())
        val randomPart = (100..999).random()
        return "MTM-$datePart-$randomPart"
    }

    // ------------------------------------------------------------------------
    // Staff & Salary Actions
    // ------------------------------------------------------------------------
    fun saveStaff(staff: com.example.data.model.StaffMember) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveStaff(staff)
            triggerAutoVaultSave()
            showMessage(if (staff.id == 0L) "कर्मचारी जोड़ा गया (${staff.name})" else "कर्मचारी अपडेट हुआ (${staff.name})")
        }
    }

    fun deleteStaff(staffId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStaff(staffId)
            triggerAutoVaultSave()
            showMessage("Staff removed")
        }
    }

    fun markAttendance(staffId: Long, dateMillis: Long, dateString: String, status: String, notes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAttendance(staffId, dateMillis, dateString, status, notes)
            triggerAutoVaultSave()
            val statusLabel = when (status) {
                "PRESENT" -> "उपस्थित (Present)"
                "HALF_DAY" -> "आधा दिन (Half Day)"
                "ABSENT" -> "छुट्टी / गैरहाजिर (Absent)"
                "PAID_LEAVE" -> "सवेतन अवकाश (Paid Leave)"
                else -> status
            }
            try {
                val tgConfig = getActiveTelegramConfig()
                val staff = repository.getStaffById(staffId)
                if (staff != null) {
                    com.example.util.TelegramNotifier.notifyStaffAttendance(tgConfig, staff.name, dateString, status, notes)
                }
            } catch (e: Exception) {
                // Ignore telegram errors
            }
            showMessage("हाजिरी दर्ज: $statusLabel")
        }
    }

    fun deleteAttendance(staffId: Long, dateString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAttendance(staffId, dateString)
            triggerAutoVaultSave()
            showMessage("$dateString की हाजिरी प्रविष्टि हटा दी गई")
        }
    }

    fun recordAdvanceOrPayment(
        staffId: Long,
        amount: Double,
        type: String,
        forMonthYear: String,
        mode: String,
        notes: String,
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminConfig = adminGlobalConfig.value ?: repository.adminGlobalConfigDao.getConfigSync()
            if (type == "ADVANCE" && adminConfig != null && !adminConfig.allowSalaryAdvance) {
                showMessage("⚠️ एडमिन द्वारा कर्मचारियों को एडवांस सैलरी देना प्रतिबंधित (Disabled) है।")
                return@launch
            }
            repository.recordStaffPayment(
                com.example.data.model.StaffPayment(
                    staffId = staffId,
                    dateMillis = dateMillis,
                    amount = amount,
                    type = type,
                    forMonthYear = forMonthYear,
                    paymentMode = mode,
                    notes = notes
                )
            )
            triggerAutoVaultSave()
            try {
                val tgConfig = getActiveTelegramConfig()
                val staff = repository.getStaffById(staffId)
                if (staff != null) {
                    com.example.util.TelegramNotifier.notifySalaryOrAdvancePaid(
                        tgConfig,
                        staff.name,
                        type,
                        amount,
                        mode,
                        staff.openingBalance,
                        notes
                    )
                }
            } catch (_: Exception) {}
            val typeLabel = if (type == "ADVANCE") "एडवांस दर्ज: ₹$amount" else "वेतन भुगतान दर्ज: ₹$amount"
            showMessage(typeLabel)
        }
    }

    fun deleteStaffPayment(paymentId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStaffPayment(paymentId)
            triggerAutoVaultSave()
            showMessage("Payment entry removed")
        }
    }

    // ------------------------------------------------------------------------
    // Staff Attendance for Any Date
    // ------------------------------------------------------------------------
    fun getAttendanceForDate(dateString: String): kotlinx.coroutines.flow.Flow<List<com.example.data.model.StaffAttendance>> {
        return repository.getAttendanceForDate(dateString)
    }

    // ------------------------------------------------------------------------
    // Staff Monthly Settlements (Yes/No Status)
    // ------------------------------------------------------------------------
    fun getSettlementsForStaff(staffId: Long): kotlinx.coroutines.flow.Flow<List<com.example.data.model.StaffMonthlySettlement>> =
        repository.getSettlementsForStaff(staffId)

    fun saveStaffSettlement(
        staffId: Long,
        monthKey: String,
        isFinalized: Boolean,
        notes: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveStaffSettlement(
                com.example.data.model.StaffMonthlySettlement(
                    staffId = staffId,
                    monthKey = monthKey,
                    isFinalized = isFinalized,
                    settledAt = System.currentTimeMillis(),
                    notes = notes
                )
            )
            triggerAutoVaultSave()
            showMessage(if (isFinalized) "✔ $monthKey का हिसाब फाइनल कर दिया गया!" else "⏳ $monthKey का हिसाब अनफाइनल रखा गया (अगले माह जुड़ेगा)")
        }
    }

    // ------------------------------------------------------------------------
    // Auto Attendance Today
    // ------------------------------------------------------------------------
    fun runAutoAttendanceToday() {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = repository.getCurrentShopSettings()
            if (settings.autoAttendanceEnabled) {
                val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date())
                val count = repository.autoMarkPresentForDate(todayStr)
                if (count > 0) {
                    triggerAutoVaultSave()
                    showMessage("⚡ $count कर्मचारियों की आज की ऑटो-हाजिरी दर्ज हो गई!")
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Cumulative Daily Backup & Share to Email / Google Drive
    // ------------------------------------------------------------------------
    fun triggerCumulativeBackupAndShare(
        context: Context,
        customFromDateString: String? = null,
        onFileReady: (File, android.content.Intent) -> Unit = { _, shareIntent ->
            val chooser = android.content.Intent.createChooser(shareIntent, "Save or Share Backup")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = repository.getCurrentShopSettings()
                val fromDate = customFromDateString ?: settings.backupFromDateString
                val file = BackupRestoreHelper.createCumulativeBackupFile(
                    context,
                    repository.database,
                    fromDate
                )
                val shareIntent = BackupRestoreHelper.createShareBackupIntent(
                    context,
                    file,
                    settings.backupTargetEmail
                )
                showMessage("बैकअप तैयार: ${file.name}")
                onFileReady(file, shareIntent)
            } catch (e: Exception) {
                showMessage("बैकअप विफल: ${e.localizedMessage}")
            }
        }
    }

    // ------------------------------------------------------------------------
    // Client Licenses & Admin Licensing Control
    // ------------------------------------------------------------------------
    val clientLicenses: StateFlow<List<com.example.data.model.ClientLicense>> = repository.allLicenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveClientLicense(license: com.example.data.model.ClientLicense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveLicense(license)
            showMessage("नया लाइसेंस सुरक्षित हुआ (License Saved)")
        }
    }

    fun deleteClientLicense(licenseId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLicense(licenseId)
            showMessage("लाइसेंस हटाया गया (Deleted)")
        }
    }

    fun toggleClientLicenseStatus(license: com.example.data.model.ClientLicense) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = license.copy(isActive = !license.isActive)
            repository.updateLicense(updated)
            showMessage(if (updated.isActive) "लाइसेंस सक्रिय किया गया" else "लाइसेंस निलंबित (Deactivated)")
        }
    }

    fun extendClientLicense(license: com.example.data.model.ClientLicense, addDays: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val baseTime = if (license.expiresAt > System.currentTimeMillis()) license.expiresAt else System.currentTimeMillis()
            val newExpiresAt = baseTime + (addDays * 24L * 60 * 60 * 1000L)
            val updated = license.copy(
                expiresAt = newExpiresAt,
                isActive = true,
                planDays = license.planDays + addDays
            )
            repository.updateLicense(updated)
            showMessage("लाइसेंस $addDays दिनों के लिए बढ़ाया गया")
        }
    }
}
