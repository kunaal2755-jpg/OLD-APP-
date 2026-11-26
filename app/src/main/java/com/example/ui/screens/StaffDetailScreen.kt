package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.StaffAttendance
import com.example.data.model.StaffMember
import com.example.data.model.StaffPayment
import com.example.ui.MainViewModel
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.getAppThemeOption
import com.example.util.ShareHelper
import com.example.util.StaffPdfGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun calculateStaffDailyRate(staff: StaffMember, daysInMonth: Int): Double {
    return when (staff.salaryCalculationType) {
        "FIXED_30_DAYS" -> if (staff.monthlySalary > 0) staff.monthlySalary / 30.0 else 0.0
        "ACTUAL_MONTH_DAYS" -> if (daysInMonth > 0 && staff.monthlySalary > 0) staff.monthlySalary / daysInMonth.toDouble() else 0.0
        "WORKING_DAYS" -> {
            val wDays = if (staff.workingDaysPerMonth > 0) staff.workingDaysPerMonth else 26
            if (staff.monthlySalary > 0) staff.monthlySalary / wDays.toDouble() else 0.0
        }
        "DAILY_WAGE" -> staff.dailyWageRate
        else -> if (daysInMonth > 0) staff.monthlySalary / daysInMonth.toDouble() else 0.0
    }
}

fun getSalaryCalculationLabel(staff: StaffMember): String {
    return when (staff.salaryCalculationType) {
        "FIXED_30_DAYS" -> "30 दिन का आधार"
        "ACTUAL_MONTH_DAYS" -> "वास्तविक महीने के दिन"
        "WORKING_DAYS" -> "कार्य दिवस आधार (${staff.workingDaysPerMonth} दिन)"
        "DAILY_WAGE" -> "दैनिक मजदूरी (₹${staff.dailyWageRate.toInt()}/दिन)"
        else -> "30 दिन का आधार"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDetailScreen(
    staffId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val staffList by viewModel.staffMembers.collectAsStateWithLifecycle()
    val staff = staffList.find { it.id == staffId }
    val settings by viewModel.shopSettings.collectAsStateWithLifecycle()
    val activeTheme = getAppThemeOption(settings.appTheme)
    val adminConfig by viewModel.adminGlobalConfig.collectAsStateWithLifecycle()
    val allowSalaryAdvance = adminConfig?.allowSalaryAdvance != false

    val attendanceFlow = remember(staffId) { viewModel.repository.getAttendanceForStaff(staffId) }
    val allAttendance by attendanceFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val paymentsFlow = remember(staffId) { viewModel.repository.getPaymentsForStaff(staffId) }
    val allPayments by paymentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val settlementsFlow = remember(staffId) { viewModel.getSettlementsForStaff(staffId) }
    val allSettlements by settlementsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // Month Selector State
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.ENGLISH) }
    val monthKeyFormat = remember { SimpleDateFormat("yyyy-MM", Locale.ENGLISH) }
    val monthYearDisplayFormat = remember { SimpleDateFormat("MMM yyyy", Locale.ENGLISH) }

    val currentMonthDisplay = remember(selectedCalendar) { monthYearFormat.format(selectedCalendar.time) }
    val currentMonthKey = remember(selectedCalendar) { monthKeyFormat.format(selectedCalendar.time) }
    val currentMonthYearFilter = remember(selectedCalendar) { monthYearDisplayFormat.format(selectedCalendar.time) }

    // Tabs: 0 = Monthly Calendar & Leaves, 1 = Advances & Payments Ledger
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    var showAdvanceDialog by remember { mutableStateOf(false) }
    var selectedDateForAttendance by remember { mutableStateOf<String?>(null) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showPdfPreviewDialog by remember { mutableStateOf(false) }

    // Custom Date Range PDF State
    var showCustomDateDialog by remember { mutableStateOf(false) }
    val sdfDisplay = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var customFromDate by remember {
        mutableStateOf(sdfDisplay.format(Date(System.currentTimeMillis() - 30L * 86400000L)))
    }
    var customToDate by remember {
        mutableStateOf(sdfDisplay.format(Date()))
    }

    if (staff == null) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(color = activeTheme.primary)
        }
        return
    }

    // Days in current selected month
    val daysInMonth = remember(selectedCalendar) {
        val cal = selectedCalendar.clone() as Calendar
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Filter attendance for current selected month
    val monthAttendance = remember(allAttendance, currentMonthKey) {
        allAttendance.filter { it.dateString.startsWith(currentMonthKey) }
    }

    // Map of dateString -> StaffAttendance
    val attendanceMap = remember(monthAttendance) {
        monthAttendance.associateBy { it.dateString }
    }

    // Filter payments for current selected month
    val monthPayments = remember(allPayments, currentMonthYearFilter, currentMonthKey) {
        allPayments.filter { p ->
            val cal = Calendar.getInstance().apply { timeInMillis = p.dateMillis }
            val pKey = monthKeyFormat.format(cal.time)
            pKey == currentMonthKey || p.forMonthYear.equals(currentMonthYearFilter, ignoreCase = true)
        }
    }

    val presentDays = monthAttendance.count { it.status == "PRESENT" }
    val halfDays = monthAttendance.count { it.status == "HALF_DAY" }
    val absentDays = monthAttendance.count { it.status == "ABSENT" }
    val paidLeaveDays = monthAttendance.count { it.status == "PAID_LEAVE" }

    // Calculations: Based on chosen salaryCalculationType (30 days, actual days, working days, daily wage)
    val dailyRate = calculateStaffDailyRate(staff, daysInMonth)
    val effectiveDaysWorked = presentDays + paidLeaveDays + (halfDays * 0.5)
    val earnedGrossSalary = effectiveDaysWorked * dailyRate

    val totalAdvancesTaken = monthPayments.filter { it.type == "ADVANCE" }.sumOf { it.amount }
    val totalSalariesPaid = monthPayments.filter { it.type == "SALARY" }.sumOf { it.amount }
    val netBalancePayable = earnedGrossSalary + staff.openingBalance - totalAdvancesTaken - totalSalariesPaid

    // Current Month Finalized settlement check
    val currentSettlement = allSettlements.find { it.monthKey == currentMonthKey }
    val isCurrentMonthFinalized = currentSettlement?.isFinalized ?: false

    // Prior Unfinalized Arrears Calculation from earliest employment / history date
    val (priorUnfinalizedBalance, unfinalizedDetailsList, unfinalizedMonthsSummaryText) = remember(
        allSettlements,
        allAttendance,
        allPayments,
        staff,
        currentMonthKey,
        selectedCalendar
    ) {
        var accumulatedUnfinalized = 0.0
        val list = mutableListOf<com.example.data.model.UnfinalizedMonthDetail>()

        val earliestMillis = listOfNotNull(
            staff.joiningDate,
            allAttendance.minOfOrNull { it.dateMillis },
            allPayments.minOfOrNull { it.dateMillis }
        ).minOrNull() ?: staff.joiningDate

        val curCalIter = Calendar.getInstance().apply {
            timeInMillis = earliestMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val selectedMonthStart = Calendar.getInstance().apply {
            time = selectedCalendar.time
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val mKeyFormat = SimpleDateFormat("yyyy-MM", Locale.ENGLISH)
        val mDisplayFormat = SimpleDateFormat("MMM yyyy", Locale.ENGLISH)

        while (curCalIter.before(selectedMonthStart)) {
            val pKey = mKeyFormat.format(curCalIter.time)
            val pDisplay = mDisplayFormat.format(curCalIter.time)
            val pastDaysInMonth = curCalIter.getActualMaximum(Calendar.DAY_OF_MONTH)
            val pDailyRate = calculateStaffDailyRate(staff, pastDaysInMonth)

            val pastSettlement = allSettlements.find { it.monthKey == pKey }
            val isFinalized = pastSettlement?.isFinalized ?: false

            if (!isFinalized) {
                val pAttendance = allAttendance.filter { it.dateString.startsWith(pKey) }
                val pP = pAttendance.count { it.status == "PRESENT" }
                val pH = pAttendance.count { it.status == "HALF_DAY" }
                val pPL = pAttendance.count { it.status == "PAID_LEAVE" }
                val pEffDays = pP + pPL + (pH * 0.5)
                val pEarned = pEffDays * pDailyRate

                val pPayments = allPayments.filter { p ->
                    val pDateCal = Calendar.getInstance().apply { timeInMillis = p.dateMillis }
                    val paymentMKey = mKeyFormat.format(pDateCal.time)
                    paymentMKey == pKey || p.forMonthYear.equals(pDisplay, ignoreCase = true)
                }
                val pAdvance = pPayments.filter { it.type == "ADVANCE" }.sumOf { it.amount }
                val pSalary = pPayments.filter { it.type == "SALARY" }.sumOf { it.amount }

                val netUnpaidForMonth = (pEarned - pAdvance - pSalary).coerceAtLeast(0.0)
                if (netUnpaidForMonth > 0.0) {
                    accumulatedUnfinalized += netUnpaidForMonth
                    list.add(
                        com.example.data.model.UnfinalizedMonthDetail(
                            monthKey = pKey,
                            monthDisplay = pDisplay,
                            earnedGross = pEarned,
                            advancesTaken = pAdvance,
                            salaryPaid = pSalary,
                            netUnpaid = netUnpaidForMonth
                        )
                    )
                }
            }

            curCalIter.add(Calendar.MONTH, 1)
        }

        val text = if (list.isNotEmpty()) {
            list.joinToString(", ") { "${it.monthDisplay} (₹${String.format(Locale.US, "%,.0f", it.netUnpaid)})" }
        } else ""

        Triple(accumulatedUnfinalized, list, text)
    }

    val totalNetPayable = netBalancePayable + priorUnfinalizedBalance

    val currentMonthlySummary = remember(
        staff, currentMonthDisplay, daysInMonth, presentDays, halfDays, absentDays, paidLeaveDays,
        totalAdvancesTaken, totalSalariesPaid, earnedGrossSalary, totalNetPayable, priorUnfinalizedBalance,
        unfinalizedMonthsSummaryText, isCurrentMonthFinalized
    ) {
        com.example.data.model.StaffMonthlySummary(
            staff = staff,
            monthYear = currentMonthDisplay,
            totalDaysInMonth = daysInMonth,
            presentDays = presentDays,
            halfDays = halfDays,
            absentDays = absentDays,
            paidLeaveDays = paidLeaveDays,
            totalAdvanceTaken = totalAdvancesTaken,
            totalSalaryPaid = totalSalariesPaid,
            earnedSalary = earnedGrossSalary,
            netBalancePayable = totalNetPayable,
            previousUnfinalizedBalance = priorUnfinalizedBalance,
            unfinalizedMonthsText = unfinalizedMonthsSummaryText,
            isFinalized = isCurrentMonthFinalized
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = activeTheme.primary,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = staff.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = PureWhite
                        )
                        Text(
                            text = "${staff.role} • ₹${String.format(Locale.US, "%,.0f", staff.monthlySalary)}/माह",
                            fontSize = 11.sp,
                            color = Color(0xFF89C2D9)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                actions = {
                    // Custom Date Range PDF Button
                    IconButton(onClick = { showCustomDateDialog = true }) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Custom Date Range PDF", tint = PureWhite)
                    }

                    // Generate Monthly PDF Button
                    IconButton(onClick = {
                        val pdf = StaffPdfGenerator.generateSalaryPdf(
                            context = context,
                            shopSettings = settings,
                            staff = staff,
                            monthYearDisplay = currentMonthDisplay,
                            daysInMonth = daysInMonth,
                            attendanceList = monthAttendance,
                            paymentsList = monthPayments,
                            summary = currentMonthlySummary
                        )
                        generatedPdfFile = pdf
                        showPdfPreviewDialog = true
                    }) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF Salary Slip", tint = PureWhite)
                    }

                    // Share on WhatsApp
                    IconButton(onClick = {
                        shareStaffSalarySlip(
                            context = context,
                            shopName = settings.shopName,
                            staff = staff,
                            monthYear = currentMonthDisplay,
                            daysInMonth = daysInMonth,
                            presentDays = presentDays,
                            halfDays = halfDays,
                            absentDays = absentDays,
                            earnedSalary = earnedGrossSalary,
                            totalAdvance = totalAdvancesTaken,
                            totalPaid = totalSalariesPaid,
                            netBalance = totalNetPayable,
                            advanceList = monthPayments.filter { it.type == "ADVANCE" }
                        )
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = PureWhite)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Month Selector Header
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        IconButton(onClick = {
                            val newCal = selectedCalendar.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            selectedCalendar = newCal
                        }) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = activeTheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentMonthDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = activeTheme.primary
                            )
                        }

                        IconButton(onClick = {
                            val newCal = selectedCalendar.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            selectedCalendar = newCal
                        }) {
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month")
                        }
                    }
                }
            }

            // 1.5 Monthly Settlement Final Yes/No Option
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentMonthFinalized) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                    ),
                    border = BorderStroke(1.2.dp, if (isCurrentMonthFinalized) StatusSuccess else Color(0xFFF59E0B)),
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
                                    text = "💼 इस माह ($currentMonthDisplay) का हिसाब फाइनल?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isCurrentMonthFinalized) Color(0xFF166534) else Color(0xFF92400E)
                                )
                                Text(
                                    text = if (isCurrentMonthFinalized)
                                        "✅ हिसाब चुकता/फाइनल है (यह बकाया अगले माह में नहीं जुड़ेगा)"
                                    else
                                        "⚠️ हिसाब अनफाइनल (बकाया अगले माह की स्लिप/PDF में जुड़ेगा)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    viewModel.saveStaffSettlement(staff.id, currentMonthKey, true)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrentMonthFinalized) StatusSuccess else Color(0xFFE2E8F0),
                                    contentColor = if (isCurrentMonthFinalized) PureWhite else Color(0xFF475569)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("✅ हाँ (Finalized)", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                            Button(
                                onClick = {
                                    viewModel.saveStaffSettlement(staff.id, currentMonthKey, false)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isCurrentMonthFinalized) Color(0xFFDC2626) else Color(0xFFE2E8F0),
                                    contentColor = if (!isCurrentMonthFinalized) PureWhite else Color(0xFF475569)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("❌ नहीं (Unfinalized)", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }

            // 2. Comprehensive Monthly Salary & Leave Calculation Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "महीने का पगार व छुट्टी हिसाब",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = activeTheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = activeTheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "$daysInMonth दिन का महीना",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = activeTheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats Grid (Present, Half Day, Absent)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CalculationChip(
                                title = "उपस्थित (P)",
                                value = "$presentDays दिन",
                                color = StatusSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            CalculationChip(
                                title = "आधा दिन (½)",
                                value = "$halfDays दिन",
                                color = Color(0xFFEA580C),
                                modifier = Modifier.weight(1f)
                            )
                            CalculationChip(
                                title = "छुट्टियाँ (A)",
                                value = "$absentDays दिन",
                                color = StatusError,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        CalculationRow(
                            label = "वेतन गणना का तरीका:",
                            amount = getSalaryCalculationLabel(staff),
                            color = activeTheme.primary,
                            isBold = true
                        )
                        CalculationRow(
                            label = "साप्ताहिक छुट्टी (Weekly Off):",
                            amount = if (staff.hasWeeklyOff) "हाँ (रविवार अवकाश मान्य)" else "नहीं (सभी दिन कार्य)",
                            color = if (staff.hasWeeklyOff) StatusSuccess else Color(0xFF64748B)
                        )
                        if (staff.salaryCalculationType == "DAILY_WAGE") {
                            CalculationRow(
                                label = "तय दैनिक मजदूरी दर (Daily Wage):",
                                amount = "₹${staff.dailyWageRate.toInt()}/दिन"
                            )
                        } else {
                            CalculationRow(
                                label = "मासिक तय वेतन (Monthly Base):",
                                amount = "₹${String.format(Locale.US, "%,.2f", staff.monthlySalary)}"
                            )
                        }
                        CalculationRow(
                            label = "लागू प्रति दिन दर (Effective Daily Rate):",
                            amount = "₹${String.format(Locale.US, "%,.2f", dailyRate)}/दिन"
                        )
                        CalculationRow(
                            label = "काम के अनुसार अर्जित वेतन (Earned):",
                            amount = "₹${String.format(Locale.US, "%,.2f", earnedGrossSalary)}",
                            isBold = true
                        )
                        if (staff.openingBalance != 0.0) {
                            CalculationRow(
                                label = "पिछला बकाया / पूर्व हिसाब (+ Previous Balance):",
                                amount = "+ ₹${String.format(Locale.US, "%,.2f", staff.openingBalance)}",
                                color = StatusSuccess,
                                isBold = true
                            )
                        }
                        if (priorUnfinalizedBalance > 0.0) {
                            CalculationRow(
                                label = "पिछला अनफाइनल बकाया (+ Prior Arrears):",
                                amount = "+ ₹${String.format(Locale.US, "%,.2f", priorUnfinalizedBalance)}",
                                color = Color(0xFFDC2626),
                                isBold = true
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(0.8.dp, Color(0xFFFECACA)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "⚠️ अनफाइनल महीने: $unfinalizedMonthsSummaryText",
                                    fontSize = 10.sp,
                                    color = Color(0xFFB91C1C),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        CalculationRow(
                            label = "कुल एडवांस लिया गया (Advance Deducted):",
                            amount = "- ₹${String.format(Locale.US, "%,.2f", totalAdvancesTaken)}",
                            color = StatusError,
                            isBold = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Net Balance Box
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (totalNetPayable >= 0) activeTheme.primary.copy(alpha = 0.08f) else Color(0xFFFEE2E2),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (totalNetPayable >= 0) activeTheme.primary else StatusError
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = if (totalNetPayable >= 0) "बाकी देना है (Net Payable)" else "कर्मचारी पर एडवांस बकाया (Excess Advance)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (totalNetPayable >= 0) activeTheme.primary else StatusError
                                    )
                                    Text(
                                        text = "${presentDays} दिन उपस्थिति • ₹${String.format(Locale.US, "%.0f", totalAdvancesTaken)} एडवांस" +
                                                if (priorUnfinalizedBalance > 0.0) " • ₹${String.format(Locale.US, "%.0f", priorUnfinalizedBalance)} पुराना बकाया" else "",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Text(
                                    text = "₹${String.format(Locale.US, "%,.2f", kotlin.math.abs(totalNetPayable))}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = if (totalNetPayable >= 0) activeTheme.primary else StatusError
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons: + Record Advance, PDF Preview/Download, Custom Date Range PDF
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (allowSalaryAdvance) {
                                Button(
                                    onClick = { showAdvanceDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ एडवांस दिया", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.showMessage("⚠️ एडमिन द्वारा कर्मचारियों को एडवांस सैलरी देना बंद (Disabled) किया गया है।") },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("🔒 एडवांस बंद", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    val pdf = StaffPdfGenerator.generateSalaryPdf(
                                        context = context,
                                        shopSettings = settings,
                                        staff = staff,
                                        monthYearDisplay = currentMonthDisplay,
                                        daysInMonth = daysInMonth,
                                        attendanceList = monthAttendance,
                                        paymentsList = monthPayments,
                                        summary = currentMonthlySummary
                                    )
                                    generatedPdfFile = pdf
                                    showPdfPreviewDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF हिसाब", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showCustomDateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("कस्टम PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Tab Selector: Calendar vs Advances Ledger
            item {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = activeTheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Text(
                                text = "📅 हाजिरी व छुट्टी कैलेंडर",
                                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Text(
                                text = "💰 एडवांस व लेन-देन (${monthPayments.size})",
                                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            // Tab 0: Interactive Calendar Grid (Kisi bhi din ki chutti ya present lagayein)
            if (selectedTabIndex == 0) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "किसी भी तारीख पर टैप करके हाजिरी/छुट्टी बदलें:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = activeTheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "यदि कल या किसी भी दिन हाजिरी लगाना भूल गए हों, तो उस तारीख पर क्लिक करें।",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Interactive Days Grid (1 to daysInMonth)
                            val daysList = (1..daysInMonth).toList()
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                daysList.chunked(7).forEach { weekDays ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        weekDays.forEach { dayNum ->
                                            val dateStr = String.format(Locale.ENGLISH, "%s-%02d", currentMonthKey, dayNum)
                                            val att = attendanceMap[dateStr]
                                            val status = att?.status

                                            CalendarDayCell(
                                                dayNum = dayNum,
                                                status = status,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { selectedDateForAttendance = dateStr }
                                            )
                                        }
                                        // Fill remaining columns in last week if needed
                                        repeat(7 - weekDays.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Legend
                            Row(
                                horizontalArrangement = Arrangement.SpaceAround,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LegendItem(label = "P = उपस्थित", color = StatusSuccess)
                                LegendItem(label = "½ = आधा दिन", color = Color(0xFFEA580C))
                                LegendItem(label = "A = छुट्टी", color = StatusError)
                                LegendItem(label = "- = खाली", color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }

            // Tab 1: Advance / Khata Ledger with Date, Amount, Reason
            if (selectedTabIndex == 1) {
                if (monthPayments.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                Text(
                                    text = "$currentMonthDisplay में कोई एडवांस नहीं लिया गया",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "किसी भी तारीख को लिया गया पैसा ऊपर '+ एडवांस दिया' से तारीख चुनकर दर्ज करें।",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                } else {
                    items(monthPayments, key = { it.id }) { payment ->
                        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (payment.type == "ADVANCE") Color(0xFFFEE2E2) else Color(0xFFDCFCE7))
                                    ) {
                                        Text(
                                            text = if (payment.type == "ADVANCE") "एड" else "वे",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (payment.type == "ADVANCE") StatusError else StatusSuccess
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (payment.type == "ADVANCE") "एडवांस पैसा लिया" else "वेतन भुगतान",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = sdf.format(Date(payment.dateMillis)) + " • ${payment.paymentMode}",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        if (payment.notes.isNotBlank()) {
                                            Text(
                                                text = "कारण: ${payment.notes}",
                                                fontSize = 11.sp,
                                                color = activeTheme.primary
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,.2f", payment.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (payment.type == "ADVANCE") StatusError else StatusSuccess
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteStaffPayment(payment.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Modal to mark attendance for ANY date tapped on the calendar (with Clear Attendance support)
    if (selectedDateForAttendance != null) {
        val dateStr = selectedDateForAttendance!!
        val currentRecord = attendanceMap[dateStr]
        MarkCalendarDateAttendanceDialog(
            staffName = staff.name,
            dateString = dateStr,
            currentStatus = currentRecord?.status,
            currentNotes = currentRecord?.notes ?: "",
            onDismiss = { selectedDateForAttendance = null },
            onSave = { status, notes ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val d = sdf.parse(dateStr) ?: Date()
                viewModel.markAttendance(
                    staffId = staffId,
                    dateMillis = d.time,
                    dateString = dateStr,
                    status = status,
                    notes = notes
                )
                selectedDateForAttendance = null
            },
            onClearAttendance = {
                viewModel.deleteAttendance(staffId, dateStr)
                selectedDateForAttendance = null
            }
        )
    }

    // Record Advance Dialog (Allows picking ANY back-dated or future date for advance entry)
    if (showAdvanceDialog) {
        RecordAdvanceWithDateDialog(
            staffName = staff.name,
            currentMonthKey = currentMonthKey,
            onDismiss = { showAdvanceDialog = false },
            onSave = { amount, type, dateMillis, mode, notes ->
                viewModel.recordAdvanceOrPayment(
                    staffId = staffId,
                    amount = amount,
                    type = type,
                    forMonthYear = currentMonthYearFilter,
                    mode = mode,
                    notes = notes,
                    dateMillis = dateMillis
                )
                showAdvanceDialog = false
            }
        )
    }

    // Custom Date Range PDF Dialog
    if (showCustomDateDialog) {
        CustomDateRangePdfDialog(
            staffName = staff.name,
            fromDateStr = customFromDate,
            toDateStr = customToDate,
            onDismiss = { showCustomDateDialog = false },
            onGenerate = { from, to ->
                customFromDate = from
                customToDate = to
                val sdfUser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fromDateObj = try { sdfUser.parse(from) } catch (_: Exception) { null } ?: Date(0)
                val toDateObj = try { sdfUser.parse(to) } catch (_: Exception) { null } ?: Date()
                val fromMillis = fromDateObj.time
                val toMillis = toDateObj.time + 86399999L // Include whole end day

                val filteredAtt = allAttendance.filter { it.dateMillis in fromMillis..toMillis }
                val filteredPay = allPayments.filter { it.dateMillis in fromMillis..toMillis }

                val pdf = StaffPdfGenerator.generateCustomDateRangePdf(
                    context = context,
                    shopSettings = settings,
                    staff = staff,
                    fromDate = from,
                    toDate = to,
                    attendanceList = filteredAtt,
                    paymentsList = filteredPay
                )
                generatedPdfFile = pdf
                showCustomDateDialog = false
                showPdfPreviewDialog = true
            }
        )
    }

    // PDF Preview & Download Dialog
    if (showPdfPreviewDialog && generatedPdfFile != null) {
        val pdf = generatedPdfFile!!
        PdfSalaryPreviewModal(
            pdfFile = pdf,
            staffName = staff.name,
            monthYear = currentMonthDisplay,
            onDismiss = { showPdfPreviewDialog = false },
            onOpenPdf = {
                ShareHelper.viewOrPrintPdf(context, pdf)
            },
            onSharePdf = {
                val msg = "Salary Slip for ${staff.name} ($currentMonthDisplay) from ${settings.shopName}"
                ShareHelper.sharePdfGeneral(context, pdf, msg)
            }
        )
    }
}

@Composable
private fun CustomDateRangePdfDialog(
    staffName: String,
    fromDateStr: String,
    toDateStr: String,
    onDismiss: () -> Unit,
    onGenerate: (String, String) -> Unit
) {
    var from by remember { mutableStateOf(fromDateStr) }
    var to by remember { mutableStateOf(toDateStr) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF0F766E))
                Spacer(modifier = Modifier.width(8.dp))
                Text("📅 कस्टम तारीख हाजिरी व पगार PDF", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "$staffName के लिए जिस अवधि की रिपोर्ट चाहिए, उसकी तारीख दर्ज करें:",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                OutlinedTextField(
                    value = from,
                    onValueChange = { from = it },
                    label = { Text("प्रारंभिक तारीख (From Date)") },
                    placeholder = { Text("dd/MM/yyyy उदा. 01/10/2026") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = to,
                    onValueChange = { to = it },
                    label = { Text("अंतिम तारीख (To Date)") },
                    placeholder = { Text("dd/MM/yyyy उदा. 31/10/2026") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(from.trim(), to.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("📄 PDF बनाएं", fontWeight = FontWeight.Bold)
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
private fun CalendarDayCell(
    dayNum: Int,
    status: String?,
    modifier: Modifier = Modifier
) {
    val (bg, badge, textColor) = when (status) {
        "PRESENT" -> Triple(Color(0xFFDCFCE7), "P", StatusSuccess)
        "HALF_DAY" -> Triple(Color(0xFFFFEDD5), "½", Color(0xFFEA580C))
        "ABSENT" -> Triple(Color(0xFFFEE2E2), "A", StatusError)
        "PAID_LEAVE" -> Triple(Color(0xFFE0F2FE), "L", Color(0xFF0284C7))
        else -> Triple(Color(0xFFF8FAFC), "-", Color(0xFF94A3B8))
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (status != null) textColor.copy(alpha = 0.4f) else Color(0xFFE2E8F0)),
        modifier = modifier.height(48.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(2.dp)
        ) {
            Text(
                text = dayNum.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color(0xFF1E293B)
            )
            Text(
                text = badge,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = textColor
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color(0xFF475569))
    }
}

@Composable
private fun MarkCalendarDateAttendanceDialog(
    staffName: String,
    dateString: String,
    currentStatus: String?,
    currentNotes: String,
    onDismiss: () -> Unit,
    onSave: (status: String, notes: String) -> Unit,
    onClearAttendance: (() -> Unit)? = null
) {
    var status by remember { mutableStateOf(currentStatus ?: "PRESENT") }
    var notes by remember { mutableStateOf(currentNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "$dateString की हाजिरी / छुट्टी",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "कर्मचारी: $staffName",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                listOf(
                    "PRESENT" to "उपस्थित (Present)",
                    "HALF_DAY" to "आधा दिन (Half Day)",
                    "ABSENT" to "छुट्टी / नहीं आया (Absent)",
                    "PAID_LEAVE" to "सवेतन छुट्टी (Paid Leave)"
                ).forEach { (s, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { status = s }
                            .padding(vertical = 4.dp)
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = status == s,
                            onClick = { status = s }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, fontSize = 13.sp)
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("छुट्टी का कारण / नोट्स (उदा. बीमारी, गाँव आदि)") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (currentStatus != null && onClearAttendance != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onClearAttendance,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("❌ यह हाजिरी हटाएं (Clear Attendance / अचिह्नित करें)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(status, notes.trim()) }) {
                Text("हाजिरी दर्ज करें")
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
private fun RecordAdvanceWithDateDialog(
    staffName: String,
    currentMonthKey: String,
    onDismiss: () -> Unit,
    onSave: (amount: Double, type: String, dateMillis: Long, mode: String, notes: String) -> Unit
) {
    val sdfDisplay = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var dateStr by remember { mutableStateOf(sdfDisplay.format(Date())) }
    var amountStr by remember { mutableStateOf("") }
    var entryType by remember { mutableStateOf("ADVANCE") }
    var paymentMode by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "$staffName - एडवांस पैसा दर्ज करें",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text("एडवांस लेने की तारीख (Date):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { dateStr = sdfDisplay.format(Date()) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("आज (Today)", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, -1)
                            dateStr = sdfDisplay.format(cal.time)
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
                    label = { Text("तारीख (DD/MM/YYYY या YYYY-MM-DD) *") },
                    placeholder = { Text("उदा. 15/10/2026") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("रकम ₹ (Amount in Rupees) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
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
                    label = { Text("कारण / किस काम के लिए लिया (Reason)") },
                    placeholder = { Text("उदा. घर खर्च, दवाई, राशन आदि") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    val sdf1 = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val sdf2 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val d = try {
                        sdf1.parse(dateStr.trim()) ?: sdf2.parse(dateStr.trim()) ?: Date()
                    } catch (_: Exception) {
                        try { sdf2.parse(dateStr.trim()) ?: Date() } catch (_: Exception) { Date() }
                    }
                    if (amt > 0) {
                        onSave(amt, entryType, d.time, paymentMode, notes.trim())
                    }
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
private fun PdfSalaryPreviewModal(
    pdfFile: File,
    staffName: String,
    monthYear: String,
    onDismiss: () -> Unit,
    onOpenPdf: () -> Unit,
    onSharePdf: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Salary Slip PDF Ready",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "कर्मचारी: $staffName",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "माह: $monthYear",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "फ़ाइल: ${pdfFile.name} (${pdfFile.length() / 1024} KB)",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Text(
                    text = "इस PDF में हाजिरी, कुल छुट्टियाँ, तय पगार, प्रतिदिन दर, एडवांस की पूरी सूची तारीख सहित और बाकी भुगतान शामिल है।",
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenPdf,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF देखें")
                }

                Button(
                    onClick = onSharePdf,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp / शेयर")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("बंद करें")
            }
        }
    )
}

@Composable
private fun CalculationChip(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Text(text = title, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun CalculationRow(
    label: String,
    amount: String,
    color: Color = Color(0xFF1E293B),
    isBold: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF475569))
        Text(
            text = amount,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}

private fun shareStaffSalarySlip(
    context: Context,
    shopName: String,
    staff: StaffMember,
    monthYear: String,
    daysInMonth: Int,
    presentDays: Int,
    halfDays: Int,
    absentDays: Int,
    earnedSalary: Double,
    totalAdvance: Double,
    totalPaid: Double,
    netBalance: Double,
    advanceList: List<StaffPayment>
) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val slipText = buildString {
        append("🏢 *${shopName}*\n")
        append("📋 *कर्मचारी वेतन व हाजिरी स्लिप (Salary Slip)*\n")
        append("━━━━━━━━━━━━━━━━━━━━━\n")
        append("👤 *नाम:* ${staff.name}\n")
        append("💼 *पद:* ${staff.role}\n")
        append("📞 *मोबाइल:* ${staff.phone.ifBlank { "N/A" }}\n")
        append("📅 *माह:* $monthYear\n")
        append("━━━━━━━━━━━━━━━━━━━━━\n")
        append("📊 *हाजिरी विवरण:*\n")
        append("• कुल दिन: $daysInMonth दिन\n")
        append("• उपस्थिति (Present): $presentDays दिन\n")
        append("• आधा दिन (Half Day): $halfDays दिन\n")
        append("• कुल छुट्टियाँ (Leaves): $absentDays दिन\n\n")
        append("💰 *वेतन गणना:*\n")
        append("• तय मासिक वेतन: ₹${String.format(Locale.US, "%,.2f", staff.monthlySalary)}\n")
        append("• काम के अनुसार अर्जित: ₹${String.format(Locale.US, "%,.2f", earnedSalary)}\n")
        if (staff.openingBalance != 0.0) {
            append("• पिछला बकाया / पूर्व हिसाब: + ₹${String.format(Locale.US, "%,.2f", staff.openingBalance)}\n")
        }
        append("• कुल एडवांस लिया गया: - ₹${String.format(Locale.US, "%,.2f", totalAdvance)}\n")
        if (totalPaid > 0) {
            append("• पूर्व में दिया वेतन: - ₹${String.format(Locale.US, "%,.2f", totalPaid)}\n")
        }
        append("━━━━━━━━━━━━━━━━━━━━━\n")
        if (netBalance >= 0) {
            append("💵 *कुल बाकी देय वेतन: ₹${String.format(Locale.US, "%,.2f", netBalance)}*\n")
        } else {
            append("⚠️ *कर्मचारी पर बकाया एडवांस: ₹${String.format(Locale.US, "%,.2f", kotlin.math.abs(netBalance))}*\n")
        }

        if (advanceList.isNotEmpty()) {
            append("\n📝 *एडवांस की तारीखें:*\n")
            advanceList.forEach { adv ->
                append("• ${sdf.format(Date(adv.dateMillis))}: ₹${String.format(Locale.US, "%,.0f", adv.amount)} (${adv.paymentMode}) ${if (adv.notes.isNotBlank()) "- ${adv.notes}" else ""}\n")
            }
        }
        append("\n_Mourya Tiles Business Manager द्वारा निर्मित_")
    }

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, slipText)
        putExtra(Intent.EXTRA_SUBJECT, "Salary Slip - ${staff.name} ($monthYear)")
    }
    val chooser = Intent.createChooser(sendIntent, "Salary Slip शेयर करें via")
    context.startActivity(chooser)
}
