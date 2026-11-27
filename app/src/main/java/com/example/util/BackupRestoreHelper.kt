package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.ShopSettings
import com.example.data.model.SizePricing
import com.example.data.model.StaffAttendance
import com.example.data.model.StaffMember
import com.example.data.model.StaffPayment
import com.example.data.model.TileProduct
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.CRC32

object BackupRestoreHelper {

    private class EmbeddedImageCollector(
        private val context: Context?,
        private val includeImages: Boolean,
        private val maxDim: Int,
        private val jpegQuality: Int
    ) {
        private val uriToKey = mutableMapOf<String, String>()
        val embeddedImagesJson = JSONObject()
        var count: Int = 0
            private set

        fun registerImage(uriString: String?): String {
            if (!includeImages || uriString.isNullOrBlank()) return ""
            val trimmed = uriString.trim()
            uriToKey[trimmed]?.let { return it }

            val encoded = encodeImageToBase64(context, trimmed, maxDim, jpegQuality) ?: return ""
            count++
            val key = "img_$count"
            uriToKey[trimmed] = key
            embeddedImagesJson.put(key, encoded)
            return key
        }
    }

    private fun encodeImageToBase64(
        context: Context?,
        uriString: String,
        maxDim: Int = 800,
        jpegQuality: Int = 78
    ): String? {
        return try {
            val isDirectPath = uriString.startsWith("/") || uriString.startsWith("file://")
            val filePath = if (uriString.startsWith("file://")) uriString.removePrefix("file://") else uriString

            val decodedBitmap: Bitmap? = if (isDirectPath && File(filePath).exists()) {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(filePath, boundsOpts)
                val origMax = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                if (origMax <= 0) return null
                var sampleSize = 1
                while (origMax / (sampleSize * 2) >= maxDim) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeFile(filePath, decodeOpts)
            } else if (context != null) {
                val uri = Uri.parse(uriString)
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOpts)
                }
                val origMax = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                if (origMax <= 0) return null
                var sampleSize = 1
                while (origMax / (sampleSize * 2) >= maxDim) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                }
            } else {
                null
            }

            if (decodedBitmap == null) return null

            val finalBitmap = try {
                val curMax = maxOf(decodedBitmap.width, decodedBitmap.height)
                if (curMax > maxDim && curMax > 0) {
                    val scale = maxDim.toFloat() / curMax.toFloat()
                    val targetW = (decodedBitmap.width * scale).toInt().coerceAtLeast(1)
                    val targetH = (decodedBitmap.height * scale).toInt().coerceAtLeast(1)
                    val scaled = Bitmap.createScaledBitmap(decodedBitmap, targetW, targetH, true)
                    if (scaled !== decodedBitmap) decodedBitmap.recycle()
                    scaled
                } else {
                    decodedBitmap
                }
            } catch (_: Exception) {
                decodedBitmap
            }

            val baos = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(40, 92), baos)
            finalBitmap.recycle()
            val bytes = baos.toByteArray()
            if (bytes.isEmpty()) null else Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private fun saveBase64ImageToLocalStorage(context: Context, base64Str: String): String? {
        return try {
            if (base64Str.isBlank()) return null
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            if (bytes.isEmpty()) return null

            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) imagesDir.mkdirs()

            val crc = CRC32().apply { update(bytes) }.value
            val fileName = "restored_${crc.toString(16)}_${bytes.size}.jpg"
            val destFile = File(imagesDir, fileName)
            if (!destFile.exists() || destFile.length() == 0L) {
                FileOutputStream(destFile).use { fos ->
                    fos.write(bytes)
                }
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun extractEmbeddedImages(context: Context?, root: JSONObject): Map<String, String> {
        if (context == null || !root.has("embeddedImages")) return emptyMap()
        val result = mutableMapOf<String, String>()
        val obj = root.optJSONObject("embeddedImages") ?: return emptyMap()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val b64 = obj.optString(key, "")
            val savedPath = saveBase64ImageToLocalStorage(context, b64)
            if (!savedPath.isNullOrBlank()) {
                result[key] = savedPath
            }
        }
        return result
    }

    private fun isLocalImageAccessible(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        val path = if (uriString.startsWith("file://")) uriString.removePrefix("file://") else uriString
        return path.startsWith("/") && File(path).exists() && File(path).length() > 0L
    }

    suspend fun exportDatabaseToJson(
        database: AppDatabase,
        context: Context? = null,
        includeImages: Boolean = true,
        maxImageDim: Int = 800,
        jpegQuality: Int = 78
    ): String {
        val root = JSONObject()
        root.put("version", 4)
        root.put("timestamp", System.currentTimeMillis())

        val imageCollector = EmbeddedImageCollector(
            context = context,
            includeImages = includeImages,
            maxDim = maxImageDim,
            jpegQuality = jpegQuality
        )

        // 1. Settings
        val settings = database.shopSettingsDao().getSettingsSync() ?: ShopSettings()
        val settingsObj = JSONObject().apply {
            put("shopName", settings.shopName)
            put("subtitle", settings.subtitle)
            put("ownerName", settings.ownerName)
            put("ownerPhone", settings.ownerPhone)
            put("altPhone", settings.altPhone)
            put("address", settings.address)
            put("logoUri", settings.logoUri ?: "")
            val logoKey = imageCollector.registerImage(settings.logoUri)
            if (logoKey.isNotEmpty()) put("logoImageKey", logoKey)
            put("gstNumber", settings.gstNumber)
            put("panNumber", settings.panNumber)
            put("bankName", settings.bankName)
            put("bankAccountNumber", settings.bankAccountNumber)
            put("bankIfsc", settings.bankIfsc)
            put("bankBranch", settings.bankBranch)
            put("invoicePrefix", settings.invoicePrefix)
            put("defaultTaxPercent", settings.defaultTaxPercent)
            put("showQrOnInvoice", settings.showQrOnInvoice)
            put("showBankOnInvoice", settings.showBankOnInvoice)
            put("invoiceFooterNotes", settings.invoiceFooterNotes)
            put("termsNotes", settings.termsNotes)
            put("appTheme", settings.appTheme)
            put("upiId", settings.upiId)
            put("telegramBotToken", settings.telegramBotToken)
            put("telegramChatId", settings.telegramChatId)
            put("telegramEnabled", settings.telegramEnabled)
            put("telegramFormat", settings.telegramFormat)
            put("telegramCustomTitle", settings.telegramCustomTitle)
            put("autoAttendanceEnabled", settings.autoAttendanceEnabled)
            put("autoBackupEnabled", settings.autoBackupEnabled)
            put("backupTargetEmail", settings.backupTargetEmail)
            put("backupScheduledTime", settings.backupScheduledTime)
            put("backupFromDateString", settings.backupFromDateString)
            put("lastAutoBackupDate", settings.lastAutoBackupDate)
            put("pdfSizeDisplayConfigJson", settings.pdfSizeDisplayConfigJson)
        }
        root.put("settings", settingsObj)

        // 2. Size Pricing
        val pricings = database.sizePricingDao().getAllPricingsSync()
        val pricingArr = JSONArray()
        for (p in pricings) {
            val po = JSONObject().apply {
                put("sizeName", p.sizeName)
                put("defaultPricePerBox", p.defaultPricePerBox)
                put("defaultPiecesPerBox", p.defaultPiecesPerBox)
            }
            pricingArr.put(po)
        }
        root.put("sizePricing", pricingArr)

        // 3. Tile Products
        val products = database.tileProductDao().getAllProductsSync()
        val productArr = JSONArray()
        for (prod in products) {
            val po = JSONObject().apply {
                put("id", prod.id)
                put("code", prod.code)
                put("name", prod.name)
                put("size", prod.size)
                put("photoUri", prod.photoUri ?: "")
                val photoKey = imageCollector.registerImage(prod.photoUri)
                if (photoKey.isNotEmpty()) put("photoImageKey", photoKey)
                put("description", prod.description)
                put("piecesPerBox", prod.piecesPerBox)
                put("stockPieces", prod.stockPieces)
                put("defaultSellingPrice", prod.defaultSellingPrice)
                put("purchaseCost", prod.purchaseCost)
                put("createdAt", prod.createdAt)
            }
            productArr.put(po)
        }
        root.put("products", productArr)

        // 4. Customers
        val customers = database.customerDao().getAllCustomersSync()
        val custArr = JSONArray()
        for (c in customers) {
            val co = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phone", c.phone)
                put("address", c.address)
                put("notes", c.notes)
                put("outstandingBalance", c.outstandingBalance)
                put("createdAt", c.createdAt)
            }
            custArr.put(co)
        }
        root.put("customers", custArr)

        // 5. Orders
        val orders = database.orderDao().getAllOrdersSync()
        val orderArr = JSONArray()
        for (o in orders) {
            val oo = JSONObject().apply {
                put("id", o.id)
                put("invoiceNumber", o.invoiceNumber)
                put("customerId", o.customerId ?: -1L)
                put("customerName", o.customerName)
                put("customerPhone", o.customerPhone)
                put("customerAddress", o.customerAddress)
                put("status", o.status)
                put("showPriceOnPdf", o.showPriceOnPdf)
                put("subtotal", o.subtotal)
                put("discount", o.discount)
                put("grandTotal", o.grandTotal)
                put("paidAmount", o.paidAmount)
                put("balanceAmount", o.balanceAmount)
                put("paymentHistoryJson", o.paymentHistoryJson)
                put("notes", o.notes)
                put("createdAt", o.createdAt)
                put("confirmedAt", o.confirmedAt ?: -1L)
            }
            orderArr.put(oo)
        }
        root.put("orders", orderArr)

        // 6. Order Items
        val items = database.orderItemDao().getAllItemsSync()
        val itemArr = JSONArray()
        for (item in items) {
            val io = JSONObject().apply {
                put("id", item.id)
                put("orderId", item.orderId)
                put("productId", item.productId)
                put("tileCode", item.tileCode)
                put("tileName", item.tileName)
                put("tileSize", item.tileSize)
                put("piecesPerBox", item.piecesPerBox)
                put("tilePhotoUri", item.tilePhotoUri ?: "")
                val itemPhotoKey = imageCollector.registerImage(item.tilePhotoUri)
                if (itemPhotoKey.isNotEmpty()) put("tilePhotoImageKey", itemPhotoKey)
                put("fullBoxes", item.fullBoxes)
                put("loosePieces", item.loosePieces)
                put("pricePerBox", item.pricePerBox)
                put("lineTotal", item.lineTotal)
            }
            itemArr.put(io)
        }
        root.put("orderItems", itemArr)

        // 7. Staff Members
        val staffList = database.staffDao().getAllStaff().let {
            database.staffDao().getStaffById(1) // ping
        }
        // Staff export: We can get list of staff using staffDao
        // Let's get all staff safely
        val allStaffList = database.openHelper.readableDatabase.query("SELECT * FROM staff_members").use { cursor ->
            val list = mutableListOf<StaffMember>()
            val idIdx = cursor.getColumnIndex("id")
            val nameIdx = cursor.getColumnIndex("name")
            val phoneIdx = cursor.getColumnIndex("phone")
            val roleIdx = cursor.getColumnIndex("role")
            val salaryIdx = cursor.getColumnIndex("monthlySalary")
            val joinIdx = cursor.getColumnIndex("joiningDate")
            val activeIdx = cursor.getColumnIndex("isActive")
            val notesIdx = cursor.getColumnIndex("notes")
            val openBalIdx = cursor.getColumnIndex("openingBalance")
            val calcTypeIdx = cursor.getColumnIndex("salaryCalculationType")
            val wageIdx = cursor.getColumnIndex("dailyWageRate")
            val weeklyOffIdx = cursor.getColumnIndex("hasWeeklyOff")
            val offDayIdx = cursor.getColumnIndex("weeklyOffDay")
            val workDaysIdx = cursor.getColumnIndex("workingDaysPerMonth")
            while (cursor.moveToNext()) {
                list.add(
                    StaffMember(
                        id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L,
                        name = if (nameIdx >= 0) cursor.getString(nameIdx) else "",
                        phone = if (phoneIdx >= 0) cursor.getString(phoneIdx) else "",
                        role = if (roleIdx >= 0) cursor.getString(roleIdx) else "Worker",
                        monthlySalary = if (salaryIdx >= 0) cursor.getDouble(salaryIdx) else 0.0,
                        joiningDate = if (joinIdx >= 0) cursor.getLong(joinIdx) else System.currentTimeMillis(),
                        isActive = if (activeIdx >= 0) cursor.getInt(activeIdx) == 1 else true,
                        notes = if (notesIdx >= 0) cursor.getString(notesIdx) else "",
                        openingBalance = if (openBalIdx >= 0) cursor.getDouble(openBalIdx) else 0.0,
                        salaryCalculationType = if (calcTypeIdx >= 0 && !cursor.isNull(calcTypeIdx)) cursor.getString(calcTypeIdx) else "FIXED_30_DAYS",
                        dailyWageRate = if (wageIdx >= 0) cursor.getDouble(wageIdx) else 0.0,
                        hasWeeklyOff = if (weeklyOffIdx >= 0) cursor.getInt(weeklyOffIdx) == 1 else true,
                        weeklyOffDay = if (offDayIdx >= 0 && !cursor.isNull(offDayIdx)) cursor.getString(offDayIdx) else "SUNDAY",
                        workingDaysPerMonth = if (workDaysIdx >= 0) cursor.getInt(workDaysIdx) else 26
                    )
                )
            }
            list
        }
        val staffArr = JSONArray()
        for (st in allStaffList) {
            val so = JSONObject().apply {
                put("id", st.id)
                put("name", st.name)
                put("phone", st.phone)
                put("role", st.role)
                put("monthlySalary", st.monthlySalary)
                put("joiningDate", st.joiningDate)
                put("isActive", st.isActive)
                put("notes", st.notes)
                put("openingBalance", st.openingBalance)
                put("salaryCalculationType", st.salaryCalculationType)
                put("dailyWageRate", st.dailyWageRate)
                put("hasWeeklyOff", st.hasWeeklyOff)
                put("weeklyOffDay", st.weeklyOffDay)
                put("workingDaysPerMonth", st.workingDaysPerMonth)
            }
            staffArr.put(so)
        }
        root.put("staff", staffArr)

        // 8. Staff Attendance
        val attendances = database.staffDao().getAllAttendanceSync()
        val attArr = JSONArray()
        for (a in attendances) {
            val ao = JSONObject().apply {
                put("id", a.id)
                put("staffId", a.staffId)
                put("dateMillis", a.dateMillis)
                put("dateString", a.dateString)
                put("status", a.status)
                put("notes", a.notes)
            }
            attArr.put(ao)
        }
        root.put("staffAttendance", attArr)

        // 9. Staff Payments
        val payments = database.staffDao().getAllPaymentsSync()
        val payArr = JSONArray()
        for (p in payments) {
            val po = JSONObject().apply {
                put("id", p.id)
                put("staffId", p.staffId)
                put("dateMillis", p.dateMillis)
                put("amount", p.amount)
                put("type", p.type)
                put("forMonthYear", p.forMonthYear)
                put("paymentMode", p.paymentMode)
                put("notes", p.notes)
            }
            payArr.put(po)
        }
        root.put("staffPayments", payArr)

        // 10. Stock Movements
        val movements = database.stockMovementDao().getAllMovementsSync()
        val movArr = JSONArray()
        for (m in movements) {
            val mo = JSONObject().apply {
                put("id", m.id)
                put("title", m.title)
                put("description", m.description)
                put("changeDisplay", m.changeDisplay)
                put("isPositive", m.isPositive)
                put("timestamp", m.timestamp)
            }
            movArr.put(mo)
        }
        root.put("stockMovements", movArr)

        // 11. Recycle Bin
        val recycleItems = database.recycleBinDao().getAllRecycleItemsSync()
        val recArr = JSONArray()
        for (r in recycleItems) {
            val ro = JSONObject().apply {
                put("id", r.id)
                put("userPhone", r.userPhone)
                put("itemType", r.itemType)
                put("originalId", r.originalId)
                put("title", r.title)
                put("subtitle", r.subtitle)
                put("payloadJson", r.payloadJson)
                put("deletedAt", r.deletedAt)
            }
            recArr.put(ro)
        }
        root.put("recycleBin", recArr)

        // 12. Staff Monthly Settlements
        val settlements = database.staffDao().getAllSettlementsSync()
        val setArr = JSONArray()
        for (s in settlements) {
            val so = JSONObject().apply {
                put("id", s.id)
                put("staffId", s.staffId)
                put("monthKey", s.monthKey)
                put("isFinalized", s.isFinalized)
                put("settledAt", s.settledAt)
                put("notes", s.notes)
            }
            setArr.put(so)
        }
        root.put("staffMonthlySettlements", setArr)

        if (imageCollector.count > 0) {
            root.put("embeddedImages", imageCollector.embeddedImagesJson)
            root.put("embeddedImageCount", imageCollector.count)
        }

        return if (imageCollector.count > 0) root.toString() else root.toString(2)
    }

    suspend fun createCumulativeBackupFile(
        context: Context,
        database: AppDatabase,
        customFromDateString: String? = null
    ): File {
        val root = JSONObject()
        root.put("version", 4)
        root.put("timestamp", System.currentTimeMillis())

        val imageCollector = EmbeddedImageCollector(
            context = context,
            includeImages = true,
            maxDim = 800,
            jpegQuality = 78
        )

        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        val fromMillis: Long = if (!customFromDateString.isNullOrBlank()) {
            try {
                sdfDate.parse(customFromDateString.trim())?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        } else {
            0L
        }

        // Settings
        val settings = database.shopSettingsDao().getSettingsSync() ?: ShopSettings()
        val settingsObj = JSONObject().apply {
            put("shopName", settings.shopName)
            put("subtitle", settings.subtitle)
            put("ownerName", settings.ownerName)
            put("ownerPhone", settings.ownerPhone)
            put("altPhone", settings.altPhone)
            put("address", settings.address)
            put("logoUri", settings.logoUri ?: "")
            val logoKey = imageCollector.registerImage(settings.logoUri)
            if (logoKey.isNotEmpty()) put("logoImageKey", logoKey)
            put("gstNumber", settings.gstNumber)
            put("panNumber", settings.panNumber)
            put("bankName", settings.bankName)
            put("bankAccountNumber", settings.bankAccountNumber)
            put("bankIfsc", settings.bankIfsc)
            put("bankBranch", settings.bankBranch)
            put("invoicePrefix", settings.invoicePrefix)
            put("defaultTaxPercent", settings.defaultTaxPercent)
            put("showQrOnInvoice", settings.showQrOnInvoice)
            put("showBankOnInvoice", settings.showBankOnInvoice)
            put("invoiceFooterNotes", settings.invoiceFooterNotes)
            put("termsNotes", settings.termsNotes)
            put("appTheme", settings.appTheme)
            put("upiId", settings.upiId)
            put("telegramBotToken", settings.telegramBotToken)
            put("telegramChatId", settings.telegramChatId)
            put("telegramEnabled", settings.telegramEnabled)
            put("telegramFormat", settings.telegramFormat)
            put("telegramCustomTitle", settings.telegramCustomTitle)
            put("autoAttendanceEnabled", settings.autoAttendanceEnabled)
            put("autoBackupEnabled", settings.autoBackupEnabled)
            put("backupTargetEmail", settings.backupTargetEmail)
            put("backupScheduledTime", settings.backupScheduledTime)
            put("backupFromDateString", settings.backupFromDateString)
            put("lastAutoBackupDate", settings.lastAutoBackupDate)
            put("pdfSizeDisplayConfigJson", settings.pdfSizeDisplayConfigJson)
        }
        root.put("settings", settingsObj)

        // Size Pricing
        val pricings = database.sizePricingDao().getAllPricingsSync()
        val pricingArr = JSONArray()
        for (p in pricings) {
            pricingArr.put(JSONObject().apply {
                put("sizeName", p.sizeName)
                put("defaultPricePerBox", p.defaultPricePerBox)
                put("defaultPiecesPerBox", p.defaultPiecesPerBox)
            })
        }
        root.put("sizePricing", pricingArr)

        // Products
        val products = database.tileProductDao().getAllProductsSync()
        val productArr = JSONArray()
        for (prod in products) {
            productArr.put(JSONObject().apply {
                put("id", prod.id)
                put("code", prod.code)
                put("name", prod.name)
                put("size", prod.size)
                put("photoUri", prod.photoUri ?: "")
                val photoKey = imageCollector.registerImage(prod.photoUri)
                if (photoKey.isNotEmpty()) put("photoImageKey", photoKey)
                put("description", prod.description)
                put("piecesPerBox", prod.piecesPerBox)
                put("stockPieces", prod.stockPieces)
                put("defaultSellingPrice", prod.defaultSellingPrice)
                put("purchaseCost", prod.purchaseCost)
                put("createdAt", prod.createdAt)
            })
        }
        root.put("products", productArr)

        // Customers
        val customers = database.customerDao().getAllCustomersSync()
        val custArr = JSONArray()
        for (c in customers) {
            custArr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phone", c.phone)
                put("address", c.address)
                put("notes", c.notes)
                put("outstandingBalance", c.outstandingBalance)
                put("createdAt", c.createdAt)
            })
        }
        root.put("customers", custArr)

        // Orders (from fromMillis to today)
        val orders = database.orderDao().getAllOrdersSync().filter { it.createdAt >= fromMillis }
        val orderIds = orders.map { it.id }.toSet()
        val orderArr = JSONArray()
        for (o in orders) {
            orderArr.put(JSONObject().apply {
                put("id", o.id)
                put("invoiceNumber", o.invoiceNumber)
                put("customerId", o.customerId ?: -1L)
                put("customerName", o.customerName)
                put("customerPhone", o.customerPhone)
                put("customerAddress", o.customerAddress)
                put("status", o.status)
                put("showPriceOnPdf", o.showPriceOnPdf)
                put("subtotal", o.subtotal)
                put("discount", o.discount)
                put("grandTotal", o.grandTotal)
                put("paidAmount", o.paidAmount)
                put("balanceAmount", o.balanceAmount)
                put("paymentHistoryJson", o.paymentHistoryJson)
                put("notes", o.notes)
                put("createdAt", o.createdAt)
                put("confirmedAt", o.confirmedAt ?: -1L)
            })
        }
        root.put("orders", orderArr)

        // Order Items for included orders
        val allItems = database.orderItemDao().getAllItemsSync()
        val itemArr = JSONArray()
        for (item in allItems) {
            if (orderIds.isEmpty() || orderIds.contains(item.orderId)) {
                itemArr.put(JSONObject().apply {
                    put("id", item.id)
                    put("orderId", item.orderId)
                    put("productId", item.productId)
                    put("tileCode", item.tileCode)
                    put("tileName", item.tileName)
                    put("tileSize", item.tileSize)
                    put("piecesPerBox", item.piecesPerBox)
                    put("tilePhotoUri", item.tilePhotoUri ?: "")
                    val itemPhotoKey = imageCollector.registerImage(item.tilePhotoUri)
                    if (itemPhotoKey.isNotEmpty()) put("tilePhotoImageKey", itemPhotoKey)
                    put("fullBoxes", item.fullBoxes)
                    put("loosePieces", item.loosePieces)
                    put("pricePerBox", item.pricePerBox)
                    put("lineTotal", item.lineTotal)
                })
            }
        }
        root.put("orderItems", itemArr)

        // Staff
        val allStaffList = database.staffDao().getAllStaffSync()
        val staffArr = JSONArray()
        for (st in allStaffList) {
            staffArr.put(JSONObject().apply {
                put("id", st.id)
                put("name", st.name)
                put("phone", st.phone)
                put("role", st.role)
                put("monthlySalary", st.monthlySalary)
                put("joiningDate", st.joiningDate)
                put("isActive", st.isActive)
                put("notes", st.notes)
                put("openingBalance", st.openingBalance)
                put("salaryCalculationType", st.salaryCalculationType)
                put("dailyWageRate", st.dailyWageRate)
                put("hasWeeklyOff", st.hasWeeklyOff)
                put("weeklyOffDay", st.weeklyOffDay)
                put("workingDaysPerMonth", st.workingDaysPerMonth)
            })
        }
        root.put("staff", staffArr)

        // Attendance (from fromMillis)
        val attendances = database.staffDao().getAllAttendanceSync().filter { it.dateMillis >= fromMillis }
        val attArr = JSONArray()
        for (a in attendances) {
            attArr.put(JSONObject().apply {
                put("id", a.id)
                put("staffId", a.staffId)
                put("dateMillis", a.dateMillis)
                put("dateString", a.dateString)
                put("status", a.status)
                put("notes", a.notes)
            })
        }
        root.put("staffAttendance", attArr)

        // Staff Payments (from fromMillis)
        val payments = database.staffDao().getAllPaymentsSync().filter { it.dateMillis >= fromMillis }
        val payArr = JSONArray()
        for (p in payments) {
            payArr.put(JSONObject().apply {
                put("id", p.id)
                put("staffId", p.staffId)
                put("dateMillis", p.dateMillis)
                put("amount", p.amount)
                put("type", p.type)
                put("forMonthYear", p.forMonthYear)
                put("paymentMode", p.paymentMode)
                put("notes", p.notes)
            })
        }
        root.put("staffPayments", payArr)

        // Settlements
        val settlements = database.staffDao().getAllSettlementsSync()
        val setArr = JSONArray()
        for (s in settlements) {
            setArr.put(JSONObject().apply {
                put("id", s.id)
                put("staffId", s.staffId)
                put("monthKey", s.monthKey)
                put("isFinalized", s.isFinalized)
                put("settledAt", s.settledAt)
                put("notes", s.notes)
            })
        }
        root.put("staffMonthlySettlements", setArr)

        // Stock Movements
        val movements = database.stockMovementDao().getAllMovementsSync().filter { it.timestamp >= fromMillis }
        val movArr = JSONArray()
        for (m in movements) {
            movArr.put(JSONObject().apply {
                put("id", m.id)
                put("title", m.title)
                put("description", m.description)
                put("changeDisplay", m.changeDisplay)
                put("isPositive", m.isPositive)
                put("timestamp", m.timestamp)
            })
        }
        root.put("stockMovements", movArr)

        if (imageCollector.count > 0) {
            root.put("embeddedImages", imageCollector.embeddedImagesJson)
            root.put("embeddedImageCount", imageCollector.count)
        }

        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        // Name format starting with date: e.g. "01_11_2026_backup_data.json"
        val fileNameDate = SimpleDateFormat("dd_MM_yyyy", Locale.ENGLISH).format(Date())
        val backupFile = File(backupDir, "${fileNameDate}_backup_data.json")

        val outputText = if (imageCollector.count > 0) root.toString() else root.toString(2)
        FileOutputStream(backupFile).use { fos ->
            fos.write(outputText.toByteArray(Charsets.UTF_8))
        }

        return backupFile
    }

    fun createShareBackupIntent(
        context: Context,
        backupFile: File,
        emailRecipient: String? = null
    ): android.content.Intent {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )
        return android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Mourya Tiles Backup: ${backupFile.name}")
            putExtra(
                android.content.Intent.EXTRA_TEXT,
                "Mourya Tiles & Sanitary - Auto Backup File Attached (${backupFile.name}). This backup can be saved to Google Drive or restored in the app."
            )
            if (!emailRecipient.isNullOrBlank()) {
                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(emailRecipient.trim()))
            }
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun createBackup(context: Context, database: AppDatabase): File {
        val jsonString = exportDatabaseToJson(
            database = database,
            context = context,
            includeImages = true,
            maxImageDim = 800,
            jpegQuality = 78
        )

        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val backupFile = File(backupDir, "TilesAndMarbles_Backup_${sdf.format(Date())}.json")

        FileOutputStream(backupFile).use { fos ->
            fos.write(jsonString.toByteArray(Charsets.UTF_8))
        }

        return backupFile
    }

    suspend fun restoreFromJsonString(
        database: AppDatabase,
        jsonContent: String,
        context: Context? = null
    ): Result<String> {
        return try {
            val root = JSONObject(jsonContent)
            val restoredImagesMap = extractEmbeddedImages(context, root)
            val restoredPhotoByCode = mutableMapOf<String, String>()
            val restoredPhotoByOldUri = mutableMapOf<String, String>()
            var restoredTilePhotoCount = 0

            database.withTransaction {
                // Restore settings
                if (root.has("settings")) {
                    val s = root.getJSONObject("settings")
                    val existing = database.shopSettingsDao().getSettingsSync() ?: ShopSettings()
                    val logoKey = s.optString("logoImageKey", "")
                    val rawLogoUri = s.optString("logoUri").ifEmpty { null }
                    val resolvedLogoUri = when {
                        logoKey.isNotEmpty() && restoredImagesMap.containsKey(logoKey) -> restoredImagesMap[logoKey]
                        rawLogoUri != null && isLocalImageAccessible(rawLogoUri) -> rawLogoUri
                        existing.logoUri != null && isLocalImageAccessible(existing.logoUri) -> existing.logoUri
                        else -> rawLogoUri ?: existing.logoUri
                    }
                    val settings = existing.copy(
                        shopName = s.optString("shopName", existing.shopName),
                        subtitle = s.optString("subtitle", existing.subtitle),
                        ownerName = s.optString("ownerName", existing.ownerName),
                        ownerPhone = s.optString("ownerPhone", existing.ownerPhone),
                        altPhone = s.optString("altPhone", existing.altPhone),
                        address = s.optString("address", existing.address),
                        logoUri = resolvedLogoUri,
                        gstNumber = s.optString("gstNumber", existing.gstNumber),
                        panNumber = s.optString("panNumber", existing.panNumber),
                        bankName = s.optString("bankName", existing.bankName),
                        bankAccountNumber = s.optString("bankAccountNumber", existing.bankAccountNumber),
                        bankIfsc = s.optString("bankIfsc", existing.bankIfsc),
                        bankBranch = s.optString("bankBranch", existing.bankBranch),
                        invoicePrefix = s.optString("invoicePrefix", existing.invoicePrefix),
                        defaultTaxPercent = s.optDouble("defaultTaxPercent", existing.defaultTaxPercent),
                        showQrOnInvoice = s.optBoolean("showQrOnInvoice", existing.showQrOnInvoice),
                        showBankOnInvoice = s.optBoolean("showBankOnInvoice", existing.showBankOnInvoice),
                        invoiceFooterNotes = s.optString("invoiceFooterNotes", existing.invoiceFooterNotes),
                        termsNotes = s.optString("termsNotes", existing.termsNotes),
                        appTheme = s.optString("appTheme", existing.appTheme),
                        upiId = s.optString("upiId", existing.upiId),
                        telegramBotToken = s.optString("telegramBotToken", existing.telegramBotToken),
                        telegramChatId = s.optString("telegramChatId", existing.telegramChatId),
                        telegramEnabled = s.optBoolean("telegramEnabled", existing.telegramEnabled),
                        telegramFormat = s.optString("telegramFormat", existing.telegramFormat),
                        telegramCustomTitle = s.optString("telegramCustomTitle", existing.telegramCustomTitle),
                        autoAttendanceEnabled = s.optBoolean("autoAttendanceEnabled", existing.autoAttendanceEnabled),
                        autoBackupEnabled = s.optBoolean("autoBackupEnabled", existing.autoBackupEnabled),
                        backupTargetEmail = s.optString("backupTargetEmail", existing.backupTargetEmail),
                        backupScheduledTime = s.optString("backupScheduledTime", existing.backupScheduledTime),
                        backupFromDateString = s.optString("backupFromDateString", existing.backupFromDateString),
                        lastAutoBackupDate = s.optString("lastAutoBackupDate", existing.lastAutoBackupDate),
                        pdfSizeDisplayConfigJson = s.optString("pdfSizeDisplayConfigJson", existing.pdfSizeDisplayConfigJson)
                    )
                    database.shopSettingsDao().saveSettings(settings)
                }

                // Restore size pricing & packing
                if (root.has("sizePricing")) {
                    val arr = root.getJSONArray("sizePricing")
                    for (i in 0 until arr.length()) {
                        val po = arr.getJSONObject(i)
                        val sizeName = po.getString("sizeName")
                        val price = po.getDouble("defaultPricePerBox")
                        val pcs = po.optInt("defaultPiecesPerBox", 0)
                        val existing = database.sizePricingDao().getPricingForSize(sizeName)
                        if (existing != null) {
                            database.sizePricingDao().update(existing.copy(defaultPricePerBox = price, defaultPiecesPerBox = pcs))
                        } else {
                            database.sizePricingDao().insert(SizePricing(sizeName = sizeName, defaultPricePerBox = price, defaultPiecesPerBox = pcs))
                        }
                    }
                }

                // Restore products
                if (root.has("products")) {
                    val arr = root.getJSONArray("products")
                    for (i in 0 until arr.length()) {
                        val po = arr.getJSONObject(i)
                        val code = po.getString("code")
                        val rawPhotoUri = po.optString("photoUri").ifEmpty { null }
                        val photoKey = po.optString("photoImageKey", "")
                        val inlineB64 = po.optString("photoBase64", "")
                        val existing = database.tileProductDao().getProductByCode(code)

                        val restoredPath: String? = when {
                            photoKey.isNotEmpty() && restoredImagesMap.containsKey(photoKey) -> {
                                restoredTilePhotoCount++
                                restoredImagesMap[photoKey]
                            }
                            inlineB64.isNotEmpty() && context != null -> {
                                val p = saveBase64ImageToLocalStorage(context, inlineB64)
                                if (!p.isNullOrBlank()) restoredTilePhotoCount++
                                p
                            }
                            rawPhotoUri != null && isLocalImageAccessible(rawPhotoUri) -> rawPhotoUri
                            existing?.photoUri != null && isLocalImageAccessible(existing.photoUri) -> existing.photoUri
                            else -> rawPhotoUri
                        }

                        if (!restoredPath.isNullOrBlank()) {
                            restoredPhotoByCode[code] = restoredPath
                            if (!rawPhotoUri.isNullOrBlank()) {
                                restoredPhotoByOldUri[rawPhotoUri] = restoredPath
                            }
                        }

                        val prod = TileProduct(
                            id = po.optLong("id", 0),
                            code = code,
                            name = po.getString("name"),
                            size = po.getString("size"),
                            photoUri = restoredPath,
                            description = po.optString("description", ""),
                            piecesPerBox = po.optInt("piecesPerBox", 5),
                            stockPieces = po.optInt("stockPieces", 0),
                            defaultSellingPrice = po.optDouble("defaultSellingPrice", 0.0),
                            purchaseCost = po.optDouble("purchaseCost", 0.0),
                            createdAt = po.optLong("createdAt", System.currentTimeMillis())
                        )
                        if (existing != null) {
                            database.tileProductDao().update(prod.copy(id = existing.id))
                        } else {
                            database.tileProductDao().insert(prod)
                        }
                    }
                }

                // Restore customers
                if (root.has("customers")) {
                    val arr = root.getJSONArray("customers")
                    for (i in 0 until arr.length()) {
                        val co = arr.getJSONObject(i)
                        val cust = Customer(
                            id = co.optLong("id", 0),
                            name = co.getString("name"),
                            phone = co.optString("phone", ""),
                            address = co.optString("address", ""),
                            notes = co.optString("notes", ""),
                            outstandingBalance = co.optDouble("outstandingBalance", 0.0),
                            createdAt = co.optLong("createdAt", System.currentTimeMillis())
                        )
                        database.customerDao().insert(cust)
                    }
                }

                // Restore orders & items
                if (root.has("orders")) {
                    val arr = root.getJSONArray("orders")
                    for (i in 0 until arr.length()) {
                        val oo = arr.getJSONObject(i)
                        val custId = oo.optLong("customerId", -1L)
                        val confAt = oo.optLong("confirmedAt", -1L)
                        val order = OrderEntity(
                            id = oo.optLong("id", 0),
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
                            paidAmount = oo.optDouble("paidAmount", 0.0),
                            balanceAmount = oo.optDouble("balanceAmount", oo.optDouble("grandTotal", 0.0)),
                            paymentHistoryJson = oo.optString("paymentHistoryJson", ""),
                            notes = oo.optString("notes", ""),
                            createdAt = oo.optLong("createdAt", System.currentTimeMillis()),
                            confirmedAt = if (confAt > 0) confAt else null
                        )
                        database.orderDao().insert(order)
                    }
                }

                if (root.has("orderItems")) {
                    val arr = root.getJSONArray("orderItems")
                    val items = mutableListOf<OrderItemEntity>()
                    for (i in 0 until arr.length()) {
                        val io = arr.getJSONObject(i)
                        val itemCode = io.getString("tileCode")
                        val rawItemPhoto = io.optString("tilePhotoUri").ifEmpty { null }
                        val itemPhotoKey = io.optString("tilePhotoImageKey", "")
                        val resolvedItemPhoto = when {
                            itemPhotoKey.isNotEmpty() && restoredImagesMap.containsKey(itemPhotoKey) -> restoredImagesMap[itemPhotoKey]
                            rawItemPhoto != null && restoredPhotoByOldUri.containsKey(rawItemPhoto) -> restoredPhotoByOldUri[rawItemPhoto]
                            rawItemPhoto != null && isLocalImageAccessible(rawItemPhoto) -> rawItemPhoto
                            restoredPhotoByCode.containsKey(itemCode) -> restoredPhotoByCode[itemCode]
                            else -> rawItemPhoto
                        }
                        items.add(
                            OrderItemEntity(
                                id = io.optLong("id", 0),
                                orderId = io.getLong("orderId"),
                                productId = io.getLong("productId"),
                                tileCode = itemCode,
                                tileName = io.getString("tileName"),
                                tileSize = io.getString("tileSize"),
                                piecesPerBox = io.getInt("piecesPerBox"),
                                tilePhotoUri = resolvedItemPhoto,
                                fullBoxes = io.getInt("fullBoxes"),
                                loosePieces = io.optInt("loosePieces", 0),
                                pricePerBox = io.getDouble("pricePerBox"),
                                lineTotal = io.getDouble("lineTotal")
                            )
                        )
                    }
                    if (items.isNotEmpty()) {
                        database.orderItemDao().insertAll(items)
                    }
                }

                // Restore staff
                if (root.has("staff")) {
                    val arr = root.getJSONArray("staff")
                    for (i in 0 until arr.length()) {
                        val so = arr.getJSONObject(i)
                        val staff = StaffMember(
                            id = so.optLong("id", 0),
                            name = so.getString("name"),
                            phone = so.optString("phone", ""),
                            role = so.optString("role", "Worker"),
                            monthlySalary = so.optDouble("monthlySalary", 0.0),
                            joiningDate = so.optLong("joiningDate", System.currentTimeMillis()),
                            isActive = so.optBoolean("isActive", true),
                            notes = so.optString("notes", ""),
                            openingBalance = so.optDouble("openingBalance", 0.0),
                            salaryCalculationType = so.optString("salaryCalculationType", "FIXED_30_DAYS"),
                            dailyWageRate = so.optDouble("dailyWageRate", 0.0),
                            hasWeeklyOff = so.optBoolean("hasWeeklyOff", true),
                            weeklyOffDay = so.optString("weeklyOffDay", "SUNDAY"),
                            workingDaysPerMonth = so.optInt("workingDaysPerMonth", 26)
                        )
                        database.staffDao().insertStaff(staff)
                    }
                }

                // Restore staff attendance
                if (root.has("staffAttendance")) {
                    val arr = root.getJSONArray("staffAttendance")
                    val attList = mutableListOf<StaffAttendance>()
                    for (i in 0 until arr.length()) {
                        val ao = arr.getJSONObject(i)
                        attList.add(
                            StaffAttendance(
                                id = ao.optLong("id", 0),
                                staffId = ao.getLong("staffId"),
                                dateMillis = ao.getLong("dateMillis"),
                                dateString = ao.getString("dateString"),
                                status = ao.optString("status", "PRESENT"),
                                notes = ao.optString("notes", "")
                            )
                        )
                    }
                    if (attList.isNotEmpty()) {
                        database.staffDao().insertAllAttendance(attList)
                    }
                }

                // Restore staff payments
                if (root.has("staffPayments")) {
                    val arr = root.getJSONArray("staffPayments")
                    val payList = mutableListOf<StaffPayment>()
                    for (i in 0 until arr.length()) {
                        val po = arr.getJSONObject(i)
                        payList.add(
                            StaffPayment(
                                id = po.optLong("id", 0),
                                staffId = po.getLong("staffId"),
                                dateMillis = po.optLong("dateMillis", System.currentTimeMillis()),
                                amount = po.getDouble("amount"),
                                type = po.optString("type", "ADVANCE"),
                                forMonthYear = po.optString("forMonthYear", ""),
                                paymentMode = po.optString("paymentMode", "CASH"),
                                notes = po.optString("notes", "")
                            )
                        )
                    }
                    if (payList.isNotEmpty()) {
                        database.staffDao().insertAllPayments(payList)
                    }
                }

                // Restore stock movements
                if (root.has("stockMovements")) {
                    val arr = root.getJSONArray("stockMovements")
                    val movList = mutableListOf<com.example.data.model.StockMovement>()
                    for (i in 0 until arr.length()) {
                        val mo = arr.getJSONObject(i)
                        movList.add(
                            com.example.data.model.StockMovement(
                                id = mo.optLong("id", 0),
                                title = mo.getString("title"),
                                description = mo.getString("description"),
                                changeDisplay = mo.getString("changeDisplay"),
                                isPositive = mo.optBoolean("isPositive", true),
                                timestamp = mo.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    if (movList.isNotEmpty()) {
                        database.stockMovementDao().insertAll(movList)
                    }
                }

                // Restore recycle bin
                if (root.has("recycleBin")) {
                    val arr = root.getJSONArray("recycleBin")
                    for (i in 0 until arr.length()) {
                        val ro = arr.getJSONObject(i)
                        database.recycleBinDao().insert(
                            com.example.data.model.RecycleBinItem(
                                id = ro.optLong("id", 0),
                                userPhone = ro.optString("userPhone", ""),
                                itemType = ro.getString("itemType"),
                                originalId = ro.getLong("originalId"),
                                title = ro.getString("title"),
                                subtitle = ro.getString("subtitle"),
                                payloadJson = ro.getString("payloadJson"),
                                deletedAt = ro.optLong("deletedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                // Restore staff monthly settlements
                if (root.has("staffMonthlySettlements")) {
                    val arr = root.getJSONArray("staffMonthlySettlements")
                    val setList = mutableListOf<com.example.data.model.StaffMonthlySettlement>()
                    for (i in 0 until arr.length()) {
                        val so = arr.getJSONObject(i)
                        setList.add(
                            com.example.data.model.StaffMonthlySettlement(
                                id = so.optLong("id", 0),
                                staffId = so.getLong("staffId"),
                                monthKey = so.getString("monthKey"),
                                isFinalized = so.optBoolean("isFinalized", false),
                                settledAt = so.optLong("settledAt", System.currentTimeMillis()),
                                notes = so.optString("notes", "")
                            )
                        )
                    }
                    if (setList.isNotEmpty()) {
                        database.staffDao().insertAllSettlements(setList)
                    }
                }
            }

            val successMsg = if (restoredImagesMap.isNotEmpty()) {
                "डेटा और ${restoredImagesMap.size} टाइल/दुकान फोटो सफलतापूर्वक रीस्टोर हो गईं!"
            } else {
                "डेटा सफलतापूर्वक रीस्टोर हो गया!"
            }
            Result.success(successMsg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(context: Context, database: AppDatabase, backupFile: File): Result<String> {
        return try {
            val jsonContent = FileInputStream(backupFile).use { it.bufferedReader().readText() }
            restoreFromJsonString(database, jsonContent, context)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
