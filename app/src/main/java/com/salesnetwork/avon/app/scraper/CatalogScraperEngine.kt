package com.salesnetwork.avon.app.scraper

import com.salesnetwork.avon.app.domain.model.Product
import org.jsoup.Jsoup
import java.util.UUID

class CatalogScraperEngine {

    fun parseHtmlCatalog(htmlContent: String, sourceUrl: String = ""): List<Product> {
        val products = mutableListOf<Product>()
        try {
            val doc = Jsoup.parse(htmlContent)
            val productCards = doc.select(".product-card, .item-card, .catalog-item, article")
            
            for (i in 0 until productCards.size) {
                val card = productCards[i]
                val titleText = card.select(".product-title, .title, h2, h3").text()
                val title = if (titleText.isNotBlank()) titleText else "Producto Cosmético"
                
                val rawPrice = card.select(".price, .product-price, .amount").text()
                val normalizedPrice = rawPrice.replace(",", ".").replace("[^0-9.]".toRegex(), "")
                val price = normalizedPrice.toDoubleOrNull() ?: 29.90
                
                val catText = card.select(".category, .badge").text()
                val category = if (catText.isNotBlank()) catText else "Perfumería y Maquillaje"
                
                val imgUrl = card.select("img").attr("src")
                val dataSku = card.attr("data-sku")
                val sku = if (dataSku.isNotBlank()) dataSku else "AVON-${(1000..9999).random()}"
                
                products.add(
                    Product(
                        id = UUID.randomUUID().toString(),
                        sku = sku,
                        name = title,
                        category = category,
                        price = price,
                        imageUrl = imgUrl,
                        description = "Producto oficial de la campaña de ventas.",
                        sourceUrl = sourceUrl
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return if (products.isNotEmpty()) products else generateDefaultAvonCatalog()
    }

    fun generateDefaultAvonCatalog(): List<Product> {
        return listOf(
            Product(
                id = "p-101",
                sku = "AV-101",
                name = "Perfume Far Away Glamour 50ml",
                category = "Perfumería",
                price = 69.90,
                imageUrl = "https://images.unsplash.com/photo-1541643600914-78b084683601?auto=format&fit=crop&w=400&q=80",
                description = "Fragancia de larga duración con notas de grosella negra y flor de naranjo."
            ),
            Product(
                id = "p-102",
                sku = "AV-102",
                name = "Labial Ultra Matte Red Supreme",
                category = "Maquillaje",
                price = 24.90,
                imageUrl = "https://images.unsplash.com/photo-1586495777744-4413f21062fa?auto=format&fit=crop&w=400&q=80",
                description = "Acabado mate de alta cobertura que no reseca los labios."
            ),
            Product(
                id = "p-103",
                sku = "AV-103",
                name = "Crema Anew Reversalist Día SPF 25",
                category = "Cuidado de la Piel",
                price = 85.00,
                imageUrl = "https://images.unsplash.com/photo-1556228720-195a672e8a03?auto=format&fit=crop&w=400&q=80",
                description = "Crema facial antiedad con tecnología Protinol para elasticidad e hidratación."
            ),
            Product(
                id = "p-104",
                sku = "AV-104",
                name = "Máscara de Pestañas Lash Genius 5 en 1",
                category = "Maquillaje",
                price = 32.50,
                imageUrl = "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?auto=format&fit=crop&w=400&q=80",
                description = "Volumen, longitud, definición, elevación y color negro intenso."
            ),
            Product(
                id = "p-105",
                sku = "AV-105",
                name = "Loción Corporal Encanto Seducción 400ml",
                category = "Cuidado Corporal",
                price = 39.90,
                imageUrl = "https://images.unsplash.com/photo-1608248597261-833258657640?auto=format&fit=crop&w=400&q=80",
                description = "Hidratación 48 horas con infusión de aceites florales."
            )
        )
    }
}
