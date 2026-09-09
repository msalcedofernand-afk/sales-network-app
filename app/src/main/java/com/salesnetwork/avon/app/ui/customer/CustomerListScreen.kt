package com.salesnetwork.avon.app.ui.customer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.salesnetwork.avon.app.domain.model.CustomerContact
import com.salesnetwork.avon.app.ui.*
import com.salesnetwork.avon.app.ui.viewmodel.ChiclayoZone
import com.salesnetwork.avon.app.utils.ContactActionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    customers: List<CustomerContact>,
    onAddCustomer: (CustomerContact) -> Unit,
    onDeleteCustomer: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var zoneMenuExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var customerToDelete by remember { mutableStateOf<CustomerContact?>(null) }

    val chiclayoZones = remember {
        listOf(
            ChiclayoZone("Centro / Balta", "Av. Jose Balta, Chiclayo", -6.7725, -79.8390),
            ChiclayoZone("Bolognesi / Santa Victoria", "Av. Bolognesi 450, Chiclayo", -6.7750, -79.8420),
            ChiclayoZone("Luis Gonzales / Mercado", "Av. Luis Gonzales 890, Chiclayo", -6.7680, -79.8375),
            ChiclayoZone("La Victoria / Grau", "Av. Miguel Grau 350, La Victoria", -6.7820, -79.8460),
            ChiclayoZone("JLO / Moshoqueque", "Av. Augusto B. Leguia, JLO", -6.7580, -79.8350),
            ChiclayoZone("Pimentel / Balneario", "Av. Quinones, Pimentel", -6.8333, -79.9333)
        )
    }

    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }
    var newNotes by remember { mutableStateOf("") }
    var selectedZone by remember { mutableStateOf(chiclayoZones[0]) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newName = ""
                    newPhone = ""
                    newAddress = selectedZone.addressHint
                    newNotes = ""
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = SH.Avatar
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Cliente")
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
            SectionIntro("VV / Relaciones", "Cada contacto cuenta", "Organiza tus clientes y prepara tu proxima visita.")

            if (customers.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Person,
                    title = "Sin clientes registrados",
                    description = "Presiona + para agregar tu primer contacto con GPS.",
                    actionLabel = "Agregar Cliente",
                    onAction = {
                        newName = ""
                        newPhone = ""
                        newAddress = selectedZone.addressHint
                        newNotes = ""
                        showAddDialog = true
                    }
                )
            } else {
                SectionHeader(title = "Directorio", count = customers.size)

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(S.S)
                ) {
                    items(customers) { customer ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = SH.Card,
                            border = B.cardBorder(),
                            elevation = CardDefaults.cardElevation(S.ElevationNone)
                        ) {
                            Column(modifier = Modifier.padding(S.M), verticalArrangement = Arrangement.spacedBy(S.S)) {
                                // Header: name + route badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = S.TextBody)

                                    StatusBadge(
                                        text = if (customer.estimatedMinutes != null) "~${customer.estimatedMinutes} min" else "Ruta sin calcular",
                                        color = if (customer.estimatedMinutes != null) C.Success else C.Warning,
                                        backgroundColor = if (customer.estimatedMinutes != null) C.SuccessLight else C.WarningLight
                                    )
                                }

                                // Address
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(S.IconXS), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(S.XS))
                                    Text(customer.address, fontSize = S.TextSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                // Notes
                                if (customer.notes.isNotBlank()) {
                                    Text(customer.notes, fontSize = S.TextSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                HorizontalDivider()

                                // Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(S.XS)) {
                                        OutlinedButton(
                                            onClick = { ContactActionHelper.openPhoneDialer(context, customer.phone) },
                                            contentPadding = PaddingValues(horizontal = S.SM, vertical = S.XS),
                                            shape = SH.ButtonSmall
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(S.IconXS))
                                            Spacer(modifier = Modifier.width(S.XS))
                                            Text("Llamar", fontSize = S.TextSmall)
                                        }

                                        Button(
                                            onClick = { ContactActionHelper.openWhatsAppChat(context, customer.whatsapp) },
                                            colors = ButtonDefaults.buttonColors(containerColor = C.WhatsApp),
                                            contentPadding = PaddingValues(horizontal = S.SM, vertical = S.XS),
                                            shape = SH.ButtonSmall
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(S.IconXS), tint = Color.White)
                                            Spacer(modifier = Modifier.width(S.XS))
                                            Text("WhatsApp", fontSize = S.TextSmall, color = Color.White)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Button(
                                            onClick = {
                                                ContactActionHelper.openTurnByTurnNavigation(context, customer.address, customer.latitude, customer.longitude)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            contentPadding = PaddingValues(horizontal = S.SM, vertical = S.XS),
                                            shape = SH.ButtonSmall
                                        ) {
                                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(S.IconXS), tint = Color.White)
                                            Spacer(modifier = Modifier.width(S.XS))
                                            Text("Navegar", fontSize = S.TextSmall, color = Color.White)
                                        }

                                        IconButton(
                                            onClick = { customerToDelete = customer },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = C.Error, modifier = Modifier.size(S.IconS))
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

    // Delete confirmation dialog
    if (customerToDelete != null) {
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Eliminar Cliente", fontWeight = FontWeight.Bold, fontSize = S.TextTitle) },
            text = { Text("Deseas eliminar a '${customerToDelete?.name}' de tu directorio?", fontSize = S.TextBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        customerToDelete?.id?.let { onDeleteCustomer(it) }
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = C.Error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    // Add customer dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(S.IconM))
                    Spacer(modifier = Modifier.width(S.S))
                    Text("Nuevo cliente", fontWeight = FontWeight.Bold, fontSize = S.TextTitle)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(S.SM)
                ) {
                    OutlinedTextField(
                        value = newName, onValueChange = { newName = it },
                        label = { Text("Nombre Completo") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = SH.Input
                    )
                    OutlinedTextField(
                        value = newPhone, onValueChange = { newPhone = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        label = { Text("Telefono / WhatsApp") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = SH.Input
                    )

                    Text("Zona GPS en Chiclayo:", fontSize = S.TextSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Box {
                        OutlinedButton(
                            onClick = { zoneMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = SH.Input
                        ) { Text(selectedZone.name, fontSize = S.TextBody) }
                        DropdownMenu(expanded = zoneMenuExpanded, onDismissRequest = { zoneMenuExpanded = false }) {
                            chiclayoZones.forEach { zone ->
                                DropdownMenuItem(
                                    text = { Text(zone.name, fontSize = S.TextBody) },
                                    onClick = { selectedZone = zone; newAddress = zone.addressHint; zoneMenuExpanded = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newAddress, onValueChange = { newAddress = it },
                        label = { Text("Direccion") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = SH.Input
                    )
                    OutlinedTextField(
                        value = newNotes, onValueChange = { newNotes = it },
                        label = { Text("Notas / Preferencias") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = SH.Input
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = newName.isNotBlank() && newPhone.count { it.isDigit() } >= 7,
                    onClick = {
                        if (newName.isNotBlank() && newPhone.isNotBlank()) {
                            onAddCustomer(
                                CustomerContact(
                                    id = "c-${System.currentTimeMillis()}",
                                    name = newName.trim(), phone = newPhone.trim(), whatsapp = newPhone.trim(),
                                    address = newAddress.trim(), city = "Chiclayo",
                                    latitude = selectedZone.lat, longitude = selectedZone.lng, notes = newNotes.trim()
                                )
                            )
                            showAddDialog = false
                        }
                    },
                    shape = SH.Button
                ) { Text("Guardar Cliente", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
