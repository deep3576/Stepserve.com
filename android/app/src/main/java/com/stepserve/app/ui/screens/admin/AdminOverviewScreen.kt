package com.stepserve.app.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.stepserve.app.data.api.*
import com.stepserve.app.ui.components.*
import com.stepserve.app.ui.navigation.Routes
import com.stepserve.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminOverviewViewModel : ViewModel() {
    private val _overview = MutableStateFlow<AdminOverview?>(null)
    val overview: StateFlow<AdminOverview?> = _overview

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val r = safeApiCall { RetrofitClient.api.adminOverview() }) {
                is ApiResult.Success -> _overview.value = r.data
                is ApiResult.Error -> _error.value = r.message
            }
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOverviewScreen(navController: NavController, vm: AdminOverviewViewModel = viewModel()) {
    val overview by vm.overview.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Green700, titleContentColor = White),
                actions = { IconButton(onClick = { vm.load() }) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = White) } },
            )
        }
    ) { padding ->
        when {
            isLoading -> FullScreenLoader()
            error != null -> ErrorBanner(error!!, onRetry = { vm.load() })
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Text("Overview", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { StatCard("Users", "${overview?.usersCount ?: 0}", Icons.Filled.People) }
                        Box(Modifier.weight(1f)) { StatCard("Services", "${overview?.servicesCount ?: 0}", Icons.Filled.List) }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { StatCard("Bookings", "${overview?.bookingsCount ?: 0}", Icons.Filled.DateRange) }
                        Box(Modifier.weight(1f)) { StatCard("Revenue", "CAD ${"%.0f".format(overview?.paidTotal ?: 0.0)}", Icons.Filled.AttachMoney, tint = AmberWarning) }
                    }
                }

                item {
                    Text("Management", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("Users", Icons.Filled.People, Routes.ADMIN_USERS),
                            Triple("Bookings", Icons.Filled.DateRange, Routes.ADMIN_BOOKINGS),
                            Triple("Categories", Icons.Filled.Category, Routes.ADMIN_CATEGORIES),
                        ).forEach { (label, icon, route) ->
                            Card(
                                onClick = { navController.navigate(route) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row {
                                        Icon(icon, contentDescription = null, tint = Green700)
                                        Spacer(Modifier.width(12.dp))
                                        Text(label, fontWeight = FontWeight.Medium)
                                    }
                                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Gray500)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

