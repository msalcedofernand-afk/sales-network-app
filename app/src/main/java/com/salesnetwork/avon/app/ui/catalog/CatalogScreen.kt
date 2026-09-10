package com.salesnetwork.avon.app.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.salesnetwork.avon.app.domain.model.Product
import com.salesnetwork.avon.app.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    products: List<Product>,
    isScraping: Boolean,
    onSyncWebCatalogClick: () -> Unit,
    onProductSelectedForOrder: (Product) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }
    var detailProduct by remember { mutableStateOf<Product?>(null) }

    val categories = listOf("Todos") + products.map { it.category }.distinct().sorted()

    val filteredProducts = products.filter { p ->
        val matchesSearch = searchQuery.isBlank() ||
            p.name.contains(searchQuery, ignoreCase = true) ||
            p.sku.contains(searchQuery, ignoreCase = true)
        val matchesCat = selectedCategory == "Todos" || p.category.equals(selectedCategory, ignoreCase = true)
        matchesSearch && matchesCat
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(S.M),
        verticalArrangement = Arrangement.spacedBy(S.SM)
    ) {
        SectionIntro("VV / Colecciones", "Encuentra tu proxima venta", "${products.size} productos para explorar y compartir.")

        // Refreshes the catalog already approved in Supabase; it never scrapes on-device.
        TextButton(onClick = onSyncWebCatalogClick, enabled = !isScraping, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(S.IconS))
            Spacer(Modifier.width(S.S))
            Text(if (isScraping) "Actualizando..." else "Actualizar catalogo", fontSize = S.TextSmall)
        }

        // Search
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar producto o SKU...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(S.IconM)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true, shape = SH.Input
        )

        // Category tabs
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
            edgePadding = S.XXS, divider = {}
        ) {
            categories.forEach { cat ->
                Tab(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    text = {
                        Text(
                            cat, fontSize = S.TextSmall,
                            fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Products grid
        if (filteredProducts.isEmpty()) {
            EmptyState(
                icon = Icons.Default.ShoppingCart,
                title = "Sin productos",
                description = "No se encontraron productos en el catalogo."
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                horizontalArrangement = Arrangement.spacedBy(S.S),
                verticalArrangement = Arrangement.spacedBy(S.S),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductCard(product = product, onClick = { detailProduct = product })
                }
            }
        }
    }

    // Product detail dialog
    if (detailProduct != null) {
        val p = detailProduct!!
        AlertDialog(
            onDismissRequest = { detailProduct = null },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(S.S)) {
                    StatusBadge(text = p.category, color = MaterialTheme.colorScheme.primary, backgroundColor = MaterialTheme.colorScheme.primaryContainer)
                    Text(p.name, fontWeight = FontWeight.ExtraBold, fontSize = S.TextTitle)
                    Text("SKU: ${p.sku}", fontSize = S.TextSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(S.S)) {
                    Text(
                        text = if (p.description.isNotBlank()) p.description else "Producto oficial del portafolio VV de alta calidad.",
                        fontSize = S.TextBody
                    )
                    HorizontalDivider()
                    Text("MODO DE USO:", fontWeight = FontWeight.Bold, fontSize = S.TextCaption, color = MaterialTheme.colorScheme.primary)
                    Text(p.usageMode, fontSize = S.TextSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Stock:", fontSize = S.TextSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StatusBadge(text = p.stockLabel, color = if (p.canOrder) C.Success else C.Error, backgroundColor = if (p.canOrder) C.SuccessLight else C.ErrorLight)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Precio Campana:", fontSize = S.TextBody, fontWeight = FontWeight.SemiBold)
                        Text("S/ ${String.format("%.2f", p.price)}", fontWeight = FontWeight.Black, fontSize = S.TextHeadline, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Button(enabled = p.canOrder, onClick = { onProductSelectedForOrder(p); detailProduct = null }, shape = SH.Button) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(S.IconS))
                    Spacer(modifier = Modifier.width(S.XS))
                    Text(if (p.canOrder) "+ Agregar a Pedido" else "Producto agotado", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { detailProduct = null }) { Text("Cerrar") }
            }
        )
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SH.Card,
        border = B.cardBorder(),
        elevation = CardDefaults.cardElevation(S.ElevationNone),
        onClick = onClick
    ) {
        Column {
            if (product.primaryImageUrl.isNotBlank()) {
                AsyncImage(
                    model = product.primaryImageUrl, contentDescription = product.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color.White)
                )
            }
            Column(modifier = Modifier.padding(S.M), verticalArrangement = Arrangement.spacedBy(S.XS)) {
                StatusBadge(text = product.category, color = MaterialTheme.colorScheme.primary, backgroundColor = MaterialTheme.colorScheme.primaryContainer)

                Text(product.name, fontWeight = FontWeight.Bold, fontSize = S.TextBody, maxLines = 2, minLines = 2)

                Text("SKU: ${product.sku}", fontSize = S.TextCaption, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Text(
                    product.stockLabel,
                    fontSize = S.TextCaption,
                    color = if (product.canOrder) C.Success else C.Error,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "S/ ${String.format("%.2f", product.price)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = S.TextTitle,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(Icons.Default.Info, contentDescription = "Ver Detalle", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(S.IconS))
                }
            }
        }
    }
}
