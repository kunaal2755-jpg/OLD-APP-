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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.OrderItemEntity
import com.example.data.model.OrderWithItems
import com.example.ui.MainViewModel
import com.example.ui.components.ConfirmDeleteOrderDialog
import com.example.ui.components.PdfPriceVisibilitySwitchCard
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
    val settings by viewModel.shopSettings.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var showWhatsAppPhoneDialog by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var addPaymentAmount by remember { mutableStateOf("") }
    var addPaymentMode by remember { mutableStateOf("CASH") }
    var addPaymentNote by remember { mutableStateOf("") }

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

            // 2. Per-Order PDF Price Visibility Switch (Live switchable per user requirement)
            item {
                PdfPriceVisibilitySwitchCard(
                    showPrice = order.showPriceOnPdf,
                    onToggle = { newSetting ->
                        viewModel.toggleShowPriceOnPdf(order.id, newSetting)
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

        AlertDialog(
            onDismissRequest = { returningItem = null },
            title = {
                Text(
                    text = "↩️ टाइल वापसी (Tile Return & Restock)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "टाइल: ${targetItem.tileName} (Code: ${targetItem.tileCode})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Navy900
                    )
                    Text(
                        text = "इस ऑर्डर में बेची गई मात्रा: ${targetItem.fullBoxes} Bx" + if (targetItem.loosePieces > 0) " + ${targetItem.loosePieces} Pcs" else "" + " (कुल $maxAvailablePieces पीस)",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )

                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ℹ️ ग्राहक द्वारा वापस की गई टाइल सीधे इन्वेंटरी वेयरहाउस स्टॉक में वापस जुड़ जाएगी और बिल व बैलेंस से रिफंड राशि घट जाएगी।",
                            fontSize = 11.sp,
                            color = Color(0xFF991B1B),
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = returnBoxesStr,
                            onValueChange = { returnBoxesStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text("वापस बॉक्स (Boxes)") },
                            placeholder = { Text("0") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = returnLooseStr,
                            onValueChange = { returnLooseStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text("खुले पीस (Loose)") },
                            placeholder = { Text("0") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (calculatedRefund > 0) {
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💰 कुल रिफंड / बिल समायोजन: ₹${String.format(Locale.US, "%.2f", calculatedRefund)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF166534),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = returnReasonStr,
                        onValueChange = { returnReasonStr = it },
                        label = { Text("वापसी का कारण (Reason - Optional)") },
                        placeholder = { Text("उदा. काम खत्म होने के बाद 2 बॉक्स बचे") },
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
                        viewModel.returnTileFromOrder(
                            orderId = order.id,
                            orderItemId = targetItem.id,
                            returnBoxes = boxes,
                            returnLoose = loose,
                            reason = returnReasonStr.trim(),
                            onSuccess = {
                                returningItem = null
                                Toast.makeText(context, "टाइल स्टॉक में वापस जोड़ दी गई!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, "त्रुटि: $err", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("वापस लें व स्टॉक जोड़ें (Confirm Return)", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { returningItem = null }) {
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
                    Text(
                        text = item.tileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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

                Column(horizontalAlignment = Alignment.End) {
                    if (showPrice) {
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", item.lineTotal)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Navy900
                        )
                        Text(
                            text = "@ ₹${item.pricePerBox.toInt()}/Bx",
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

            // Return Tile Action Button
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
                        text = "↩️ टाइल वापस लें (Return Tile)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}
