package com.salesnetwork.avon.app.ui.order

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.salesnetwork.avon.app.domain.model.*
import com.salesnetwork.avon.app.ui.*
import com.salesnetwork.avon.app.utils.ContactActionHelper
import com.salesnetwork.avon.app.utils.formatMoney

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
    statusMessage: String? = null,
    onUpdateStatus: (String, OrderStatus, String?, Uri?, PaymentMethod?) -> Unit,
    onCreateOrder: (customerId: String, customerName: String, items: List<OrderItem>, method: PaymentMethod, paid: Double) -> Unit = { _, _, _, _, _ -> },
    onShareTicket: (Order) -> String
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var orderKpiTitle by remember { mutableStateOf<String?>(null) }
    var orderKpiBody by remember { mutableStateOf<String?>(null) }

    var selectedCustomerName by remember { mutableStateOf("") }
    var selectedCustomerId by remember { mutableStateOf("") }
    var cartItems by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.YAPE) }

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
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = SH.Avatar
            ) {
                Row(modifier = Modifier.padding(horizontal = S.SM), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(S.IconM))
                    Spacer(modifier = Modifier.width(S.XS))
                    Text("Nuevo pedido", fontWeight = FontWeight.Bold, fontSize = S.TextBody)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(S.M),
            verticalArrangement = Arrangement.spacedBy(S.SM)
        ) {
            SectionIntro("VV / Ventas", "Tu negocio en movimiento", "Pedidos, cobros y entregas en un solo lugar.")

            statusMessage?.let {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = SH.Badge, modifier = Modifier.fillMaxWidth()) {
                    Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(S.SM), fontSize = S.TextSmall, fontWeight = FontWeight.Medium)
                }
            }

            // KPIs Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(S.S)) {
                KpiCard(
                    label = "Ventas",
                    value = "S/ ${formatMoney(totalSales)}",
                    icon = Icons.Default.ReceiptLong,
                    onClick = {
                        orderKpiTitle = "Ventas de Campana"
                        orderKpiBody = "Facturacion total de S/ ${formatMoney(totalSales)} en ${orders.size} pedidos."
                    }
                )
                KpiCard(
                    label = "Ganancia",
                    value = "S/ ${formatMoney(totalProfit)}",
                    valueColor = C.Success,
                    icon = Icons.Default.Payments,
                    onClick = {
                        orderKpiTitle = "Ganancia Estimada"
                        orderKpiBody = "Comision directa: S/ ${formatMoney(directCommission)}\nSobrecomision red (5%): S/ ${formatMoney(networkCommission)}\nTotal: S/ ${formatMoney(totalProfit)}"
                    }
                )
                KpiCard(
                    label = "Por Cobrar",
                    value = "S/ ${formatMoney(pendingDebt)}",
                    valueColor = if (pendingDebt > 0) C.Error else C.Success,
                    icon = Icons.Default.Payments,
                    onClick = {
                        orderKpiTitle = "Saldos por Cobrar"
                        orderKpiBody = if (pendingDebt > 0) "Hay S/ ${formatMoney(pendingDebt)} pendientes en $pendingCount pedidos." else "Todos los pedidos estan cobrados."
                    }
                )
            }

            // KPI Dialog
            if (orderKpiTitle != null) {
                AlertDialog(
                    onDismissRequest = { orderKpiTitle = null },
                    title = { Text(orderKpiTitle!!, fontWeight = FontWeight.Bold, fontSize = S.TextTitle) },
                    text = { Text(orderKpiBody.orEmpty(), fontSize = S.TextBody) },
                    confirmButton = { TextButton(onClick = { orderKpiTitle = null }) { Text("Entendido") } }
                )
            }

            // Orders list
            if (orders.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.ReceiptLong,
                    title = "Sin pedidos",
                    description = "Toca 'Nuevo pedido' para registrar compras de tus clientes.",
                    actionLabel = "Crear Pedido",
                    onAction = {
                        if (availableCustomers.isNotEmpty()) {
                            selectedCustomerId = availableCustomers[0].id
                            selectedCustomerName = availableCustomers[0].name
                        }
                        cartItems = emptyMap()
                        showCreateDialog = true
                    }
                )
            } else {
                SectionHeader(title = "Pedidos", count = orders.size)

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(S.S)
                ) {
                    items(orders) { order ->
                        OrderCard(
                            order = order,
                            onStatusUpdate = { st, reason, proof, method -> onUpdateStatus(order.id, st, reason, proof, method) },
                            onShare = {
                                val ticket = onShareTicket(order)
                                ContactActionHelper.openWhatsAppChat(context, "", ticket)
                            }
                        )
                    }
                }
            }
        }
    }

    // Create order dialog
    if (showCreateDialog) {
        val totalCart = cartItems.entries.sumOf { entry ->
            val p = availableProducts.firstOrNull { it.sku == entry.key }
            (p?.price ?: 0.0) * entry.value
        }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(S.IconM))
                    Spacer(modifier = Modifier.width(S.S))
                    Text("Nuevo pedido", fontWeight = FontWeight.Bold, fontSize = S.TextTitle)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(S.SM)
                ) {
                    // Step 1: Customer
                    Text("1. Cliente:", fontSize = S.TextSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (availableCustomers.isEmpty()) {
                        Text("Sin clientes. Agrega uno en Clientes primero.", fontSize = S.TextBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        availableCustomers.forEach { cust ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                RadioButton(selected = selectedCustomerId == cust.id, onClick = { selectedCustomerId = cust.id; selectedCustomerName = cust.name })
                                Spacer(modifier = Modifier.width(S.XS))
                                Text(cust.name, fontSize = S.TextSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    HorizontalDivider()

                    // Step 2: Products
                    Text("2. Productos:", fontSize = S.TextSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    availableProducts.filter { it.canOrder }.forEach { product ->
                        val qty = cartItems[product.sku] ?: 0
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontSize = S.TextSmall, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text("S/ ${formatMoney(product.price)}", fontSize = S.TextSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (qty > 0) cartItems = cartItems.toMutableMap().apply { if (qty == 1) remove(product.sku) else put(product.sku, qty - 1) } }, modifier = Modifier.size(48.dp)) {
                                    Text("-", fontWeight = FontWeight.Bold, fontSize = S.TextSubtitle)
                                }
                                Text("$qty", fontSize = S.TextSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = S.XS))
                                IconButton(onClick = { cartItems = cartItems.toMutableMap().apply { put(product.sku, qty + 1) } }, modifier = Modifier.size(48.dp)) {
                                    Text("+", fontWeight = FontWeight.Bold, fontSize = S.TextSubtitle)
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Total
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL:", fontWeight = FontWeight.Bold, fontSize = S.TextBody)
                        Text("S/ ${formatMoney(totalCart)}", fontWeight = FontWeight.ExtraBold, fontSize = S.TextTitle, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("La comisión se calculará en el servidor al confirmar.", fontSize = S.TextSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Step 3: Payment
                    Text("3. Metodo de Cobro:", fontSize = S.TextSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(S.XS)) {
                        FilterChip(selected = selectedPaymentMethod == PaymentMethod.YAPE, onClick = { selectedPaymentMethod = PaymentMethod.YAPE }, label = { Text("Yape", fontSize = S.TextSmall) }, shape = SH.Chip)
                        FilterChip(selected = selectedPaymentMethod == PaymentMethod.PLIN, onClick = { selectedPaymentMethod = PaymentMethod.PLIN }, label = { Text("Plin", fontSize = S.TextSmall) }, shape = SH.Chip)
                        FilterChip(selected = selectedPaymentMethod == PaymentMethod.EFECTIVO, onClick = { selectedPaymentMethod = PaymentMethod.EFECTIVO }, label = { Text("Efectivo", fontSize = S.TextSmall) }, shape = SH.Chip)
                        FilterChip(selected = selectedPaymentMethod == PaymentMethod.PENDIENTE, onClick = { selectedPaymentMethod = PaymentMethod.PENDIENTE }, label = { Text("Fiado", fontSize = S.TextSmall) }, shape = SH.Chip)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = selectedCustomerId.isNotBlank() && cartItems.isNotEmpty(),
                    onClick = {
                        val itemsList = cartItems.mapNotNull { entry ->
                            val p = availableProducts.firstOrNull { it.sku == entry.key }
                            if (p != null) OrderItem(productSku = p.sku, productName = p.name, unitPrice = p.price, quantity = entry.value) else null
                        }
                        val paid = if (selectedPaymentMethod == PaymentMethod.PENDIENTE) 0.0 else totalCart
                        onCreateOrder(selectedCustomerId, selectedCustomerName, itemsList, selectedPaymentMethod, paid)
                        showCreateDialog = false
                    },
                    shape = SH.Button
                ) { Text("Crear Pedido", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun OrderCard(
    order: Order,
    onStatusUpdate: (OrderStatus, String?, Uri?, PaymentMethod?) -> Unit,
    onShare: () -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    var detailStatus by remember { mutableStateOf<OrderStatus?>(null) }
    var detailText by rememberSaveable { mutableStateOf("") }
    var detailPaymentMethod by remember { mutableStateOf(PaymentMethod.YAPE) }
    val proofPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onStatusUpdate(OrderStatus.COBRADO, null, uri, detailPaymentMethod)
    }
    val deliveryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onStatusUpdate(OrderStatus.ENTREGADO, null, uri, null)
    }

    val statusColor = when (order.status) {
        OrderStatus.COBRADO -> C.Success
        OrderStatus.CONFIRMADO -> C.Info
        OrderStatus.ENTREGADO -> Color(0xFF1976D2)
        OrderStatus.PENDIENTE -> C.Warning
        OrderStatus.CANCELADO -> C.Error
        OrderStatus.DEVUELTO -> C.Error
    }
    val statusBg = when (order.status) {
        OrderStatus.COBRADO -> C.SuccessLight
        OrderStatus.CONFIRMADO -> C.InfoLight
        OrderStatus.ENTREGADO -> C.InfoLight
        OrderStatus.PENDIENTE -> C.WarningLight
        OrderStatus.CANCELADO -> C.ErrorLight
        OrderStatus.DEVUELTO -> C.ErrorLight
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SH.Card,
        border = B.cardBorder(),
        elevation = CardDefaults.cardElevation(S.ElevationNone)
    ) {
        Column(modifier = Modifier.padding(S.M), verticalArrangement = Arrangement.spacedBy(S.S)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(order.customerName, fontWeight = FontWeight.Bold, fontSize = S.TextBody)
                    Text("${order.campaignCode} | ${order.createdAt}", fontSize = S.TextSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    Surface(onClick = { showStatusMenu = true }, color = statusColor, shape = SH.Pill) {
                        Text(order.status.name, fontSize = S.TextCaption, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = S.SM, vertical = S.XXS))
                    }
                    DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                        order.status.allowedNext().forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st.name, fontSize = S.TextSmall, fontWeight = if (st == order.status) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    showStatusMenu = false
                                    if (st in listOf(OrderStatus.CANCELADO, OrderStatus.COBRADO, OrderStatus.ENTREGADO, OrderStatus.DEVUELTO)) {
                                        detailText = ""
                                        detailStatus = st
                                    } else onStatusUpdate(st, null, null, null)
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Items
            order.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${item.quantity}x ${item.productName}", fontSize = S.TextSmall)
                    Text("S/ ${formatMoney(item.subtotal)}", fontSize = S.TextSmall, fontWeight = FontWeight.SemiBold)
                }
            }

            // Footer
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Total: S/ ${formatMoney(order.totalAmount)}", fontWeight = FontWeight.ExtraBold, fontSize = S.TextBody, color = MaterialTheme.colorScheme.primary)
                    if (order.remainingDebt > 0) {
                        StatusBadge(text = "Debe: S/ ${formatMoney(order.remainingDebt)}", color = C.Error, backgroundColor = C.ErrorLight)
                    } else {
                        StatusBadge(text = "Pagado con ${order.paymentMethod.name}", color = C.Success, backgroundColor = C.SuccessLight)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(S.XS)) {
                    Button(onClick = onShare, colors = ButtonDefaults.buttonColors(containerColor = C.WhatsApp), contentPadding = PaddingValues(horizontal = S.SM, vertical = S.XS), shape = SH.ButtonSmall) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(S.IconXS), tint = Color.White)
                        Spacer(modifier = Modifier.width(S.XS))
                        Text("Ticket", fontSize = S.TextSmall, color = Color.White)
                    }
                }
            }
        }
    }
    if (detailStatus != null) {
        AlertDialog(
            onDismissRequest = { detailStatus = null },
            title = { Text(when (detailStatus) {
                OrderStatus.COBRADO -> "Registrar cobro"
                OrderStatus.ENTREGADO -> "Registrar entrega"
                OrderStatus.DEVUELTO -> "Registrar devolución"
                else -> "Cancelar pedido"
            }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(S.S)) {
                    if (detailStatus == OrderStatus.COBRADO) {
                        Text("Método de pago", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(S.XS)) {
                            listOf(PaymentMethod.YAPE, PaymentMethod.PLIN, PaymentMethod.EFECTIVO).forEach { method ->
                                FilterChip(selected = detailPaymentMethod == method, onClick = { detailPaymentMethod = method }, label = { Text(method.name) })
                            }
                        }
                        Text(if (detailPaymentMethod == PaymentMethod.EFECTIVO) "Puedes confirmar sin fotografía." else "Selecciona una fotografía del comprobante.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (detailStatus == OrderStatus.CANCELADO || detailStatus == OrderStatus.DEVUELTO) {
                        OutlinedTextField(
                            value = detailText,
                            onValueChange = { detailText = it },
                            label = { Text("Motivo") },
                            supportingText = { Text("Mínimo 3 caracteres.") },
                            singleLine = false,
                            minLines = 2
                        )
                    }
                    if (detailStatus == OrderStatus.ENTREGADO) Text("Puedes incluir una fotografía como evidencia de entrega.")
                }
            },
            confirmButton = {
                Button(
                    enabled = detailStatus in listOf(OrderStatus.COBRADO, OrderStatus.ENTREGADO) || detailText.trim().length >= 3,
                    onClick = {
                        val selected = detailStatus ?: return@Button
                        when {
                            selected == OrderStatus.COBRADO && detailPaymentMethod != PaymentMethod.EFECTIVO -> proofPicker.launch("image/*")
                            selected == OrderStatus.COBRADO -> onStatusUpdate(selected, null, null, detailPaymentMethod)
                            selected == OrderStatus.ENTREGADO -> deliveryPicker.launch("image/*")
                            else -> onStatusUpdate(selected, detailText.trim(), null, null)
                        }
                        detailStatus = null
                    }
                ) { Text(if (detailStatus == OrderStatus.COBRADO || detailStatus == OrderStatus.ENTREGADO) "Continuar" else "Guardar") }
            },
            dismissButton = {
                Row {
                    if (detailStatus == OrderStatus.ENTREGADO) {
                        TextButton(onClick = { onStatusUpdate(OrderStatus.ENTREGADO, null, null, null); detailStatus = null }) { Text("Sin foto") }
                    }
                    TextButton(onClick = { detailStatus = null }) { Text("Volver") }
                }
            }
        )
    }
}
