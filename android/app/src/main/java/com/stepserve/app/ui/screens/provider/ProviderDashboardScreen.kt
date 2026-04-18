package com.stepserve.app.ui.screens.provider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

class ProviderDashboardViewModel : ViewModel() {
    private val _dashboard = MutableStateFlow<ProviderDashboard?>(null)
    val dashboard: StateFlow<ProviderDashboard?> = _dashboard

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val r = safeApiCall { RetrofitClient.api.providerDashboard() }) {
                is ApiResult.Success -> _dashboard.value = r.data
                is ApiResult.Error -> _error.value = r.message
            }
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDashboardScreen(navController: NavController, vm: ProviderDashboardViewModel = viewModel()) {
    val dashboard by vm.dashboard.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Green700, titleContentColor = White),
                actions = {
                    IconButton(onClick = { vm.load() }) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = White) }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.CREATE_LISTING) }, containerColor = Green700) {
                Icon(Icons.Filled.Add, contentDescription = "New Listing", tint = White)
            }
        }
    ) { padding ->
        when {
            isLoading -> FullScreenLoader()
            error != null -> ErrorBanner(error!!, onRetry = { vm.load() })
            else -> {
                val d = dashboard
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    // Stats
                    item {
                        Column(Modifier.padding(16.dp)) {
                            Text("Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard(
                                    "Active Listings",
                                    "${d?.services?.count { it.isActive == 1 } ?: 0}",
                                    Icons.Filled.List,
                                    modifier = Modifier.weight(1f),
                                )
                                StatCard(
                                    "Bookings",
                                    "${d?.bookings?.size ?: 0}",
                                    Icons.Filled.DateRange,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard(
                                    "Uploads",
                                    "${d?.uploads?.size ?: 0}",
                                    Icons.Filled.Upload,
                                    tint = BlueInfo,
                                    modifier = Modifier.weight(1f),
                                )
                                StatCard(
                                    "Pending",
                                    "${d?.services?.count { it.paymentStatus == "pending" } ?: 0}",
                                    Icons.Filled.Pending,
                                    tint = AmberWarning,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    // Quick nav
                    item {
                        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(
                                Triple("Listings", Icons.Filled.List, Routes.PROVIDER_LISTINGS),
                                Triple("Documents", Icons.Filled.Upload, Routes.PROVIDER_DOCUMENTS),
                            ).forEach { (label, icon, route) ->
                                Card(
                                    onClick = { navController.navigate(route) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = GreenLight),
                                ) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(icon, contentDescription = null, tint = Green700, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(label, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Green700)
                                    }
                                }
                            }
                        }
                    }

                    // Recent bookings
                    if (d?.bookings?.isNotEmpty() == true) {
                        item { SectionHeader("Recent Bookings", action = "All", onAction = {}) }
                        items(d.bookings.take(5)) { booking ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Green700, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("BK-${"%06d".format(booking.id)}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(booking.startTime.take(10), style = MaterialTheme.typography.bodySmall, color = Gray500)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        StatusBadge(booking.status)
                                        Text("CAD ${"%.0f".format(booking.totalPrice)}", fontWeight = FontWeight.Bold, color = Green700, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        item { EmptyState("No bookings yet", Icons.Filled.DateRange) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color = Green700, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
