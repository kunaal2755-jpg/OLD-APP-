package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.OrderWithItems
import com.example.data.model.RecycleBinItem
import com.example.data.model.ShopSettings
import com.example.data.model.SizePricing
import com.example.data.model.StaffAttendance
import com.example.data.model.StaffMember
import com.example.data.model.StaffPayment
import com.example.data.model.TileProduct
import com.example.util.BackupRestoreHelper
import com.example.util.TelegramNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class MouryaRepository(val database: AppDatabase) {

    private val settingsDao = database.shopSettingsDao()
    private val sizePricingDao = database.sizePricingDao()
    private val productDao = database.tileProductDao()
    private val customerDao = database.customerDao()
    private val orderDao = database.orderDao()
    private val orderItemDao = database.orderItemDao()
    private val stockMovementDao = database.stockMovementDao()
    private val clientLicenseDao = database.clientLicenseDao()
    val appUserDao = database.appUserDao()
    val recycleBinDao = database.recycleBinDao()
    private val staffDao = database.staffDao()
    val adminGlobalConfigDao = database.adminGlobalConfigDao()

    val adminGlobalConfig: Flow<com.example.data.model.AdminGlobalConfig?> = adminGlobalConfigDao.getConfig()

    suspend fun saveAdminGlobalConfig(context: Context, config: com.example.data.model.AdminGlobalConfig) {
        adminGlobalConfigDao.saveConfig(config)
        com.example.util.PermanentVaultHelper.saveAdminConfig(context, config)
        com.example.util.PermanentVaultHelper.publishCloudAdminConfig(config)
    }

    val stockMovements: Flow<List<com.example.data.model.StockMovement>> = stockMovementDao.getRecentMovements()

    suspend fun recordMovement(title: String, desc: String, changeDisplay: String, isPositive: Boolean) {
        stockMovementDao.insert(
            com.example.data.model.StockMovement(
                title = title,
                description = desc,
                changeDisplay = changeDisplay,
                isPositive = isPositive
            )
        )
    }

    // ------------------------------------------------------------------------
    // Shop Settings
    // ------------------------------------------------------------------------
    val shopSettings: Flow<ShopSettings> = settingsDao.getSettings().map { it ?: ShopSettings() }

    suspend fun getCurrentShopSettings(): ShopSettings {
        return settingsDao.getSettingsSync() ?: ShopSettings()
    }

    suspend fun saveSettings(settings: ShopSettings) {
        settingsDao.saveSettings(settings)
    }

    // ------------------------------------------------------------------------
    // Size Pricing Rules
    // ------------------------------------------------------------------------
    val allSizePricings: Flow<List<SizePricing>> = sizePricingDao.getAllPricings()

    suspend fun saveSizePricing(sizeName: String, pricePerBox: Double, piecesPerBox: Int = 0): Long {
        val existing = sizePricingDao.getPricingForSize(sizeName.trim())
        return if (existing != null) {
            sizePricingDao.update(existing.copy(defaultPricePerBox = pricePerBox, defaultPiecesPerBox = piecesPerBox))
            existing.id
        } else {
            sizePricingDao.insert(SizePricing(sizeName = sizeName.trim(), defaultPricePerBox = pricePerBox, defaultPiecesPerBox = piecesPerBox))
        }
    }

    suspend fun deleteSizePricing(id: Long) {
        sizePricingDao.deleteById(id)
    }

    suspend fun getPricingForSize(sizeName: String): Double? {
        return sizePricingDao.getPricingForSize(sizeName.trim())?.defaultPricePerBox
    }

    suspend fun updateProductsOfSize(sizeName: String, newPrice: Double, newPiecesPerBox: Int): Int {
        val allProds = productDao.getAllProductsSync()
        var count = 0
        allProds.filter { it.size.equals(sizeName.trim(), ignoreCase = true) }.forEach { prod ->
            val updated = prod.copy(
                defaultSellingPrice = if (newPrice > 0) newPrice else prod.defaultSellingPrice,
                piecesPerBox = if (newPiecesPerBox > 0) newPiecesPerBox else prod.piecesPerBox
            )
            productDao.update(updated)
            count++
        }
        return count
    }

    // ------------------------------------------------------------------------
    // Tile Products & Stock
    // ------------------------------------------------------------------------
    val allProducts: Flow<List<TileProduct>> = productDao.getAllProducts()

    suspend fun getProductById(id: Long): TileProduct? = productDao.getProductById(id)

    suspend fun addProduct(product: TileProduct): Result<Long> {
        val existing = productDao.getProductByCode(product.code.trim())
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Tile code '${product.code}' already exists!"))
        }
        val id = productDao.insert(product.copy(code = product.code.trim()))
        return Result.success(id)
    }

    suspend fun updateProduct(product: TileProduct): Result<Unit> {
        val existing = productDao.getProductByCode(product.code.trim())
        if (existing != null && existing.id != product.id) {
            return Result.failure(IllegalArgumentException("Tile code '${product.code}' is used by another product!"))
        }
        productDao.update(product.copy(code = product.code.trim()))
        return Result.success(Unit)
    }

    suspend fun deleteProduct(id: Long): Result<Unit> {
        val prod = productDao.getProductById(id)
        if (prod != null) {
            val json = JSONObject().apply {
                put("id", prod.id)
                put("code", prod.code)
                put("name", prod.name)
                put("size", prod.size)
                put("photoUri", prod.photoUri ?: "")
                put("description", prod.description)
                put("piecesPerBox", prod.piecesPerBox)
                put("stockPieces", prod.stockPieces)
                put("defaultSellingPrice", prod.defaultSellingPrice)
                put("purchaseCost", prod.purchaseCost)
                put("createdAt", prod.createdAt)
            }.toString()

            recycleBinDao.insert(
                RecycleBinItem(
                    itemType = "PRODUCT",
                    originalId = id,
                    title = "टाइल्स: ${prod.name}",
                    subtitle = "कोड: ${prod.code} • ${prod.stockDisplay}",
                    payloadJson = json
                )
            )
        }
        productDao.deleteById(id)
        return Result.success(Unit)
    }

    suspend fun adjustDirectStock(productId: Long, newStockPieces: Int) {
        productDao.updateStock(productId, newStockPieces.coerceAtLeast(0))
    }

    // ------------------------------------------------------------------------
    // Customers
    // ------------------------------------------------------------------------
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    suspend fun saveCustomer(customer: Customer): Long {
        return if (customer.id == 0L) {
            customerDao.insert(customer)
        } else {
            customerDao.update(customer)
            customer.id
        }
    }

    suspend fun deleteCustomer(id: Long) {
        val cust = customerDao.getCustomerById(id)
        if (cust != null) {
            val json = JSONObject().apply {
                put("id", cust.id)
                put("name", cust.name)
                put("phone", cust.phone)
                put("address", cust.address)
                put("notes", cust.notes)
                put("outstandingBalance", cust.outstandingBalance)
                put("createdAt", cust.createdAt)
            }.toString()

            recycleBinDao.insert(
                RecycleBinItem(
                    itemType = "CUSTOMER",
                    originalId = id,
                    title = "ग्राहक: ${cust.name}",
                    subtitle = "फोन: ${cust.phone} • बैलेंस: ₹${cust.outstandingBalance.toInt()}",
                    payloadJson = json
                )
            )
        }
        customerDao.deleteById(id)
    }

    // ------------------------------------------------------------------------
    // Orders & Transactions
    // ------------------------------------------------------------------------
    val allOrders: Flow<List<OrderEntity>> = orderDao.getAllOrders()

    fun getOrderWithItems(orderId: Long): Flow<OrderWithItems?> {
        return combine(
            allOrders,
            orderItemDao.getItemsForOrder(orderId)
        ) { orders, items ->
            val order = orders.find { it.id == orderId }
            if (order != null) OrderWithItems(order, items) else null
        }
    }

    suspend fun getOrderWithItemsSync(orderId: Long): OrderWithItems? {
        val order = orderDao.getOrderById(orderId) ?: return null
        val items = orderItemDao.getItemsForOrderSync(orderId)
        return OrderWithItems(order, items)
    }

    suspend fun createOrder(
        order: OrderEntity,
        items: List<OrderItemEntity>
    ): Result<Long> {
        return try {
            database.withTransaction {
                val isConfirmed = order.status == "CONFIRMED"

                if (isConfirmed) {
                    // Check stock for all items
                    for (item in items) {
                        val product = productDao.getProductById(item.productId)
                            ?: return@withTransaction Result.failure(
                                IllegalStateException("Product '${item.tileName}' not found.")
                            )
                        val requiredPieces = item.totalPieces
                        if (product.stockPieces < requiredPieces) {
                            return@withTransaction Result.failure(
                                IllegalStateException(
                                    "Insufficient stock for ${product.name}! Available: ${product.stockDisplay}, Required: ${item.fullBoxes} Bx + ${item.loosePieces} Pcs"
                                )
                            )
                        }
                    }

                    // Deduct stock
                    for (item in items) {
                        val product = productDao.getProductById(item.productId)!!
                        val newStock = product.stockPieces - item.totalPieces
                        productDao.updateStock(product.id, newStock)
                    }
                }

                val orderToSave = if (isConfirmed) {
                    order.copy(confirmedAt = System.currentTimeMillis())
                } else {
                    order
                }

                val orderId = orderDao.insert(orderToSave)
                val itemsWithOrderId = items.map { it.copy(orderId = orderId) }
                orderItemDao.insertAll(itemsWithOrderId)

                Result.success(orderId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmPendingOrder(orderId: Long): Result<Unit> {
        return try {
            database.withTransaction {
                val order = orderDao.getOrderById(orderId)
                    ?: return@withTransaction Result.failure(IllegalStateException("Order not found."))
                if (order.status == "CONFIRMED") {
                    return@withTransaction Result.failure(IllegalStateException("Order is already confirmed."))
                }

                val items = orderItemDao.getItemsForOrderSync(orderId)

                // Validate stock
                for (item in items) {
                    val product = productDao.getProductById(item.productId)
                        ?: return@withTransaction Result.failure(
                            IllegalStateException("Product '${item.tileName}' not found.")
                        )
                    if (product.stockPieces < item.totalPieces) {
                        return@withTransaction Result.failure(
                            IllegalStateException(
                                "Insufficient stock for ${product.name}! Available: ${product.stockDisplay}, Required: ${item.fullBoxes} Bx + ${item.loosePieces} Pcs"
                            )
                        )
                    }
                }

                // Deduct stock
                for (item in items) {
                    val product = productDao.getProductById(item.productId)!!
                    productDao.updateStock(product.id, product.stockPieces - item.totalPieces)
                }

                orderDao.updateStatus(orderId, "CONFIRMED", System.currentTimeMillis())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrder(
        updatedOrder: OrderEntity,
        newItems: List<OrderItemEntity>
    ): Result<Unit> {
        return try {
            database.withTransaction {
                val oldOrder = orderDao.getOrderById(updatedOrder.id)
                    ?: return@withTransaction Result.failure(IllegalStateException("Order not found"))
                val oldItems = orderItemDao.getItemsForOrderSync(updatedOrder.id)

                val wasConfirmed = oldOrder.status == "CONFIRMED"
                val willBeConfirmed = updatedOrder.status == "CONFIRMED"

                if (wasConfirmed && !willBeConfirmed) {
                    // Revert old stock
                    for (item in oldItems) {
                        val product = productDao.getProductById(item.productId)
                        if (product != null) {
                            productDao.updateStock(product.id, product.stockPieces + item.totalPieces)
                        }
                    }
                } else if (wasConfirmed && willBeConfirmed) {
                    // Calculate deltas
                    val oldPiecesMap = oldItems.groupBy { it.productId }.mapValues { entry -> entry.value.sumOf { it.totalPieces } }
                    val newPiecesMap = newItems.groupBy { it.productId }.mapValues { entry -> entry.value.sumOf { it.totalPieces } }

                    val allProductIds = (oldPiecesMap.keys + newPiecesMap.keys).distinct()

                    // Check stock sufficiency
                    for (pid in allProductIds) {
                        val oldP = oldPiecesMap[pid] ?: 0
                        val newP = newPiecesMap[pid] ?: 0
                        val delta = newP - oldP
                        if (delta > 0) {
                            val product = productDao.getProductById(pid)
                                ?: return@withTransaction Result.failure(IllegalStateException("Product not found"))
                            if (product.stockPieces < delta) {
                                return@withTransaction Result.failure(
                                    IllegalStateException("Insufficient stock for '${product.name}'!")
                                )
                            }
                        }
                    }

                    // Apply deltas
                    for (pid in allProductIds) {
                        val oldP = oldPiecesMap[pid] ?: 0
                        val newP = newPiecesMap[pid] ?: 0
                        val delta = newP - oldP
                        if (delta != 0) {
                            val product = productDao.getProductById(pid)!!
                            productDao.updateStock(pid, product.stockPieces - delta)
                        }
                    }
                } else if (!wasConfirmed && willBeConfirmed) {
                    // Deduct all new pieces
                    for (item in newItems) {
                        val product = productDao.getProductById(item.productId)
                            ?: return@withTransaction Result.failure(IllegalStateException("Product '${item.tileName}' not found"))
                        if (product.stockPieces < item.totalPieces) {
                            return@withTransaction Result.failure(
                                IllegalStateException("Insufficient stock for '${product.name}'!")
                            )
                        }
                    }
                    for (item in newItems) {
                        val product = productDao.getProductById(item.productId)!!
                        productDao.updateStock(product.id, product.stockPieces - item.totalPieces)
                    }
                }

                // Update order and items
                orderDao.update(
                    updatedOrder.copy(
                        confirmedAt = if (willBeConfirmed && updatedOrder.confirmedAt == null) System.currentTimeMillis() else updatedOrder.confirmedAt
                    )
                )
                orderItemDao.deleteItemsForOrder(updatedOrder.id)
                orderItemDao.insertAll(newItems.map { it.copy(orderId = updatedOrder.id) })

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun processTileReturn(
        orderId: Long,
        orderItemId: Long,
        returnBoxes: Int,
        returnLoose: Int,
        returnReason: String = ""
    ): Result<Unit> {
        return try {
            database.withTransaction {
                val order = orderDao.getOrderById(orderId)
                    ?: return@withTransaction Result.failure(IllegalStateException("Order not found."))
                val orderItem = orderItemDao.getItemById(orderItemId)
                    ?: return@withTransaction Result.failure(IllegalStateException("Item not found in order."))

                val piecesPerBox = if (orderItem.piecesPerBox > 0) orderItem.piecesPerBox else 1
                val returnedTotalPieces = (returnBoxes * piecesPerBox) + returnLoose

                if (returnedTotalPieces <= 0) {
                    return@withTransaction Result.failure(IllegalStateException("Return quantity must be greater than 0."))
                }

                if (returnedTotalPieces > orderItem.totalPieces) {
                    return@withTransaction Result.failure(
                        IllegalStateException("वापसी संख्या बेची गई मात्रा (${orderItem.fullBoxes} Bx + ${orderItem.loosePieces} Pcs) से अधिक नहीं हो सकती!")
                    )
                }

                // 1. Calculate return refund value
                val pieceRate = if (orderItem.piecesPerBox > 0) orderItem.pricePerBox / orderItem.piecesPerBox else 0.0
                val refundAmount = (returnBoxes * orderItem.pricePerBox) + (returnLoose * pieceRate)

                // 2. Restock tile in inventory if order was CONFIRMED
                if (order.status == "CONFIRMED" && orderItem.productId > 0) {
                    val prod = productDao.getProductById(orderItem.productId)
                    if (prod != null) {
                        productDao.updateStock(prod.id, prod.stockPieces + returnedTotalPieces)
                    }
                }

                // 3. Update or remove order item
                val remainingPieces = orderItem.totalPieces - returnedTotalPieces
                if (remainingPieces <= 0) {
                    // Entire item returned
                    orderItemDao.deleteById(orderItem.id)
                } else {
                    val newFullBoxes = remainingPieces / piecesPerBox
                    val newLoose = remainingPieces % piecesPerBox
                    val newLineTotal = (newFullBoxes * orderItem.pricePerBox) + (newLoose * pieceRate)
                    orderItemDao.update(
                        orderItem.copy(
                            fullBoxes = newFullBoxes,
                            loosePieces = newLoose,
                            lineTotal = newLineTotal
                        )
                    )
                }

                // 4. Update order financial totals
                val newSubtotal = (order.subtotal - refundAmount).coerceAtLeast(0.0)
                val newGrandTotal = (newSubtotal - order.discount).coerceAtLeast(0.0)
                // If paidAmount exceeds new grand total, excess can be refunded
                val newPaidAmount = order.paidAmount.coerceAtMost(newGrandTotal)
                val newBalance = (newGrandTotal - newPaidAmount).coerceAtLeast(0.0)

                val reasonSuffix = if (returnReason.isNotBlank()) " ($returnReason)" else ""
                val returnNote = "\n[वापसी / Return]: ${orderItem.tileName} $returnBoxes Bx + $returnLoose Pcs (Refund: -₹${refundAmount.toInt()})$reasonSuffix"
                val updatedNotes = (order.notes + returnNote).trim()

                // Append refund to payment history
                val updatedHistoryJson = try {
                    val arr = if (order.paymentHistoryJson.isNotBlank()) org.json.JSONArray(order.paymentHistoryJson) else org.json.JSONArray()
                    arr.put(org.json.JSONObject().apply {
                        put("amount", -refundAmount)
                        put("date", java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.ENGLISH).format(java.util.Date()))
                        put("mode", "TILE_RETURN_REFUND")
                        put("note", "टाइल वापसी समायोजन: ${orderItem.tileName} ($returnBoxes Bx, $returnLoose Pcs)$reasonSuffix")
                        put("balanceAfter", newBalance)
                    })
                    arr.toString()
                } catch (_: Exception) {
                    order.paymentHistoryJson
                }

                orderDao.update(
                    order.copy(
                        subtotal = newSubtotal,
                        grandTotal = newGrandTotal,
                        paidAmount = newPaidAmount,
                        balanceAmount = newBalance,
                        paymentHistoryJson = updatedHistoryJson,
                        notes = updatedNotes
                    )
                )

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOrder(orderId: Long, restoreStock: Boolean): Result<Unit> {
        return try {
            database.withTransaction {
                val order = orderDao.getOrderById(orderId)
                    ?: return@withTransaction Result.failure(IllegalStateException("Order not found."))
                val items = orderItemDao.getItemsForOrderSync(orderId)

                // Archive to Recycle Bin before deleting
                val rootJson = JSONObject()
                val orderObj = JSONObject().apply {
                    put("id", order.id)
                    put("invoiceNumber", order.invoiceNumber)
                    put("customerId", order.customerId ?: -1L)
                    put("customerName", order.customerName)
                    put("customerPhone", order.customerPhone)
                    put("customerAddress", order.customerAddress)
                    put("status", order.status)
                    put("showPriceOnPdf", order.showPriceOnPdf)
                    put("subtotal", order.subtotal)
                    put("discount", order.discount)
                    put("grandTotal", order.grandTotal)
                    put("notes", order.notes)
                    put("createdAt", order.createdAt)
                    put("confirmedAt", order.confirmedAt ?: -1L)
                }
                rootJson.put("order", orderObj)

                val itemsArr = JSONArray()
                for (it in items) {
                    val itObj = JSONObject().apply {
                        put("id", it.id)
                        put("orderId", it.orderId)
                        put("productId", it.productId)
                        put("tileCode", it.tileCode)
                        put("tileName", it.tileName)
                        put("tileSize", it.tileSize)
                        put("piecesPerBox", it.piecesPerBox)
                        put("tilePhotoUri", it.tilePhotoUri ?: "")
                        put("fullBoxes", it.fullBoxes)
                        put("loosePieces", it.loosePieces)
                        put("pricePerBox", it.pricePerBox)
                        put("lineTotal", it.lineTotal)
                    }
                    itemsArr.put(itObj)
                }
                rootJson.put("items", itemsArr)

                recycleBinDao.insert(
                    RecycleBinItem(
                        itemType = "ORDER",
                        originalId = orderId,
                        title = "बिल #${order.invoiceNumber}",
                        subtitle = "${order.customerName} • ₹${order.grandTotal.toInt()} • ${order.status}",
                        payloadJson = rootJson.toString()
                    )
                )

                if (order.status == "CONFIRMED" && restoreStock) {
                    for (item in items) {
                        val product = productDao.getProductById(item.productId)
                        if (product != null) {
                            productDao.updateStock(product.id, product.stockPieces + item.totalPieces)
                        }
                    }
                }

                orderItemDao.deleteItemsForOrder(orderId)
                orderDao.deleteById(orderId)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePdfPriceVisibility(orderId: Long, showPrice: Boolean) {
        orderDao.updateShowPriceOnPdf(orderId, showPrice)
    }

    suspend fun addOrderPayment(
        orderId: Long,
        additionalAmount: Double,
        paymentMode: String,
        note: String
    ): Result<Double> {
        return try {
            database.withTransaction {
                val order = orderDao.getOrderById(orderId)
                    ?: return@withTransaction Result.failure(IllegalStateException("Order not found"))
                val newPaid = (order.paidAmount + additionalAmount).coerceAtMost(order.grandTotal)
                val newBalance = (order.grandTotal - newPaid).coerceAtLeast(0.0)

                val historyArr = try {
                    if (order.paymentHistoryJson.isNotBlank()) JSONArray(order.paymentHistoryJson) else JSONArray()
                } catch (e: Exception) {
                    JSONArray()
                }

                val paymentObj = JSONObject().apply {
                    put("amount", additionalAmount)
                    put("date", java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.ENGLISH).format(java.util.Date()))
                    put("mode", paymentMode)
                    put("note", note)
                    put("balanceAfter", newBalance)
                }
                historyArr.put(paymentObj)

                orderDao.updateOrderPayment(orderId, newPaid, newBalance, historyArr.toString())
                Result.success(newBalance)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrderById(orderId: Long): OrderEntity? = orderDao.getOrderById(orderId)

    // ------------------------------------------------------------------------
    // Staff, Attendance, Advance & Salary Management
    // ------------------------------------------------------------------------
    val allStaff: Flow<List<StaffMember>> = staffDao.getAllStaff()
    val activeStaff: Flow<List<StaffMember>> = staffDao.getAllActiveStaff()
    val recentStaffPayments: Flow<List<StaffPayment>> = staffDao.getRecentPayments()

    suspend fun getStaffById(id: Long): StaffMember? = staffDao.getStaffById(id)

    suspend fun saveStaff(staff: StaffMember): Long {
        return if (staff.id == 0L) {
            staffDao.insertStaff(staff)
        } else {
            staffDao.updateStaff(staff)
            staff.id
        }
    }

    suspend fun deleteStaff(id: Long) {
        val staff = staffDao.getStaffById(id)
        if (staff != null) {
            val json = JSONObject().apply {
                put("id", staff.id)
                put("name", staff.name)
                put("phone", staff.phone)
                put("role", staff.role)
                put("monthlySalary", staff.monthlySalary)
                put("joiningDate", staff.joiningDate)
                put("isActive", staff.isActive)
                put("notes", staff.notes)
            }.toString()

            recycleBinDao.insert(
                RecycleBinItem(
                    itemType = "STAFF",
                    originalId = id,
                    title = "कर्मचारी: ${staff.name}",
                    subtitle = "${staff.role} • ₹${staff.monthlySalary.toInt()}/माह",
                    payloadJson = json
                )
            )
        }
        staffDao.deleteStaffById(id)
    }

    fun getAttendanceForStaff(staffId: Long): Flow<List<StaffAttendance>> =
        staffDao.getAttendanceForStaff(staffId)

    fun getAttendanceForStaffInMonth(staffId: Long, monthPrefix: String): Flow<List<StaffAttendance>> =
        staffDao.getAttendanceForStaffInMonth(staffId, monthPrefix)

    fun getAttendanceForDate(dateString: String): Flow<List<StaffAttendance>> =
        staffDao.getAttendanceForDate(dateString)

    suspend fun recordAttendance(attendance: StaffAttendance): Long {
        val cleanDate = attendance.dateString.trim()
        staffDao.deleteAttendance(attendance.staffId, cleanDate)
        return staffDao.recordAttendance(attendance.copy(dateString = cleanDate))
    }

    suspend fun deleteAttendance(staffId: Long, dateString: String) {
        staffDao.deleteAttendance(staffId, dateString.trim())
    }

    fun getPaymentsForStaff(staffId: Long): Flow<List<StaffPayment>> =
        staffDao.getPaymentsForStaff(staffId)

    fun getPaymentsForStaffInMonth(staffId: Long, monthYear: String): Flow<List<StaffPayment>> =
        staffDao.getPaymentsForStaffInMonth(staffId, monthYear)

    suspend fun recordStaffPayment(payment: StaffPayment): Long =
        staffDao.recordPayment(payment)

    suspend fun deleteStaffPayment(id: Long) =
        staffDao.deletePaymentById(id)

    fun getSettlementsForStaff(staffId: Long): Flow<List<com.example.data.model.StaffMonthlySettlement>> =
        staffDao.getSettlementsForStaff(staffId)

    suspend fun getSettlement(staffId: Long, monthKey: String): com.example.data.model.StaffMonthlySettlement? =
        staffDao.getSettlement(staffId, monthKey)

    suspend fun saveStaffSettlement(settlement: com.example.data.model.StaffMonthlySettlement) {
        staffDao.saveSettlement(settlement)
    }

    suspend fun autoMarkPresentForDate(dateString: String, timeMillis: Long = System.currentTimeMillis()): Int {
        val cleanDate = dateString.trim()
        val allActive = staffDao.getAllActiveStaffSync()
        val existingToday = staffDao.getAttendanceForDateSync(cleanDate).associateBy { it.staffId }
        var markedCount = 0

        for (staff in allActive) {
            val existing = existingToday[staff.id]
            if (existing == null) {
                // Not marked yet, mark PRESENT
                staffDao.recordAttendance(
                    StaffAttendance(
                        staffId = staff.id,
                        dateString = cleanDate,
                        dateMillis = timeMillis,
                        status = "PRESENT",
                        notes = "ऑटो-हाजिरी (Auto Present)"
                    )
                )
                markedCount++
            }
            // If already marked (ABSENT, HALF_DAY, PAID_LEAVE, or PRESENT), skip as requested!
        }
        return markedCount
    }

    // ------------------------------------------------------------------------
    // Recycle Bin / Backup Trash (Auto-purge after 10 days & restore to place)
    // ------------------------------------------------------------------------
    val allRecycleItems: Flow<List<RecycleBinItem>> = recycleBinDao.getAllRecycleItems()
    val recycleBinCount: Flow<Int> = recycleBinDao.getRecycleCount()

    suspend fun purgeOldRecycleBinItems() {
        val tenDays = 10L * 24 * 60 * 60 * 1000L
        recycleBinDao.purgeOlderThan(System.currentTimeMillis(), tenDays)
    }

    suspend fun restoreRecycleItem(id: Long): Result<String> {
        return try {
            val item = recycleBinDao.getItemById(id)
                ?: return Result.failure(IllegalStateException("आइटम रीसायकल बिन में नहीं मिला"))

            when (item.itemType) {
                "CUSTOMER" -> {
                    val co = JSONObject(item.payloadJson)
                    val cust = Customer(
                        id = 0L,
                        name = co.getString("name"),
                        phone = co.optString("phone", ""),
                        address = co.optString("address", ""),
                        notes = co.optString("notes", ""),
                        outstandingBalance = co.optDouble("outstandingBalance", 0.0),
                        createdAt = co.optLong("createdAt", System.currentTimeMillis())
                    )
                    customerDao.insert(cust)
                }
                "PRODUCT" -> {
                    val po = JSONObject(item.payloadJson)
                    var code = po.getString("code")
                    if (productDao.getProductByCode(code) != null) {
                        code = "${code}-REST"
                    }
                    val prod = TileProduct(
                        id = 0L,
                        code = code,
                        name = po.getString("name"),
                        size = po.getString("size"),
                        photoUri = po.optString("photoUri").ifEmpty { null },
                        description = po.optString("description", ""),
                        piecesPerBox = po.optInt("piecesPerBox", 5),
                        stockPieces = po.optInt("stockPieces", 0),
                        defaultSellingPrice = po.optDouble("defaultSellingPrice", 0.0),
                        purchaseCost = po.optDouble("purchaseCost", 0.0),
                        createdAt = po.optLong("createdAt", System.currentTimeMillis())
                    )
                    productDao.insert(prod)
                }
                "ORDER" -> {
                    val root = JSONObject(item.payloadJson)
                    val oo = root.getJSONObject("order")
                    val confAt = oo.optLong("confirmedAt", -1L)
                    val custId = oo.optLong("customerId", -1L)
                    val order = OrderEntity(
                        id = 0L,
                        invoiceNumber = oo.getString("invoiceNumber"),
                        customerId = if (custId > 0) custId else null,
                        customerName = oo.getString("customerName"),
                        customerPhone = oo.optString("customerPhone", ""),
                        customerAddress = oo.optString("customerAddress", ""),
                        status = oo.optString("status", "PENDING"),
                        showPriceOnPdf = oo.optBoolean("showPriceOnPdf", true),
                        subtotal = oo.optDouble("subtotal", 0.0),
                        discount = oo.optDouble("discount", 0.0),
                        grandTotal = oo.optDouble("grandTotal", 0.0),
                        notes = oo.optString("notes", ""),
                        createdAt = oo.optLong("createdAt", System.currentTimeMillis()),
                        confirmedAt = if (confAt > 0) confAt else null
                    )
                    val newOrderId = orderDao.insert(order)
                    if (root.has("items")) {
                        val arr = root.getJSONArray("items")
                        val items = mutableListOf<OrderItemEntity>()
                        for (i in 0 until arr.length()) {
                            val io = arr.getJSONObject(i)
                            items.add(
                                OrderItemEntity(
                                    id = 0L,
                                    orderId = newOrderId,
                                    productId = io.getLong("productId"),
                                    tileCode = io.getString("tileCode"),
                                    tileName = io.getString("tileName"),
                                    tileSize = io.getString("tileSize"),
                                    piecesPerBox = io.getInt("piecesPerBox"),
                                    tilePhotoUri = io.optString("tilePhotoUri").ifEmpty { null },
                                    fullBoxes = io.getInt("fullBoxes"),
                                    loosePieces = io.optInt("loosePieces", 0),
                                    pricePerBox = io.getDouble("pricePerBox"),
                                    lineTotal = io.getDouble("lineTotal")
                                )
                            )
                        }
                        if (items.isNotEmpty()) {
                            orderItemDao.insertAll(items)
                        }
                    }
                }
                "STAFF" -> {
                    val so = JSONObject(item.payloadJson)
                    val staff = StaffMember(
                        id = 0L,
                        name = so.getString("name"),
                        phone = so.optString("phone", ""),
                        role = so.optString("role", "Worker"),
                        monthlySalary = so.optDouble("monthlySalary", 0.0),
                        joiningDate = so.optLong("joiningDate", System.currentTimeMillis()),
                        isActive = so.optBoolean("isActive", true),
                        notes = so.optString("notes", "")
                    )
                    staffDao.insertStaff(staff)
                }
            }

            recycleBinDao.deleteById(id)
            Result.success("आइटम को अपनी जगह सफलतापूर्वक वापस रीस्टोर कर दिया गया!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreAllRecycleItems(): Result<Int> {
        val items = recycleBinDao.getAllRecycleItemsSync()
        var count = 0
        for (it in items) {
            if (restoreRecycleItem(it.id).isSuccess) {
                count++
            }
        }
        return Result.success(count)
    }

    suspend fun deleteRecycleItemPermanently(id: Long) {
        recycleBinDao.deleteById(id)
    }

    suspend fun clearRecycleBin() {
        recycleBinDao.clearAll()
    }

    // ------------------------------------------------------------------------
    // User Accounts, Self-Registration & Admin Approval
    // ------------------------------------------------------------------------
    val allUsers: Flow<List<com.example.data.model.AppUser>> = appUserDao.getAllUsers()
    val pendingUsers: Flow<List<com.example.data.model.AppUser>> = appUserDao.getUsersByApprovalStatus("PENDING")
    val pendingApprovalCount: Flow<Int> = appUserDao.getPendingApprovalCount()

    suspend fun getUserByPhone(phone: String): com.example.data.model.AppUser? =
        appUserDao.getUserByPhone(phone.trim())

    suspend fun getUserById(id: Long): com.example.data.model.AppUser? =
        appUserDao.getUserById(id)

    suspend fun registerClientUser(
        context: Context,
        name: String,
        shopName: String,
        phone: String,
        password: String,
        address: String
    ): Result<com.example.data.model.AppUser> {
        val cleanPhone = phone.trim()
        val existing = appUserDao.getUserByPhone(cleanPhone)
        if (existing != null) {
            return Result.failure(IllegalStateException("यह मोबाइल नंबर ($cleanPhone) पहले से रजिस्टर्ड है।"))
        }

        val adminConfig = adminGlobalConfigDao.getConfigSync()
        if (adminConfig != null && !adminConfig.allowUserSelfRegistration) {
            return Result.failure(IllegalStateException("❌ एडमिन द्वारा नया यूजर रजिस्ट्रेशन वर्तमान में बंद रखा गया है।"))
        }

        val requiresApproval = adminConfig?.requireAdminApproval ?: true
        val trialDays = adminConfig?.defaultTrialDays ?: 30
        val expiresAt = if (trialDays == -1) Long.MAX_VALUE else (System.currentTimeMillis() + (trialDays.toLong() * 86400000L))

        val newUser = com.example.data.model.AppUser(
            name = name.trim(),
            shopName = shopName.trim(),
            phone = cleanPhone,
            password = password.trim(),
            role = "CLIENT",
            approvalStatus = if (requiresApproval) "PENDING" else "APPROVED",
            isActive = !requiresApproval,
            address = address.trim(),
            planDays = trialDays,
            planName = if (trialDays == -1) "आजीवन (Lifetime)" else "$trialDays Days Trial",
            expiresAt = expiresAt,
            notes = "सेल्फ-रजिस्टर किया गया"
        )
        val id = appUserDao.insert(newUser)
        val created = newUser.copy(id = id)

        // Save immediately to permanent vault so "Clear Data" never deletes this account!
        com.example.util.PermanentVaultHelper.saveUsersRegistry(context, appUserDao.getAllUsersSync())

        // 1. Broadcast registration to cloud channels so Admin App receives it instantly across devices!
        com.example.util.PermanentVaultHelper.publishNewRegistration(created)

        // 2. Notify Admin on Telegram if configured
        try {
            val liveAdminConfig = com.example.util.PermanentVaultHelper.fetchLiveCloudAdminConfig()
                ?: adminGlobalConfigDao.getConfigSync()
            val activeTelegram = com.example.util.TelegramActiveConfig.resolve(liveAdminConfig, settingsDao.getSettingsSync())
            if (activeTelegram.isReady && activeTelegram.alertUserAction) {
                val text = "🆕 *नया रजिस्ट्रेशन अनुरोध!*\n\n" +
                        "👤 *नाम:* ${created.name}\n" +
                        "🏪 *दुकान:* ${created.shopName}\n" +
                        "📱 *मोबाइल:* ${created.phone}\n" +
                        "📍 *पता:* ${created.address}\n\n" +
                        "⚠️ *कृपया एडमिन ऐप में जाकर इसे स्वीकृत (Approve) करें।*"
                TelegramNotifier.sendMessage(
                    botToken = activeTelegram.botToken,
                    chatId = activeTelegram.chatId,
                    text = text,
                    parseMode = "Markdown"
                )
            }
        } catch (_: Exception) {}

        return Result.success(created)
    }

    suspend fun approveUser(userId: Long, context: Context? = null): Result<Unit> {
        appUserDao.updateApproval(userId, "APPROVED", true)
        val user = appUserDao.getUserById(userId)
        if (user != null) {
            com.example.util.PermanentVaultHelper.publishCloudUser(user.copy(approvalStatus = "APPROVED", isActive = true))
            com.example.util.PermanentVaultHelper.publishBlockedUser(user.phone, false)
            try {
                val liveAdminConfig = com.example.util.PermanentVaultHelper.fetchLiveCloudAdminConfig()
                    ?: adminGlobalConfigDao.getConfigSync()
                val activeTelegram = com.example.util.TelegramActiveConfig.resolve(liveAdminConfig, settingsDao.getSettingsSync())
                if (activeTelegram.isReady && activeTelegram.alertUserAction) {
                    val text = "✅ *यूजर खाता स्वीकृत (Approved)!*\n\n" +
                            "👤 *नाम:* ${user.name}\n" +
                            "🏪 *दुकान:* ${user.shopName}\n" +
                            "📱 *मोबाइल:* ${user.phone}\n\n" +
                            "यूजर अब अपने मोबाइल नंबर व पासवर्ड से ऐप में लॉगिन कर सकता है।"
                    TelegramNotifier.sendMessage(
                        botToken = activeTelegram.botToken,
                        chatId = activeTelegram.chatId,
                        text = text,
                        parseMode = "Markdown"
                    )
                }
            } catch (_: Exception) {}
        }
        if (context != null) {
            com.example.util.PermanentVaultHelper.saveUsersRegistry(context, appUserDao.getAllUsersSync())
        }
        return Result.success(Unit)
    }

    suspend fun rejectUser(userId: Long, context: Context? = null): Result<Unit> {
        appUserDao.updateApproval(userId, "REJECTED", false)
        val user = appUserDao.getUserById(userId)
        if (user != null) {
            com.example.util.PermanentVaultHelper.publishCloudUser(user.copy(approvalStatus = "REJECTED", isActive = false))
            com.example.util.PermanentVaultHelper.publishBlockedUser(user.phone, true)
        }
        if (context != null) {
            com.example.util.PermanentVaultHelper.saveUsersRegistry(context, appUserDao.getAllUsersSync())
        }
        return Result.success(Unit)
    }


    suspend fun saveUser(user: com.example.data.model.AppUser, context: Context? = null): Long {
        com.example.util.PermanentVaultHelper.unmarkPhoneDeleted(user.phone, context)
        val id = if (user.id == 0L) {
            appUserDao.insert(user)
        } else {
            appUserDao.update(user)
            user.id
        }
        val saved = appUserDao.getUserById(id) ?: user.copy(id = id)
        com.example.util.PermanentVaultHelper.publishCloudUser(saved)
        if (context != null) {
            com.example.util.PermanentVaultHelper.saveUsersRegistry(context, appUserDao.getAllUsersSync())
        }
        return id
    }

    suspend fun updateUser(user: com.example.data.model.AppUser, context: Context? = null) {
        com.example.util.PermanentVaultHelper.unmarkPhoneDeleted(user.phone, context)
        appUserDao.update(user)
        com.example.util.PermanentVaultHelper.publishCloudUser(user)
        if (context != null) {
            com.example.util.PermanentVaultHelper.saveUsersRegistry(context, appUserDao.getAllUsersSync())
        }
    }

    suspend fun updateUserPassword(userId: Long, newPass: String, context: Context? = null) {
        appUserDao.updatePassword(userId, newPass.trim())
        val user = appUserDao.getUserById(userId)
        if (user != null) {
            com.example.util.PermanentVaultHelper.publishCloudUser(user)
        }
        if (context != null) {
            com.example.util.PermanentVaultHelper.saveUsersRegistry(context, appUserDao.getAllUsersSync())
        }
    }

    suspend fun updateUserStatus(userId: Long, isActive: Boolean, context: Context? = null) {
        appUserDao.updateStatus(userId, isActive)
        if (isActive) {
            appUserDao.updateApproval(userId, "APPROVED", true)
        } else {
            appUserDao.updateApproval(userId, "BLOCKED", false)
        }
        val user = appUserDao.getUserById(userId)
        if (user != null) {
            com.example.util.PermanentVaultHelper.publishBlockedUser(user.phone, !isActive)
            com.example.util.PermanentVaultHelper.publishCloudUser(user.copy(isActive = isActive, approvalStatus = if (isActive) "APPROVED" else "BLOCKED"))
            try {
                val cfg = com.example.util.TelegramActiveConfig.resolve(
                    adminGlobalConfigDao.getConfigSync(),
                    settingsDao.getSettingsSync()
                )
                val action = if (isActive) "✅ यूजर सक्रिय (Unblocked) किया गया" else "🚫 यूजर ब्लॉक (Blocked) किया गया - तुरंत ऑटो-लॉगआउट ट्रिगर"
                com.example.util.TelegramNotifier.notifyUserAction(cfg, user.name, user.phone, action)
            } catch (_: Exception) {}
        }
        if (context != null) {
            com.example.util.PermanentVaultHelper.saveUsersRegistry(context, appUserDao.getAllUsersSync())
        }
    }

    suspend fun deleteUser(id: Long, context: Context? = null) {
        val user = appUserDao.getUserById(id)
        if (user != null) {
            com.example.util.PermanentVaultHelper.publishDeletedUser(user.phone, context)
            appUserDao.deleteByPhone(user.phone)
            try {
                val cfg = com.example.util.TelegramActiveConfig.resolve(
                    adminGlobalConfigDao.getConfigSync(),
                    settingsDao.getSettingsSync()
                )
                com.example.util.TelegramNotifier.notifyUserAction(
                    cfg,
                    user.name,
                    user.phone,
                    "⚠️ यूजर आईडी हटाई गई (Deleted by Admin) - सभी डिवाइस से तुरंत ऑटो-लॉगआउट ट्रिगर"
                )
            } catch (_: Exception) {}
        }
        appUserDao.deleteById(id)
        if (context != null) {
            val remainingUsers = appUserDao.getAllUsersSync().filter {
                !com.example.util.PermanentVaultHelper.isPhoneDeleted(it.phone, context)
            }
            com.example.util.PermanentVaultHelper.saveUsersRegistry(context, remainingUsers)
        }
    }

    suspend fun ensureDefaultUserExists() {
        if (com.example.util.PermanentVaultHelper.isPhoneDeleted("9876543210")) {
            return
        }

        if (appUserDao.getUserCount() == 0) {
            val cloudUser = com.example.util.PermanentVaultHelper.fetchLiveCloudUser("9876543210")
            if (cloudUser != null) {
                if (cloudUser.approvalStatus == "DELETED") {
                    return
                }
                if (cloudUser.approvalStatus == "APPROVED" && cloudUser.isActive) {
                    appUserDao.insert(cloudUser)
                    return
                }
            }

            val defaultUser = com.example.data.model.AppUser(
                name = "दुकानदार",
                shopName = "TILES AND MARBLES",
                phone = "9876543210",
                password = "123456",
                planDays = 365,
                planName = "1 Year (1 वर्ष)",
                amountCharged = 3499.0,
                approvalStatus = "APPROVED",
                isActive = true,
                notes = "Master Client Account"
            )
            val id = appUserDao.insert(defaultUser)
            com.example.util.PermanentVaultHelper.publishCloudUser(defaultUser.copy(id = id))
        } else {
            val defaultUser = appUserDao.getUserByPhone("9876543210")
            if (defaultUser != null && (!defaultUser.isActive || defaultUser.approvalStatus == "REJECTED" || defaultUser.approvalStatus == "BLOCKED")) {
                val cloudUser = com.example.util.PermanentVaultHelper.fetchLiveCloudUser("9876543210")
                if (cloudUser != null && cloudUser.isActive && cloudUser.approvalStatus == "APPROVED") {
                    appUserDao.update(defaultUser.copy(isActive = true, approvalStatus = "APPROVED"))
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Multi-Device Cloud Sync & Backup Snapshot (Survives Clear Data & Device Switch)
    // ------------------------------------------------------------------------
    suspend fun syncCloudBackupForUser(context: Context, userPhone: String): Result<String> {
        return com.example.util.PermanentVaultHelper.saveShopSnapshot(context, database, userPhone)
    }

    suspend fun restoreCloudBackupForUser(context: Context, userPhone: String): Result<Boolean> {
        return com.example.util.PermanentVaultHelper.restoreShopSnapshot(context, database, userPhone)
    }

    val sizePricings = allSizePricings
    suspend fun getSettingsSync(): ShopSettings? = settingsDao.getSettingsSync()

    suspend fun markAttendance(staffId: Long, dateMillis: Long, dateString: String, status: String, notes: String): Long {
        val cleanDate = dateString.trim()
        staffDao.deleteAttendance(staffId, cleanDate)
        return staffDao.recordAttendance(
            StaffAttendance(
                staffId = staffId,
                dateMillis = dateMillis,
                dateString = cleanDate,
                status = status,
                notes = notes
            )
        )
    }

    val allLicenses: Flow<List<com.example.data.model.ClientLicense>> = clientLicenseDao.getAllLicenses()
    suspend fun saveLicense(license: com.example.data.model.ClientLicense): Long {
        return if (license.id == 0L) {
            clientLicenseDao.insert(license)
        } else {
            clientLicenseDao.update(license)
            license.id
        }
    }
    suspend fun updateLicense(license: com.example.data.model.ClientLicense) = clientLicenseDao.update(license)
    suspend fun deleteLicense(id: Long) = clientLicenseDao.deleteById(id)

    suspend fun getAllDataForBackup(): Map<String, Any> {
        val settings = settingsDao.getSettingsSync() ?: ShopSettings()
        val pricings = sizePricingDao.getAllPricingsSync()
        val products = productDao.getAllProductsSync()
        val customers = customerDao.getAllCustomersSync()
        val orders = orderDao.getAllOrdersSync()
        val items = orderItemDao.getAllItemsSync()
        return mapOf(
            "settings" to settings,
            "pricings" to pricings,
            "products" to products,
            "customers" to customers,
            "orders" to orders,
            "items" to items
        )
    }
}
