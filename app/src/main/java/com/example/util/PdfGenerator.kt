package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.model.OrderItemEntity
import com.example.data.model.OrderWithItems
import com.example.data.model.ShopSettings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 36f

    fun generateOrderPdf(
        context: Context,
        orderWithItems: OrderWithItems,
        settings: ShopSettings
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawPdfContent(context, canvas, orderWithItems, settings)

        pdfDocument.finishPage(page)

        // Save to cache/pdfs directory
        val pdfsDir = File(context.cacheDir, "pdfs")
        if (!pdfsDir.exists()) pdfsDir.mkdirs()

        val cleanInvoice = orderWithItems.order.invoiceNumber.replace("[^a-zA-Z0-9-]".toRegex(), "_")
        val outputFile = File(pdfsDir, "Bill_${cleanInvoice}.pdf")
        FileOutputStream(outputFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        return outputFile
    }

    private fun drawPdfContent(
        context: Context,
        canvas: Canvas,
        orderWithItems: OrderWithItems,
        settings: ShopSettings
    ) {
        val order = orderWithItems.order
        val items = orderWithItems.items
        val showPrices = order.showPriceOnPdf
        val pdfSizeConfig = PdfSizeDisplayConfig.fromJson(settings.pdfSizeDisplayConfigJson)

        // Color definitions dynamically derived from Shop App Theme
        val themeOption = com.example.ui.theme.getAppThemeOption(settings.appTheme)
        val navyPrimary = Color.rgb(
            (themeOption.primary.red * 255).toInt(),
            (themeOption.primary.green * 255).toInt(),
            (themeOption.primary.blue * 255).toInt()
        )
        val navySecondary = Color.rgb(
            (themeOption.primaryContainer.red * 255).toInt(),
            (themeOption.primaryContainer.green * 255).toInt(),
            (themeOption.primaryContainer.blue * 255).toInt()
        )
        val accentBlue = Color.rgb(
            (themeOption.accent.red * 255).toInt(),
            (themeOption.accent.green * 255).toInt(),
            (themeOption.accent.blue * 255).toInt()
        )
        val textDark = Color.rgb(30, 41, 59)
        val textMuted = Color.rgb(100, 116, 139)
        val lightBg = Color.rgb(248, 250, 252)
        val borderColor = Color.rgb(226, 232, 240)
        val pendingOrange = Color.rgb(234, 88, 12)
        val confirmedGreen = Color.rgb(22, 163, 74)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Top Decorative Brand Bar
        paint.color = navyPrimary
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 12f, paint)

        // 2. Shop Logo or Brand Monogram
        val logoSize = 54f
        val logoLeft = MARGIN
        val logoTop = 24f
        var logoDrawn = false

        if (!settings.logoUri.isNullOrBlank()) {
            val logoBitmap = loadBitmap(context, settings.logoUri)
            if (logoBitmap != null) {
                val destRect = RectF(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)
                canvas.drawBitmap(logoBitmap, null, destRect, paint)
                logoDrawn = true
            }
        }

        if (!logoDrawn) {
            // Elegant Tile Monogram Crest using shop's first letter
            paint.color = navySecondary
            val badgeRect = RectF(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)
            canvas.drawRoundRect(badgeRect, 8f, 8f, paint)

            paint.color = accentBlue
            paint.textSize = 28f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            val monogramLetter = settings.shopName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "M"
            canvas.drawText(monogramLetter, logoLeft + (logoSize / 2f), logoTop + 38f, paint)
        }

        // 3. Shop Details & Document Badge Layout (Dynamic spacing so long shop names never overlap)
        val rightX = PAGE_WIDTH - MARGIN
        val docTitle = when {
            !showPrices -> "DELIVERY CHALLAN / MATERIAL SLIP"
            order.status == "CONFIRMED" -> "TAX INVOICE / SALE BILL"
            else -> "PRICE QUOTATION / ESTIMATE"
        }

        // Measure right badge area first so shop details know their max allowed boundary
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        val docTitleWidth = paint.measureText(docTitle)
        val badgeBlockWidth = maxOf(docTitleWidth, 140f) + 12f
        val maxShopWidth = (rightX - badgeBlockWidth) - (logoLeft + logoSize + 14f)

        val headerTextLeft = logoLeft + logoSize + 14f
        paint.textAlign = Paint.Align.LEFT
        paint.color = navyPrimary

        // Auto-scale shop name if long to fit nicely without overlapping
        val rawShopName = settings.shopName.ifBlank { "TILES AND MARBLES" }
        var shopFontSize = 16f
        paint.textSize = shopFontSize
        while (paint.measureText(rawShopName) > maxShopWidth && shopFontSize > 10.5f) {
            shopFontSize -= 0.5f
            paint.textSize = shopFontSize
        }
        canvas.drawText(rawShopName, headerTextLeft, 38f, paint)

        var curY = 38f
        if (settings.subtitle.isNotBlank()) {
            curY += 13f
            paint.color = accentBlue
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val sub = if (paint.measureText(settings.subtitle.trim()) > maxShopWidth) settings.subtitle.trim().take(40) else settings.subtitle.trim()
            canvas.drawText(sub, headerTextLeft, curY, paint)
        }

        if (settings.address.isNotBlank()) {
            curY += 11f
            paint.color = textMuted
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8f
            val addr = if (paint.measureText(settings.address.trim()) > maxShopWidth) settings.address.trim().take(45) else settings.address.trim()
            canvas.drawText(addr, headerTextLeft, curY, paint)
        }

        if (settings.ownerPhone.isNotBlank() || settings.gstNumber.isNotBlank()) {
            curY += 11f
            paint.color = textMuted
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8f
            val contactText = if (settings.ownerPhone.isNotBlank()) "Ph: ${settings.ownerPhone}" else ""
            val gstText = if (settings.gstNumber.isNotBlank()) " | GST: ${settings.gstNumber}" else ""
            canvas.drawText((contactText + gstText).take(48), headerTextLeft, curY, paint)
        }

        // 4. Document Badge (Tax Invoice / Quotation / Delivery Slip) on Right
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12.5f
        paint.color = navyPrimary
        canvas.drawText(docTitle, rightX, 38f, paint)

        // Order status badge pill
        val statusText = if (order.status == "CONFIRMED") "CONFIRMED SALE" else "PENDING ORDER"
        val statusBg = if (order.status == "CONFIRMED") Color.rgb(220, 252, 231) else Color.rgb(255, 237, 213)
        val statusFg = if (order.status == "CONFIRMED") confirmedGreen else pendingOrange

        paint.textSize = 8f
        val pillWidth = paint.measureText(statusText) + 16f
        val pillRect = RectF(rightX - pillWidth, 46f, rightX, 62f)
        paint.color = statusBg
        canvas.drawRoundRect(pillRect, 4f, 4f, paint)

        paint.color = statusFg
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(statusText, pillRect.centerX(), 57f, paint)

        // 5. Divider Line (Placed dynamically below shop details and badge)
        val headerBottom = maxOf(curY + 12f, 78f)
        paint.color = borderColor
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, headerBottom, rightX, headerBottom, paint)

        // 6. Meta Information Panel (Invoice & Customer Details)
        val metaTop = headerBottom + 8f
        paint.textAlign = Paint.Align.LEFT

        // Left box: Customer Information
        paint.color = lightBg
        val custBox = RectF(MARGIN, metaTop, (PAGE_WIDTH / 2f) - 6f, metaTop + 68f)
        canvas.drawRoundRect(custBox, 6f, 6f, paint)
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(custBox, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = accentBlue
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawText("BILLED TO / CUSTOMER DETAILS", MARGIN + 10f, metaTop + 14f, paint)

        paint.color = textDark
        paint.textSize = 10f
        canvas.drawText(order.customerName.ifBlank { "Cash Customer" }, MARGIN + 10f, metaTop + 28f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8.5f
        paint.color = textMuted
        val phoneStr = if (order.customerPhone.isNotBlank()) "Mobile: ${order.customerPhone}" else "Mobile: Not specified"
        canvas.drawText(phoneStr, MARGIN + 10f, metaTop + 42f, paint)
        val addrStr = if (order.customerAddress.isNotBlank()) "Address: ${order.customerAddress}" else "Address: Local Counter"
        canvas.drawText(addrStr.take(45), MARGIN + 10f, metaTop + 56f, paint)

        // Right box: Invoice Details
        val invBox = RectF((PAGE_WIDTH / 2f) + 6f, metaTop, rightX, metaTop + 68f)
        paint.color = lightBg
        canvas.drawRoundRect(invBox, 6f, 6f, paint)
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(invBox, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = accentBlue
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawText("ORDER & INVOICE DETAILS", (PAGE_WIDTH / 2f) + 16f, metaTop + 14f, paint)

        paint.color = textDark
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Invoice No:", (PAGE_WIDTH / 2f) + 16f, metaTop + 28f, paint)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(order.invoiceNumber, (PAGE_WIDTH / 2f) + 80f, metaTop + 28f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Date:", (PAGE_WIDTH / 2f) + 16f, metaTop + 42f, paint)
        paint.typeface = Typeface.DEFAULT
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        canvas.drawText(sdf.format(Date(order.createdAt)), (PAGE_WIDTH / 2f) + 80f, metaTop + 42f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Price Mode:", (PAGE_WIDTH / 2f) + 16f, metaTop + 56f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.color = if (showPrices) confirmedGreen else accentBlue
        canvas.drawText(if (showPrices) "Prices Displayed" else "Hidden (Quantity Slip)", (PAGE_WIDTH / 2f) + 80f, metaTop + 56f, paint)

        // 7. Items Table Header
        val tableTop = 180f
        val headerHeight = 22f

        paint.color = navyPrimary
        paint.style = Paint.Style.FILL
        canvas.drawRect(MARGIN, tableTop, rightX, tableTop + headerHeight, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.textAlign = Paint.Align.LEFT

        // Column Layout:
        // Col 1: # (MARGIN + 4)
        // Col 2: Photo (MARGIN + 24)
        // Col 3: Tile Description & Size (MARGIN + 70)
        // Col 4: Pcs/Bx (rightX - (if showPrices 220 else 120))
        // Col 5: Boxes (rightX - (if showPrices 160 else 60))
        // Col 6: Loose (rightX - (if showPrices 110 else 10))
        // If showPrices:
        // Col 7: Rate/Bx (rightX - 60)
        // Col 8: Amount (rightX - 8)

        canvas.drawText("#", MARGIN + 6f, tableTop + 14f, paint)
        canvas.drawText("PHOTO", MARGIN + 26f, tableTop + 14f, paint)
        canvas.drawText("TILE PRODUCT & SIZE", MARGIN + 72f, tableTop + 14f, paint)

        val colPcsBxX = if (showPrices) rightX - 220f else rightX - 140f
        val colBoxesX = if (showPrices) rightX - 165f else rightX - 85f
        val colLooseX = if (showPrices) rightX - 110f else rightX - 30f
        val colRateX = rightX - 58f
        val colTotalX = rightX - 8f

        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("PCS/BX", colPcsBxX, tableTop + 14f, paint)
        canvas.drawText("BOXES", colBoxesX, tableTop + 14f, paint)
        canvas.drawText("LOOSE", colLooseX, tableTop + 14f, paint)

        if (showPrices) {
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("RATE/BX", colRateX, tableTop + 14f, paint)
            canvas.drawText("AMOUNT (₹)", colTotalX, tableTop + 14f, paint)
        }

        // 8. Items Table Rows
        var currentY = tableTop + headerHeight
        val rowHeight = 44f
        val imgThumbSize = 34f

        for (i in items.indices) {
            val item = items[i]
            val isEven = i % 2 == 0

            // Zebra background
            paint.color = if (isEven) Color.WHITE else lightBg
            paint.style = Paint.Style.FILL
            canvas.drawRect(MARGIN, currentY, rightX, currentY + rowHeight, paint)

            // Bottom border
            paint.color = borderColor
            canvas.drawLine(MARGIN, currentY + rowHeight, rightX, currentY + rowHeight, paint)

            // Index
            paint.color = textDark
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("${i + 1}", MARGIN + 6f, currentY + 24f, paint)

            // Thumbnail Photo respecting user's custom PDF Size Display rules (orientation, scale, fit/crop, label)
            val maxThumbW = 42f
            val maxThumbH = 34f
            val resolvedSize = PdfSizeDisplayConfig.resolveForTile(item.tileSize, pdfSizeConfig)
            val ratio = resolvedSize.renderAspectRatioWtoH.coerceIn(0.4f, 2.5f)
            val thumbScale = if (resolvedSize.hasCustomScale) resolvedSize.scaleFactor.coerceIn(0.55f, 1.0f) else 1.0f
            val targetThumbW = maxThumbW * thumbScale
            val targetThumbH = maxThumbH * thumbScale

            var renderW: Float
            var renderH: Float
            if (targetThumbW / targetThumbH > ratio) {
                renderH = targetThumbH
                renderW = renderH * ratio
            } else {
                renderW = targetThumbW
                renderH = renderW / ratio
            }
            if (renderW > maxThumbW) {
                val s = maxThumbW / renderW
                renderW *= s
                renderH *= s
            }
            if (renderH > maxThumbH) {
                val s = maxThumbH / renderH
                renderW *= s
                renderH *= s
            }
            val thumbLeft = MARGIN + 20f + (maxThumbW - renderW) / 2f
            val thumbTop = currentY + 5f + (maxThumbH - renderH) / 2f
            val thumbRect = RectF(thumbLeft, thumbTop, thumbLeft + renderW, thumbTop + renderH)

            var photoDrawn = false
            if (!item.tilePhotoUri.isNullOrBlank()) {
                val bitmap = loadBitmap(context, item.tilePhotoUri)
                if (bitmap != null) {
                    val srcRect = if (resolvedSize.imageFitMode == PdfImageScaleMode.CENTER_CROP) {
                        PdfSizeDisplayConfig.computeCenterCropSrcRect(bitmap.width, bitmap.height, renderW, renderH)
                    } else {
                        null
                    }
                    canvas.drawBitmap(bitmap, srcRect, thumbRect, paint)
                    photoDrawn = true
                }
            }

            if (!photoDrawn) {
                // Sleek tile graphic placeholder with realistic aspect ratio
                paint.color = Color.rgb(241, 245, 249)
                canvas.drawRoundRect(thumbRect, 3f, 3f, paint)
                paint.color = borderColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.8f
                canvas.drawRoundRect(thumbRect, 3f, 3f, paint)
                paint.style = Paint.Style.FILL

                paint.color = textMuted
                paint.textSize = 6f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(resolvedSize.sizeLabelForPdf.take(10), thumbRect.centerX(), thumbRect.centerY() + 2.5f, paint)
            }

            // Tile Description & Size
            paint.textAlign = Paint.Align.LEFT
            paint.color = textDark
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9.5f
            val maxNameLen = if (showPrices) 28 else 40
            canvas.drawText(item.tileName.take(maxNameLen), MARGIN + 72f, currentY + 18f, paint)

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8f
            paint.color = textMuted
            val detailsStr = "Code: ${item.tileCode} | Size: ${resolvedSize.sizeLabelForPdf}"
            canvas.drawText(detailsStr, MARGIN + 72f, currentY + 32f, paint)

            // Quantities
            paint.textAlign = Paint.Align.CENTER
            paint.color = textDark
            paint.textSize = 9f
            canvas.drawText("${item.piecesPerBox}", colPcsBxX, currentY + 24f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${item.fullBoxes}", colBoxesX, currentY + 24f, paint)

            paint.typeface = Typeface.DEFAULT
            val looseStr = if (item.loosePieces > 0) "${item.loosePieces} pcs" else "-"
            canvas.drawText(looseStr, colLooseX, currentY + 24f, paint)

            // (CRITICAL RULE CHECK: Total pieces is NEVER drawn on customer PDF!)

            if (showPrices) {
                paint.textAlign = Paint.Align.RIGHT
                paint.typeface = Typeface.DEFAULT
                canvas.drawText(String.format(Locale.US, "₹%.2f", item.pricePerBox), colRateX, currentY + 24f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = navyPrimary
                canvas.drawText(String.format(Locale.US, "₹%.2f", item.lineTotal), colTotalX, currentY + 24f, paint)
            }

            currentY += rowHeight
        }

        // 9. Summary & Totals Section
        val summaryTop = currentY + 12f
        if (showPrices) {
            val totalBoxLeft = rightX - 220f
            val totalBoxWidth = 220f
            val hasDiscount = order.discount > 0
            val totalBoxHeight = if (hasDiscount) 124f else 108f
            val summaryRect = RectF(totalBoxLeft, summaryTop, rightX, summaryTop + totalBoxHeight)

            paint.color = lightBg
            canvas.drawRoundRect(summaryRect, 6f, 6f, paint)
            paint.color = borderColor
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(summaryRect, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            var sy = summaryTop + 16f
            paint.textAlign = Paint.Align.LEFT
            paint.color = textMuted
            paint.textSize = 8.5f
            canvas.drawText("Subtotal:", totalBoxLeft + 12f, sy, paint)
            paint.textAlign = Paint.Align.RIGHT
            paint.color = textDark
            canvas.drawText(String.format(Locale.US, "₹%.2f", order.subtotal), rightX - 12f, sy, paint)

            if (hasDiscount) {
                sy += 14f
                paint.textAlign = Paint.Align.LEFT
                paint.color = Color.rgb(220, 38, 38)
                canvas.drawText("Discount:", totalBoxLeft + 12f, sy, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(String.format(Locale.US, "-₹%.2f", order.discount), rightX - 12f, sy, paint)
            }

            sy += 16f
            paint.color = borderColor
            canvas.drawLine(totalBoxLeft + 12f, sy - 4f, rightX - 12f, sy - 4f, paint)

            // Grand Total
            paint.textAlign = Paint.Align.LEFT
            paint.color = navyPrimary
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            canvas.drawText("Grand Total (कुल):", totalBoxLeft + 12f, sy + 8f, paint)
            paint.textAlign = Paint.Align.RIGHT
            paint.textSize = 11f
            canvas.drawText(String.format(Locale.US, "₹%.2f", order.grandTotal), rightX - 12f, sy + 8f, paint)

            // Paid Amount
            sy += 18f
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.rgb(22, 101, 52) // Green
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9.5f
            canvas.drawText("Paid / Advance (जमा):", totalBoxLeft + 12f, sy + 6f, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(String.format(Locale.US, "₹%.2f", order.paidAmount), rightX - 12f, sy + 6f, paint)

            // Balance Due
            sy += 18f
            val balance = order.balanceAmount
            paint.textAlign = Paint.Align.LEFT
            if (balance > 0.01) {
                paint.color = Color.rgb(185, 28, 28) // Bold Red
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
                canvas.drawText("Balance Due (बकाया):", totalBoxLeft + 12f, sy + 6f, paint)
                paint.textAlign = Paint.Align.RIGHT
                paint.textSize = 11f
                canvas.drawText(String.format(Locale.US, "₹%.2f", balance), rightX - 12f, sy + 6f, paint)
            } else {
                paint.color = Color.rgb(21, 128, 61) // Full paid green
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
                canvas.drawText("Payment Status:", totalBoxLeft + 12f, sy + 6f, paint)
                paint.textAlign = Paint.Align.RIGHT
                paint.textSize = 10f
                canvas.drawText("FULL PAID (चुकता)", rightX - 12f, sy + 6f, paint)
            }

            // Left side of summary: Installment History (if any)
            try {
                if (order.paymentHistoryJson.isNotBlank()) {
                    val pArr = org.json.JSONArray(order.paymentHistoryJson)
                    if (pArr.length() > 0) {
                        var histY = summaryTop + 14f
                        paint.textAlign = Paint.Align.LEFT
                        paint.color = accentBlue
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        paint.textSize = 8.5f
                        canvas.drawText("भुगतान किश्त विवरण (Payment History):", MARGIN, histY, paint)

                        paint.typeface = Typeface.DEFAULT
                        paint.textSize = 7.5f
                        paint.color = textDark
                        val maxItems = minOf(pArr.length(), 4)
                        for (i in 0 until maxItems) {
                            histY += 12f
                            val pObj = pArr.getJSONObject(i)
                            val pAmt = pObj.optDouble("amount", 0.0)
                            val pDate = pObj.optString("date", "")
                            val pMode = pObj.optString("mode", "")
                            val pNote = pObj.optString("note", "")
                            val noteSuffix = if (pNote.isNotBlank()) " ($pNote)" else ""
                            canvas.drawText("• ₹${pAmt.toInt()} via $pMode on $pDate$noteSuffix", MARGIN + 4f, histY, paint)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore parsing errors for display
            }
        } else {
            // Price Hidden Quantity Slip Banner
            val bannerRect = RectF(MARGIN, summaryTop, rightX, summaryTop + 40f)
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRoundRect(bannerRect, 6f, 6f, paint)

            paint.color = accentBlue
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("MATERIAL DISPATCH & DELIVERY CHALLAN (PRICES OMITTED AS REQUESTED)", PAGE_WIDTH / 2f, summaryTop + 18f, paint)

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8.5f
            paint.color = textMuted
            val totalBoxesSum = items.sumOf { it.fullBoxes }
            val loosePiecesSum = items.sumOf { it.loosePieces }
            val summaryBoxesStr = "Total Boxes: $totalBoxesSum Bx" + if (loosePiecesSum > 0) " + $loosePiecesSum Loose Pieces" else ""
            canvas.drawText(summaryBoxesStr, PAGE_WIDTH / 2f, summaryTop + 32f, paint)
        }

        // 10. Terms & Signature Block at Bottom
        val footerY = PAGE_HEIGHT - 90f

        paint.color = borderColor
        canvas.drawLine(MARGIN, footerY, rightX, footerY, paint)

        // Terms Notes & UPI on Left
        paint.textAlign = Paint.Align.LEFT
        if (settings.upiId.isNotBlank()) {
            paint.color = accentBlue
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("UPI PAYMENT: ${settings.upiId}", MARGIN, footerY + 12f, paint)
            paint.color = textMuted
            paint.textSize = 7f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TERMS & CONDITIONS:", MARGIN, footerY + 23f, paint)
        } else {
            paint.color = textMuted
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TERMS & CONDITIONS:", MARGIN, footerY + 14f, paint)
        }

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 7f
        val termsLines = settings.termsNotes.lines().take(3)
        var ty = footerY + 26f
        for (line in termsLines) {
            canvas.drawText(line.take(75), MARGIN, ty, paint)
            ty += 11f
        }

        // Authorized Signatory on Right
        val signX = rightX - 60f
        paint.textAlign = Paint.Align.CENTER
        paint.color = textDark
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("For ${settings.shopName.ifBlank { "MOURYA TILES MARBLES" }}", signX, footerY + 44f, paint)

        paint.color = textMuted
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 7.5f
        canvas.drawText("(Authorized Signatory)", signX, footerY + 58f, paint)

        // Bottom accent footer line
        paint.color = navyPrimary
        canvas.drawRect(0f, PAGE_HEIGHT - 6f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)
    }

    private fun loadBitmap(context: Context, uriString: String): Bitmap? {
        return try {
            val isDirectPath = uriString.startsWith("/") || uriString.startsWith("file://")
            val filePath = if (uriString.startsWith("file://")) uriString.removePrefix("file://") else uriString

            if (isDirectPath && File(filePath).exists()) {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(filePath, boundsOpts)
                val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                var sampleSize = 1
                while (maxDim / sampleSize > 800) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeFile(filePath, decodeOpts)
            } else {
                val uri = Uri.parse(uriString)
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOpts)
                }
                val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                var sampleSize = 1
                while (maxDim / sampleSize > 800) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
