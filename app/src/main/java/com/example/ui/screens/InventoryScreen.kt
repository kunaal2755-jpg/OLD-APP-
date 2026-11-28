package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.ui.theme.getAppThemeOption
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.example.util.CatalogPdfConfig
import com.example.util.ShareHelper
import com.example.util.TileSampleCatalogPdfGenerator
import java.io.File
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.TileProduct
import com.example.ui.MainViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val sizePricings by viewModel.sizePricings.collectAsStateWithLifecycle()
    val tileLocations by viewModel.tileLocations.collectAsStateWithLifecycle()
    val settings by viewModel.shopSettings.collectAsStateWithLifecycle()
    val activeTheme = getAppThemeOption(settings.appTheme)

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "LOW_STOCK", "LOC:<name>", or size name
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<TileProduct?>(null) }
    var productToDelete by remember { mutableStateOf<TileProduct?>(null) }

    // Tile Sample Catalog PDF States
    var showSampleCatalogDialog by remember { mutableStateOf(false) }
    var isGeneratingCatalogPdf by remember { mutableStateOf(false) }
    var showPdfSizeRulesDialog by remember { mutableStateOf(false) }
    var generatedCatalogPdfFile by remember { mutableStateOf<File?>(null) }
    var showCatalogPdfPreview by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val filteredProducts = products.filter { prod ->
        val matchesQuery = searchQuery.isBlank() ||
                prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.code.contains(searchQuery, ignoreCase = true) ||
                prod.size.contains(searchQuery, ignoreCase = true) ||
                prod.location.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when {
            selectedFilter == "ALL" -> true
            selectedFilter == "LOW_STOCK" -> prod.isLowStock
            selectedFilter.startsWith("LOC:") -> {
                val targetLoc = selectedFilter.removePrefix("LOC:")
                if (targetLoc == "__NONE__") prod.location.isBlank()
                else prod.location.equals(targetLoc, ignoreCase = true)
            }
            else -> prod.size.equals(selectedFilter, ignoreCase = true)
        }

        matchesQuery && matchesFilter
    }

    val availableSizes = products.map { it.size }.distinct()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = activeTheme.primary,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tile Inventory & Stock",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = PureWhite
                        )
                        Text(
                            text = "${products.size} Products • ${products.sumOf { it.fullBoxes }} Boxes Total",
                            fontSize = 11.sp,
                            color = Color(0xFF89C2D9)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSampleCatalogDialog = true },
                        modifier = Modifier.testTag("btn_sample_catalog_pdf")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "सैंपल कैटलॉग PDF",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = activeTheme.primary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showAddEditDialog = true
                },
                containerColor = activeTheme.primary,
                contentColor = PureWhite,
                modifier = Modifier.testTag("fab_add_tile")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Tile")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Download Sample Catalog Button (PDF with proportional tile dimensions & full details)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = activeTheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { showSampleCatalogDialog = true }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = activeTheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "📥 डाउनलोड सैंपल कैटलॉग (Tile Catalog PDF)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = activeTheme.primary
                            )
                            Text(
                                text = "फोटो, वास्तविक आकार, कुल स्क्वायर फीट व स्टॉक विवरण",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    Button(
                        onClick = { showSampleCatalogDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("PDF बनाएं", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("नाम, कोड, साइज़ या लोकेशन (Godown/Dukaan) खोजें...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("inventory_search_input")
            )

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${products.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "LOW_STOCK",
                        onClick = { selectedFilter = "LOW_STOCK" },
                        label = { Text("Low Stock (${products.count { it.isLowStock }})") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = StatusError,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                items(availableSizes) { size ->
                    FilterChip(
                        selected = selectedFilter == size,
                        onClick = { selectedFilter = size },
                        label = { Text(size) }
                    )
                }
                items(tileLocations) { loc ->
                    val locKey = "LOC:${loc.name}"
                    val locCount = products.count { it.location.equals(loc.name, ignoreCase = true) }
                    FilterChip(
                        selected = selectedFilter == locKey,
                        onClick = { selectedFilter = locKey },
                        label = { Text("📍 ${loc.name} ($locCount)") }
                    )
                }
            }

            if (filteredProducts.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Inventory,
                    title = if (searchQuery.isNotBlank()) "No tiles match '$searchQuery'" else "No tiles in inventory",
                    description = "Add your first tile product with custom pieces-per-box and stock count.",
                    actionLabel = "Add New Tile",
                    onAction = {
                        editingProduct = null
                        showAddEditDialog = true
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        TileProductCard(
                            product = product,
                            onEdit = {
                                editingProduct = product
                                showAddEditDialog = true
                            },
                            onDelete = {
                                productToDelete = product
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Tile Dialog
    if (showAddEditDialog) {
        AddEditTileDialog(
            product = editingProduct,
            sizePricings = sizePricings.map { it.sizeName },
            locations = tileLocations.map { it.name },
            onQuickAddLocation = { newLocName ->
                viewModel.addTileLocation(newLocName)
            },
            onDismiss = { showAddEditDialog = false },
            onSave = { id, code, name, size, photoUri, desc, piecesPerBox, totalPieces, sellingPrice, cost, location ->
                viewModel.saveProduct(
                    id = id,
                    code = code,
                    name = name,
                    size = size,
                    photoUri = photoUri,
                    description = desc,
                    piecesPerBox = piecesPerBox,
                    stockPieces = totalPieces,
                    defaultSellingPrice = sellingPrice,
                    purchaseCost = cost,
                    location = location,
                    onSuccess = { showAddEditDialog = false },
                    onError = { viewModel.showMessage(it) }
                )
            },
            onSuggestPrice = { size ->
                viewModel.sizePricings.value.find { it.sizeName.equals(size, ignoreCase = true) }?.defaultPricePerBox
            },
            onSuggestPacking = { size ->
                viewModel.sizePricings.value.find { it.sizeName.equals(size, ignoreCase = true) }?.defaultPiecesPerBox
            },
            onSaveImageLocally = { uri ->
                // helper method to persist image to internal app files
                viewModel.persistImageLocally(viewModel.getApplication(), uri)
            }
        )
    }

    // Delete Confirmation Dialog
    if (productToDelete != null) {
        val prod = productToDelete!!
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = StatusError
                )
            },
            title = { Text("Delete Tile Product") },
            text = {
                Text("Are you sure you want to delete '${prod.name}' (${prod.code})? This product will no longer appear in future sale orders.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(prod.id)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Delete", color = PureWhite)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { productToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Tile Sample Catalog Options Dialog
    if (showSampleCatalogDialog) {
        val context = LocalContext.current
        TileSampleCatalogConfigDialog(
            sizesList = availableSizes,
            primaryColor = activeTheme.primary,
            onDismiss = { showSampleCatalogDialog = false },
            onOpenPerSizeRules = { showPdfSizeRulesDialog = true },
            onGenerate = { config ->
                showSampleCatalogDialog = false
                if (isGeneratingCatalogPdf) return@TileSampleCatalogConfigDialog
                isGeneratingCatalogPdf = true
                val productsSnapshot = products.toList()
                val settingsSnapshot = settings
                coroutineScope.launch {
                    try {
                        val pdfFile = withContext(Dispatchers.IO) {
                            TileSampleCatalogPdfGenerator.generateCatalogPdf(
                                context = context.applicationContext,
                                settings = settingsSnapshot,
                                products = productsSnapshot,
                                config = config
                            )
                        }
                        generatedCatalogPdfFile = pdfFile
                        showCatalogPdfPreview = true
                    } catch (t: Throwable) {
                        viewModel.showMessage("PDF बनाने में त्रुटि: ${t.localizedMessage ?: "कृपया दोबारा प्रयास करें"}")
                    } finally {
                        isGeneratingCatalogPdf = false
                    }
                }
            }
        )
    }

    // Per-Size PDF Display Customization Modal (accessible directly from Inventory PDF dialog)
    if (showPdfSizeRulesDialog) {
        AlertDialog(
            onDismissRequest = { showPdfSizeRulesDialog = false },
            title = {
                Text(
                    text = "2X4, 12X18 — PDF साइज़ व इमेज नियम",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    com.example.ui.components.PdfSizeCustomizationSection(
                        configJson = settings.pdfSizeDisplayConfigJson,
                        availableSizes = availableSizes,
                        primaryColor = activeTheme.primary,
                        accentColor = activeTheme.accent,
                        onSaveConfigJson = { json -> viewModel.savePdfSizeDisplayConfig(json) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPdfSizeRulesDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primary)
                ) {
                    Text("हो गया (Done)", color = PureWhite)
                }
            }
        )
    }

    // Loading Dialog while PDF generates on background thread (prevents ANR & crash)
    if (isGeneratingCatalogPdf) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("PDF कैटलॉग बन रहा है...", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    CircularProgressIndicator(
                        color = activeTheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "सभी टाइल्स व उनकी फोटो सुरक्षित तरीके से PDF में जोड़ी जा रही हैं। कृपया प्रतीक्षा करें...",
                        fontSize = 12.sp,
                        color = Color(0xFF334155)
                    )
                }
            },
            confirmButton = {}
        )
    }

    // Catalog PDF Preview & Share Modal
    if (showCatalogPdfPreview && generatedCatalogPdfFile != null) {
        val context = LocalContext.current
        val pdfFile = generatedCatalogPdfFile!!
        CatalogPdfPreviewModal(
            pdfFile = pdfFile,
            shopName = settings.shopName.ifBlank { "Mourya Tiles" },
            onDismiss = { showCatalogPdfPreview = false },
            onOpenPdf = {
                try {
                    ShareHelper.viewPdf(context, pdfFile)
                } catch (t: Throwable) {
                    viewModel.showMessage("PDF खोलने के लिए कोई PDF Viewer ऐप इंस्टॉल करें")
                }
            },
            onSharePdf = {
                try {
                    ShareHelper.sharePdf(context, pdfFile, "Tile Sample Catalog - ${settings.shopName}")
                } catch (t: Throwable) {
                    viewModel.showMessage("PDF शेयर करने में त्रुटि: ${t.localizedMessage ?: ""}")
                }
            }
        )
    }
}

@Composable
private fun TileProductCard(
    product: TileProduct,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo thumbnail
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
            ) {
                if (!product.photoUri.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(product.photoUri),
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = product.size,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xFFE0F2FE),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = product.code,
                            color = Color(0xFF0369A1),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${product.size} (${product.piecesPerBox} Pcs/Bx)",
                            color = Color(0xFF475569),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Storage Location Badge (shows set location or prompt to add)
                if (product.location.isNotBlank()) {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "📍 ${product.location}",
                            color = Color(0xFF047857),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "📍 लोकेशन नहीं चुनी गई (Edit से जोड़ें)",
                        fontSize = 9.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Stock Display
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Stock: ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${product.fullBoxes} Boxes" + if (product.loosePieces > 0) " + ${product.loosePieces} Pcs" else "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (product.isLowStock) StatusError else Color(0xFF15803D)
                    )
                    if (product.isLowStock) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "LOW",
                                color = StatusError,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Rate: ₹${String.format(Locale.US, "%.0f", product.defaultSellingPrice)} / Box",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (product.purchaseCost > 0) {
                        Text(
                            text = "Cost: ₹${String.format(Locale.US, "%.0f", product.purchaseCost)}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Action Buttons
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Tile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Tile",
                        tint = StatusError,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEditTileDialog(
    product: TileProduct?,
    sizePricings: List<String>,
    locations: List<String>,
    onQuickAddLocation: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        code: String,
        name: String,
        size: String,
        photoUri: String?,
        description: String,
        piecesPerBox: Int,
        totalPieces: Int,
        sellingPrice: Double,
        purchaseCost: Double,
        location: String
    ) -> Unit,
    onSuggestPrice: (String) -> Double?,
    onSuggestPacking: (String) -> Int? = { null },
    onSaveImageLocally: (Uri) -> String?
) {
    val isEdit = product != null
    var code by remember { mutableStateOf(product?.code ?: "") }
    var name by remember { mutableStateOf(product?.name ?: "") }
    var size by remember { mutableStateOf(product?.size ?: "12x18") }
    var photoUri by remember { mutableStateOf(product?.photoUri) }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var selectedLocation by remember { mutableStateOf(product?.location ?: "") }
    var showInlineAddLoc by remember { mutableStateOf(false) }
    var inlineLocName by remember { mutableStateOf("") }
    var piecesPerBoxStr by remember { mutableStateOf(product?.piecesPerBox?.toString() ?: "5") }
    var boxesStockStr by remember { mutableStateOf(if (product != null) product.fullBoxes.toString() else "50") }
    var looseStockStr by remember { mutableStateOf(if (product != null && product.loosePieces > 0) product.loosePieces.toString() else "") }
    var priceStr by remember { mutableStateOf(product?.defaultSellingPrice?.toString() ?: "") }
    var costStr by remember { mutableStateOf(if (product != null && product.purchaseCost > 0) product.purchaseCost.toString() else "") }

    fun sanitizeInt(prev: String, incoming: String): String {
        val digits = incoming.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        if (prev == "0" && digits.length == 2) {
            val nonZero = digits.replace("0", "")
            return if (nonZero.isNotEmpty()) nonZero else "0"
        }
        val noLeading = digits.trimStart('0')
        return if (noLeading.isEmpty()) "0" else noLeading
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = onSaveImageLocally(uri)
            photoUri = localPath ?: uri.toString()
        }
    }

    // Auto-suggest price and packing when size changes
    fun onSizeSelected(newSize: String) {
        size = newSize
        if (priceStr.isBlank() || priceStr == "0" || priceStr == "0.0") {
            val suggested = onSuggestPrice(newSize)
            if (suggested != null) {
                priceStr = suggested.toString()
            }
        }
        val suggestedPcs = onSuggestPacking(newSize)
        if (suggestedPcs != null && suggestedPcs > 0 && (!isEdit || piecesPerBoxStr.isBlank() || piecesPerBoxStr == "0")) {
            piecesPerBoxStr = suggestedPcs.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Edit Tile Product" else "Add New Tile Product",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Photo Picker Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        if (!photoUri.isNullOrBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(photoUri),
                                contentDescription = "Tile Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    tint = Navy900
                                )
                                Text("Add Photo", fontSize = 9.sp, color = Navy900)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Tile Product Photo", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Photo will appear on customer PDF bill", fontSize = 11.sp, color = Color(0xFF64748B))
                        TextButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Text(if (photoUri != null) "Change Photo" else "Select From Gallery", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tile Code (Unique)
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase(Locale.getDefault()) },
                    label = { Text("Tile Code (e.g. MTM-101) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_tile_code")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tile Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tile Name (e.g. Royal Statuario) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_tile_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tile Size & Quick Chips
                Text("Tile Size (e.g. 12x18, 2x4) *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = size,
                    onValueChange = { onSizeSelected(it) },
                    placeholder = { Text("12x18, 2x4, 2x2, 16x16...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Size Chips
                val presetSizes = listOf("12x18", "2x4", "2x2", "16x16", "8x12")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(presetSizes) { s ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (size.equals(s, ignoreCase = true)) Color(0xFF0D1B2A) else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { onSizeSelected(s) }
                        ) {
                            Text(
                                text = s,
                                color = if (size.equals(s, ignoreCase = true)) PureWhite else Color(0xFF334155),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pieces Per Box (Product specific)
                OutlinedTextField(
                    value = piecesPerBoxStr,
                    onValueChange = { piecesPerBoxStr = sanitizeInt(piecesPerBoxStr, it) },
                    label = { Text("Pieces Per Box *") },
                    placeholder = { Text("e.g. 5 or 2 or 6") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stock (Boxes & Loose pieces)
                Text("Initial Warehouse Stock *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = boxesStockStr,
                        onValueChange = { boxesStockStr = sanitizeInt(boxesStockStr, it) },
                        label = { Text("Full Boxes") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = looseStockStr,
                        onValueChange = { looseStockStr = sanitizeInt(looseStockStr, it) },
                        label = { Text("Loose Pieces") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pricing
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Selling Rate/Box (₹) *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_tile_price")
                    )
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it },
                        label = { Text("Purchase Cost (₹)") },
                        placeholder = { Text("Internal profit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Category (Optional)") },
                    placeholder = { Text("e.g. Glossy Vitrified, Elevation") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tile Storage Location Selector (Settings locations + Quick Add)
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📍 टाइल कहाँ रखी है (Location)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            TextButton(
                                onClick = { showInlineAddLoc = !showInlineAddLoc },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text(
                                    text = if (showInlineAddLoc) "बंद करें" else "+ नई लोकेशन",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (showInlineAddLoc) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                OutlinedTextField(
                                    value = inlineLocName,
                                    onValueChange = { inlineLocName = it },
                                    placeholder = { Text("जैसे: गोदाम-2", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val clean = inlineLocName.trim()
                                        if (clean.isNotEmpty()) {
                                            onQuickAddLocation(clean)
                                            selectedLocation = clean
                                            inlineLocName = ""
                                            showInlineAddLoc = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("जोड़ें", fontSize = 11.sp, color = PureWhite)
                                }
                            }
                        }

                        val allLocChoices = remember(locations, selectedLocation) {
                            buildList {
                                add("")
                                locations.forEach { if (!contains(it)) add(it) }
                                if (selectedLocation.isNotBlank() && !contains(selectedLocation)) add(selectedLocation)
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            items(allLocChoices) { locOption ->
                                val isSel = selectedLocation.equals(locOption, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) Color(0xFF047857) else Color(0xFFE2E8F0),
                                    modifier = Modifier.clickable { selectedLocation = locOption }
                                ) {
                                    Text(
                                        text = if (locOption.isBlank()) "कोई नहीं (None)" else "📍 $locOption",
                                        color = if (isSel) PureWhite else Color(0xFF334155),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "सेटिंग्स में लोकेशन का नाम बदलने पर यहाँ भी अपने आप बदल जाएगा।",
                            fontSize = 9.5.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ppb = piecesPerBoxStr.toIntOrNull() ?: 5
                    val boxes = boxesStockStr.toIntOrNull() ?: 0
                    val loose = looseStockStr.toIntOrNull() ?: 0
                    val totalPieces = (boxes * ppb) + loose
                    val sellingPrice = priceStr.toDoubleOrNull() ?: 0.0
                    val cost = costStr.toDoubleOrNull() ?: 0.0

                    onSave(
                        product?.id ?: 0L,
                        code,
                        name,
                        size,
                        photoUri,
                        description,
                        ppb,
                        totalPieces,
                        sellingPrice,
                        cost,
                        selectedLocation
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                modifier = Modifier.testTag("save_tile_button")
            ) {
                Text(if (isEdit) "Update Tile" else "Save Tile", color = PureWhite)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TileSampleCatalogConfigDialog(
    sizesList: List<String>,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onOpenPerSizeRules: () -> Unit,
    onGenerate: (CatalogPdfConfig) -> Unit
) {
    var showPhoto by remember { mutableStateOf(true) }
    var showCodeAndName by remember { mutableStateOf(true) }
    var showSizeAndProportion by remember { mutableStateOf(true) }
    var showStockBoxes by remember { mutableStateOf(true) }
    var showTotalSqFt by remember { mutableStateOf(true) }
    var showPiecesPerBox by remember { mutableStateOf(true) }
    var showQualityAndDescription by remember { mutableStateOf(true) }
    var showPrice by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var itemsPerPage by remember { mutableIntStateOf(4) }
    var imageHeightPercent by remember { mutableFloatStateOf(100f) }
    var imageWidthPercent by remember { mutableFloatStateOf(100f) }
    var imageFitOverride by remember { mutableStateOf("RULE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "टाइल सैंपल कैटलॉग PDF तैयार करें",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "इमेज साइज़, पेज लेआउट और विवरण कस्टमाइज़ करें",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Category / Size Selector
                Text(
                    text = "1. टाइल श्रेणी / साइज़ चुनें (Select Category):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == "ALL",
                            onClick = { selectedCategory = "ALL" },
                            label = { Text("सभी टाइल्स (All)", fontSize = 11.sp) }
                        )
                    }
                    items(sizesList) { sz ->
                        FilterChip(
                            selected = selectedCategory == sz,
                            onClick = { selectedCategory = sz },
                            label = { Text(sz, fontSize = 11.sp) }
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Enhanced PDF Image Size & Layout Customization Section
                Text(
                    text = "2. PDF में इमेज साइज़ व पेज लेआउट (Image Size Customizer):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Tiles Per Page chips
                        Text(
                            text = "एक पेज पर कितनी टाइल्स (Tiles Per Page):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                2 to "2 बड़ी फोटो",
                                4 to "4 मानक",
                                6 to "6 कॉम्पैक्ट"
                            ).forEach { (count, label) ->
                                FilterChip(
                                    selected = itemsPerPage == count,
                                    onClick = { itemsPerPage = count },
                                    label = { Text(label, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Live Visual Preview of Image Size Sliders
                        val previewW = (96f * (imageWidthPercent / 100f)).coerceIn(48f, 130f)
                        val previewH = (56f * (imageHeightPercent / 100f)).coerceIn(30f, 84f)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(92.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PureWhite)
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(width = previewW.dp, height = previewH.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(primaryColor.copy(alpha = 0.85f))
                                    .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(6.dp))
                            ) {
                                Text(
                                    text = "फोटो ${imageWidthPercent.roundToInt()}% × ${imageHeightPercent.roundToInt()}%",
                                    color = PureWhite,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Image Height Slider
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("इमेज ऊंचाई (Height):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("${imageHeightPercent.roundToInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                        }
                        Slider(
                            value = imageHeightPercent,
                            onValueChange = { imageHeightPercent = it },
                            valueRange = 60f..150f,
                            steps = 8,
                            colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                        )

                        // Image Width Slider
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("इमेज चौड़ाई / ज़ूम (Width Scale):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("${imageWidthPercent.roundToInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                        }
                        Slider(
                            value = imageWidthPercent,
                            onValueChange = { imageWidthPercent = it },
                            valueRange = 60f..120f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                        )

                        // Image Fit Mode Override
                        Text(
                            text = "फोटो फिट तरीका (Photo Fit Mode):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "RULE" to "सेटिंग अनुसार",
                                "FIT_BOX" to "पूरा फिट",
                                "CENTER_CROP" to "क्रॉप फिट"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = imageFitOverride == key,
                                    onClick = { imageFitOverride = key },
                                    label = { Text(label, fontSize = 9.5.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Quick button to edit per-size rules (2X4, 12X18, etc.)
                        OutlinedButton(
                            onClick = onOpenPerSizeRules,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("catalog_per_size_rules_button")
                        ) {
                            Icon(imageVector = Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("2X4, 12X18 अलग-अलग साइज़ नियम सेट करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "3. PDF में क्या-क्या विवरण दिखाएं (Details to Display):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        CatalogOptionRow(
                            title = "टाइल फोटो व वास्तविक आकार (Proportional Size Image)",
                            subtitle = "12x18 = 1.5 ft × 1 ft, 2x4 = 2×4 ft आदि अनुपात में",
                            checked = showPhoto,
                            onCheckedChange = { showPhoto = it }
                        )
                        CatalogOptionRow(
                            title = "टाइल कोड व नाम (Tile Code & Name)",
                            subtitle = "उदा. [MTM-101] Royal Statuario Vitrified",
                            checked = showCodeAndName,
                            onCheckedChange = { showCodeAndName = it }
                        )
                        CatalogOptionRow(
                            title = "साइज़ व फ़ीट विवरण (Size & Dimensions)",
                            subtitle = "इंच और फ़ीट दोनों में माप",
                            checked = showSizeAndProportion,
                            onCheckedChange = { showSizeAndProportion = it }
                        )
                        CatalogOptionRow(
                            title = "क्वालिटी व फिनिश (Quality & Finish)",
                            subtitle = "हाई ग्लॉस, मिरर पॉलिश, विट्रीफाइड आदि",
                            checked = showQualityAndDescription,
                            onCheckedChange = { showQualityAndDescription = it }
                        )
                        CatalogOptionRow(
                            title = "बचा हुआ स्टॉक (Available Stock)",
                            subtitle = "कुल कितने बॉक्स व पीस दुकान में उपलब्ध हैं",
                            checked = showStockBoxes,
                            onCheckedChange = { showStockBoxes = it }
                        )
                        CatalogOptionRow(
                            title = "कुल स्क्वायर फीट स्टॉक (Total Sq. Feet)",
                            subtitle = "स्टॉक का कुल वर्ग फीट एरिया",
                            checked = showTotalSqFt,
                            onCheckedChange = { showTotalSqFt = it }
                        )
                        CatalogOptionRow(
                            title = "एक बॉक्स में कितनी टाइल (Pieces per Box)",
                            subtitle = "पैकिंग व 1 बॉक्स का कुल एरिया",
                            checked = showPiecesPerBox,
                            onCheckedChange = { showPiecesPerBox = it }
                        )
                        CatalogOptionRow(
                            title = "बिक्री रेट / मूल्य ₹ (Selling Rate)",
                            subtitle = "प्रति बॉक्स व प्रति स्क्वायर फीट मूल्य",
                            checked = showPrice,
                            onCheckedChange = { showPrice = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onGenerate(
                        CatalogPdfConfig(
                            showPhoto = showPhoto,
                            showCodeAndName = showCodeAndName,
                            showSizeAndProportion = showSizeAndProportion,
                            showStockBoxes = showStockBoxes,
                            showTotalSqFt = showTotalSqFt,
                            showPiecesPerBox = showPiecesPerBox,
                            showQualityAndDescription = showQualityAndDescription,
                            showPrice = showPrice,
                            selectedCategory = selectedCategory,
                            itemsPerPage = itemsPerPage,
                            imageHeightPercent = imageHeightPercent.roundToInt(),
                            imageWidthPercent = imageWidthPercent.roundToInt(),
                            imageFitOverride = imageFitOverride
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("generate_catalog_pdf_confirm_button")
            ) {
                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("PDF बनाएं", fontWeight = FontWeight.Bold, color = PureWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
private fun CatalogOptionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
            Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
private fun CatalogPdfPreviewModal(
    pdfFile: File,
    shopName: String,
    onDismiss: () -> Unit,
    onOpenPdf: () -> Unit,
    onSharePdf: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFF0F766E),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "टाइल सैंपल कैटलॉग PDF तैयार है!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "दुकान: $shopName",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "फ़ाइल: ${pdfFile.name} (${pdfFile.length() / 1024} KB)",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }

                Text(
                    text = "इस कैटलॉग में हर टाइल का फोटो उसके वास्तविक आकार/अनुपात में बनाया गया है। नीचे टाइल कोड, नाम, क्वालिटी, उपलब्ध स्टॉक, कुल स्क्वायर फीट व बॉक्स पैकिंग विवरण दिया गया है।",
                    fontSize = 11.5.sp,
                    color = Color(0xFF334155)
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenPdf,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF देखें")
                }

                Button(
                    onClick = onSharePdf,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp / शेयर")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("बंद करें")
            }
        }
    )
}

