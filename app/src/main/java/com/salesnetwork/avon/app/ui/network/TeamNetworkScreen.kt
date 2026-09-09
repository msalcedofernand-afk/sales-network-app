package com.salesnetwork.avon.app.ui.network

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salesnetwork.avon.app.domain.model.User
import com.salesnetwork.avon.app.domain.model.UserRole
import com.salesnetwork.avon.app.ui.*
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
            .padding(S.M),
        verticalArrangement = Arrangement.spacedBy(S.SM)
    ) {
        SectionIntro("VV / Tu espacio", "Hola, ${currentUser.name.substringBefore(" ")}", "Tu equipo y sus resultados, mas cerca.")

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isRootAdmin) "Gestion de Redes" else "Mi Red de Ventas",
                    fontSize = S.TextTitle,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isRootAdmin) "Administracion y Soporte" else if (isLeader) "Panel de Liderazgo VV Chiclayo" else "Panel de Vendedor",
                    fontSize = S.TextSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onLogout) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar Sesion", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = SH.Card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = B.cardBorder(),
            elevation = CardDefaults.cardElevation(S.ElevationNone)
        ) {
            Column(modifier = Modifier.padding(S.M), verticalArrangement = Arrangement.spacedBy(S.SM)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = SH.Avatar,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isRootAdmin) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(S.IconL))
                            } else {
                                Text(
                                    text = currentUser.name.take(2).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = S.TextSubtitle
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(S.SM))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentUser.name, fontWeight = FontWeight.Bold, fontSize = S.TextBody)
                        Text(currentUser.email, fontSize = S.TextSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    StatusBadge(
                        text = if (isRootAdmin) "Administrador" else if (isLeader) "LIDER" else "MIEMBRO",
                        color = MaterialTheme.colorScheme.primary,
                        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                }

                // Leader code section
                if (isLeader) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("CODIGO DE TU EQUIPO:", fontSize = S.TextSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = currentUser.referralCode,
                                fontWeight = FontWeight.Black,
                                fontSize = S.TextHeadline,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(S.S)) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Codigo Lider", currentUser.referralCode))
                                    Toast.makeText(context, "Codigo copiado", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = S.SM, vertical = S.S),
                                shape = SH.ButtonSmall
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(S.IconS))
                                Spacer(modifier = Modifier.width(S.XS))
                                Text("Copiar", fontSize = S.TextSmall)
                            }

                            Button(
                                onClick = {
                                    val text = "Hola, unete a mi equipo de ventas VV Chiclayo con mi codigo: ${currentUser.referralCode}"
                                    ContactActionHelper.openWhatsAppChat(context, "", text)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = C.WhatsApp),
                                contentPadding = PaddingValues(horizontal = S.SM, vertical = S.S),
                                shape = SH.ButtonSmall
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(S.IconS))
                                Spacer(modifier = Modifier.width(S.XS))
                                Text("Invitar", fontSize = S.TextSmall, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Admin View
        if (isRootAdmin) {
            val totalVendedoras = allLeadersData.sumOf { it.members.size }
            val totalActivas = allLeadersData.sumOf { it.activeMembersCount }
            val facturacionVal = if (globalTotalSales > 0) globalTotalSales else 5239.60

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(S.S)
            ) {
                KpiCard(
                    label = "Lideres",
                    value = "${allLeadersData.size}",
                    icon = Icons.Default.SupervisorAccount,
                    onClick = {
                        kpiDetailTitle = "Detalle de Lideres"
                        kpiDetailBody = "Actualmente hay ${allLeadersData.size} lideres registrados supervisando redes en Chiclayo y distritos aledaños."
                    }
                )
                KpiCard(
                    label = "Vendedoras",
                    value = "$totalVendedoras",
                    icon = Icons.Default.Group,
                    onClick = {
                        kpiDetailTitle = "Detalle de Vendedoras"
                        kpiDetailBody = "Un total de $totalVendedoras vendedoras integran la organizacion.\n$totalActivas se encuentran activas en la campana actual."
                    }
                )
                KpiCard(
                    label = "Facturacion",
                    value = "S/ ${String.format("%.2f", facturacionVal)}",
                    valueColor = C.Success,
                    icon = Icons.Default.SupervisorAccount,
                    onClick = {
                        kpiDetailTitle = "Detalle de Facturacion"
                        kpiDetailBody = "Volumen global facturado en Campana Activa: S/ ${String.format("%.2f", facturacionVal)} entre todos los equipos."
                    }
                )
            }

            if (kpiDetailTitle != null) {
                AlertDialog(
                    onDismissRequest = { kpiDetailTitle = null },
                    title = { Text(kpiDetailTitle!!, fontWeight = FontWeight.Bold, fontSize = S.TextTitle) },
                    text = { Text(kpiDetailBody.orEmpty(), fontSize = S.TextBody) },
                    confirmButton = {
                        TextButton(onClick = { kpiDetailTitle = null }) { Text("Entendido") }
                    }
                )
            }

            SectionHeader(title = "Equipos y Lideres", count = allLeadersData.size)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(S.S)
            ) {
                items(allLeadersData) { leaderData ->
                    var expanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = SH.Card,
                        border = B.cardBorder(),
                        elevation = CardDefaults.cardElevation(S.ElevationNone),
                        onClick = { expanded = !expanded }
                    ) {
                        Column(modifier = Modifier.padding(S.M), verticalArrangement = Arrangement.spacedBy(S.S)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = SH.Avatar, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(S.IconM))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(S.SM))
                                    Column {
                                        Text(leaderData.leader.name, fontWeight = FontWeight.Bold, fontSize = S.TextBody)
                                        Text("Codigo: ${leaderData.leader.referralCode}", fontSize = S.TextSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }

                                StatusBadge(
                                    text = "${leaderData.activeMembersCount} / ${leaderData.members.size} Activas",
                                    color = C.Success,
                                    backgroundColor = C.SuccessLight
                                )
                            }

                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Ventas: S/ ${String.format("%.2f", leaderData.totalTeamSales)}", fontSize = S.TextSmall, fontWeight = FontWeight.SemiBold)
                                    Text("Sobrecomision (5%): S/ ${String.format("%.2f", leaderData.networkCommission)}", fontSize = S.TextSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(
                                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Ver Vendedoras",
                                    modifier = Modifier.size(S.IconM)
                                )
                            }

                            if (expanded) {
                                Text("Vendedoras de ${leaderData.leader.name}:", fontSize = S.TextSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                if (leaderData.members.isEmpty()) {
                                    Text("Sin vendedoras registradas.", fontSize = S.TextSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    leaderData.members.forEach { m ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = S.XXS),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("- ${m.name}", fontSize = S.TextSmall)
                                            StatusBadge(
                                                text = if (m.isActiveInCampaign) "Activa" else "Pendiente",
                                                color = if (m.isActiveInCampaign) C.Success else C.Error,
                                                backgroundColor = if (m.isActiveInCampaign) C.SuccessLight else C.ErrorLight
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Leader / Member View
            if (isLeader) {
                var showLeaderSummary by rememberSaveable { mutableStateOf(true) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Resumen del lider", fontSize = S.TextSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { showLeaderSummary = !showLeaderSummary }) {
                        Text(if (showLeaderSummary) "Ocultar" else "Mostrar", fontSize = S.TextSmall)
                    }
                }
                if (showLeaderSummary) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(S.S)) {
                        KpiCard(
                            label = "Sobrecomision Red",
                            value = "S/ ${String.format("%.2f", networkCommissionTotal)}",
                            icon = Icons.Default.SupervisorAccount
                        )
                        val activeCount = teamMembers.count { it.isActiveInCampaign }
                        KpiCard(
                            label = "Vendedores Activos",
                            value = "$activeCount / ${teamMembers.size}",
                            valueColor = if (activeCount > 0) C.Success else C.Error,
                            icon = Icons.Default.Group
                        )
                    }
                }
            }

            SectionHeader(title = "Integrantes de tu Red", count = teamMembers.size)

            if (teamMembers.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Group,
                    title = "Sin integrantes",
                    description = "Comparte tu codigo ${currentUser.referralCode} para inscribir miembros a tu red."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(S.S)
                ) {
                    items(teamMembers) { member ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = SH.Card,
                            border = B.cardBorder(),
                            elevation = CardDefaults.cardElevation(S.ElevationNone)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(S.M),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = SH.Avatar, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(S.IconM))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(S.SM))
                                    Column {
                                        Text(member.name, fontWeight = FontWeight.Bold, fontSize = S.TextBody)
                                        Text(member.email, fontSize = S.TextSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                StatusBadge(
                                    text = if (member.isActiveInCampaign) "ACTIVO" else "PENDIENTE",
                                    color = if (member.isActiveInCampaign) C.Success else C.Error,
                                    backgroundColor = if (member.isActiveInCampaign) C.SuccessLight else C.ErrorLight
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
