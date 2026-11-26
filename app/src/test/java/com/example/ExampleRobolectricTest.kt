package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.ShopSettings
import com.example.data.model.TileProduct
import com.example.data.repository.MouryaRepository
import com.example.ui.screens.DraftOrderItem
import com.example.util.ShareHelper
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: MouryaRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MouryaRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read app_name string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("TILES AND MARBLES", appName)
    }

    @Test
    fun `test phone validation for Indian mobile numbers`() {
        val (valid1, formatted1) = ShareHelper.validateIndianMobileNumber("9876543210")
        assertTrue(valid1)
        assertEquals("919876543210", formatted1)

        val (valid2, formatted2) = ShareHelper.validateIndianMobileNumber("+91 98765 43210")
        assertTrue(valid2)
        assertEquals("919876543210", formatted2)

        val (invalid, _) = ShareHelper.validateIndianMobileNumber("12345")
        assertFalse(invalid)
    }

    @Test
    fun `test tile product stock calculations with custom pieces per box`() {
        // Product 1: 5 pieces per box, 53 total pieces -> 10 boxes, 3 loose
        val p1 = TileProduct(
            code = "MTM-001",
            name = "Statuario White",
            size = "12x18",
            piecesPerBox = 5,
            stockPieces = 53
        )
        assertEquals(10, p1.fullBoxes)
        assertEquals(3, p1.loosePieces)
        assertEquals("10 Bx + 3 Pcs", p1.stockDisplay)
        assertTrue(p1.isLowStock) // 10 boxes <= 10

        // Product 2: 2 pieces per box, 100 pieces -> 50 boxes, 0 loose
        val p2 = TileProduct(
            code = "MTM-002",
            name = "Onyx Slab",
            size = "2x4",
            piecesPerBox = 2,
            stockPieces = 100
        )
        assertEquals(50, p2.fullBoxes)
        assertEquals(0, p2.loosePieces)
        assertEquals("50 Bx", p2.stockDisplay)
        assertFalse(p2.isLowStock)
    }

    @Test
    fun `test draft order calculations with loose pieces rate`() {
        val product = TileProduct(
            code = "MTM-003",
            name = "Elevation Wood",
            size = "12x18",
            piecesPerBox = 4,
            stockPieces = 40,
            defaultSellingPrice = 400.0 // 400 per box -> 100 per piece
        )
        // 2 full boxes + 2 loose pieces @ 400/box
        val draft = DraftOrderItem(
            product = product,
            fullBoxes = 2,
            loosePieces = 2,
            pricePerBox = 400.0
        )
        assertEquals(10, draft.totalPieces) // 2 * 4 + 2
        // 2 boxes * 400 = 800 + 2 pieces * 100 = 200 -> total 1000.0
        assertEquals(1000.0, draft.lineTotal, 0.001)
    }

    @Test
    fun `test pending order does not reduce stock, confirming deducts stock, delete restores`() = runTest {
        // 1. Add product with 10 boxes (5 pcs/box = 50 pieces)
        val product = TileProduct(
            code = "MTM-T01",
            name = "Test Vitrified",
            size = "2x4",
            piecesPerBox = 5,
            stockPieces = 50,
            defaultSellingPrice = 500.0
        )
        val prodId = repository.addProduct(product).getOrThrow()

        // 2. Create PENDING order for 2 boxes (10 pieces)
        val order = OrderEntity(
            invoiceNumber = "TEST-001",
            customerName = "Test Buyer",
            status = "PENDING",
            grandTotal = 1000.0
        )
        val item = OrderItemEntity(
            productId = prodId,
            tileCode = "MTM-T01",
            tileName = "Test Vitrified",
            tileSize = "2x4",
            piecesPerBox = 5,
            fullBoxes = 2,
            loosePieces = 0,
            pricePerBox = 500.0,
            lineTotal = 1000.0
        )
        val orderId = repository.createOrder(order, listOf(item)).getOrThrow()

        // Verify stock is still 50 pieces (10 boxes)
        var currentProd = repository.getProductById(prodId)
        assertNotNull(currentProd)
        assertEquals(50, currentProd!!.stockPieces)
        assertEquals(10, currentProd.fullBoxes)

        // 3. Confirm the pending order
        val confirmResult = repository.confirmPendingOrder(orderId)
        assertTrue(confirmResult.isSuccess)

        // Verify stock is deducted by 10 pieces -> 40 pieces (8 boxes)
        currentProd = repository.getProductById(prodId)
        assertNotNull(currentProd)
        assertEquals(40, currentProd!!.stockPieces)
        assertEquals(8, currentProd.fullBoxes)

        // 4. Delete confirmed order with restoreStock = true
        val deleteResult = repository.deleteOrder(orderId, restoreStock = true)
        assertTrue(deleteResult.isSuccess)

        // Verify stock restored back to 50 pieces (10 boxes)
        currentProd = repository.getProductById(prodId)
        assertNotNull(currentProd)
        assertEquals(50, currentProd!!.stockPieces)
    }

    @Test
    fun `test WhatsApp bill message generation`() {
        val settings = ShopSettings(
            shopName = "TILES AND MARBLES",
            subtitle = "MAIN ROAD",
            ownerPhone = "+91 98765 43210"
        )
        val orderWithPrice = OrderEntity(
            invoiceNumber = "MTM-2026-001",
            customerName = "Ramesh Kumar",
            status = "CONFIRMED",
            showPriceOnPdf = true,
            grandTotal = 12500.0
        )
        val msgWithPrice = ShareHelper.buildShareMessage(settings, orderWithPrice)
        assertTrue(msgWithPrice.contains("TILES AND MARBLES"))
        assertTrue(msgWithPrice.contains("MAIN ROAD"))
        assertTrue(msgWithPrice.contains("12500.00"))

        val orderWithoutPrice = orderWithPrice.copy(showPriceOnPdf = false)
        val msgWithoutPrice = ShareHelper.buildShareMessage(settings, orderWithoutPrice)
        assertTrue(msgWithoutPrice.contains("Material Delivery Slip"))
        assertFalse(msgWithoutPrice.contains("12500.00"))
    }
}
