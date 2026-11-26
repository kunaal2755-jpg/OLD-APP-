package com.example.util

import android.util.Log
import com.example.data.model.AdminGlobalConfig
import com.example.data.model.OrderWithItems
import com.example.data.model.ShopSettings
import com.example.data.model.TileProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TelegramActiveConfig(
    val enabled: Boolean = false,
    val botToken: String = "",
    val chatId: String = "",
    val adminCopyChatId: String = "",
    val format: String = "HINDI_DETAILED",
    val customTitle: String = "TILES AND MARBLES",
    val alertNewSale: Boolean = true,
    val alertPayment: Boolean = true,
    val alertLowStock: Boolean = true,
    val alertAttendance: Boolean = true,
    val alertSalaryAdvance: Boolean = true,
    val alertNewProduct: Boolean = true,
    val alertInvoiceDeleted: Boolean = true,
    val alertUserAction: Boolean = true
) {
    val isReady: Boolean
        get() = enabled && botToken.isNotBlank() && chatId.isNotBlank()

    companion object {
        fun resolve(
            adminConfig: AdminGlobalConfig?,
            shopSettings: ShopSettings?,
            cloudConfig: AdminGlobalConfig? = null
        ): TelegramActiveConfig {
            val effectiveAdmin = when {
                adminConfig != null && adminConfig.telegramBotToken.isNotBlank() -> adminConfig
                cloudConfig != null && cloudConfig.telegramBotToken.isNotBlank() -> cloudConfig
                else -> null
            }

            val masterBotToken = effectiveAdmin?.telegramBotToken?.trim().orEmpty()
            val masterAdminChatId = effectiveAdmin?.telegramChatId?.trim().orEmpty()
            val userChatId = shopSettings?.telegramChatId?.trim().orEmpty()

            val effectiveBotToken = if (masterBotToken.isNotBlank()) masterBotToken else (shopSettings?.telegramBotToken?.trim().orEmpty())
            val effectiveChatId = if (userChatId.isNotBlank()) userChatId else masterAdminChatId
            val copyChatId = if (userChatId.isNotBlank() && masterAdminChatId.isNotBlank() && userChatId != masterAdminChatId) masterAdminChatId else ""

            val isEnabled = (effectiveAdmin?.telegramEnabled == true) || (shopSettings?.telegramEnabled == true) || (effectiveBotToken.isNotBlank() && effectiveChatId.isNotBlank())

            return TelegramActiveConfig(
                enabled = isEnabled,
                botToken = effectiveBotToken,
                chatId = effectiveChatId,
                adminCopyChatId = copyChatId,
                format = effectiveAdmin?.telegramFormat ?: shopSettings?.telegramFormat ?: "HINDI_DETAILED",
                customTitle = shopSettings?.shopName?.ifBlank { effectiveAdmin?.brandAppName ?: "TILES AND MARBLES" } ?: "TILES AND MARBLES",
                alertNewSale = effectiveAdmin?.telegramAlertNewSale ?: shopSettings?.notifyNewSale ?: true,
                alertPayment = effectiveAdmin?.telegramAlertPayment ?: shopSettings?.notifyPaymentReceived ?: true,
                alertLowStock = effectiveAdmin?.telegramAlertLowStock ?: shopSettings?.notifyLowStock ?: true,
                alertAttendance = effectiveAdmin?.telegramAlertAttendance ?: shopSettings?.notifyStaffAttendance ?: true,
                alertSalaryAdvance = effectiveAdmin?.telegramAlertSalaryAdvance ?: true,
                alertNewProduct = effectiveAdmin?.telegramAlertNewProduct ?: true,
                alertInvoiceDeleted = effectiveAdmin?.telegramAlertInvoiceDeleted ?: true,
                alertUserAction = effectiveAdmin?.telegramAlertUserAction ?: true
            )
        }
    }
}

object TelegramNotifier {

    private const val TAG = "TelegramNotifier"

    /**
     * Sends a raw message to Telegram Bot API.
     * Returns Pair<Boolean, String> -> (isSuccess, responseOrErrorMessage)
     */
    suspend fun sendMessage(
        botToken: String,
        chatId: String,
        text: String,
        parseMode: String = "HTML"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanToken = botToken.trim()
        val cleanChatId = chatId.trim()

        if (cleanToken.isBlank() || cleanChatId.isBlank()) {
            return@withContext Pair(false, "Bot Token और Chat ID आवश्यक हैं")
        }

        try {
            val urlString = "https://api.telegram.org/bot$cleanToken/sendMessage"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true

            val jsonBody = JSONObject().apply {
                put("chat_id", cleanChatId)
                put("text", text)
                put("parse_mode", parseMode)
                put("disable_web_page_preview", true)
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val inputStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = BufferedReader(InputStreamReader(inputStream ?: conn.inputStream)).use { it.readText() }

            if (responseCode in 200..299) {
                Pair(true, "संदेश सफलतापूर्वक भेजा गया! (HTTP $responseCode)")
            } else {
                val parsed = try {
                    val errorJson = JSONObject(responseText)
                    errorJson.optString("description", responseText)
                } catch (e: Exception) {
                    responseText
                }
                Log.e(TAG, "Telegram error: $responseCode - $parsed")
                Pair(false, "Telegram एरर ($responseCode): $parsed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send telegram message", e)
            Pair(false, "कनेक्शन विफलता: ${e.localizedMessage ?: "इंटरनेट कनेक्शन जांचें"}")
        }
    }

    suspend fun sendActiveMessage(
        config: TelegramActiveConfig,
        text: String,
        parseMode: String = "HTML"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!config.isReady) return@withContext Pair(false, "Bot Token या Chat ID सक्रिय नहीं है")
        val primaryRes = sendMessage(config.botToken, config.chatId, text, parseMode)
        if (config.adminCopyChatId.isNotBlank() && config.adminCopyChatId != config.chatId) {
            try {
                sendMessage(config.botToken, config.adminCopyChatId, text, parseMode)
            } catch (_: Exception) {}
        }
        primaryRes
    }

    /**
     * Send test connection message
     */
    suspend fun sendTestMessage(
        botToken: String,
        chatId: String,
        format: String = "HINDI_DETAILED",
        customTitle: String = "TILES AND MARBLES"
    ): Pair<Boolean, String> {
        val shopHeader = customTitle.ifBlank { "TILES AND MARBLES" }
        val timeStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val message = when (format) {
            "COMPACT_HINDI" -> """
                <b>🏬 $shopHeader</b>
                🔔 <b>टेलीग्राम बॉट टेस्ट सफल!</b>
                ✅ आपका बॉट सफलतापूर्वक कनेक्ट हो गया है। सभी व्यापार अलर्ट्स सक्रिय हैं।
                ⏰ $timeStr
            """.trimIndent()

            "ENGLISH_PRO" -> """
                <b>🏢 $shopHeader</b>
                🔔 <b>Telegram Bot Test Successful!</b>
                Your Telegram alert integration is active and properly connected.
                All business notifications will be delivered to this chat.
                ⏰ <i>$timeStr</i>
            """.trimIndent()

            else -> """
                <b>🏬 $shopHeader</b>
                🔔 <b>टेलीग्राम बॉट परीक्षण संदेश (Test Alert)</b>
                ━━━━━━━━━━━━━━━━━━━━
                ✅ <b>सफल कनेक्शन:</b> आपका टेलीग्राम बॉट सही तरीके से कॉन्फ़िगर हो चुका है।
                📱 <b>सिस्टम:</b> $shopHeader
                ⏰ <b>समय:</b> $timeStr
                ━━━━━━━━━━━━━━━━━━━━
                <i>अब नए बिल, पेमेंट्स, स्टॉक चेतावनी, हाजिरी व सैलरी अलर्ट्स यहां सीधे आते रहेंगे।</i>
            """.trimIndent()
        }

        return sendMessage(botToken, chatId, message, parseMode = "HTML")
    }

    suspend fun sendTestMessage(
        settings: ShopSettings,
        botToken: String = settings.telegramBotToken,
        chatId: String = settings.telegramChatId,
        format: String = settings.telegramFormat,
        customTitle: String = settings.telegramCustomTitle
    ): Pair<Boolean, String> {
        return sendTestMessage(botToken, chatId, format, customTitle.ifBlank { settings.shopName })
    }

    /**
     * 1. Notify when a new sale / invoice is created
     */
    suspend fun notifyNewSale(config: TelegramActiveConfig, orderWithItems: OrderWithItems) {
        if (!config.isReady || !config.alertNewSale) return

        val order = orderWithItems.order
        val items = orderWithItems.items
        val shopHeader = config.customTitle
        val timeStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(order.createdAt))
        val totalBoxes = items.sumOf { it.fullBoxes }

        val text = when (config.format) {
            "COMPACT_HINDI" -> """
                <b>🏬 $shopHeader</b> | 🧾 <b>नया बिल #${order.invoiceNumber}</b>
                👤 ग्राहक: <b>${order.customerName}</b> (${order.customerPhone.ifBlank { "N/A" }})
                📦 बॉक्स: <b>$totalBoxes Bx</b> (${items.size} टाइल्स)
                💰 कुल बिल: <b>₹${String.format(Locale.US, "%,.2f", order.grandTotal)}</b>
                📊 स्थिति: <b>${if (order.status == "CONFIRMED") "स्वीकृत (Confirmed)" else "पेंडिंग"}</b>
                ⏰ $timeStr
            """.trimIndent()

            "ENGLISH_PRO" -> """
                <b>🏢 $shopHeader</b>
                🧾 <b>NEW INVOICE GENERATED</b>
                ━━━━━━━━━━━━━━━━━━━━
                📋 <b>Invoice:</b> #${order.invoiceNumber}
                👤 <b>Customer:</b> ${order.customerName} (${order.customerPhone.ifBlank { "N/A" }})
                📦 <b>Total Boxes:</b> $totalBoxes Bx (${items.size} tile items)
                💰 <b>Invoice Total:</b> ₹${String.format(Locale.US, "%,.2f", order.grandTotal)}
                📊 <b>Status:</b> ${order.status}
                📅 <b>Date:</b> $timeStr
                ━━━━━━━━━━━━━━━━━━━━
                ✅ <i>TILES AND MARBLES System</i>
            """.trimIndent()

            else -> """
                <b>🏬 $shopHeader</b>
                🧾 <b>नया बिल / बिक्री दर्ज हुई!</b>
                ━━━━━━━━━━━━━━━━━━━━
                📋 <b>बिल नंबर:</b> #${order.invoiceNumber}
                👤 <b>ग्राहक का नाम:</b> ${order.customerName}
                📞 <b>मोबाइल:</b> ${order.customerPhone.ifBlank { "उपलब्ध नहीं" }}
                📦 <b>कुल टाइल्स बॉक्स:</b> $totalBoxes Bx (${items.size} वैरायटी)
                💰 <b>कुल बिल राशि:</b> <b>₹${String.format(Locale.US, "%,.2f", order.grandTotal)}</b>
                💵 <b>जमा राशि:</b> ₹${String.format(Locale.US, "%,.2f", order.paidAmount)}
                ⚠️ <b>बकाया उधार:</b> ₹${String.format(Locale.US, "%,.2f", order.balanceAmount)}
                📊 <b>आर्डर स्थिति:</b> ${if (order.status == "CONFIRMED") "✅ स्वीकृत / कन्फर्म" else "⏳ पेंडिंग"}
                📅 <b>दिनांक:</b> $timeStr
                ━━━━━━━━━━━━━━━━━━━━
                ✅ <i>TILES AND MARBLES स्मार्ट व्यापार सिस्टम</i>
            """.trimIndent()
        }

        sendActiveMessage(config, text)
    }

    suspend fun notifyNewSale(settings: ShopSettings, orderWithItems: OrderWithItems) {
        notifyNewSale(TelegramActiveConfig.resolve(null, settings), orderWithItems)
    }

    /**
     * 2. Notify when customer makes a payment
     */
    suspend fun notifyPaymentReceived(
        config: TelegramActiveConfig,
        invoiceNumber: String,
        customerName: String,
        amount: Double,
        paymentMode: String,
        remainingBalance: Double
    ) {
        if (!config.isReady || !config.alertPayment) return

        val shopHeader = config.customTitle
        val timeStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val text = when (config.format) {
            "COMPACT_HINDI" -> """
                <b>🏬 $shopHeader</b> | 💵 <b>पेमेंट प्राप्त!</b>
                👤 <b>$customerName</b> | बिल: #$invoiceNumber
                💰 जमा राशि: <b>+₹${String.format(Locale.US, "%,.2f", amount)}</b> ($paymentMode)
                ⚠️ शेष बाकी: ₹${String.format(Locale.US, "%,.2f", remainingBalance)}
                ⏰ $timeStr
            """.trimIndent()

            "ENGLISH_PRO" -> """
                <b>🏢 $shopHeader</b>
                💵 <b>PAYMENT RECEIVED</b>
                ━━━━━━━━━━━━━━━━━━━━
                👤 <b>Customer:</b> $customerName
                📋 <b>Invoice:</b> #$invoiceNumber
                💰 <b>Amount Received:</b> +₹${String.format(Locale.US, "%,.2f", amount)}
                💳 <b>Payment Mode:</b> $paymentMode
                ⚠️ <b>Remaining Due:</b> ₹${String.format(Locale.US, "%,.2f", remainingBalance)}
                ⏰ <b>Time:</b> $timeStr
            """.trimIndent()

            else -> """
                <b>🏬 $shopHeader</b>
                💵 <b>भुगतान / पेमेंट जमा हुआ!</b>
                ━━━━━━━━━━━━━━━━━━━━
                👤 <b>ग्राहक:</b> $customerName
                📋 <b>बिल संदर्भ:</b> #$invoiceNumber
                💰 <b>जमा की गई राशि:</b> <b>₹${String.format(Locale.US, "%,.2f", amount)}</b>
                💳 <b>भुगतान माध्यम:</b> $paymentMode
                ⚠️ <b>अब बकाया शेष:</b> ₹${String.format(Locale.US, "%,.2f", remainingBalance)}
                ⏰ <b>समय:</b> $timeStr
                ━━━━━━━━━━━━━━━━━━━━
                ✅ <i>खाता बही अपडेट</i>
            """.trimIndent()
        }

        sendActiveMessage(config, text)
    }

    suspend fun notifyPaymentReceived(
        settings: ShopSettings,
        invoiceNumber: String,
        customerName: String,
        amount: Double,
        paymentMode: String,
        remainingBalance: Double
    ) {
        notifyPaymentReceived(
            TelegramActiveConfig.resolve(null, settings),
            invoiceNumber,
            customerName,
            amount,
            paymentMode,
            remainingBalance
        )
    }

    /**
     * 3. Notify when product reaches low stock
     */
    suspend fun notifyLowStock(
        config: TelegramActiveConfig,
        product: TileProduct,
        currentBoxes: Int,
        threshold: Int
    ) {
        if (!config.isReady || !config.alertLowStock) return

        val shopHeader = config.customTitle
        val text = """
            <b>⚠️ स्टॉक अलर्ट (Low Stock Alert)</b>
            🏬 <b>$shopHeader</b>
            ━━━━━━━━━━━━━━━━━━━━
            📦 <b>उत्पाद:</b> ${product.name}
            🏷️ <b>कोड / साइज:</b> ${product.code} (${product.size})
            🚨 <b>उपलब्ध स्टॉक:</b> केवल <b>$currentBoxes बॉक्स</b> (न्यूनतम सीमा: $threshold)
            💡 <i>कृपया सप्लायर से नया माल मंगवाने का आर्डर दें।</i>
        """.trimIndent()

        sendActiveMessage(config, text)
    }

    suspend fun notifyLowStock(settings: ShopSettings, product: TileProduct) {
        notifyLowStock(TelegramActiveConfig.resolve(null, settings), product, product.fullBoxes, 10)
    }

    /**
     * 4. Notify staff attendance marking
     */
    suspend fun notifyStaffAttendance(
        config: TelegramActiveConfig,
        staffName: String,
        dateString: String,
        status: String,
        notes: String = ""
    ) {
        if (!config.isReady || !config.alertAttendance) return

        val shopHeader = config.customTitle
        val statusHindi = when (status) {
            "PRESENT" -> "✅ उपस्थित (Present)"
            "HALF_DAY" -> "⚠️ आधा दिन (Half Day)"
            "ABSENT" -> "❌ अनुपस्थित / छुट्टी (Absent)"
            "PAID_LEAVE" -> "🏖️ सवेतन अवकाश (Paid Leave)"
            else -> status
        }

        val text = """
            <b>👷 स्टाफ हाजिरी अलर्ट</b>
            🏬 <b>$shopHeader</b>
            ━━━━━━━━━━━━━━━━━━━━
            👤 <b>कर्मचारी:</b> $staffName
            📅 <b>तारीख:</b> $dateString
            📊 <b>स्थिति:</b> <b>$statusHindi</b>
            ${if (notes.isNotBlank()) "📝 <b>नोट:</b> $notes\n" else ""}━━━━━━━━━━━━━━━━━━━━
            ✅ <i>कर्मचारी उपस्थिति रजिस्टर</i>
        """.trimIndent()

        sendActiveMessage(config, text)
    }

    suspend fun notifyStaffAttendance(
        settings: ShopSettings,
        staffName: String,
        dateString: String,
        status: String,
        notes: String = ""
    ) {
        notifyStaffAttendance(TelegramActiveConfig.resolve(null, settings), staffName, dateString, status, notes)
    }

    /**
     * 5. Notify Staff Salary or Advance Payment
     */
    suspend fun notifySalaryOrAdvancePaid(
        config: TelegramActiveConfig,
        staffName: String,
        type: String, // "SALARY", "ADVANCE"
        amount: Double,
        paymentMode: String,
        remainingBalance: Double,
        notes: String = ""
    ) {
        if (!config.isReady || !config.alertSalaryAdvance) return

        val shopHeader = config.customTitle
        val typeLabel = if (type == "ADVANCE") "💸 अग्रिम भुगतान (Advance Payment)" else "💰 वेतन भुगतान (Salary Paid)"
        val timeStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val text = """
            <b>$typeLabel</b>
            🏬 <b>$shopHeader</b>
            ━━━━━━━━━━━━━━━━━━━━
            👤 <b>स्टाफ सदस्य:</b> $staffName
            💵 <b>भुगतान राशि:</b> <b>₹${String.format(Locale.US, "%,.2f", amount)}</b>
            💳 <b>माध्यम:</b> $paymentMode
            📊 <b>शेष बैलेंस:</b> ₹${String.format(Locale.US, "%,.2f", remainingBalance)}
            ${if (notes.isNotBlank()) "📝 <b>नोट:</b> $notes\n" else ""}⏰ <b>समय:</b> $timeStr
            ━━━━━━━━━━━━━━━━━━━━
            ✅ <i>स्टाफ पेरोल रिकॉर्ड</i>
        """.trimIndent()

        sendActiveMessage(config, text)
    }

    /**
     * 6. Notify New Product Added or Stock Adjusted
     */
    suspend fun notifyNewProductOrStock(
        config: TelegramActiveConfig,
        title: String,
        productName: String,
        code: String,
        size: String,
        stockDisplay: String,
        sellingPrice: Double
    ) {
        if (!config.isReady || !config.alertNewProduct) return

        val shopHeader = config.customTitle
        val timeStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val text = """
            <b>📦 $title</b>
            🏬 <b>$shopHeader</b>
            ━━━━━━━━━━━━━━━━━━━━
            🏷️ <b>उत्पाद नाम:</b> $productName
            🔖 <b>कोड / साइज:</b> $code ($size)
            📊 <b>उपलब्ध स्टॉक:</b> $stockDisplay
            💰 <b>बिक्री मूल्य:</b> ₹${String.format(Locale.US, "%,.2f", sellingPrice)} / बॉक्स
            ⏰ <b>समय:</b> $timeStr
            ━━━━━━━━━━━━━━━━━━━━
            ✅ <i>इन्वेंटरी स्टॉक रजिस्टर</i>
        """.trimIndent()

        sendActiveMessage(config, text)
    }

    /**
     * 7. Notify Invoice Deletion or Cancellation
     */
    suspend fun notifyInvoiceDeleted(
        config: TelegramActiveConfig,
        invoiceNumber: String,
        customerName: String,
        grandTotal: Double,
        reason: String
    ) {
        if (!config.isReady || !config.alertInvoiceDeleted) return

        val shopHeader = config.customTitle
        val timeStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val text = """
            <b>🗑️ बिल हटाया गया (Invoice Deleted Alert)</b>
            🏬 <b>$shopHeader</b>
            ━━━━━━━━━━━━━━━━━━━━
            📋 <b>बिल नंबर:</b> #$invoiceNumber
            👤 <b>ग्राहक:</b> $customerName
            💰 <b>रद्द बिल राशि:</b> ₹${String.format(Locale.US, "%,.2f", grandTotal)}
            ⚠️ <b>कारण / विवरण:</b> ${reason.ifBlank { "प्रशासक / यूजर द्वारा हटाया गया" }}
            ⏰ <b>समय:</b> $timeStr
            ━━━━━━━━━━━━━━━━━━━━
            ♻️ <i>बिल रिसाइकिल बिन में स्थानांतरित किया गया</i>
        """.trimIndent()

        sendActiveMessage(config, text)
    }

    /**
     * 8. Notify User Action / Security Event
     */
    suspend fun notifyUserAction(
        config: TelegramActiveConfig,
        eventTitle: String,
        userPhone: String,
        details: String
    ) {
        if (!config.isReady || !config.alertUserAction) return

        val shopHeader = config.customTitle
        val timeStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val text = """
            <b>🛡️ सुरक्षा एवं खाता अलर्ट: $eventTitle</b>
            🏬 <b>$shopHeader</b>
            ━━━━━━━━━━━━━━━━━━━━
            📱 <b>यूजर मोबाइल:</b> $userPhone
            📋 <b>विवरण:</b> $details
            ⏰ <b>समय:</b> $timeStr
            ━━━━━━━━━━━━━━━━━━━━
            ✅ <i>प्रशासक सुरक्षा मॉनिटर</i>
        """.trimIndent()

        sendActiveMessage(config, text)
    }

    fun getSamplePreview(format: String, customTitle: String): String {
        val title = customTitle.ifBlank { "TILES AND MARBLES" }
        return when (format) {
            "COMPACT_HINDI" -> """
                🏬 $title | 🧾 नया बिल #INV-0012
                👤 ग्राहक: राजेश शर्मा (9823456780)
                📦 बॉक्स: 24 Bx | 💰 कुल बिल: ₹18,500
                💵 जमा: ₹10,000 | ⚠️ बाकी: ₹8,500
                ⏰ 21 Oct 2026, 12:30 PM
            """.trimIndent()

            "ENGLISH_PRO" -> """
                🏢 $title
                🧾 NEW INVOICE GENERATED
                ━━━━━━━━━━━━━━━━━━━━
                📋 Invoice: #INV-0012
                👤 Customer: Rajesh Sharma (9823456780)
                📦 Total Boxes: 24 Bx
                💰 Total: ₹18,500.00 | Paid: ₹10,000.00
                ⚠️ Balance Due: ₹8,500.00
                📅 Date: 21 Oct 2026, 12:30 PM
                ━━━━━━━━━━━━━━━━━━━━
                ✅ TILES AND MARBLES System
            """.trimIndent()

            else -> """
                🏬 $title
                🧾 नया बिल / बिक्री दर्ज हुई!
                ━━━━━━━━━━━━━━━━━━━━
                📋 बिल नंबर: #INV-0012
                👤 ग्राहक का नाम: राजेश शर्मा
                📞 मोबाइल: 9823456780
                📦 कुल टाइल्स बॉक्स: 24 Bx
                💰 कुल बिल राशि: ₹18,500.00
                💵 जमा भुगतान: ₹10,000.00 (UPI)
                ⚠️ उधारी / बाकी: ₹8,500.00
                📅 दिनांक: 21 Oct 2026, 12:30 PM
                ━━━━━━━━━━━━━━━━━━━━
                ✅ TILES AND MARBLES स्मार्ट व्यापार
            """.trimIndent()
        }
    }
}
