package com.salesnetwork.avon.app.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.salesnetwork.avon.app.ui.SectionIntro
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
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
            ChiclayoZone("Pimentel / Balneario", "Av. Quiñones, Pimentel", -6.8333, -79.9333)
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
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Cliente")
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
            SectionIntro("VV / Relaciones", "Cada contacto cuenta", "Organiza tus clientes y prepara tu proxima visita.")

            Spacer(modifier = Modifier.height(14.dp))

            if (customers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tienes clientes registrados aun.\nPresiona '+' para agregar tu primer contacto con GPS.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(customers) { customer ->
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
                                    Text(
                                        text = customer.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )

                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (customer.estimatedMinutes != null) {
                                                "~${customer.estimatedMinutes} min (${customer.estimatedDistanceKm ?: 0.0} km)"
                                            } else {
                                                "Ruta sin calcular"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = customer.address,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (customer.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = customer.notes,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                ContactActionHelper.openPhoneDialer(context, customer.phone)
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Llamar", fontSize = 13.sp)
                                        }

                                        Button(
                                            onClick = {
                                                ContactActionHelper.openWhatsAppChat(context, customer.whatsapp)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("WhatsApp", fontSize = 13.sp, color = Color.White)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Button(
                                            onClick = {
                                                ContactActionHelper.openTurnByTurnNavigation(
                                                    context,
                                                    customer.address,
                                                    customer.latitude,
                                                    customer.longitude
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Navegar", fontSize = 13.sp, color = Color.White)
                                        }

                                        IconButton(
                                            onClick = { customerToDelete = customer },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Eliminar Cliente",
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

    if (customerToDelete != null) {
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Eliminar Cliente", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas eliminar de la lista al cliente '${customerToDelete?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        customerToDelete?.id?.let { onDeleteCustomer(it) }
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nuevo cliente", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nombre Completo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPhone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        onValueChange = { newPhone = it },
                        label = { Text("Telefono / WhatsApp") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Selecciona Zona GPS en Chiclayo:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Box {
                        OutlinedButton(onClick = { zoneMenuExpanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(selectedZone.name)
                        }
                        DropdownMenu(expanded = zoneMenuExpanded, onDismissRequest = { zoneMenuExpanded = false }) {
                            chiclayoZones.forEach { zone ->
                                DropdownMenuItem(text = { Text(zone.name) }, onClick = {
                                    selectedZone = zone
                                    newAddress = zone.addressHint
                                    zoneMenuExpanded = false
                                })
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        label = { Text("Direccion Especifica") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newNotes,
                        onValueChange = { newNotes = it },
                        label = { Text("Notas de Pedido o Preferencias") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = newName.isNotBlank() && newPhone.count { it.isDigit() } >= 7,
                    onClick = {
                        if (newName.isNotBlank() && newPhone.isNotBlank()) {
                            val contact = CustomerContact(
                                id = "c-${System.currentTimeMillis()}",
                                name = newName.trim(),
                                phone = newPhone.trim(),
                                whatsapp = newPhone.trim(),
                                address = newAddress.trim(),
                                city = "Chiclayo",
                                latitude = selectedZone.lat,
                                longitude = selectedZone.lng,
                                notes = newNotes.trim()
                            )
                            onAddCustomer(contact)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Guardar Cliente")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
