package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.OrderEntity
import com.example.data.model.ShopSettings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareHelper {

    fun validateIndianMobileNumber(rawNumber: String): Pair<Boolean, String> {
        val digitsOnly = rawNumber.replace("[^0-9]".toRegex(), "")
        val tenDigit = when {
            digitsOnly.length == 10 -> digitsOnly
            digitsOnly.length == 12 && digitsOnly.startsWith("91") -> digitsOnly.substring(2)
            digitsOnly.length == 11 && digitsOnly.startsWith("0") -> digitsOnly.substring(1)
            else -> return Pair(false, "Phone number must be a valid 10-digit Indian mobile number")
        }

        val isValidPrefix = tenDigit.first() in listOf('6', '7', '8', '9')
        return if (isValidPrefix) {
            Pair(true, "91$tenDigit")
        } else {
            Pair(false, "Indian mobile numbers must start with 6, 7, 8, or 9")
        }
    }

    fun buildShareMessage(
        settings: ShopSettings,
        order: OrderEntity
    ): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateStr = sdf.format(Date(order.createdAt))
        val statusStr = if (order.status == "CONFIRMED") "Confirmed Sale" else "Quotation / Estimate"

        return buildString {
            append("Dear ${order.customerName},\n\n")
            val sub = if (settings.subtitle.isNotBlank()) " (${settings.subtitle})" else ""
            append("Greetings from *${settings.shopName.ifBlank { "TILES AND MARBLES" }}*$sub.\n\n")
            append("Please find attached your bill:\n")
            append("📄 *Invoice / Ref:* ${order.invoiceNumber}\n")
            append("📅 *Date:* $dateStr\n")
            append("📌 *Status:* $statusStr\n")
            if (order.showPriceOnPdf) {
                append("💰 *Amount:* ₹${String.format(Locale.US, "%.2f", order.grandTotal)}\n")
            } else {
                append("📦 *Document:* Material Delivery Slip (Prices hidden)\n")
            }
            append("\nFor any queries, please reach us at ${settings.ownerPhone}.\n")
            append("Thank you for your business!")
        }
    }

    fun sharePdfViaWhatsApp(
        context: Context,
        pdfFile: File,
        targetPhoneFormatted: String?,
        messageText: String
    ): Boolean {
        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, messageText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Check if WhatsApp is installed
            val packageManager = context.packageManager
            val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
            var targetPackage: String? = null

            for (pkg in whatsappPackages) {
                try {
                    packageManager.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
                    targetPackage = pkg
                    break
                } catch (ignored: Exception) {
                }
            }

            if (targetPackage != null) {
                intent.setPackage(targetPackage)
                context.startActivity(intent)
                true
            } else {
                // Fall back to general share chooser
                sharePdfGeneral(context, pdfFile, messageText)
                true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun sharePdfGeneral(
        context: Context,
        pdfFile: File,
        messageText: String
    ) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, messageText)
                putExtra(Intent.EXTRA_SUBJECT, "Bill - ${pdfFile.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, "Share Bill via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open share dialog: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun viewOrPrintPdf(context: Context, pdfFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF viewer app found. Please share the file instead.", Toast.LENGTH_LONG).show()
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, title)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun viewPdf(context: Context, pdfFile: File) {
        viewOrPrintPdf(context, pdfFile)
    }

    fun sharePdf(context: Context, pdfFile: File, title: String = "Share PDF") {
        sharePdfGeneral(context, pdfFile, title)
    }
}
