package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
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
    val selectedCategory: String = "ALL", // "ALL" or specific size like "12x18", "2x4", etc.
    val itemsPerPage: Int = 4, // 2 (Large), 4 (Standard), 6 (Compact)
    val imageHeightPercent: Int = 100, // 60..150 % image area height multiplier
    val imageWidthPercent: Int = 100, // 60..120 % image render width multiplier
    val imageFitOverride: String = "RULE" // "RULE", "FIT_BOX", "CENTER_CROP"
)

object TileSampleCatalogPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 28f

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
            val d1 = (parts[0].toFloatOrNull() ?: 12f).coerceIn(1f, 200f)
            val d2 = (parts[1].toFloatOrNull() ?: 18f).coerceIn(1f, 200f)

            val v1 = if (d1 <= 8f && d2 <= 8f) d1 * 12f else d1
            val v2 = if (d1 <= 8f && d2 <= 8f) d2 * 12f else d2

            val minDim = minOf(v1, v2)
            val maxDim = maxOf(v1, v2)
            hIn = minDim.coerceAtLeast(1f)
            wIn = maxDim.coerceAtLeast(1f)
        }

        val widthFt = wIn / 12f
        val heightFt = hIn / 12f
        val areaSqFt = (widthFt * heightFt).coerceAtLeast(0.01f)
        val ratio = if (hIn > 0f) (wIn / hIn).coerceIn(0.35f, 3.0f) else 1.5f

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

        val pdfSizeConfig = PdfSizeDisplayConfig.fromJson(settings.pdfSizeDisplayConfigJson)

        val safeItemsPerPage = when (config.itemsPerPage) {
            2 -> 2
            6 -> 6
            else -> 4
        }
        val columns = if (safeItemsPerPage == 2) 1 else 2
        val rows = safeItemsPerPage / columns
        val maxDecodeDim = when (safeItemsPerPage) {
            2 -> 520
            6 -> 320
            else -> 400
        }

        val pagesCount = maxOf(1, Math.ceil(filteredList.size / safeItemsPerPage.toDouble()).toInt())

        val themeOption = com.example.ui.theme.getAppThemeOption(settings.appTheme)
        val primaryColor = Color.rgb(
            (themeOption.primary.red * 255).toInt().coerceIn(0, 255),
            (themeOption.primary.green * 255).toInt().coerceIn(0, 255),
            (themeOption.primary.blue * 255).toInt().coerceIn(0, 255)
        )
        val textDark = Color.rgb(30, 41, 59)
        val textMuted = Color.rgb(100, 116, 139)
        val cardBg = Color.rgb(255, 255, 255)
        val pageBg = Color.rgb(248, 250, 252)
        val borderColor = Color.rgb(226, 232, 240)
        val greenSuccess = Color.rgb(22, 163, 74)
        val orangeWarning = Color.rgb(234, 88, 12)

        val pdfDocument = PdfDocument()
        try {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val heightMult = (config.imageHeightPercent.coerceIn(60, 150) / 100f)
            val widthMult = (config.imageWidthPercent.coerceIn(60, 120) / 100f)

            for (pageIndex in 0 until pagesCount) {
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Page Background
                paint.style = Paint.Style.FILL
                paint.textAlign = Paint.Align.LEFT
                paint.color = pageBg
                canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)

                // Top decorative bar
                paint.color = primaryColor
                canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 10f, paint)

                // Header Section
                val headerTop = 20f
                paint.color = primaryColor
                paint.textSize = 14.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val shopTitle = settings.shopName.ifBlank { "MOURYA TILES & MARBLES" }.take(34)
                canvas.drawText(shopTitle, MARGIN, headerTop + 14f, paint)

                paint.color = textMuted
                paint.textSize = 8.2f
                paint.typeface = Typeface.DEFAULT
                val subtitle = "${settings.subtitle}  |  फोन: ${settings.ownerPhone.ifBlank { "Contact Shop" }}  |  ${settings.address}"
                canvas.drawText(subtitle.take(66), MARGIN, headerTop + 26f, paint)

                // Catalog Title on right
                paint.textAlign = Paint.Align.RIGHT
                paint.color = primaryColor
                paint.textSize = 11.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("TILE SAMPLE CATALOG", PAGE_WIDTH - MARGIN, headerTop + 14f, paint)

                paint.color = textMuted
                paint.textSize = 8f
                paint.typeface = Typeface.DEFAULT
                val nowStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                canvas.drawText("Date: $nowStr  •  Items: ${filteredList.size}", PAGE_WIDTH - MARGIN, headerTop + 26f, paint)

                // Header separator line
                paint.color = borderColor
                paint.strokeWidth = 1f
                canvas.drawLine(MARGIN, headerTop + 32f, PAGE_WIDTH - MARGIN, headerTop + 32f, paint)

                if (filteredList.isEmpty()) {
                    paint.textAlign = Paint.Align.CENTER
                    paint.color = textMuted
                    paint.textSize = 13f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("चुनी गई श्रेणी में कोई टाइल उपलब्ध नहीं है", PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f, paint)
                } else {
                    val contentTop = headerTop + 40f
                    val contentBottom = PAGE_HEIGHT - 28f
                    val availableWidth = PAGE_WIDTH - (2 * MARGIN)
                    val availableHeight = contentBottom - contentTop
                    val colGap = if (columns == 1) 0f else 14f
                    val rowGap = if (rows >= 3) 10f else 14f

                    val cardWidth = (availableWidth - colGap * (columns - 1)) / columns
                    val cardHeight = (availableHeight - rowGap * (rows - 1)) / rows

                    val startIndex = pageIndex * safeItemsPerPage
                    val endIndex = minOf(startIndex + safeItemsPerPage, filteredList.size)

                    for (idx in startIndex until endIndex) {
                        try {
                            val itemOffset = idx - startIndex
                            val col = itemOffset % columns
                            val row = itemOffset / columns

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

                            val isCompact = safeItemsPerPage == 6
                            val isLarge = safeItemsPerPage == 2
                            val pad = if (isCompact) 7f else 10f

                            // Calculate dynamic image box area respecting user height slider while guaranteeing room for details
                            val minDetailsHeight = if (isCompact) 86f else if (isLarge) 108f else 104f
                            val baseImgHeight = when (safeItemsPerPage) {
                                2 -> 185f
                                6 -> 104f
                                else -> 142f
                            }
                            val maxPossibleImgHeight = (cardHeight - minDetailsHeight - pad * 2f).coerceAtLeast(64f)
                            val imgAreaHeight = (baseImgHeight * heightMult).coerceIn(64f, maxPossibleImgHeight)
                            val imgAreaLeft = cardLeft + pad
                            val imgAreaTop = cardTop + pad
                            val imgAreaWidth = (cardWidth - pad * 2f).coerceAtLeast(60f)

                            if (config.showPhoto) {
                                // Image panel background
                                paint.color = Color.rgb(241, 245, 249)
                                canvas.drawRoundRect(
                                    RectF(imgAreaLeft, imgAreaTop, imgAreaLeft + imgAreaWidth, imgAreaTop + imgAreaHeight),
                                    6f,
                                    6f,
                                    paint
                                )

                                val maxAllowedW = (imgAreaWidth - 8f).coerceAtLeast(36f)
                                val maxAllowedH = (imgAreaHeight - 8f).coerceAtLeast(36f)
                                val ratio = resolved.renderAspectRatioWtoH.coerceIn(0.35f, 3.0f)
                                val effectiveScale = (resolved.scaleFactor * widthMult).coerceIn(0.40f, 1.0f)
                                val targetW = maxAllowedW * effectiveScale
                                val targetH = maxAllowedH * effectiveScale

                                var renderW: Float
                                var renderH: Float
                                if (targetW / targetH > ratio) {
                                    renderH = targetH
                                    renderW = renderH * ratio
                                } else {
                                    renderW = targetW
                                    renderH = renderW / ratio
                                }

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
                                renderW = renderW.coerceIn(32f, maxAllowedW)
                                renderH = renderH.coerceIn(32f, maxAllowedH)

                                val renderLeft = imgAreaLeft + (imgAreaWidth - renderW) / 2f
                                val renderTop = imgAreaTop + (imgAreaHeight - renderH) / 2f
                                val renderRect = RectF(renderLeft, renderTop, renderLeft + renderW, renderTop + renderH)

                                // Subtle drop shadow
                                paint.color = Color.rgb(203, 213, 225)
                                canvas.drawRoundRect(
                                    RectF(renderLeft + 2f, renderTop + 2f, renderLeft + renderW + 2f, renderTop + renderH + 2f),
                                    4f,
                                    4f,
                                    paint
                                )

                                // Resolve fit mode (override or per-size rule)
                                val effectiveFitMode = when (config.imageFitOverride) {
                                    "FIT_BOX" -> PdfImageScaleMode.FIT_BOX
                                    "CENTER_CROP" -> PdfImageScaleMode.CENTER_CROP
                                    else -> resolved.imageFitMode
                                }

                                // Draw Tile Image with guaranteed immediate bitmap.recycle() to prevent OOM crash!
                                var photoDrawn = false
                                if (!product.photoUri.isNullOrBlank()) {
                                    val bitmap = loadBitmapSafely(context, product.photoUri, maxDecodeDim)
                                    if (bitmap != null) {
                                        try {
                                            if (!bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0) {
                                                val srcRect: Rect? = if (effectiveFitMode == PdfImageScaleMode.CENTER_CROP) {
                                                    PdfSizeDisplayConfig.computeCenterCropSrcRect(
                                                        bitmap.width,
                                                        bitmap.height,
                                                        renderW,
                                                        renderH
                                                    )
                                                } else {
                                                    null
                                                }
                                                canvas.drawBitmap(bitmap, srcRect, renderRect, paint)
                                                photoDrawn = true
                                            }
                                        } catch (_: Throwable) {
                                            photoDrawn = false
                                        } finally {
                                            try {
                                                if (!bitmap.isRecycled) {
                                                    bitmap.recycle()
                                                }
                                            } catch (_: Throwable) {}
                                        }
                                    }
                                }

                                if (!photoDrawn) {
                                    paint.color = Color.rgb(238, 242, 246)
                                    canvas.drawRoundRect(renderRect, 6f, 6f, paint)

                                    paint.color = Color.rgb(203, 213, 225)
                                    paint.style = Paint.Style.STROKE
                                    paint.strokeWidth = 0.8f
                                    canvas.drawRoundRect(renderRect, 6f, 6f, paint)

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

                                    paint.style = Paint.Style.FILL
                                    paint.textAlign = Paint.Align.CENTER
                                    paint.color = Color.rgb(71, 85, 105)
                                    paint.textSize = if (renderW < 75f) 7.5f else 9.5f
                                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                    canvas.drawText(resolved.sizeLabelForPdf.take(18), renderRect.centerX(), renderRect.centerY() + 3f, paint)
                                }

                                // Border around tile image
                                paint.style = Paint.Style.STROKE
                                paint.color = Color.rgb(148, 163, 184)
                                paint.strokeWidth = 1f
                                canvas.drawRoundRect(renderRect, 6f, 6f, paint)
                                paint.style = Paint.Style.FILL

                                // Dimension Badge at bottom of image
                                if (resolved.showDimensionBadge && config.showSizeAndProportion) {
                                    paint.textAlign = Paint.Align.CENTER
                                    paint.textSize = if (isCompact) 6.2f else 6.8f
                                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                    val badgeText = resolved.badgeTextForCatalog.take(36)
                                    val measuredW = paint.measureText(badgeText) + 10f
                                    val badgeW = measuredW.coerceIn(54f, imgAreaWidth - 8f)
                                    val badgeH = if (isCompact) 12f else 14f
                                    val minCX = imgAreaLeft + badgeW / 2f + 2f
                                    val maxCX = imgAreaLeft + imgAreaWidth - badgeW / 2f - 2f
                                    val badgeCenterX = if (minCX <= maxCX) {
                                        renderRect.centerX().coerceIn(minCX, maxCX)
                                    } else {
                                        imgAreaLeft + imgAreaWidth / 2f
                                    }
                                    val badgeRect = RectF(
                                        badgeCenterX - badgeW / 2f,
                                        renderRect.bottom - badgeH - 3f,
                                        badgeCenterX + badgeW / 2f,
                                        renderRect.bottom - 3f
                                    )
                                    paint.color = Color.argb(220, 15, 23, 42)
                                    canvas.drawRoundRect(badgeRect, 3f, 3f, paint)

                                    paint.color = Color.WHITE
                                    canvas.drawText(badgeText, badgeCenterX, badgeRect.centerY() + 2.3f, paint)
                                }
                            }

                            // Tile Details Section under image
                            var textY = if (config.showPhoto) {
                                imgAreaTop + imgAreaHeight + (if (isCompact) 11f else 14f)
                            } else {
                                cardTop + 18f
                            }
                            val maxTextY = cardBottom - 6f
                            val lineStep = if (isCompact) 10.5f else if (isLarge) 14f else 12.2f
                            val maxChars = if (isLarge) 76 else if (isCompact) 38 else 42
                            paint.textAlign = Paint.Align.LEFT

                            // 1. Code & Name
                            if (config.showCodeAndName && textY <= maxTextY) {
                                paint.color = primaryColor
                                paint.textSize = if (isCompact) 9.2f else if (isLarge) 12f else 10.2f
                                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                canvas.drawText("[${product.code}] ${product.name}".take(maxChars), cardLeft + pad, textY, paint)
                                textY += lineStep + 1f
                            }

                            // 2. Quality, Description & Location
                            if (config.showQualityAndDescription && textY <= maxTextY) {
                                paint.color = textDark
                                paint.textSize = if (isCompact) 7.5f else if (isLarge) 9.5f else 8.3f
                                paint.typeface = Typeface.DEFAULT
                                val desc = product.description.ifBlank { "Premium Glazed Vitrified Finish" }
                                val locSuffix = if (product.location.isNotBlank()) " | स्थान: ${product.location}" else ""
                                canvas.drawText("क्वालिटी: $desc$locSuffix".take(maxChars + 6), cardLeft + pad, textY, paint)
                                textY += lineStep
                            }

                            // Divider line inside card
                            if (textY + 4f <= maxTextY) {
                                paint.color = borderColor
                                paint.strokeWidth = 0.6f
                                canvas.drawLine(cardLeft + pad, textY - 3f, cardRight - pad, textY - 3f, paint)
                                textY += 7f
                            }

                            // 3. Size & Pieces per Box
                            if (config.showSizeAndProportion && textY <= maxTextY) {
                                paint.color = textMuted
                                paint.textSize = if (isCompact) 7.2f else 8f
                                paint.typeface = Typeface.DEFAULT
                                val sizeLine = if (resolved.sizeLabelForPdf == resolved.detailSubLabel) {
                                    "साइज़: ${resolved.sizeLabelForPdf}"
                                } else {
                                    "साइज़: ${resolved.sizeLabelForPdf} (${resolved.detailSubLabel})"
                                }
                                canvas.drawText(sizeLine.take(maxChars + 6), cardLeft + pad, textY, paint)
                                textY += lineStep - 1f
                            }

                            if (config.showPiecesPerBox && textY <= maxTextY) {
                                paint.color = textDark
                                paint.textSize = if (isCompact) 7.5f else 8.4f
                                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                val safePcs = product.piecesPerBox.coerceAtLeast(1)
                                val boxSqFt = safePcs * resolved.areaSqFtPerPiece
                                canvas.drawText(
                                    "पैकिंग: $safePcs टाइल्स / बॉक्स (${String.format(Locale.US, "%.2f", boxSqFt)} Sq.Ft/Box)".take(maxChars + 6),
                                    cardLeft + pad,
                                    textY,
                                    paint
                                )
                                paint.typeface = Typeface.DEFAULT
                                textY += lineStep
                            }

                            // 4. Available Stock
                            if (config.showStockBoxes && textY <= maxTextY) {
                                paint.color = if (product.isLowStock) orangeWarning else greenSuccess
                                paint.textSize = if (isCompact) 8f else 9.2f
                                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                val stockText = "बचा स्टॉक: ${product.fullBoxes} बॉक्स + ${product.loosePieces} पीस (${product.stockPieces} टाइल्स)"
                                canvas.drawText(stockText.take(maxChars + 6), cardLeft + pad, textY, paint)
                                paint.typeface = Typeface.DEFAULT
                                textY += lineStep
                            }

                            // 5. Total Available Area in Square Feet
                            if (config.showTotalSqFt && textY <= maxTextY) {
                                val totalSqFt = product.stockPieces * resolved.areaSqFtPerPiece
                                paint.color = primaryColor
                                paint.textSize = if (isCompact) 7.8f else 8.8f
                                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                canvas.drawText(
                                    "कुल स्टॉक एरिया: ${String.format(Locale.US, "%,.1f", totalSqFt)} स्क्वायर फीट".take(maxChars + 4),
                                    cardLeft + pad,
                                    textY,
                                    paint
                                )
                                paint.typeface = Typeface.DEFAULT
                                textY += lineStep
                            }

                            // 6. Price per Box & per Sq. Ft
                            if (config.showPrice && product.defaultSellingPrice > 0 && textY <= maxTextY) {
                                paint.color = textDark
                                paint.textSize = if (isCompact) 7.8f else 8.8f
                                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                val sqFtRate = if (product.piecesPerBox > 0 && resolved.areaSqFtPerPiece > 0f) {
                                    product.defaultSellingPrice / (product.piecesPerBox * resolved.areaSqFtPerPiece)
                                } else 0.0
                                val priceText = "बिक्री मूल्य: ₹${String.format(Locale.US, "%,.0f", product.defaultSellingPrice)} / बॉक्स (₹${String.format(Locale.US, "%.1f", sqFtRate)}/sq.ft)"
                                canvas.drawText(priceText.take(maxChars + 6), cardLeft + pad, textY, paint)
                                paint.typeface = Typeface.DEFAULT
                            }
                        } catch (_: Throwable) {
                            // Never let a single product tile crash the entire catalog PDF
                        }
                    }
                }

                // Page Footer
                paint.textAlign = Paint.Align.CENTER
                paint.color = textMuted
                paint.textSize = 7.8f
                paint.typeface = Typeface.DEFAULT
                val footerText = "Page ${pageIndex + 1} of $pagesCount  •  ${settings.shopName.ifBlank { "TILES AND MARBLES" }} Sample Catalog"
                canvas.drawText(footerText, PAGE_WIDTH / 2f, PAGE_HEIGHT - 12f, paint)

                pdfDocument.finishPage(page)
            }

            val outputDir = File(context.cacheDir, "tile_catalogs")
            if (!outputDir.exists()) outputDir.mkdirs()

            // Clean up old cached catalog PDFs (> 10 files) so cache doesn't bloat
            try {
                val existingFiles = outputDir.listFiles()?.sortedByDescending { it.lastModified() }.orEmpty()
                if (existingFiles.size > 10) {
                    existingFiles.drop(10).forEach { it.delete() }
                }
            } catch (_: Throwable) {}

            val timeTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val cleanShop = settings.shopName.replace("[^a-zA-Z0-9]".toRegex(), "_").ifBlank { "TilesAndMarbles" }
            val outputFile = File(outputDir, "TileCatalog_${cleanShop}_${timeTag}.pdf")

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
                fos.flush()
            }

            return outputFile
        } finally {
            try {
                pdfDocument.close()
            } catch (_: Throwable) {}
        }
    }

    private fun loadBitmapSafely(context: Context, uriString: String, maxDim: Int): Bitmap? {
        return try {
            val trimmed = uriString.trim()
            if (trimmed.isEmpty()) return null
            val isDirectPath = trimmed.startsWith("/") || trimmed.startsWith("file://")
            val filePath = if (trimmed.startsWith("file://")) trimmed.removePrefix("file://") else trimmed

            val decoded: Bitmap? = if (isDirectPath) {
                val f = File(filePath)
                if (!f.exists() || !f.canRead() || f.length() == 0L) return null
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(filePath, boundsOpts)
                val origMax = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                if (origMax <= 0) return null
                var sampleSize = 1
                while (origMax / sampleSize > maxDim) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeFile(filePath, decodeOpts)
            } else {
                val uri = Uri.parse(trimmed)
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOpts)
                }
                val origMax = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                if (origMax <= 0) return null
                var sampleSize = 1
                while (origMax / sampleSize > maxDim) {
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

            if (decoded == null || decoded.isRecycled) return null

            // Cap final bitmap size so PDF file size stays compact and RAM stays minimal
            val curMax = maxOf(decoded.width, decoded.height)
            if (curMax > maxDim && curMax > 0) {
                val scale = maxDim.toFloat() / curMax.toFloat()
                val targetW = (decoded.width * scale).toInt().coerceAtLeast(1)
                val targetH = (decoded.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
                if (scaled !== decoded && !decoded.isRecycled) {
                    decoded.recycle()
                }
                scaled
            } else {
                decoded
            }
        } catch (_: Throwable) {
            null
        }
    }
}
