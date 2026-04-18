package com.stepserve.app.ui.screens.admin

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
import com.stepserve.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminBookingsViewModel : ViewModel() {
    private val _bookings = MutableStateFlow<List<AdminBooking>>(emptyList())
    val bookings: StateFlow<List<AdminBooking>> = _bookings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val r = safeApiCall { RetrofitClient.api.adminBookings() }) {
                is ApiResult.Success -> _bookings.value = r.data
                is ApiResult.Error -> _error.value = r.message
            }
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBookingsScreen(navController: NavController, vm: AdminBookingsViewModel = viewModel()) {
    val bookings by vm.bookings.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(bookings, searchQuery) {
        if (searchQuery.isBlank()) bookings
        else bookings.filter {
            it.customerEmail.contains(searchQuery, ignoreCase = true) ||
                it.status.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Bookings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Green700,
                    titleContentColor = White,
                    navigationIconContentColor = White,
                ),
                actions = {
                    IconButton(onClick = { vm.load() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = White)
                    }
                },
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by email or status…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp),
                singleLine = true,
            )

            when {
                isLoading -> FullScreenLoader()
                error != null -> ErrorBanner(error!!, onRetry = { vm.load() })
                filtered.isEmpty() -> EmptyState("No bookings found", Icons.Filled.DateRange)
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            "${filtered.size} booking${if (filtered.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(filtered) { booking ->
                        AdminBookingCard(booking)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminBookingCard(booking: AdminBooking) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "BK-${"%06d".format(booking.id)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text(
                        booking.customerEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500,
                        maxLines = 1,
                    )
                }
                StatusBadge(booking.status)
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
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
            Spacer(Modifier.height(6.dp))
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
