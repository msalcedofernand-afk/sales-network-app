package com.salesnetwork.avon.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salesnetwork.avon.app.domain.model.UserRole
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
    orderViewModel: OrderViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val catalogState by catalogViewModel.uiState.collectAsState()
    val customerState by customerViewModel.uiState.collectAsState()
    val teamState by teamViewModel.uiState.collectAsState()
    val orderState by orderViewModel.uiState.collectAsState()

    var selectedTab by remember { mutableStateOf(SalesAppTab.NETWORK) }
    var showCampaignMenu by remember { mutableStateOf(false) }

    val currentUser = authState.currentUser

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val isRoot = currentUser.role == UserRole.ROOT_ADMIN
            teamViewModel.loadTeamForUser(currentUser)
            customerViewModel.setUser(currentUser.id, isRoot)
            orderViewModel.setUser(currentUser.id, isRoot)
        }
    }

    if (currentUser == null) {
        LoginRegisterScreen(
            onLoginSuccess = { selectedTab = SalesAppTab.NETWORK },
            onRegisterLeader = { name, email, password -> authViewModel.registerLeader(name, email, password) },
            onRegisterMember = { name, email, password, code -> authViewModel.registerMember(name, email, password, code) },
            onLoginClick = { email, password -> authViewModel.login(email, password) },
            onResetPassword = { email, newPass -> authViewModel.resetPassword(email, newPass) }
        )
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
                                        fontSize = 17.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = orderState.activeCampaign,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = "Cambiar Campana",
                                            modifier = Modifier.size(16.dp),
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
                        .padding(bottom = 88.dp)
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

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FloatingNavIcon(
                            icon = Icons.Default.Group,
                            label = "Equipo",
                            isSelected = selectedTab == SalesAppTab.NETWORK,
                            onClick = { selectedTab = SalesAppTab.NETWORK }
                        )

                        FloatingNavIcon(
                            icon = Icons.Default.ShoppingCart,
                            label = "Catalogo",
                            isSelected = selectedTab == SalesAppTab.CATALOG,
                            onClick = { selectedTab = SalesAppTab.CATALOG }
                        )

                        FloatingNavIcon(
                            icon = Icons.Default.LocationOn,
                            label = "Clientes",
                            isSelected = selectedTab == SalesAppTab.CUSTOMERS,
                            onClick = { selectedTab = SalesAppTab.CUSTOMERS }
                        )

                        FloatingNavIcon(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            label = "Pedidos",
                            isSelected = selectedTab == SalesAppTab.ORDERS,
                            onClick = { selectedTab = SalesAppTab.ORDERS }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingNavIcon(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier
            .width(64.dp)
            .height(60.dp)
            .semantics {
                selected = isSelected
                role = Role.Tab
                contentDescription = label
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
