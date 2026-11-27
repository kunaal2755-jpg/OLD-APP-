package com.example.util

import android.graphics.Rect
import org.json.JSONObject
import java.util.Locale

enum class PdfTileOrientation(val key: String, val labelHi: String, val shortLabel: String) {
    LANDSCAPE("LANDSCAPE", "आड़ा (Horizontal)", "↔️ आड़ा"),
    PORTRAIT("PORTRAIT", "खड़ा (Vertical)", "↕️ खड़ा"),
    SQUARE("SQUARE", "चौकोर (1:1)", "⬜ चौकोर"),
    CUSTOM("CUSTOM", "कस्टम अनुपात (W:H)", "📐 कस्टम");

    companion object {
        fun fromKey(key: String?): PdfTileOrientation {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: LANDSCAPE
        }
    }
}

enum class PdfImageScaleMode(val key: String, val labelHi: String) {
    FIT_BOX("FIT_BOX", "पूरा फिट (Exact Box)"),
    CENTER_CROP("CENTER_CROP", "क्रॉप फिट (No Stretch)");

    companion object {
        fun fromKey(key: String?): PdfImageScaleMode {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: FIT_BOX
        }
    }
}

enum class PdfSizeTextMode(val key: String, val labelHi: String) {
    DEFAULT("DEFAULT", "ऑटो (Auto)"),
    CUSTOM_LABEL("CUSTOM_LABEL", "कस्टम टेक्स्ट"),
    FEET_ONLY("FEET_ONLY", "सिर्फ फीट (Ft)"),
    INCHES_ONLY("INCHES_ONLY", "सिर्फ इंच (Inch)"),
    BOTH("BOTH", "इंच + फीट दोनों");

    companion object {
        fun fromKey(key: String?): PdfSizeTextMode {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: DEFAULT
        }
    }
}

enum class PdfGlobalSizeCase(val key: String, val label: String) {
    AS_IS("AS_IS", "जैसा है"),
    UPPERCASE("UPPERCASE", "2X4"),
    LOWERCASE("LOWERCASE", "2x4"),
    MULTIPLY_SIGN("MULTIPLY_SIGN", "2×4");

    companion object {
        fun fromKey(key: String?): PdfGlobalSizeCase {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: AS_IS
        }
    }
}

data class PdfSizeDisplayRule(
    val sizeKey: String,
    val customDisplayLabel: String = "",
    val orientation: String = PdfTileOrientation.LANDSCAPE.key,
    val customWidthRatio: Float = 0f,
    val customHeightRatio: Float = 0f,
    val boxScalePercent: Int = 0, // 0 = Auto based on physical size, else 40..100
    val imageFitMode: String = PdfImageScaleMode.FIT_BOX.key,
    val textMode: String = PdfSizeTextMode.DEFAULT.key,
    val showDimensionBadge: Boolean = true
)

data class PdfSizeDisplayConfig(
    val globalSizeCase: String = PdfGlobalSizeCase.AS_IS.key,
    val rules: Map<String, PdfSizeDisplayRule> = emptyMap()
) {
    fun getRuleForSize(rawSize: String): PdfSizeDisplayRule? {
        val norm = normalizeSizeKey(rawSize)
        return rules[norm]
    }

    fun withRule(rule: PdfSizeDisplayRule): PdfSizeDisplayConfig {
        val norm = normalizeSizeKey(rule.sizeKey)
        val updated = rules.toMutableMap()
        updated[norm] = rule.copy(sizeKey = norm)
        return copy(rules = updated)
    }

    fun withoutRule(rawSize: String): PdfSizeDisplayConfig {
        val norm = normalizeSizeKey(rawSize)
        val updated = rules.toMutableMap()
        updated.remove(norm)
        return copy(rules = updated)
    }

    fun toJson(): String {
        val root = JSONObject()
        root.put("globalSizeCase", globalSizeCase)
        val rulesObj = JSONObject()
        for ((key, rule) in rules) {
            val ro = JSONObject().apply {
                put("sizeKey", rule.sizeKey)
                put("customDisplayLabel", rule.customDisplayLabel)
                put("orientation", rule.orientation)
                put("customWidthRatio", rule.customWidthRatio.toDouble())
                put("customHeightRatio", rule.customHeightRatio.toDouble())
                put("boxScalePercent", rule.boxScalePercent)
                put("imageFitMode", rule.imageFitMode)
                put("textMode", rule.textMode)
                put("showDimensionBadge", rule.showDimensionBadge)
            }
            rulesObj.put(key, ro)
        }
        root.put("rules", rulesObj)
        return root.toString()
    }

    companion object {
        fun normalizeSizeKey(rawSize: String): String {
            return rawSize.trim()
                .lowercase(Locale.ENGLISH)
                .replace("×", "x")
                .replace("*", "x")
                .replace("\\s*x\\s*".toRegex(), "x")
                .replace("\\s+".toRegex(), " ")
        }

        fun fromJson(json: String?): PdfSizeDisplayConfig {
            if (json.isNullOrBlank()) return PdfSizeDisplayConfig()
            return try {
                val root = JSONObject(json)
                val globalCase = root.optString("globalSizeCase", PdfGlobalSizeCase.AS_IS.key)
                val rulesMap = mutableMapOf<String, PdfSizeDisplayRule>()
                val rulesObj = root.optJSONObject("rules")
                if (rulesObj != null) {
                    val keys = rulesObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val ro = rulesObj.optJSONObject(key) ?: continue
                        val normKey = normalizeSizeKey(ro.optString("sizeKey", key))
                        rulesMap[normKey] = PdfSizeDisplayRule(
                            sizeKey = normKey,
                            customDisplayLabel = ro.optString("customDisplayLabel", ""),
                            orientation = ro.optString("orientation", PdfTileOrientation.LANDSCAPE.key),
                            customWidthRatio = ro.optDouble("customWidthRatio", 0.0).toFloat(),
                            customHeightRatio = ro.optDouble("customHeightRatio", 0.0).toFloat(),
                            boxScalePercent = ro.optInt("boxScalePercent", 0),
                            imageFitMode = ro.optString("imageFitMode", PdfImageScaleMode.FIT_BOX.key),
                            textMode = ro.optString("textMode", PdfSizeTextMode.DEFAULT.key),
                            showDimensionBadge = ro.optBoolean("showDimensionBadge", true)
                        )
                    }
                }
                PdfSizeDisplayConfig(
                    globalSizeCase = globalCase,
                    rules = rulesMap
                )
            } catch (_: Exception) {
                PdfSizeDisplayConfig()
            }
        }

        fun formatSizeWithGlobalCase(rawSize: String, globalCase: PdfGlobalSizeCase): String {
            val trimmed = rawSize.trim()
            if (trimmed.isBlank()) return trimmed
            return when (globalCase) {
                PdfGlobalSizeCase.AS_IS -> trimmed
                PdfGlobalSizeCase.UPPERCASE -> trimmed.uppercase(Locale.ENGLISH).replace("×", "X")
                PdfGlobalSizeCase.LOWERCASE -> trimmed.lowercase(Locale.ENGLISH).replace("×", "x")
                PdfGlobalSizeCase.MULTIPLY_SIGN -> trimmed.replace("x", "×", ignoreCase = true).replace("*", "×")
            }
        }

        fun resolveForTile(
            rawSize: String,
            config: PdfSizeDisplayConfig
        ): ResolvedPdfTileDisplay {
            val baseDim = TileSampleCatalogPdfGenerator.parseTileDimension(rawSize)
            val rule = config.getRuleForSize(rawSize)
            val globalCase = PdfGlobalSizeCase.fromKey(config.globalSizeCase)

            val orientation = PdfTileOrientation.fromKey(rule?.orientation)
            val fitMode = PdfImageScaleMode.fromKey(rule?.imageFitMode)
            val textMode = PdfSizeTextMode.fromKey(rule?.textMode)

            val baseLandscapeRatio = if (baseDim.heightInches > 0f) {
                maxOf(baseDim.widthInches, baseDim.heightInches) / minOf(baseDim.widthInches, baseDim.heightInches)
            } else {
                1.5f
            }

            val renderRatioWtoH = when (orientation) {
                PdfTileOrientation.LANDSCAPE -> baseLandscapeRatio.coerceIn(1f, 3.0f)
                PdfTileOrientation.PORTRAIT -> (1f / baseLandscapeRatio).coerceIn(0.35f, 1f)
                PdfTileOrientation.SQUARE -> 1f
                PdfTileOrientation.CUSTOM -> {
                    val cw = rule?.customWidthRatio ?: 0f
                    val ch = rule?.customHeightRatio ?: 0f
                    if (cw > 0f && ch > 0f) (cw / ch).coerceIn(0.35f, 3.0f) else baseLandscapeRatio
                }
            }

            val displayWIn: Float
            val displayHIn: Float
            if (orientation == PdfTileOrientation.PORTRAIT) {
                displayWIn = minOf(baseDim.widthInches, baseDim.heightInches)
                displayHIn = maxOf(baseDim.widthInches, baseDim.heightInches)
            } else {
                displayWIn = maxOf(baseDim.widthInches, baseDim.heightInches)
                displayHIn = minOf(baseDim.widthInches, baseDim.heightInches)
            }
            val displayWFt = displayWIn / 12f
            val displayHFt = displayHIn / 12f

            val isCustomScale = rule != null && rule.boxScalePercent in 40..100
            val defaultScale = when {
                baseDim.widthFeet >= 3.5f || baseDim.heightFeet >= 3.5f -> 1.0f
                (baseDim.widthFeet in 1.2f..1.8f && baseDim.heightFeet in 0.9f..1.6f) -> 0.84f
                else -> (baseDim.widthFeet / 4f).coerceIn(0.65f, 0.92f)
            }
            val scaleFactor = if (isCustomScale) {
                (rule!!.boxScalePercent / 100f).coerceIn(0.40f, 1.0f)
            } else {
                defaultScale
            }

            val ftText = "${formatTrimmedFloat(displayWFt)} × ${formatTrimmedFloat(displayHFt)} Ft"
            val inText = "${displayWIn.toInt()}\" × ${displayHIn.toInt()}\""

            val sizeLabelForPdf = when {
                !rule?.customDisplayLabel.isNullOrBlank() &&
                    (textMode == PdfSizeTextMode.CUSTOM_LABEL || textMode == PdfSizeTextMode.DEFAULT) -> {
                    rule!!.customDisplayLabel.trim()
                }
                textMode == PdfSizeTextMode.FEET_ONLY -> ftText
                textMode == PdfSizeTextMode.INCHES_ONLY -> inText
                textMode == PdfSizeTextMode.BOTH -> "$inText ($ftText)"
                textMode == PdfSizeTextMode.CUSTOM_LABEL -> {
                    rule?.customDisplayLabel?.trim()?.ifBlank { formatSizeWithGlobalCase(rawSize, globalCase) }
                        ?: formatSizeWithGlobalCase(rawSize, globalCase)
                }
                else -> formatSizeWithGlobalCase(rawSize, globalCase)
            }

            val detailSubLabel = when (textMode) {
                PdfSizeTextMode.FEET_ONLY -> ftText
                PdfSizeTextMode.INCHES_ONLY -> inText
                PdfSizeTextMode.CUSTOM_LABEL -> sizeLabelForPdf
                else -> "$inText ($ftText)"
            }

            val badgeTextForCatalog = when {
                !rule?.customDisplayLabel.isNullOrBlank() && textMode == PdfSizeTextMode.CUSTOM_LABEL -> {
                    "${rule!!.customDisplayLabel.trim()} (${formatTrimmedFloat(baseDim.areaSqFtPerPiece)} Sq.Ft/pc)"
                }
                textMode == PdfSizeTextMode.INCHES_ONLY -> {
                    "$inText (${formatTrimmedFloat(baseDim.areaSqFtPerPiece)} Sq.Ft/pc)"
                }
                else -> {
                    "${formatTrimmedFloat(displayWFt)} Ft × ${formatTrimmedFloat(displayHFt)} Ft (${formatTrimmedFloat(baseDim.areaSqFtPerPiece)} Sq.Ft/pc)"
                }
            }

            return ResolvedPdfTileDisplay(
                rawSize = rawSize,
                normalizedKey = normalizeSizeKey(rawSize),
                sizeLabelForPdf = sizeLabelForPdf,
                detailSubLabel = detailSubLabel,
                badgeTextForCatalog = badgeTextForCatalog,
                renderAspectRatioWtoH = renderRatioWtoH,
                scaleFactor = scaleFactor,
                hasCustomScale = isCustomScale,
                orientation = orientation,
                imageFitMode = fitMode,
                showDimensionBadge = rule?.showDimensionBadge ?: true,
                widthFeet = displayWFt,
                heightFeet = displayHFt,
                widthInches = displayWIn,
                heightInches = displayHIn,
                areaSqFtPerPiece = baseDim.areaSqFtPerPiece
            )
        }

        fun computeCenterCropSrcRect(
            bitmapWidth: Int,
            bitmapHeight: Int,
            destWidth: Float,
            destHeight: Float
        ): Rect {
            if (bitmapWidth <= 0 || bitmapHeight <= 0 || destWidth <= 0f || destHeight <= 0f) {
                return Rect(0, 0, maxOf(1, bitmapWidth), maxOf(1, bitmapHeight))
            }
            val srcRatio = bitmapWidth.toFloat() / bitmapHeight.toFloat()
            val dstRatio = destWidth / destHeight
            return if (srcRatio > dstRatio) {
                // Source is wider -> crop left/right
                val newW = (bitmapHeight * dstRatio).toInt().coerceIn(1, bitmapWidth)
                val left = (bitmapWidth - newW) / 2
                Rect(left, 0, left + newW, bitmapHeight)
            } else {
                // Source is taller -> crop top/bottom
                val newH = (bitmapWidth / dstRatio).toInt().coerceIn(1, bitmapHeight)
                val top = (bitmapHeight - newH) / 2
                Rect(0, top, bitmapWidth, top + newH)
            }
        }

        fun formatTrimmedFloat(value: Float): String {
            val rounded = kotlin.math.round(value * 100f) / 100f
            return if (rounded == rounded.toInt().toFloat()) {
                rounded.toInt().toString()
            } else {
                String.format(Locale.US, "%.1f", rounded).trimEnd('0').trimEnd('.')
            }
        }
    }
}

data class ResolvedPdfTileDisplay(
    val rawSize: String,
    val normalizedKey: String,
    val sizeLabelForPdf: String,
    val detailSubLabel: String,
    val badgeTextForCatalog: String,
    val renderAspectRatioWtoH: Float,
    val scaleFactor: Float,
    val hasCustomScale: Boolean,
    val orientation: PdfTileOrientation,
    val imageFitMode: PdfImageScaleMode,
    val showDimensionBadge: Boolean,
    val widthFeet: Float,
    val heightFeet: Float,
    val widthInches: Float,
    val heightInches: Float,
    val areaSqFtPerPiece: Float
)
