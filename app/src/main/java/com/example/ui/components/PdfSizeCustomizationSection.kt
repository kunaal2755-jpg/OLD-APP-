package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PureWhite
import com.example.util.PdfGlobalSizeCase
import com.example.util.PdfImageScaleMode
import com.example.util.PdfSizeDisplayConfig
import com.example.util.PdfSizeDisplayRule
import com.example.util.PdfSizeTextMode
import com.example.util.PdfTileOrientation
import kotlin.math.roundToInt

@Composable
fun PdfSizeCustomizationSection(
    configJson: String,
    availableSizes: List<String>,
    primaryColor: Color,
    accentColor: Color,
    onSaveConfigJson: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val config = remember(configJson) { PdfSizeDisplayConfig.fromJson(configJson) }

    val allSizes = remember(availableSizes, config) {
        val merged = linkedMapOf<String, String>()
        val defaults = listOf("2x4", "12x18", "2x2", "16x16", "8x12")
        for (s in availableSizes + defaults + config.rules.keys) {
            val trimmed = s.trim()
            if (trimmed.isNotBlank()) {
                val norm = PdfSizeDisplayConfig.normalizeSizeKey(trimmed)
                if (!merged.containsKey(norm)) {
                    merged[norm] = trimmed
                }
            }
        }
        merged.values.toList()
    }

    var editingSize by remember { mutableStateOf<String?>(null) }
    var showAddCustomSizeDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pdf_size_customizer_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(primaryColor.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF Size Customizer",
                            tint = primaryColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PDF में टाइल साइज़ डिस्प्ले सेटिंग",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = primaryColor
                        )
                        Text(
                            text = "2X4, 12X18 आदि का साइज़, दिशा (खड़ा/आड़ा) व टेक्स्ट PDF में कैसा दिखेगा",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                TextButton(
                    onClick = { showAddCustomSizeDialog = true },
                    modifier = Modifier.testTag("add_pdf_size_rule_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Size",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("जोड़ें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Global Default Format Style (2X4 vs 2x4 vs 2×4)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "डिफ़ॉल्ट साइज़ लिखने का तरीका (PDF Text Style):",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    PdfGlobalSizeCase.entries.forEach { caseOpt ->
                        val isSelected = config.globalSizeCase.equals(caseOpt.key, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val updated = config.copy(globalSizeCase = caseOpt.key)
                                onSaveConfigJson(updated.toJson())
                            },
                            label = {
                                Text(
                                    text = caseOpt.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Per-size visual list
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                allSizes.forEach { rawSize ->
                    val resolved = remember(rawSize, config) {
                        PdfSizeDisplayConfig.resolveForTile(rawSize, config)
                    }
                    val hasCustomRule = config.getRuleForSize(rawSize) != null

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasCustomRule) primaryColor.copy(alpha = 0.05f) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            width = if (hasCustomRule) 1.5.dp else 1.dp,
                            color = if (hasCustomRule) primaryColor.copy(alpha = 0.45f) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingSize = rawSize }
                            .testTag("pdf_size_item_${resolved.normalizedKey}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            // Mini live proportional preview box
                            MiniTileAspectBox(
                                ratioWtoH = resolved.renderAspectRatioWtoH,
                                scaleFactor = resolved.scaleFactor,
                                primaryColor = primaryColor,
                                label = resolved.sizeLabelForPdf
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = rawSize.uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (hasCustomRule) accentColor.copy(alpha = 0.2f) else Color(0xFFE2E8F0)
                                    ) {
                                        Text(
                                            text = if (hasCustomRule) "कस्टम" else "डिफ़ॉल्ट",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasCustomRule) primaryColor else Color(0xFF475569),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "PDF टेक्स्ट: ${resolved.sizeLabelForPdf}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val scalePct = (resolved.scaleFactor * 100f).roundToInt()
                                Text(
                                    text = "${resolved.orientation.shortLabel} • आकार: $scalePct% • ${if (resolved.imageFitMode == PdfImageScaleMode.CENTER_CROP) "Crop" else "Fit"}",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            OutlinedButton(
                                onClick = { editingSize = rawSize },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Customize $rawSize",
                                    tint = primaryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("सेटिंग", fontSize = 11.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add New Size Name Dialog
    if (showAddCustomSizeDialog) {
        var customSizeInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCustomSizeDialog = false },
            title = { Text("नया टाइल साइज़ जोड़ें", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "जिस साइज़ के लिए PDF डिस्प्ले सेट करना है वह लिखें (जैसे 2X4, 12X18, 12X24, 32X64):",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )
                    OutlinedTextField(
                        value = customSizeInput,
                        onValueChange = { customSizeInput = it },
                        label = { Text("टाइल साइज़ *") },
                        placeholder = { Text("उदा. 12x24 या 32x64") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = customSizeInput.trim()
                        if (cleaned.isNotBlank()) {
                            showAddCustomSizeDialog = false
                            editingSize = cleaned
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("आगे बढ़ें", color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomSizeDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Customize Single Size Dialog with Live Preview
    editingSize?.let { targetSize ->
        PdfSizeRuleEditorDialog(
            rawSize = targetSize,
            config = config,
            primaryColor = primaryColor,
            onDismiss = { editingSize = null },
            onSaveRule = { newRule ->
                val updated = config.withRule(newRule)
                onSaveConfigJson(updated.toJson())
                editingSize = null
            },
            onResetRule = {
                val updated = config.withoutRule(targetSize)
                onSaveConfigJson(updated.toJson())
                editingSize = null
            }
        )
    }
}

@Composable
private fun MiniTileAspectBox(
    ratioWtoH: Float,
    scaleFactor: Float,
    primaryColor: Color,
    label: String
) {
    val maxBoxW = 54f
    val maxBoxH = 44f
    val safeRatio = ratioWtoH.coerceIn(0.35f, 3.0f)
    val safeScale = scaleFactor.coerceIn(0.45f, 1.0f)
    val targetW = maxBoxW * safeScale
    val targetH = maxBoxH * safeScale

    var w: Float
    var h: Float
    if (targetW / targetH > safeRatio) {
        h = targetH
        w = h * safeRatio
    } else {
        w = targetW
        h = w / safeRatio
    }
    w = w.coerceIn(14f, maxBoxW)
    h = h.coerceIn(14f, maxBoxH)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 60.dp, height = 50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE2E8F0))
            .padding(3.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = w.dp, height = h.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.linearGradient(
                        listOf(primaryColor.copy(alpha = 0.85f), primaryColor.copy(alpha = 0.65f))
                    )
                )
                .border(1.dp, PureWhite, RoundedCornerShape(4.dp))
        ) {
            Text(
                text = label.take(6),
                color = PureWhite,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PdfSizeRuleEditorDialog(
    rawSize: String,
    config: PdfSizeDisplayConfig,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onSaveRule: (PdfSizeDisplayRule) -> Unit,
    onResetRule: () -> Unit
) {
    val normKey = remember(rawSize) { PdfSizeDisplayConfig.normalizeSizeKey(rawSize) }
    val existingRule = remember(rawSize, config) { config.getRuleForSize(rawSize) }
    val initialResolved = remember(rawSize, config) { PdfSizeDisplayConfig.resolveForTile(rawSize, config) }

    var orientation by remember(normKey) {
        mutableStateOf(PdfTileOrientation.fromKey(existingRule?.orientation))
    }
    var customWInput by remember(normKey) {
        mutableStateOf(
            if ((existingRule?.customWidthRatio ?: 0f) > 0f) {
                PdfSizeDisplayConfig.formatTrimmedFloat(existingRule!!.customWidthRatio)
            } else {
                PdfSizeDisplayConfig.formatTrimmedFloat(initialResolved.widthFeet)
            }
        )
    }
    var customHInput by remember(normKey) {
        mutableStateOf(
            if ((existingRule?.customHeightRatio ?: 0f) > 0f) {
                PdfSizeDisplayConfig.formatTrimmedFloat(existingRule!!.customHeightRatio)
            } else {
                PdfSizeDisplayConfig.formatTrimmedFloat(initialResolved.heightFeet)
            }
        )
    }
    var scalePercent by remember(normKey) {
        mutableFloatStateOf(
            if ((existingRule?.boxScalePercent ?: 0) in 40..100) {
                existingRule!!.boxScalePercent.toFloat()
            } else {
                (initialResolved.scaleFactor * 100f).coerceIn(40f, 100f)
            }
        )
    }
    var imageFitMode by remember(normKey) {
        mutableStateOf(PdfImageScaleMode.fromKey(existingRule?.imageFitMode))
    }
    var textMode by remember(normKey) {
        mutableStateOf(PdfSizeTextMode.fromKey(existingRule?.textMode))
    }
    var customLabelInput by remember(normKey) {
        mutableStateOf(existingRule?.customDisplayLabel ?: "")
    }
    var showDimensionBadge by remember(normKey) {
        mutableStateOf(existingRule?.showDimensionBadge ?: true)
    }

    val previewRule = PdfSizeDisplayRule(
        sizeKey = normKey,
        customDisplayLabel = customLabelInput.trim(),
        orientation = orientation.key,
        customWidthRatio = customWInput.toFloatOrNull() ?: 0f,
        customHeightRatio = customHInput.toFloatOrNull() ?: 0f,
        boxScalePercent = scalePercent.roundToInt().coerceIn(40, 100),
        imageFitMode = imageFitMode.key,
        textMode = textMode.key,
        showDimensionBadge = showDimensionBadge
    )
    val previewResolved = remember(previewRule, config.globalSizeCase) {
        PdfSizeDisplayConfig.resolveForTile(rawSize, config.withRule(previewRule))
    }

    // Live animated dimensions inside a 210dp x 116dp mock PDF card frame
    val maxPreviewW = 196f
    val maxPreviewH = 104f
    val pRatio = previewResolved.renderAspectRatioWtoH.coerceIn(0.35f, 3.0f)
    val targetW = maxPreviewW * previewResolved.scaleFactor
    val targetH = maxPreviewH * previewResolved.scaleFactor
    val calcW: Float
    val calcH: Float
    if (targetW / targetH > pRatio) {
        calcH = targetH
        calcW = calcH * pRatio
    } else {
        calcW = targetW
        calcH = calcW / pRatio
    }
    val animW by animateDpAsState(targetValue = calcW.coerceIn(32f, maxPreviewW).dp, label = "previewW")
    val animH by animateDpAsState(targetValue = calcH.coerceIn(32f, maxPreviewH).dp, label = "previewH")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AspectRatio,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${rawSize.uppercase()} — PDF डिस्प्ले सेटिंग",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "बिल और कैटलॉग PDF दोनों पर लागू होगा",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Live PDF Visual Preview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            text = "👁️ PDF लाइव प्रीव्यू (Live Preview)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(116.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PureWhite)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(width = animW, height = animH)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(primaryColor, primaryColor.copy(alpha = 0.72f))
                                        )
                                    )
                                    .border(1.5.dp, Color(0xFF94A3B8), RoundedCornerShape(6.dp))
                            ) {
                                Text(
                                    text = previewResolved.sizeLabelForPdf,
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                if (showDimensionBadge) {
                                    Surface(
                                        color = Color(0xCC0F172A),
                                        shape = RoundedCornerShape(3.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 3.dp, start = 3.dp, end = 3.dp)
                                    ) {
                                        Text(
                                            text = previewResolved.badgeTextForCatalog,
                                            color = PureWhite,
                                            fontSize = 7.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "बिल PDF में: Size: ${previewResolved.sizeLabelForPdf}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                    }
                }

                // 1. Orientation selector
                Text(
                    text = "1. टाइल की दिशा / शेप (Orientation):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = primaryColor
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    PdfTileOrientation.entries.forEach { opt ->
                        FilterChip(
                            selected = orientation == opt,
                            onClick = { orientation = opt },
                            label = { Text(opt.shortLabel, fontSize = 11.sp) }
                        )
                    }
                }

                if (orientation == PdfTileOrientation.CUSTOM) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = customWInput,
                            onValueChange = { customWInput = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("चौड़ाई (Width)") },
                            placeholder = { Text("2") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customHInput,
                            onValueChange = { customHInput = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("ऊंचाई (Height)") },
                            placeholder = { Text("4") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. Box Scale Slider
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "2. PDF में फोटो बॉक्स का आकार (Size Scale):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = primaryColor
                        )
                        Text(
                            text = "${scalePercent.roundToInt()}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = primaryColor
                        )
                    }
                    Slider(
                        value = scalePercent,
                        onValueChange = { scalePercent = it },
                        valueRange = 40f..100f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = primaryColor,
                            activeTrackColor = primaryColor
                        )
                    )
                    Text(
                        text = "बड़ी टाइल (2X4) के लिए 95-100% और छोटी टाइल (12X18) के लिए 65-85% रख सकते हैं",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // 3. Image Fit Mode
                Text(
                    text = "3. फोटो फिट मोड (Photo Fit Mode):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = primaryColor
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PdfImageScaleMode.entries.forEach { mode ->
                        FilterChip(
                            selected = imageFitMode == mode,
                            onClick = { imageFitMode = mode },
                            label = { Text(mode.labelHi, fontSize = 10.5.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Size Label Format in PDF
                Text(
                    text = "4. PDF में साइज़ नाम कैसे लिखा दिखेगा:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = primaryColor
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    PdfSizeTextMode.entries.forEach { mode ->
                        FilterChip(
                            selected = textMode == mode,
                            onClick = { textMode = mode },
                            label = { Text(mode.labelHi, fontSize = 10.5.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = customLabelInput,
                    onValueChange = {
                        customLabelInput = it
                        if (it.isNotBlank() && textMode != PdfSizeTextMode.CUSTOM_LABEL) {
                            textMode = PdfSizeTextMode.CUSTOM_LABEL
                        }
                    },
                    label = { Text("कस्टम PDF साइज़ नाम (Optional)") },
                    placeholder = { Text("उदा. 2X4 Ft, 12\"×18\", 600×1200mm") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 5. Show dimension badge switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDimensionBadge = !showDimensionBadge }
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "फोटो पर साइज़ पट्टी (Badge) दिखाएं",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "कैटलॉग PDF में फोटो के नीचे Sq.Ft बैज दिखेगा",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Switch(
                        checked = showDimensionBadge,
                        onCheckedChange = { showDimensionBadge = it }
                    )
                }

                if (existingRule != null) {
                    TextButton(
                        onClick = onResetRule,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Default",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("डिफ़ॉल्ट पर रीसेट करें", fontSize = 11.5.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveRule(previewRule) },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("सेव करें (Save)", color = PureWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
