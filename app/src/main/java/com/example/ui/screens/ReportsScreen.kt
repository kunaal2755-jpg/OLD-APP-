package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.ui.theme.getAppThemeOption
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OrderItemEntity
import com.example.ui.MainViewModel
import com.example.ui.components.StatCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess
import com.example.util.ShareHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val settings by viewModel.shopSettings.collectAsStateWithLifecycle()

    var allOrderItems by remember { mutableStateOf<List<OrderItemEntity>>(emptyList()) }
    var timeFilter by remember { mutableStateOf("ALL") } // "TODAY", "WEEK", "MONTH", "ALL"

    LaunchedEffect(orders) {
        allOrderItems = viewModel.repository.getAllDataForBackup()["items"] as? List<OrderItemEntity> ?: emptyList()
    }

    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L
    val oneWeekMs = 7 * oneDayMs
    val oneMonthMs = 30 * oneDayMs

    val filteredOrders = orders.filter { o ->
        when (timeFilter) {
            "TODAY" -> (now - o.createdAt) <= oneDayMs
            "WEEK" -> (now - o.createdAt) <= oneWeekMs
            "MONTH" -> (now - o.createdAt) <= oneMonthMs
            else -> true
        }
    }

    val confirmedOrders = filteredOrders.filter { it.status == "CONFIRMED" }
    val pendingOrders = filteredOrders.filter { it.status == "PENDING" }

    val totalConfirmedRevenue = confirmedOrders.sumOf { it.grandTotal }
    val totalPendingValue = pendingOrders.sumOf { it.grandTotal }

    // Boxes sold by tile size (from confirmed orders)
    val confirmedOrderIds = confirmedOrders.map { it.id }.toSet()
    val relevantItems = allOrderItems.filter { it.orderId in confirmedOrderIds }

    val boxesBySize = relevantItems.groupBy { it.tileSize }
        .mapValues { entry -> entry.value.sumOf { it.fullBoxes } }

    val totalBoxesSold = relevantItems.sumOf { it.fullBoxes }
    val totalLoosePiecesSold = relevantItems.sumOf { it.loosePieces }

    // Total warehouse stock
    val warehouseTotalBoxes = products.sumOf { it.fullBoxes }
    val warehouseTotalLoose = products.sumOf { it.loosePieces }
    val warehouseTotalPieces = products.sumOf { it.stockPieces }

    fun shareReport() {
        val summaryText = buildString {
            append("*${settings.shopName}* - Business Sales & Stock Report\n")
            append("Period: $timeFilter\n\n")
            append("📊 *Sales Summary*\n")
            append("• Confirmed Sales: ${confirmedOrders.size} (₹${String.format(Locale.US, "%,.2f", totalConfirmedRevenue)})\n")
            append("• Pending Orders: ${pendingOrders.size} (₹${String.format(Locale.US, "%,.2f", totalPendingValue)})\n")
            append("• Total Boxes Sold: $totalBoxesSold Bx\n\n")
            append("📦 *Stock by Size Sold*\n")
            boxesBySize.forEach { (size, bxs) ->
                append("• $size: $bxs Boxes\n")
            }
            append("\n🏢 *Warehouse Inventory Status*\n")
            append("• Total Stock: $warehouseTotalBoxes Boxes + $warehouseTotalLoose Loose Pcs\n")
            append("• Total Pieces in Stock: $warehouseTotalPieces\n")
            append("• Active Tile Catalog: ${products.size} products\n")
        }

        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, summaryText)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Mourya Tiles - Business Report")
        }
        val chooser = android.content.Intent.createChooser(sendIntent, "Share Report via")
        context.startActivity(chooser)
    }

    val activeTheme = getAppThemeOption(settings.appTheme)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = activeTheme.primary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Business Intelligence & Reports",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = PureWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { shareReport() }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Report", tint = PureWhite)
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
            // Period Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = timeFilter == "TODAY",
                            onClick = { timeFilter = "TODAY" },
                            label = { Text("Today") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = timeFilter == "WEEK",
                            onClick = { timeFilter = "WEEK" },
                            label = { Text("Last 7 Days") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = timeFilter == "MONTH",
                            onClick = { timeFilter = "MONTH" },
                            label = { Text("Last 30 Days") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = timeFilter == "ALL",
                            onClick = { timeFilter = "ALL" },
                            label = { Text("All Time") }
                        )
                    }
                }
            }

            // Stat Cards Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Confirmed Revenue",
                        value = "₹${String.format(Locale.US, "%,.0f", totalConfirmedRevenue)}",
                        subtitle = "${confirmedOrders.size} sales confirmed",
                        icon = Icons.Default.MonetizationOn,
                        accentColor = StatusSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Boxes Sold",
                        value = "$totalBoxesSold Bx",
                        subtitle = if (totalLoosePiecesSold > 0) "+ $totalLoosePiecesSold Loose Pcs" else "Confirmed orders",
                        icon = Icons.Default.Inventory2,
                        accentColor = BlueAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Pending Pipeline",
                        value = "₹${String.format(Locale.US, "%,.0f", totalPendingValue)}",
                        subtitle = "${pendingOrders.size} pending orders",
                        icon = Icons.Default.HourglassEmpty,
                        accentColor = StatusPending,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Total Stock Available",
                        value = "$warehouseTotalBoxes Bx",
                        subtitle = "$warehouseTotalPieces total pieces",
                        icon = Icons.Default.Assessment,
                        accentColor = AmberGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Boxes Sold by Tile Size breakdown
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Boxes Sold by Tile Size (Confirmed Orders)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (boxesBySize.isEmpty()) {
                            Text(
                                text = "No confirmed orders with tile items in this time period.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            boxesBySize.forEach { (size, boxes) ->
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
                                            text = size,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "$boxes Boxes Sold",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Navy900
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Warehouse Inventory & Low Stock Overview
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Warehouse Stock Status",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val lowStockList = products.filter { it.isLowStock }
                        if (lowStockList.isNotEmpty()) {
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "⚠️ Low Stock Products (${lowStockList.size}):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = StatusError
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    lowStockList.forEach { p ->
                                        Text(
                                            text = "• ${p.name} (${p.size}): ${p.stockDisplay} remaining",
                                            fontSize = 11.sp,
                                            color = Color(0xFF991B1B)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text(
                            text = "Total Catalog Products: ${products.size}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Total Warehouse Pieces: $warehouseTotalPieces Pcs",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Share Report Button
            item {
                Button(
                    onClick = { shareReport() },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = PureWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Business Report Summary", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
