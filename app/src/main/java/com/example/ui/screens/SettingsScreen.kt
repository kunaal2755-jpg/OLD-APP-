package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.util.TelegramNotifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.SizePricing
import com.example.data.security.UserSessionManager
import com.example.ui.MainViewModel
import com.example.ui.theme.AppThemeOptions
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.getAppThemeOption
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.shopSettings.collectAsStateWithLifecycle()
    val sizePricings by viewModel.sizePricings.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()

    var currentUser by remember { mutableStateOf(UserSessionManager.getCurrentUser(context)) }

    var shopName by remember { mutableStateOf(settings.shopName) }
    var subtitle by remember { mutableStateOf(settings.subtitle) }
    var ownerName by remember { mutableStateOf(settings.ownerName) }
    var ownerPhone by remember { mutableStateOf(settings.ownerPhone) }
    var altPhone by remember { mutableStateOf(settings.altPhone) }
    var address by remember { mutableStateOf(settings.address) }
    var gstNumber by remember { mutableStateOf(settings.gstNumber) }
    var panNumber by remember { mutableStateOf(settings.panNumber) }
    var bankName by remember { mutableStateOf(settings.bankName) }
    var bankAccountNumber by remember { mutableStateOf(settings.bankAccountNumber) }
    var bankIfsc by remember { mutableStateOf(settings.bankIfsc) }
    var bankBranch by remember { mutableStateOf(settings.bankBranch) }
    var invoiceFooterNotes by remember { mutableStateOf(settings.invoiceFooterNotes) }
    var termsNotes by remember { mutableStateOf(settings.termsNotes) }
    var logoUri by remember { mutableStateOf(settings.logoUri) }
    var appTheme by remember { mutableStateOf(settings.appTheme) }
    var upiId by remember { mutableStateOf(settings.upiId) }

    // Telegram Bot Notifications State
    var telegramBotToken by remember { mutableStateOf(settings.telegramBotToken) }
    var telegramChatId by remember { mutableStateOf(settings.telegramChatId) }
    var telegramEnabled by remember { mutableStateOf(settings.telegramEnabled) }
    var telegramFormat by remember { mutableStateOf(settings.telegramFormat) }
    var telegramCustomTitle by remember { mutableStateOf(settings.telegramCustomTitle) }
    var notifyNewSale by remember { mutableStateOf(settings.notifyNewSale) }
    var notifyPaymentReceived by remember { mutableStateOf(settings.notifyPaymentReceived) }
    var notifyLowStock by remember { mutableStateOf(settings.notifyLowStock) }
    var notifyStaffAttendance by remember { mutableStateOf(settings.notifyStaffAttendance) }
    var notifyDailySummary by remember { mutableStateOf(settings.notifyDailySummary) }

    // Evening Google Drive / Email Auto-Backup State
    var autoBackupEnabled by remember { mutableStateOf(settings.autoBackupEnabled) }
    var backupTargetEmail by remember { mutableStateOf(settings.backupTargetEmail) }
    var backupScheduledTime by remember { mutableStateOf(settings.backupScheduledTime) }
    var backupFromDateString by remember { mutableStateOf(settings.backupFromDateString) }

    LaunchedEffect(settings) {
        shopName = settings.shopName
        subtitle = settings.subtitle
        ownerName = settings.ownerName
        ownerPhone = settings.ownerPhone
        altPhone = settings.altPhone
        address = settings.address
        gstNumber = settings.gstNumber
        panNumber = settings.panNumber
        bankName = settings.bankName
        bankAccountNumber = settings.bankAccountNumber
        bankIfsc = settings.bankIfsc
        bankBranch = settings.bankBranch
        invoiceFooterNotes = settings.invoiceFooterNotes
        termsNotes = settings.termsNotes
        logoUri = settings.logoUri
        appTheme = settings.appTheme
        upiId = settings.upiId
        telegramBotToken = settings.telegramBotToken
        telegramChatId = settings.telegramChatId
        telegramEnabled = settings.telegramEnabled
        telegramFormat = settings.telegramFormat
        telegramCustomTitle = settings.telegramCustomTitle
        notifyNewSale = settings.notifyNewSale
        notifyPaymentReceived = settings.notifyPaymentReceived
        notifyLowStock = settings.notifyLowStock
        notifyStaffAttendance = settings.notifyStaffAttendance
        notifyDailySummary = settings.notifyDailySummary
        autoBackupEnabled = settings.autoBackupEnabled
        backupTargetEmail = settings.backupTargetEmail
        backupScheduledTime = settings.backupScheduledTime
        backupFromDateString = settings.backupFromDateString
    }

    val activeTheme = getAppThemeOption(appTheme)

    var showAddSizePriceDialog by remember { mutableStateOf(false) }
    var editingSizePricing by remember { mutableStateOf<SizePricing?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var backupFileToRestore by remember { mutableStateOf<File?>(null) }
    var lastCreatedBackupFile by remember { mutableStateOf<File?>(null) }

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // Recycle Bin State & Cloud Sync State
    val recycleItems by viewModel.recycleItems.collectAsStateWithLifecycle()
    val recycleCount by viewModel.recycleCount.collectAsStateWithLifecycle()
    var showRecycleBinDialog by remember { mutableStateOf(false) }
    var isCloudSyncing by remember { mutableStateOf(false) }
    var isCloudRestoring by remember { mutableStateOf(false) }
    var generatedCloudCode by remember { mutableStateOf<String?>(null) }
    var isGeneratingCloudCode by remember { mutableStateOf(false) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = viewModel.persistImageLocally(context, uri)
            logoUri = localPath ?: uri.toString()
        }
    }

    val backupFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val tempFile = File(context.cacheDir, "temp_restore.json")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                backupFileToRestore = tempFile
                showRestoreConfirmDialog = true
            } catch (e: Exception) {
                viewModel.showMessage("Failed to read backup file: ${e.localizedMessage}")
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = activeTheme.primary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "दुकान व खाता सेटिंग्स (Settings)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = PureWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutConfirmDialog = true }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = activeTheme.primary)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. User Profile & Account Status Card (Strictly Client-Facing, No In-App Admin)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = activeTheme.primaryContainer),
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
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(activeTheme.accent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = activeTheme.accentText,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = currentUser?.name ?: "दुकानदार खाता",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = PureWhite
                                    )
                                    Text(
                                        text = "मोबाइल (ID): ${currentUser?.phone ?: "Not set"}",
                                        fontSize = 12.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StatusSuccess
                            ) {
                                Text(
                                    text = "🟢 सक्रिय (Active)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val expiryDate = currentUser?.expiresAt?.let {
                            if (it == Long.MAX_VALUE) "आजीवन (Lifetime)"
                            else SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "सक्रिय"

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x22000000),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "सब्सक्रिप्शन वैधता:",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                                Text(
                                    text = expiryDate,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeTheme.accent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { showChangePasswordDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = activeTheme.accent),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = activeTheme.accentText,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "पासवर्ड बदलें",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeTheme.accentText
                                )
                            }

                            OutlinedButton(
                                onClick = { showLogoutConfirmDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                                border = BorderStroke(1.dp, Color(0x55FFFFFF)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("लॉगआउट", fontSize = 12.sp, color = PureWhite)
                            }
                        }
                    }
                }
            }

            // 2. App Theme & Color Customizer
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "App Colour & Design Theme (ऐप का रंग व डिज़ाइन)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = activeTheme.primary
                        )
                        Text(
                            text = "Select theme for entire app: Top bar, buttons, emblem & PDF bills",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        AppThemeOptions.forEach { opt ->
                            val isSelected = appTheme.equals(opt.id, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) opt.primary.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) opt.primary else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { appTheme = opt.id }
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(opt.primary)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(opt.accent)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = opt.nameEn,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) opt.primary else Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = opt.nameHi,
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = opt.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Shop Profile & Full Business Details (Expanded Editing)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Shop & Business Profile (दुकान व बिलिंग विवरण)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = activeTheme.primary
                        )
                        Text(
                            text = "यहां दी गई सभी जानकारियां बिल, रसीद व कोटेशन PDF पर छपेंगी",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        // Logo Picker
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                    .clickable {
                                        logoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                if (!logoUri.isNullOrBlank()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(logoUri),
                                        contentDescription = "Shop Logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, tint = activeTheme.primary)
                                        Text("Shop Logo", fontSize = 9.sp, color = activeTheme.primary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text("दुकान का लोगो / फोटो", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("बिल व ऐप के टॉप हेडर में दिखेगा", fontSize = 11.sp, color = Color(0xFF64748B))
                                Row {
                                    TextButton(
                                        onClick = {
                                            logoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    ) {
                                        Text(if (logoUri != null) "फोटो बदलें" else "लोगो अपलोड करें", fontSize = 12.sp)
                                    }
                                    if (!logoUri.isNullOrBlank()) {
                                        TextButton(onClick = { logoUri = null }) {
                                            Text("हटाएं", fontSize = 12.sp, color = StatusError)
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = { Text("दुकान / फर्म का नाम (Shop Name) *") },
                            placeholder = { Text("TILES AND MARBLES") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("settings_shop_name_input")
                        )

                        OutlinedTextField(
                            value = subtitle,
                            onValueChange = { subtitle = it },
                            label = { Text("दुकान की टैगलाइन / लैंडमार्क (वैकल्पिक)") },
                            placeholder = { Text("खाली छोड़ें या अपनी मर्जी से लिखें") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("settings_subtitle_input")
                        )

                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("मालिक / संपर्क व्यक्ति का नाम (Owner Name)") },
                            placeholder = { Text("मालिक का नाम") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = ownerPhone,
                                onValueChange = { ownerPhone = it },
                                label = { Text("मुख्य मोबाइल नंबर *") },
                                placeholder = { Text("+91 98765 43210") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = altPhone,
                                onValueChange = { altPhone = it },
                                label = { Text("दूसरा / WhatsApp नंबर") },
                                placeholder = { Text("9823456780") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("दुकान का पूरा पता (Address) *") },
                            placeholder = { Text("दुकान का पता लिखें...") },
                            maxLines = 2,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = gstNumber,
                                onValueChange = { gstNumber = it },
                                label = { Text("GSTIN नंबर (Optional)") },
                                placeholder = { Text("07AAAAA0000A1Z5") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = panNumber,
                                onValueChange = { panNumber = it },
                                label = { Text("PAN कार्ड नंबर") },
                                placeholder = { Text("ABCDE1234F") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it },
                            label = { Text("UPI ID / QR Pay (बिल पर QR भुगतान के लिए)") },
                            placeholder = { Text("9876543210@paytm या shop@upi") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Bank Details Section
                        Text(
                            text = "बैंक खाता विवरण (Bank Details for Invoices)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = activeTheme.primary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = bankName,
                                onValueChange = { bankName = it },
                                label = { Text("बैंक का नाम") },
                                placeholder = { Text("State Bank of India") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = bankAccountNumber,
                                onValueChange = { bankAccountNumber = it },
                                label = { Text("खाता संख्या (A/C No.)") },
                                placeholder = { Text("123456789012") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = bankIfsc,
                                onValueChange = { bankIfsc = it },
                                label = { Text("IFSC कोड") },
                                placeholder = { Text("SBIN0001234") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = bankBranch,
                                onValueChange = { bankBranch = it },
                                label = { Text("शाखा (Branch)") },
                                placeholder = { Text("Main Road Branch") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = invoiceFooterNotes,
                            onValueChange = { invoiceFooterNotes = it },
                            label = { Text("बिल फुटर संदेश (Invoice Footer Note)") },
                            placeholder = { Text("Thank you for your business! Visit again.") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = termsNotes,
                            onValueChange = { termsNotes = it },
                            label = { Text("शर्तें व नियम (Terms & Conditions printed on bills)") },
                            maxLines = 4,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                viewModel.saveSettings(
                                    shopName = shopName,
                                    subtitle = subtitle,
                                    ownerName = ownerName,
                                    ownerPhone = ownerPhone,
                                    altPhone = altPhone,
                                    address = address,
                                    logoUri = logoUri,
                                    gstNumber = gstNumber,
                                    panNumber = panNumber,
                                    bankName = bankName,
                                    bankAccountNumber = bankAccountNumber,
                                    bankIfsc = bankIfsc,
                                    bankBranch = bankBranch,
                                    invoiceFooterNotes = invoiceFooterNotes,
                                    termsNotes = termsNotes,
                                    appTheme = appTheme,
                                    upiId = upiId,
                                    telegramBotToken = telegramBotToken,
                                    telegramChatId = telegramChatId,
                                    telegramEnabled = telegramEnabled,
                                    telegramFormat = telegramFormat,
                                    telegramCustomTitle = telegramCustomTitle,
                                    notifyNewSale = notifyNewSale,
                                    notifyPaymentReceived = notifyPaymentReceived,
                                    notifyLowStock = notifyLowStock,
                                    notifyStaffAttendance = notifyStaffAttendance,
                                    notifyDailySummary = notifyDailySummary,
                                    autoBackupEnabled = autoBackupEnabled,
                                    backupTargetEmail = backupTargetEmail,
                                    backupScheduledTime = backupScheduledTime,
                                    backupFromDateString = backupFromDateString
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = PureWhite)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("दुकान की सभी सेटिंग्स सुरक्षित करें (Save Profile)", color = PureWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3.5. Smart Telegram Bot Alerts Integration Section
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("telegram_bot_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "टेलीग्राम बॉट अलर्ट्स (Telegram Bot)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = activeTheme.primary
                                    )
                                    Text(
                                        text = "दुकान के लाइव अलर्ट्स सीधे Telegram पर प्राप्त करें",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            Switch(
                                checked = telegramEnabled,
                                onCheckedChange = { telegramEnabled = it }
                            )
                        }

                        if (telegramEnabled) {
                            HorizontalDivider(color = Color(0xFFE2E8F0))

                            // Admin Master Bot Active Notice (User doesn't need to create/enter bot token)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "🤖 सेंट्रल टेलीग्राम बॉट सक्रिय (Admin Master Bot)",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF166534),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "मास्टर बॉट एडमिन ऐप से स्वतः जुड़ा हुआ है। आपको बॉट टोकन बनाने की आवश्यकता नहीं है, केवल नीचे अपनी व्यक्तिगत Chat ID दर्ज करें।",
                                            fontSize = 10.sp,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            }

                            // Personal Telegram Chat ID field
                            OutlinedTextField(
                                value = telegramChatId,
                                onValueChange = { telegramChatId = it },
                                label = { Text("आपका व्यक्तिगत Telegram Chat ID *") },
                                placeholder = { Text("उदा. 1234567890 या ग्रुप ID") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                supportingText = {
                                    Text("Telegram पर @userinfobot या @getmyid_bot से अपना Chat ID पता करें। आपके बिल व अलर्ट आपके निजी टेलीग्राम पर आएंगे!", fontSize = 10.sp, color = Color(0xFF64748B))
                                }
                            )

                            // Custom Title
                            OutlinedTextField(
                                value = telegramCustomTitle,
                                onValueChange = { telegramCustomTitle = it },
                                label = { Text("अलर्ट में दुकान का नाम / हेडर (Header Title)") },
                                placeholder = { Text("उदा. TILES AND MARBLES") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Format Choice
                            Text(
                                text = "संदेश प्रारूप चुनें (Message Format / Language):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = activeTheme.primary
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilterChip(
                                    selected = telegramFormat == "DETAILED_HINDI",
                                    onClick = { telegramFormat = "DETAILED_HINDI" },
                                    label = { Text("📋 विस्तृत हिंदी", fontSize = 10.5.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = telegramFormat == "COMPACT_HINDI",
                                    onClick = { telegramFormat = "COMPACT_HINDI" },
                                    label = { Text("⚡ संक्षिप्त", fontSize = 10.5.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = telegramFormat == "ENGLISH_PRO",
                                    onClick = { telegramFormat = "ENGLISH_PRO" },
                                    label = { Text("💼 English", fontSize = 10.5.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Live Format Preview
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "👁️ संदेश कैसा दिखेगा (Format Preview & Correction):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeTheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = TelegramNotifier.getSamplePreview(telegramFormat, telegramCustomTitle),
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp,
                                        color = Color(0xFF334155),
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }

                            // Alert ON/OFF Toggles
                            Text(
                                text = "कौन-कौन सी सूचनाएं बॉट में भेजनी हैं (ON/OFF Toggles):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = activeTheme.primary
                            )

                            NotificationToggleRow(
                                title = "🧾 नया बिल / आर्डर बनने पर (New Sales Alert)",
                                isChecked = notifyNewSale,
                                onCheckedChange = { notifyNewSale = it }
                            )
                            NotificationToggleRow(
                                title = "💵 भुगतान / पेमेंट जमा होने पर (Payment Alert)",
                                isChecked = notifyPaymentReceived,
                                onCheckedChange = { notifyPaymentReceived = it }
                            )
                            NotificationToggleRow(
                                title = "🚨 स्टॉक कम होने पर चेतावनी (Low Stock Alert <= 10 Bx)",
                                isChecked = notifyLowStock,
                                onCheckedChange = { notifyLowStock = it }
                            )
                            NotificationToggleRow(
                                title = "👷 स्टाफ हाजिरी दर्ज होने पर (Staff Attendance)",
                                isChecked = notifyStaffAttendance,
                                onCheckedChange = { notifyStaffAttendance = it }
                            )

                            // Test Connection Button
                            var isTesting by remember { mutableStateOf(false) }
                            var testResultMsg by remember { mutableStateOf<String?>(null) }
                            var testResultSuccess by remember { mutableStateOf(false) }

                            OutlinedButton(
                                onClick = {
                                    isTesting = true
                                    testResultMsg = null
                                    viewModel.testTelegramBot(
                                        botToken = telegramBotToken,
                                        chatId = telegramChatId,
                                        format = telegramFormat,
                                        customTitle = telegramCustomTitle
                                    ) { success, msg ->
                                        isTesting = false
                                        testResultSuccess = success
                                        testResultMsg = msg
                                    }
                                },
                                    enabled = !isTesting && telegramChatId.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("test_telegram_button")
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("जांच हो रही है...")
                                } else {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("🔔 टेस्ट संदेश भेजें (Test Telegram Bot)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            testResultMsg?.let { msg ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (testResultSuccess) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, if (testResultSuccess) StatusSuccess else StatusError),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = msg,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (testResultSuccess) StatusSuccess else StatusError,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Smart Size-Based Default Pricing Section
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Smart Size-Based Default Pricing & Packing",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Navy900
                                )
                                Text(
                                    text = "प्रत्येक साइज़ का डिफ़ॉल्ट बिक्री रेट व बॉक्स पैकिंग (Pieces/Box)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Button(
                                onClick = { showAddSizePriceDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (sizePricings.isEmpty()) {
                            Text("No size default rates configured.", fontSize = 12.sp, color = Color(0xFF64748B))
                        } else {
                            sizePricings.forEach { sp ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = sp.sizeName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val pcsText = if (sp.defaultPiecesPerBox > 0) " • ${sp.defaultPiecesPerBox} Pcs/Bx" else ""
                                        Text(
                                            text = "₹${sp.defaultPricePerBox.toInt()} / Box$pcsText",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = Navy900
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { editingSizePricing = sp },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Navy900, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(2.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteSizePricing(sp.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = StatusError, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4.5. PDF Tile Size Display Customizer (2X4, 12X18, etc.)
            item {
                val allKnownSizes = remember(sizePricings, products) {
                    (sizePricings.map { it.sizeName } + products.map { it.size }).distinct()
                }
                com.example.ui.components.PdfSizeCustomizationSection(
                    configJson = settings.pdfSizeDisplayConfigJson,
                    availableSizes = allKnownSizes,
                    primaryColor = activeTheme.primary,
                    accentColor = activeTheme.accent,
                    onSaveConfigJson = { newJson ->
                        viewModel.savePdfSizeDisplayConfig(newJson)
                    }
                )
            }

            // 5. Recycle Bin & Trash (10-Day Auto Purge & 1-Click Restore)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "रीसायकल बिन व बैकअप ट्रैश (Recycle Bin)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = activeTheme.primary
                                )
                                Text(
                                    text = "डिलीट हुआ डेटा 10 दिन तक सुरक्षित रहता है और बटन दबाने पर अपनी जगह वापस चला जाता है",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (recycleCount > 0) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = "$recycleCount आइटम",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (recycleCount > 0) Color(0xFFB45309) else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { showRecycleBinDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = if (recycleCount > 0) activeTheme.primary else Color(0xFF475569)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🗑️ रीसायकल बिन खोलें व डेटा वापस लाएं (${recycleCount})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 6. Cloud Backup & Multi-device Sync (Phone switch sync)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = activeTheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "क्लाउड बैकअप व मल्टी-मोबाइल सिंक",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = activeTheme.primary
                                )
                                Text(
                                    text = "मोबाइल खोने या दूसरा फ़ोन लेने पर ID व पासवर्ड डालने से सारा डेटा स्वतः लोड हो जाएगा",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "क्लाउड बैकअप सक्रिय: हर डिवाइस में सुरक्षित",
                                    fontSize = 11.sp,
                                    color = Color(0xFF166534),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    isCloudSyncing = true
                                    viewModel.syncCloudBackup(
                                        onDone = { isCloudSyncing = false },
                                        onError = { isCloudSyncing = false; viewModel.showMessage(it) }
                                    )
                                },
                                enabled = !isCloudSyncing,
                                colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isCloudSyncing) "सेव हो रहा है..." else "वॉल्ट में सुरक्षित करें", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    isCloudRestoring = true
                                    viewModel.restoreCloudBackup(
                                        onDone = { isCloudRestoring = false },
                                        onError = { isCloudRestoring = false; viewModel.showMessage(it) }
                                    )
                                },
                                enabled = !isCloudRestoring,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isCloudRestoring) "आ रहा है..." else "वॉल्ट से लाएं", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Multi-device Cloud Sync Code Button
                        OutlinedButton(
                            onClick = {
                                isGeneratingCloudCode = true
                                viewModel.uploadToCloudSync(
                                    onSuccess = { code ->
                                        isGeneratingCloudCode = false
                                        generatedCloudCode = code
                                    },
                                    onError = { err ->
                                        isGeneratingCloudCode = false
                                        viewModel.showMessage("त्रुटि: $err")
                                    }
                                )
                            },
                            enabled = !isGeneratingCloudCode,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = activeTheme.primary),
                            border = BorderStroke(1.dp, activeTheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = activeTheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isGeneratingCloudCode) "क्लाउड कोड बन रहा है..." else "🔑 दूसरे फोन के लिए क्लाउड सिंक कोड बनाएं",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeTheme.primary
                            )
                        }
                    }
                }
            }

            // 7. Backup & Restore Section
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "ऑफ़लाइन बैकअप व रीस्टोर (Offline Backup & Restore)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Navy900
                        )
                        Text(
                            text = "पूरे व्यापार का डेटा (टाइल फोटो, दुकान लोगो, स्टॉक, बिल, ग्राहक, लेजर व PDF सेटिंग्स) सुरक्षित फ़ाइल में सेव व रीस्टोर करें",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    viewModel.createBackup(
                                        context = context,
                                        onDone = { file ->
                                            lastCreatedBackupFile = file
                                            val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "application/json"
                                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                putExtra(android.content.Intent.EXTRA_SUBJECT, file.name)
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            val chooser = android.content.Intent.createChooser(sendIntent, "Save or Share Backup File")
                                            context.startActivity(chooser)
                                        },
                                        onError = { viewModel.showMessage(it) }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("बैकअप बनाएं", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    backupFilePickerLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("रीस्टोर करें", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 7.5 Scheduled Cumulative Evening Auto-Backup to Drive / Email
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = activeTheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "📦 शाम का संचयी ऑटो-बैकअप (Drive / Email)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = activeTheme.primary
                                    )
                                    Text(
                                        text = "नियत समय पर स्वतः ड्राइव/ईमेल पर संचयी बैकअप सुरक्षित होगा",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            Switch(
                                checked = autoBackupEnabled,
                                onCheckedChange = { autoBackupEnabled = it }
                            )
                        }

                        if (autoBackupEnabled) {
                            HorizontalDivider(color = Color(0xFFE2E8F0))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "फ़ाइल का नाम: dd_MM_yyyy_backup_data.json (उदा. 01_11_2026_backup_data.json)। इसमें सभी टाइल फोटो, स्टॉक, बिल और सेटिंग्स संचयी रूप से सुरक्षित रहते हैं।",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF1E40AF)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = backupScheduledTime,
                                    onValueChange = { backupScheduledTime = it },
                                    label = { Text("बैकअप का समय (24H)") },
                                    placeholder = { Text("20:00 (शाम 8 बजे)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    supportingText = { Text("उदा. 20:00 या 21:30", fontSize = 10.sp) }
                                )

                                OutlinedTextField(
                                    value = backupFromDateString,
                                    onValueChange = { backupFromDateString = it },
                                    label = { Text("डेटा प्रारंभिक तारीख") },
                                    placeholder = { Text("dd/MM/yyyy उदा. 01/10/2026") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.3f),
                                    supportingText = { Text("इस तारीख से आगे का संचयी डेटा", fontSize = 10.sp) }
                                )
                            }

                            OutlinedTextField(
                                value = backupTargetEmail,
                                onValueChange = { backupTargetEmail = it },
                                label = { Text("Google Drive / ईमेल ID (Target Email)") },
                                placeholder = { Text("उदा. yourname@gmail.com") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                supportingText = { Text("बैकअप इस ईमेल / ड्राइव खाते पर भेजा जाएगा", fontSize = 10.sp) }
                            )

                            Button(
                                onClick = {
                                    viewModel.triggerCumulativeBackupAndShare(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📤 ड्राइव / ईमेल पर अभी बैकअप भेजें (Share Now)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Size Price Dialog
    if (showAddSizePriceDialog) {
        var newSizeName by remember { mutableStateOf("") }
        var newPriceInput by remember { mutableStateOf("") }
        var newPiecesInput by remember { mutableStateOf("") }
        var updateExistingTiles by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddSizePriceDialog = false },
            title = { Text("Add Size Pricing & Packing") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newSizeName,
                        onValueChange = { newSizeName = it },
                        label = { Text("Size (e.g. 12x24, 8x12, 32x64)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPriceInput,
                        onValueChange = { newPriceInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Default Price Per Box (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPiecesInput,
                        onValueChange = { newPiecesInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Default Pieces Per Box (एक बॉक्स में कितने पीस)") },
                        placeholder = { Text("e.g. 5, 6, 2, 4") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { updateExistingTiles = !updateExistingTiles }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = updateExistingTiles,
                            onCheckedChange = { updateExistingTiles = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "इस साइज़ की मौजूदा टाइलों का रेट व पैकिंग भी अपडेट करें",
                            fontSize = 11.5.sp,
                            color = Navy900
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = newPriceInput.toDoubleOrNull() ?: 0.0
                        val pcs = newPiecesInput.toIntOrNull() ?: 0
                        if (newSizeName.isNotBlank() && price > 0.0) {
                            viewModel.updateSizePricingAndProducts(newSizeName.trim(), price, pcs, updateExistingTiles)
                            showAddSizePriceDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSizePriceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Size Price Dialog (User can edit size pricing and update all tiles of this size)
    if (editingSizePricing != null) {
        val sp = editingSizePricing!!
        var editPriceInput by remember(sp.id) { mutableStateOf(sp.defaultPricePerBox.toInt().toString()) }
        var editPiecesInput by remember(sp.id) { mutableStateOf(if (sp.defaultPiecesPerBox > 0) sp.defaultPiecesPerBox.toString() else "") }
        var updateExistingTiles by remember(sp.id) { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { editingSizePricing = null },
            title = { Text("Edit Size Pricing & Packing") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("साइज़: ${sp.sizeName}", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 14.sp)

                    OutlinedTextField(
                        value = editPriceInput,
                        onValueChange = { editPriceInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Default Price Per Box (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPiecesInput,
                        onValueChange = { editPiecesInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Default Pieces Per Box (एक बॉक्स में कितने पीस)") },
                        placeholder = { Text("e.g. 5, 6, 2, 4") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { updateExistingTiles = !updateExistingTiles }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = updateExistingTiles,
                            onCheckedChange = { updateExistingTiles = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "इस साइज़ (${sp.sizeName}) की सभी मौजूदा इन्वेंटरी टाइलों का रेट व पैकिंग भी अपडेट करें",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Navy900
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = editPriceInput.toDoubleOrNull() ?: 0.0
                        val pcs = editPiecesInput.toIntOrNull() ?: 0
                        if (price > 0.0) {
                            viewModel.updateSizePricingAndProducts(sp.sizeName, price, pcs, updateExistingTiles)
                            editingSizePricing = null
                        }
                    }
                ) {
                    Text("अपडेट करें (Update)")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSizePricing = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmDialog && backupFileToRestore != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            icon = {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusError, modifier = Modifier.size(36.dp))
            },
            title = { Text("Confirm Database Restore") },
            text = {
                Text(
                    text = "Restoring this backup file (${backupFileToRestore?.name}) will replace all current inventory, products, orders, and customer records with the backup file data. Are you sure you wish to continue?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(
                            context = context,
                            backupFile = backupFileToRestore!!,
                            onDone = {
                                showRestoreConfirmDialog = false
                            },
                            onError = { err ->
                                showRestoreConfirmDialog = false
                                viewModel.showMessage("Restore failed: $err")
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Proceed & Restore", color = PureWhite)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // User Change Password Dialog
    if (showChangePasswordDialog) {
        var oldPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var changeError by remember { mutableStateOf<String?>(null) }
        var isChanging by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            icon = {
                Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = activeTheme.primary, modifier = Modifier.size(36.dp))
            },
            title = {
                Text(text = "पासवर्ड बदलें (Change Password)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "अपना वर्तमान पासवर्ड और नया पासवर्ड दर्ज करें:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = { oldPass = it; changeError = null },
                        label = { Text("वर्तमान पासवर्ड (Old Password) *") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it; changeError = null },
                        label = { Text("नया पासवर्ड (New Password) *") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it; changeError = null },
                        label = { Text("नया पासवर्ड दोबारा दर्ज करें *") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (changeError != null) {
                        Text(
                            text = changeError!!,
                            color = StatusError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (oldPass.isBlank() || newPass.isBlank()) {
                            changeError = "कृपया सभी फ़ील्ड भरें!"
                            return@Button
                        }
                        if (newPass.length < 4) {
                            changeError = "नया पासवर्ड कम से कम 4 अक्षरों का होना चाहिए!"
                            return@Button
                        }
                        if (newPass != confirmPass) {
                            changeError = "नया पासवर्ड और कन्फर्म पासवर्ड मेल नहीं खा रहे!"
                            return@Button
                        }

                        isChanging = true
                        viewModel.changeCurrentUserPassword(
                            oldPass = oldPass,
                            newPass = newPass,
                            onSuccess = {
                                isChanging = false
                                showChangePasswordDialog = false
                                currentUser = UserSessionManager.getCurrentUser(context)
                            },
                            onError = { err ->
                                isChanging = false
                                changeError = err
                            }
                        )
                    },
                    enabled = !isChanging,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isChanging) "बदल रहा है..." else "पासवर्ड अपडेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            icon = {
                Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = StatusError, modifier = Modifier.size(36.dp))
            },
            title = {
                Text(text = "लॉगआउट कन्फर्म करें", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    text = "क्या आप निश्चित रूप से ऐप से लॉगआउट करना चाहते हैं? दोबारा ऐप खोलने के लिए आपको अपना मोबाइल नंबर और पासवर्ड दर्ज करना होगा।",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logoutClientUser()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("हां, लॉगआउट करें", color = PureWhite)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Recycle Bin Dialog
    if (showRecycleBinDialog) {
        AlertDialog(
            onDismissRequest = { showRecycleBinDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = activeTheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("रीसायकल बिन (${recycleItems.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    Text(
                        text = "यहाँ डिलीट किया गया डेटा 10 दिन तक सुरक्षित रहता है। 'रीस्टोर करें' दबाने पर यह वापस अपनी जगह चला जाएगा। 10 दिन बाद स्वतः हट जाएगा।",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (recycleItems.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("रीसायकल बिन खाली है", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                Text("कोई भी डिलीट किया हुआ डेटा नहीं है", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(recycleItems, key = { it.id }) { item ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = when (item.itemType) {
                                                    "ORDER" -> Color(0xFFDBEAFE)
                                                    "PRODUCT" -> Color(0xFFDCFCE7)
                                                    "CUSTOMER" -> Color(0xFFFEF3C7)
                                                    else -> Color(0xFFF3E8FF)
                                                }
                                            ) {
                                                Text(
                                                    text = when (item.itemType) {
                                                        "ORDER" -> "बिल / ऑर्डर"
                                                        "PRODUCT" -> "टाइल प्रोडक्ट"
                                                        "CUSTOMER" -> "ग्राहक"
                                                        "STAFF" -> "कर्मचारी"
                                                        else -> item.itemType
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (item.itemType) {
                                                        "ORDER" -> Color(0xFF1D4ED8)
                                                        "PRODUCT" -> Color(0xFF15803D)
                                                        "CUSTOMER" -> Color(0xFFB45309)
                                                        else -> Color(0xFF7E22CE)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "${item.daysRemaining} दिन शेष",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309)
                                                )
                                            }
                                        }

                                        Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                                        Text(text = item.subtitle, fontSize = 11.sp, color = Color(0xFF64748B))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Button(
                                                onClick = { viewModel.restoreRecycleItem(item.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("🔄 अपनी जगह रीस्टोर करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = { viewModel.deleteRecycleItemPermanently(item.id) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("हमेशा हटाएं", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (recycleItems.isNotEmpty()) {
                    Button(
                        onClick = { viewModel.restoreAllRecycleItems() },
                        colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary)
                    ) {
                        Text("सब कुछ रीस्टोर करें (${recycleItems.size})")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecycleBinDialog = false }) {
                    Text("बंद करें")
                }
            }
        )
    }

    if (generatedCloudCode != null) {
        val code = generatedCloudCode!!
        AlertDialog(
            onDismissRequest = { generatedCloudCode = null },
            title = {
                Text(
                    text = "🔑 आपका क्लाउड सिंक कोड",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = activeTheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "इस कोड को नोट कर लें या कॉपी करें। किसी भी दूसरे फोन में ऐप खोलकर लॉगिन स्क्रीन पर 'Restore from Cloud Code' में यह कोड डालें:",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, activeTheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = code,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = activeTheme.primary
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Cloud Sync Code", code))
                                    viewModel.showMessage("📋 कोड कॉपी हो गया!")
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Key, contentDescription = "Copy Code", tint = activeTheme.primary)
                            }
                        }
                    }

                    Text(
                        text = "⏰ यह कोड 365 दिनों तक वैध रहेगा और आपके पूरे व्यापार डेटा (स्टाफ, बिल, ग्राहक) को नए फोन में ट्रांसफर कर देगा।",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { generatedCloudCode = null },
                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary)
                ) {
                    Text("ठीक है (Done)")
                }
            }
        )
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onCheckedChange(!isChecked) }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.5.sp,
            color = Color(0xFF334155),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}
