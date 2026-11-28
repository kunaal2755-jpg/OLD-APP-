package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.FilterChip
import com.example.ui.theme.getAppThemeOption
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.Customer
import com.example.data.model.OrderItemEntity
import com.example.data.model.TileProduct
import com.example.ui.MainViewModel
import com.example.ui.Screen
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.PdfDisplaySwitchesCard
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess
import java.util.Locale

// Mutable draft item for sale builder
data class DraftOrderItem(
    val product: TileProduct,
    var fullBoxes: Int = 1,
    var loosePieces: Int = 0,
    var pricePerBox: Double = 0.0,
    var isPremiumHighlighter: Boolean = false,
    val itemType: String = "TILE", // "TILE", "MARBLE", "MANUAL"
    val marbleLength: Double = 0.0,
    val marbleWidth: Double = 0.0,
    val marbleUnit: String = "INCH", // "INCH" or "FEET"
    val marblePieces: Int = 0,
    val marbleTotalSqFt: Double = 0.0,
    val marbleRatePerSqFt: Double = 0.0
) {
    val isMarble: Boolean get() = itemType == "MARBLE"
    val totalPieces: Int
        get() = if (isMarble) marblePieces.coerceAtLeast(fullBoxes) else (fullBoxes * product.piecesPerBox) + loosePieces
    val lineTotal: Double
        get() {
            if (isMarble) {
                return marbleTotalSqFt * marbleRatePerSqFt
            }
            val boxTotal = fullBoxes * pricePerBox
            val pieceRate = if (product.piecesPerBox > 0) pricePerBox / product.piecesPerBox else 0.0
            val looseTotal = loosePieces * pieceRate
            return boxTotal + looseTotal
        }

    fun toOrderItemEntity(orderId: Long = 0L): OrderItemEntity {
        val qtyBoxes = if (isMarble) marblePieces.coerceAtLeast(1) else fullBoxes
        val qtyLoose = if (isMarble) 0 else loosePieces
        val effectiveRatePerBox = if (isMarble) {
            if (qtyBoxes > 0) lineTotal / qtyBoxes else lineTotal
        } else {
            pricePerBox
        }
        return OrderItemEntity(
            orderId = orderId,
            productId = product.id,
            tileCode = product.code,
            tileName = product.name,
            tileSize = product.size,
            piecesPerBox = if (isMarble) 1 else product.piecesPerBox,
            tilePhotoUri = product.photoUri,
            fullBoxes = qtyBoxes,
            loosePieces = qtyLoose,
            pricePerBox = effectiveRatePerBox,
            lineTotal = lineTotal,
            itemType = itemType,
            marbleLength = marbleLength,
            marbleWidth = marbleWidth,
            marbleUnit = marbleUnit,
            marblePieces = marblePieces,
            marbleTotalSqFt = marbleTotalSqFt,
            marbleRatePerSqFt = marbleRatePerSqFt
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleOrderScreen(
    viewModel: MainViewModel,
    editOrderId: Long? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val sizePricings by viewModel.sizePricings.collectAsStateWithLifecycle()
    val settings by viewModel.shopSettings.collectAsStateWithLifecycle()
    val activeTheme = getAppThemeOption(settings.appTheme)

    val existingOrderWithItems by remember(editOrderId) {
        if (editOrderId != null) {
            viewModel.repository.getOrderWithItems(editOrderId)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.collectAsStateWithLifecycle(initialValue = null)

    val adminConfig by viewModel.adminGlobalConfig.collectAsStateWithLifecycle()
    val allowDiscounts = adminConfig?.allowBillingDiscounts != false

    var invoiceNumber by remember { mutableStateOf(viewModel.generateNextInvoiceNumber()) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var orderNotes by remember { mutableStateOf("") }
    var showPriceOnPdf by remember { mutableStateOf(true) }
    var showSizeTotalOnPdf by remember { mutableStateOf(true) }
    var showDeliveryStatusOnPdf by remember { mutableStateOf(true) }
    var discountStr by remember { mutableStateOf("") }
    var paidAmountStr by remember { mutableStateOf("") }
    var orderStatus by remember { mutableStateOf("PENDING") }
    var isDeliveredImmediately by remember { mutableStateOf(true) }
    var isInitialized by remember { mutableStateOf(false) }

    val orderItems = remember { mutableStateListOf<DraftOrderItem>() }
    var showTilePickerModal by remember { mutableStateOf(false) }
    var pendingTileForQtyPopup by remember { mutableStateOf<TileProduct?>(null) }
    var showMarbleItemModal by remember { mutableStateOf(false) }
    var showManualItemModal by remember { mutableStateOf(false) }

    LaunchedEffect(existingOrderWithItems, products) {
        val owi = existingOrderWithItems
        if (owi != null && !isInitialized) {
            val order = owi.order
            invoiceNumber = order.invoiceNumber
            customerName = order.customerName
            customerPhone = order.customerPhone
            customerAddress = order.customerAddress
            orderNotes = order.notes
            showPriceOnPdf = order.showPriceOnPdf
            showSizeTotalOnPdf = order.showSizeTotalOnPdf
            showDeliveryStatusOnPdf = order.showDeliveryStatusOnPdf
            discountStr = if (order.discount > 0) order.discount.toString() else "0"
            paidAmountStr = if (order.paidAmount > 0) String.format(Locale.US, "%.0f", order.paidAmount) else ""
            orderStatus = order.status

            orderItems.clear()
            owi.items.forEach { item ->
                val matchedProduct = products.find { it.id == item.productId }
                    ?: TileProduct(
                        id = item.productId,
                        name = item.tileName,
                        code = item.tileCode,
                        size = item.tileSize,
                        piecesPerBox = item.piecesPerBox,
                        defaultSellingPrice = item.pricePerBox,
                        photoUri = item.tilePhotoUri
                    )
                orderItems.add(
                    DraftOrderItem(
                        product = matchedProduct,
                        fullBoxes = item.fullBoxes,
                        loosePieces = item.loosePieces,
                        pricePerBox = item.pricePerBox,
                        itemType = item.itemType,
                        marbleLength = item.marbleLength,
                        marbleWidth = item.marbleWidth,
                        marbleUnit = item.marbleUnit,
                        marblePieces = item.marblePieces,
                        marbleTotalSqFt = item.marbleTotalSqFt,
                        marbleRatePerSqFt = item.marbleRatePerSqFt
                    )
                )
            }
            isInitialized = true
        }
    }

    // Pre-populate customer details when picked
    fun selectCustomer(cust: Customer) {
        selectedCustomer = cust
        customerName = cust.name
        customerPhone = cust.phone
        customerAddress = cust.address
    }

    // Calculations
    val subtotal = orderItems.sumOf { it.lineTotal }
    val discount = if (allowDiscounts) (discountStr.toDoubleOrNull() ?: 0.0) else 0.0
    val grandTotal = (subtotal - discount).coerceAtLeast(0.0)
    val paidAmount = if (paidAmountStr.isNotBlank()) (paidAmountStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, grandTotal) else 0.0
    val balanceAmount = (grandTotal - paidAmount).coerceAtLeast(0.0)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = activeTheme.primary,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (editOrderId != null) "Edit Order (ऑर्डर एडिट करें)" else "New Sale / Order Bill",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = PureWhite
                        )
                        Text(
                            text = "Invoice: $invoiceNumber" + if (editOrderId != null) " • Status: $orderStatus" else "",
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
            // 1. Customer Section
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
                            Text(
                                text = "Customer Information",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Navy900
                            )
                            if (customers.isNotEmpty()) {
                                Text(
                                    text = "Select Existing",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BlueAccent
                                )
                            }
                        }

                        // Existing customers quick row if any
                        if (customers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (customers.size > 2) 72.dp else 40.dp)
                            ) {
                                items(customers) { c ->
                                    Surface(
                                        color = if (selectedCustomer?.id == c.id) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                                        shape = RoundedCornerShape(8.dp),
                                        border = if (selectedCustomer?.id == c.id) CardDefaults.outlinedCardBorder() else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectCustomer(c) }
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = BlueAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${c.name} ${if (c.phone.isNotBlank()) "(${c.phone})" else ""}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = {
                                customerName = it
                                selectedCustomer = null
                            },
                            label = { Text("Customer Name *") },
                            placeholder = { Text("e.g. Ramesh Kumar or Cash Client") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("order_customer_name_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = customerPhone,
                                onValueChange = { customerPhone = it },
                                label = { Text("Mobile Number (Optional)") },
                                placeholder = { Text("10 digits") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("order_customer_phone_input")
                            )
                            OutlinedTextField(
                                value = customerAddress,
                                onValueChange = { customerAddress = it },
                                label = { Text("Delivery Address") },
                                placeholder = { Text("Site / Area") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 2. Per-Order PDF Customization Switches (Price, Size-wise Box Totals, Delivery Status)
            item {
                PdfDisplaySwitchesCard(
                    showPrice = showPriceOnPdf,
                    onTogglePrice = { showPriceOnPdf = it },
                    showSizeTotal = showSizeTotalOnPdf,
                    onToggleSizeTotal = { showSizeTotalOnPdf = it },
                    showDeliveryStatus = showDeliveryStatusOnPdf,
                    onToggleDeliveryStatus = { showDeliveryStatusOnPdf = it }
                )
            }

            // 3. Tile / Marble / Manual Items Header & Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Order Items (टाइल / मार्बल / सामान)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${orderItems.size} items added",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { showTilePickerModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_add_tile_to_order")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = PureWhite)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("टाइल जोड़ें", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        }

                        Button(
                            onClick = { showMarbleItemModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_add_marble_item")
                        ) {
                            Text("🪨 मार्बल/पत्थर", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        }

                        OutlinedButton(
                            onClick = { showManualItemModal = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(0.95f)
                                .testTag("button_add_manual_item")
                        ) {
                            Text("➕ अन्य", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (orderItems.isEmpty()) {
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
                                .padding(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No tiles added to this order yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Tap '+ Add Tile' above to select tiles from your saved inventory.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Quick Size Pricing Bar (Customize rate in 1-click for 12x18 and other sizes - Tiles only)
                val tileOnlyItems = orderItems.filter { !it.isMarble }
                if (tileOnlyItems.isNotEmpty()) {
                item {
                    val uniqueSizes = tileOnlyItems.map { it.product.size.trim() }.distinct().filter { it.isNotBlank() }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = activeTheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "त्वरित रेट सेट करें (Quick Size Pricing):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = activeTheme.primary
                                )
                            }
                            Text(
                                text = "एक ही जगह से सभी टाइलों या हाईलाइटर का रेट एक क्लिक में सेट करें:",
                                fontSize = 11.sp,
                                color = Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            uniqueSizes.forEach { sz ->
                                val count = orderItems.count { it.product.size.equals(sz, ignoreCase = true) }
                                val highlightersCount = orderItems.count { it.product.size.equals(sz, ignoreCase = true) && it.isPremiumHighlighter }
                                val normalCount = count - highlightersCount

                                var bulkNormalRate by remember(sz) {
                                    val currentNormal = orderItems.find { it.product.size.equals(sz, ignoreCase = true) && !it.isPremiumHighlighter }?.pricePerBox
                                        ?: orderItems.find { it.product.size.equals(sz, ignoreCase = true) }?.pricePerBox ?: 0.0
                                    mutableStateOf(if (currentNormal > 0) currentNormal.toString() else "")
                                }
                                var bulkHlRate by remember(sz) {
                                    val currentHl = orderItems.find { it.product.size.equals(sz, ignoreCase = true) && it.isPremiumHighlighter }?.pricePerBox
                                    mutableStateOf(if (currentHl != null && currentHl > 0) currentHl.toString() else "")
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Size $sz ($count टाइलें)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF1E293B)
                                            )
                                            if (highlightersCount > 0) {
                                                Text(
                                                    text = "⭐ $highlightersCount हाईलाइटर",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFD97706)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = bulkNormalRate,
                                                onValueChange = { bulkNormalRate = it },
                                                label = { Text(if (highlightersCount > 0) "सामान्य ₹/Box" else "सभी ₹/Box", fontSize = 10.sp) },
                                                placeholder = { Text("0") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1.1f)
                                            )

                                            Button(
                                                onClick = {
                                                    val r = bulkNormalRate.toDoubleOrNull() ?: return@Button
                                                    orderItems.forEachIndexed { i, itm ->
                                                        if (itm.product.size.equals(sz, ignoreCase = true)) {
                                                            if (highlightersCount == 0 || !itm.isPremiumHighlighter) {
                                                                orderItems[i] = itm.copy(pricePerBox = r)
                                                            }
                                                        }
                                                    }
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(46.dp)
                                            ) {
                                                Text("लागू (Apply)", fontSize = 11.sp)
                                            }

                                            // If 12x18 or user has highlighters, show separate highlighter input
                                            if (sz.contains("12", ignoreCase = true) || highlightersCount > 0) {
                                                OutlinedTextField(
                                                    value = bulkHlRate,
                                                    onValueChange = { bulkHlRate = it },
                                                    label = { Text("प्रीमियम/HL ₹", fontSize = 10.sp) },
                                                    placeholder = { Text("0") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.weight(1.1f)
                                                )

                                                Button(
                                                    onClick = {
                                                        val r = bulkHlRate.toDoubleOrNull() ?: return@Button
                                                        orderItems.forEachIndexed { i, itm ->
                                                            if (itm.product.size.equals(sz, ignoreCase = true) && itm.isPremiumHighlighter) {
                                                                orderItems[i] = itm.copy(pricePerBox = r)
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(46.dp)
                                                ) {
                                                    Text("HL लागू", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }

                itemsIndexed(orderItems) { index, draftItem ->
                    DraftOrderItemCard(
                        item = draftItem,
                        onQuantityChanged = { fullBoxes, loosePieces ->
                            if (draftItem.isMarble) {
                                val newPcs = fullBoxes.coerceAtLeast(1)
                                val perSlabSqFt = if (draftItem.marbleUnit == "FEET") {
                                    draftItem.marbleLength * draftItem.marbleWidth
                                } else {
                                    (draftItem.marbleLength * draftItem.marbleWidth) / 144.0
                                }
                                orderItems[index] = draftItem.copy(
                                    fullBoxes = newPcs,
                                    marblePieces = newPcs,
                                    marbleTotalSqFt = perSlabSqFt * newPcs
                                )
                            } else {
                                orderItems[index] = draftItem.copy(fullBoxes = fullBoxes, loosePieces = loosePieces)
                            }
                        },
                        onPriceChanged = { price ->
                            if (draftItem.isMarble) {
                                orderItems[index] = draftItem.copy(marbleRatePerSqFt = price)
                            } else {
                                orderItems[index] = draftItem.copy(pricePerBox = price)
                            }
                        },
                        onHighlighterToggled = { isHl ->
                            orderItems[index] = draftItem.copy(isPremiumHighlighter = isHl)
                        },
                        onRemove = {
                            orderItems.removeAt(index)
                        }
                    )
                }
            }

            // 4. Order Summary Card
            if (orderItems.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Order Financial Summary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Navy900
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Subtotal (${orderItems.sumOf { it.fullBoxes }} Boxes)", fontSize = 13.sp)
                                Text("₹${String.format(Locale.US, "%.2f", subtotal)}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Discount (₹):", fontSize = 13.sp)
                                if (allowDiscounts) {
                                    OutlinedTextField(
                                        value = discountStr,
                                        onValueChange = { discountStr = it.filter { c -> c.isDigit() || c == '.' } },
                                        placeholder = { Text("0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.width(130.dp)
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFEF2F2),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "🔒 डिस्काउंट बंद (Admin)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
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
                                    Column {
                                        Text("Final Payable Amount", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                                        Text(if (showPriceOnPdf) "Will show on customer PDF" else "Hidden on customer PDF", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Text(
                                        text = "₹${String.format(Locale.US, "%.2f", grandTotal)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Payment Input & Balance Due preview
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = if (balanceAmount > 0.01) Color(0xFFFEF2F2) else Color(0xFFF0FDF4)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "💰 भुगतान व बकाया (Payment & Balance):",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (balanceAmount > 0.01) Color(0xFF991B1B) else Color(0xFF166534)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            TextButton(
                                                onClick = { paidAmountStr = String.format(Locale.US, "%.0f", grandTotal) },
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("पूरा जमा", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            TextButton(
                                                onClick = { paidAmountStr = "0" },
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("उधार (Zero)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = paidAmountStr,
                                            onValueChange = { paidAmountStr = it },
                                            label = { Text("जमा राशि (Received ₹)") },
                                            placeholder = { Text("0") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("बकाया राशि (Balance)", fontSize = 11.sp, color = Color(0xFF64748B))
                                            Text(
                                                text = if (balanceAmount > 0.01) "₹${String.format(Locale.US, "%.2f", balanceAmount)}" else "चुकता (Full)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (balanceAmount > 0.01) Color(0xFFDC2626) else Color(0xFF16A34A)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = orderNotes,
                                onValueChange = { orderNotes = it },
                                label = { Text("Order Notes / Remarks (Optional)") },
                                placeholder = { Text("e.g. Delivery by Wednesday, 2nd floor") },
                                maxLines = 2,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 5. Action Buttons (Pending vs Confirmed vs Update)
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (editOrderId != null) {
                            // Status selector card
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Order Status (ऑर्डर स्थिति):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = activeTheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = orderStatus == "PENDING",
                                            onClick = { orderStatus = "PENDING" },
                                            label = { Text("Pending (कच्चा बिल)") }
                                        )
                                        FilterChip(
                                            selected = orderStatus == "CONFIRMED",
                                            onClick = { orderStatus = "CONFIRMED" },
                                            label = { Text("Confirmed (पक्का बिल)") }
                                        )
                                    }
                                }
                            }

                            // Update Order Button
                            Button(
                                onClick = {
                                    if (customerName.isBlank()) {
                                        viewModel.showMessage("Please enter customer name")
                                        return@Button
                                    }
                                    if (orderItems.isEmpty()) {
                                        viewModel.showMessage("Please add at least one tile to the order")
                                        return@Button
                                    }
                                    val entityItems = orderItems.map { draft -> draft.toOrderItemEntity(editOrderId) }
                                    val existingOrder = existingOrderWithItems?.order
                                    val currentPaid = if (paidAmountStr.isNotBlank()) paidAmount else (existingOrder?.paidAmount ?: 0.0)
                                    val currentBal = (grandTotal - currentPaid).coerceAtLeast(0.0)
                                    val updatedOrder = existingOrder?.copy(
                                        customerName = customerName.trim().ifBlank { "Cash Customer" },
                                        customerPhone = customerPhone.trim(),
                                        customerAddress = customerAddress.trim(),
                                        status = orderStatus,
                                        showPriceOnPdf = showPriceOnPdf,
                                        showSizeTotalOnPdf = showSizeTotalOnPdf,
                                        showDeliveryStatusOnPdf = showDeliveryStatusOnPdf,
                                        subtotal = subtotal,
                                        discount = discount,
                                        grandTotal = grandTotal,
                                        paidAmount = currentPaid,
                                        balanceAmount = currentBal,
                                        notes = orderNotes.trim()
                                    ) ?: com.example.data.model.OrderEntity(
                                        id = editOrderId,
                                        invoiceNumber = invoiceNumber,
                                        customerName = customerName.trim().ifBlank { "Cash Customer" },
                                        customerPhone = customerPhone.trim(),
                                        customerAddress = customerAddress.trim(),
                                        status = orderStatus,
                                        showPriceOnPdf = showPriceOnPdf,
                                        showSizeTotalOnPdf = showSizeTotalOnPdf,
                                        showDeliveryStatusOnPdf = showDeliveryStatusOnPdf,
                                        subtotal = subtotal,
                                        discount = discount,
                                        grandTotal = grandTotal,
                                        paidAmount = currentPaid,
                                        balanceAmount = currentBal,
                                        notes = orderNotes.trim()
                                    )
                                    viewModel.updateOrder(
                                        order = updatedOrder,
                                        items = entityItems,
                                        onSuccess = {
                                            viewModel.navigateTo(Screen.OrderDetail(editOrderId))
                                        },
                                        onError = { err ->
                                            viewModel.showMessage("Error: $err")
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("button_update_order")
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = PureWhite)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Update & Save Order (ऑर्डर सेव करें)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PureWhite)
                            }
                        } else {
                            // Delivery Mode Selector (Full Delivery Now vs Hold / Partial Delivery)
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDeliveredImmediately) Color(0xFFF0FDF4) else Color(0xFFFFF7ED)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDeliveredImmediately) Color(0xFFBBF7D0) else Color(0xFFFED7AA)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "🚚 टाइल डिलीवरी स्थिति (Tile Pickup Mode)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isDeliveredImmediately) Color(0xFF166534) else Color(0xFFC2410C)
                                    )
                                    Text(
                                        text = "क्या ग्राहक अभी पूरी टाइल ले जा रहे हैं या पर्ची बनवाकर बाद में थोड़ी-थोड़ी ले जाएंगे?",
                                        fontSize = 11.sp,
                                        color = Color(0xFF475569)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isDeliveredImmediately) Color(0xFF16A34A) else Color.White,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isDeliveredImmediately) Color(0xFF16A34A) else Color(0xFFCBD5E1)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { isDeliveredImmediately = true }
                                                .testTag("delivery_mode_full_now")
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                                Text(
                                                    text = "✅ अभी पूरी ले जा रहे",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = if (isDeliveredImmediately) PureWhite else Navy900
                                                )
                                                Text(
                                                    text = "Full Delivery Now",
                                                    fontSize = 10.sp,
                                                    color = if (isDeliveredImmediately) Color(0xFFDCFCE7) else Color(0xFF64748B)
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (!isDeliveredImmediately) Color(0xFFEA580C) else Color.White,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (!isDeliveredImmediately) Color(0xFFEA580C) else Color(0xFFCBD5E1)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { isDeliveredImmediately = false }
                                                .testTag("delivery_mode_hold_partial")
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                                Text(
                                                    text = "📦 बाद में थोड़ी-थोड़ी",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = if (!isDeliveredImmediately) PureWhite else Navy900
                                                )
                                                Text(
                                                    text = "Hold / Partial Pickup",
                                                    fontSize = 10.sp,
                                                    color = if (!isDeliveredImmediately) Color(0xFFFFEDD5) else Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    }
                                    if (!isDeliveredImmediately) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "ℹ️ पर्ची व भुगतान अभी सेव होगा; ग्राहक जिस तारीख को जितनी टाइल ले जाएंगे, आप ऑर्डर खोलकर 'डिलीवरी दर्ज करें' में लिख सकेंगे।",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF9A3412)
                                        )
                                    }
                                }
                            }

                            // Confirm & Deduct Stock immediately
                            Button(
                                onClick = {
                                    if (customerName.isBlank()) {
                                        viewModel.showMessage("Please enter customer name")
                                        return@Button
                                    }
                                    val entityItems = orderItems.map { draft -> draft.toOrderItemEntity() }
                                    viewModel.createSaleOrder(
                                        invoiceNumber = invoiceNumber,
                                        customerId = selectedCustomer?.id,
                                        customerName = customerName,
                                        customerPhone = customerPhone,
                                        customerAddress = customerAddress,
                                        status = "CONFIRMED",
                                        showPriceOnPdf = showPriceOnPdf,
                                        showSizeTotalOnPdf = showSizeTotalOnPdf,
                                        showDeliveryStatusOnPdf = showDeliveryStatusOnPdf,
                                        subtotal = subtotal,
                                        discount = discount,
                                        grandTotal = grandTotal,
                                        paidAmount = paidAmount,
                                        notes = orderNotes,
                                        items = entityItems,
                                        isDeliveredImmediately = isDeliveredImmediately,
                                        onSuccess = { orderId ->
                                            viewModel.navigateTo(Screen.OrderDetail(orderId))
                                        },
                                        onError = { err ->
                                            viewModel.showMessage("Error: $err")
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("button_confirm_sale")
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = PureWhite)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isDeliveredImmediately) "Confirm Sale & Deduct Stock" else "Confirm Booking (Hold / Partial Delivery)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = PureWhite
                                )
                            }

                            // Save as Pending (Estimate) without stock deduction
                            OutlinedButton(
                                onClick = {
                                    if (customerName.isBlank()) {
                                        viewModel.showMessage("Please enter customer name")
                                        return@OutlinedButton
                                    }
                                    val entityItems = orderItems.map { draft -> draft.toOrderItemEntity() }
                                    viewModel.createSaleOrder(
                                        invoiceNumber = invoiceNumber,
                                        customerId = selectedCustomer?.id,
                                        customerName = customerName,
                                        customerPhone = customerPhone,
                                        customerAddress = customerAddress,
                                        status = "PENDING",
                                        showPriceOnPdf = showPriceOnPdf,
                                        showSizeTotalOnPdf = showSizeTotalOnPdf,
                                        showDeliveryStatusOnPdf = showDeliveryStatusOnPdf,
                                        subtotal = subtotal,
                                        discount = discount,
                                        grandTotal = grandTotal,
                                        paidAmount = paidAmount,
                                        notes = orderNotes,
                                        items = entityItems,
                                        isDeliveredImmediately = isDeliveredImmediately,
                                        onSuccess = { orderId ->
                                            viewModel.navigateTo(Screen.OrderDetail(orderId))
                                        },
                                        onError = { err ->
                                            viewModel.showMessage("Error: $err")
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("button_save_pending_order")
                            ) {
                                Icon(imageVector = Icons.Default.HourglassEmpty, contentDescription = null, tint = StatusPending)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save as Pending (Stock Unchanged)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = StatusPending)
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

    // Modal: Select Tile from Inventory -> Opens Quick Quantity Pop-up!
    if (showTilePickerModal) {
        TileProductPickerModal(
            products = products,
            onDismiss = { showTilePickerModal = false },
            onTileSelected = { product ->
                showTilePickerModal = false
                pendingTileForQtyPopup = product
            }
        )
    }

    // Modal: Quick Quantity Pop-up when tapping any tile
    pendingTileForQtyPopup?.let { selectedProd ->
        val existingIdx = orderItems.indexOfFirst { !it.isMarble && it.product.id == selectedProd.id }
        val sizeDefault = sizePricings.find { it.sizeName.equals(selectedProd.size, ignoreCase = true) }?.defaultPricePerBox
        val suggestedPrice = when {
            existingIdx >= 0 -> orderItems[existingIdx].pricePerBox
            selectedProd.defaultSellingPrice > 0 -> selectedProd.defaultSellingPrice
            sizeDefault != null && sizeDefault > 0 -> sizeDefault
            else -> 350.0
        }
        QuickAddTileQtyDialog(
            product = selectedProd,
            initialBoxes = if (existingIdx >= 0) orderItems[existingIdx].fullBoxes else 1,
            initialLoose = if (existingIdx >= 0) orderItems[existingIdx].loosePieces else 0,
            initialRate = suggestedPrice,
            isUpdatingExisting = existingIdx >= 0,
            onDismiss = { pendingTileForQtyPopup = null },
            onConfirm = { boxes, loose, rate ->
                val isHighlighterInitial = selectedProd.name.contains("highlighter", ignoreCase = true) ||
                        selectedProd.name.contains("hl", ignoreCase = true) ||
                        selectedProd.name.contains("decor", ignoreCase = true) ||
                        selectedProd.name.contains("dark", ignoreCase = true) ||
                        selectedProd.name.contains("premium", ignoreCase = true)
                if (existingIdx >= 0) {
                    orderItems[existingIdx] = orderItems[existingIdx].copy(
                        fullBoxes = boxes,
                        loosePieces = loose,
                        pricePerBox = rate
                    )
                    viewModel.showMessage("${selectedProd.name} की मात्रा अपडेट हो गई!")
                } else {
                    orderItems.add(
                        DraftOrderItem(
                            product = selectedProd,
                            fullBoxes = boxes,
                            loosePieces = loose,
                            pricePerBox = rate,
                            isPremiumHighlighter = isHighlighterInitial,
                            itemType = "TILE"
                        )
                    )
                    viewModel.showMessage("${selectedProd.name} ($boxes बॉक्स) बिल में जुड़ा!")
                }
                pendingTileForQtyPopup = null
            }
        )
    }

    // Modal: Add Marble / Stone (मार्बल / पत्थर — लंबाई × चौड़ाई → Sq.Ft व ₹/Sq.Ft कैलकुलेटर)
    if (showMarbleItemModal) {
        MarbleItemDialog(
            onDismiss = { showMarbleItemModal = false },
            onMarbleAdded = { name, unit, length, width, pieces, totalSqFt, ratePerSqFt ->
                val virtualId = -System.currentTimeMillis()
                val fmtL = if (length % 1.0 == 0.0) length.toInt().toString() else String.format(Locale.US, "%.1f", length)
                val fmtW = if (width % 1.0 == 0.0) width.toInt().toString() else String.format(Locale.US, "%.1f", width)
                val unitShort = if (unit == "FEET") "ft" else "in"
                val lineTot = totalSqFt * ratePerSqFt
                val virtualProduct = TileProduct(
                    id = virtualId,
                    name = name,
                    code = "MARBLE",
                    size = "${fmtL}x${fmtW} $unitShort",
                    piecesPerBox = 1,
                    defaultSellingPrice = if (pieces > 0) lineTot / pieces else lineTot,
                    stockPieces = 99999
                )
                orderItems.add(
                    DraftOrderItem(
                        product = virtualProduct,
                        fullBoxes = pieces.coerceAtLeast(1),
                        loosePieces = 0,
                        pricePerBox = if (pieces > 0) lineTot / pieces else lineTot,
                        itemType = "MARBLE",
                        marbleLength = length,
                        marbleWidth = width,
                        marbleUnit = unit,
                        marblePieces = pieces.coerceAtLeast(1),
                        marbleTotalSqFt = totalSqFt,
                        marbleRatePerSqFt = ratePerSqFt
                    )
                )
                showMarbleItemModal = false
                viewModel.showMessage("🪨 $name (${String.format(Locale.US, "%.2f", totalSqFt)} Sq.Ft) बिल में जुड़ा!")
            }
        )
    }

    // Modal: Manually Add Custom Item (Chemical, Grout, Profile, Extra Tile, Transport)
    if (showManualItemModal) {
        ManualItemDialog(
            onDismiss = { showManualItemModal = false },
            onItemAdded = { name, size, code, boxes, pieces, rate ->
                val virtualId = -System.currentTimeMillis()
                val virtualProduct = TileProduct(
                    id = virtualId,
                    name = name,
                    code = code.ifBlank { "MANUAL" },
                    size = size.ifBlank { "ITEM" },
                    piecesPerBox = if (pieces > 0) pieces else 1,
                    defaultSellingPrice = rate,
                    stockPieces = 99999
                )
                orderItems.add(
                    DraftOrderItem(
                        product = virtualProduct,
                        fullBoxes = boxes,
                        loosePieces = 0,
                        pricePerBox = rate,
                        itemType = "MANUAL"
                    )
                )
                showManualItemModal = false
                viewModel.showMessage("'$name' जोड़ा गया!")
            }
        )
    }
}

@Composable
private fun DraftOrderItemCard(
    item: DraftOrderItem,
    onQuantityChanged: (fullBoxes: Int, loosePieces: Int) -> Unit,
    onPriceChanged: (Double) -> Unit,
    onHighlighterToggled: (Boolean) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var boxesStr by remember(item.fullBoxes, item.marblePieces) {
        val v = if (item.isMarble) item.marblePieces else item.fullBoxes
        mutableStateOf(if (v > 0) v.toString() else "")
    }
    var looseStr by remember(item.loosePieces) { mutableStateOf(if (item.loosePieces > 0) item.loosePieces.toString() else "") }
    var priceStr by remember(item.pricePerBox, item.marbleRatePerSqFt) {
        val p = if (item.isMarble) item.marbleRatePerSqFt else item.pricePerBox
        mutableStateOf(if (p > 0) p.toString() else "")
    }

    fun sanitizeQuantity(previous: String, incoming: String): String {
        val digits = incoming.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        if (previous == "0" && digits.length == 2) {
            val nonZero = digits.replace("0", "")
            return if (nonZero.isNotEmpty()) nonZero else "0"
        }
        val noLeadingZeros = digits.trimStart('0')
        return if (noLeadingZeros.isEmpty()) "0" else noLeadingZeros
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isMarble) Color(0xFFFFFBEB) else MaterialTheme.colorScheme.surface
        ),
        border = if (item.isMarble) BorderStroke(1.dp, Color(0xFFFCD34D)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Photo, Name, Size & Remove button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (item.isMarble) Color(0xFFFEF3C7) else Color(0xFFF1F5F9))
                        .border(1.dp, if (item.isMarble) Color(0xFFF59E0B) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                ) {
                    if (!item.product.photoUri.isNullOrBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(item.product.photoUri),
                            contentDescription = item.product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (item.isMarble) {
                        Text("🪨", fontSize = 22.sp)
                    } else {
                        Text(
                            text = item.product.size,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = item.product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (item.isMarble) {
                            Surface(color = Color(0xFFD97706), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "MARBLE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    if (item.isMarble) {
                        val unitLbl = if (item.marbleUnit == "FEET") "ft" else "in"
                        Text(
                            text = "माप: ${item.marbleLength} × ${item.marbleWidth} $unitLbl • ${item.marblePieces} नग",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "कुल क्षेत्रफल: ${String.format(Locale.US, "%.2f", item.marbleTotalSqFt)} Sq.Ft  •  ₹${String.format(Locale.US, "%.2f", item.marbleRatePerSqFt)}/Sq.Ft",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    } else {
                        Text(
                            text = "Code: ${item.product.code} | Size: ${item.product.size} (${item.product.piecesPerBox} Pcs/Bx)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.product.id > 0L) {
                            Text(
                                text = "Available Warehouse Stock: ${item.product.stockDisplay}",
                                fontSize = 11.sp,
                                color = if (item.product.isLowStock) StatusError else Color(0xFF16A34A)
                            )
                        }
                    }
                }

                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = StatusError)
                }
            }

            if (!item.isMarble) {
                Spacer(modifier = Modifier.height(6.dp))

                // Type selector: Standard vs Premium / Highlighter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (!item.isPremiumHighlighter) Color(0xFFE2E8F0) else Color.Transparent,
                        border = BorderStroke(1.dp, if (!item.isPremiumHighlighter) Color(0xFF94A3B8) else Color(0xFFCBD5E1)),
                        modifier = Modifier.clickable { onHighlighterToggled(false) }
                    ) {
                        Text(
                            text = "सामान्य (Standard)",
                            fontSize = 11.sp,
                            fontWeight = if (!item.isPremiumHighlighter) FontWeight.Bold else FontWeight.Normal,
                            color = if (!item.isPremiumHighlighter) Color(0xFF1E293B) else Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (item.isPremiumHighlighter) Color(0xFFFEF3C7) else Color.Transparent,
                        border = BorderStroke(1.dp, if (item.isPremiumHighlighter) Color(0xFFD97706) else Color(0xFFCBD5E1)),
                        modifier = Modifier.clickable { onHighlighterToggled(true) }
                    ) {
                        Text(
                            text = "✨ प्रीमियम / हाईलाइटर",
                            fontSize = 11.sp,
                            fontWeight = if (item.isPremiumHighlighter) FontWeight.Bold else FontWeight.Normal,
                            color = if (item.isPremiumHighlighter) Color(0xFFB45309) else Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity & Rate inputs
            if (item.isMarble) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = boxesStr,
                        onValueChange = {
                            val cleaned = sanitizeQuantity(boxesStr, it)
                            boxesStr = cleaned
                            val p = cleaned.toIntOrNull() ?: 1
                            onQuantityChanged(p.coerceAtLeast(1), 0)
                        },
                        label = { Text("नग / पीस (Slabs)") },
                        placeholder = { Text("1") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = {
                            priceStr = it
                            val r = it.toDoubleOrNull() ?: 0.0
                            onPriceChanged(r)
                        },
                        label = { Text("रेट ₹ / Sq.Ft *") },
                        placeholder = { Text("0.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f)
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = boxesStr,
                        onValueChange = {
                            val cleaned = sanitizeQuantity(boxesStr, it)
                            boxesStr = cleaned
                            val b = cleaned.toIntOrNull() ?: 0
                            onQuantityChanged(b, item.loosePieces)
                        },
                        label = { Text("Full Boxes *") },
                        placeholder = { Text("1") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = looseStr,
                        onValueChange = {
                            val cleaned = sanitizeQuantity(looseStr, it)
                            looseStr = cleaned
                            val l = cleaned.toIntOrNull() ?: 0
                            onQuantityChanged(item.fullBoxes, l)
                        },
                        label = { Text("Loose Pcs") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = {
                            priceStr = it
                            val p = it.toDoubleOrNull() ?: 0.0
                            onPriceChanged(p)
                        },
                        label = { Text("Rate/Box (₹) *") },
                        placeholder = { Text("0.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Footer
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (item.isMarble) {
                        "कुल: ${String.format(Locale.US, "%.2f", item.marbleTotalSqFt)} Sq.Ft × ₹${String.format(Locale.US, "%.2f", item.marbleRatePerSqFt)}"
                    } else {
                        "Internal: ${item.totalPieces} total pieces to adjust"
                    },
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "Line Total: ₹${String.format(Locale.US, "%.2f", item.lineTotal)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Navy900
                )
            }
        }
    }
}

@Composable
private fun TileProductPickerModal(
    products: List<TileProduct>,
    onDismiss: () -> Unit,
    onTileSelected: (TileProduct) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = products.filter {
        search.isBlank() ||
                it.name.contains(search, ignoreCase = true) ||
                it.code.contains(search, ignoreCase = true) ||
                it.size.contains(search, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Tile from Inventory", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search tile name, code, size...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(filtered) { prod ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTileSelected(prod) }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE2E8F0))
                                ) {
                                    if (!prod.photoUri.isNullOrBlank()) {
                                        Image(
                                            painter = rememberAsyncImagePainter(prod.photoUri),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(prod.size, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                    Text("Code: ${prod.code} • Stock: ${prod.stockDisplay}", fontSize = 10.sp, color = Color(0xFF64748B))
                                }
                                Text("₹${prod.defaultSellingPrice.toInt()}/Bx", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy900)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ManualItemDialog(
    onDismiss: () -> Unit,
    onItemAdded: (name: String, size: String, code: String, boxes: Int, pieces: Int, rate: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sizeOrCategory by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var boxesStr by remember { mutableStateOf("1") }
    var rateStr by remember { mutableStateOf("") }
    var quickItemType by remember { mutableStateOf("TILE") }

    val quickPresets = listOf(
        "टाइल्स (Tile)",
        "केमिकल / एडहेसिव (Adhesive)",
        "ग्राउट / पुट्टी (Grout)",
        "मार्बल / ग्रेनाइट (Granite)",
        "किचन / पट्टी (Profile)",
        "किराया / भाड़ा (Transport)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("➕ मैन्युअल आइटम जोड़ें (Custom Item)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "त्वरित प्रकार चुनें (Quick Presets):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (quickItemType == "TILE") Color(0xFFE2E8F0) else Color.Transparent,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.clickable {
                            quickItemType = "TILE"
                            if (name.isBlank() || name == "Chemical / Adhesive" || name == "Epoxy / Grout") name = ""
                            if (sizeOrCategory.isBlank()) sizeOrCategory = "12X18"
                        }
                    ) {
                        Text("टाइल (Tile)", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (quickItemType == "CHEMICAL") Color(0xFFE2E8F0) else Color.Transparent,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.clickable {
                            quickItemType = "CHEMICAL"
                            name = "Tile Adhesive Chemical (20kg)"
                            sizeOrCategory = "20 KG"
                            code = "CHEM-20"
                        }
                    ) {
                        Text("केमिकल (Adhesive)", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (quickItemType == "GROUT") Color(0xFFE2E8F0) else Color.Transparent,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.clickable {
                            quickItemType = "GROUT"
                            name = "Tile Grout / Epoxy"
                            sizeOrCategory = "1 KG"
                            code = "GROUT-1"
                        }
                    ) {
                        Text("ग्राउट / पुट्टी", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("आइटम का नाम (Item Name) *") },
                    placeholder = { Text("उदा. 12x18 Special Tile / Chemical") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = sizeOrCategory,
                        onValueChange = { sizeOrCategory = it },
                        label = { Text("साइज़ / पैकिंग (Size)") },
                        placeholder = { Text("12X18 / 20kg") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("कोड / मार्का") },
                        placeholder = { Text("CUSTOM") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = boxesStr,
                        onValueChange = { boxesStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("मात्रा (Boxes/Qty) *") },
                        placeholder = { Text("1") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text("रेट प्रति बॉक्स/यूनिट (₹) *") },
                        placeholder = { Text("0.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val b = boxesStr.toIntOrNull() ?: 1
                    val r = rateStr.toDoubleOrNull() ?: 0.0
                    onItemAdded(
                        name.trim(),
                        sizeOrCategory.trim().ifBlank { "MANUAL" },
                        code.trim().ifBlank { "MANUAL" },
                        if (b > 0) b else 1,
                        1,
                        r
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900)
            ) {
                Text("बिल में जोड़ें (Add to Bill)", color = PureWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
private fun QuickAddTileQtyDialog(
    product: TileProduct,
    initialBoxes: Int,
    initialLoose: Int,
    initialRate: Double,
    isUpdatingExisting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (boxes: Int, loose: Int, rate: Double) -> Unit
) {
    var boxesStr by remember { mutableStateOf(if (initialBoxes > 0) initialBoxes.toString() else "1") }
    var looseStr by remember { mutableStateOf(if (initialLoose > 0) initialLoose.toString() else "0") }
    var rateStr by remember { mutableStateOf(if (initialRate > 0) String.format(Locale.US, "%.0f", initialRate) else "") }

    val boxes = boxesStr.toIntOrNull() ?: 0
    val loose = looseStr.toIntOrNull() ?: 0
    val rate = rateStr.toDoubleOrNull() ?: 0.0
    val pieceRate = if (product.piecesPerBox > 0) rate / product.piecesPerBox else 0.0
    val totalPieces = (boxes * product.piecesPerBox) + loose
    val lineTotal = (boxes * rate) + (loose * pieceRate)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                ) {
                    if (!product.photoUri.isNullOrBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(product.photoUri),
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(product.size, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Navy900)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                    Text(
                        text = "${product.size} • Code: ${product.code} • ${product.piecesPerBox} Pcs/Bx",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (product.isLowStock) Color(0xFFFEF2F2) else Color(0xFFECFDF5),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📦 उपलब्ध स्टॉक: ${product.stockDisplay}" + if (product.location.isNotBlank()) "  •  📍 ${product.location}" else "",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (product.isLowStock) Color(0xFFDC2626) else Color(0xFF065F46),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Quick Box Shortcut Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf(1, 2, 5, 10, 15, 20, 25).forEach { q ->
                        FilterChip(
                            selected = boxes == q,
                            onClick = { boxesStr = q.toString() },
                            label = { Text("$q Bx", fontSize = 11.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = boxesStr,
                        onValueChange = { boxesStr = it.filter { c -> c.isDigit() } },
                        label = { Text("पेटी / बॉक्स *") },
                        placeholder = { Text("1") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("quick_qty_boxes_input")
                    )

                    OutlinedTextField(
                        value = looseStr,
                        onValueChange = { looseStr = it.filter { c -> c.isDigit() } },
                        label = { Text("खुले पीस (Pcs)") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("quick_qty_loose_input")
                    )
                }

                OutlinedTextField(
                    value = rateStr,
                    onValueChange = { rateStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("रेट प्रति पेटी (₹ / Box) *") },
                    placeholder = { Text("350") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text("कुल: $totalPieces पीस", fontSize = 11.5.sp, color = Color(0xFF475569))
                        Text(
                            text = "कुल राशि: ₹${String.format(Locale.US, "%.2f", lineTotal)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Navy900
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (totalPieces <= 0) return@Button
                    onConfirm(boxes, loose, rate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                modifier = Modifier.testTag("quick_qty_ok_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = PureWhite)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isUpdatingExisting) "OK / अपडेट करें" else "OK / जोड़ें", color = PureWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
private fun MarbleItemDialog(
    onDismiss: () -> Unit,
    onMarbleAdded: (
        name: String,
        unit: String,
        length: Double,
        width: Double,
        pieces: Int,
        totalSqFt: Double,
        ratePerSqFt: Double
    ) -> Unit
) {
    var marbleName by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("INCH") } // "INCH" or "FEET"
    var lengthStr by remember { mutableStateOf("") }
    var widthStr by remember { mutableStateOf("") }
    var piecesStr by remember { mutableStateOf("1") }
    var ratePerSqFtStr by remember { mutableStateOf("") }

    val presets = listOf("Makrana White", "Black Granite", "Italian Marble", "Kota Stone", "Quartz", "Green Marble")

    val lengthVal = lengthStr.toDoubleOrNull() ?: 0.0
    val widthVal = widthStr.toDoubleOrNull() ?: 0.0
    val piecesVal = (piecesStr.toIntOrNull() ?: 1).coerceAtLeast(1)
    val rateVal = ratePerSqFtStr.toDoubleOrNull() ?: 0.0

    val sqFtPerSlab = if (lengthVal > 0 && widthVal > 0) {
        if (selectedUnit == "FEET") lengthVal * widthVal else (lengthVal * widthVal) / 144.0
    } else 0.0
    val totalSqFt = sqFtPerSlab * piecesVal
    val grandAmount = totalSqFt * rateVal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🪨 मार्बल / पत्थर जोड़ें (Sq.Ft Calculator)", fontWeight = FontWeight.Bold, fontSize = 15.5.sp, color = Color(0xFF92400E))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("जल्दी चुनें (Quick Presets):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = marbleName.equals(preset, ignoreCase = true),
                            onClick = { marbleName = preset },
                            label = { Text(preset, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = marbleName,
                    onValueChange = { marbleName = it },
                    label = { Text("मार्बल / पत्थर का नाम *") },
                    placeholder = { Text("उदा. Black Granite / Makrana White") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("marble_name_input")
                )

                Text("माप की इकाई (Unit):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = selectedUnit == "INCH",
                        onClick = { selectedUnit = "INCH" },
                        label = { Text("📏 इंच (Inches) ÷ 144", fontSize = 11.5.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedUnit == "FEET",
                        onClick = { selectedUnit = "FEET" },
                        label = { Text("📐 फीट (Feet)", fontSize = 11.5.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                val unitLbl = if (selectedUnit == "FEET") "फीट" else "इंच"
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = lengthStr,
                        onValueChange = { lengthStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("लंबाई ($unitLbl) *") },
                        placeholder = { Text(if (selectedUnit == "FEET") "8" else "96") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("marble_length_input")
                    )

                    OutlinedTextField(
                        value = widthStr,
                        onValueChange = { widthStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("चौड़ाई ($unitLbl) *") },
                        placeholder = { Text(if (selectedUnit == "FEET") "3" else "36") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("marble_width_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = piecesStr,
                        onValueChange = { piecesStr = it.filter { c -> c.isDigit() } },
                        label = { Text("नग / पीस (Slabs)") },
                        placeholder = { Text("1") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(0.9f).testTag("marble_pieces_input")
                    )

                    OutlinedTextField(
                        value = ratePerSqFtStr,
                        onValueChange = { ratePerSqFtStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("रेट ₹ / Sq.Ft *") },
                        placeholder = { Text("120") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f).testTag("marble_rate_input")
                    )
                }

                // Live Sq.Ft & Amount Calculation Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("प्रति नग क्षेत्रफल:", fontSize = 11.sp, color = Color(0xFF78350F))
                            Text("${String.format(Locale.US, "%.2f", sqFtPerSlab)} Sq.Ft", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("कुल क्षेत्रफल ($piecesVal नग):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                            Text("${String.format(Locale.US, "%.2f", totalSqFt)} Sq.Ft", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("कुल राशि (Total Amount):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy900)
                            Text("₹${String.format(Locale.US, "%.2f", grandAmount)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (marbleName.isBlank() || lengthVal <= 0.0 || widthVal <= 0.0) return@Button
                    onMarbleAdded(
                        marbleName.trim(),
                        selectedUnit,
                        lengthVal,
                        widthVal,
                        piecesVal,
                        totalSqFt,
                        rateVal
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
                modifier = Modifier.testTag("marble_confirm_add_button")
            ) {
                Text("बिल में जोड़ें (Add Marble)", color = PureWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
