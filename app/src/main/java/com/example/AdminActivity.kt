package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.model.AppUser
import com.example.data.repository.MouryaRepository
import com.example.data.security.LicenseManager
import com.example.ui.theme.MouryaTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.getAppThemeOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = MouryaRepository(database)

        setContent {
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }
            val activeTheme = getAppThemeOption("NAVY_GOLD")

            var isAdminUnlocked by remember { mutableStateOf(false) }
            var adminPinInput by remember { mutableStateOf("") }
            var adminPinError by remember { mutableStateOf(false) }

            MouryaTheme(themeOption = activeTheme) {
                if (!isAdminUnlocked) {
                    // Admin PIN Gate
                    Scaffold(
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        containerColor = activeTheme.primary,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.background)
                                .padding(24.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(activeTheme.accent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = activeTheme.accentText,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "TILES AND MARBLES ADMIN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = activeTheme.primary
                            )
                            Text(
                                text = "सुपर एडमिन नियंत्रण केंद्र (Master Control App)",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "मास्टर पिन दर्ज करें (Enter Master PIN):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = activeTheme.primary
                                    )

                                    OutlinedTextField(
                                        value = adminPinInput,
                                        onValueChange = {
                                            if (it.length <= 6) {
                                                adminPinInput = it
                                                adminPinError = false
                                            }
                                        },
                                        label = { Text("मास्टर एडमिन पिन *") },
                                        placeholder = { Text("पिन दर्ज करें") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        isError = adminPinError,
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (adminPinError) {
                                        Text(
                                            text = "❌ गलत पिन! कृपया सही मास्टर पिन दर्ज करें",
                                            color = StatusError,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (LicenseManager.verifyAdminPin(this@AdminActivity, adminPinInput)) {
                                                isAdminUnlocked = true
                                            } else {
                                                adminPinError = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("🔐 एडमिन ऐप खोलें", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Full Admin Application Screen
                    AdminAppContent(
                        repository = repository,
                        activeThemeColor = activeTheme.primary,
                        accentColor = activeTheme.accent,
                        accentTextColor = activeTheme.accentText,
                        onShowMessage = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminAppContent(
    repository: MouryaRepository,
    activeThemeColor: Color,
    accentColor: Color,
    accentTextColor: Color,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val usersList by repository.allUsers.collectAsStateWithLifecycle(initialValue = emptyList())

    var isCloudSyncing by remember { mutableStateOf(false) }

    suspend fun syncRegistrationsFromCloud(notifyIfNew: Boolean = false) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                // 1. Fetch live cloud users directory (all self-registered & active User App accounts)
                val cloudDirList = com.example.util.PermanentVaultHelper.fetchCloudPendingRegistrations()
                var newCount = 0
                for (pu in cloudDirList) {
                    val norm = com.example.util.PermanentVaultHelper.normalizePhone(pu.phone)
                    if (norm.isBlank() || com.example.util.PermanentVaultHelper.isPhoneDeleted(norm, context)) continue
                    val existing = repository.appUserDao.getUserByPhone(norm) ?: repository.appUserDao.getUserByPhone(pu.phone)
                    if (existing == null) {
                        repository.appUserDao.insert(pu.copy(id = 0L, phone = norm))
                        newCount++
                    }
                }

                // 2. Also sync with local persistent users vault
                val vaultUsers = com.example.util.PermanentVaultHelper.loadUsersRegistry(context)
                for (cu in vaultUsers) {
                    val norm = com.example.util.PermanentVaultHelper.normalizePhone(cu.phone)
                    if (norm.isBlank() || com.example.util.PermanentVaultHelper.isPhoneDeleted(norm, context)) continue
                    val existing = repository.appUserDao.getUserByPhone(norm) ?: repository.appUserDao.getUserByPhone(cu.phone)
                    if (existing == null) {
                        repository.appUserDao.insert(cu.copy(id = 0L, phone = norm))
                        newCount++
                    }
                }

                if (newCount > 0 && notifyIfNew) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        onShowMessage("🆕 $newCount नया यूजर क्लाउड से सिंक हुआ!")
                    }
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        repository.ensureDefaultUserExists()
        // Sync latest cloud AdminGlobalConfig once on startup so Admin App shows live settings
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val cloudCfg = com.example.util.PermanentVaultHelper.fetchLiveCloudAdminConfig()
                if (cloudCfg != null) {
                    repository.adminGlobalConfigDao.saveConfig(cloudCfg)
                }
            } catch (_: Exception) {}
        }
        // Continuous real-time listener: fetches new user registrations & active users every 5 seconds!
        while (true) {
            syncRegistrationsFromCloud(notifyIfNew = true)
            kotlinx.coroutines.delay(5000)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0 = All, 1 = Active, 2 = Suspended, 3 = Pending, 4 = Expiring <= 7d
    var cloudHealthStatusText by remember { mutableStateOf<String?>(null) }
    var isTestingCloud by remember { mutableStateOf(false) }

    var showCreateUserDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<AppUser?>(null) }
    var userForValidityDialog by remember { mutableStateOf<AppUser?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var newlyCreatedUser by remember { mutableStateOf<AppUser?>(null) }

    val pendingCount = remember(usersList) {
        usersList.count { it.approvalStatus == "PENDING" }
    }

    val expiringSoonUsers = remember(usersList) {
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24L * 60L * 60L * 1000L
        usersList.filter { u ->
            u.isActive && u.approvalStatus == "APPROVED" &&
                    u.expiresAt != Long.MAX_VALUE && u.planDays != -1 &&
                    u.expiresAt > now && (u.expiresAt - now) <= sevenDaysMs
        }
    }

    val filteredUsers = remember(usersList, searchQuery, selectedFilter) {
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24L * 60L * 60L * 1000L
        usersList.filter { u ->
            val matchesQuery = searchQuery.isBlank() ||
                    u.name.contains(searchQuery, ignoreCase = true) ||
                    u.shopName.contains(searchQuery, ignoreCase = true) ||
                    u.phone.contains(searchQuery, ignoreCase = true)

            val isExpired = u.expiresAt in 1..now
            val isExpiringSoon = u.isActive && u.approvalStatus == "APPROVED" &&
                    u.expiresAt != Long.MAX_VALUE && u.planDays != -1 &&
                    u.expiresAt > now && (u.expiresAt - now) <= sevenDaysMs
            val matchesFilter = when (selectedFilter) {
                1 -> u.isActive && !isExpired && u.approvalStatus == "APPROVED"
                2 -> !u.isActive || isExpired || u.approvalStatus == "REJECTED"
                3 -> u.approvalStatus == "PENDING"
                4 -> isExpiringSoon
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    val activeCount = remember(usersList) {
        val now = System.currentTimeMillis()
        usersList.count { it.isActive && (it.expiresAt == Long.MAX_VALUE || it.expiresAt > now) && it.approvalStatus == "APPROVED" }
    }
    val totalRevenue = remember(usersList) { usersList.sumOf { it.amountCharged } }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = activeThemeColor,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Tiles & Marbles Admin",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = accentColor
                            ) {
                                Text(
                                    text = "CONTROL CONSOLE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentTextColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "यूजर आईडी, पासवर्ड व ग्राहक एक्सेस नियंत्रण",
                            fontSize = 11.sp,
                            color = Color(0xFF93C5FD)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isCloudSyncing = true
                                syncRegistrationsFromCloud(notifyIfNew = false)
                                isCloudSyncing = false
                                onShowMessage("🔄 सभी डिवाइस से यूजर डेटा सिंक हो गया!")
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync Registrations", tint = PureWhite)
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = "Admin Settings", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = activeThemeColor)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stats Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = activeThemeColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        tint = accentTextColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "कुल रजिस्टर्ड यूजर: ${usersList.size}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = PureWhite
                                    )
                                    Text(
                                        text = "सक्रिय यूजर: $activeCount | ब्लॉक: ${usersList.size - activeCount}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "कुल शुल्क कलेक्शन", fontSize = 10.sp, color = Color(0xFFE2E8F0))
                                Text(
                                    text = "₹${String.format(Locale.US, "%,.0f", totalRevenue)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showCreateUserDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.45f)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = accentTextColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ नया यूजर जोड़ें",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = accentTextColor
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (isTestingCloud) return@OutlinedButton
                                    isTestingCloud = true
                                    cloudHealthStatusText = "⏳ लाइव क्लाउड सर्वर जांचा जा रहा है..."
                                    scope.launch(Dispatchers.IO) {
                                        fun pingHost(urlStr: String): Pair<Boolean, Long> {
                                            val start = System.currentTimeMillis()
                                            return try {
                                                val conn = (java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection).apply {
                                                    requestMethod = "GET"
                                                    connectTimeout = 5000
                                                    readTimeout = 5000
                                                }
                                                val code = conn.responseCode
                                                conn.disconnect()
                                                Pair(code in 200..499, System.currentTimeMillis() - start)
                                            } catch (_: Exception) {
                                                Pair(false, System.currentTimeMillis() - start)
                                            }
                                        }
                                        val (ok1, ms1) = pingHost("https://textdb.dev/api/data/mourya_health_ping")
                                        val (ok2, ms2) = pingHost("https://textdb.online/mourya_health_ping")
                                        val summary = buildString {
                                            append(if (ok1 || ok2) "✅ क्लाउड रिमोट कंट्रोल लाइव है | " else "❌ क्लाउड संपर्क विफल (इंटरनेट जांचें) | ")
                                            append("Server1: ${if (ok1) "✔ ${ms1}ms" else "✖"} • ")
                                            append("Server2: ${if (ok2) "✔ ${ms2}ms" else "✖"}")
                                        }
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            isTestingCloud = false
                                            cloudHealthStatusText = summary
                                            onShowMessage(summary)
                                        }
                                    }
                                },
                                border = BorderStroke(1.dp, accentColor),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isTestingCloud) "जांच..." else "☁️ क्लाउड टेस्ट",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = PureWhite
                                )
                            }
                        }

                        if (!cloudHealthStatusText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (cloudHealthStatusText!!.startsWith("✅")) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = cloudHealthStatusText!!,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureWhite,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Search & Filter
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("नाम, दुकान या मोबाइल नंबर से खोजें...") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = activeThemeColor)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            FilterChip(
                                selected = selectedFilter == 0,
                                onClick = { selectedFilter = 0 },
                                label = { Text("सभी (${usersList.size})") }
                            )
                            FilterChip(
                                selected = selectedFilter == 3,
                                onClick = { selectedFilter = 3 },
                                label = { Text("⏳ पेंडिंग अनुरोध ($pendingCount)") }
                            )
                            FilterChip(
                                selected = selectedFilter == 1,
                                onClick = { selectedFilter = 1 },
                                label = { Text("🟢 सक्रिय ($activeCount)") }
                            )
                            FilterChip(
                                selected = selectedFilter == 4,
                                onClick = { selectedFilter = 4 },
                                label = { Text("⏰ 7 दिन में समाप्त (${expiringSoonUsers.size})") }
                            )
                            FilterChip(
                                selected = selectedFilter == 2,
                                onClick = { selectedFilter = 2 },
                                label = { Text("🔴 ब्लॉक / समाप्त") }
                            )
                        }
                    }
                }
            }

            // Expiring Soon Alert Banner (<= 7 Days Remaining) with 1-Tap +30 Days Renew
            if (expiringSoonUsers.isNotEmpty() && selectedFilter != 4) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                        border = BorderStroke(1.dp, Color(0xFFFDBA74)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFilter = 4 }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "⏰ ${expiringSoonUsers.size} यूजर का प्लान 7 दिन में समाप्त हो रहा है!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF9A3412)
                                )
                                Text(
                                    text = "रिन्यूअल रिमाइंडर भेजने या +30 दिन बढ़ाने के लिए टैप करें।",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFFC2410C)
                                )
                            }
                            Button(
                                onClick = { selectedFilter = 4 },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("देखें", fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Pending Registrations Banner
            if (pendingCount > 0 && selectedFilter != 3) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFilter = 3 }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("⚠️ $pendingCount नया यूजर रजिस्ट्रेशन अनुरोध आया है!", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF92400E))
                                Text("स्वीकृत (Approve) या अस्वीकार करने के लिए यहाँ क्लिक करें।", fontSize = 11.sp, color = Color(0xFFB45309))
                            }
                            Button(
                                onClick = { selectedFilter = 3 },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("देखें", fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Users List
            if (filteredUsers.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "कोई यूजर नहीं मिला",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ऊपर दिए गए बटन से नया यूजर बनाकर उसे ID और Password दें।",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredUsers, key = { it.id }) { user ->
                    UserAccountCard(
                        user = user,
                        activeThemeColor = activeThemeColor,
                        onApprove = {
                            scope.launch(Dispatchers.IO) {
                                val ok = repository.approveUser(user.id, context).getOrDefault(false)
                                onShowMessage(if (ok) "✅ ${user.name} स्वीकृत (Approved) व क्लाउड सिंक सफल!" else "✅ ${user.name} लोकल स्वीकृत (इंटरनेट जांचें)")
                            }
                        },
                        onReject = {
                            scope.launch(Dispatchers.IO) {
                                val ok = repository.rejectUser(user.id, context).getOrDefault(false)
                                onShowMessage(if (ok) "❌ खाता अस्वीकार व क्लाउड से ब्लॉक हुआ!" else "❌ खाता लोकल अस्वीकार (इंटरनेट जांचें)")
                            }
                        },
                        onCopyCredentials = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = "User ID (Mobile): ${user.phone}\nPassword: ${user.password}"
                            clipboard.setPrimaryClip(ClipData.newPlainText("Credentials", clip))
                            onShowMessage("आईडी व पासवर्ड कॉपी हुआ")
                        },
                        onEditUser = {
                            userToEdit = user
                        },
                        onToggleStatus = {
                            scope.launch(Dispatchers.IO) {
                                val targetActive = !user.isActive
                                val ok = repository.updateUserStatus(user.id, targetActive, context)
                                val msg = when {
                                    targetActive && ok -> "✅ यूजर अनब्लॉक (Active) हुआ — User App तुरंत चालू होगा!"
                                    !targetActive && ok -> "🚫 यूजर ब्लॉक हुआ — User App 3 सेकंड में ऑटो-लॉगआउट होगा!"
                                    targetActive -> "⚠️ लोकल अनब्लॉक हुआ, पर क्लाउड सिंक विफल (इंटरनेट जांचें)"
                                    else -> "⚠️ लोकल ब्लॉक हुआ, पर क्लाउड सिंक विफल (इंटरनेट जांचें)"
                                }
                                onShowMessage(msg)
                            }
                        },
                        onManageValidity = {
                            userForValidityDialog = user
                        },
                        onShareWhatsApp = {
                            val cleanPhone = user.phone.replace("[^0-9]".toRegex(), "")
                            val expiryText = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(user.expiresAt))
                            val msg = """
                                नमस्ते ${user.name} जी!
                                आपके ${user.shopName} के लिए Mourya Tiles ERP ऐप का लॉगिन विवरण:
                                
                                📱 मोबाइल नंबर (User ID): ${user.phone}
                                🔑 पासवर्ड (Password): ${user.password}
                                📅 प्लान: ${user.planName} (वैधता: $expiryText)
                                
                                ऐप खोलें और अपने मोबाइल नंबर व पासवर्ड से लॉगिन करके इस्तेमाल शुरू करें। धन्यवाद!
                            """.trimIndent()

                            try {
                                val url = if (cleanPhone.length >= 10) {
                                    "https://api.whatsapp.com/send?phone=$cleanPhone&text=${URLEncoder.encode(msg, "UTF-8")}"
                                } else {
                                    "https://api.whatsapp.com/send?text=${URLEncoder.encode(msg, "UTF-8")}"
                                }
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (e: Exception) {
                                onShowMessage("WhatsApp नहीं खुला")
                            }
                        },
                        onQuickRenew30Days = {
                            scope.launch(Dispatchers.IO) {
                                val baseMs = maxOf(System.currentTimeMillis(), user.expiresAt)
                                val newExp = if (user.expiresAt == Long.MAX_VALUE) Long.MAX_VALUE else baseMs + (30L * 86400000L)
                                val updated = user.copy(expiresAt = newExp, isActive = true, approvalStatus = "APPROVED")
                                val ok = repository.updateUser(updated, context)
                                onShowMessage(if (ok) "✅ ${user.name} को +30 दिन रिन्यू व क्लाउड सिंक सफल!" else "✅ +30 दिन लोकल रिन्यू (इंटरनेट जांचें)")
                            }
                        },
                        onSendRenewalReminder = {
                            val cleanPhone = user.phone.replace("[^0-9]".toRegex(), "")
                            val expiryText = if (user.expiresAt == Long.MAX_VALUE) "Lifetime" else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(user.expiresAt))
                            val msg = """
                                नमस्ते ${user.name} जी! 🙏
                                आपकी दुकान ${user.shopName} के Tiles & Marbles ERP ऐप की वैधता ($expiryText) जल्द समाप्त हो रही है / समाप्त हो गई है।
                                
                                बिना किसी रुकावट के बिलिंग, स्टॉक व बैकअप जारी रखने के लिए कृपया अपना सब्सक्रिप्शन रिन्यू करवाएं। धन्यवाद!
                            """.trimIndent()
                            try {
                                val url = if (cleanPhone.length >= 10) {
                                    "https://api.whatsapp.com/send?phone=$cleanPhone&text=${URLEncoder.encode(msg, "UTF-8")}"
                                } else {
                                    "https://api.whatsapp.com/send?text=${URLEncoder.encode(msg, "UTF-8")}"
                                }
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (_: Exception) {
                                onShowMessage("WhatsApp नहीं खुला")
                            }
                        },
                        onDelete = {
                            scope.launch(Dispatchers.IO) {
                                val ok = repository.deleteUser(user.id, context)
                                onShowMessage(if (ok) "🗑️ यूजर हटाया गया — User App 3 सेकंड में लॉगआउट होगा!" else "🗑️ लोकल हटाया गया (क्लाउड सिंक हेतु इंटरनेट जांचें)")
                            }
                        }
                    )
                }
            }
        }
    }

    // Create User Modal
    if (showCreateUserDialog) {
        CreateUserDialog(
            onDismiss = { showCreateUserDialog = false },
            onSave = { name, shop, phone, pass, days, planName, amount, notes ->
                scope.launch(Dispatchers.IO) {
                    val expiresAt = if (days == -1) {
                        Long.MAX_VALUE
                    } else {
                        System.currentTimeMillis() + (days * 24L * 60 * 60 * 1000L)
                    }
                    val newUser = AppUser(
                        name = name,
                        shopName = shop,
                        phone = phone,
                        password = pass,
                        planDays = days,
                        planName = planName,
                        amountCharged = amount,
                        expiresAt = expiresAt,
                        notes = notes
                    )
                    val id = repository.saveUser(newUser, context)
                    newlyCreatedUser = newUser.copy(id = id)
                    showCreateUserDialog = false
                    onShowMessage("नया यूजर अकाउंट बन गया!")
                }
            }
        )
    }

    // Manage User Access & Days Modal (+ / - Stepper & Custom Days)
    userForValidityDialog?.let { targetUser ->
        ManageUserAccessDialog(
            user = targetUser,
            onDismiss = { userForValidityDialog = null },
            onSave = { newExpiresAt, newPlanDays, newPlanName ->
                scope.launch(Dispatchers.IO) {
                    val updated = targetUser.copy(
                        expiresAt = newExpiresAt,
                        planDays = newPlanDays,
                        planName = newPlanName,
                        isActive = true,
                        approvalStatus = "APPROVED"
                    )
                    val ok = repository.updateUser(updated, context)
                    userForValidityDialog = null
                    onShowMessage(if (ok) "✅ ${targetUser.name} की वैधता अपडेट व क्लाउड सिंक सफल!" else "✅ वैधता लोकल अपडेट हुई (इंटरनेट जांचें)")
                }
            }
        )
    }

    // Success Share Modal after creating user
    newlyCreatedUser?.let { created ->
        AlertDialog(
            onDismissRequest = { newlyCreatedUser = null },
            icon = {
                Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(40.dp))
            },
            title = {
                Text(text = "यूजर अकाउंट सफलतापूर्वक बना!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "ग्राहक: ${created.name} (${created.shopName})", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "📱 मोबाइल नंबर (ID): ${created.phone}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "🔑 पासवर्ड: ${created.password}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E3A8A))
                        }
                    }

                    Text(text = "यह लॉगिन विवरण ग्राहक को WhatsApp पर भेजें ताकि वे ऐप में लॉगिन कर सकें।", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanPhone = created.phone.replace("[^0-9]".toRegex(), "")
                        val msg = """
                            नमस्ते ${created.name} जी!
                            आपके ${created.shopName} के लिए Mourya Tiles ERP ऐप का लॉगिन विवरण:
                            
                            📱 मोबाइल (User ID): ${created.phone}
                            🔑 पासवर्ड: ${created.password}
                            📅 प्लान: ${created.planName}
                            
                            ऐप खोलें और लॉगिन करके शुरू करें।
                        """.trimIndent()
                        try {
                            val url = if (cleanPhone.length >= 10) {
                                "https://api.whatsapp.com/send?phone=$cleanPhone&text=${URLEncoder.encode(msg, "UTF-8")}"
                            } else {
                                "https://api.whatsapp.com/send?text=${URLEncoder.encode(msg, "UTF-8")}"
                            }
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (e: Exception) {
                            onShowMessage("WhatsApp नहीं खुला")
                        }
                        newlyCreatedUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp भेजें")
                }
            },
            dismissButton = {
                TextButton(onClick = { newlyCreatedUser = null }) {
                    Text("बंद करें")
                }
            }
        )
    }

    // Edit User Dialog (Full ID & Password Control for Admin)
    userToEdit?.let { user ->
        var editName by remember { mutableStateOf(user.name) }
        var editShop by remember { mutableStateOf(user.shopName) }
        var editPhone by remember { mutableStateOf(user.phone) }
        var editPass by remember { mutableStateOf(user.password) }
        var editNotes by remember { mutableStateOf(user.notes) }
        var editError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { userToEdit = null },
            icon = {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = activeThemeColor, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(text = "यूजर आईडी व पासवर्ड बदलें (Edit User)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "ग्राहक का मोबाइल नंबर (User ID) और पासवर्ड बदलें:", fontSize = 12.sp, color = Color(0xFF64748B))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("ग्राहक का नाम *") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editShop,
                        onValueChange = { editShop = it },
                        label = { Text("दुकान / फर्म का नाम *") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("मोबाइल नंबर (User ID) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPass,
                        onValueChange = { editPass = it },
                        label = { Text("पासवर्ड (Password) *") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("नोट्स (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (editError != null) {
                        Text(text = editError!!, color = StatusError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanPhone = editPhone.trim()
                        val cleanPass = editPass.trim()
                        if (editName.isBlank() || cleanPhone.isBlank() || cleanPass.isBlank()) {
                            editError = "नाम, मोबाइल और पासवर्ड खाली नहीं हो सकते!"
                            return@Button
                        }
                        scope.launch(Dispatchers.IO) {
                            val updated = user.copy(
                                name = editName.trim(),
                                shopName = editShop.trim(),
                                phone = cleanPhone,
                                password = cleanPass,
                                notes = editNotes.trim()
                            )
                            val ok = repository.updateUser(updated, context, previousPhone = user.phone)
                            onShowMessage(if (ok) "✅ यूजर विवरण व ID/Password अपडेट व क्लाउड सिंक सफल!" else "✅ लोकल अपडेट हुआ (क्लाउड हेतु इंटरनेट जांचें)")
                        }
                        userToEdit = null
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("अपडेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToEdit = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Admin Master Control Center Dialog (User App Controls, Security, Vault)
    if (showSettingsDialog) {
        val currentConfig by repository.adminGlobalConfig.collectAsStateWithLifecycle(initialValue = null)
        val config = currentConfig ?: com.example.data.model.AdminGlobalConfig()

        var selectedTab by remember { mutableIntStateOf(0) }

        // User App System Controls State
        var isMaintenanceMode by remember(config) { mutableStateOf(config.isMaintenanceMode) }
        var maintenanceMessage by remember(config) { mutableStateOf(config.maintenanceMessage) }
        var allowSelfReg by remember(config) { mutableStateOf(config.allowUserSelfRegistration) }
        var requireApproval by remember(config) { mutableStateOf(config.requireAdminApproval) }
        var defaultTrialDays by remember(config) { mutableIntStateOf(config.defaultTrialDays) }
        var allowInvoiceDel by remember(config) { mutableStateOf(config.allowInvoiceDeletion) }
        var allowDiscounts by remember(config) { mutableStateOf(config.allowBillingDiscounts) }
        var strictAttendance by remember(config) { mutableStateOf(config.enforceSingleDailyAttendance) }
        var allowAdvance by remember(config) { mutableStateOf(config.allowSalaryAdvance) }
        var lowStockThreshold by remember(config) { mutableIntStateOf(config.lowStockThreshold) }
        var showBroadcast by remember(config) { mutableStateOf(config.showBroadcastAnnouncement) }
        var broadcastMsg by remember(config) { mutableStateOf(config.broadcastAnnouncement) }
        var retentionDays by remember(config) { mutableIntStateOf(config.recycleBinRetentionDays) }

        // Telegram Bot Alert State
        var tgEnabled by remember(config) { mutableStateOf(config.telegramEnabled) }
        var tgBotToken by remember(config) { mutableStateOf(config.telegramBotToken) }
        var tgChatId by remember(config) { mutableStateOf(config.telegramChatId) }
        var tgFormat by remember(config) { mutableStateOf(config.telegramFormat) }
        var tgCustomTitle by remember(config) { mutableStateOf(config.telegramCustomTitle) }
        var tgAlertNewSale by remember(config) { mutableStateOf(config.telegramAlertNewSale) }
        var tgAlertPayment by remember(config) { mutableStateOf(config.telegramAlertPayment) }
        var tgAlertLowStock by remember(config) { mutableStateOf(config.telegramAlertLowStock) }
        var tgAlertAttendance by remember(config) { mutableStateOf(config.telegramAlertAttendance) }
        var tgAlertSalaryAdvance by remember(config) { mutableStateOf(config.telegramAlertSalaryAdvance) }
        var tgAlertNewProduct by remember(config) { mutableStateOf(config.telegramAlertNewProduct) }
        var tgAlertInvoiceDeleted by remember(config) { mutableStateOf(config.telegramAlertInvoiceDeleted) }
        var tgAlertUserAction by remember(config) { mutableStateOf(config.telegramAlertUserAction) }
        var tgTestingStatus by remember { mutableStateOf<String?>(null) }
        var isTgTesting by remember { mutableStateOf(false) }

        // In-App Update & Branding State
        var brandAppNameInput by remember(config) { mutableStateOf(config.brandAppName) }
        var latestVersionCodeInput by remember(config) { mutableStateOf(config.latestAppVersionCode.toString()) }
        var latestVersionNameInput by remember(config) { mutableStateOf(config.latestAppVersionName) }
        var latestDownloadUrlInput by remember(config) { mutableStateOf(config.latestApkDownloadUrl) }
        var updateChangelogInput by remember(config) { mutableStateOf(config.updateChangelog) }
        var isForceUpdateInput by remember(config) { mutableStateOf(config.isForceUpdate) }

        // Admin Security & Support State
        var currentPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var adminPhoneInput by remember(config) { mutableStateOf(config.adminPhone) }
        var adminWhatsappInput by remember(config) { mutableStateOf(config.adminWhatsapp) }
        var adminEmailInput by remember(config) { mutableStateOf(config.adminEmail) }
        var pinError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Column {
                    Text(text = "⚙️ सुपर एडमिन नियंत्रण व सेटिंग्स", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = activeThemeColor)
                    Text(text = "User App सिस्टम नियंत्रण, टेलीग्राम अलर्ट्स, अपडेट व वॉल्ट", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 4.dp) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("📱 ऐप नियंत्रण", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("📢 टेलीग्राम", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("🚀 ऐप अपडेट", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("🔐 सुरक्षा", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            text = { Text("💾 वॉल्ट", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                // App Name Branding
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🏷️ एप्लिकेशन नाम ब्रांडिंग (App Branding Name):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    OutlinedTextField(
                                        value = brandAppNameInput,
                                        onValueChange = { brandAppNameInput = it },
                                        label = { Text("ऐप नाम (उदा. TILES AND MARBLES)") },
                                        placeholder = { Text("TILES AND MARBLES") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // USER APP SYSTEM CONTROLS
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isMaintenanceMode) Color(0xFFFEE2E2) else Color(0xFFF1F5F9),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("⚠️ मेंटेनेंस मोड (Emergency Lock)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("User App तुरंत लॉक हो जाएगा", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }
                                            Switch(
                                                checked = isMaintenanceMode,
                                                onCheckedChange = { isMaintenanceMode = it },
                                                colors = SwitchDefaults.colors(checkedThumbColor = StatusError, checkedTrackColor = Color(0xFFFECACA))
                                            )
                                        }
                                        if (isMaintenanceMode) {
                                            OutlinedTextField(
                                                value = maintenanceMessage,
                                                onValueChange = { maintenanceMessage = it },
                                                label = { Text("मेंटेनेंस संदेश (Message to Show)") },
                                                singleLine = false,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                // Announcement Banner Control
                                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF8FAFC), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("📢 लाइव प्रसारण (Broadcast Banner)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("दुकानदारों के डैशबोर्ड पर सूचना पट्टी", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }
                                            Switch(checked = showBroadcast, onCheckedChange = { showBroadcast = it })
                                        }
                                        if (showBroadcast) {
                                            OutlinedTextField(
                                                value = broadcastMsg,
                                                onValueChange = { broadcastMsg = it },
                                                label = { Text("सूचना टेक्स्ट दर्ज करें") },
                                                placeholder = { Text("उदा. नया अपडेट उपलब्ध है! या विशेष ऑफर") },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                // Registration & Approval Toggles
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("📝 नया रजिस्ट्रेशन खुला रखें", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("दुकानदार खुद रजिस्टर कर सकेंगे", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Switch(checked = allowSelfReg, onCheckedChange = { allowSelfReg = it })
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("🛡️ एडमिन स्वीकृति अनिवार्य", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("स्वीकार करने के बाद ही लॉगिन होगा", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Switch(checked = requireApproval, onCheckedChange = { requireApproval = it })
                                }

                                HorizontalDivider()

                                // Default Trial Days
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("⏱️ नए यूजर की डिफ़ॉल्ट अवधि (Default Plan):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                    ) {
                                        listOf(7 to "7 दिन", 30 to "30 दिन", 90 to "3 माह", 365 to "1 वर्ष", -1 to "आजीवन").forEach { (days, label) ->
                                            FilterChip(
                                                selected = defaultTrialDays == days,
                                                onClick = { defaultTrialDays = days },
                                                label = { Text(label, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider()

                                // Feature Controls in Billing & Staff
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🏷️ बिलिंग में डिस्काउंट छूट दें", fontSize = 13.sp)
                                    Switch(checked = allowDiscounts, onCheckedChange = { allowDiscounts = it })
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🗑️ बिल डिलीट करने की अनुमति", fontSize = 13.sp)
                                    Switch(checked = allowInvoiceDel, onCheckedChange = { allowInvoiceDel = it })
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("👷 1 दिन में 1 हाजिरी की पाबंदी", fontSize = 13.sp)
                                    Switch(checked = strictAttendance, onCheckedChange = { strictAttendance = it })
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("💵 स्टाफ एडवांस सैलरी की अनुमति", fontSize = 13.sp)
                                    Switch(checked = allowAdvance, onCheckedChange = { allowAdvance = it })
                                }

                                // Low Stock Threshold
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🚨 लो-स्टॉक चेतावनी थ्रेशोल्ड (Alert Boxes):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(5, 10, 15, 20).forEach { limit ->
                                            FilterChip(
                                                selected = lowStockThreshold == limit,
                                                onClick = { lowStockThreshold = limit },
                                                label = { Text("$limit बॉक्स", fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }

                                // Recycle Bin Days
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("♻️ रीसायकल बिन ऑटो-डिलीट दिन:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(7, 10, 15, 30).forEach { rDays ->
                                            FilterChip(
                                                selected = retentionDays == rDays,
                                                onClick = { retentionDays = rDays },
                                                label = { Text("$rDays दिन", fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }
                            }

                            1 -> {
                                // TELEGRAM BOT ALERT CONTROLS
                                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF0FDF4), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("📢 टेलीग्राम अलर्ट्स सक्रिय करें", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("सभी यूजर ऐप्स से सीधे आपके टेलीग्राम पर अलर्ट्स आएंगे", fontSize = 10.sp, color = Color(0xFF64748B))
                                            }
                                            Switch(checked = tgEnabled, onCheckedChange = { tgEnabled = it })
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = tgBotToken,
                                    onValueChange = { tgBotToken = it },
                                    label = { Text("Telegram Bot Token *") },
                                    placeholder = { Text("123456789:ABCdefGhIJKlmNoPQRstuVWXyz") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = tgChatId,
                                    onValueChange = { tgChatId = it },
                                    label = { Text("Telegram Chat ID / Channel ID *") },
                                    placeholder = { Text("उदा. 123456789 या -1001234567890") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = tgCustomTitle,
                                    onValueChange = { tgCustomTitle = it },
                                    label = { Text("अलर्ट हेडर / दुकान शीर्षक") },
                                    placeholder = { Text("TILES AND MARBLES") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Test Alert Button
                                Button(
                                    onClick = {
                                        if (tgBotToken.isBlank() || tgChatId.isBlank()) {
                                            tgTestingStatus = "❌ कृपया Bot Token और Chat ID भरें"
                                            return@Button
                                        }
                                        isTgTesting = true
                                        tgTestingStatus = null
                                        scope.launch {
                                            val res = com.example.util.TelegramNotifier.sendTestMessage(
                                                botToken = tgBotToken.trim(),
                                                chatId = tgChatId.trim(),
                                                format = tgFormat,
                                                customTitle = tgCustomTitle.ifBlank { "TILES AND MARBLES" }
                                            )
                                            isTgTesting = false
                                            tgTestingStatus = if (res.first) "✅ " + res.second else "❌ " + res.second
                                        }
                                    },
                                    enabled = !isTgTesting,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isTgTesting) "परीक्षण हो रहा है..." else "🧪 बॉट कनेक्शन टेस्ट करें (Send Test Alert)")
                                }

                                tgTestingStatus?.let { status ->
                                    Text(
                                        text = status,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (status.startsWith("✅")) StatusSuccess else StatusError
                                    )
                                }

                                HorizontalDivider()

                                Text("🔔 अलर्ट प्रकार नियंत्रण (Alert Toggles):", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("🧾 नया बिल / बिक्री अलर्ट", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                        Text("जब भी कोई नया आर्डर या बिल बने", fontSize = 10.sp, color = Color(0xFF64748B))
                                    }
                                    Switch(checked = tgAlertNewSale, onCheckedChange = { tgAlertNewSale = it })
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("💵 पेमेंट जमा / भुगतान अलर्ट", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                        Text("ग्राहकों द्वारा पेमेंट जमा होने पर", fontSize = 10.sp, color = Color(0xFF64748B))
                                    }
                                    Switch(checked = tgAlertPayment, onCheckedChange = { tgAlertPayment = it })
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("⚠️ कम स्टॉक चेतावनी", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                        Text("जब माल न्यूनतम सीमा से कम हो", fontSize = 10.sp, color = Color(0xFF64748B))
                                    }
                                    Switch(checked = tgAlertLowStock, onCheckedChange = { tgAlertLowStock = it })
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("👷 स्टाफ हाजिरी अलर्ट", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                        Text("दैनिक उपस्थिति / आधा दिन / छुट्टी", fontSize = 10.sp, color = Color(0xFF64748B))
                                    }
                                    Switch(checked = tgAlertAttendance, onCheckedChange = { tgAlertAttendance = it })
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("💰 वेतन व एडवांस भुगतान अलर्ट", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                        Text("स्टाफ को एडवांस या वेतन देने पर", fontSize = 10.sp, color = Color(0xFF64748B))
                                    }
                                    Switch(checked = tgAlertSalaryAdvance, onCheckedChange = { tgAlertSalaryAdvance = it })
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("📦 नया माल / स्टॉक जोड़ना", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                        Text("नया टाइल उत्पाद या स्टॉक बढ़ाने पर", fontSize = 10.sp, color = Color(0xFF64748B))
                                    }
                                    Switch(checked = tgAlertNewProduct, onCheckedChange = { tgAlertNewProduct = it })
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("🗑️ बिल हटाना / डिलीट अलर्ट", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                        Text("जब कोई बिल हटाया या रद्द किया जाए", fontSize = 10.sp, color = Color(0xFF64748B))
                                    }
                                    Switch(checked = tgAlertInvoiceDeleted, onCheckedChange = { tgAlertInvoiceDeleted = it })
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("🛡️ यूजर सुरक्षा व लॉगिन अलर्ट", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                        Text("नया रजिस्ट्रेशन या सुरक्षा बदलाव पर", fontSize = 10.sp, color = Color(0xFF64748B))
                                    }
                                    Switch(checked = tgAlertUserAction, onCheckedChange = { tgAlertUserAction = it })
                                }
                            }

                            2 -> {
                                // IN-APP UPDATE CENTER
                                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFEFF6FF), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("🚀 इन-ऐप अपडेट रिलीज सेंटर", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1D4ED8))
                                        Text("यहाँ नया Version Code और नया APK लिंक डालने पर सभी यूजर्स के फोन पर अपने आप अपडेट का पॉपअप आ जाएगा।", fontSize = 11.sp, color = Color(0xFF1E293B))
                                    }
                                }

                                OutlinedTextField(
                                    value = latestVersionCodeInput,
                                    onValueChange = { latestVersionCodeInput = it },
                                    label = { Text("नवीनतम Version Code * (उदा. 2)") },
                                    placeholder = { Text("2") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = latestVersionNameInput,
                                    onValueChange = { latestVersionNameInput = it },
                                    label = { Text("Version Name * (उदा. 2.0.0)") },
                                    placeholder = { Text("2.0.0") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = latestDownloadUrlInput,
                                    onValueChange = { latestDownloadUrlInput = it },
                                    label = { Text("नया APK डाउनलोड लिंक (Direct Link) *") },
                                    placeholder = { Text("https://...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = updateChangelogInput,
                                    onValueChange = { updateChangelogInput = it },
                                    label = { Text("अपडेट विवरण (Changelog)") },
                                    placeholder = { Text("नए फीचर्स, तेज स्पीड व बग फिक्स") },
                                    maxLines = 3,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("🔒 फोर्स अपडेट (Mandatory Update)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("यूजर बिना अपडेट किए आगे नहीं बढ़ सकेगा", fontSize = 10.sp, color = Color(0xFF64748B))
                                    }
                                    Switch(checked = isForceUpdateInput, onCheckedChange = { isForceUpdateInput = it })
                                }
                            }

                            3 -> {
                                // ADMIN MASTER SECURITY & SUPPORT (No 8899 shown on UI)
                                Text("🔐 मास्टर एडमिन पिन बदलें:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                OutlinedTextField(
                                    value = currentPinInput,
                                    onValueChange = { currentPinInput = it; pinError = false },
                                    label = { Text("वर्तमान मास्टर पिन *") },
                                    placeholder = { Text("वर्तमान पिन दर्ज करें") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = newPinInput,
                                    onValueChange = { newPinInput = it },
                                    label = { Text("नया मास्टर पिन दर्ज करें (4-6 अंक)") },
                                    placeholder = { Text("उदा. 5566") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (pinError) {
                                    Text("❌ गलत वर्तमान पिन! कृपया सही पिन दर्ज करें।", color = StatusError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                HorizontalDivider()

                                Text("📞 एडमिन हेल्पलाइन व सपोर्ट विवरण:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                OutlinedTextField(
                                    value = adminPhoneInput,
                                    onValueChange = { adminPhoneInput = it },
                                    label = { Text("एडमिन हेल्पलाइन फ़ोन नंबर") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = adminWhatsappInput,
                                    onValueChange = { adminWhatsappInput = it },
                                    label = { Text("एडमिन WhatsApp नंबर") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = adminEmailInput,
                                    onValueChange = { adminEmailInput = it },
                                    label = { Text("एडमिन सपोर्ट ईमेल") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            4 -> {
                                // PERMANENT VAULT & BACKUP STATUS
                                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFEFF6FF), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("🛡️ परमानेंट वॉल्ट सक्रिय (Permanent Vault Active)", fontWeight = FontWeight.Bold, color = activeThemeColor)
                                        Text("• ऐप का डेटा क्लियर करने पर भी सभी रजिस्टर्ड यूजर व रिकॉर्ड्स सुरक्षित रहते हैं।", fontSize = 11.sp, color = Color(0xFF334155))
                                        Text("• कुल रजिस्टर्ड दुकानें / यूजर: ${usersList.size}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            com.example.util.PermanentVaultHelper.saveUsersRegistry(context, usersList)
                                            val configToSave = config.copy(
                                                isMaintenanceMode = isMaintenanceMode,
                                                maintenanceMessage = maintenanceMessage,
                                                allowUserSelfRegistration = allowSelfReg,
                                                requireAdminApproval = requireApproval,
                                                defaultTrialDays = defaultTrialDays,
                                                allowInvoiceDeletion = allowInvoiceDel,
                                                allowBillingDiscounts = allowDiscounts,
                                                enforceSingleDailyAttendance = strictAttendance,
                                                allowSalaryAdvance = allowAdvance,
                                                lowStockThreshold = lowStockThreshold,
                                                broadcastAnnouncement = broadcastMsg,
                                                showBroadcastAnnouncement = showBroadcast,
                                                recycleBinRetentionDays = retentionDays,
                                                brandAppName = brandAppNameInput.trim().ifBlank { "TILES AND MARBLES" },
                                                latestAppVersionCode = latestVersionCodeInput.trim().toIntOrNull() ?: config.latestAppVersionCode,
                                                latestAppVersionName = latestVersionNameInput.trim().ifBlank { "1.0.0" },
                                                latestApkDownloadUrl = latestDownloadUrlInput.trim(),
                                                updateChangelog = updateChangelogInput.trim(),
                                                isForceUpdate = isForceUpdateInput,
                                                telegramEnabled = tgEnabled,
                                                telegramBotToken = tgBotToken.trim(),
                                                telegramChatId = tgChatId.trim(),
                                                telegramFormat = tgFormat,
                                                telegramCustomTitle = tgCustomTitle.trim().ifBlank { "TILES AND MARBLES" },
                                                telegramAlertNewSale = tgAlertNewSale,
                                                telegramAlertPayment = tgAlertPayment,
                                                telegramAlertLowStock = tgAlertLowStock,
                                                telegramAlertAttendance = tgAlertAttendance,
                                                telegramAlertSalaryAdvance = tgAlertSalaryAdvance,
                                                telegramAlertNewProduct = tgAlertNewProduct,
                                                telegramAlertInvoiceDeleted = tgAlertInvoiceDeleted,
                                                telegramAlertUserAction = tgAlertUserAction
                                            )
                                            com.example.util.PermanentVaultHelper.saveAdminConfig(context, configToSave)
                                            com.example.util.PermanentVaultHelper.publishCloudAdminConfig(configToSave)
                                            onShowMessage("⚡ सभी यूजर व सेटिंग्स वॉल्ट में सुरक्षित सिंक हो गईं!")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = activeThemeColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("⚡ तुरंत वॉल्ट सिंक करें (Force Vault Sync)")
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val file = com.example.util.BackupRestoreHelper.createBackup(context, repository.database)
                                                com.example.util.ShareHelper.shareFile(context, file, "application/json", "Tiles and Marbles Master Backup")
                                            } catch (e: Exception) {
                                                onShowMessage("बैकअप एक्सपोर्ट त्रुटि: ${e.message}")
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("📤 पूरा मास्टर डेटा बैकअप एक्सपोर्ट करें")
                                }

                                HorizontalDivider()

                                // Bulk +30 Days Bonus to All Active Users
                                Button(
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            val now = System.currentTimeMillis()
                                            var boosted = 0
                                            usersList.filter { it.isActive && it.approvalStatus == "APPROVED" && it.expiresAt != Long.MAX_VALUE && it.planDays != -1 }.forEach { u ->
                                                val baseMs = maxOf(now, u.expiresAt)
                                                repository.updateUser(u.copy(expiresAt = baseMs + (30L * 86400000L)), context)
                                                boosted++
                                            }
                                            onShowMessage("🎁 $boosted सक्रिय यूजर्स को +30 दिन बोनस वैधता दी गई!")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🎁 सभी सक्रिय यूजर्स को +30 दिन बोनस दें", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // If PIN change attempted
                        if (newPinInput.isNotBlank()) {
                            if (!LicenseManager.verifyAdminPin(context, currentPinInput)) {
                                pinError = true
                                selectedTab = 3
                                return@Button
                            }
                            LicenseManager.setAdminPin(context, newPinInput.trim())
                        }

                        if (adminPhoneInput.isNotBlank()) {
                            LicenseManager.setAdminPhone(context, adminPhoneInput.trim())
                        }

                        // Save updated AdminGlobalConfig
                        val updatedConfig = config.copy(
                            isMaintenanceMode = isMaintenanceMode,
                            maintenanceMessage = maintenanceMessage,
                            allowUserSelfRegistration = allowSelfReg,
                            requireAdminApproval = requireApproval,
                            defaultTrialDays = defaultTrialDays,
                            allowInvoiceDeletion = allowInvoiceDel,
                            allowBillingDiscounts = allowDiscounts,
                            enforceSingleDailyAttendance = strictAttendance,
                            allowSalaryAdvance = allowAdvance,
                            lowStockThreshold = lowStockThreshold,
                            broadcastAnnouncement = broadcastMsg,
                            showBroadcastAnnouncement = showBroadcast,
                            customAdminPin = if (newPinInput.isNotBlank()) newPinInput.trim() else config.customAdminPin,
                            adminPhone = adminPhoneInput.trim(),
                            adminWhatsapp = adminWhatsappInput.trim(),
                            adminEmail = adminEmailInput.trim(),
                            recycleBinRetentionDays = retentionDays,
                            brandAppName = brandAppNameInput.trim().ifBlank { "TILES AND MARBLES" },
                            latestAppVersionCode = latestVersionCodeInput.trim().toIntOrNull() ?: config.latestAppVersionCode,
                            latestAppVersionName = latestVersionNameInput.trim().ifBlank { "1.0.0" },
                            latestApkDownloadUrl = latestDownloadUrlInput.trim(),
                            updateChangelog = updateChangelogInput.trim(),
                            isForceUpdate = isForceUpdateInput,
                            telegramEnabled = tgEnabled,
                            telegramBotToken = tgBotToken.trim(),
                            telegramChatId = tgChatId.trim(),
                            telegramFormat = tgFormat,
                            telegramCustomTitle = tgCustomTitle.trim().ifBlank { "TILES AND MARBLES" },
                            telegramAlertNewSale = tgAlertNewSale,
                            telegramAlertPayment = tgAlertPayment,
                            telegramAlertLowStock = tgAlertLowStock,
                            telegramAlertAttendance = tgAlertAttendance,
                            telegramAlertSalaryAdvance = tgAlertSalaryAdvance,
                            telegramAlertNewProduct = tgAlertNewProduct,
                            telegramAlertInvoiceDeleted = tgAlertInvoiceDeleted,
                            telegramAlertUserAction = tgAlertUserAction
                        )

                        scope.launch(Dispatchers.IO) {
                            val ok = repository.saveAdminGlobalConfig(context, updatedConfig)
                            onShowMessage(if (ok) "✅ सभी एडमिन नियंत्रण लाइव क्लाउड पर सिंक हुए (User App 5 सेकंड में अपडेट होगा)!" else "⚠️ सेटिंग्स लोकल सेव हुईं, पर क्लाउड सिंक विफल (इंटरनेट जांचें)")
                        }
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeThemeColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("सुरक्षित करें (Save All)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
private fun UserAccountCard(
    user: AppUser,
    activeThemeColor: Color,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onCopyCredentials: () -> Unit,
    onEditUser: () -> Unit,
    onToggleStatus: () -> Unit,
    onManageValidity: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onQuickRenew30Days: () -> Unit,
    onSendRenewalReminder: () -> Unit,
    onDelete: () -> Unit
) {
    val now = System.currentTimeMillis()
    val isPending = user.approvalStatus == "PENDING"
    val isExpired = user.expiresAt in 1..now
    val isLifetime = user.expiresAt == Long.MAX_VALUE || user.planDays == -1
    val remainingDays = if (isLifetime) 99999 else ((user.expiresAt - now) / (24 * 60 * 60 * 1000L)).toInt()

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: User Name & Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${user.shopName} • ID: ${user.phone}",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isPending -> Color(0xFFFEF3C7)
                        !user.isActive -> Color(0xFFFEE2E2)
                        isExpired -> Color(0xFFFEF3C7)
                        else -> Color(0xFFDCFCE7)
                    }
                ) {
                    Text(
                        text = when {
                            isPending -> "⏳ पेंडिंग स्वीकृति"
                            !user.isActive -> "🚫 ब्लॉक (Blocked)"
                            isExpired -> "⚠️ समाप्त (Expired)"
                            isLifetime -> "⭐ आजीवन (Lifetime)"
                            else -> "🟢 $remainingDays दिन शेष"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isPending -> Color(0xFFB45309)
                            !user.isActive -> StatusError
                            isExpired -> Color(0xFFB45309)
                            else -> StatusSuccess
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Pending Approval Callout Box
            if (isPending) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFFBEB),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "नया दुकानदार रजिस्ट्रेशन - तुरंत स्वीकृति दें",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                        if (user.address.isNotBlank()) {
                            Text(
                                text = "📍 पता: ${user.address}",
                                fontSize = 11.sp,
                                color = Color(0xFF78350F)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onApprove,
                                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("✅ स्वीकार करें (Approve)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onReject,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(0.7f)
                            ) {
                                Text("अस्वीकार", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Credentials Box (ID & Password Display)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = "लॉगिन मोबाइल (ID): ${user.phone}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "पासवर्ड: ",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = user.password,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeThemeColor
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = onEditUser, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit User", tint = activeThemeColor, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onCopyCredentials, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = activeThemeColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Plan details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "प्लान: ${user.planName} • शुल्क: ₹${user.amountCharged.toInt()}",
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )

                val expiryDisplay = if (isLifetime) "आजीवन" else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(user.expiresAt))
                Text(
                    text = "वैधता: $expiryDisplay",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
            }

            // Dedicated Stepper & Days Management Button (+ / -) + Quick Renew / Reminder for Expiring Users
            val isExpiringOrExpired = !isLifetime && !isPending && (isExpired || remainingDays <= 7)
            if (isExpiringOrExpired) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onQuickRenew30Days,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⚡ +30 दिन रिन्यू", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    }
                    OutlinedButton(
                        onClick = onSendRenewalReminder,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Text("🔔 रिन्यूअल रिमाइंडर", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                    }
                }
            }

            Button(
                onClick = onManageValidity,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("⏱️ दिन व वैधता बदलें (+ / - Stepper & दिन लिखें)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onShareWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onEditUser,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ID/पासवर्ड बदलें", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onToggleStatus,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (user.isActive) StatusError else StatusSuccess),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (user.isActive) "ब्लॉक" else "अनब्लॉक", fontSize = 11.sp)
                }

                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = StatusError, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, shop: String, phone: String, pass: String, days: Int, planName: String, amount: Double, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("123456") }

    var selectedPlanDays by remember { mutableIntStateOf(365) }
    var selectedPlanName by remember { mutableStateOf("1 Year (1 वर्ष)") }
    var amountCharged by remember { mutableStateOf("3499") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "नया यूजर अकाउंट बनाएं", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ग्राहक का नाम (Client Name) *") },
                    placeholder = { Text("उदा. रमेश शर्मा") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("दुकान का नाम (Shop Name) *") },
                    placeholder = { Text("उदा. बालाजी टाइल्स & मार्बल") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("मोबाइल नंबर (Login User ID) *") },
                    placeholder = { Text("उदा. 9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("पासवर्ड (Login Password) *") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "सब्सक्रिप्शन प्लान:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1E3A8A))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = selectedPlanDays == 30,
                        onClick = {
                            selectedPlanDays = 30
                            selectedPlanName = "1 Month (1 माह)"
                            amountCharged = "499"
                        },
                        label = { Text("1 माह") }
                    )
                    FilterChip(
                        selected = selectedPlanDays == 90,
                        onClick = {
                            selectedPlanDays = 90
                            selectedPlanName = "3 Months (3 माह)"
                            amountCharged = "1299"
                        },
                        label = { Text("3 माह") }
                    )
                    FilterChip(
                        selected = selectedPlanDays == 365,
                        onClick = {
                            selectedPlanDays = 365
                            selectedPlanName = "1 Year (1 वर्ष)"
                            amountCharged = "3499"
                        },
                        label = { Text("1 वर्ष") }
                    )
                    FilterChip(
                        selected = selectedPlanDays == -1,
                        onClick = {
                            selectedPlanDays = -1
                            selectedPlanName = "Lifetime (आजीवन)"
                            amountCharged = "7999"
                        },
                        label = { Text("⭐ आजीवन") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountCharged,
                        onValueChange = { amountCharged = it },
                        label = { Text("शुल्क ₹ (Amount)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("शहर / नोट्स") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || shopName.isBlank() || phone.isBlank() || password.isBlank()) return@Button
                    val fee = amountCharged.toDoubleOrNull() ?: 0.0
                    onSave(name.trim(), shopName.trim(), phone.trim(), password.trim(), selectedPlanDays, selectedPlanName, fee, notes.trim())
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("सुरक्षित करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
private fun ManageUserAccessDialog(
    user: AppUser,
    onDismiss: () -> Unit,
    onSave: (newExpiresAt: Long, newPlanDays: Int, newPlanName: String) -> Unit
) {
    val now = System.currentTimeMillis()
    val isCurrentlyLifetime = user.expiresAt == Long.MAX_VALUE || user.planDays == -1
    val currentRemainingDays = if (isCurrentlyLifetime) 365 else (((user.expiresAt - now) / 86400000L).toInt()).coerceAtLeast(0)

    var customDays by remember { mutableIntStateOf(if (isCurrentlyLifetime) 365 else currentRemainingDays.coerceAtLeast(1)) }
    var inputDaysText by remember { mutableStateOf(customDays.toString()) }
    var isLifetimeSelected by remember { mutableStateOf(isCurrentlyLifetime) }

    val sdf = remember { java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault()) }
    val previewExpiryDate = remember(customDays, isLifetimeSelected) {
        if (isLifetimeSelected) {
            "🌟 आजीवन / लाइफटाइम (कोई समाप्ति नहीं)"
        } else {
            val targetMillis = now + (customDays.toLong() * 86400000L)
            sdf.format(java.util.Date(targetMillis))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(36.dp))
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("वैधता व दिन प्रबंधन (Manage Access)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${user.name} (${user.shopName})", fontSize = 12.sp, color = Color(0xFF64748B))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Current status banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "वर्तमान स्थिति: " + if (isCurrentlyLifetime) "🌟 लाइफटाइम" else "$currentRemainingDays दिन शेष",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF1E293B)
                        )
                        val currExpiry = if (isCurrentlyLifetime) "आजीवन" else sdf.format(java.util.Date(user.expiresAt))
                        Text(
                            text = "वर्तमान समाप्ति: $currExpiry",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }

                // Lifetime toggle checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isLifetimeSelected = !isLifetimeSelected }
                ) {
                    Checkbox(
                        checked = isLifetimeSelected,
                        onCheckedChange = { isLifetimeSelected = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🌟 आजीवन / लाइफटाइम एक्सेस दें (No Expiry)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFB45309)
                    )
                }

                if (!isLifetimeSelected) {
                    Text("दिन कम या ज्यादा करें (+ / - Stepper):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    // Stepper: [-1 दिन] [ Display ] [+1 दिन]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                if (customDays > 1) {
                                    customDays -= 1
                                    inputDaysText = customDays.toString()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("- 1 दिन", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.5.dp, Color(0xFF3B82F6)),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                text = "$customDays दिन",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color(0xFF1D4ED8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                customDays += 1
                                inputDaysText = customDays.toString()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ 1 दिन", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Jump buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                customDays = (customDays - 30).coerceAtLeast(1)
                                inputDaysText = customDays.toString()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                        ) {
                            Text("-30", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                customDays = (customDays - 7).coerceAtLeast(1)
                                inputDaysText = customDays.toString()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                        ) {
                            Text("-7", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                customDays += 7
                                inputDaysText = customDays.toString()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                        ) {
                            Text("+7", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                customDays += 30
                                inputDaysText = customDays.toString()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                        ) {
                            Text("+30", fontSize = 11.sp)
                        }
                    }

                    // Direct text input
                    OutlinedTextField(
                        value = inputDaysText,
                        onValueChange = { str ->
                            val clean = str.filter { it.isDigit() }
                            inputDaysText = clean
                            val num = clean.toIntOrNull()
                            if (num != null && num > 0) {
                                customDays = num
                            }
                        },
                        label = { Text("सीधे दिनों की संख्या लिखें (Exact Days)") },
                        placeholder = { Text("उदा. 45, 90, 365") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = customDays == 30,
                            onClick = { customDays = 30; inputDaysText = "30" },
                            label = { Text("30 दिन", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = customDays == 90,
                            onClick = { customDays = 90; inputDaysText = "90" },
                            label = { Text("90 दिन", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = customDays == 365,
                            onClick = { customDays = 365; inputDaysText = "365" },
                            label = { Text("365 दिन", fontSize = 10.sp) }
                        )
                    }
                }

                // New expiry preview box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("नई समाप्ति तिथि (New Expiry):", fontSize = 10.sp, color = Color(0xFF92400E))
                        Text(
                            text = previewExpiryDate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalExpiresAt = if (isLifetimeSelected) {
                        Long.MAX_VALUE
                    } else {
                        now + (customDays.toLong() * 86400000L)
                    }
                    val planName = if (isLifetimeSelected) {
                        "Lifetime (आजीवन)"
                    } else {
                        "$customDays Days ($customDays दिन)"
                    }
                    onSave(finalExpiresAt, if (isLifetimeSelected) -1 else customDays, planName)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("लागू करें व सेव करें (Save)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
