package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.ui.Screen
import com.example.ui.theme.getAppThemeOption
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Search
import com.example.data.model.OrderItemEntity
import com.example.data.model.OrderReturnExchangeLog
import com.example.data.model.OrderWithItems
import com.example.data.model.TileProduct
import com.example.ui.MainViewModel
import com.example.ui.components.ConfirmDeleteOrderDialog
import com.example.ui.components.PdfDisplaySwitchesCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess
import com.example.util.ShareHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val orderWithItemsFlow = remember(orderId) { viewModel.repository.getOrderWithItems(orderId) }
    val orderWithItems by orderWithItemsFlow.collectAsStateWithLifecycle(initialValue = null)
    val deliveryLogsFlow = remember(orderId) { viewModel.repository.getDeliveriesForOrder(orderId) }
    val deliveryLogs by deliveryLogsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val returnExchangeLogsFlow = remember(orderId) { viewModel.getReturnExchangeLogsForOrder(orderId) }
    val returnExchangeLogs by returnExchangeLogsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val products by viewModel.products.collectAsStateWithLifecycle()
    val settings by viewModel.shopSettings.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var showWhatsAppPhoneDialog by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var addPaymentAmount by remember { mutableStateOf("") }
    var addPaymentMode by remember { mutableStateOf("CASH") }
    var addPaymentNote by remember { mutableStateOf("") }

    // Partial / Staged Tile Delivery Tracker State
    var selectedDeliveryTab by remember { mutableStateOf(0) } // 0 = बाकी बची (Pending), 1 = ले गए व तारीख हिस्ट्री (Taken)
    var showPickupEntryDialog by remember { mutableStateOf(false) }
    var showMarkAllDeliveredConfirm by remember { mutableStateOf(false) }

    var returningItem by remember { mutableStateOf<OrderItemEntity?>(null) }
    var returnBoxesStr by remember { mutableStateOf("") }
    var returnLooseStr by remember { mutableStateOf("") }
    var returnReasonStr by remember { mutableStateOf("") }

    if (orderWithItems == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(color = Navy900)
        }
        return
    }

    val order = orderWithItems!!.order
    val items = orderWithItems!!.items
    val isConfirmed = order.status == "CONFIRMED"
    val activeTheme = getAppThemeOption(settings.appTheme)

    val paymentInstallments = remember(order.paymentHistoryJson) {
        val list = mutableListOf<Pair<Double, Triple<String, String, String>>>()
        try {
            if (order.paymentHistoryJson.isNotBlank()) {
                val pArr = org.json.JSONArray(order.paymentHistoryJson)
                for (i in 0 until pArr.length()) {
                    val pObj = pArr.getJSONObject(i)
                    val pAmt = pObj.optDouble("amount", 0.0)
                    val pDate = pObj.optString("date", "")
                    val pMode = pObj.optString("mode", "")
                    val pNote = pObj.optString("note", "")
                    list.add(Pair(pAmt, Triple(pDate, pMode, pNote)))
                }
            }
        } catch (_: Exception) {}
        list
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = activeTheme.primary,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = order.invoiceNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = PureWhite
                        )
                        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        Text(
                            text = sdf.format(Date(order.createdAt)),
                            fontSize = 11.sp,
                            color = Color(0xFF89C2D9)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.EditOrder(order.id)) },
                        modifier = Modifier.testTag("action_edit_order")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Order",
                            tint = PureWhite
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Order",
                            tint = Color(0xFFFCA5A5)
                        )
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
            // 1. Status & Customer Details Card
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
                            StatusBadge(status = order.status)
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", order.grandTotal)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = order.customerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Navy900
                        )
                        if (order.customerPhone.isNotBlank()) {
                            Text(
                                text = "Mobile: ${order.customerPhone}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (order.customerAddress.isNotBlank()) {
                            Text(
                                text = "Address: ${order.customerAddress}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // If order is Pending, provide direct "Confirm Order" button!
                        if (!isConfirmed) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.confirmPendingOrder(order.id) { success, _ ->
                                        // Status updates automatically via Flow
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_pending_order_button")
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = PureWhite)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Confirm Sale & Deduct Warehouse Stock", fontWeight = FontWeight.Bold, color = PureWhite)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.navigateTo(Screen.EditOrder(order.id))
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("detail_edit_order_button")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Order (टाइल, रेट या मात्रा बदलें)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Payment & Balance Due Card (किश्त जमा व बकाया स्थिति)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (order.balanceAmount > 0.01) Color(0xFFFEF2F2) else Color(0xFFF0FDF4)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💰 भुगतान व बकाया स्थिति (Payments)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (order.balanceAmount > 0.01) Color(0xFF991B1B) else Color(0xFF166534)
                            )
                            if (order.balanceAmount > 0.01) {
                                Surface(
                                    color = Color(0xFFFEE2E2),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "उधार / बकाया",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFFDC2626),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "✔ चुकता (Full Paid)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF16A34A),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("कुल बिल राशि", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("₹${String.format(Locale.US, "%.2f", order.grandTotal)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("जमा राशि (Paid)", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("₹${String.format(Locale.US, "%.2f", order.paidAmount)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF16A34A))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("शेष बकाया (Due)", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", order.balanceAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (order.balanceAmount > 0.01) Color(0xFFDC2626) else Color(0xFF16A34A)
                                )
                            }
                        }

                        // Add Payment button (even if balance is 0, user can add more or take advance)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                addPaymentAmount = if (order.balanceAmount > 0) String.format(Locale.US, "%.0f", order.balanceAmount) else ""
                                addPaymentMode = "CASH"
                                addPaymentNote = ""
                                showAddPaymentDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (order.balanceAmount > 0.01) Color(0xFFDC2626) else activeTheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("button_add_order_payment")
                        ) {
                            Text(
                                text = if (order.balanceAmount > 0.01) "💰 बकाया राशि जमा करें (Add Payment / किश्त जमा)" else "💰 अतिरिक्त राशि जमा करें (Add Payment)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = PureWhite
                            )
                        }

                        // Display Installment History if exists
                        if (paymentInstallments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "📜 जमा किश्तों का इतिहास (${paymentInstallments.size} Entries):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            paymentInstallments.forEach { (pAmt, trip) ->
                                val (pDate, pMode, pNote) = trip
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Column {
                                            Text(text = "₹${pAmt.toInt()} via $pMode", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF166534))
                                            if (pNote.isNotBlank()) {
                                                Text(text = pNote, fontSize = 10.sp, color = Color(0xFF64748B))
                                            }
                                        }
                                        Text(text = pDate, fontSize = 10.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 1.5. Partial / Staged Tile Delivery Tracker (लिखवाई थी vs ले गए तारीख-वार vs बाकी बची)
            item {
                val pendingItems = items.filter { it.pendingTotalPieces > 0 }
                val takenItems = items.filter { it.deliveredTotalPieces > 0 }
                val totalBookedBoxes = items.sumOf { it.fullBoxes }
                val totalBookedLoose = items.sumOf { it.loosePieces }
                val totalTakenBoxes = items.sumOf { it.deliveredTotalPieces / it.piecesPerBox.coerceAtLeast(1) }
                val totalTakenLoose = items.sumOf { it.deliveredTotalPieces % it.piecesPerBox.coerceAtLeast(1) }
                val totalPendingBoxes = items.sumOf { it.pendingFullBoxes }
                val totalPendingLoose = items.sumOf { it.pendingLoosePieces }
                val hasPending = pendingItems.isNotEmpty()

                val (delivBadgeText, delivBadgeBg, delivBadgeFg) = when (order.deliveryStatus) {
                    "NOT_DELIVERED" -> Triple("📦 होल्ड (बाकी)", Color(0xFFFEE2E2), Color(0xFFDC2626))
                    "PARTIAL_PENDING" -> Triple("🚚 आंशिक बाकी", Color(0xFFFFEDD5), Color(0xFFC2410C))
                    else -> Triple("✅ पूरी डिलीवरी", Color(0xFFDCFCE7), Color(0xFF15803D))
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasPending) Color(0xFFFFFBEB) else Color(0xFFF0FDF4)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (hasPending) Color(0xFFFDE68A) else Color(0xFFBBF7D0)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🚚 टाइल डिलीवरी व बाकी हिसाब",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (hasPending) Color(0xFF9A3412) else Color(0xFF166534)
                            )
                            Surface(color = delivBadgeBg, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = delivBadgeText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    color = delivBadgeFg,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3-Column Booked / Taken / Pending Summary
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text("कुल लिखवाई थी", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(
                                        text = "${totalBookedBoxes} Bx" + if (totalBookedLoose > 0) " + ${totalBookedLoose}P" else "",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Navy900
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("ले जा चुके (Taken)", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(
                                        text = "${totalTakenBoxes} Bx" + if (totalTakenLoose > 0) " + ${totalTakenLoose}P" else "",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("बाकी बची (Pending)", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(
                                        text = "${totalPendingBoxes} Bx" + if (totalPendingLoose > 0) " + ${totalPendingLoose}P" else "",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = if (hasPending) Color(0xFFDC2626) else Color(0xFF16A34A)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2 Clear Separation Tabs: Pending (नहीं ले गए) vs Taken + History (ले गए)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedDeliveryTab == 0) Color(0xFFEA580C) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selectedDeliveryTab == 0) Color(0xFFEA580C) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .testTag("tab_delivery_pending")
                            ) {
                                TextButton(
                                    onClick = { selectedDeliveryTab = 0 },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "⏳ बाकी बची (${pendingItems.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = if (selectedDeliveryTab == 0) PureWhite else Color(0xFF334155)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedDeliveryTab == 1) Color(0xFF15803D) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selectedDeliveryTab == 1) Color(0xFF15803D) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .testTag("tab_delivery_taken")
                            ) {
                                TextButton(
                                    onClick = { selectedDeliveryTab = 1 },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "✅ ले गए व हिस्ट्री (${deliveryLogs.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = if (selectedDeliveryTab == 1) PureWhite else Color(0xFF334155)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (selectedDeliveryTab == 0) {
                            // TAB 0: PENDING (जो टाइल अभी नहीं ले गए)
                            if (pendingItems.isEmpty()) {
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "✅ इस ऑर्डर की सभी टाइल्स ग्राहक ले जा चुके हैं — कोई टाइल बाकी नहीं है!",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF166534),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            } else {
                                pendingItems.forEach { pItem ->
                                    val takenBx = pItem.deliveredTotalPieces / pItem.piecesPerBox.coerceAtLeast(1)
                                    val takenPcs = pItem.deliveredTotalPieces % pItem.piecesPerBox.coerceAtLeast(1)
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "[${pItem.tileCode}] ${pItem.tileName}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Navy900
                                                )
                                                Text(
                                                    text = "लिखवाई: ${pItem.fullBoxes} Bx" + (if (pItem.loosePieces > 0) "+${pItem.loosePieces}P" else "") +
                                                            "  •  ले गए: ${takenBx} Bx" + (if (takenPcs > 0) "+${takenPcs}P" else ""),
                                                    fontSize = 10.5.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                            Surface(
                                                color = Color(0xFFFEE2E2),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "बाकी: ${pItem.pendingFullBoxes} Bx" + if (pItem.pendingLoosePieces > 0) "+${pItem.pendingLoosePieces}P" else "",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp,
                                                    color = Color(0xFFDC2626),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { showPickupEntryDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("button_record_partial_delivery")
                                ) {
                                    Text(
                                        text = "➕ नई टाइल डिलीवरी दर्ज करें (Pickup Entry)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PureWhite
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedButton(
                                    onClick = { showMarkAllDeliveredConfirm = true },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("button_mark_all_delivered")
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "✔ सभी बाकी एक साथ डिलीवर करें",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        } else {
                            // TAB 1: TAKEN + DATE-WISE HISTORY (जो ले गए + तारीख व समय)
                            if (takenItems.isEmpty() && deliveryLogs.isEmpty()) {
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "अभी तक इस ऑर्डर से कोई भी टाइल ग्राहक नहीं ले गए हैं।",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            } else {
                                if (takenItems.isNotEmpty()) {
                                    Text(
                                        text = "✅ अब तक ले जाई गई टाइल्स (Item Summary):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF166534)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    takenItems.forEach { tItem ->
                                        val tBx = tItem.deliveredTotalPieces / tItem.piecesPerBox.coerceAtLeast(1)
                                        val tPcs = tItem.deliveredTotalPieces % tItem.piecesPerBox.coerceAtLeast(1)
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "[${tItem.tileCode}] ${tItem.tileName}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = Navy900
                                                    )
                                                    Text(
                                                        text = "कुल लिखवाई थी: ${tItem.fullBoxes} Bx" + if (tItem.loosePieces > 0) "+${tItem.loosePieces}P" else "",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                                Surface(
                                                    color = Color(0xFFDCFCE7),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "ले गए: ${tBx} Bx" + if (tPcs > 0) "+${tPcs}P" else "",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF15803D),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "📅 तारीख-वार डिलीवरी हिस्ट्री (${deliveryLogs.size} Entries):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569)
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                if (deliveryLogs.isEmpty()) {
                                    Text(
                                        text = "बिल बनाते समय पूरी टाइल एक साथ डिलीवर की गई थी।",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                } else {
                                    val logSdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                    deliveryLogs.forEach { log ->
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "📅 ${logSdf.format(Date(log.deliveredAt))}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.5.sp,
                                                        color = activeTheme.primary
                                                    )
                                                    Text(
                                                        text = "[${log.tileCode}] ${log.tileName} — ${log.boxesDelivered} Bx" +
                                                                if (log.piecesDelivered > 0) " + ${log.piecesDelivered} Pcs" else "",
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.sp,
                                                        color = Navy900
                                                    )
                                                    val meta = buildList {
                                                        if (log.receiverName.isNotBlank()) add("रिसीवर: ${log.receiverName}")
                                                        if (log.notes.isNotBlank()) add("नोट: ${log.notes}")
                                                    }.joinToString(" • ")
                                                    if (meta.isNotBlank()) {
                                                        Text(
                                                            text = meta,
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteDeliveryLogEntry(log.id) },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Undo Delivery Entry",
                                                        tint = StatusError,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // WhatsApp / Text Share of Delivery Account
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val sdfShort = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                val sb = StringBuilder()
                                sb.append("🚚 *टाइल डिलीवरी हिसाब (${settings.shopName.ifBlank { "TILES & MARBLES" }})*\n")
                                sb.append("बिल क्र.: #${order.invoiceNumber}\n")
                                sb.append("ग्राहक: ${order.customerName}\n")
                                sb.append("━━━━━━━━━━━━━━━━━━\n")
                                sb.append("📦 *कुल लिखवाई:* ${totalBookedBoxes} Box" + (if (totalBookedLoose > 0) " + ${totalBookedLoose} Pcs" else "") + "\n")
                                sb.append("✅ *ले जा चुके:* ${totalTakenBoxes} Box" + (if (totalTakenLoose > 0) " + ${totalTakenLoose} Pcs" else "") + "\n")
                                sb.append("⏳ *बाकी बची:* ${totalPendingBoxes} Box" + (if (totalPendingLoose > 0) " + ${totalPendingLoose} Pcs" else "") + "\n")

                                if (pendingItems.isNotEmpty()) {
                                    sb.append("\n⏳ *बाकी बची टाइल्स:*\n")
                                    pendingItems.forEach { p ->
                                        sb.append("• [${p.tileCode}] ${p.tileName}: ${p.pendingFullBoxes} Bx" + (if (p.pendingLoosePieces > 0) "+${p.pendingLoosePieces}P" else "") + " बाकी (कुल ${p.fullBoxes} Bx)\n")
                                    }
                                }
                                if (deliveryLogs.isNotEmpty()) {
                                    sb.append("\n📅 *तारीख-वार ले जाई गई हिस्ट्री:*\n")
                                    deliveryLogs.forEach { l ->
                                        val rec = if (l.receiverName.isNotBlank()) " (${l.receiverName})" else ""
                                        sb.append("• ${sdfShort.format(Date(l.deliveredAt))}: ${l.tileName} — ${l.boxesDelivered} Bx" + (if (l.piecesDelivered > 0) "+${l.piecesDelivered}P" else "") + "$rec\n")
                                    }
                                }
                                sb.append("\n💰 बिल राशि: ₹${order.grandTotal.toInt()} | जमा: ₹${order.paidAmount.toInt()} | बकाया: ₹${order.balanceAmount.toInt()}")

                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "डिलीवरी हिसाब भेजें"))
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("button_share_delivery_summary")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = activeTheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "📲 डिलीवरी हिसाब WhatsApp / शेयर करें",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = activeTheme.primary
                            )
                        }
                    }
                }
            }

            // 1.75 Return / Exchange / Refund / Hold History Card
            if (returnExchangeLogs.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD8B4FE)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "↩️ वापसी व बदली हिसाब (Returns & Exchanges)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF6B21A8)
                                )
                                Surface(color = Color(0xFFF3E8FF), shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = "${returnExchangeLogs.size} रिकॉर्ड",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF7E22CE),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            val retSdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
                            returnExchangeLogs.forEach { rLog ->
                                val (outcomeLabel, outcomeBg, outcomeFg) = when (rLog.outcomeType) {
                                    "REFUND_CASH" -> Triple("💵 नकद पैसे वापस", Color(0xFFFEE2E2), Color(0xFFDC2626))
                                    "REFUND_BALANCE" -> Triple("📉 बिल में घटाया", Color(0xFFE0F2FE), Color(0xFF0369A1))
                                    "EXCHANGE_TILE" -> Triple("🔄 बदली दूसरी टाइल", Color(0xFFDCFCE7), Color(0xFF15803D))
                                    "EXCHANGE_OTHER" -> Triple("🪨 बदली अन्य सामान", Color(0xFFFFEDD5), Color(0xFFC2410C))
                                    else -> Triple(rLog.outcomeType, Color(0xFFF1F5F9), Color(0xFF334155))
                                }

                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "📅 ${retSdf.format(Date(rLog.createdAt))}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B)
                                            )
                                            Surface(color = outcomeBg, shape = RoundedCornerShape(5.dp)) {
                                                Text(
                                                    text = outcomeLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = outcomeFg,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "वापस आई: ${rLog.returnedTileName} — ${rLog.returnedBoxes} Bx" +
                                                    (if (rLog.returnedPieces > 0) " + ${rLog.returnedPieces} Pcs" else "") +
                                                    " (₹${String.format(Locale.US, "%.0f", rLog.returnedAmount)})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF991B1B)
                                        )

                                        if (rLog.outcomeType == "EXCHANGE_TILE" || rLog.outcomeType == "EXCHANGE_OTHER") {
                                            val isTaken = rLog.replacementDeliveryStatus == "TAKEN"
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "बदले में: ${rLog.replacementItemName}" +
                                                            (if (rLog.replacementBoxes > 0 || rLog.replacementPieces > 0) " (${rLog.replacementBoxes} Bx" + (if (rLog.replacementPieces > 0) "+${rLog.replacementPieces}P" else "") + ")" else "") +
                                                            " • ₹${String.format(Locale.US, "%.0f", rLog.replacementAmount)}",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.5.sp,
                                                    color = Navy900,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = if (isTaken) Color(0xFFDCFCE7) else Color(0xFFFFEDD5),
                                                    shape = RoundedCornerShape(5.dp)
                                                ) {
                                                    Text(
                                                        text = if (isTaken) "✅ ले गए" else "📦 होल्ड",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isTaken) Color(0xFF15803D) else Color(0xFFC2410C),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            val diffText = when {
                                                rLog.netAmountDifference > 0.01 -> "अंतर: ग्राहक से ₹${String.format(Locale.US, "%.0f", rLog.netAmountDifference)} अतिरिक्त जुड़े"
                                                rLog.netAmountDifference < -0.01 -> "अंतर: ग्राहक के ₹${String.format(Locale.US, "%.0f", kotlin.math.abs(rLog.netAmountDifference))} कम हुए"
                                                else -> "अंतर: ₹0 (बराबर बदली)"
                                            }
                                            Text(text = diffText, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                                        }

                                        if (rLog.notes.isNotBlank()) {
                                            Text(text = "नोट: ${rLog.notes}", fontSize = 10.sp, color = Color(0xFF64748B))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Per-Order PDF Customization Switches (Price, Size-wise Box Totals, Delivery Status)
            item {
                PdfDisplaySwitchesCard(
                    showPrice = order.showPriceOnPdf,
                    onTogglePrice = { newSetting ->
                        viewModel.toggleShowPriceOnPdf(order.id, newSetting)
                    },
                    showSizeTotal = order.showSizeTotalOnPdf,
                    onToggleSizeTotal = { newSetting ->
                        viewModel.toggleShowSizeTotalOnPdf(order.id, newSetting)
                    },
                    showDeliveryStatus = order.showDeliveryStatusOnPdf,
                    onToggleDeliveryStatus = { newSetting ->
                        viewModel.toggleShowDeliveryStatusOnPdf(order.id, newSetting)
                    }
                )
            }

            // 3. Share & PDF Actions
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Customer PDF Bill & Sharing",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // WhatsApp Share
                            Button(
                                onClick = {
                                    isGeneratingPdf = true
                                    viewModel.generateOrderPdf(
                                        context = context,
                                        orderId = order.id,
                                        onGenerated = { file ->
                                            isGeneratingPdf = false
                                            generatedPdfFile = file
                                            showWhatsAppPhoneDialog = true
                                        },
                                        onError = {
                                            isGeneratingPdf = false
                                            viewModel.showMessage(it)
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_share_whatsapp")
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            // General Share
                            Button(
                                onClick = {
                                    isGeneratingPdf = true
                                    viewModel.generateOrderPdf(
                                        context = context,
                                        orderId = order.id,
                                        onGenerated = { file ->
                                            isGeneratingPdf = false
                                            val msg = ShareHelper.buildShareMessage(settings, order)
                                            ShareHelper.sharePdfGeneral(context, file, msg)
                                        },
                                        onError = {
                                            isGeneratingPdf = false
                                            viewModel.showMessage(it)
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_share_general")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share PDF", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            // View / Print PDF
                            OutlinedButton(
                                onClick = {
                                    isGeneratingPdf = true
                                    viewModel.generateOrderPdf(
                                        context = context,
                                        orderId = order.id,
                                        onGenerated = { file ->
                                            isGeneratingPdf = false
                                            ShareHelper.viewOrPrintPdf(context, file)
                                        },
                                        onError = {
                                            isGeneratingPdf = false
                                            viewModel.showMessage(it)
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview", fontSize = 12.sp)
                            }
                        }

                        if (isGeneratingPdf) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generating Offline PDF Bill with Tile Photos...", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 4. Tile Products in this Order
            item {
                Text(
                    text = "Ordered Tiles (${items.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(items) { item ->
                OrderItemDetailCard(
                    item = item,
                    showPrice = order.showPriceOnPdf,
                    isConfirmedOrder = isConfirmed,
                    onReturnClick = {
                        returningItem = item
                        returnBoxesStr = ""
                        returnLooseStr = ""
                        returnReasonStr = ""
                    }
                )
            }

            // 5. Financial Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Subtotal", fontSize = 13.sp)
                            Text("₹${String.format(Locale.US, "%.2f", order.subtotal)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (order.discount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Discount", fontSize = 13.sp, color = StatusError)
                                Text("-₹${String.format(Locale.US, "%.2f", order.discount)}", fontSize = 13.sp, color = StatusError, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text("Grand Total", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", order.grandTotal)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (order.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Notes: ${order.notes}", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Delete Confirmation Dialog with stock restore prompt
    if (showDeleteDialog) {
        ConfirmDeleteOrderDialog(
            isConfirmed = isConfirmed,
            onDismiss = { showDeleteDialog = false },
            onConfirmDelete = { restoreStock ->
                viewModel.deleteOrder(order.id, restoreStock) {
                    showDeleteDialog = false
                    onNavigateBack()
                }
            }
        )
    }

    // WhatsApp Send Dialog with validated phone number
    if (showWhatsAppPhoneDialog && generatedPdfFile != null) {
        var recipientPhone by remember { mutableStateOf(order.customerPhone) }
        var phoneValidationError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showWhatsAppPhoneDialog = false },
            title = { Text("Send Bill via WhatsApp", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter or verify the customer's 10-digit Indian mobile number to attach and send the bill:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = recipientPhone,
                        onValueChange = {
                            recipientPhone = it
                            phoneValidationError = null
                        },
                        label = { Text("Customer Mobile (10 digits)") },
                        placeholder = { Text("9876543210") },
                        isError = phoneValidationError != null,
                        supportingText = {
                            if (phoneValidationError != null) {
                                Text(phoneValidationError!!, color = StatusError, fontSize = 11.sp)
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val (isValid, formattedPhone) = ShareHelper.validateIndianMobileNumber(recipientPhone)
                        if (!isValid) {
                            phoneValidationError = formattedPhone
                            return@Button
                        }
                        val msg = ShareHelper.buildShareMessage(settings, order)
                        val sent = ShareHelper.sharePdfViaWhatsApp(
                            context = context,
                            pdfFile = generatedPdfFile!!,
                            targetPhoneFormatted = formattedPhone,
                            messageText = msg
                        )
                        if (sent) {
                            showWhatsAppPhoneDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Text("Send to WhatsApp", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showWhatsAppPhoneDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showAddPaymentDialog = false },
            title = {
                Text(
                    text = "💰 बकाया राशि / किश्त जमा करें",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = activeTheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "बिल क्र.: #${order.invoiceNumber} • ग्राहक: ${order.customerName}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "वर्तमान शेष बकाया: ₹${String.format(Locale.US, "%.2f", order.balanceAmount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (order.balanceAmount > 0.01) Color(0xFFDC2626) else Color(0xFF16A34A)
                    )

                    OutlinedTextField(
                        value = addPaymentAmount,
                        onValueChange = { addPaymentAmount = it },
                        label = { Text("जमा की जाने वाली राशि (Amount ₹)") },
                        placeholder = { Text("e.g. 5000") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Mode selection chips
                    Text("भुगतान माध्यम (Payment Mode):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("CASH", "UPI", "BANK", "CHEQUE").forEach { mode ->
                            androidx.compose.material3.FilterChip(
                                selected = addPaymentMode == mode,
                                onClick = { addPaymentMode = mode },
                                label = { Text(mode, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = addPaymentNote,
                        onValueChange = { addPaymentNote = it },
                        label = { Text("नोट / विवरण (Optional)") },
                        placeholder = { Text("उदा. PhonePe / नकद दिया") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = addPaymentAmount.toDoubleOrNull() ?: 0.0
                        if (amt <= 0) {
                            Toast.makeText(context, "कृपया वैध राशि दर्ज करें", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addOrderPayment(
                            orderId = order.id,
                            additionalAmount = amt,
                            paymentMode = addPaymentMode,
                            note = addPaymentNote,
                            onSuccess = {
                                showAddPaymentDialog = false
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary)
                ) {
                    Text("रुपये जमा करें (Save)", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddPaymentDialog = false }) {
                    Text("रद्द करें (Cancel)")
                }
            }
        )
    }

    if (returningItem != null) {
        val targetItem = returningItem!!
        val pieceRate = if (targetItem.piecesPerBox > 0) targetItem.pricePerBox / targetItem.piecesPerBox else 0.0
        val retB = returnBoxesStr.toIntOrNull() ?: 0
        val retL = returnLooseStr.toIntOrNull() ?: 0
        val calculatedRefund = (retB * targetItem.pricePerBox) + (retL * pieceRate)
        val maxAvailablePieces = targetItem.totalPieces

        var outcomeType by remember(targetItem.id) { mutableStateOf("REFUND_BALANCE") }
        var replacementSearch by remember(targetItem.id) { mutableStateOf("") }
        var selectedReplacementProduct by remember(targetItem.id) { mutableStateOf<TileProduct?>(null) }
        var replBoxesStr by remember(targetItem.id) { mutableStateOf("1") }
        var replLooseStr by remember(targetItem.id) { mutableStateOf("0") }
        var replRateStr by remember(targetItem.id) { mutableStateOf("") }
        var replCustomName by remember(targetItem.id) { mutableStateOf("") }
        var replCustomSize by remember(targetItem.id) { mutableStateOf("") }
        var replCustomQtyStr by remember(targetItem.id) { mutableStateOf("1") }
        var replCustomTotalStr by remember(targetItem.id) { mutableStateOf("") }
        var replDeliveryStatus by remember(targetItem.id) { mutableStateOf("TAKEN") } // "TAKEN" or "HOLD"

        val replB = replBoxesStr.toIntOrNull() ?: 0
        val replL = replLooseStr.toIntOrNull() ?: 0
        val replRate = replRateStr.toDoubleOrNull() ?: (selectedReplacementProduct?.defaultSellingPrice ?: 0.0)
        val replPpb = (selectedReplacementProduct?.piecesPerBox ?: 1).coerceAtLeast(1)
        val calculatedReplacementAmt = when (outcomeType) {
            "EXCHANGE_TILE" -> (replB * replRate) + (replL * (replRate / replPpb))
            "EXCHANGE_OTHER" -> replCustomTotalStr.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val netDiff = calculatedReplacementAmt - calculatedRefund

        val filteredReplacementProducts = remember(products, replacementSearch) {
            products.filter {
                replacementSearch.isBlank() ||
                        it.name.contains(replacementSearch, ignoreCase = true) ||
                        it.code.contains(replacementSearch, ignoreCase = true) ||
                        it.size.contains(replacementSearch, ignoreCase = true)
            }.take(8)
        }

        AlertDialog(
            onDismissRequest = { returningItem = null },
            title = {
                Text(
                    text = "↩️ वापसी व बदली (Return / Exchange)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(460.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "वापस आने वाला आइटम: ${targetItem.tileName} (${targetItem.tileCode})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = Navy900
                    )
                    Text(
                        text = "बिल में मात्रा: ${targetItem.fullBoxes} Bx" + (if (targetItem.loosePieces > 0) " + ${targetItem.loosePieces} Pcs" else "") + " (कुल $maxAvailablePieces पीस)",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = returnBoxesStr,
                            onValueChange = { returnBoxesStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text("वापस पेटी (Boxes)") },
                            placeholder = { Text("0") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = returnLooseStr,
                            onValueChange = { returnLooseStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text("खुले पीस (Loose)") },
                            placeholder = { Text("0") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (calculatedRefund > 0) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "वापस आई पेटी का मूल्य: ₹${String.format(Locale.US, "%.2f", calculatedRefund)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = "ग्राहक ने इसके बदले क्या लिया? (Select Action):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = Navy900
                    )

                    // 4 Outcome Chips in 2 clean rows
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.material3.FilterChip(
                            selected = outcomeType == "REFUND_BALANCE",
                            onClick = { outcomeType = "REFUND_BALANCE" },
                            label = { Text("📉 बिल में घटाएं", fontSize = 10.5.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.FilterChip(
                            selected = outcomeType == "REFUND_CASH",
                            onClick = { outcomeType = "REFUND_CASH" },
                            label = { Text("💵 नकद वापस", fontSize = 10.5.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.material3.FilterChip(
                            selected = outcomeType == "EXCHANGE_TILE",
                            onClick = { outcomeType = "EXCHANGE_TILE" },
                            label = { Text("🔄 दूसरी टाइल", fontSize = 10.5.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.FilterChip(
                            selected = outcomeType == "EXCHANGE_OTHER",
                            onClick = { outcomeType = "EXCHANGE_OTHER" },
                            label = { Text("🪨 अन्य सामान", fontSize = 10.5.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // EXCHANGE WITH ANOTHER TILE FROM INVENTORY
                    if (outcomeType == "EXCHANGE_TILE") {
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🔄 बदले में कौन सी टाइल दी?", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF166534))

                                OutlinedTextField(
                                    value = replacementSearch,
                                    onValueChange = { replacementSearch = it },
                                    placeholder = { Text("टाइल खोजें (नाम / कोड / साइज़)...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                filteredReplacementProducts.forEach { prod ->
                                    val isSel = selectedReplacementProduct?.id == prod.id
                                    Surface(
                                        color = if (isSel) Color(0xFFDCFCE7) else Color.White,
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSel) Color(0xFF16A34A) else Color(0xFFE2E8F0)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedReplacementProduct = prod
                                                replRateStr = String.format(Locale.US, "%.0f", prod.defaultSellingPrice)
                                            }
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                                Text("${prod.size} • स्टॉक: ${prod.stockDisplay}", fontSize = 10.sp, color = Color(0xFF64748B))
                                            }
                                            Text("₹${prod.defaultSellingPrice.toInt()}/Bx", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Navy900)
                                        }
                                    }
                                }

                                if (selectedReplacementProduct != null) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = replBoxesStr,
                                            onValueChange = { replBoxesStr = it.filter { c -> c.isDigit() } },
                                            label = { Text("बदली बॉक्स", fontSize = 10.sp) },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = replLooseStr,
                                            onValueChange = { replLooseStr = it.filter { c -> c.isDigit() } },
                                            label = { Text("खुले पीस", fontSize = 10.sp) },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(0.9f)
                                        )
                                        OutlinedTextField(
                                            value = replRateStr,
                                            onValueChange = { replRateStr = it.filter { c -> c.isDigit() || c == '.' } },
                                            label = { Text("रेट ₹/Bx", fontSize = 10.sp) },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                            singleLine = true,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1.1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // EXCHANGE WITH OTHER ITEM / MARBLE / CHEMICAL
                    if (outcomeType == "EXCHANGE_OTHER") {
                        Surface(
                            color = Color(0xFFFFF7ED),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🪨 बदले में दिया गया अन्य सामान / मार्बल:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFFC2410C))
                                OutlinedTextField(
                                    value = replCustomName,
                                    onValueChange = { replCustomName = it },
                                    label = { Text("सामान का नाम *") },
                                    placeholder = { Text("उदा. Black Granite / Chemical / Grout") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = replCustomSize,
                                        onValueChange = { replCustomSize = it },
                                        label = { Text("साइज़ / माप") },
                                        placeholder = { Text("उदा. 20 Kg") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = replCustomQtyStr,
                                        onValueChange = { replCustomQtyStr = it.filter { c -> c.isDigit() } },
                                        label = { Text("मात्रा (Qty)") },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(0.7f)
                                    )
                                }
                                OutlinedTextField(
                                    value = replCustomTotalStr,
                                    onValueChange = { replCustomTotalStr = it.filter { c -> c.isDigit() || c == '.' } },
                                    label = { Text("बदले सामान की कुल कीमत (₹) *") },
                                    placeholder = { Text("0") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // DELIVERY STATUS FOR REPLACEMENT ITEM (TAKEN NOW vs HOLD)
                    if (outcomeType == "EXCHANGE_TILE" || outcomeType == "EXCHANGE_OTHER") {
                        Text("🚚 बदली वाला सामान अभी ले गए या होल्ड है?", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Navy900)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (replDeliveryStatus == "TAKEN") Color(0xFF16A34A) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (replDeliveryStatus == "TAKEN") Color(0xFF16A34A) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { replDeliveryStatus = "TAKEN" }
                            ) {
                                Text(
                                    text = "✅ अभी ले गए (Taken)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (replDeliveryStatus == "TAKEN") PureWhite else Navy900,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (replDeliveryStatus == "HOLD") Color(0xFFEA580C) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (replDeliveryStatus == "HOLD") Color(0xFFEA580C) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { replDeliveryStatus = "HOLD" }
                            ) {
                                Text(
                                    text = "📦 होल्ड / बाद में (Hold)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (replDeliveryStatus == "HOLD") PureWhite else Navy900,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // Net Price Difference Summary
                        Surface(
                            color = if (netDiff > 0.01) Color(0xFFFFF7ED) else Color(0xFFECFDF5),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = when {
                                    netDiff > 0.01 -> "💰 हिसाब अंतर: ग्राहक से ₹${String.format(Locale.US, "%.2f", netDiff)} और लेने हैं (बिल में जुड़ेंगे)"
                                    netDiff < -0.01 -> "💰 हिसाब अंतर: ग्राहक के ₹${String.format(Locale.US, "%.2f", kotlin.math.abs(netDiff))} कम हुए (बिल से घटेंगे)"
                                    else -> "💰 हिसाब अंतर: ₹0.00 (बराबर बदली)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = if (netDiff > 0.01) Color(0xFF9A3412) else Color(0xFF065F46),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = returnReasonStr,
                        onValueChange = { returnReasonStr = it },
                        label = { Text("कारण / नोट्स (Optional)") },
                        placeholder = { Text("उदा. 2 बॉक्स बचे थे, बदले में फ्लोर टाइल लिखवाई") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val boxes = returnBoxesStr.toIntOrNull() ?: 0
                        val loose = returnLooseStr.toIntOrNull() ?: 0
                        val returnTotalPieces = (boxes * (if (targetItem.piecesPerBox > 0) targetItem.piecesPerBox else 1)) + loose
                        if (returnTotalPieces <= 0) {
                            Toast.makeText(context, "कृपया वापसी की मात्रा (Boxes या Pcs) दर्ज करें", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (returnTotalPieces > maxAvailablePieces) {
                            Toast.makeText(context, "वापसी मात्रा बेचे गए कुल पीस ($maxAvailablePieces) से ज्यादा नहीं हो सकती!", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (outcomeType == "EXCHANGE_TILE" && selectedReplacementProduct == null) {
                            Toast.makeText(context, "कृपया बदले में दी जाने वाली टाइल चुनें", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (outcomeType == "EXCHANGE_OTHER" && replCustomName.isBlank()) {
                            Toast.makeText(context, "कृपया बदले सामान का नाम दर्ज करें", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        viewModel.processReturnOrExchange(
                            orderId = order.id,
                            orderItemId = targetItem.id,
                            returnBoxes = boxes,
                            returnLoose = loose,
                            outcomeType = outcomeType,
                            replacementProductId = selectedReplacementProduct?.id,
                            replacementCustomName = replCustomName.trim(),
                            replacementCustomSize = replCustomSize.trim(),
                            replacementBoxes = if (outcomeType == "EXCHANGE_OTHER") (replCustomQtyStr.toIntOrNull() ?: 1) else replB,
                            replacementLoose = if (outcomeType == "EXCHANGE_TILE") replL else 0,
                            replacementRatePerBox = replRate,
                            replacementCustomTotal = replCustomTotalStr.toDoubleOrNull() ?: 0.0,
                            replacementDeliveryStatus = if (outcomeType == "EXCHANGE_TILE" || outcomeType == "EXCHANGE_OTHER") replDeliveryStatus else "NONE",
                            notes = returnReasonStr.trim(),
                            onSuccess = {
                                returningItem = null
                            },
                            onError = { err ->
                                Toast.makeText(context, "त्रुटि: $err", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("पुष्टि करें (Confirm)", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { returningItem = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Pickup Entry Dialog (Record Partial / Staged Tile Delivery with Date Picker)
    if (showPickupEntryDialog) {
        val pendingItemsForDialog = items.filter { it.pendingTotalPieces > 0 }
        val boxesInputs = remember(showPickupEntryDialog, pendingItemsForDialog.size) {
            androidx.compose.runtime.mutableStateMapOf<Long, String>().apply {
                pendingItemsForDialog.forEach { put(it.id, "") }
            }
        }
        val looseInputs = remember(showPickupEntryDialog, pendingItemsForDialog.size) {
            androidx.compose.runtime.mutableStateMapOf<Long, String>().apply {
                pendingItemsForDialog.forEach { put(it.id, "") }
            }
        }
        var selectedDeliveryMillis by remember { mutableStateOf(System.currentTimeMillis()) }
        var dateMode by remember { mutableStateOf("TODAY") } // TODAY, YESTERDAY, CUSTOM
        var receiverName by remember { mutableStateOf("") }
        var deliveryNote by remember { mutableStateOf("") }
        val dateDisplaySdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { showPickupEntryDialog = false },
            title = {
                Text(
                    text = "🚚 नई टाइल डिलीवरी दर्ज करें",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFC2410C)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "ग्राहक आज/चुनी गई तारीख को जितनी टाइल ले जा रहे हैं, वह मात्रा भरें:",
                        fontSize = 11.5.sp,
                        color = Color(0xFF475569)
                    )

                    // Date selector chips
                    Text("📅 डिलीवरी तारीख: ${dateDisplaySdf.format(Date(selectedDeliveryMillis))}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy900)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        androidx.compose.material3.FilterChip(
                            selected = dateMode == "TODAY",
                            onClick = {
                                dateMode = "TODAY"
                                selectedDeliveryMillis = System.currentTimeMillis()
                            },
                            label = { Text("आज", fontSize = 11.sp) }
                        )
                        androidx.compose.material3.FilterChip(
                            selected = dateMode == "YESTERDAY",
                            onClick = {
                                dateMode = "YESTERDAY"
                                selectedDeliveryMillis = System.currentTimeMillis() - (24L * 60 * 60 * 1000)
                            },
                            label = { Text("कल", fontSize = 11.sp) }
                        )
                        androidx.compose.material3.FilterChip(
                            selected = dateMode == "CUSTOM",
                            onClick = {
                                val cal = java.util.Calendar.getInstance().apply { timeInMillis = selectedDeliveryMillis }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val picked = java.util.Calendar.getInstance().apply {
                                            set(year, month, dayOfMonth, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
                                        }
                                        selectedDeliveryMillis = picked.timeInMillis
                                        dateMode = "CUSTOM"
                                    },
                                    cal.get(java.util.Calendar.YEAR),
                                    cal.get(java.util.Calendar.MONTH),
                                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            label = { Text("तारीख चुनें", fontSize = 11.sp) }
                        )
                    }

                    // Pending items list with box/loose inputs + Fill All shortcut
                    pendingItemsForDialog.forEach { pItem ->
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "[${pItem.tileCode}] ${pItem.tileName}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Navy900
                                        )
                                        Text(
                                            text = "बाकी: ${pItem.pendingFullBoxes} बॉक्स" + if (pItem.pendingLoosePieces > 0) " + ${pItem.pendingLoosePieces} पीस" else "",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            boxesInputs[pItem.id] = pItem.pendingFullBoxes.toString()
                                            looseInputs[pItem.id] = if (pItem.pendingLoosePieces > 0) pItem.pendingLoosePieces.toString() else ""
                                        },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text("सब भरें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = boxesInputs[pItem.id] ?: "",
                                        onValueChange = { boxesInputs[pItem.id] = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("बॉक्स (Max ${pItem.pendingFullBoxes})", fontSize = 10.sp) },
                                        placeholder = { Text("0") },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = looseInputs[pItem.id] ?: "",
                                        onValueChange = { looseInputs[pItem.id] = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("खुले पीस", fontSize = 10.sp) },
                                        placeholder = { Text("0") },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = receiverName,
                        onValueChange = { receiverName = it },
                        label = { Text("किसने ली / ड्राइवर नाम (Optional)") },
                        placeholder = { Text("उदा. रमेश ड्राइवर / स्वयं ग्राहक") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = deliveryNote,
                        onValueChange = { deliveryNote = it },
                        label = { Text("गाड़ी नंबर / नोट (Optional)") },
                        placeholder = { Text("उदा. ई-रिक्शा / पिकअप UP-65...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val entries = pendingItemsForDialog.mapNotNull { pItem ->
                            val b = boxesInputs[pItem.id]?.toIntOrNull() ?: 0
                            val p = looseInputs[pItem.id]?.toIntOrNull() ?: 0
                            if (b > 0 || p > 0) Triple(pItem.id, b, p) else null
                        }
                        if (entries.isEmpty()) {
                            Toast.makeText(context, "कृपया कम से कम 1 टाइल की मात्रा भरें", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.recordPartialDelivery(
                            orderId = order.id,
                            entries = entries,
                            receiverName = receiverName,
                            notes = deliveryNote,
                            deliveredAt = selectedDeliveryMillis,
                            onSuccess = {
                                showPickupEntryDialog = false
                                selectedDeliveryTab = 1
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                ) {
                    Text("डिलीवरी सेव करें", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPickupEntryDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Confirm Mark All Remaining Delivered
    if (showMarkAllDeliveredConfirm) {
        AlertDialog(
            onDismissRequest = { showMarkAllDeliveredConfirm = false },
            title = { Text("सभी बाकी टाइल डिलीवर करें?", fontWeight = FontWeight.Bold) },
            text = {
                Text("इस ऑर्डर की सभी बची हुई टाइल्स आज की तारीख व समय के साथ डिलीवर (Complete) के रूप में दर्ज हो जाएंगी।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markAllRemainingDelivered(
                            orderId = order.id,
                            receiverName = order.customerName,
                            notes = "सभी बाकी टाइल एक साथ डिलीवर"
                        ) {
                            showMarkAllDeliveredConfirm = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                ) {
                    Text("हाँ, डिलीवर करें", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showMarkAllDeliveredConfirm = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
private fun OrderItemDetailCard(
    item: OrderItemEntity,
    showPrice: Boolean,
    isConfirmedOrder: Boolean,
    onReturnClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                ) {
                    if (!item.tilePhotoUri.isNullOrBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(item.tilePhotoUri),
                            contentDescription = item.tileName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(item.tileSize, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = item.tileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (item.isMarble) {
                            Surface(color = Color(0xFFD97706), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "🪨 MARBLE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    if (item.isMarble) {
                        val unitLbl = if (item.marbleUnit == "FEET") "ft" else "in"
                        Text(
                            text = "माप: ${item.marbleLength} × ${item.marbleWidth} $unitLbl × ${item.marblePieces} नग",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "कुल: ${String.format(Locale.US, "%.2f", item.marbleTotalSqFt)} Sq.Ft @ ₹${String.format(Locale.US, "%.2f", item.marbleRatePerSqFt)}/ft²",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Navy900
                        )
                    } else {
                        Text(
                            text = "Code: ${item.tileCode} | Size: ${item.tileSize}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Quantity: ${item.fullBoxes} Boxes" + if (item.loosePieces > 0) " + ${item.loosePieces} loose pcs" else "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Navy900
                        )
                        Text(
                            text = "Internal: ${item.totalPieces} total pieces (${item.piecesPerBox} Pcs/Bx)",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    if (!item.isFullyDelivered) {
                        val tBx = item.deliveredTotalPieces / item.piecesPerBox.coerceAtLeast(1)
                        val tPcs = item.deliveredTotalPieces % item.piecesPerBox.coerceAtLeast(1)
                        Text(
                            text = "🚚 ले गए: ${tBx} Bx" + (if (tPcs > 0) "+${tPcs}P" else "") +
                                    "  |  बाकी: ${item.pendingFullBoxes} Bx" + (if (item.pendingLoosePieces > 0) "+${item.pendingLoosePieces}P" else ""),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEA580C)
                        )
                    } else {
                        Text(
                            text = "✅ पूरी डिलीवरी हो गई",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF15803D)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (showPrice) {
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", item.lineTotal)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Navy900
                        )
                        Text(
                            text = if (item.isMarble) "@ ₹${String.format(Locale.US, "%.0f", item.marbleRatePerSqFt)}/ft²" else "@ ₹${item.pricePerBox.toInt()}/Bx",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Price Hidden",
                                fontSize = 9.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Return / Exchange Action Button
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onReturnClick,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = "↩️ वापसी / बदली (Return / Exchange)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}
