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

class CustomerBookingsViewModel : ViewModel() {
    private val _bookings = MutableStateFlow<List<CustomerBooking>>(emptyList())
    val bookings: StateFlow<List<CustomerBooking>> = _bookings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val r = safeApiCall { RetrofitClient.api.customerBookings() }) {
                is ApiResult.Success -> _bookings.value = r.data
                is ApiResult.Error -> _error.value = r.message
            }
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerBookingsScreen(navController: NavController, vm: CustomerBookingsViewModel = viewModel()) {
    val bookings by vm.bookings.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    if (!TokenManager.isLoggedIn()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Gray300, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text("Sign in to view your bookings")
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Sign In", onClick = { navController.navigate(Routes.LOGIN) }, modifier = Modifier.width(200.dp))
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Bookings", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Green700, titleContentColor = White),
                actions = {
                    IconButton(onClick = { vm.load() }) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = White) }
                },
            )
        }
    ) { padding ->
        when {
            isLoading -> FullScreenLoader()
            error != null -> ErrorBanner(error!!, onRetry = { vm.load() })
            bookings.isEmpty() -> Column(Modifier.fillMaxSize().padding(padding)) {
                EmptyState("No bookings yet.\nFind a service and make your first booking!", Icons.Filled.CalendarToday)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PrimaryButton("Browse Services", onClick = { navController.navigate(Routes.SEARCH) }, modifier = Modifier.width(220.dp))
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text("${bookings.size} booking${if (bookings.size != 1) "s" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(bookings) { booking ->
                    BookingCard(booking)
                }
            }
        }
    }
}

@Composable
private fun BookingCard(booking: CustomerBooking) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(booking.serviceTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("BK-${"%06d".format(booking.id)}", style = MaterialTheme.typography.bodySmall, color = Gray500)
                }
                StatusBadge(booking.status)
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("From", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text(
                        booking.startTime.take(16).replace("T", " "),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("To", style = MaterialTheme.typography.bodySmall, color = Gray500)
                    Text(
                        booking.endTime.take(16).replace("T", " "),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "CAD ${"%.2f".format(booking.totalPrice)}",
                    fontWeight = FontWeight.Bold,
                    color = Green700,
                    fontSize = 15.sp,
                )
            }
        }
    }
}
