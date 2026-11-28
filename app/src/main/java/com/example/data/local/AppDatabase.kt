package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Customer
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.ShopSettings
import com.example.data.model.SizePricing
import com.example.data.model.TileProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.example.data.model.StaffAttendance
import com.example.data.model.StaffMember
import com.example.data.model.StaffPayment

@Database(
    entities = [
        ShopSettings::class,
        SizePricing::class,
        TileProduct::class,
        Customer::class,
        OrderEntity::class,
        OrderItemEntity::class,
        com.example.data.model.StockMovement::class,
        StaffMember::class,
        StaffAttendance::class,
        StaffPayment::class,
        com.example.data.model.StaffMonthlySettlement::class,
        com.example.data.model.ClientLicense::class,
        com.example.data.model.AppUser::class,
        com.example.data.model.RecycleBinItem::class,
        com.example.data.model.AdminGlobalConfig::class,
        com.example.data.model.TileLocation::class,
        com.example.data.model.OrderDeliveryLog::class,
        com.example.data.model.OrderReturnExchangeLog::class
    ],
    version = 19,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shopSettingsDao(): ShopSettingsDao
    abstract fun sizePricingDao(): SizePricingDao
    abstract fun tileLocationDao(): TileLocationDao
    abstract fun tileProductDao(): TileProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun orderDeliveryDao(): OrderDeliveryDao
    abstract fun orderReturnExchangeDao(): OrderReturnExchangeDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun staffDao(): StaffDao
    abstract fun clientLicenseDao(): ClientLicenseDao
    abstract fun appUserDao(): AppUserDao
    abstract fun recycleBinDao(): RecycleBinDao
    abstract fun adminGlobalConfigDao(): AdminGlobalConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE staff_members ADD COLUMN openingBalance REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN brandAppName TEXT NOT NULL DEFAULT 'TILES AND MARBLES'")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN latestAppVersionCode INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN latestAppVersionName TEXT NOT NULL DEFAULT '1.0.0'")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN latestApkDownloadUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN updateChangelog TEXT NOT NULL DEFAULT 'नई सुविधाएं व बग फिक्स'")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN isForceUpdate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN telegramEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN telegramBotToken TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN telegramChatId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN telegramFormat TEXT NOT NULL DEFAULT 'HINDI_DETAILED'")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN telegramCustomTitle TEXT NOT NULL DEFAULT 'TILES AND MARBLES'")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN telegramAlertNewSale INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN telegramAlertPayment INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN telegramAlertLowStock INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN telegramAlertAttendance INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN telegramAlertSalaryAdvance INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN telegramAlertNewProduct INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN telegramAlertInvoiceDeleted INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE admin_global_config ADD COLUMN telegramAlertUserAction INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Orders payment & balance columns
                db.execSQL("ALTER TABLE orders ADD COLUMN paidAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE orders ADD COLUMN balanceAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE orders ADD COLUMN paymentHistoryJson TEXT NOT NULL DEFAULT ''")

                // Shop settings columns for auto backup & auto attendance
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN autoAttendanceEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN autoBackupEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN backupTargetEmail TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN backupScheduledTime TEXT NOT NULL DEFAULT '20:00'")
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN backupFromDateString TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN lastAutoBackupDate TEXT NOT NULL DEFAULT ''")

                // Staff Monthly Settlements table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS staff_monthly_settlements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        staffId INTEGER NOT NULL,
                        monthKey TEXT NOT NULL,
                        isFinalized INTEGER NOT NULL DEFAULT 0,
                        settledAt INTEGER NOT NULL,
                        notes TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_staff_monthly_settlements_staffId_monthKey ON staff_monthly_settlements (staffId, monthKey)")
            }
        }

        val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE staff_members ADD COLUMN salaryCalculationType TEXT NOT NULL DEFAULT 'FIXED_30_DAYS'")
                db.execSQL("ALTER TABLE staff_members ADD COLUMN dailyWageRate REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE staff_members ADD COLUMN hasWeeklyOff INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE staff_members ADD COLUMN weeklyOffDay TEXT NOT NULL DEFAULT 'SUNDAY'")
                db.execSQL("ALTER TABLE staff_members ADD COLUMN workingDaysPerMonth INTEGER NOT NULL DEFAULT 26")
            }
        }

        val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE size_pricing ADD COLUMN defaultPiecesPerBox INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN pdfSizeDisplayConfigJson TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN autoAttendanceTime TEXT NOT NULL DEFAULT '09:00'")
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN lastAutoAttendanceDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN backupFolderUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN backupFolderName TEXT NOT NULL DEFAULT 'TilesAndMarbles_Backups'")
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN driveWebhookUrl TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Tile Storage Locations table + location column on tile_products
                db.execSQL("ALTER TABLE tile_products ADD COLUMN location TEXT NOT NULL DEFAULT ''")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tile_locations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tile_locations_name ON tile_locations (name)")
                val now = System.currentTimeMillis()
                db.execSQL("INSERT OR IGNORE INTO tile_locations (name, createdAt) VALUES ('दुकान (Shop)', $now)")
                db.execSQL("INSERT OR IGNORE INTO tile_locations (name, createdAt) VALUES ('गोदाम (Godown)', $now)")
                db.execSQL("INSERT OR IGNORE INTO tile_locations (name, createdAt) VALUES ('वेयरहाउस (Warehouse)', $now)")

                // 2. Partial / Staged Tile Delivery tracking columns & delivery logs table
                db.execSQL("ALTER TABLE orders ADD COLUMN deliveryStatus TEXT NOT NULL DEFAULT 'FULL_DELIVERED'")
                db.execSQL("ALTER TABLE order_items ADD COLUMN deliveredBoxes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE order_items ADD COLUMN deliveredPieces INTEGER NOT NULL DEFAULT 0")
                // Mark existing historical orders' items as delivered so old bills stay consistent
                db.execSQL("UPDATE order_items SET deliveredBoxes = fullBoxes, deliveredPieces = loosePieces")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS order_deliveries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        orderId INTEGER NOT NULL,
                        orderItemId INTEGER NOT NULL,
                        tileCode TEXT NOT NULL,
                        tileName TEXT NOT NULL,
                        tileSize TEXT NOT NULL,
                        piecesPerBox INTEGER NOT NULL,
                        boxesDelivered INTEGER NOT NULL,
                        piecesDelivered INTEGER NOT NULL DEFAULT 0,
                        deliveredAt INTEGER NOT NULL,
                        receiverName TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_18_19 = object : androidx.room.migration.Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Per-order PDF display toggles
                db.execSQL("ALTER TABLE orders ADD COLUMN showSizeTotalOnPdf INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE orders ADD COLUMN showDeliveryStatusOnPdf INTEGER NOT NULL DEFAULT 1")

                // 2. Marble / Stone calculation fields on order_items
                db.execSQL("ALTER TABLE order_items ADD COLUMN itemType TEXT NOT NULL DEFAULT 'TILE'")
                db.execSQL("ALTER TABLE order_items ADD COLUMN marbleLength REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE order_items ADD COLUMN marbleWidth REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE order_items ADD COLUMN marbleUnit TEXT NOT NULL DEFAULT 'INCH'")
                db.execSQL("ALTER TABLE order_items ADD COLUMN marblePieces INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE order_items ADD COLUMN marbleTotalSqFt REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE order_items ADD COLUMN marbleRatePerSqFt REAL NOT NULL DEFAULT 0.0")

                // 3. Return & Exchange history table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS order_return_exchanges (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        orderId INTEGER NOT NULL,
                        returnedItemId INTEGER NOT NULL DEFAULT 0,
                        returnedTileCode TEXT NOT NULL DEFAULT '',
                        returnedTileName TEXT NOT NULL DEFAULT '',
                        returnedTileSize TEXT NOT NULL DEFAULT '',
                        returnedBoxes INTEGER NOT NULL DEFAULT 0,
                        returnedPieces INTEGER NOT NULL DEFAULT 0,
                        returnedAmount REAL NOT NULL DEFAULT 0.0,
                        outcomeType TEXT NOT NULL DEFAULT 'REFUND_BALANCE',
                        replacementItemName TEXT NOT NULL DEFAULT '',
                        replacementTileCode TEXT NOT NULL DEFAULT '',
                        replacementTileSize TEXT NOT NULL DEFAULT '',
                        replacementBoxes INTEGER NOT NULL DEFAULT 0,
                        replacementPieces INTEGER NOT NULL DEFAULT 0,
                        replacementAmount REAL NOT NULL DEFAULT 0.0,
                        replacementDeliveryStatus TEXT NOT NULL DEFAULT 'NONE',
                        netAmountDifference REAL NOT NULL DEFAULT 0.0,
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mourya_tiles.db"
                )
                    .addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate defaults
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                database.shopSettingsDao().saveSettings(ShopSettings())
                                database.adminGlobalConfigDao().saveConfig(com.example.data.model.AdminGlobalConfig())
                                database.tileLocationDao().insert(com.example.data.model.TileLocation(name = "दुकान (Shop)"))
                                database.tileLocationDao().insert(com.example.data.model.TileLocation(name = "गोदाम (Godown)"))
                                database.tileLocationDao().insert(com.example.data.model.TileLocation(name = "वेयरहाउस (Warehouse)"))
                                database.sizePricingDao().insert(SizePricing(sizeName = "12x18", defaultPricePerBox = 340.0, defaultPiecesPerBox = 6))
                                database.sizePricingDao().insert(SizePricing(sizeName = "2x4", defaultPricePerBox = 750.0, defaultPiecesPerBox = 2))
                                database.sizePricingDao().insert(SizePricing(sizeName = "2x2", defaultPricePerBox = 560.0, defaultPiecesPerBox = 4))
                                database.sizePricingDao().insert(SizePricing(sizeName = "16x16", defaultPricePerBox = 420.0, defaultPiecesPerBox = 5))
                                database.sizePricingDao().insert(SizePricing(sizeName = "8x12", defaultPricePerBox = 280.0, defaultPiecesPerBox = 8))

                                // Pre-populate sample commercial tile catalog to jumpstart usability
                                val p1 = TileProduct(
                                    code = "MTM-101",
                                    name = "Royal Statuario Gold Vitrified",
                                    size = "2x4",
                                    description = "High Gloss Porcelain Polished Slab",
                                    piecesPerBox = 2,
                                    stockPieces = 150 * 2, // 150 boxes
                                    defaultSellingPrice = 780.0,
                                    purchaseCost = 620.0
                                )
                                val p2 = TileProduct(
                                    code = "MTM-102",
                                    name = "Spanish Crema Onyx Marble Finish",
                                    size = "2x4",
                                    description = "Mirror Polished Double Charge",
                                    piecesPerBox = 2,
                                    stockPieces = 80 * 2,
                                    defaultSellingPrice = 820.0,
                                    purchaseCost = 650.0
                                )
                                val p3 = TileProduct(
                                    code = "MTM-103",
                                    name = "Classic Glossy Ceramic Wall Tile",
                                    size = "12x18",
                                    description = "Kitchen & Bathroom Wall Highlighter",
                                    piecesPerBox = 6,
                                    stockPieces = 200 * 6, // 200 boxes
                                    defaultSellingPrice = 350.0,
                                    purchaseCost = 280.0
                                )
                                val p4 = TileProduct(
                                    code = "MTM-104",
                                    name = "Dark Wood Plank Elevation Tile",
                                    size = "12x18",
                                    description = "Exterior Weatherproof Rustic Plank",
                                    piecesPerBox = 6,
                                    stockPieces = 9 * 6, // 9 boxes (Low Stock Alert!)
                                    defaultSellingPrice = 380.0,
                                    purchaseCost = 300.0
                                )
                                val p5 = TileProduct(
                                    code = "MTM-105",
                                    name = "Heavy Duty Anti-Skid Parking Tile",
                                    size = "16x16",
                                    description = "Full Body Vitrified Floor Paver",
                                    piecesPerBox = 5,
                                    stockPieces = 120 * 5,
                                    defaultSellingPrice = 450.0,
                                    purchaseCost = 360.0
                                )
                                database.tileProductDao().insert(p1)
                                database.tileProductDao().insert(p2)
                                database.tileProductDao().insert(p3)
                                database.tileProductDao().insert(p4)
                                database.tileProductDao().insert(p5)

                                // Sample customer
                                val c1 = Customer(
                                    name = "Rajesh Sharma",
                                    phone = "9823456780",
                                    address = "Plot 42, Green Park Colony",
                                    notes = "Home renovation project"
                                )
                                val c2 = Customer(
                                    name = "Balaji Construction Ltd",
                                    phone = "9412398765",
                                    address = "Bypass Road, Near Toll Plaza",
                                    notes = "Commercial Builder"
                                )
                                database.customerDao().insert(c1)
                                database.customerDao().insert(c2)

                                // Initial Stock Movements matching UI design
                                database.stockMovementDao().insert(
                                    com.example.data.model.StockMovement(
                                        title = "12X18 (23019)",
                                        description = "Initial Stock",
                                        changeDisplay = "+50 bxs, 0 pcs",
                                        isPositive = true
                                    )
                                )
                                database.stockMovementDao().insert(
                                    com.example.data.model.StockMovement(
                                        title = "12×18 Tiel (26019-L)",
                                        description = "Sale Cancelled / Restored • #MTM-2026-0001",
                                        changeDisplay = "+4 bxs, 0 pcs",
                                        isPositive = true
                                    )
                                )

                                // Seed default client account for user login
                                database.appUserDao().insert(
                                    com.example.data.model.AppUser(
                                        name = "दुकानदार",
                                        shopName = "TILES AND MARBLES",
                                        phone = "9876543210",
                                        password = "123456",
                                        planDays = 365,
                                        planName = "1 Year (1 वर्ष)",
                                        amountCharged = 3499.0,
                                        notes = "Master Client Account"
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
