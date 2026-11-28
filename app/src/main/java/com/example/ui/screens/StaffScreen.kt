package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import java.util.Calendar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.StaffMember
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.getAppThemeOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val staffList by viewModel.staffMembers.collectAsStateWithLifecycle()
    val settings by viewModel.shopSettings.collectAsStateWithLifecycle()
    val activeTheme = getAppThemeOption(settings.appTheme)
    val adminConfig by viewModel.adminGlobalConfig.collectAsStateWithLifecycle()
    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }
    val allowSalaryAdvance = adminConfig?.allowSalaryAdvance != false

    var showAddStaffDialog by remember { mutableStateOf(false) }
    var editingStaff by remember { mutableStateOf<StaffMember?>(null) }
    var staffToDelete by remember { mutableStateOf<StaffMember?>(null) }
    var showQuickAdvanceDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val dateKeyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    val dateDisplayFormat = remember { SimpleDateFormat("dd MMMM yyyy (EEEE)", Locale("hi", "IN")) }

    val selectedDateStr = remember(selectedCalendar) { dateKeyFormat.format(selectedCalendar.time) }
    val selectedDateDisplay = remember(selectedCalendar) { dateDisplayFormat.format(selectedCalendar.time) }

    val attendanceFlow = remember(selectedDateStr) { viewModel.getAttendanceForDate(selectedDateStr) }
    val currentAttendance by attendanceFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = activeTheme.primary,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Staff & Salary Register",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = PureWhite
                        )
                        Text(
                            text = "दुकान कर्मचारी, हाजिरी, एडवांस व वेतन हिसाब",
                            fontSize = 11.sp,
                            color = Color(0xFF89C2D9)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = activeTheme.primary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingStaff = null
                    showAddStaffDialog = true
                },
                containerColor = activeTheme.primary,
                contentColor = PureWhite,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_staff")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Staff")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ नया कर्मचारी", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Overview Summary Card
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(activeTheme.accent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        tint = activeTheme.accentText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "कुल कर्मचारी: ${staffList.size}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = PureWhite
                                    )
                                    val totalPayroll = staffList.filter { it.isActive }.sumOf { it.monthlySalary }
                                    Text(
                                        text = "मासिक पेरोल: ₹${String.format(Locale.US, "%,.0f", totalPayroll)}/माह",
                                        fontSize = 12.sp,
                                        color = activeTheme.accent
                                    )
                                }
                            }

                            if (allowSalaryAdvance) {
                                Button(
                                    onClick = { showQuickAdvanceDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.accent),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payment,
                                        contentDescription = null,
                                        tint = activeTheme.accentText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+ एडवांस दिया",
                                        color = activeTheme.accentText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFEF2F2),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                    modifier = Modifier.clickable {
                                        viewModel.showMessage("⚠️ एडमिन द्वारा कर्मचारियों को एडवांस सैलरी देना बंद (Disabled) किया गया है।")
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("🔒 एडवांस बंद (Admin)", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Attendance Quick-Mark Card (Any Date Selector)
            if (staffList.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Auto-Attendance Switch & Background Schedule Control (Works Open or Closed!)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (settings.autoAttendanceEnabled) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (settings.autoAttendanceEnabled) Color(0xFF93C5FD) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "⚡ ऑटो-हाजिरी (Auto-Present):",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (settings.autoAttendanceEnabled) activeTheme.primary else Color(0xFF64748B)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (settings.autoAttendanceEnabled) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                                modifier = Modifier
                                                    .clickable {
                                                        val updated = settings.copy(autoAttendanceEnabled = !settings.autoAttendanceEnabled)
                                                        if (updated.autoAttendanceEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                        }
                                                        viewModel.saveShopSettings(updated)
                                                        if (updated.autoAttendanceEnabled) {
                                                            viewModel.runAutoAttendanceToday()
                                                        }
                                                    }
                                                    .testTag("auto_attendance_toggle")
                                            ) {
                                                Text(
                                                    text = if (settings.autoAttendanceEnabled) "चालू (ON) ✔" else "बंद (OFF)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (settings.autoAttendanceEnabled) Color(0xFF16A34A) else Color(0xFF64748B),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        if (settings.autoAttendanceEnabled) {
                                            Button(
                                                onClick = { viewModel.runAutoAttendanceToday() },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary),
                                                modifier = Modifier
                                                    .height(28.dp)
                                                    .testTag("run_auto_attendance_now_button")
                                            ) {
                                                Text("आज ऑटो लगाएं", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                                            }
                                        }
                                    }

                                    if (settings.autoAttendanceEnabled) {
                                        Text(
                                            text = "✔ ऐप बंद हो या खुला — रोज़ ${settings.autoAttendanceTime.ifBlank { "09:00" }} बजे सक्रिय स्टाफ की हाजिरी अपने आप लग जाएगी।",
                                            fontSize = 10.sp,
                                            color = Color(0xFF1E40AF)
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState())
                                        ) {
                                            Text("समय चुनें:", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                            listOf("08:00", "09:00", "09:30", "10:00", "10:30").forEach { timeSlot ->
                                                val isSel = settings.autoAttendanceTime.ifBlank { "09:00" } == timeSlot
                                                FilterChip(
                                                    selected = isSel,
                                                    onClick = {
                                                        val updated = settings.copy(autoAttendanceTime = timeSlot)
                                                        viewModel.saveShopSettings(updated)
                                                        viewModel.showMessage("⚡ ऑटो-हाजिरी रोज़ $timeSlot पर सेट हो गई (ऐप बंद होने पर भी चलेगी)")
                                                    },
                                                    label = { Text(timeSlot, fontSize = 10.sp) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Header with Date Navigator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.EventNote,
                                        contentDescription = null,
                                        tint = activeTheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "हाजिरी रजिस्टर (Attendance Register)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = activeTheme.primary
                                        )
                                        Text(
                                            text = "कल या किसी भी पुरानी तारीख की हाजिरी लगाएं",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = { showDatePickerDialog = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = activeTheme.primary)
                                ) {
                                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("तारीख बदलें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Date Navigation Controls (< कल, 📅 आज, अगला >)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val cal = selectedCalendar.clone() as Calendar
                                            cal.add(Calendar.DAY_OF_YEAR, -1)
                                            selectedCalendar = cal
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = selectedDateDisplay,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = activeTheme.primary
                                        )
                                        Text(
                                            text = "तारीख: $selectedDateStr",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(
                                            onClick = { selectedCalendar = Calendar.getInstance() },
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("आज", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        IconButton(
                                            onClick = {
                                                val cal = selectedCalendar.clone() as Calendar
                                                cal.add(Calendar.DAY_OF_YEAR, 1)
                                                selectedCalendar = cal
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Day")
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            staffList.filter { it.isActive }.forEach { member ->
                                val attendanceRec = currentAttendance.find { it.staffId == member.id }
                                val status = attendanceRec?.status ?: "NONE"

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = member.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${member.role} • ₹${String.format(Locale.US, "%,.0f", member.monthlySalary)}/mo",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    // Status Chips: P, H, A with clear/unmark support
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        AttendanceToggleButton(
                                            label = "P (आया)",
                                            isSelected = status == "PRESENT",
                                            selectedColor = StatusSuccess,
                                            onClick = {
                                                if (status == "PRESENT") {
                                                    // Clicking again clears/removes mistakenly marked attendance
                                                    viewModel.deleteAttendance(member.id, selectedDateStr)
                                                } else {
                                                    viewModel.markAttendance(
                                                        staffId = member.id,
                                                        dateMillis = selectedCalendar.timeInMillis,
                                                        dateString = selectedDateStr,
                                                        status = "PRESENT"
                                                    )
                                                }
                                            }
                                        )
                                        AttendanceToggleButton(
                                            label = "½ (आधा)",
                                            isSelected = status == "HALF_DAY",
                                            selectedColor = Color(0xFFEA580C),
                                            onClick = {
                                                if (status == "HALF_DAY") {
                                                    viewModel.deleteAttendance(member.id, selectedDateStr)
                                                } else {
                                                    viewModel.markAttendance(
                                                        staffId = member.id,
                                                        dateMillis = selectedCalendar.timeInMillis,
                                                        dateString = selectedDateStr,
                                                        status = "HALF_DAY"
                                                    )
                                                }
                                            }
                                        )
                                        AttendanceToggleButton(
                                            label = "A (छुट्टी)",
                                            isSelected = status == "ABSENT",
                                            selectedColor = StatusError,
                                            onClick = {
                                                if (status == "ABSENT") {
                                                    viewModel.deleteAttendance(member.id, selectedDateStr)
                                                } else {
                                                    viewModel.markAttendance(
                                                        staffId = member.id,
                                                        dateMillis = selectedCalendar.timeInMillis,
                                                        dateString = selectedDateStr,
                                                        status = "ABSENT",
                                                        notes = "छुट्टी दर्ज"
                                                    )
                                                }
                                            }
                                        )

                                        if (status != "NONE") {
                                            IconButton(
                                                onClick = { viewModel.deleteAttendance(member.id, selectedDateStr) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "हाजिरी हटाएं",
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            // 3. Staff List Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "सभी कर्मचारी सूची (Staff Directory)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${staffList.size} व्यक्ति",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // 4. Staff Cards
            if (staffList.isEmpty()) {
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
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "कोई कर्मचारी दर्ज नहीं है (No Staff Added)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "दुकान के मुनीम, कारीगर, ड्राइवर व हेल्पर को जोड़ने के लिए नीचे '+ नया कर्मचारी' दबाएं।",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(staffList, key = { it.id }) { member ->
                    StaffMemberCard(
                        member = member,
                        activeThemeColor = activeTheme.primary,
                        onCardClick = {
                            viewModel.navigateTo(Screen.StaffDetail(member.id))
                        },
                        onEditClick = {
                            editingStaff = member
                            showAddStaffDialog = true
                        },
                        onDeleteClick = {
                            staffToDelete = member
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Add / Edit Staff Dialog
    if (showAddStaffDialog) {
        AddEditStaffDialog(
            staff = editingStaff,
            onDismiss = { showAddStaffDialog = false },
            onSave = { updatedStaff ->
                viewModel.saveStaff(updatedStaff)
                showAddStaffDialog = false
            }
        )
    }

    // Delete Staff Confirmation Dialog
    if (staffToDelete != null) {
        val member = staffToDelete!!
        AlertDialog(
            onDismissRequest = { staffToDelete = null },
            title = {
                Text(
                    text = "कर्मचारी हटाएं (Delete Staff)?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Text(
                    text = "क्या आप '${member.name}' (${member.role}) को सूची से हटाना चाहते हैं? इनका पुराना डेटा रीसायकल बिन में 10 दिन तक सुरक्षित रहेगा।",
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStaff(member.id)
                        staffToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("हाँ, हटाएं (Delete)", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { staffToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Quick Advance Dialog (With Back-dated Date support)
    if (showQuickAdvanceDialog && staffList.isNotEmpty()) {
        QuickAdvanceDialog(
            staffList = staffList,
            onDismiss = { showQuickAdvanceDialog = false },
            onSave = { staffId, amount, mode, notes, dateMillis ->
                val monthYear = SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(Date(dateMillis))
                viewModel.recordAdvanceOrPayment(
                    staffId = staffId,
                    amount = amount,
                    type = "ADVANCE",
                    forMonthYear = monthYear,
                    mode = mode,
                    notes = notes,
                    dateMillis = dateMillis
                )
                showQuickAdvanceDialog = false
            }
        )
    }

    // Pick Date Dialog for attendance
    if (showDatePickerDialog) {
        var inputDateStr by remember { mutableStateOf(selectedDateStr) }
        AlertDialog(
            onDismissRequest = { showDatePickerDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = activeTheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "हाजिरी की तारीख चुनें (Select Date)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "यदि कल या किसी भी दिन हाजिरी लगाना भूल गए हों, तो वह तारीख चुनें:",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )

                    OutlinedTextField(
                        value = inputDateStr,
                        onValueChange = { inputDateStr = it },
                        label = { Text("तारीख दर्ज करें (YYYY-MM-DD) *") },
                        placeholder = { Text("उदा. 2026-10-19") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.DAY_OF_YEAR, -1)
                                inputDateStr = dateKeyFormat.format(cal.time)
                            },
                            label = { Text("कल (Yesterday)") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                inputDateStr = dateKeyFormat.format(Date())
                            },
                            label = { Text("आज (Today)") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = try {
                            dateKeyFormat.parse(inputDateStr.trim())
                        } catch (e: Exception) {
                            null
                        }
                        if (parsed != null) {
                            val cal = Calendar.getInstance()
                            cal.time = parsed
                            selectedCalendar = cal
                            showDatePickerDialog = false
                        } else {
                            viewModel.showMessage("अमान्य तारीख! कृपया YYYY-MM-DD प्रारूप में डालें।")
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("तारीख सेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
private fun AttendanceToggleButton(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) selectedColor else Color(0xFFF1F5F9),
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) PureWhite else Color(0xFF475569),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun StaffMemberCard(
    member: StaffMember,
    activeThemeColor: Color,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(activeThemeColor.copy(alpha = 0.12f))
                    ) {
                        Text(
                            text = member.name.firstOrNull()?.uppercaseChar()?.toString() ?: "S",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = activeThemeColor
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = member.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = member.role.ifBlank { "Worker" },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = activeThemeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format(Locale.US, "%,.0f", member.monthlySalary)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = activeThemeColor
                    )
                    Text(
                        text = "प्रति माह (Monthly)",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                    if (member.openingBalance != 0.0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "पिछला: +₹${String.format(Locale.US, "%,.0f", member.openingBalance)}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (member.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = member.phone, fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                } else {
                    Text(text = "No phone number", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Staff",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("एडिट", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onCardClick,
                        colors = ButtonDefaults.buttonColors(containerColor = activeThemeColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("खाता देखें →", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditStaffDialog(
    staff: StaffMember?,
    onDismiss: () -> Unit,
    onSave: (StaffMember) -> Unit
) {
    var name by remember { mutableStateOf(staff?.name ?: "") }
    var phone by remember { mutableStateOf(staff?.phone ?: "") }
    var role by remember { mutableStateOf(staff?.role ?: "Karigar / Fitter") }
    var salaryCalculationType by remember { mutableStateOf(staff?.salaryCalculationType ?: "FIXED_30_DAYS") }
    var salaryStr by remember { mutableStateOf(if (staff != null && staff.monthlySalary > 0) staff.monthlySalary.toInt().toString() else "15000") }
    var dailyWageRateStr by remember { mutableStateOf(if (staff != null && staff.dailyWageRate > 0) staff.dailyWageRate.toInt().toString() else "500") }
    var workingDaysStr by remember { mutableStateOf(if (staff != null && staff.workingDaysPerMonth > 0) staff.workingDaysPerMonth.toString() else "26") }
    var hasWeeklyOff by remember { mutableStateOf(staff?.hasWeeklyOff ?: true) }
    val sdfDate = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var joiningDateStr by remember {
        mutableStateOf(sdfDate.format(Date(staff?.joiningDate ?: System.currentTimeMillis())))
    }
    var openingBalanceStr by remember { mutableStateOf(if (staff != null && staff.openingBalance != 0.0) staff.openingBalance.toInt().toString() else "") }
    var notes by remember { mutableStateOf(staff?.notes ?: "") }

    val rolesList = listOf("Karigar / Fitter", "Salesman", "Loading / Helper", "Driver", "Munim / Accountant", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (staff == null) "नया कर्मचारी जोड़ें (Add Staff)" else "कर्मचारी जानकारी अपडेट करें",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("कर्मचारी का नाम (Full Name) *") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("मोबाइल नंबर (Mobile Phone)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("काम का पद (Role / Designation):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rolesList.take(3).forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, fontSize = 10.sp) }
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rolesList.drop(3).forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, fontSize = 10.sp) }
                        )
                    }
                }

                // Joining Date field
                OutlinedTextField(
                    value = joiningDateStr,
                    onValueChange = { joiningDateStr = it },
                    label = { Text("कार्य शुरू करने की तारीख (Joining Date: DD/MM/YYYY)") },
                    placeholder = { Text("उदा. 01/01/2026") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Salary Calculation Method (4 Options)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "वेतन गणना का तरीका (Salary Calculation Method):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )

                        listOf(
                            "FIXED_30_DAYS" to "1. 30 दिन का आधार (30 Days Basis)",
                            "ACTUAL_MONTH_DAYS" to "2. वास्तविक महीने के दिन (Actual Days in Month)",
                            "WORKING_DAYS" to "3. कार्य दिवस (Working Days e.g. 26 दिन)",
                            "DAILY_WAGE" to "4. दैनिक मजदूरी (Daily Wage Rate)"
                        ).forEach { (typeKey, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { salaryCalculationType = typeKey }
                                    .padding(vertical = 2.dp)
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = salaryCalculationType == typeKey,
                                    onClick = { salaryCalculationType = typeKey }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(label, fontSize = 11.sp, color = Color(0xFF334155))
                            }
                        }
                    }
                }

                // Inputs based on calculation type
                if (salaryCalculationType == "DAILY_WAGE") {
                    OutlinedTextField(
                        value = dailyWageRateStr,
                        onValueChange = { dailyWageRateStr = it },
                        label = { Text("प्रति दिन मजदूरी ₹ (Daily Wage Rate) *") },
                        placeholder = { Text("उदा. 500") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = salaryStr,
                        onValueChange = { salaryStr = it },
                        label = { Text("प्रति माह सैलरी ₹ (Monthly Salary) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (salaryCalculationType == "WORKING_DAYS") {
                        OutlinedTextField(
                            value = workingDaysStr,
                            onValueChange = { workingDaysStr = it },
                            label = { Text("महीने के कुल कार्य दिवस (Working Days Count) *") },
                            placeholder = { Text("डिफ़ॉल्ट 26 दिन") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Weekly Off (साप्ताहिक छुट्टी रखनी है या नहीं)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "साप्ताहिक छुट्टी (Weekly Off):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = hasWeeklyOff,
                                onClick = { hasWeeklyOff = true },
                                label = { Text("हाँ (रविवार अवकाश मान्य)", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = !hasWeeklyOff,
                                onClick = { hasWeeklyOff = false },
                                label = { Text("नहीं (कोई छुट्टी नहीं)", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = openingBalanceStr,
                    onValueChange = { openingBalanceStr = it },
                    label = { Text("पिछला बकाया / पूर्व हिसाब ₹ (Previous Balance / Due)") },
                    placeholder = { Text("उदा. 2500 (जो पहले से देना या बकाया था)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("नोट्स / पता (Optional Remarks)") },
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val sal = salaryStr.toDoubleOrNull() ?: 0.0
                    val dailyWage = dailyWageRateStr.toDoubleOrNull() ?: 0.0
                    val workDays = workingDaysStr.toIntOrNull() ?: 26
                    val openBal = openingBalanceStr.toDoubleOrNull() ?: 0.0
                    val joinMillis = try {
                        sdfDate.parse(joiningDateStr.trim())?.time ?: System.currentTimeMillis()
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }

                    onSave(
                        (staff ?: StaffMember(name = name.trim())).copy(
                            name = name.trim(),
                            phone = phone.trim(),
                            role = role,
                            monthlySalary = if (salaryCalculationType == "DAILY_WAGE" && sal <= 0) dailyWage * 30 else sal,
                            dailyWageRate = dailyWage,
                            salaryCalculationType = salaryCalculationType,
                            workingDaysPerMonth = workDays,
                            hasWeeklyOff = hasWeeklyOff,
                            joiningDate = joinMillis,
                            notes = notes.trim(),
                            openingBalance = openBal
                        )
                    )
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("सुरक्षित करें (Save)")
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
private fun QuickAdvanceDialog(
    staffList: List<StaffMember>,
    onDismiss: () -> Unit,
    onSave: (staffId: Long, amount: Double, mode: String, notes: String, dateMillis: Long) -> Unit
) {
    var selectedStaffId by remember { mutableStateOf(staffList.firstOrNull()?.id ?: 0L) }
    val sdfDate = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var dateStr by remember { mutableStateOf(sdfDate.format(Date())) }
    var amountStr by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "एडवांस पैसा दिया (+ Record Advance)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text("किस कर्मचारी को दिया (Select Staff):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                staffList.forEach { s ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStaffId = s.id }
                            .padding(vertical = 3.dp)
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedStaffId == s.id,
                            onClick = { selectedStaffId = s.id }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "${s.name} (${s.role})", fontSize = 13.sp)
                    }
                }

                // Date Selection with Quick Presets
                Text("एडवांस देने की तारीख (Date of Advance):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { dateStr = sdfDate.format(Date()) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("आज (Today)", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, -1)
                            dateStr = sdfDate.format(cal.time)
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("कल (Yesterday)", fontSize = 10.sp)
                    }
                }

                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("तारीख (DD/MM/YYYY) *") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("एडवांस रकम ₹ (Amount in Rupees) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = paymentMode == "CASH",
                        onClick = { paymentMode = "CASH" },
                        label = { Text("Cash (नकद)") }
                    )
                    FilterChip(
                        selected = paymentMode == "UPI",
                        onClick = { paymentMode = "UPI" },
                        label = { Text("UPI / Online") }
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("किस काम के लिए लिया (Reason / Purpose)") },
                    placeholder = { Text("उदा. घर के खर्च के लिए, दवाई आदि") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    val dateMillis = try {
                        sdfDate.parse(dateStr.trim())?.time ?: System.currentTimeMillis()
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }
                    if (amt > 0 && selectedStaffId != 0L) {
                        onSave(selectedStaffId, amt, paymentMode, notes.trim(), dateMillis)
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("एडवांस दर्ज करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
