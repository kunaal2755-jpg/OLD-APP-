package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "shop_settings")
data class ShopSettings(
    @PrimaryKey val id: Int = 1,
    val shopName: String = "",
    val subtitle: String = "",
    val ownerName: String = "",
    val ownerPhone: String = "",
    val altPhone: String = "",
    val address: String = "",
    val logoUri: String? = null,
    val gstNumber: String = "",
    val panNumber: String = "",
    val bankName: String = "",
    val bankAccountNumber: String = "",
    val bankIfsc: String = "",
    val bankBranch: String = "",
    val invoicePrefix: String = "INV-",
    val defaultTaxPercent: Double = 0.0,
    val showQrOnInvoice: Boolean = true,
    val showBankOnInvoice: Boolean = true,
    val invoiceFooterNotes: String = "Thank you for your business! Visit again.",
    val termsNotes: String = "1. Goods once sold will not be taken back without original invoice.\n2. Breakage allowance as per standard industry norms.\n3. Payment due within 15 days of invoice date.",
    val appTheme: String = "NAVY_GOLD",
    val upiId: String = "",
    // Telegram Bot Notifications
    val telegramBotToken: String = "",
    val telegramChatId: String = "",
    val telegramEnabled: Boolean = false,
    val telegramFormat: String = "DETAILED_HINDI", // "DETAILED_HINDI", "COMPACT_HINDI", "ENGLISH_PRO"
    val telegramCustomTitle: String = "TILES AND MARBLES",
    val notifyNewSale: Boolean = true,
    val notifyPaymentReceived: Boolean = true,
    val notifyLowStock: Boolean = true,
    val notifyStaffAttendance: Boolean = true,
    val notifyDailySummary: Boolean = true,
    // Auto Attendance
    val autoAttendanceEnabled: Boolean = false,
    val autoAttendanceTime: String = "09:00",
    val lastAutoAttendanceDate: String = "",
    // Cumulative Daily Evening Backup & Drive/Email Upload
    val autoBackupEnabled: Boolean = true,
    val backupTargetEmail: String = "",
    val backupScheduledTime: String = "20:00", // 8:00 PM default
    val backupFromDateString: String = "", // e.g. "01/11/2026"
    val lastAutoBackupDate: String = "",
    val backupFolderUri: String = "", // SAF tree URI chosen by user for any custom mobile folder
    val backupFolderName: String = "TilesAndMarbles_Backups", // Custom folder inside Downloads/
    val driveWebhookUrl: String = "", // Optional Google Apps Script Web App URL for unattended Google Drive + Email upload
    // Custom PDF Tile Size Display Configuration (JSON)
    val pdfSizeDisplayConfigJson: String = ""
)

@Entity(tableName = "size_pricing")
data class SizePricing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sizeName: String, // e.g. "12x18", "2x4", "2x2", "16x16"
    val defaultPricePerBox: Double,
    val defaultPiecesPerBox: Int = 0
)

@Entity(
    tableName = "tile_locations",
    indices = [Index(value = ["name"], unique = true)]
)
data class TileLocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // e.g. "दुकान (Shop)", "गोदाम (Godown)", "वेयरहाउस (Warehouse)"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tile_products")
data class TileProduct(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String, // e.g. "MTM-101", must be unique
    val name: String, // e.g. "Royal Statuario White"
    val size: String, // e.g. "12x18", "2x4"
    val photoUri: String? = null,
    val description: String = "",
    val piecesPerBox: Int = 5, // each product has its own custom pieces/box
    val stockPieces: Int = 0, // stored in pieces as base unit
    val defaultSellingPrice: Double = 0.0, // default price per box
    val purchaseCost: Double = 0.0, // cost price per box for internal profit
    val createdAt: Long = System.currentTimeMillis(),
    val location: String = "" // Tile storage location from Settings (Godown, Warehouse, Dukaan, etc.)
) {
    val fullBoxes: Int get() = if (piecesPerBox > 0) stockPieces / piecesPerBox else 0
    val loosePieces: Int get() = if (piecesPerBox > 0) stockPieces % piecesPerBox else 0
    val stockDisplay: String
        get() = "$fullBoxes Bx" + if (loosePieces > 0) " + $loosePieces Pcs" else ""
    val isLowStock: Boolean get() = fullBoxes <= 10
}

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val outstandingBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String, // e.g. "MTM-2026-0001"
    val customerId: Long? = null,
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",
    val status: String = "PENDING", // "PENDING" or "CONFIRMED"
    val showPriceOnPdf: Boolean = true, // Per-order PDF Price Visibility Switch
    val showSizeTotalOnPdf: Boolean = true, // Per-order PDF Size-wise Box Total Switch
    val showDeliveryStatusOnPdf: Boolean = true, // Per-order PDF Delivery (Taken/Pending) Switch
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paidAmount: Double = 0.0, // जमा राशि (Received / Paid Amount)
    val balanceAmount: Double = 0.0, // बकाया राशि (Remaining Balance Due)
    val paymentHistoryJson: String = "", // JSON list of partial installments
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val confirmedAt: Long? = null,
    val deliveryStatus: String = "FULL_DELIVERED" // "FULL_DELIVERED", "PARTIAL_PENDING", "NOT_DELIVERED"
)

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long = 0,
    val productId: Long,
    val tileCode: String,
    val tileName: String,
    val tileSize: String,
    val piecesPerBox: Int,
    val tilePhotoUri: String? = null,
    val fullBoxes: Int,
    val loosePieces: Int = 0,
    val pricePerBox: Double,
    val lineTotal: Double,
    val deliveredBoxes: Int = 0,
    val deliveredPieces: Int = 0,
    val itemType: String = "TILE", // "TILE", "MARBLE", "MANUAL"
    val marbleLength: Double = 0.0,
    val marbleWidth: Double = 0.0,
    val marbleUnit: String = "INCH", // "INCH" or "FEET"
    val marblePieces: Int = 0,
    val marbleTotalSqFt: Double = 0.0,
    val marbleRatePerSqFt: Double = 0.0
) {
    val isMarble: Boolean get() = itemType == "MARBLE"
    // Total pieces calculated internally for inventory adjustments
    val totalPieces: Int get() = (fullBoxes * piecesPerBox) + loosePieces
    val deliveredTotalPieces: Int get() = ((deliveredBoxes * piecesPerBox.coerceAtLeast(1)) + deliveredPieces).coerceIn(0, totalPieces)
    val pendingTotalPieces: Int get() = (totalPieces - deliveredTotalPieces).coerceAtLeast(0)
    val pendingFullBoxes: Int get() = if (piecesPerBox > 0) pendingTotalPieces / piecesPerBox else 0
    val pendingLoosePieces: Int get() = if (piecesPerBox > 0) pendingTotalPieces % piecesPerBox else 0
    val isFullyDelivered: Boolean get() = pendingTotalPieces <= 0
}

@Entity(tableName = "order_deliveries")
data class OrderDeliveryLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val orderItemId: Long,
    val tileCode: String,
    val tileName: String,
    val tileSize: String,
    val piecesPerBox: Int,
    val boxesDelivered: Int,
    val piecesDelivered: Int = 0,
    val deliveredAt: Long = System.currentTimeMillis(),
    val receiverName: String = "",
    val notes: String = ""
) {
    val totalPiecesDelivered: Int get() = (boxesDelivered * piecesPerBox.coerceAtLeast(1)) + piecesDelivered
}

@Entity(tableName = "order_return_exchanges")
data class OrderReturnExchangeLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val returnedItemId: Long = 0,
    val returnedTileCode: String = "",
    val returnedTileName: String = "",
    val returnedTileSize: String = "",
    val returnedBoxes: Int = 0,
    val returnedPieces: Int = 0,
    val returnedAmount: Double = 0.0,
    val outcomeType: String = "REFUND_BALANCE", // "REFUND_CASH", "REFUND_BALANCE", "EXCHANGE_TILE", "EXCHANGE_OTHER"
    val replacementItemName: String = "",
    val replacementTileCode: String = "",
    val replacementTileSize: String = "",
    val replacementBoxes: Int = 0,
    val replacementPieces: Int = 0,
    val replacementAmount: Double = 0.0,
    val replacementDeliveryStatus: String = "NONE", // "TAKEN", "HOLD", "NONE"
    val netAmountDifference: Double = 0.0, // >0 customer pays extra, <0 refund/credit
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_movements")
data class StockMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, // e.g. "12X18 (23019)"
    val description: String, // e.g. "Initial Stock" or "Sale Confirmed • #MTM-2026-0001"
    val changeDisplay: String, // e.g. "+50 bxs, 0 pcs"
    val isPositive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class OrderWithItems(
    val order: OrderEntity,
    val items: List<OrderItemEntity>
)

@Entity(tableName = "staff_members")
data class StaffMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val role: String = "Worker", // "Salesman", "Karigar / Fitter", "Helper", "Driver", "Munim"
    val monthlySalary: Double = 0.0, // Per month salary
    val joiningDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val notes: String = "",
    val openingBalance: Double = 0.0, // पिछला बकाया / पूर्व हिसाब (Previous Balance)
    val salaryCalculationType: String = "FIXED_30_DAYS", // "FIXED_30_DAYS", "ACTUAL_MONTH_DAYS", "WORKING_DAYS", "DAILY_WAGE"
    val dailyWageRate: Double = 0.0, // Used when salaryCalculationType == "DAILY_WAGE"
    val hasWeeklyOff: Boolean = true, // साप्ताहिक छुट्टी रखनी है या नहीं (Yes/No)
    val weeklyOffDay: String = "SUNDAY",
    val workingDaysPerMonth: Int = 26 // For working days calculation
)

@Entity(
    tableName = "staff_attendance",
    indices = [Index(value = ["staffId", "dateString"], unique = true)]
)
data class StaffAttendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val dateMillis: Long,
    val dateString: String, // "yyyy-MM-dd"
    val status: String = "PRESENT", // "PRESENT", "HALF_DAY", "ABSENT", "PAID_LEAVE"
    val notes: String = ""
)

@Entity(tableName = "staff_payments")
data class StaffPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val dateMillis: Long = System.currentTimeMillis(),
    val amount: Double,
    val type: String = "ADVANCE", // "ADVANCE", "SALARY", "BONUS"
    val forMonthYear: String = "", // "MMM yyyy" e.g. "Oct 2026"
    val paymentMode: String = "CASH", // "CASH", "UPI", "BANK"
    val notes: String = ""
)

data class StaffMonthlySummary(
    val staff: StaffMember,
    val monthYear: String,
    val totalDaysInMonth: Int,
    val presentDays: Int,
    val halfDays: Int,
    val absentDays: Int,
    val paidLeaveDays: Int,
    val totalAdvanceTaken: Double,
    val totalSalaryPaid: Double,
    val earnedSalary: Double,
    val netBalancePayable: Double,
    val previousUnfinalizedBalance: Double = 0.0,
    val unfinalizedMonthsText: String = "",
    val isFinalized: Boolean = false
)

@Entity(
    tableName = "staff_monthly_settlements",
    indices = [Index(value = ["staffId", "monthKey"], unique = true)]
)
data class StaffMonthlySettlement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val monthKey: String, // "yyyy-MM" e.g. "2026-10"
    val isFinalized: Boolean = false, // true = Yes (हिसाब फाइनल/चुकता), false = No (बकाया आगे जुड़े)
    val settledAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

data class UnfinalizedMonthDetail(
    val monthKey: String, // "yyyy-MM"
    val monthDisplay: String, // "Oct 2026"
    val earnedGross: Double,
    val advancesTaken: Double,
    val salaryPaid: Double,
    val netUnpaid: Double
)

@Entity(tableName = "client_licenses")
data class ClientLicense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientName: String,
    val shopName: String,
    val phone: String,
    val deviceId: String = "UNIVERSAL", // Specific Device ID or "UNIVERSAL"
    val planDays: Int = 365, // 30, 90, 180, 365, -1 for lifetime
    val planName: String = "1 Year",
    val amountCharged: Double = 0.0,
    val activationKey: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val isActive: Boolean = true,
    val notes: String = ""
)

@Entity(tableName = "app_users")
data class AppUser(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val shopName: String,
    val phone: String, // Mobile number (User ID)
    val password: String, // Password
    val role: String = "CLIENT", // "CLIENT" or "ADMIN"
    val planDays: Int = 365,
    val planName: String = "1 Year (1 वर्ष)",
    val amountCharged: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000L),
    val isActive: Boolean = true,
    val notes: String = "",
    val approvalStatus: String = "APPROVED", // "PENDING", "APPROVED", "REJECTED"
    val address: String = "",
    val lastCloudBackupAt: Long = 0L,
    val cloudBackupData: String? = null
)

@Entity(tableName = "recycle_bin")
data class RecycleBinItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userPhone: String = "",
    val itemType: String, // "ORDER", "PRODUCT", "CUSTOMER", "STAFF"
    val originalId: Long,
    val title: String,
    val subtitle: String,
    val payloadJson: String,
    val deletedAt: Long = System.currentTimeMillis()
) {
    val daysRemaining: Int
        get() {
            val elapsed = System.currentTimeMillis() - deletedAt
            val tenDays = 10L * 24 * 60 * 60 * 1000L
            val rem = (tenDays - elapsed) / (24 * 60 * 60 * 1000L)
            return rem.toInt().coerceAtLeast(0)
        }
}

@Entity(tableName = "admin_global_config")
data class AdminGlobalConfig(
    @PrimaryKey val id: Int = 1,
    val isMaintenanceMode: Boolean = false,
    val maintenanceMessage: String = "⚠️ सिस्टम में तकनीकी रखरखाव चल रहा है। कृपया कुछ देर बाद लॉगिन करें।",
    val allowUserSelfRegistration: Boolean = true,
    val requireAdminApproval: Boolean = true,
    val defaultTrialDays: Int = 30,
    val allowInvoiceDeletion: Boolean = true,
    val allowBillingDiscounts: Boolean = true,
    val enforceSingleDailyAttendance: Boolean = true,
    val allowSalaryAdvance: Boolean = true,
    val lowStockThreshold: Int = 10,
    val broadcastAnnouncement: String = "",
    val showBroadcastAnnouncement: Boolean = false,
    val customAdminPin: String = "8899",
    val adminPhone: String = "+91 98765 43210",
    val adminWhatsapp: String = "9876543210",
    val adminEmail: String = "cp622499@gmail.com",
    val recycleBinRetentionDays: Int = 10,
    // Brand Name configured from Admin App
    val brandAppName: String = "TILES AND MARBLES",
    // In-App Update System
    val latestAppVersionCode: Int = 1,
    val latestAppVersionName: String = "1.0.0",
    val latestApkDownloadUrl: String = "",
    val updateChangelog: String = "नई सुविधाएं व बग फिक्स",
    val isForceUpdate: Boolean = false,
    // Centralized Telegram Bot Alerts & Controls
    val telegramEnabled: Boolean = false,
    val telegramBotToken: String = "",
    val telegramChatId: String = "",
    val telegramFormat: String = "HINDI_DETAILED",
    val telegramCustomTitle: String = "TILES AND MARBLES",
    val telegramAlertNewSale: Boolean = true,
    val telegramAlertPayment: Boolean = true,
    val telegramAlertLowStock: Boolean = true,
    val telegramAlertAttendance: Boolean = true,
    val telegramAlertSalaryAdvance: Boolean = true,
    val telegramAlertNewProduct: Boolean = true,
    val telegramAlertInvoiceDeleted: Boolean = true,
    val telegramAlertUserAction: Boolean = true
)


