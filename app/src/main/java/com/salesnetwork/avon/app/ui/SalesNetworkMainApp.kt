package com.salesnetwork.avon.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salesnetwork.avon.app.domain.model.UserRole
import com.salesnetwork.avon.app.update.AppUpdateInfo
import com.salesnetwork.avon.app.ui.auth.LoginRegisterScreen
import com.salesnetwork.avon.app.ui.catalog.CatalogScreen
import com.salesnetwork.avon.app.ui.customer.CustomerListScreen
import com.salesnetwork.avon.app.ui.network.TeamNetworkScreen
import com.salesnetwork.avon.app.ui.order.OrderListScreen
import com.salesnetwork.avon.app.ui.viewmodel.AuthViewModel
import com.salesnetwork.avon.app.ui.viewmodel.CatalogViewModel
import com.salesnetwork.avon.app.ui.viewmodel.CustomerViewModel
import com.salesnetwork.avon.app.ui.viewmodel.OrderViewModel
import com.salesnetwork.avon.app.ui.viewmodel.TeamViewModel

enum class SalesAppTab {
    NETWORK,
    CATALOG,
    CUSTOMERS,
    ORDERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesNetworkMainApp(
    authViewModel: AuthViewModel = viewModel(),
    catalogViewModel: CatalogViewModel = viewModel(),
    customerViewModel: CustomerViewModel = viewModel(),
    teamViewModel: TeamViewModel = viewModel(),
    orderViewModel: OrderViewModel = viewModel(),
    availableUpdate: AppUpdateInfo? = null,
    onOpenUpdate: (String) -> Unit = {}
) {
    val authState by authViewModel.uiState.collectAsState()
    val catalogState by catalogViewModel.uiState.collectAsState()
    val customerState by customerViewModel.uiState.collectAsState()
    val teamState by teamViewModel.uiState.collectAsState()
    val orderState by orderViewModel.uiState.collectAsState()

    var selectedTab by remember { mutableStateOf(SalesAppTab.NETWORK) }
    var showCampaignMenu by remember { mutableStateOf(false) }
    var showMandatoryUpdate by remember { mutableStateOf(true) }

    val currentUser = authState.currentUser

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            catalogViewModel.scrapeOfficialWebCatalog()
            val isRoot = currentUser.role == UserRole.ROOT_ADMIN
            teamViewModel.loadTeamForUser(currentUser)
            customerViewModel.setUser(currentUser.id, isRoot)
            orderViewModel.setUser(currentUser.id, isRoot)
        }
    }

    if (currentUser == null) {
        Column(Modifier.fillMaxSize()) {
            availableUpdate?.let { update ->
                if (update.mandatory && showMandatoryUpdate) {
                    UpdateBanner(update, onOpenUpdate)
                } else if (!update.mandatory) {
                    UpdateBanner(update, onOpenUpdate, onDismiss = { showMandatoryUpdate = false })
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                LoginRegisterScreen(
                    onLoginSuccess = { selectedTab = SalesAppTab.NETWORK },
                    onRegisterLeader = { name, email, password -> authViewModel.registerLeader(name, email, password) },
                    onRegisterMember = { name, email, password, code -> authViewModel.registerMember(name, email, password, code) },
                    onLoginClick = { email, password -> authViewModel.login(email, password) },
                    onResetPassword = { email, newPass -> authViewModel.resetPassword(email, newPass) },
                    isLoading = authState.isLoading
                )
            }
        }
    } else {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showCampaignMenu = true }
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = when (selectedTab) {
                                            SalesAppTab.NETWORK -> "Mi Red de Liderazgo"
                                            SalesAppTab.CATALOG -> "Catalogo de Productos"
                                            SalesAppTab.CUSTOMERS -> "Directorio de Clientes"
                                            SalesAppTab.ORDERS -> "Pedidos & Cobranza"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = S.TextSubtitle
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = orderState.activeCampaign,
                                            fontSize = S.TextCaption,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = "Cambiar Campana",
                                            modifier = Modifier.size(S.IconS),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showCampaignMenu,
                                    onDismissRequest = { showCampaignMenu = false }
                                ) {
                                    orderState.campaigns.forEach { c ->
                                        DropdownMenuItem(
                                            text = { Text(c, fontWeight = if (c == orderState.activeCampaign) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                orderViewModel.setCampaign(c)
                                                showCampaignMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )

                    HorizontalDivider()
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 88.dp, top = if (availableUpdate != null) 84.dp else 0.dp)
                ) {
                    when (selectedTab) {
                        SalesAppTab.NETWORK -> {
                            TeamNetworkScreen(
                                currentUser = currentUser,
                                teamMembers = teamState.members,
                                networkCommissionTotal = teamState.networkCommissionTotal,
                                allLeadersData = teamState.allLeadersData,
                                globalTotalSales = teamState.globalTotalSales,
                                onLogout = { authViewModel.logout() }
                            )
                        }

                        SalesAppTab.CATALOG -> {
                            CatalogScreen(
                                products = catalogState.products,
                                isScraping = catalogState.isScraping,
                                onSyncWebCatalogClick = {
                                    catalogViewModel.scrapeOfficialWebCatalog()
                                },
                                onProductSelectedForOrder = { product ->
                                    selectedTab = SalesAppTab.ORDERS
                                    orderViewModel.openCreateDialog()
                                }
                            )
                        }

                        SalesAppTab.CUSTOMERS -> {
                            CustomerListScreen(
                                customers = customerState.customers,
                                onAddCustomer = { customer ->
                                    customerViewModel.addCustomer(
                                        name = customer.name,
                                        phone = customer.phone,
                                        address = customer.address,
                                        notes = customer.notes,
                                        latitude = customer.latitude,
                                        longitude = customer.longitude
                                    )
                                },
                                onDeleteCustomer = { id -> customerViewModel.deleteCustomer(id) }
                            )
                        }

                        SalesAppTab.ORDERS -> {
                            OrderListScreen(
                                orders = orderState.orders,
                                totalSales = orderState.totalSales,
                                directCommission = orderState.directCommission,
                                networkCommission = orderState.networkCommission,
                                totalProfit = orderState.totalProfit,
                                pendingDebt = orderState.pendingDebt,
                                pendingCount = orderState.pendingCount,
                                availableCustomers = customerState.customers,
                                availableProducts = catalogState.products,
                                statusMessage = orderState.statusMessage,
                                onUpdateStatus = { id, status -> orderViewModel.updateStatus(id, status) },
                                onRegisterPayment = { id, method, amount -> orderViewModel.registerPayment(id, method, amount) },
                                onCreateOrder = { custId, custName, items, method, paid ->
                                    orderViewModel.createOrder(custId, custName, items, method, paid)
                                },
                                onDeleteOrder = { id -> orderViewModel.deleteOrder(id) },
                                onShareTicket = { order -> orderViewModel.buildWhatsAppTicket(order) }
                            )
                        }
                    }
                }

                availableUpdate?.let { update ->
                    if (update.mandatory && showMandatoryUpdate) {
                        UpdateBanner(
                            info = update,
                            onOpenUpdate = onOpenUpdate,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    } else if (!update.mandatory) {
                        UpdateBanner(
                            info = update,
                            onOpenUpdate = onOpenUpdate,
                            onDismiss = { showMandatoryUpdate = false },
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }

                FloatingNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun FloatingNavBar(
    selectedTab: SalesAppTab,
    onTabSelected: (SalesAppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(bottom = S.M)
.shadow(S.ElevationMed, RoundedCornerShape(S.RHero)),
                    shape = RoundedCornerShape(S.RHero),
        color = MaterialTheme.colorScheme.surface,
        border = B.subtleBorder()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = S.SM, vertical = S.S),
            horizontalArrangement = Arrangement.spacedBy(S.XS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingNavIcon(
                icon = Icons.Default.Group,
                label = "Equipo",
                isSelected = selectedTab == SalesAppTab.NETWORK,
                onClick = { onTabSelected(SalesAppTab.NETWORK) }
            )

            FloatingNavIcon(
                icon = Icons.Default.ShoppingCart,
                label = "Catalogo",
                isSelected = selectedTab == SalesAppTab.CATALOG,
                onClick = { onTabSelected(SalesAppTab.CATALOG) }
            )

            FloatingNavIcon(
                icon = Icons.Default.LocationOn,
                label = "Clientes",
                isSelected = selectedTab == SalesAppTab.CUSTOMERS,
                onClick = { onTabSelected(SalesAppTab.CUSTOMERS) }
            )

            FloatingNavIcon(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                label = "Pedidos",
                isSelected = selectedTab == SalesAppTab.ORDERS,
                onClick = { onTabSelected(SalesAppTab.ORDERS) }
            )
        }
    }
}

@Composable
private fun UpdateBanner(
    info: AppUpdateInfo,
    onOpenUpdate: (String) -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isMandatory = info.mandatory
    val containerColor = if (isMandatory) C.Error else MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (isMandatory) Color.White else MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = S.SM, vertical = S.S),
        color = containerColor,
        shape = RoundedCornerShape(S.RCard),
        border = B.cardBorder()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = S.M, vertical = S.SM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(S.SM)
        ) {
            if (isMandatory) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(S.IconM)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (isMandatory) "Actualizacion Obligatoria v${info.versionName}" else "Nueva version ${info.versionName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = S.TextBody,
                    color = contentColor
                )
                Text(
                    info.releaseNotes,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    fontSize = S.TextSmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
            TextButton(onClick = { onOpenUpdate(info.apkUrl) }) {
                Text("Actualizar", color = contentColor)
            }
            if (onDismiss != null && !isMandatory) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(S.IconS))
                }
            }
        }
    }
}

@Composable
private fun FloatingNavIcon(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "nav_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "nav_content"
    )
    val scale by animateDpAsState(
        targetValue = if (isSelected) S.IconM else S.IconS,
        animationSpec = tween(200),
        label = "nav_icon"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(S.RL),
        color = bgColor,
        modifier = Modifier
            .width(72.dp)
            .height(56.dp)
            .semantics {
                selected = isSelected
                role = Role.Tab
                contentDescription = label
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(scale)
            )
            Spacer(Modifier.height(S.XXS))
            Text(
                label,
                fontSize = S.TextCaption,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}
