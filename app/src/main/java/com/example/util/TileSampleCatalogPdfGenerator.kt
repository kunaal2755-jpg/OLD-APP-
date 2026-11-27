package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.model.ShopSettings
import com.example.data.model.TileProduct
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CatalogPdfConfig(
    val showPhoto: Boolean = true,
    val showCodeAndName: Boolean = true,
    val showSizeAndProportion: Boolean = true,
    val showStockBoxes: Boolean = true,
    val showTotalSqFt: Boolean = true,
    val showPiecesPerBox: Boolean = true,
    val showQualityAndDescription: Boolean = true,
    val showPrice: Boolean = true,
    val selectedCategory: String = "ALL" // "ALL" or specific size like "12x18", "2x4", etc.
)

object TileSampleCatalogPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 30f

    data class TileDimension(
        val widthInches: Float,
        val heightInches: Float,
        val widthFeet: Float,
        val heightFeet: Float,
        val areaSqFtPerPiece: Float,
        val ratioWtoH: Float,
        val displaySize: String
    )

    fun parseTileDimension(sizeStr: String): TileDimension {
        val clean = sizeStr.lowercase(Locale.ENGLISH)
            .replace("inch", "")
            .replace("in", "")
            .replace("ft", "")
            .replace("feet", "")
            .trim()
        val parts = clean.split("x", "×", "*").map { it.trim() }

        var hIn = 12f
        var wIn = 18f

        if (parts.size >= 2) {
            val d1 = parts[0].toFloatOrNull() ?: 12f
            val d2 = parts[1].toFloatOrNull() ?: 18f

            val v1 = if (d1 <= 8f && d2 <= 8f) d1 * 12f else d1
            val v2 = if (d1 <= 8f && d2 <= 8f) d2 * 12f else d2

            // User requirement: 12x18 = 1.0 ft height (unchai) & 1.5 ft width (left to right)
            // 2x4 = 2.0 ft height & 4.0 ft width
            val minDim = minOf(v1, v2)
            val maxDim = maxOf(v1, v2)
            hIn = minDim
            wIn = maxDim
        }

        // Standardize: width is horizontal, height is vertical
        val widthFt = wIn / 12f
        val heightFt = hIn / 12f
        val areaSqFt = widthFt * heightFt
        val ratio = if (hIn > 0) wIn / hIn else 1.5f

        val display = "${hIn.toInt()}\" × ${wIn.toInt()}\" (${String.format(Locale.US, "%.1f", heightFt)} × ${String.format(Locale.US, "%.1f", widthFt)} Ft)"
        return TileDimension(
            widthInches = wIn,
            heightInches = hIn,
            widthFeet = widthFt,
            heightFeet = heightFt,
            areaSqFtPerPiece = areaSqFt,
            ratioWtoH = ratio,
            displaySize = display
        )
    }

    fun generateCatalogPdf(
        context: Context,
        settings: ShopSettings,
        products: List<TileProduct>,
        config: CatalogPdfConfig = CatalogPdfConfig()
    ): File {
        val filteredList = if (config.selectedCategory == "ALL") {
            products
        } else {
            products.filter { it.size.equals(config.selectedCategory, ignoreCase = true) }
        }

        val pdfDocument = PdfDocument()
        val pdfSizeConfig = PdfSizeDisplayConfig.fromJson(settings.pdfSizeDisplayConfigJson)

        // 4 tiles per page (2 columns x 2 rows) allows generous space for large proportional image and complete itemized details
        val itemsPerPage = 4
        val pagesCount = maxOf(1, Math.ceil(filteredList.size / itemsPerPage.toDouble()).toInt())

        val themeOption = com.example.ui.theme.getAppThemeOption(settings.appTheme)
        val primaryColor = Color.rgb(
            (themeOption.primary.red * 255).toInt(),
            (themeOption.primary.green * 255).toInt(),
            (themeOption.primary.blue * 255).toInt()
        )
        val accentColor = Color.rgb(
            (themeOption.accent.red * 255).toInt(),
            (themeOption.accent.green * 255).toInt(),
            (themeOption.accent.blue * 255).toInt()
        )
        val textDark = Color.rgb(30, 41, 59)
        val textMuted = Color.rgb(100, 116, 139)
        val cardBg = Color.rgb(255, 255, 255)
        val pageBg = Color.rgb(248, 250, 252)
        val borderColor = Color.rgb(226, 232, 240)
        val greenSuccess = Color.rgb(22, 163, 74)
        val orangeWarning = Color.rgb(234, 88, 12)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (pageIndex in 0 until pagesCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Page Background
            paint.color = pageBg
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)

            // Top decorative bar
            paint.color = primaryColor
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 10f, paint)

            // Header Section
            val headerTop = 22f
            paint.color = primaryColor
            paint.textSize = 15f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val shopTitle = settings.shopName.ifBlank { "MOURYA TILES & MARBLES" }
            canvas.drawText(shopTitle, MARGIN, headerTop + 14f, paint)

            paint.color = textMuted
            paint.textSize = 8.5f
            paint.typeface = Typeface.DEFAULT
            val subtitle = "${settings.subtitle}  |  फोन: ${settings.ownerPhone.ifBlank { "Contact Shop" }}  |  ${settings.address.ifBlank { "" }}"
            canvas.drawText(subtitle, MARGIN, headerTop + 27f, paint)

            // Catalog Title on right
            paint.textAlign = Paint.Align.RIGHT
            paint.color = primaryColor
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TILE SAMPLE CATALOG", PAGE_WIDTH - MARGIN, headerTop + 14f, paint)

            paint.color = textMuted
            paint.textSize = 8f
            paint.typeface = Typeface.DEFAULT
            val nowStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            canvas.drawText("Date: $nowStr  •  Total Items: ${filteredList.size}", PAGE_WIDTH - MARGIN, headerTop + 27f, paint)

            // Header separator line
            paint.color = borderColor
            paint.strokeWidth = 1f
            canvas.drawLine(MARGIN, headerTop + 34f, PAGE_WIDTH - MARGIN, headerTop + 34f, paint)

            // Grid calculation for items
            val contentTop = headerTop + 42f
            val availableWidth = PAGE_WIDTH - (2 * MARGIN)
            val colGap = 16f
            val rowGap = 16f
            val cardWidth = (availableWidth - colGap) / 2f
            val cardHeight = 350f // Generous height for proportional preview + 8 lines of clear specifications

            val startIndex = pageIndex * itemsPerPage
            val endIndex = minOf(startIndex + itemsPerPage, filteredList.size)

            for (idx in startIndex until endIndex) {
                val itemOffset = idx - startIndex
                val col = itemOffset % 2
                val row = itemOffset / 2

                val cardLeft = MARGIN + col * (cardWidth + colGap)
                val cardTop = contentTop + row * (cardHeight + rowGap)
                val cardRight = cardLeft + cardWidth
                val cardBottom = cardTop + cardHeight

                val product = filteredList[idx]
                val resolved = PdfSizeDisplayConfig.resolveForTile(product.size, pdfSizeConfig)

                // Draw Tile Card
                paint.style = Paint.Style.FILL
                paint.color = cardBg
                canvas.drawRoundRect(RectF(cardLeft, cardTop, cardRight, cardBottom), 8f, 8f, paint)

                paint.style = Paint.Style.STROKE
                paint.color = borderColor
                paint.strokeWidth = 1f
                canvas.drawRoundRect(RectF(cardLeft, cardTop, cardRight, cardBottom), 8f, 8f, paint)
                paint.style = Paint.Style.FILL

                // Proportional Tile Image Box respecting user's custom PDF Size Display rules!
                // Maximum image area inside card: width = cardWidth - 20, height = 135
                val imgAreaLeft = cardLeft + 10f
                val imgAreaTop = cardTop + 10f
                val imgAreaWidth = cardWidth - 20f
                val imgAreaHeight = 135f

                // Draw tile preview area background panel
                paint.color = Color.rgb(241, 245, 249)
                canvas.drawRoundRect(RectF(imgAreaLeft, imgAreaTop, imgAreaLeft + imgAreaWidth, imgAreaTop + imgAreaHeight), 6f, 6f, paint)

                val maxAllowedW = imgAreaWidth - 8f
                val maxAllowedH = imgAreaHeight - 8f
                val ratio = resolved.renderAspectRatioWtoH.coerceIn(0.35f, 3.0f)
                val targetW = maxAllowedW * resolved.scaleFactor
                val targetH = maxAllowedH * resolved.scaleFactor

                var renderW: Float
                var renderH: Float
                if (targetW / targetH > ratio) {
                    renderH = targetH
                    renderW = renderH * ratio
                } else {
                    renderW = targetW
                    renderH = renderW / ratio
                }

                // Final safety bounds
                if (renderW > maxAllowedW) {
                    val s = maxAllowedW / renderW
                    renderW *= s
                    renderH *= s
                }
                if (renderH > maxAllowedH) {
                    val s = maxAllowedH / renderH
                    renderW *= s
                    renderH *= s
                }
                renderW = renderW.coerceAtLeast(36f)
                renderH = renderH.coerceAtLeast(36f)

                // Center within image area
                val renderLeft = imgAreaLeft + (imgAreaWidth - renderW) / 2f
                val renderTop = imgAreaTop + (imgAreaHeight - renderH) / 2f
                val renderRect = RectF(renderLeft, renderTop, renderLeft + renderW, renderTop + renderH)

                // Draw tile drop shadow
                paint.color = Color.rgb(203, 213, 225)
                canvas.drawRoundRect(RectF(renderLeft + 2f, renderTop + 2f, renderLeft + renderW + 2f, renderTop + renderH + 2f), 4f, 4f, paint)

                // Draw Tile Image or Realistic Vector Pattern
                var photoDrawn = false
                if (config.showPhoto && !product.photoUri.isNullOrBlank()) {
                    val bitmap = loadBitmap(context, product.photoUri)
                    if (bitmap != null) {
                        val srcRect = if (resolved.imageFitMode == PdfImageScaleMode.CENTER_CROP) {
                            PdfSizeDisplayConfig.computeCenterCropSrcRect(bitmap.width, bitmap.height, renderW, renderH)
                        } else {
                            null
                        }
                        canvas.drawBitmap(bitmap, srcRect, renderRect, paint)
                        photoDrawn = true
                    }
                }

                if (!photoDrawn) {
                    // Draw realistic tile slab texture
                    paint.color = Color.rgb(238, 242, 246)
                    canvas.drawRoundRect(renderRect, 6f, 6f, paint)

                    // Draw subtle grid texture mimicking real porcelain glaze
                    paint.color = Color.rgb(203, 213, 225)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.8f
                    canvas.drawRoundRect(renderRect, 6f, 6f, paint)

                    // Diagonal light reflection sheen
                    val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            renderLeft, renderTop,
                            renderLeft + renderW, renderTop + renderH,
                            Color.argb(80, 255, 255, 255),
                            Color.argb(0, 255, 255, 255),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRoundRect(renderRect, 6f, 6f, sheenPaint)

                    // Dimension watermark on the tile
                    paint.style = Paint.Style.FILL
                    paint.textAlign = Paint.Align.CENTER
                    paint.color = Color.rgb(71, 85, 105)
                    paint.textSize = if (renderW < 75f) 7.5f else 9.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(resolved.sizeLabelForPdf, renderRect.centerX(), renderRect.centerY() + 3f, paint)
                }

                // Border around tile image
                paint.style = Paint.Style.STROKE
                paint.color = Color.rgb(148, 163, 184)
                paint.strokeWidth = 1f
                canvas.drawRoundRect(renderRect, 6f, 6f, paint)
                paint.style = Paint.Style.FILL

                // Dimension Badge at bottom of image (configurable per size in Settings)
                if (resolved.showDimensionBadge && config.showSizeAndProportion) {
                    paint.textAlign = Paint.Align.CENTER
                    paint.textSize = 6.8f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val badgeText = resolved.badgeTextForCatalog
                    val measuredW = paint.measureText(badgeText) + 12f
                    val badgeW = measuredW.coerceIn(64f, imgAreaWidth - 10f)
                    val badgeH = 14f
                    val badgeCenterX = renderRect.centerX().coerceIn(imgAreaLeft + badgeW / 2f + 2f, imgAreaLeft + imgAreaWidth - badgeW / 2f - 2f)
                    val badgeRect = RectF(badgeCenterX - badgeW / 2f, renderRect.bottom - badgeH - 3f, badgeCenterX + badgeW / 2f, renderRect.bottom - 3f)
                    paint.color = Color.argb(220, 15, 23, 42)
                    canvas.drawRoundRect(badgeRect, 3f, 3f, paint)

                    paint.color = Color.WHITE
                    canvas.drawText(badgeText, badgeCenterX, badgeRect.centerY() + 2.5f, paint)
                }

                // Tile Details Section under image (User requested: Code, name, quality, kitni bachi hai, kitne feet hogi, ek box me kitni tile)
                var textY = imgAreaTop + imgAreaHeight + 14f
                paint.textAlign = Paint.Align.LEFT

                // 1. Code & Name
                if (config.showCodeAndName) {
                    paint.color = primaryColor
                    paint.textSize = 10.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("[${product.code}] ${product.name}", cardLeft + 10f, textY, paint)
                    textY += 13f
                }

                // 2. Size & Quality
                if (config.showQualityAndDescription) {
                    paint.color = textDark
                    paint.textSize = 8.5f
                    paint.typeface = Typeface.DEFAULT
                    val desc = product.description.ifBlank { "Premium Glazed Vitrified Finish" }
                    val qualText = "क्वालिटी: $desc"
                    canvas.drawText(qualText.take(42), cardLeft + 10f, textY, paint)
                    textY += 12f
                }

                // Divider line inside card
                paint.color = borderColor
                paint.strokeWidth = 0.6f
                canvas.drawLine(cardLeft + 10f, textY, cardRight - 10f, textY, paint)
                textY += 10f

                // 3. Pieces per Box & Size
                if (config.showPiecesPerBox) {
                    paint.color = textMuted
                    paint.textSize = 8f
                    val sizeLine = if (resolved.sizeLabelForPdf == resolved.detailSubLabel) {
                        "साइज़: ${resolved.sizeLabelForPdf}"
                    } else {
                        "साइज़: ${resolved.sizeLabelForPdf} (${resolved.detailSubLabel})"
                    }
                    canvas.drawText(sizeLine.take(44), cardLeft + 10f, textY, paint)
                    textY += 11f

                    paint.color = textDark
                    paint.textSize = 8.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("पैकिंग: ${product.piecesPerBox} टाइल्स / बॉक्स (${String.format(Locale.US, "%.2f", product.piecesPerBox * resolved.areaSqFtPerPiece)} Sq.Ft/Box)", cardLeft + 10f, textY, paint)
                    paint.typeface = Typeface.DEFAULT
                    textY += 12f
                }

                // 4. Available Stock (Boxes + Loose pieces)
                if (config.showStockBoxes) {
                    val stockColor = if (product.isLowStock) orangeWarning else greenSuccess
                    paint.color = stockColor
                    paint.textSize = 9.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val stockText = "बचा स्टॉक: ${product.fullBoxes} बॉक्स + ${product.loosePieces} पीस (${product.stockPieces} कुल टाइल्स)"
                    canvas.drawText(stockText, cardLeft + 10f, textY, paint)
                    paint.typeface = Typeface.DEFAULT
                    textY += 12f
                }

                // 5. Total Available Area in Square Feet
                if (config.showTotalSqFt) {
                    val totalSqFt = product.stockPieces * resolved.areaSqFtPerPiece
                    paint.color = primaryColor
                    paint.textSize = 9f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("कुल स्टॉक एरिया: ${String.format(Locale.US, "%,.1f", totalSqFt)} स्क्वायर फीट", cardLeft + 10f, textY, paint)
                    paint.typeface = Typeface.DEFAULT
                    textY += 12f
                }

                // 6. Price per Box & per Sq. Ft (if toggled)
                if (config.showPrice && product.defaultSellingPrice > 0) {
                    paint.color = textDark
                    paint.textSize = 9f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val sqFtRate = if (product.piecesPerBox > 0 && resolved.areaSqFtPerPiece > 0) {
                        product.defaultSellingPrice / (product.piecesPerBox * resolved.areaSqFtPerPiece)
                    } else 0.0
                    val priceText = "बिक्री मूल्य: ₹${String.format(Locale.US, "%,.0f", product.defaultSellingPrice)} / बॉक्स (₹${String.format(Locale.US, "%.1f", sqFtRate)}/sq.ft)"
                    canvas.drawText(priceText, cardLeft + 10f, textY, paint)
                    paint.typeface = Typeface.DEFAULT
                }
            }

            // Page Footer
            paint.textAlign = Paint.Align.CENTER
            paint.color = textMuted
            paint.textSize = 8f
            val footerText = "Page ${pageIndex + 1} of $pagesCount  •  ${settings.shopName.ifBlank { "Mourya Tiles" }} Sample Directory  •  Powered by AI Studio"
            canvas.drawText(footerText, PAGE_WIDTH / 2f, PAGE_HEIGHT - 16f, paint)

            pdfDocument.finishPage(page)
        }

        // Write to cache directory
        val outputDir = File(context.cacheDir, "tile_catalogs")
        if (!outputDir.exists()) outputDir.mkdirs()

        val timeTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanShop = settings.shopName.replace("[^a-zA-Z0-9]".toRegex(), "_").ifBlank { "MouryaTiles" }
        val outputFile = File(outputDir, "TileCatalog_${cleanShop}_${timeTag}.pdf")

        FileOutputStream(outputFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        return outputFile
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
