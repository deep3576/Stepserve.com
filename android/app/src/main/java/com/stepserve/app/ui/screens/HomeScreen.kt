package com.stepserve.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

class HomeViewModel : ViewModel() {
    private val _homeData = MutableStateFlow<HomeData?>(null)
    val homeData: StateFlow<HomeData?> = _homeData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val result = safeApiCall { RetrofitClient.api.home() }) {
                is ApiResult.Success -> _homeData.value = result.data
                is ApiResult.Error -> _error.value = result.message
            }
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, vm: HomeViewModel = viewModel()) {
    val homeData by vm.homeData.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("StepServe", fontWeight = FontWeight.Bold, color = White, fontSize = 20.sp)
                    }
                },
                actions = {
                    if (!TokenManager.isLoggedIn()) {
                        TextButton(onClick = { navController.navigate(Routes.LOGIN) }) {
                            Text("Sign In", color = White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Green700),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            // Hero search bar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Green700)
                        .padding(16.dp, 0.dp, 16.dp, 24.dp),
                ) {
                    Column {
                        Text(
                            "Find a music teacher",
                            color = White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Connect with qualified instructors near you",
                            color = White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by instrument, genre…", color = Gray300) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Gray500) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = {
                                        navController.navigate("${Routes.SEARCH}?query=${searchQuery}")
                                    }) {
                                        Icon(Icons.Filled.ArrowForward, contentDescription = "Search", tint = Green700)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = White,
                                unfocusedContainerColor = White,
                                focusedBorderColor = White,
                                unfocusedBorderColor = White,
                            ),
                            shape = RoundedCornerShape(10.dp),
                        )
                    }
                }
            }

            if (isLoading) {
                item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Green700) } }
            } else if (error != null) {
                item { ErrorBanner(error!!, onRetry = { vm.load() }) }
            } else if (homeData != null) {
                val data = homeData!!

                // Top locations strip
                if (data.topLocations.isNotEmpty()) {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 12.dp),
                        ) {
                            items(data.topLocations) { loc ->
                                AssistChip(
                                    onClick = { navController.navigate("${Routes.SEARCH}?location=${loc.location}") },
                                    label = { Text("${loc.location} (${loc.listingsCount})", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                )
                            }
                        }
                    }
                }

                // Categories
                if (data.categories.isNotEmpty()) {
                    item { SectionHeader("Browse Categories", action = "All", onAction = { navController.navigate(Routes.SEARCH) }) }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 8.dp),
                        ) {
                            items(data.categories) { cat ->
                                Card(
                                    onClick = { navController.navigate("${Routes.SEARCH}?categoryId=${cat.id}") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = GreenLight),
                                    modifier = Modifier.width(110.dp),
                                ) {
                                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(cat.name.take(1).uppercase(), fontSize = 24.sp, color = Green700, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(4.dp))
                                        Text(cat.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                        Text("${cat.servicesCount}", fontSize = 11.sp, color = Gray500)
                                    }
                                }
                            }
                        }
                    }
                }

                // Featured
                if (data.featured.isNotEmpty()) {
                    item { SectionHeader("Featured Teachers") }
                    items(data.featured) { svc ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            ServiceCard(svc) { navController.navigate(Routes.serviceDetail(svc.id)) }
                        }
                    }
                }

                // Latest
                if (data.latest.isNotEmpty()) {
                    item { SectionHeader("Latest Listings", action = "See all", onAction = { navController.navigate(Routes.SEARCH) }) }
                    items(data.latest.take(6)) { svc ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            ServiceCard(svc) { navController.navigate(Routes.serviceDetail(svc.id)) }
                        }
                    }
                }

                // CTA for providers
                if (!TokenManager.isLoggedIn()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = GreenLight),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text("Are you a music teacher?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Green700)
                                Spacer(Modifier.height(4.dp))
                                Text("List your services for a one-time CAD \$5 fee and reach thousands of students.", style = MaterialTheme.typography.bodySmall, color = Gray700)
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { navController.navigate(Routes.REGISTER) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Green700),
                                    shape = RoundedCornerShape(8.dp),
                                ) { Text("Get Started", fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
            }
        }
    }
}
