package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.OrderEntity
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusError
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val settings by viewModel.shopSettings.collectAsStateWithLifecycle()
    val activeTheme = getAppThemeOption(settings.appTheme)

    var searchQuery by remember { mutableStateOf("") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var customerForHistory by remember { mutableStateOf<Customer?>(null) }

    val filteredCustomers = customers.filter { cust ->
        searchQuery.isBlank() ||
                cust.name.contains(searchQuery, ignoreCase = true) ||
                cust.phone.contains(searchQuery, ignoreCase = true) ||
                cust.address.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = activeTheme.primary,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Customer Directory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = PureWhite
                        )
                        Text(
                            text = "${customers.size} Registered Clients",
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
                onClick = {
                    editingCustomer = null
                    showAddEditDialog = true
                },
                containerColor = activeTheme.primary,
                contentColor = PureWhite,
                modifier = Modifier.testTag("fab_add_customer")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Customer")
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
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by customer name or phone...") },
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

            if (filteredCustomers.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.People,
                    title = if (searchQuery.isNotBlank()) "No customers match '$searchQuery'" else "No customers yet",
                    description = "Save your tile customers and contractors for fast billing and purchase history.",
                    actionLabel = "Add Customer",
                    onAction = {
                        editingCustomer = null
                        showAddEditDialog = true
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        val customerOrders = orders.filter {
                            it.customerId == customer.id ||
                                    (it.customerName.equals(customer.name, ignoreCase = true) && customer.name.isNotBlank())
                        }
                        val totalBilled = customerOrders.filter { it.status == "CONFIRMED" }.sumOf { it.grandTotal }

                        CustomerItemCard(
                            customer = customer,
                            ordersCount = customerOrders.size,
                            totalBilled = totalBilled,
                            onEdit = {
                                editingCustomer = customer
                                showAddEditDialog = true
                            },
                            onDelete = {
                                customerToDelete = customer
                            },
                            onViewHistory = {
                                customerForHistory = customer
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Customer Dialog
    if (showAddEditDialog) {
        AddEditCustomerDialog(
            customer = editingCustomer,
            onDismiss = { showAddEditDialog = false },
            onSave = { id, name, phone, address, notes, balance ->
                viewModel.saveCustomer(id, name, phone, address, notes, balance) {
                    showAddEditDialog = false
                }
            }
        )
    }

    // Delete Customer Dialog
    if (customerToDelete != null) {
        val cust = customerToDelete!!
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusError) },
            title = { Text("Delete Customer") },
            text = { Text("Are you sure you want to delete '${cust.name}'? Existing orders will retain historical customer name details.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(cust.id)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Delete", color = PureWhite)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { customerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Customer Purchase History Modal
    if (customerForHistory != null) {
        val cust = customerForHistory!!
        val customerOrders = orders.filter {
            it.customerId == cust.id ||
                    (it.customerName.equals(cust.name, ignoreCase = true) && cust.name.isNotBlank())
        }

        AlertDialog(
            onDismissRequest = { customerForHistory = null },
            title = {
                Column {
                    Text("Order History", fontWeight = FontWeight.Bold)
                    Text(cust.name, fontSize = 12.sp, color = BlueAccent)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (customerOrders.isEmpty()) {
                        Text("No orders created for this customer yet.", fontSize = 13.sp)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                            items(customerOrders) { ord ->
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            customerForHistory = null
                                            viewModel.navigateTo(Screen.OrderDetail(ord.id))
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(ord.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(ord.status, fontSize = 10.sp, color = if (ord.status == "CONFIRMED") Color(0xFF16A34A) else Color(0xFFEA580C))
                                        }
                                        Text("₹${String.format(Locale.US, "%.2f", ord.grandTotal)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { customerForHistory = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun CustomerItemCard(
    customer: Customer,
    ordersCount: Int,
    totalBilled: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = BlueAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = customer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (customer.phone.isNotBlank()) {
                            Text(
                                text = customer.phone,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = BlueAccent, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = StatusError, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (customer.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Address: ${customer.address}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$ordersCount Orders • Total: ₹${String.format(Locale.US, "%,.0f", totalBilled)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Navy900
                    )
                    Text(
                        text = "History →",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlueAccent,
                        modifier = Modifier.clickable { onViewHistory() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEditCustomerDialog(
    customer: Customer?,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, phone: String, address: String, notes: String, balance: Double) -> Unit
) {
    val isEdit = customer != null
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }
    var balanceStr by remember { mutableStateOf(customer?.outstandingBalance?.toString() ?: "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEdit) "Edit Customer" else "Add New Customer", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number (Optional)") },
                    placeholder = { Text("10 digits") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Site / Delivery Address") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Project Details") },
                    placeholder = { Text("e.g. Bungalow renovation") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val bal = balanceStr.toDoubleOrNull() ?: 0.0
                    onSave(customer?.id ?: 0L, name, phone, address, notes, bal)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900)
            ) {
                Text(if (isEdit) "Update" else "Save", color = PureWhite)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
