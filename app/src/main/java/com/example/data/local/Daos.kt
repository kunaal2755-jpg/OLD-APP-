package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Customer
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.ShopSettings
import com.example.data.model.SizePricing
import com.example.data.model.TileProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopSettingsDao {
    @Query("SELECT * FROM shop_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<ShopSettings?>

    @Query("SELECT * FROM shop_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): ShopSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: ShopSettings)
}

@Dao
interface SizePricingDao {
    @Query("SELECT * FROM size_pricing ORDER BY sizeName ASC")
    fun getAllPricings(): Flow<List<SizePricing>>

    @Query("SELECT * FROM size_pricing ORDER BY sizeName ASC")
    suspend fun getAllPricingsSync(): List<SizePricing>

    @Query("SELECT * FROM size_pricing WHERE LOWER(sizeName) = LOWER(:size) LIMIT 1")
    suspend fun getPricingForSize(size: String): SizePricing?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pricing: SizePricing): Long

    @Update
    suspend fun update(pricing: SizePricing)

    @Query("DELETE FROM size_pricing WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface TileLocationDao {
    @Query("SELECT * FROM tile_locations ORDER BY name ASC")
    fun getAllLocations(): Flow<List<com.example.data.model.TileLocation>>

    @Query("SELECT * FROM tile_locations ORDER BY name ASC")
    suspend fun getAllLocationsSync(): List<com.example.data.model.TileLocation>

    @Query("SELECT * FROM tile_locations WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getByName(name: String): com.example.data.model.TileLocation?

    @Query("SELECT * FROM tile_locations WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): com.example.data.model.TileLocation?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(location: com.example.data.model.TileLocation): Long

    @Update
    suspend fun update(location: com.example.data.model.TileLocation)

    @Query("DELETE FROM tile_locations WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface TileProductDao {
    @Query("SELECT * FROM tile_products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<TileProduct>>

    @Query("SELECT * FROM tile_products ORDER BY name ASC")
    suspend fun getAllProductsSync(): List<TileProduct>

    @Query("SELECT * FROM tile_products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): TileProduct?

    @Query("SELECT * FROM tile_products WHERE LOWER(code) = LOWER(:code) LIMIT 1")
    suspend fun getProductByCode(code: String): TileProduct?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: TileProduct): Long

    @Update
    suspend fun update(product: TileProduct)

    @Query("UPDATE tile_products SET stockPieces = :newStock WHERE id = :id")
    suspend fun updateStock(id: Long, newStock: Int)

    @Query("UPDATE tile_products SET location = :newName WHERE location = :oldName")
    suspend fun renameLocationAcrossProducts(oldName: String, newName: String): Int

    @Query("UPDATE tile_products SET location = '' WHERE location = :deletedName")
    suspend fun clearLocationAcrossProducts(deletedName: String): Int

    @Query("DELETE FROM tile_products WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomersSync(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: Customer): Long

    @Update
    suspend fun update(customer: Customer)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    suspend fun getAllOrdersSync(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getOrdersForCustomer(customerId: Long): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity): Long

    @Update
    suspend fun update(order: OrderEntity)

    @Query("UPDATE orders SET showPriceOnPdf = :showPrice WHERE id = :id")
    suspend fun updateShowPriceOnPdf(id: Long, showPrice: Boolean)

    @Query("UPDATE orders SET showSizeTotalOnPdf = :showSizeTotal WHERE id = :id")
    suspend fun updateShowSizeTotalOnPdf(id: Long, showSizeTotal: Boolean)

    @Query("UPDATE orders SET showDeliveryStatusOnPdf = :showDelivery WHERE id = :id")
    suspend fun updateShowDeliveryStatusOnPdf(id: Long, showDelivery: Boolean)

    @Query("UPDATE orders SET status = :status, confirmedAt = :confirmedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, confirmedAt: Long?)

    @Query("UPDATE orders SET deliveryStatus = :deliveryStatus WHERE id = :id")
    suspend fun updateDeliveryStatus(id: Long, deliveryStatus: String)

    @Query("UPDATE orders SET paidAmount = :paidAmount, balanceAmount = :balanceAmount, paymentHistoryJson = :historyJson WHERE id = :id")
    suspend fun updateOrderPayment(id: Long, paidAmount: Double, balanceAmount: Double, historyJson: String)

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface OrderItemDao {
    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getItemsForOrder(orderId: Long): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrderSync(orderId: Long): List<OrderItemEntity>

    @Query("SELECT * FROM order_items")
    suspend fun getAllItemsSync(): List<OrderItemEntity>

    @Query("SELECT * FROM order_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): OrderItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: OrderItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<OrderItemEntity>)

    @Update
    suspend fun update(item: OrderItemEntity)

    @Query("UPDATE order_items SET deliveredBoxes = :deliveredBoxes, deliveredPieces = :deliveredPieces WHERE id = :id")
    suspend fun updateDeliveredQty(id: Long, deliveredBoxes: Int, deliveredPieces: Int)

    @Query("DELETE FROM order_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteItemsForOrder(orderId: Long)
}

@Dao
interface OrderDeliveryDao {
    @Query("SELECT * FROM order_deliveries WHERE orderId = :orderId ORDER BY deliveredAt DESC, id DESC")
    fun getDeliveriesForOrder(orderId: Long): Flow<List<com.example.data.model.OrderDeliveryLog>>

    @Query("SELECT * FROM order_deliveries WHERE orderId = :orderId ORDER BY deliveredAt DESC, id DESC")
    suspend fun getDeliveriesForOrderSync(orderId: Long): List<com.example.data.model.OrderDeliveryLog>

    @Query("SELECT * FROM order_deliveries ORDER BY deliveredAt DESC")
    suspend fun getAllDeliveriesSync(): List<com.example.data.model.OrderDeliveryLog>

    @Query("SELECT * FROM order_deliveries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): com.example.data.model.OrderDeliveryLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: com.example.data.model.OrderDeliveryLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<com.example.data.model.OrderDeliveryLog>)

    @Query("DELETE FROM order_deliveries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM order_deliveries WHERE orderId = :orderId")
    suspend fun deleteForOrder(orderId: Long)
}

@Dao
interface OrderReturnExchangeDao {
    @Query("SELECT * FROM order_return_exchanges WHERE orderId = :orderId ORDER BY createdAt DESC, id DESC")
    fun getForOrder(orderId: Long): Flow<List<com.example.data.model.OrderReturnExchangeLog>>

    @Query("SELECT * FROM order_return_exchanges WHERE orderId = :orderId ORDER BY createdAt DESC, id DESC")
    suspend fun getForOrderSync(orderId: Long): List<com.example.data.model.OrderReturnExchangeLog>

    @Query("SELECT * FROM order_return_exchanges ORDER BY createdAt DESC")
    suspend fun getAllSync(): List<com.example.data.model.OrderReturnExchangeLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: com.example.data.model.OrderReturnExchangeLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<com.example.data.model.OrderReturnExchangeLog>)

    @Query("DELETE FROM order_return_exchanges WHERE orderId = :orderId")
    suspend fun deleteForOrder(orderId: Long)
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC LIMIT 20")
    fun getRecentMovements(): Flow<List<com.example.data.model.StockMovement>>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    suspend fun getAllMovementsSync(): List<com.example.data.model.StockMovement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: com.example.data.model.StockMovement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<com.example.data.model.StockMovement>)
}

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff_members WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveStaff(): Flow<List<com.example.data.model.StaffMember>>

    @Query("SELECT * FROM staff_members WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllActiveStaffSync(): List<com.example.data.model.StaffMember>

    @Query("SELECT * FROM staff_members ORDER BY name ASC")
    fun getAllStaff(): Flow<List<com.example.data.model.StaffMember>>

    @Query("SELECT * FROM staff_members WHERE id = :id LIMIT 1")
    suspend fun getStaffById(id: Long): com.example.data.model.StaffMember?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: com.example.data.model.StaffMember): Long

    @Update
    suspend fun updateStaff(staff: com.example.data.model.StaffMember)

    @Query("DELETE FROM staff_members WHERE id = :id")
    suspend fun deleteStaffById(id: Long)

    // Attendance
    @Query("SELECT * FROM staff_attendance WHERE staffId = :staffId ORDER BY dateMillis DESC")
    fun getAttendanceForStaff(staffId: Long): Flow<List<com.example.data.model.StaffAttendance>>

    @Query("SELECT * FROM staff_attendance WHERE staffId = :staffId AND dateString LIKE :monthPrefix || '%' ORDER BY dateMillis ASC")
    fun getAttendanceForStaffInMonth(staffId: Long, monthPrefix: String): Flow<List<com.example.data.model.StaffAttendance>>

    @Query("SELECT * FROM staff_attendance WHERE dateString = :dateString")
    fun getAttendanceForDate(dateString: String): Flow<List<com.example.data.model.StaffAttendance>>

    @Query("SELECT * FROM staff_attendance WHERE dateString = :dateString")
    suspend fun getAttendanceForDateSync(dateString: String): List<com.example.data.model.StaffAttendance>

    @Query("SELECT * FROM staff_attendance WHERE staffId = :staffId AND dateString = :dateString LIMIT 1")
    suspend fun getAttendanceRecord(staffId: Long, dateString: String): com.example.data.model.StaffAttendance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordAttendance(attendance: com.example.data.model.StaffAttendance): Long

    @Query("DELETE FROM staff_attendance WHERE staffId = :staffId AND dateString = :dateString")
    suspend fun deleteAttendance(staffId: Long, dateString: String)

    // Payments & Advance
    @Query("SELECT * FROM staff_payments WHERE staffId = :staffId ORDER BY dateMillis DESC")
    fun getPaymentsForStaff(staffId: Long): Flow<List<com.example.data.model.StaffPayment>>

    @Query("SELECT * FROM staff_payments WHERE staffId = :staffId AND forMonthYear = :monthYear ORDER BY dateMillis DESC")
    fun getPaymentsForStaffInMonth(staffId: Long, monthYear: String): Flow<List<com.example.data.model.StaffPayment>>

    @Query("SELECT * FROM staff_payments ORDER BY dateMillis DESC LIMIT 50")
    fun getRecentPayments(): Flow<List<com.example.data.model.StaffPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordPayment(payment: com.example.data.model.StaffPayment): Long

    @Query("DELETE FROM staff_payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    @Query("SELECT * FROM staff_members")
    suspend fun getAllStaffSync(): List<com.example.data.model.StaffMember>

    @Query("SELECT * FROM staff_attendance")
    suspend fun getAllAttendanceSync(): List<com.example.data.model.StaffAttendance>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAttendance(list: List<com.example.data.model.StaffAttendance>)

    @Query("SELECT * FROM staff_payments")
    suspend fun getAllPaymentsSync(): List<com.example.data.model.StaffPayment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPayments(list: List<com.example.data.model.StaffPayment>)

    // Monthly Settlements (Yes/No Status)
    @Query("SELECT * FROM staff_monthly_settlements WHERE staffId = :staffId")
    fun getSettlementsForStaff(staffId: Long): Flow<List<com.example.data.model.StaffMonthlySettlement>>

    @Query("SELECT * FROM staff_monthly_settlements WHERE staffId = :staffId AND monthKey = :monthKey LIMIT 1")
    suspend fun getSettlement(staffId: Long, monthKey: String): com.example.data.model.StaffMonthlySettlement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettlement(settlement: com.example.data.model.StaffMonthlySettlement): Long

    @Query("SELECT * FROM staff_monthly_settlements")
    suspend fun getAllSettlementsSync(): List<com.example.data.model.StaffMonthlySettlement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSettlements(list: List<com.example.data.model.StaffMonthlySettlement>)
}

@Dao
interface RecycleBinDao {
    @Query("SELECT * FROM recycle_bin ORDER BY deletedAt DESC")
    fun getAllRecycleItems(): Flow<List<com.example.data.model.RecycleBinItem>>

    @Query("SELECT * FROM recycle_bin ORDER BY deletedAt DESC")
    suspend fun getAllRecycleItemsSync(): List<com.example.data.model.RecycleBinItem>

    @Query("SELECT * FROM recycle_bin WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): com.example.data.model.RecycleBinItem?

    @Query("SELECT COUNT(*) FROM recycle_bin")
    fun getRecycleCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: com.example.data.model.RecycleBinItem): Long

    @Query("DELETE FROM recycle_bin WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recycle_bin")
    suspend fun clearAll()

    @Query("DELETE FROM recycle_bin WHERE (:currentTime - deletedAt) > :olderThanMillis")
    suspend fun purgeOlderThan(currentTime: Long, olderThanMillis: Long)
}

@Dao
interface AdminGlobalConfigDao {
    @Query("SELECT * FROM admin_global_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<com.example.data.model.AdminGlobalConfig?>

    @Query("SELECT * FROM admin_global_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigSync(): com.example.data.model.AdminGlobalConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: com.example.data.model.AdminGlobalConfig)
}

