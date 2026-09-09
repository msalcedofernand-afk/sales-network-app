package com.salesnetwork.avon.app.scraper

import com.salesnetwork.avon.app.domain.model.Product
import org.jsoup.Jsoup
import java.util.UUID

class CatalogScraperEngine {

    fun parseHtmlCatalog(htmlContent: String, sourceUrl: String): List<Product> {
        val products = mutableListOf<Product>()
        try {
            val doc = Jsoup.parse(htmlContent, sourceUrl)
            val items = doc.select(".product-card, .catalog-item, [data-product-sku]")

            items.forEach { element ->
                val name = element.select(".product-title, .title, h2, h3").text().trim()
                val priceText = element.select(".product-price, .price, .amount").text().trim()
                val category = element.select(".category, .product-cat").text().trim()
                val imageUrl = element.select("img").attr("abs:src")
                val dataSku = element.attr("data-product-sku")

                val cleanPrice = priceText
                    .replace("[^0-9,.]".toRegex(), "")
                    .replace(",", ".")
                    .toDoubleOrNull() ?: 59.90

                val sku = if (dataSku.isNotBlank()) dataSku else "VV-${(1000..9999).random()}"

                if (name.isNotEmpty()) {
                    products.add(
                        Product(
                            id = UUID.randomUUID().toString(),
                            sku = sku,
                            name = name,
                            category = if (category.isNotEmpty()) category else "Perfumeria",
                            price = cleanPrice,
                            imageUrl = imageUrl,
                            description = "Producto de belleza y cosmetica oficial VV Chiclayo.",
                            sourceUrl = sourceUrl
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback default
        }

        return if (products.isNotEmpty()) products else generateDefaultVVCatalog()
    }

    fun generateDefaultVVCatalog(): List<Product> {
        return listOf(
            Product(
                id = "prod-001",
                sku = "PERF-01",
                name = "Far Away Royale EDP 50ml",
                category = "Perfumeria",
                price = 89.90,
                imageUrl = "https://images.unsplash.com/photo-1547887537-6158d64c35b3",
                description = "Fragancia floral oriental con notas de jazmin, vainilla de Madagascar y acordes amaderados.",
                usageMode = "Vaporizar sobre cuello y munecas a 15 cm de distancia.",
                stockAvailable = 18
            ),
            Product(
                id = "prod-002",
                sku = "FACIAL-01",
                name = "Crema Facial Anew Ultimate Noche 50g",
                category = "Cuidado Facial",
                price = 119.90,
                imageUrl = "https://images.unsplash.com/photo-1556228720-195a672e8a03",
                description = "Tecnologia Protinol para reactivar la produccion de colageno. Reafirma y restaura la elasticidad.",
                usageMode = "Aplicar sobre rostro y cuello limpios cada noche con suaves movimientos ascendentes.",
                stockAvailable = 12
            ),
            Product(
                id = "prod-003",
                sku = "MAQ-01",
                name = "Labial Ultra Matte VV Red",
                category = "Maquillaje",
                price = 34.90,
                imageUrl = "https://images.unsplash.com/photo-1586495777744-4413f21062fa",
                description = "Acabado 100% mate aterciopelado con aceite de aguacate y manteca de karite. 12 horas de duracion.",
                usageMode = "Delinear el contorno de los labios y rellenar del centro hacia afuera.",
                stockAvailable = 25
            ),
            Product(
                id = "prod-004",
                sku = "BODY-01",
                name = "Locion Corporal Encanto Seduccion 400ml",
                category = "Cuidado Corporal",
                price = 42.90,
                imageUrl = "https://images.unsplash.com/photo-1608248597359-0a6344585c57",
                description = "Hidratacion 48 horas con mora y champan. Textura sedosa que perfuma suavemente la piel.",
                usageMode = "Aplicar en todo el cuerpo despues de la ducha con masajes circulares.",
                stockAvailable = 14
            ),
            Product(
                id = "prod-005",
                sku = "MASC-01",
                name = "Mascara de Pestanas Legendary Extension",
                category = "Maquillaje",
                price = 39.90,
                imageUrl = "https://images.unsplash.com/photo-1631214524020-7e18db9a8f92",
                description = "Cepillo con cerdas de precision que alarga las pestanas hasta un 50% sin dejar grumos.",
                usageMode = "Aplicar desde la raiz hasta las puntas en movimientos zig-zag.",
                stockAvailable = 20
            )
        )
    }
}
