package com.salesnetwork.avon.app.ui.network

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import com.salesnetwork.avon.app.ui.SectionIntro
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salesnetwork.avon.app.domain.model.User
import com.salesnetwork.avon.app.domain.model.UserRole
import com.salesnetwork.avon.app.ui.viewmodel.LeaderSupervisionData
import com.salesnetwork.avon.app.utils.ContactActionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamNetworkScreen(
    currentUser: User,
    teamMembers: List<User>,
    networkCommissionTotal: Double = 135.50,
    allLeadersData: List<LeaderSupervisionData> = emptyList(),
    globalTotalSales: Double = 0.0,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val isRootAdmin = currentUser.role == UserRole.ROOT_ADMIN
    val isLeader = currentUser.role == UserRole.LIDER

    var kpiDetailTitle by remember { mutableStateOf<String?>(null) }
    var kpiDetailBody by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        SectionIntro("VV / Tu espacio", "Hola, ${currentUser.name.substringBefore(" ")}", "Tu equipo y sus resultados, mas cerca.")
        Spacer(Modifier.height(16.dp))
        // Cabecera Principal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isRootAdmin) "Gestion de Redes" else "Mi Red de Ventas",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isRootAdmin) "Administracion y Soporte" else if (isLeader) "Panel de Liderazgo VV Chiclayo" else "Panel de Vendedor",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onLogout) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar Sesion", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tarjeta de Perfil
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isRootAdmin) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = currentUser.name.take(2).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentUser.email,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isRootAdmin) "Administrador" else if (isLeader) "LIDER" else "MIEMBRO",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (isLeader) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("CODIGO DE TU EQUIPO:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = currentUser.referralCode,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Codigo Lider", currentUser.referralCode))
                                    Toast.makeText(context, "Codigo copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copiar", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val text = "Hola, unete a mi equipo de ventas VV Chiclayo con mi codigo de lider: ${currentUser.referralCode}"
                                    ContactActionHelper.openWhatsAppChat(context, "", text)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Invitar", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // VISTA EXCLUSIVA ADMINISTRADOR
        if (isRootAdmin) {
            val totalVendedoras = allLeadersData.sumOf { it.members.size }
            val totalActivas = allLeadersData.sumOf { it.activeMembersCount }
            val facturacionVal = if (globalTotalSales > 0) globalTotalSales else 5239.60

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            kpiDetailTitle = "Detalle de Lideres"
                            kpiDetailBody = "Actualmente hay ${allLeadersData.size} lideres registrados supervisando redes en Chiclayo y distritos aledaños."
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Lideres", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${allLeadersData.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            kpiDetailTitle = "Detalle de Vendedoras"
                            kpiDetailBody = "Un total de $totalVendedoras vendedoras integran la organizacion.\n$totalActivas se encuentran activas en la campana actual."
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Vendedoras", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$totalVendedoras", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1.2f)
                        .clickable {
                            kpiDetailTitle = "Detalle de Facturacion"
                            kpiDetailBody = "Volumen global facturado en Campana Activa: S/ ${String.format("%.2f", facturacionVal)} entre todos los equipos."
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Facturacion", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("S/ ${String.format("%.2f", facturacionVal)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2E7D32))
                    }
                }
            }

            if (kpiDetailTitle != null) {
                AlertDialog(
                    onDismissRequest = { kpiDetailTitle = null },
                    title = { Text(kpiDetailTitle!!, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    text = { Text(kpiDetailBody.orEmpty(), fontSize = 13.sp) },
                    confirmButton = {
                        TextButton(onClick = { kpiDetailTitle = null }) {
                            Text("Entendido")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Equipos y Lideres (${allLeadersData.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(allLeadersData) { leaderData ->
                    var expanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded },
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(leaderData.leader.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("Codigo Red: ${leaderData.leader.referralCode}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${leaderData.activeMembersCount} / ${leaderData.members.size} Activas",
                                        color = Color(0xFF2E7D32),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Ventas de su Equipo: S/ ${String.format("%.2f", leaderData.totalTeamSales)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Sobrecomision Lider (5%): S/ ${String.format("%.2f", leaderData.networkCommission)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(
                                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Ver Vendedoras"
                                    )
                                }
                            }

                            if (expanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Vendedoras en la red de ${leaderData.leader.name}:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                if (leaderData.members.isEmpty()) {
                                    Text("No tiene vendedoras registradas aun.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    leaderData.members.forEach { m ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("- ${m.name} (${m.email})", fontSize = 13.sp)
                                            Text(if (m.isActiveInCampaign) "Activa" else "Pendiente", fontSize = 12.sp, color = if (m.isActiveInCampaign) Color(0xFF2E7D32) else Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // VISTA NORMAL DE LIDER
            if (isLeader) {
                var showLeaderSummary by rememberSaveable { mutableStateOf(true) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Resumen del líder", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { showLeaderSummary = !showLeaderSummary }) { Text(if (showLeaderSummary) "Ocultar" else "Mostrar") }
                }
                if (showLeaderSummary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Sobrecomision Red (5%)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "S/ ${String.format("%.2f", networkCommissionTotal)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Por ventas de tu equipo", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        val activeCount = teamMembers.count { it.isActiveInCampaign }
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Vendedores Activos", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$activeCount / ${teamMembers.size}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = if (activeCount > 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            Text("En campana activa", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Text(
                text = "Integrantes de tu Red (${teamMembers.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (teamMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aun no tienes vendedores registrados.\nComparte tu codigo ${currentUser.referralCode} para inscribir miembros.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(teamMembers) { member ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(member.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(member.email, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    color = if (member.isActiveInCampaign) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (member.isActiveInCampaign) "ACTIVO" else "PENDIENTE",
                                        color = if (member.isActiveInCampaign) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
