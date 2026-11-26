package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppUser
import com.example.data.security.LicenseManager
import com.example.ui.MainViewModel
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.getAppThemeOption
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserLoginScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.shopSettings.collectAsStateWithLifecycle()
    val activeTheme = getAppThemeOption(settings.appTheme)

    val adminPhone = remember { LicenseManager.getAdminPhone(context) }
    val usersList by viewModel.repository.allUsers.collectAsStateWithLifecycle(initialValue = emptyList())
    val registeredUser = usersList.firstOrNull { it.role == "CLIENT" } ?: usersList.firstOrNull()

    var phoneInput by remember { mutableStateOf(registeredUser?.phone ?: "") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    // Registration Dialog States
    var showRegisterDialog by remember { mutableStateOf(false) }
    var regName by remember { mutableStateOf("") }
    var regShopName by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regAddress by remember { mutableStateOf("") }
    var regError by remember { mutableStateOf<String?>(null) }
    var isRegistering by remember { mutableStateOf(false) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var newlyRegisteredUser by remember { mutableStateOf<AppUser?>(null) }

    var showCloudRestoreDialog by remember { mutableStateOf(false) }
    var cloudCodeInput by remember { mutableStateOf("") }
    var cloudRestoreError by remember { mutableStateOf<String?>(null) }
    var isRestoringCloud by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.example.util.PermanentVaultHelper.autoRestoreOnLaunch(context, viewModel.repository.database)
        }
    }

    LaunchedEffect(registeredUser?.phone) {
        if (!registeredUser?.phone.isNullOrBlank() && phoneInput.isBlank()) {
            phoneInput = registeredUser!!.phone
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = activeTheme.primary,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (settings.shopName.isNotBlank()) settings.shopName else "TILES AND MARBLES",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = PureWhite
                        )
                        Text(
                            text = "दुकानदार लॉगिन पोर्टल (User Login)",
                            fontSize = 11.sp,
                            color = Color(0xFF93C5FD)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = activeTheme.primary)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Branding Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = activeTheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(activeTheme.accent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = activeTheme.accentText,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (settings.shopName.isNotBlank()) settings.shopName else "TILES AND MARBLES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PureWhite,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "टाइल्स बिलिंग, स्टॉक व कर्मचारी वेतन रजिस्टर",
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Login Input Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "कृपया अपना लॉगिन विवरण दर्ज करें:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = activeTheme.primary
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = {
                            if (it.length <= 15) {
                                phoneInput = it.filter { char -> char.isDigit() || char == '+' }
                                errorMessage = null
                            }
                        },
                        label = { Text("मोबाइल नंबर (User ID) *") },
                        placeholder = { Text("उदा. 9876543210") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = activeTheme.primary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_login_phone")
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            errorMessage = null
                        },
                        label = { Text("पासवर्ड (Password) *") },
                        placeholder = { Text("अपना पासवर्ड दर्ज करें") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = activeTheme.primary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide Password" else "Show Password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_login_password")
                    )

                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = StatusError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val cleanPhone = phoneInput.trim()
                            val cleanPass = passwordInput.trim()
                            if (cleanPhone.isBlank()) {
                                errorMessage = "कृपया अपना रजिस्टर्ड मोबाइल नंबर दर्ज करें!"
                                return@Button
                            }
                            if (cleanPass.isBlank()) {
                                errorMessage = "कृपया अपना पासवर्ड दर्ज करें!"
                                return@Button
                            }

                            isLoggingIn = true
                            viewModel.loginClientUser(
                                phone = cleanPhone,
                                password = cleanPass,
                                onSuccess = {
                                    isLoggingIn = false
                                },
                                onError = { err ->
                                    isLoggingIn = false
                                    errorMessage = err
                                }
                            )
                        },
                        enabled = !isLoggingIn,
                        colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_user_login")
                    ) {
                        Icon(imageVector = Icons.Default.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLoggingIn) "सत्यापित हो रहा है..." else "लॉगिन करें (Sign In)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Register New Account Button
                    OutlinedButton(
                        onClick = {
                            regError = null
                            showRegisterDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_open_register_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.HowToReg, contentDescription = null, tint = activeTheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "📝 नया खाता बनाएं (Register New Account)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.primary
                        )
                    }

                    // Restore From Cloud Code (New Mobile Phone Transfer)
                    TextButton(
                        onClick = {
                            cloudRestoreError = null
                            showCloudRestoreDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_open_cloud_restore")
                    ) {
                        Text(
                            text = "☁️ नए फोन में पुराना डेटा लाएं (Cloud Restore Code)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = activeTheme.primary
                        )
                    }
                }
            }

            // Contact Admin for Account / Password Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "सहायता चाहिए या पासवर्ड रीसेट कराना है?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = activeTheme.primary
                    )
                    Text(
                        text = "सॉफ्टवेयर एडमिन से तुरंत सहायता या पासवर्ड रीसेट करवाएं:",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${adminPhone.replace(" ", "")}"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    viewModel.showMessage("कॉल नहीं लग सका")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("कॉल करें", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val cleanAdmin = adminPhone.replace("[^0-9]".toRegex(), "")
                                val currentPhone = phoneInput.ifBlank { "मेरा नंबर" }
                                val msg = "नमस्ते एडमिन जी! मुझे Tiles and Marbles ERP ऐप के लिए लॉगिन आईडी और पासवर्ड चाहिए।\nमोबाइल नंबर: $currentPhone"
                                try {
                                    val url = "https://api.whatsapp.com/send?phone=$cleanAdmin&text=${URLEncoder.encode(msg, "UTF-8")}"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (e: Exception) {
                                    viewModel.showMessage("WhatsApp नहीं खुला")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text("💬 WhatsApp सहायता", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Demo Hint
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val activeId = registeredUser?.phone ?: "9876543210"
                    val activePass = registeredUser?.password ?: "123456"
                    Text(
                        text = "🔑 वर्तमान रजिस्टर्ड लॉगिन: मोबाइल: $activeId • पासवर्ड: $activePass",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeTheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "(एडमिन ऐप से बदलने पर यह आईडी व पासवर्ड यहां स्वतः अपडेट हो जाता है)",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }

    // Register New Account Dialog
    if (showRegisterDialog) {
        AlertDialog(
            onDismissRequest = { if (!isRegistering) showRegisterDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.HowToReg, contentDescription = null, tint = activeTheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("नया खाता रजिस्टर करें", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "रजिस्टर करने के बाद आपका विवरण एडमिन ऐप में स्वीकृति (Approval) के लिए जाएगा। एडमिन द्वारा स्वीकार किए जाने के बाद आप लॉगिन कर सकेंगे।",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = regName,
                        onValueChange = { regName = it; regError = null },
                        label = { Text("दुकानदार / मालिक का नाम *") },
                        placeholder = { Text("उदा. राजेश गुप्ता") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regShopName,
                        onValueChange = { regShopName = it; regError = null },
                        label = { Text("दुकान / फर्म का नाम *") },
                        placeholder = { Text("उदा. श्री बालाजी मार्बल्स") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Store, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regPhone,
                        onValueChange = {
                            if (it.length <= 15) {
                                regPhone = it.filter { char -> char.isDigit() || char == '+' }
                                regError = null
                            }
                        },
                        label = { Text("मोबाइल नंबर (लॉगिन ID) *") },
                        placeholder = { Text("उदा. 9876543210") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it; regError = null },
                        label = { Text("पासवर्ड सेट करें *") },
                        placeholder = { Text("उदा. 123456") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regAddress,
                        onValueChange = { regAddress = it },
                        label = { Text("दुकान का पता व शहर") },
                        placeholder = { Text("उदा. रिंग रोड, जयपुर") },
                        leadingIcon = { Icon(imageVector = Icons.Default.LocationOn, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (regError != null) {
                        Text(
                            text = regError!!,
                            color = StatusError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = regName.trim()
                        val cleanShop = regShopName.trim()
                        val cleanP = regPhone.trim()
                        val cleanPass = regPassword.trim()
                        val cleanAddr = regAddress.trim()

                        if (cleanName.isBlank()) {
                            regError = "कृपया अपना नाम दर्ज करें!"
                            return@Button
                        }
                        if (cleanShop.isBlank()) {
                            regError = "कृपया दुकान का नाम दर्ज करें!"
                            return@Button
                        }
                        if (cleanP.length < 10) {
                            regError = "कृपया 10 अंकों का वैध मोबाइल नंबर दर्ज करें!"
                            return@Button
                        }
                        if (cleanPass.length < 4) {
                            regError = "पासवर्ड कम से कम 4 अक्षरों का होना चाहिए!"
                            return@Button
                        }

                        isRegistering = true
                        viewModel.registerClientUser(
                            name = cleanName,
                            shopName = cleanShop,
                            phone = cleanP,
                            password = cleanPass,
                            address = cleanAddr,
                            onSuccess = { user ->
                                isRegistering = false
                                showRegisterDialog = false
                                newlyRegisteredUser = user
                                showSuccessDialog = true
                                phoneInput = user.phone
                                passwordInput = user.password
                            },
                            onError = { err ->
                                isRegistering = false
                                regError = err
                            }
                        )
                    },
                    enabled = !isRegistering,
                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary)
                ) {
                    Text(if (isRegistering) "भेजा जा रहा है..." else "रजिस्टर करें")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRegisterDialog = false },
                    enabled = !isRegistering
                ) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Success Dialog after Registration
    if (showSuccessDialog && newlyRegisteredUser != null) {
        val u = newlyRegisteredUser!!
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = activeTheme.accent,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = "रजिस्ट्रेशन अनुरोध प्राप्त हुआ!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "नमस्ते ${u.name}! आपकी दुकान '${u.shopName}' का रजिस्ट्रेशन अनुरोध एडमिन को भेज दिया गया है।",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "⏳ एडमिन द्वारा खाता स्वीकार (Approve) किए जाने के बाद आप मोबाइल नंबर (${u.phone}) और अपने पासवर्ड से तुरंत लॉगिन कर सकेंगे।",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val cleanAdmin = adminPhone.replace("[^0-9]".toRegex(), "")
                            val msg = "नमस्ते एडमिन जी! मैंने Tiles and Marbles ERP में नए खाते के लिए रजिस्टर किया है।\n" +
                                    "👤 नाम: ${u.name}\n" +
                                    "🏪 दुकान: ${u.shopName}\n" +
                                    "📱 मोबाइल (लॉगिन ID): ${u.phone}\n" +
                                    "📍 पता: ${u.address}\n\n" +
                                    "कृपया एडमिन ऐप में जाकर इसे स्वीकार (Approve) करें।"
                            try {
                                val url = "https://api.whatsapp.com/send?phone=$cleanAdmin&text=${URLEncoder.encode(msg, "UTF-8")}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (e: Exception) {
                                viewModel.showMessage("WhatsApp नहीं खुला")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("💬 एडमिन को WhatsApp पर बताएं (त्वरित अप्रूवल)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary)
                ) {
                    Text("ठीक है (OK)")
                }
            }
        )
    }

    if (showCloudRestoreDialog) {
        AlertDialog(
            onDismissRequest = { if (!isRestoringCloud) showCloudRestoreDialog = false },
            title = {
                Text(
                    text = "☁️ क्लाउड कोड से डेटा लाएं",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = activeTheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "यदि आपने पुराने फोन से 'क्लाउड सिंक कोड' बनाया था या कोई लिंक है, तो उसे यहाँ दर्ज करें:",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )

                    OutlinedTextField(
                        value = cloudCodeInput,
                        onValueChange = {
                            cloudCodeInput = it
                            cloudRestoreError = null
                        },
                        label = { Text("क्लाउड कोड या लिंक दर्ज करें *") },
                        placeholder = { Text("उदा. 2K4Zw या dpaste लिंक") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (cloudRestoreError != null) {
                        Text(
                            text = cloudRestoreError!!,
                            color = StatusError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val code = cloudCodeInput.trim()
                        if (code.isBlank()) {
                            cloudRestoreError = "कृपया कोड दर्ज करें"
                            return@Button
                        }
                        isRestoringCloud = true
                        viewModel.restoreFromCloudSyncCode(
                            code = code,
                            onSuccess = {
                                isRestoringCloud = false
                                showCloudRestoreDialog = false
                                viewModel.showMessage("✅ डेटा रीस्टोर हो गया! अब लॉगिन करें।")
                            },
                            onError = { err ->
                                isRestoringCloud = false
                                cloudRestoreError = err
                            }
                        )
                    },
                    enabled = !isRestoringCloud,
                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary)
                ) {
                    Text(if (isRestoringCloud) "लाया जा रहा है..." else "रीस्टोर करें")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCloudRestoreDialog = false },
                    enabled = !isRestoringCloud
                ) {
                    Text("रद्द करें")
                }
            }
        )
    }
}
