package com.stepserve.app.ui.screens

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
import com.stepserve.app.data.auth.TokenManager
import com.stepserve.app.ui.components.*
import com.stepserve.app.ui.navigation.Routes
import com.stepserve.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServiceDetailViewModel : ViewModel() {
    private val _service = MutableStateFlow<Service?>(null)
    val service: StateFlow<Service?> = _service

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Load from listings (search all active services and find by id)
    fun load(serviceId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val r = safeApiCall { RetrofitClient.api.listServices() }) {
                is ApiResult.Success -> _service.value = r.data.find { it.id == serviceId }
                is ApiResult.Error -> {
                    // Fall back to search
                    when (val r2 = safeApiCall { RetrofitClient.api.searchServices(limit = 200) }) {
                        is ApiResult.Success -> _service.value = r2.data.find { it.id == serviceId }
                        is ApiResult.Error -> _error.value = r2.message
                    }
                }
            }
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailScreen(
    navController: NavController,
    serviceId: Int,
    vm: ServiceDetailViewModel = viewModel(),
) {
    val service by vm.service.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    LaunchedEffect(serviceId) { vm.load(serviceId) }

    Scaffold(
        topBar = {
            BackTopBar(service?.title ?: "Service Detail", onBack = { navController.popBackStack() })
        },
        bottomBar = {
            if (service != null && TokenManager.getRole() == "customer") {
                Surface(shadowElevation = 8.dp) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryButton(
                            text = "Book Now",
                            onClick = {
                                navController.navigate(
                                    Routes.booking(serviceId, service!!.title, service!!.price)
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else if (!TokenManager.isLoggedIn()) {
                Surface(shadowElevation = 8.dp) {
                    Row(Modifier.fillMaxWidth().padding(16.dp)) {
                        PrimaryButton("Sign In to Book", onClick = { navController.navigate(Routes.LOGIN) })
                    }
                }
            }
        }
    ) { padding ->
        when {
            isLoading -> FullScreenLoader()
            error != null -> ErrorBanner(error!!, onRetry = { vm.load(serviceId) })
            service == null -> EmptyState("Service not found")
            else -> {
                val svc = service!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item {
                        // Header card
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = GreenLight),
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text(svc.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Green700)
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "CAD ${svc.price}/hr",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Green700,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    StatusBadge("Active")
                                }
                            }
                        }
                    }

                    // Provider info
                    if (!svc.providerName.isNullOrBlank()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = Green700, modifier = Modifier.size(32.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(svc.providerName, fontWeight = FontWeight.SemiBold)
                                        if (!svc.location.isNullOrBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Gray500, modifier = Modifier.size(14.dp))
                                                Text(svc.location, style = MaterialTheme.typography.bodySmall, color = Gray500)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Description
                    if (svc.description.isNotBlank()) {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text("About this service", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(svc.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                            }
                        }
                    }

                    // How booking works
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("How it works", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.height(12.dp))
                                listOf(
                                    Icons.Filled.CalendarToday to "Pick your date & time",
                                    Icons.Filled.Payment to "Pay securely online",
                                    Icons.Filled.CheckCircle to "Service completed",
                                ).forEach { (icon, step) ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                        Icon(icon, contentDescription = null, tint = Green700, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text(step, style = MaterialTheme.typography.bodySmall)
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
