package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedButton
import com.example.ui.theme.getAppThemeOption
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OrderEntity
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val settings by viewModel.shopSettings.collectAsStateWithLifecycle()
    val activeTheme = getAppThemeOption(settings.appTheme)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("ALL") } // "ALL", "CONFIRMED", "PENDING", "DELIVERY_PENDING"

    val filteredOrders = orders.filter { order ->
        val matchesQuery = searchQuery.isBlank() ||
                order.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                order.customerName.contains(searchQuery, ignoreCase = true) ||
                order.customerPhone.contains(searchQuery, ignoreCase = true)

        val matchesTab = when (selectedTab) {
            "CONFIRMED" -> order.status == "CONFIRMED"
            "PENDING" -> order.status == "PENDING"
            "DELIVERY_PENDING" -> order.deliveryStatus != "FULL_DELIVERED"
            else -> true
        }

        matchesQuery && matchesTab
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = activeTheme.primary,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Orders & Sales Bills",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = PureWhite
                        )
                        Text(
                            text = "${orders.count { it.status == "CONFIRMED" }} Confirmed • ${orders.count { it.status == "PENDING" }} Pending",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.NewSale) },
                containerColor = activeTheme.primary,
                contentColor = PureWhite,
                modifier = Modifier.testTag("fab_new_sale")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Sale")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by invoice # or customer name...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTab == "ALL",
                        onClick = { selectedTab = "ALL" },
                        label = { Text("All (${orders.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTab == "DELIVERY_PENDING",
                        onClick = { selectedTab = "DELIVERY_PENDING" },
                        label = { Text("📦 बाकी डिलीवरी (${orders.count { it.deliveryStatus != "FULL_DELIVERED" }})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTab == "CONFIRMED",
                        onClick = { selectedTab = "CONFIRMED" },
                        label = { Text("Confirmed Sales (${orders.count { it.status == "CONFIRMED" }})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTab == "PENDING",
                        onClick = { selectedTab = "PENDING" },
                        label = { Text("Pending Orders (${orders.count { it.status == "PENDING" }})") }
                    )
                }
            }

            if (filteredOrders.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Receipt,
                    title = if (searchQuery.isNotBlank()) "No orders match '$searchQuery'" else "No orders found",
                    description = "Create a new sale order for your tile customers.",
                    actionLabel = "Create New Sale",
                    onAction = { viewModel.navigateTo(Screen.NewSale) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredOrders, key = { it.id }) { order ->
                        OrderCardItem(
                            order = order,
                            onClick = { viewModel.navigateTo(Screen.OrderDetail(order.id)) },
                            onEditClick = { viewModel.navigateTo(Screen.EditOrder(order.id)) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCardItem(
    order: OrderEntity,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.invoiceNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Navy900
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = order.status)
                }

                Text(
                    text = "₹${String.format(Locale.US, "%.2f", order.grandTotal)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = order.customerName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (order.customerPhone.isNotBlank()) {
                        Text(
                            text = "Ph: ${order.customerPhone}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    Text(
                        text = sdf.format(Date(order.createdAt)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (order.showPriceOnPdf) "PDF: Prices ON" else "PDF: Quantity Slip",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (order.showPriceOnPdf) StatusSuccess else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val (delivLabel, delivBg, delivFg) = when (order.deliveryStatus) {
                    "NOT_DELIVERED" -> Triple("📦 डिलीवरी बाकी (Hold)", Color(0xFFFEE2E2), Color(0xFFDC2626))
                    "PARTIAL_PENDING" -> Triple("🚚 आंशिक डिलीवरी / बाकी", Color(0xFFFFEDD5), Color(0xFFC2410C))
                    else -> Triple("✅ पूरी डिलीवरी", Color(0xFFDCFCE7), Color(0xFF15803D))
                }
                Surface(
                    color = delivBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = delivLabel,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = delivFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                OutlinedButton(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("order_card_edit_btn_${order.id}")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit / एडिट करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
