package com.stepserve.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class BookingViewModel : ViewModel() {
    private val _bookingState = MutableStateFlow<BookingUiState>(BookingUiState.Idle)
    val bookingState: StateFlow<BookingUiState> = _bookingState

    private val _payState = MutableStateFlow<PayUiState>(PayUiState.Idle)
    val payState: StateFlow<PayUiState> = _payState

    var bookingId: Int? = null

    fun createBooking(serviceId: Int, startIso: String, endIso: String) {
        viewModelScope.launch {
            _bookingState.value = BookingUiState.Loading
            when (val r = safeApiCall { RetrofitClient.api.createBooking(BookingCreate(serviceId, startIso, endIso)) }) {
                is ApiResult.Success -> {
                    bookingId = r.data.id
                    _bookingState.value = BookingUiState.Success(r.data)
                }
                is ApiResult.Error -> _bookingState.value = BookingUiState.Error(r.message)
            }
        }
    }

    fun payBooking() {
        val id = bookingId ?: return
        viewModelScope.launch {
            _payState.value = PayUiState.Loading
            when (val r = safeApiCall { RetrofitClient.api.createPayment(PaymentCreate(id)) }) {
                is ApiResult.Success -> _payState.value = PayUiState.Success(r.data)
                is ApiResult.Error -> _payState.value = PayUiState.Error(r.message)
            }
        }
    }
}

sealed class BookingUiState {
    object Idle : BookingUiState()
    object Loading : BookingUiState()
    data class Success(val booking: BookingResponse) : BookingUiState()
    data class Error(val message: String) : BookingUiState()
}

sealed class PayUiState {
    object Idle : PayUiState()
    object Loading : PayUiState()
    data class Success(val payment: PaymentResponse) : PayUiState()
    data class Error(val message: String) : PayUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    navController: NavController,
    serviceId: Int,
    serviceTitle: String,
    price: Double,
    vm: BookingViewModel = viewModel(),
) {
    val bookingState by vm.bookingState.collectAsState()
    val payState by vm.payState.collectAsState()

    var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var startHour by remember { mutableStateOf(10) }
    var durationHours by remember { mutableIntStateOf(1) }

    val endHour = startHour + durationHours
    val isoFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    val startIso = "${selectedDate}T${"%02d".format(startHour)}:00:00"
    val endIso = "${selectedDate}T${"%02d".format(endHour.coerceAtMost(23))}:00:00"
    val totalPrice = price * durationHours

    Scaffold(
        topBar = { BackTopBar("Book Session", onBack = { navController.popBackStack() }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
        ) {
            // Service summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GreenLight),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Green700, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(serviceTitle, fontWeight = FontWeight.Bold)
                        Text("CAD ${price}/hr", color = Green700, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Payment success state
            if (payState is PayUiState.Success) {
                SuccessBanner("Payment confirmed! Check your email for your receipt.")
                Spacer(Modifier.height(12.dp))
                val p = (payState as PayUiState.Success).payment
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        listOf(
                            "Receipt #" to "RCP-${"%06d".format(p.id)}",
                            "Amount" to "CAD ${"%.2f".format(p.amount)}",
                            "Reference" to (p.stripePaymentIntentId ?: "-"),
                            "Status" to p.status.uppercase(),
                        ).forEach { (label, value) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton("View My Bookings") { navController.navigate("bookings") { popUpTo("home") } }
                return@Column
            }

            // Booking success — show pay button
            if (bookingState is BookingUiState.Success) {
                val booking = (bookingState as BookingUiState.Success).booking
                SuccessBanner("Booking created! Complete your payment to confirm.")
                Spacer(Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        listOf(
                            "Booking #" to "BK-${"%06d".format(booking.id)}",
                            "Total" to "CAD ${"%.2f".format(booking.totalPrice)}",
                            "Status" to booking.status.uppercase(),
                        ).forEach { (label, value) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (payState is PayUiState.Error) ErrorBanner((payState as PayUiState.Error).message)
                PrimaryButton(
                    text = if (payState is PayUiState.Loading) "Processing payment…" else "Pay CAD ${"%.2f".format(booking.totalPrice)}",
                    onClick = { vm.payBooking() },
                    enabled = payState !is PayUiState.Loading,
                )
                return@Column
            }

            // Date selection
            Text("Select Date", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..6).forEach { offset ->
                    val date = LocalDate.now().plusDays(offset.toLong() + 1)
                    val selected = date == selectedDate
                    Card(
                        onClick = { selectedDate = date },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) Green700 else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(date.dayOfWeek.name.take(3), fontSize = 10.sp, color = if (selected) White else Gray500)
                            Text("${date.dayOfMonth}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (selected) White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Start time
            Text("Start Time", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (8..20 step 2).forEach { hour ->
                    val selected = startHour == hour
                    FilterChip(
                        selected = selected,
                        onClick = { startHour = hour },
                        label = { Text("${"%02d".format(hour)}:00", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Green700,
                            selectedLabelColor = White,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Duration
            Text("Duration", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (durationHours > 1) durationHours-- }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Less")
                }
                Text("$durationHours hr${if (durationHours > 1) "s" else ""}", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp))
                IconButton(onClick = { if (durationHours < 8) durationHours++ }) {
                    Icon(Icons.Filled.Add, contentDescription = "More")
                }
            }

            Spacer(Modifier.height(20.dp))

            // Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Booking Summary", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Date" to selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                        "Time" to "${"%02d".format(startHour)}:00 – ${"%02d".format(endHour.coerceAtMost(23))}:00",
                        "Duration" to "$durationHours hr${if (durationHours > 1) "s" else ""}",
                        "Rate" to "CAD ${price}/hr",
                        "Total" to "CAD ${"%.2f".format(totalPrice)}",
                    ).forEach { (label, value) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text(value, fontWeight = if (label == "Total") FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp, color = if (label == "Total") Green700 else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (bookingState is BookingUiState.Error) {
                ErrorBanner((bookingState as BookingUiState.Error).message)
                Spacer(Modifier.height(12.dp))
            }

            PrimaryButton(
                text = if (bookingState is BookingUiState.Loading) "Creating booking…" else "Continue to Payment",
                onClick = { vm.createBooking(serviceId, startIso, endIso) },
                enabled = bookingState !is BookingUiState.Loading,
            )
        }
    }
}
