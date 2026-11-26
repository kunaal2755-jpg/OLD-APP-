package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.ui.theme.AppThemeOption
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.OrderEntity
import com.example.data.model.StockMovement
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.BlueReports
import com.example.ui.theme.BlueTile
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GreenActive
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.OrangeOrders
import com.example.ui.theme.PureWhite
import com.example.ui.theme.PurpleCustomer
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TealStock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.shopSettings.collectAsStateWithLifecycle()
    val activeTheme = getAppThemeOption(settings.appTheme)
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val stockMovements by viewModel.stockMovements.collectAsStateWithLifecycle()
    val adminConfig by viewModel.adminGlobalConfig.collectAsStateWithLifecycle()

    val recentOrders = orders.take(5)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = activeTheme.primary,
        topBar = {
            val displayShopName = settings.shopName.ifBlank { adminConfig?.brandAppName?.ifBlank { "TILES AND MARBLES" } ?: "TILES AND MARBLES" }
            DashboardTopBar(
                shopName = displayShopName,
                subtitle = settings.subtitle,
                logoUri = settings.logoUri,
                activeTheme = activeTheme,
                onReportsClick = { viewModel.navigateTo(Screen.Reports) },
                onBackupClick = { viewModel.navigateTo(Screen.Settings) },
                onSettingsClick = { viewModel.navigateTo(Screen.Settings) }
            )
        },
        floatingActionButton = {
            // Pill Floating Action Button matching screenshot: [🛒 New Sale]
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.NewSale) },
                containerColor = activeTheme.primary,
                contentColor = PureWhite,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("fab_new_sale_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "New Sale",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Sale",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {

            // Admin Global Broadcast Announcement Banner
            if (adminConfig?.showBroadcastAnnouncement == true && !adminConfig?.broadcastAnnouncement.isNullOrBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text("📢", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "एडमिन संदेश (Important Announcement)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = adminConfig!!.broadcastAnnouncement,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF78350F)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Hero Banner Card matching Screenshot 1
            item {
                HeroBannerCard(
                    shopName = settings.shopName,
                    subtitle = settings.subtitle,
                    activeTheme = activeTheme,
                    onNewSaleClick = { viewModel.navigateTo(Screen.NewSale) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // 3. Section "Business Overview" matching Screenshot 1
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = "Business Overview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 1: 2 Cards (Available Stock & Tile Products)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OverviewLargeCard(
                            icon = Icons.Default.Inventory,
                            iconBg = Color(0xFFF1F5F9),
                            iconTint = Color(0xFF1E293B),
                            value = "${stats.availableBoxes} Boxes",
                            label = "Total Available Stock",
                            footer = "${stats.totalAvailablePieces} Total Pcs",
                            footerColor = Color(0xFF0F172A),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.navigateTo(Screen.Inventory) }
                        )

                        OverviewLargeCard(
                            icon = Icons.Default.Inventory2,
                            iconBg = Color(0xFFE0F2FE),
                            iconTint = BlueTile,
                            value = "${stats.totalProducts}",
                            label = "Tile Products",
                            footer = "Active Catalog",
                            footerColor = BlueTile,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.navigateTo(Screen.Inventory) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 2: 3 Cards (Confirmed Sales, Pending Orders, Customers)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OverviewSmallCard(
                            icon = Icons.Default.Check,
                            iconBg = Color(0xFFDCFCE7),
                            iconTint = StatusSuccess,
                            value = "${stats.confirmedSalesCount}",
                            label = "Confirmed Sales",
                            status = "Dispatched",
                            statusColor = StatusSuccess,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.navigateTo(Screen.Orders) }
                        )

                        OverviewSmallCard(
                            icon = Icons.Default.HourglassBottom,
                            iconBg = Color(0xFFFFEDD5),
                            iconTint = StatusPending,
                            value = "${stats.pendingOrdersCount}",
                            label = "Pending Orders",
                            status = "Awaiting Sale",
                            statusColor = StatusPending,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.navigateTo(Screen.Orders) }
                        )

                        OverviewSmallCard(
                            icon = Icons.Default.People,
                            iconBg = Color(0xFFEDE9FE),
                            iconTint = PurpleCustomer,
                            value = "${stats.totalCustomers}",
                            label = "Customers",
                            status = "Registered",
                            statusColor = PurpleCustomer,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.navigateTo(Screen.Customers) }
                        )
                    }
                }
            }

            // 4. Section "Quick Actions" matching Screenshot 2
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "Quick Actions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 1 of Quick Actions: [🛒 New Sale] [➕ Add Tile] [👥 Customer]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionCard(
                            label = "New Sale",
                            icon = Icons.Default.ShoppingCart,
                            iconTint = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(Screen.NewSale) }
                        )
                        QuickActionCard(
                            label = "Add Tile",
                            icon = Icons.Default.Add,
                            iconTint = BlueTile,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(Screen.Inventory) }
                        )
                        QuickActionCard(
                            label = "Customer",
                            icon = Icons.Default.People,
                            iconTint = PurpleCustomer,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(Screen.Customers) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2 of Quick Actions: [📦 Stock] [🧾 Orders] [📊 Reports]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionCard(
                            label = "Stock",
                            icon = Icons.Default.Inventory,
                            iconTint = TealStock,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(Screen.Inventory) }
                        )
                        QuickActionCard(
                            label = "Orders",
                            icon = Icons.Default.ReceiptLong,
                            iconTint = OrangeOrders,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(Screen.Orders) }
                        )
                        QuickActionCard(
                            label = "Reports",
                            icon = Icons.Default.Assessment,
                            iconTint = BlueReports,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(Screen.Reports) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 3 of Quick Actions: [👥 Staff & Salary (कर्मचारी व वेतन हिसाब)]
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = activeTheme.primaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(Screen.Staff) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(activeTheme.accent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = activeTheme.accentText,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "दुकान कर्मचारी, हाजिरी व वेतन रजिस्टर",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PureWhite
                                    )
                                    Text(
                                        text = "Staff Attendance, Advance (पैसा लिया) & Salary Ledger",
                                        fontSize = 11.sp,
                                        color = activeTheme.accent
                                    )
                                }
                            }
                            Text("खोलें →", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PureWhite)
                        }
                    }
                }
            }

            // 5. Section "Recent Sales & Orders" matching Screenshot 2
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Recent Sales & Orders",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "View All",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900,
                            modifier = Modifier
                                .clickable { viewModel.navigateTo(Screen.Orders) }
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (recentOrders.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No sales orders created yet",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.navigateTo(Screen.NewSale) },
                                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "Create First Sale Order",
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                items(recentOrders) { order ->
                    RecentOrderRowCard(
                        order = order,
                        onClick = { viewModel.navigateTo(Screen.OrderDetail(order.id)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // 6. Section "Recent Stock Movements" matching Screenshot 2
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Recent Stock Movements",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Stock Audit",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900,
                            modifier = Modifier
                                .clickable { viewModel.navigateTo(Screen.Inventory) }
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (stockMovements.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "No stock changes recorded yet.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(stockMovements.take(4)) { movement ->
                    StockMovementCard(
                        movement = movement,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }
    }
}

@Composable
private fun DashboardTopBar(
    shopName: String,
    subtitle: String,
    logoUri: String?,
    activeTheme: AppThemeOption,
    onReportsClick: () -> Unit,
    onBackupClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(activeTheme.primary)
            .statusBarsPadding()
            .padding(top = 10.dp, bottom = 12.dp, start = 16.dp, end = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Circular Golden/Accent Emblem or custom uploaded shop logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(activeTheme.accent)
            ) {
                if (!logoUri.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(logoUri),
                        contentDescription = "Shop Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val initial = shopName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "T"
                    Text(
                        text = initial,
                        color = activeTheme.accentText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shopName.ifBlank { "TILES AND MARBLES" },
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = activeTheme.accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Reports icon (bar chart)
            IconButton(
                onClick = onReportsClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = "Reports",
                    tint = PureWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Cloud/Backup icon
            IconButton(
                onClick = onBackupClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Cloud & Backup",
                    tint = activeTheme.accent,
                    modifier = Modifier.size(23.dp)
                )
            }

            // Settings button (Gear icon) requested by user
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("dashboard_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings & Customization",
                    tint = PureWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroBannerCard(
    shopName: String,
    subtitle: String,
    activeTheme: AppThemeOption,
    onNewSaleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = activeTheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Active badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(GreenActive)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OFFLINE BUSINESS MANAGEMENT ACTIVE",
                    color = GreenActive,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Title
            Text(
                text = shopName.ifBlank { "TILES AND MARBLES" },
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            // Subtitle - only show if user entered one
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = activeTheme.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body text
            Text(
                text = "Manage inventory, multi-tile orders, quantity challans, stock balances, and PDF invoices.",
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Golden/Accent CTA button "+ New Sale Order" matching Screenshot 1
            Button(
                onClick = onNewSaleClick,
                colors = ButtonDefaults.buttonColors(containerColor = activeTheme.accent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("hero_new_sale_order_btn")
            ) {
                Text(
                    text = "+ New Sale Order",
                    color = activeTheme.accentText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun OverviewLargeCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    value: String,
    label: String,
    footer: String,
    footerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = footer,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = footerColor
            )
        }
    }
}

@Composable
private fun OverviewSmallCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    value: String,
    label: String,
    status: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = status,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0))
        ),
        shadowElevation = 1.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecentOrderRowCard(
    order: OrderEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.customerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = order.status)
                }
                Spacer(modifier = Modifier.height(2.dp))
                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                Text(
                    text = "${order.invoiceNumber} • ${sdf.format(Date(order.createdAt))}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", order.grandTotal)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Navy900
                )
                Text(
                    text = if (order.showPriceOnPdf) "Prices ON" else "Quantity Slip",
                    fontSize = 10.sp,
                    color = if (order.showPriceOnPdf) StatusSuccess else Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun StockMovementCard(
    movement: StockMovement,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movement.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = movement.description,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Text(
                text = movement.changeDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (movement.isPositive) StatusSuccess else StatusError
            )
        }
    }
}
