package com.salesnetwork.avon.app

import com.salesnetwork.avon.app.data.RouteEtaService
import com.salesnetwork.avon.app.scraper.CatalogScraperEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SalesNetworkModuleTest {

    @Test
    fun testDefaultCatalogGeneration() {
        val engine = CatalogScraperEngine()
        val products = engine.generateDefaultAvonCatalog()

        assertNotNull(products)
        assertTrue(products.isNotEmpty())
        assertEquals(5, products.size)
        assertTrue(products.any { it.category == "Perfumería" })
        assertTrue(products.any { it.name.contains("Far Away") })
    }

    @Test
    fun testHtmlScraperParsingWithCommaDecimals() {
        val engine = CatalogScraperEngine()
        val mockHtml = """
            <div class="catalog-item">
                <h2 class="product-title">Crema Facial Anew 50g</h2>
                <span class="product-price">S/ 89,90</span>
                <span class="category">Cuidado Facial</span>
            </div>
        """.trimIndent()

        val extracted = engine.parseHtmlCatalog(mockHtml, "https://www.avon.com.pe")

        assertNotNull(extracted)
        assertTrue(extracted.isNotEmpty())
        val product = extracted.first()
        assertEquals("Crema Facial Anew 50g", product.name)
        assertEquals(89.90, product.price, 0.01)
    }

    @Test
    fun testWhatsAppNumberFormatting() {
        val phone = "+51 979 123 456"
        val digitsOnly = phone.replace("[^0-9]".toRegex(), "")
        assertEquals("51979123456", digitsOnly)
    }

    @Test
    fun testRouteEtaCalculation() = runBlocking {
        val service = RouteEtaService()
        val eta = service.calculateRouteEta(-6.7714, -79.8409, -6.7820, -79.8460)

        assertNotNull(eta)
        assertTrue(eta.distanceKm > 0.0)
        assertTrue(eta.durationMinutes > 0)
        assertTrue(eta.formattedSummary.contains("min"))
    }
}
