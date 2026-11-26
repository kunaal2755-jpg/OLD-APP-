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
import com.example.ui.components.PdfPriceVisibilitySwitchCard
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
    var isPremiumHighlighter: Boolean = false
) {
    val totalPieces: Int get() = (fullBoxes * product.piecesPerBox) + loosePieces
    val lineTotal: Double
        get() {
            val boxTotal = fullBoxes * pricePerBox
            val pieceRate = if (product.piecesPerBox > 0) pricePerBox / product.piecesPerBox else 0.0
            val looseTotal = loosePieces * pieceRate
            return boxTotal + looseTotal
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
    var discountStr by remember { mutableStateOf("") }
    var paidAmountStr by remember { mutableStateOf("") }
    var orderStatus by remember { mutableStateOf("PENDING") }
    var isInitialized by remember { mutableStateOf(false) }

    val orderItems = remember { mutableStateListOf<DraftOrderItem>() }
    var showTilePickerModal by remember { mutableStateOf(false) }
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
                        pricePerBox = item.pricePerBox
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

            // 2. Per-Order PDF Price Visibility Switch
            item {
                PdfPriceVisibilitySwitchCard(
                    showPrice = showPriceOnPdf,
                    onToggle = { showPriceOnPdf = it }
                )
            }

            // 3. Tile Items Header & "Add Tile" button
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Tile Products in Order",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${orderItems.size} tiles selected",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showManualItemModal = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("button_add_manual_item")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("मैन्युअल आइटम", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showTilePickerModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("button_add_tile_to_order")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Tile", fontSize = 12.sp)
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
                // Quick Size Pricing Bar (Customize rate in 1-click for 12x18 and other sizes)
                item {
                    val uniqueSizes = orderItems.map { it.product.size.trim() }.distinct().filter { it.isNotBlank() }
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

                itemsIndexed(orderItems) { index, draftItem ->
                    DraftOrderItemCard(
                        item = draftItem,
                        onQuantityChanged = { fullBoxes, loosePieces ->
                            orderItems[index] = draftItem.copy(fullBoxes = fullBoxes, loosePieces = loosePieces)
                        },
                        onPriceChanged = { price ->
                            orderItems[index] = draftItem.copy(pricePerBox = price)
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
                                    val entityItems = orderItems.map { draft ->
                                        OrderItemEntity(
                                            orderId = editOrderId,
                                            productId = draft.product.id,
                                            tileCode = draft.product.code,
                                            tileName = draft.product.name,
                                            tileSize = draft.product.size,
                                            piecesPerBox = draft.product.piecesPerBox,
                                            tilePhotoUri = draft.product.photoUri,
                                            fullBoxes = draft.fullBoxes,
                                            loosePieces = draft.loosePieces,
                                            pricePerBox = draft.pricePerBox,
                                            lineTotal = draft.lineTotal
                                        )
                                    }
                                    val existingOrder = existingOrderWithItems?.order
                                    val currentPaid = if (paidAmountStr.isNotBlank()) paidAmount else (existingOrder?.paidAmount ?: 0.0)
                                    val currentBal = (grandTotal - currentPaid).coerceAtLeast(0.0)
                                    val updatedOrder = existingOrder?.copy(
                                        customerName = customerName.trim().ifBlank { "Cash Customer" },
                                        customerPhone = customerPhone.trim(),
                                        customerAddress = customerAddress.trim(),
                                        status = orderStatus,
                                        showPriceOnPdf = showPriceOnPdf,
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
                            // Confirm & Deduct Stock immediately
                            Button(
                                onClick = {
                                    if (customerName.isBlank()) {
                                        viewModel.showMessage("Please enter customer name")
                                        return@Button
                                    }
                                    val entityItems = orderItems.map { draft ->
                                        OrderItemEntity(
                                            productId = draft.product.id,
                                            tileCode = draft.product.code,
                                            tileName = draft.product.name,
                                            tileSize = draft.product.size,
                                            piecesPerBox = draft.product.piecesPerBox,
                                            tilePhotoUri = draft.product.photoUri,
                                            fullBoxes = draft.fullBoxes,
                                            loosePieces = draft.loosePieces,
                                            pricePerBox = draft.pricePerBox,
                                            lineTotal = draft.lineTotal
                                        )
                                    }
                                    viewModel.createSaleOrder(
                                        invoiceNumber = invoiceNumber,
                                        customerId = selectedCustomer?.id,
                                        customerName = customerName,
                                        customerPhone = customerPhone,
                                        customerAddress = customerAddress,
                                        status = "CONFIRMED",
                                        showPriceOnPdf = showPriceOnPdf,
                                        subtotal = subtotal,
                                        discount = discount,
                                        grandTotal = grandTotal,
                                        paidAmount = paidAmount,
                                        notes = orderNotes,
                                        items = entityItems,
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
                                Text("Confirm Sale & Deduct Stock", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PureWhite)
                            }

                            // Save as Pending (Estimate) without stock deduction
                            OutlinedButton(
                                onClick = {
                                    if (customerName.isBlank()) {
                                        viewModel.showMessage("Please enter customer name")
                                        return@OutlinedButton
                                    }
                                    val entityItems = orderItems.map { draft ->
                                        OrderItemEntity(
                                            productId = draft.product.id,
                                            tileCode = draft.product.code,
                                            tileName = draft.product.name,
                                            tileSize = draft.product.size,
                                            piecesPerBox = draft.product.piecesPerBox,
                                            tilePhotoUri = draft.product.photoUri,
                                            fullBoxes = draft.fullBoxes,
                                            loosePieces = draft.loosePieces,
                                            pricePerBox = draft.pricePerBox,
                                            lineTotal = draft.lineTotal
                                        )
                                    }
                                    viewModel.createSaleOrder(
                                        invoiceNumber = invoiceNumber,
                                        customerId = selectedCustomer?.id,
                                        customerName = customerName,
                                        customerPhone = customerPhone,
                                        customerAddress = customerAddress,
                                        status = "PENDING",
                                        showPriceOnPdf = showPriceOnPdf,
                                        subtotal = subtotal,
                                        discount = discount,
                                        grandTotal = grandTotal,
                                        paidAmount = paidAmount,
                                        notes = orderNotes,
                                        items = entityItems,
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

    // Modal: Select Tile from Inventory
    if (showTilePickerModal) {
        TileProductPickerModal(
            products = products,
            onDismiss = { showTilePickerModal = false },
            onTileSelected = { product ->
                // Check if already in order
                val existing = orderItems.find { it.product.id == product.id }
                if (existing != null) {
                    viewModel.showMessage("${product.name} is already in the order.")
                } else {
                    // Smart size pricing suggestion
                    val sizeDefault = sizePricings.find { it.sizeName.equals(product.size, ignoreCase = true) }?.defaultPricePerBox
                    val initialPrice = when {
                        product.defaultSellingPrice > 0 -> product.defaultSellingPrice
                        sizeDefault != null && sizeDefault > 0 -> sizeDefault
                        else -> 350.0
                    }
                    val isHighlighterInitial = product.name.contains("highlighter", ignoreCase = true) ||
                            product.name.contains("hl", ignoreCase = true) ||
                            product.name.contains("decor", ignoreCase = true) ||
                            product.name.contains("dark", ignoreCase = true) ||
                            product.name.contains("premium", ignoreCase = true)
                    orderItems.add(
                        DraftOrderItem(
                            product = product,
                            fullBoxes = 1,
                            loosePieces = 0,
                            pricePerBox = initialPrice,
                            isPremiumHighlighter = isHighlighterInitial
                        )
                    )
                }
                showTilePickerModal = false
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
                        pricePerBox = rate
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
    var boxesStr by remember(item.fullBoxes) { mutableStateOf(if (item.fullBoxes > 0) item.fullBoxes.toString() else "") }
    var looseStr by remember(item.loosePieces) { mutableStateOf(if (item.loosePieces > 0) item.loosePieces.toString() else "") }
    var priceStr by remember(item.pricePerBox) { mutableStateOf(if (item.pricePerBox > 0) item.pricePerBox.toString() else "") }

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                ) {
                    if (!item.product.photoUri.isNullOrBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(item.product.photoUri),
                            contentDescription = item.product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
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
                    Text(
                        text = item.product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "Code: ${item.product.code} | Size: ${item.product.size} (${item.product.piecesPerBox} Pcs/Bx)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Available Warehouse Stock: ${item.product.stockDisplay}",
                        fontSize = 11.sp,
                        color = if (item.product.isLowStock) StatusError else Color(0xFF16A34A)
                    )
                }

                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = StatusError)
                }
            }

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

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity inputs: Full Boxes & Loose Pieces
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

            Spacer(modifier = Modifier.height(6.dp))

            // Internal calculation footer
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Internal: ${item.totalPieces} total pieces to adjust",
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
