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

class AdminCategoriesViewModel : ViewModel() {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _createState = MutableStateFlow<String?>(null)
    val createState: StateFlow<String?> = _createState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val r = safeApiCall { RetrofitClient.api.categories() }) {
                is ApiResult.Success -> _categories.value = r.data
                is ApiResult.Error -> _error.value = r.message
            }
            _isLoading.value = false
        }
    }

    fun createCategory(name: String, slug: String) {
        if (name.isBlank() || slug.isBlank()) {
            _createState.value = "error:Name and slug are required"
            return
        }
        viewModelScope.launch {
            _createState.value = "loading"
            when (val r = safeApiCall { RetrofitClient.api.createCategory(CategoryCreate(name, slug)) }) {
                is ApiResult.Success -> {
                    _createState.value = "success"
                    load()
                }
                is ApiResult.Error -> _createState.value = "error:${r.message}"
            }
        }
    }

    fun clearCreateState() { _createState.value = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCategoriesScreen(navController: NavController, vm: AdminCategoriesViewModel = viewModel()) {
    val categories by vm.categories.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    val createState by vm.createState.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newSlug by remember { mutableStateOf("") }

    LaunchedEffect(createState) {
        if (createState == "success") {
            showDialog = false
            newName = ""
            newSlug = ""
            vm.clearCreateState()
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false; vm.clearCreateState() },
            title = { Text("New Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (createState?.startsWith("error:") == true)
                        ErrorBanner(createState!!.removePrefix("error:"))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it; newSlug = it.lowercase().replace(" ", "-") },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = newSlug,
                        onValueChange = { newSlug = it },
                        label = { Text("Slug (URL-friendly)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.createCategory(newName, newSlug) },
                    enabled = createState != "loading",
                    colors = ButtonDefaults.buttonColors(containerColor = Green700),
                ) {
                    Text(if (createState == "loading") "Creating…" else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; vm.clearCreateState() }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories", fontWeight = FontWeight.SemiBold) },
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = Green700) {
                Icon(Icons.Filled.Add, contentDescription = "Add Category", tint = White)
            }
        }
    ) { padding ->
        when {
            isLoading -> FullScreenLoader()
            error != null -> ErrorBanner(error!!, onRetry = { vm.load() })
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        "${categories.size} categories",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(categories) { cat ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    cat.name.take(1).uppercase(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Green700,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(cat.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(cat.slug, style = MaterialTheme.typography.bodySmall, color = Gray500)
                            }
                            Surface(
                                color = GreenLight,
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    "${cat.servicesCount} services",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    color = Green700,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
