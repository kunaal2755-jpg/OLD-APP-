package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.screens.CustomerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.OrderDetailScreen
import com.example.ui.screens.OrderListScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SaleOrderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StaffScreen
import com.example.ui.screens.StaffDetailScreen
import com.example.ui.screens.UserLoginScreen
import androidx.compose.material.icons.filled.Badge
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.MouryaTheme
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.flow.collectLatest

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.ui.theme.getAppThemeOption

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val settings by viewModel.shopSettings.collectAsStateWithLifecycle()
            val activeTheme = getAppThemeOption(settings.appTheme)
            val snackbarHostState = remember { SnackbarHostState() }

            MouryaTheme(themeOption = activeTheme) {
                // Collect user messages from ViewModel
                LaunchedEffect(Unit) {
                    viewModel.userMessage.collectLatest { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                // Handle system back navigation
                BackHandler(enabled = currentScreen !is Screen.Dashboard && currentScreen !is Screen.Login) {
                    val handled = viewModel.navigateBack()
                    if (!handled) {
                        viewModel.navigateTo(Screen.Dashboard)
                    }
                }

                val showBottomBar = currentScreen !is Screen.Login

                val updateConfig by viewModel.updatePromptConfig.collectAsStateWithLifecycle()
                updateConfig?.let { upd ->
                    AlertDialog(
                        onDismissRequest = {
                            if (!upd.isForceUpdate) {
                                viewModel.dismissUpdatePrompt()
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🚀 नया ऐप अपडेट उपलब्ध है!", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "संस्करण: v${upd.latestAppVersionName} (Build #${upd.latestAppVersionCode})",
                                    fontWeight = FontWeight.SemiBold,
                                    color = activeTheme.primary
                                )
                                Text(
                                    text = upd.updateChangelog.ifBlank { "नया संस्करण उपलब्ध है। कृपया सभी नई सुविधाओं, सुधारों व सुरक्षा अपडेट के लिए तुरंत अपडेट करें।" },
                                    fontSize = 13.sp,
                                    color = Color(0xFF334155)
                                )
                                if (upd.isForceUpdate) {
                                    Text(
                                        text = "⚠️ यह अपडेट अनिवार्य है। आगे उपयोग करने के लिए कृपया ऐप को अपडेट करें।",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.launchUpdateDownload(this@MainActivity, upd.latestApkDownloadUrl)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("📲 तुरंत अपडेट करें (Update & Install)")
                            }
                        },
                        dismissButton = if (!upd.isForceUpdate) {
                            {
                                TextButton(onClick = { viewModel.dismissUpdatePrompt() }) {
                                    Text("बाद में (Later)")
                                }
                            }
                        } else null
                    )
                }

                val forcedLogoutAlert by viewModel.forcedLogoutReason.collectAsStateWithLifecycle()
                forcedLogoutAlert?.let { reason ->
                    AlertDialog(
                        onDismissRequest = { viewModel.clearForcedLogoutAlert() },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚠️ सुरक्षा सूचना (Alert)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFDC2626))
                            }
                        },
                        text = {
                            Text(reason, fontSize = 14.sp, color = Color(0xFF1E293B))
                        },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.clearForcedLogoutAlert() },
                                colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("ठीक है (OK)")
                            }
                        }
                    )
                }

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    containerColor = activeTheme.primary,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showBottomBar) {
                            MouryaBottomNavigation(
                                currentScreen = currentScreen,
                                onSelectScreen = { screen ->
                                    viewModel.navigateTo(screen)
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ScreenTransition",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) { targetScreen ->
                        when (targetScreen) {
                            is Screen.Login -> UserLoginScreen(
                                viewModel = viewModel
                            )
                            is Screen.Dashboard -> DashboardScreen(
                                viewModel = viewModel
                            )
                            is Screen.Inventory -> InventoryScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.navigateBack() }
                            )
                            is Screen.NewSale -> SaleOrderScreen(
                                viewModel = viewModel,
                                editOrderId = null,
                                onNavigateBack = { viewModel.navigateBack() }
                            )
                            is Screen.EditOrder -> SaleOrderScreen(
                                viewModel = viewModel,
                                editOrderId = targetScreen.orderId,
                                onNavigateBack = { viewModel.navigateBack() }
                            )
                            is Screen.Orders -> OrderListScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.navigateBack() }
                            )
                            is Screen.OrderDetail -> OrderDetailScreen(
                                orderId = targetScreen.orderId,
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.navigateBack() }
                            )
                            is Screen.Customers -> CustomerScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.navigateBack() }
                            )
                            is Screen.Reports -> ReportsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.navigateBack() }
                            )
                            is Screen.Settings -> SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.navigateBack() }
                            )
                            is Screen.Staff -> StaffScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.navigateBack() }
                            )
                            is Screen.StaffDetail -> StaffDetailScreen(
                                staffId = targetScreen.staffId,
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.navigateBack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MouryaBottomNavigation(
    currentScreen: Screen,
    onSelectScreen: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = Navy900,
        modifier = modifier.testTag("bottom_nav_bar")
    ) {
        NavigationBarItem(
            selected = currentScreen is Screen.Dashboard,
            onClick = { onSelectScreen(Screen.Dashboard) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PureWhite,
                indicatorColor = Navy900
            )
        )
        NavigationBarItem(
            selected = currentScreen is Screen.Inventory,
            onClick = { onSelectScreen(Screen.Inventory) },
            icon = { Icon(Icons.Default.Inventory2, contentDescription = "Tiles") },
            label = { Text("Tiles", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PureWhite,
                indicatorColor = Navy900
            )
        )
        NavigationBarItem(
            selected = currentScreen is Screen.Orders,
            onClick = { onSelectScreen(Screen.Orders) },
            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Sales") },
            label = { Text("Sales", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PureWhite,
                indicatorColor = Navy900
            )
        )
        NavigationBarItem(
            selected = currentScreen is Screen.Staff,
            onClick = { onSelectScreen(Screen.Staff) },
            icon = { Icon(Icons.Default.Badge, contentDescription = "Staff") },
            label = { Text("Staff / वेतन", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PureWhite,
                indicatorColor = Navy900
            )
        )
        NavigationBarItem(
            selected = currentScreen is Screen.Customers,
            onClick = { onSelectScreen(Screen.Customers) },
            icon = { Icon(Icons.Default.People, contentDescription = "Customers") },
            label = { Text("Customers", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PureWhite,
                indicatorColor = Navy900
            )
        )
    }
}
