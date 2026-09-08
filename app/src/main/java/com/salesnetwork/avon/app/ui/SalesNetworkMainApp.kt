package com.salesnetwork.avon.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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

    val currentUser = authState.currentUser

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            teamViewModel.loadTeamForLeader(currentUser)
            customerViewModel.setUserId(currentUser.id)
            orderViewModel.setLeaderId(currentUser.id)
        }
    }

    if (currentUser == null) {
        LoginRegisterScreen(
            onLoginSuccess = { selectedTab = SalesAppTab.NETWORK },
            onRegisterLeader = { name, email, password -> authViewModel.registerLeader(name, email, password) },
            onRegisterMember = { name, email, password, code -> authViewModel.registerMember(name, email, password, code) },
            onLoginClick = { email, password -> authViewModel.login(email, password) }
        )
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text(
                                text = when (selectedTab) {
                                    SalesAppTab.NETWORK -> "Mi red"
                                    SalesAppTab.CATALOG -> "Catálogo"
                                    SalesAppTab.CUSTOMERS -> "Clientes"
                                    SalesAppTab.ORDERS -> "Pedidos"
                                },
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Text("Sales Network", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == SalesAppTab.NETWORK,
                        onClick = { selectedTab = SalesAppTab.NETWORK },
                        icon = { Icon(Icons.Default.Group, contentDescription = "Red") },
                        label = { Text("Mi Red") }
                    )

                    NavigationBarItem(
                        selected = selectedTab == SalesAppTab.CATALOG,
                        onClick = { selectedTab = SalesAppTab.CATALOG },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Catálogo") },
                        label = { Text("Catálogo") }
                    )

                    NavigationBarItem(
                        selected = selectedTab == SalesAppTab.CUSTOMERS,
                        onClick = { selectedTab = SalesAppTab.CUSTOMERS },
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = "Clientes") },
                        label = { Text("Clientes GPS") }
                    )

                    NavigationBarItem(
                        selected = selectedTab == SalesAppTab.ORDERS,
                        onClick = { selectedTab = SalesAppTab.ORDERS },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Pedidos") },
                        label = { Text("Pedidos") }
                    )
                }
            }
        ) { innerPadding ->
            Surface(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    SalesAppTab.NETWORK -> {
                        TeamNetworkScreen(
                            currentUser = currentUser,
                            teamMembers = teamState.members,
                            onLogout = { authViewModel.logout() }
                        )
                    }

                    SalesAppTab.CATALOG -> {
                        CatalogScreen(
                            products = catalogState.products,
                            isScraping = catalogState.isScraping,
                            onSyncWebCatalogClick = {
                                catalogViewModel.scrapeOfficialWebCatalog()
                            }
                        )
                    }

                    SalesAppTab.CUSTOMERS -> {
                        CustomerListScreen(
                            customers = customerState.customers,
                            onAddCustomer = { customerViewModel.addCustomer(it.name, it.phone, it.address, it.notes) }
                        )
                    }

                    SalesAppTab.ORDERS -> {
                        OrderListScreen(
                            orders = orderState.orders,
                            totalSales = orderState.totalSales,
                            totalCommission = orderState.totalCommission,
                            pendingCount = orderState.pendingCount,
                            onUpdateStatus = { id, status -> orderViewModel.updateStatus(id, status) },
                            onShareWhatsApp = { order -> orderViewModel.buildWhatsAppSummary(order) }
                        )
                    }
                }
            }
        }
    }
}
