package com.stepserve.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.stepserve.app.data.auth.TokenManager
import com.stepserve.app.ui.components.*
import com.stepserve.app.ui.navigation.Routes
import com.stepserve.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val _user = MutableStateFlow<UserMe?>(null)
    val user: StateFlow<UserMe?> = _user

    private val _profileState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val profileState: StateFlow<ProfileUpdateState> = _profileState

    init { load() }

    fun load() {
        viewModelScope.launch {
            when (val r = safeApiCall { RetrofitClient.api.me() }) {
                is ApiResult.Success -> _user.value = r.data
                else -> {}
            }
        }
    }

    fun updateProfile(fullName: String, bio: String, location: String, hourlyRate: String) {
        viewModelScope.launch {
            _profileState.value = ProfileUpdateState.Loading
            val rate = hourlyRate.toDoubleOrNull()
            when (val r = safeApiCall {
                RetrofitClient.api.upsertProfile(
                    ProviderProfileUpsert(
                        fullName = fullName,
                        bio = bio.takeIf { it.isNotBlank() },
                        location = location.takeIf { it.isNotBlank() },
                        hourlyRate = rate,
                    )
                )
            }) {
                is ApiResult.Success -> _profileState.value = ProfileUpdateState.Success
                is ApiResult.Error -> _profileState.value = ProfileUpdateState.Error(r.message)
            }
        }
    }
}

sealed class ProfileUpdateState {
    object Idle : ProfileUpdateState()
    object Loading : ProfileUpdateState()
    object Success : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, vm: ProfileViewModel = viewModel()) {
    val user by vm.user.collectAsState()
    val profileState by vm.profileState.collectAsState()
    val role = TokenManager.getRole()

    var fullName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var hourlyRate by remember { mutableStateOf("") }
    var showProviderFields by remember { mutableStateOf(role == "provider") }

    if (!TokenManager.isLoggedIn()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Gray300, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Sign in to view your profile")
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Sign In", onClick = { navController.navigate(Routes.LOGIN) }, modifier = Modifier.width(200.dp))
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Green700, titleContentColor = White),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
        ) {
            // Avatar + basic info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GreenLight),
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Green700, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            (TokenManager.getEmail()?.take(1) ?: "?").uppercase(),
                            color = White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(TokenManager.getEmail() ?: "", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(role?.replaceFirstChar { it.uppercase() } ?: "", color = Green700, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        if (user?.isActive == false) {
                            Text("Account disabled", color = RedError, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Role-specific quick actions
            Spacer(Modifier.height(20.dp))
            Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))

            when (role) {
                "provider" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton("My Listings", Icons.Filled.List, Modifier.weight(1f)) { navController.navigate(Routes.PROVIDER_LISTINGS) }
                        ActionButton("Upload Docs", Icons.Filled.Upload, Modifier.weight(1f)) { navController.navigate(Routes.PROVIDER_DOCUMENTS) }
                    }
                }
                "customer" -> {
                    ActionButton("My Bookings", Icons.Filled.DateRange, Modifier.fillMaxWidth()) { navController.navigate(Routes.CUSTOMER_BOOKINGS) }
                }
                "admin" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton("Users", Icons.Filled.People, Modifier.weight(1f)) { navController.navigate(Routes.ADMIN_USERS) }
                        ActionButton("Bookings", Icons.Filled.DateRange, Modifier.weight(1f)) { navController.navigate(Routes.ADMIN_BOOKINGS) }
                    }
                }
            }

            // Provider profile form
            if (role == "provider") {
                Spacer(Modifier.height(24.dp))
                Text("Provider Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))

                if (profileState is ProfileUpdateState.Success) SuccessBanner("Profile updated!")
                if (profileState is ProfileUpdateState.Error) ErrorBanner((profileState as ProfileUpdateState.Error).message)

                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location (city, province)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = hourlyRate, onValueChange = { hourlyRate = it }, label = { Text("Hourly Rate (CAD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    text = if (profileState is ProfileUpdateState.Loading) "Saving…" else "Save Profile",
                    onClick = { vm.updateProfile(fullName, bio, location, hourlyRate) },
                    enabled = profileState !is ProfileUpdateState.Loading && fullName.isNotBlank(),
                )
            }

            Spacer(Modifier.height(32.dp))

            // Sign out
            OutlinedButton(
                onClick = {
                    TokenManager.clear()
                    navController.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError),
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Green700, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        }
    }
}
