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

class ProviderListingsViewModel : ViewModel() {
    private val _listings = MutableStateFlow<List<Service>>(emptyList())
    val listings: StateFlow<List<Service>> = _listings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _payResult = MutableStateFlow<Map<Int, String>>(emptyMap())
    val payResult: StateFlow<Map<Int, String>> = _payResult

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val r = safeApiCall { RetrofitClient.api.providerListings() }) {
                is ApiResult.Success -> _listings.value = r.data
                is ApiResult.Error -> _error.value = r.message
            }
            _isLoading.value = false
        }
    }

    fun payListing(serviceId: Int) {
        viewModelScope.launch {
            _payResult.value = _payResult.value + (serviceId to "loading")
            when (val r = safeApiCall { RetrofitClient.api.payListing(serviceId) }) {
                is ApiResult.Success -> {
                    _payResult.value = _payResult.value + (serviceId to "paid")
                    load() // Refresh list
                }
                is ApiResult.Error -> _payResult.value = _payResult.value + (serviceId to "error: ${r.message}")
            }
        }
    }

    fun deactivate(serviceId: Int) {
        viewModelScope.launch {
            safeApiCall { RetrofitClient.api.deactivateService(serviceId) }
            load()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderListingsScreen(navController: NavController, vm: ProviderListingsViewModel = viewModel()) {
    val listings by vm.listings.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    val payResult by vm.payResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Listings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Green700, titleContentColor = White, navigationIconContentColor = White),
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
            listings.isEmpty() -> Column(Modifier.fillMaxSize().padding(padding)) {
                EmptyState("No listings yet. Create your first listing!", Icons.Filled.List)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PrimaryButton("Create Listing", onClick = { navController.navigate(Routes.CREATE_LISTING) }, modifier = Modifier.width(220.dp))
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text("${listings.size} listing${if (listings.size != 1) "s" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(listings) { listing ->
                    val payStatus = payResult[listing.id]
                    ListingCard(
                        listing = listing,
                        payStatus = payStatus,
                        onPay = { vm.payListing(listing.id) },
                        onDeactivate = { vm.deactivate(listing.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ListingCard(
    listing: Service,
    payStatus: String?,
    onPay: () -> Unit,
    onDeactivate: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(listing.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("CAD ${listing.price}/hr", color = Green700, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge(if (listing.isActive == 1) "active" else "inactive")
                    StatusBadge(listing.paymentStatus ?: "pending")
                }
            }
            if (!listing.categoryName.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(listing.categoryName, style = MaterialTheme.typography.bodySmall, color = Gray500)
            }
            if (listing.paymentStatus == "pending" && listing.isActive == 0) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                when {
                    payStatus == "loading" -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Green700)
                        Spacer(Modifier.width(8.dp))
                        Text("Processing payment…", fontSize = 12.sp, color = Gray500)
                    }
                    payStatus == "paid" -> SuccessBanner("Listing activated!")
                    payStatus?.startsWith("error") == true -> ErrorBanner(payStatus.removePrefix("error: "))
                    else -> Button(
                        onClick = onPay,
                        colors = ButtonDefaults.buttonColors(containerColor = Green700),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Filled.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pay CAD \$5 to Activate", fontSize = 13.sp)
                    }
                }
            } else if (listing.isActive == 1) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDeactivate,
                    colors = ButtonDefaults.textButtonColors(contentColor = RedError),
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Deactivate", fontSize = 12.sp)
                }
            }
        }
    }
}
