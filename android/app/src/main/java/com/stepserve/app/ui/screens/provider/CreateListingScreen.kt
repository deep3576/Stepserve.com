package com.stepserve.app.ui.screens.provider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

class CreateListingViewModel : ViewModel() {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _state = MutableStateFlow<CreateListingState>(CreateListingState.Idle)
    val state: StateFlow<CreateListingState> = _state

    init {
        viewModelScope.launch {
            when (val r = safeApiCall { RetrofitClient.api.categories() }) {
                is ApiResult.Success -> _categories.value = r.data
                else -> {}
            }
        }
    }

    fun create(categoryId: Int, title: String, description: String, price: String) {
        val priceVal = price.toDoubleOrNull()
        if (title.isBlank() || priceVal == null || categoryId == 0) {
            _state.value = CreateListingState.Error("Please fill in all required fields")
            return
        }
        viewModelScope.launch {
            _state.value = CreateListingState.Loading
            when (val r = safeApiCall {
                RetrofitClient.api.createService(ServiceCreate(categoryId, title, description, priceVal))
            }) {
                is ApiResult.Success -> _state.value = CreateListingState.Success(r.data.id)
                is ApiResult.Error -> _state.value = CreateListingState.Error(r.message)
            }
        }
    }

    fun payListing(serviceId: Int) {
        viewModelScope.launch {
            _state.value = CreateListingState.PayLoading
            when (val r = safeApiCall { RetrofitClient.api.payListing(serviceId) }) {
                is ApiResult.Success -> _state.value = CreateListingState.PaySuccess
                is ApiResult.Error -> _state.value = CreateListingState.Error(r.message)
            }
        }
    }
}

sealed class CreateListingState {
    object Idle : CreateListingState()
    object Loading : CreateListingState()
    data class Success(val serviceId: Int) : CreateListingState()
    object PayLoading : CreateListingState()
    object PaySuccess : CreateListingState()
    data class Error(val message: String) : CreateListingState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(navController: NavController, vm: CreateListingViewModel = viewModel()) {
    val categories by vm.categories.collectAsState()
    val state by vm.state.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(0) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val selectedCategoryName = categories.find { it.id == selectedCategoryId }?.name ?: "Select a category"

    Scaffold(
        topBar = { BackTopBar("New Listing", onBack = { navController.popBackStack() }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
        ) {
            if (state is CreateListingState.PaySuccess) {
                SuccessBanner("Payment done! Your listing is now live.")
                Spacer(Modifier.height(16.dp))
                PrimaryButton(text = "Go to My Listings", onClick = { navController.navigate(Routes.PROVIDER_LISTINGS) { popUpTo(Routes.PROVIDER_DASHBOARD) } })
                return@Column
            }

            if (state is CreateListingState.Success) {
                val serviceId = (state as CreateListingState.Success).serviceId
                SuccessBanner("Listing created! Pay CAD \$5 to make it live.")
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    text = if (state is CreateListingState.PayLoading) "Processing payment…" else "Pay CAD \$5 to Activate",
                    onClick = { vm.payListing(serviceId) },
                    enabled = state !is CreateListingState.PayLoading,
                )
                return@Column
            }

            if (state is CreateListingState.Error) {
                ErrorBanner((state as CreateListingState.Error).message)
                Spacer(Modifier.height(12.dp))
            }

            Text("List your service", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("A one-time CAD \$5 fee activates your listing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))

            // Category dropdown
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                OutlinedTextField(
                    value = selectedCategoryName,
                    onValueChange = {},
                    label = { Text("Category *") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = { selectedCategoryId = cat.id; categoryExpanded = false },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") }, placeholder = { Text("e.g. Home Cleaning — 3 bedroom house") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Hourly Rate (CAD) *") }, leadingIcon = { Icon(Icons.Filled.AttachMoney, contentDescription = null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, placeholder = { Text("Describe your service, experience, teaching style…") }, modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 8)
            Spacer(Modifier.height(24.dp))

            PrimaryButton(
                text = if (state is CreateListingState.Loading) "Creating…" else "Create Listing",
                onClick = { vm.create(selectedCategoryId, title, description, price) },
                enabled = state !is CreateListingState.Loading,
            )
        }
    }
}
