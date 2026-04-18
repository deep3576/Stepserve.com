package com.stepserve.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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

// ── ViewModel ─────────────────────────────────────────────────────────────────

sealed class ForgotState {
    object Idle    : ForgotState()
    object Loading : ForgotState()
    data class Sent(val email: String) : ForgotState()
    data class Error(val message: String) : ForgotState()
}

class ForgotPasswordViewModel : ViewModel() {
    private val _state = MutableStateFlow<ForgotState>(ForgotState.Idle)
    val state: StateFlow<ForgotState> = _state

    fun sendCode(email: String) {
        if (email.isBlank()) { _state.value = ForgotState.Error("Email is required"); return }
        viewModelScope.launch {
            _state.value = ForgotState.Loading
            when (val r = safeApiCall { RetrofitClient.api.forgotPassword(ForgotPasswordRequest(email.trim())) }) {
                is ApiResult.Success -> _state.value = ForgotState.Sent(email.trim())
                is ApiResult.Error   -> _state.value = ForgotState.Error(r.message)
            }
        }
    }
}

// ── Screen: enter email ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavController, vm: ForgotPasswordViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var email by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is ForgotState.Sent) {
            navController.navigate(Routes.resetPassword((state as ForgotState.Sent).email)) {
                popUpTo(Routes.FORGOT_PASSWORD) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forgot Password", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Green700, titleContentColor = White,
                    navigationIconContentColor = White,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // Icon
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = androidx.compose.foundation.shape.CircleShape, color = GreenLight) {
                    Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.LockReset, contentDescription = null,
                            tint = Green700, modifier = Modifier.size(40.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Reset your password", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Enter your account email and we'll send a 6-digit reset code.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            if (state is ForgotState.Error) {
                ErrorBanner((state as ForgotState.Error).message)
                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email address") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(24.dp))

            PrimaryButton(
                text = if (state is ForgotState.Loading) "Sending…" else "Send Reset Code",
                onClick = { vm.sendCode(email) },
                enabled = state !is ForgotState.Loading,
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { navController.popBackStack() }) {
                Text("Back to Sign In", color = Green700, fontSize = 14.sp)
            }
        }
    }
}
