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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    private val _home = MutableStateFlow<HomeData?>(null)
    val home: StateFlow<HomeData?> = _home

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val r = safeApiCall { RetrofitClient.api.home() }) {
                is ApiResult.Success -> _home.value = r.data
                is ApiResult.Error -> _error.value = r.message
            }
            _loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, vm: HomeViewModel = viewModel()) {
    val home by vm.home.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { StepServeLogo() },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Green700),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // Hero banner
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp).background(
                        Brush.verticalGradient(listOf(Green700, Color(0xFF43A047)))
                    ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text("Find trusted local service pros", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Verified, insured, and reviewed — right in your neighbourhood", color = White.copy(alpha = 0.85f), fontSize = 14.sp)
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            color = White,
                            shape = RoundedCornerShape(8.dp),
                            onClick = { navController.navigate(Routes.SEARCH) },
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Search, null, tint = Gray500, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Search for a service…", color = Gray500, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // ── Sign-in prompt for guests ──────────────────────────────────
            if (!TokenManager.isLoggedIn()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { navController.navigate(Routes.LOGIN) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Green700),
                            shape = RoundedCornerShape(10.dp),
                        ) { Text("Sign In", fontWeight = FontWeight.SemiBold) }
                        OutlinedButton(
                            onClick = { navController.navigate(Routes.REGISTER) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Green700),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Green700),
                        ) { Text("Create Account", fontWeight = FontWeight.SemiBold) }
                    }
                }
            }

            if (loading) {
                item { FullScreenLoader() }
                return@LazyColumn
            }

            error?.let { msg ->
                item {
                    Column(Modifier.padding(16.dp)) {
                        ErrorBanner(msg, onRetry = { vm.load() })
                    }
                }
                return@LazyColumn
            }

            home?.let { data ->

                // ── Categories ────────────────────────────────────────────
                if (data.categories.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(20.dp))
                        SectionHeader("Browse Categories", action = "See All") {
                            navController.navigate(Routes.SEARCH)
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    items(data.categories.chunked(2)) { row ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { cat ->
                                Box(Modifier.weight(1f)) {
                                    CategoryCard(category = cat, onClick = {
                                        navController.navigate("${Routes.SEARCH}?categoryId=${cat.id}&categoryName=${cat.name}")
                                    })
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                // ── Featured services ──────────────────────────────────────
                if (data.featured.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        SectionHeader("Featured Services")
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(data.featured) { svc ->
                                ServiceCardCompact(service = svc, onClick = {
                                    navController.navigate(Routes.serviceDetail(svc.id))
                                })
                            }
                        }
                    }
                }

                // ── Top Locations ──────────────────────────────────────────
                if (data.topLocations.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(20.dp))
                        SectionHeader("Popular Cities")
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(data.topLocations) { loc ->
                                Card(
                                    onClick = { navController.navigate("${Routes.SEARCH}?location=${loc.location}") },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = GreenLight),
                                    elevation = CardDefaults.cardElevation(0.dp),
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Filled.LocationOn, null, tint = Green700, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Column {
                                            Text(loc.location, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Green700,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${loc.listingsCount} listings", fontSize = 10.sp, color = Gray500)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Latest listings ────────────────────────────────────────
                if (data.latest.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(20.dp))
                        SectionHeader("Latest Listings")
                        Spacer(Modifier.height(8.dp))
                    }
                    items(data.latest) { svc ->
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            ServiceCard(service = svc, onClick = {
                                navController.navigate(Routes.serviceDetail(svc.id))
                            })
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}
