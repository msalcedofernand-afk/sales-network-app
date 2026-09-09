package com.salesnetwork.avon.app.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.salesnetwork.avon.app.ui.SectionIntro
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salesnetwork.avon.app.domain.model.CustomerContact
import com.salesnetwork.avon.app.domain.model.Order
import com.salesnetwork.avon.app.domain.model.OrderItem
import com.salesnetwork.avon.app.domain.model.OrderStatus
import com.salesnetwork.avon.app.domain.model.PaymentMethod
import com.salesnetwork.avon.app.domain.model.Product
import com.salesnetwork.avon.app.utils.ContactActionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    orders: List<Order>,
    totalSales: Double,
    directCommission: Double,
    networkCommission: Double,
    totalProfit: Double,
    pendingDebt: Double,
    pendingCount: Int,
    availableCustomers: List<CustomerContact> = emptyList(),
    availableProducts: List<Product> = emptyList(),
    onUpdateStatus: (String, OrderStatus) -> Unit,
    onRegisterPayment: (String, PaymentMethod, Double) -> Unit = { _, _, _ -> },
    onCreateOrder: (customerId: String, customerName: String, items: List<OrderItem>, method: PaymentMethod, paid: Double) -> Unit = { _, _, _, _, _ -> },
    onDeleteOrder: (String) -> Unit = {},
    onShareTicket: (Order) -> String
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var orderToDelete by remember { mutableStateOf<Order?>(null) }
    var orderKpiTitle by remember { mutableStateOf<String?>(null) }
    var orderKpiBody by remember { mutableStateOf<String?>(null) }

    // Estado del Creador de Pedido
    var selectedCustomerName by remember { mutableStateOf("") }
    var selectedCustomerId by remember { mutableStateOf("") }
    var cartItems by remember { mutableStateOf<Map<String, Int>>(emptyMap()) } // SKU to quantity
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.YAPE) }
    var enteredAmountPaid by remember { mutableStateOf("") }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (availableCustomers.isNotEmpty()) {
                        selectedCustomerId = availableCustomers[0].id
                        selectedCustomerName = availableCustomers[0].name
                    }
                    cartItems = emptyMap()
                    enteredAmountPaid = ""
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo pedido", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
        ) {
            SectionIntro("VV / Ventas", "Tu negocio en movimiento", "Pedidos, cobros y entregas en un solo lugar.")

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjetas de Resumen Financiero
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            orderKpiTitle = "Ventas de Campaña"
                            orderKpiBody = "Facturación total de S/ ${String.format("%.2f", totalSales)} distribuida en ${orders.size} pedidos registrados."
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Ventas", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "S/ ${String.format("%.2f", totalSales)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            orderKpiTitle = "Ganancia Estimada"
                            orderKpiBody = "Comisión directa: S/ ${String.format("%.2f", directCommission)}\nSobrecomisión de red (5%): S/ ${String.format("%.2f", networkCommission)}\nTotal estimado: S/ ${String.format("%.2f", totalProfit)}"
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Ganancia", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "S/ ${String.format("%.2f", totalProfit)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            orderKpiTitle = "Saldos por Cobrar"
                            orderKpiBody = if (pendingDebt > 0) {
                                "Hay S/ ${String.format("%.2f", pendingDebt)} pendientes de cobro por Yape, Plin o efectivo en $pendingCount pedidos."
                            } else {
                                "Todos los pedidos de la campaña se encuentran totalmente cobrados."
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = if (pendingDebt > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Por Cobrar", fontSize = 13.sp, color = if (pendingDebt > 0) Color(0xFFC62828) else Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "S/ ${String.format("%.2f", pendingDebt)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (pendingDebt > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                    }
                }
            }

            if (orderKpiTitle != null) {
                AlertDialog(
                    onDismissRequest = { orderKpiTitle = null },
                    title = { Text(orderKpiTitle!!, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    text = { Text(orderKpiBody.orEmpty(), fontSize = 13.sp) },
                    confirmButton = {
                        TextButton(onClick = { orderKpiTitle = null }) {
                            Text("Entendido")
                        }
                    }
                )
            }

            if (orderToDelete != null) {
                AlertDialog(
                    onDismissRequest = { orderToDelete = null },
                    title = { Text("Eliminar Pedido", fontWeight = FontWeight.Bold) },
                    text = { Text("¿Deseas eliminar el pedido de '${orderToDelete?.customerName}' por S/ ${String.format("%.2f", orderToDelete?.totalAmount ?: 0.0)}? Esta acción cancelará el registro.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                orderToDelete?.id?.let { onDeleteOrder(it) }
                                orderToDelete = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC62828))
                        ) {
                            Text("Eliminar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { orderToDelete = null }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay pedidos registrados en esta campana.\nToca 'Nuevo pedido' para registrar compras de tus clientes.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(orders) { order ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = order.customerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "${order.campaignCode} | Fecha: ${order.createdAt}",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    var showStatusMenu by remember { mutableStateOf(false) }
                                    Box {
                                        Surface(
                                            onClick = { showStatusMenu = true },
                                            color = when (order.status) {
                                                OrderStatus.COBRADO -> Color(0xFF2E7D32)
                                                OrderStatus.CONFIRMADO -> Color(0xFF1565C0)
                                                OrderStatus.ENTREGADO -> Color(0xFF1976D2)
                                                OrderStatus.PENDIENTE -> Color(0xFFE65100)
                                                OrderStatus.CANCELADO -> Color(0xFFC62828)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = order.status.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = showStatusMenu,
                                            onDismissRequest = { showStatusMenu = false }
                                        ) {
                                            OrderStatus.values().forEach { st ->
                                                DropdownMenuItem(
                                                    text = { Text(st.name, fontSize = 12.sp, fontWeight = if (st == order.status) FontWeight.Bold else FontWeight.Normal) },
                                                    onClick = {
                                                        onUpdateStatus(order.id, st)
                                                        showStatusMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                order.items.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${item.quantity}x ${item.productName}", fontSize = 12.sp)
                                        Text("S/ ${String.format("%.2f", item.subtotal)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Total: S/ ${String.format("%.2f", order.totalAmount)}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (order.remainingDebt > 0) {
                                            Text(
                                                text = "Debe: S/ ${String.format("%.2f", order.remainingDebt)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFC62828)
                                            )
                                        } else {
                                            Text(
                                                text = "Pagado con ${order.paymentMethod.name}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = {
                                                val ticket = onShareTicket(order)
                                                ContactActionHelper.openWhatsAppChat(context, "", ticket)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ticket", fontSize = 13.sp, color = Color.White)
                                        }

                                        if (order.remainingDebt > 0) {
                                            OutlinedButton(
                                                onClick = {
                                                    onRegisterPayment(order.id, PaymentMethod.YAPE, order.totalAmount)
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Cobrar", fontSize = 13.sp)
                                            }
                                        }

                                        IconButton(
                                            onClick = { orderToDelete = order },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Eliminar Pedido",
                                                tint = Color(0xFFC62828),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Creador Interactivo de Pedidos
    if (showCreateDialog) {
        val totalCart = cartItems.entries.sumOf { entry ->
            val p = availableProducts.firstOrNull { it.sku == entry.key }
            (p?.price ?: 0.0) * entry.value
        }
        val estimatedLeaderProfit = totalCart * 0.35 // 30% direct + 5% network

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nuevo pedido", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("1. Selecciona el Cliente:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    if (availableCustomers.isEmpty()) {
                        Text("No tienes clientes creados. Primero agrega uno en Clientes.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        availableCustomers.take(4).forEach { cust ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = selectedCustomerId == cust.id,
                                    onClick = {
                                        selectedCustomerId = cust.id
                                        selectedCustomerName = cust.name
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(cust.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    HorizontalDivider()

                    Text("2. Selecciona Productos del Catalogo:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    availableProducts.take(4).forEach { product ->
                        val qty = cartItems[product.sku] ?: 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text("S/ ${String.format("%.2f", product.price)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (qty > 0) {
                                            cartItems = cartItems.toMutableMap().apply {
                                                if (qty == 1) remove(product.sku) else put(product.sku, qty - 1)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }

                                Text("$qty", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))

                                IconButton(
                                    onClick = {
                                        cartItems = cartItems.toMutableMap().apply { put(product.sku, qty + 1) }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL PEDIDO:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "S/ ${String.format("%.2f", totalCart)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tu Ganancia Estimada:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "S/ ${String.format("%.2f", estimatedLeaderProfit)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Text("3. Metodo de Cobro:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = selectedPaymentMethod == PaymentMethod.YAPE,
                            onClick = { selectedPaymentMethod = PaymentMethod.YAPE },
                            label = { Text("Yape", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = selectedPaymentMethod == PaymentMethod.PLIN,
                            onClick = { selectedPaymentMethod = PaymentMethod.PLIN },
                            label = { Text("Plin", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = selectedPaymentMethod == PaymentMethod.EFECTIVO,
                            onClick = { selectedPaymentMethod = PaymentMethod.EFECTIVO },
                            label = { Text("Efectivo", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = selectedPaymentMethod == PaymentMethod.PENDIENTE,
                            onClick = { selectedPaymentMethod = PaymentMethod.PENDIENTE },
                            label = { Text("Fiado", fontSize = 12.sp) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = selectedCustomerId.isNotBlank() && cartItems.isNotEmpty(),
                    onClick = {
                        val itemsList = cartItems.mapNotNull { entry ->
                            val p = availableProducts.firstOrNull { it.sku == entry.key }
                            if (p != null) {
                                OrderItem(
                                    productSku = p.sku,
                                    productName = p.name,
                                    unitPrice = p.price,
                                    quantity = entry.value
                                )
                            } else null
                        }
                        val paid = if (selectedPaymentMethod == PaymentMethod.PENDIENTE) 0.0 else totalCart
                        onCreateOrder(
                            selectedCustomerId,
                            selectedCustomerName,
                            itemsList,
                            selectedPaymentMethod,
                            paid
                        )
                        showCreateDialog = false
                    }
                ) {
                    Text("Crear Pedido")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
