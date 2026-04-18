package com.stepserve.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

class SearchViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<Service>>(emptyList())
    val results: StateFlow<List<Service>> = _results

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadCategories()
        search()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val r = safeApiCall { RetrofitClient.api.categories() }) {
                is ApiResult.Success -> _categories.value = r.data
                else -> {}
            }
        }
    }

    fun search(
        query: String? = null,
        categoryId: Int? = null,
        location: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val r = safeApiCall {
                RetrofitClient.api.searchServices(
                    query = query?.takeIf { it.isNotBlank() },
                    categoryId = categoryId,
                    location = location?.takeIf { it.isNotBlank() },
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                )
            }) {
                is ApiResult.Success -> _results.value = r.data
                is ApiResult.Error -> _error.value = r.message
            }
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController, vm: SearchViewModel = viewModel()) {
    val results by vm.results.collectAsState()
    val categories by vm.categories.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    var query by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }

    fun doSearch() = vm.search(
        query = query,
        categoryId = selectedCategoryId,
        location = location,
        minPrice = minPrice.toDoubleOrNull(),
        maxPrice = maxPrice.toDoubleOrNull(),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Services", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Green700, titleContentColor = White),
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filters", tint = White)
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            // Search input
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search by title, instrument…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotBlank()) IconButton(onClick = { query = ""; doSearch() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 0.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                )
            }

            // Filter panel
            if (showFilters) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Filters", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = { Text("Location") },
                                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = minPrice,
                                    onValueChange = { minPrice = it },
                                    label = { Text("Min $") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                )
                                OutlinedTextField(
                                    value = maxPrice,
                                    onValueChange = { maxPrice = it },
                                    label = { Text("Max $") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { doSearch() },
                                colors = ButtonDefaults.buttonColors(containerColor = Green700),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Apply Filters") }
                        }
                    }
                }
            }

            // Category chips
            if (categories.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        item {
                            CategoryChip("All", selectedCategoryId == null) {
                                selectedCategoryId = null
                                doSearch()
                            }
                        }
                        items(categories) { cat ->
                            CategoryChip(cat.name, selectedCategoryId == cat.id) {
                                selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id
                                doSearch()
                            }
                        }
                    }
                }
            }

            // Results count
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isLoading) "Searching…" else "${results.size} result${if (results.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isLoading) {
                item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Green700) } }
            } else if (error != null) {
                item { ErrorBanner(error!!, onRetry = { doSearch() }) }
            } else if (results.isEmpty()) {
                item { EmptyState("No services found. Try adjusting your filters.", Icons.Filled.SearchOff) }
            } else {
                items(results) { svc ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        ServiceCard(svc) { navController.navigate(Routes.serviceDetail(svc.id)) }
                    }
                }
            }
        }
    }
}
