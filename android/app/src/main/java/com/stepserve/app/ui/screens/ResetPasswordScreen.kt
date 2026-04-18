package com.stepserve.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

sealed class ResetState {
    object Idle    : ResetState()
    object Loading : ResetState()
    object Success : ResetState()
    data class Error(val message: String) : ResetState()
}

class ResetPasswordViewModel : ViewModel() {
    private val _state = MutableStateFlow<ResetState>(ResetState.Idle)
    val state: StateFlow<ResetState> = _state

    fun resetPassword(email: String, code: String, newPassword: String, confirmPassword: String) {
        when {
            code.length != 6 -> { _state.value = ResetState.Error("Enter the 6-digit code from your email"); return }
            newPassword.length < 8 -> { _state.value = ResetState.Error("Password must be at least 8 characters"); return }
            newPassword != confirmPassword -> { _state.value = ResetState.Error("Passwords do not match"); return }
        }
        viewModelScope.launch {
            _state.value = ResetState.Loading
            when (val r = safeApiCall {
                RetrofitClient.api.resetPassword(ResetPasswordRequest(email, code, newPassword))
            }) {
                is ApiResult.Success -> _state.value = ResetState.Success
                is ApiResult.Error   -> _state.value = ResetState.Error(r.message)
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    navController: NavController,
    email: String,
    vm: ResetPasswordViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter Reset Code", fontWeight = FontWeight.SemiBold) },
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
            Spacer(Modifier.height(16.dp))

            if (state is ResetState.Success) {
                // ── Success state ──────────────────────────────────────────
                Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    Surface(shape = androidx.compose.foundation.shape.CircleShape, color = GreenLight) {
                        Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null,
                                tint = Green700, modifier = Modifier.size(40.dp))
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("Password updated!", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Your password has been changed. You can now sign in with your new password.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))
                PrimaryButton(text = "Sign In", onClick = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                })
            } else {
                // ── Enter code + new password ──────────────────────────────
                Text("Check your email", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "We sent a 6-digit code to\n$email\nEnter it below along with your new password.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))

                if (state is ResetState.Error) {
                    ErrorBanner((state as ResetState.Error).message)
                    Spacer(Modifier.height(16.dp))
                }

                // Code field — styled prominently
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) code = it },
                    label = { Text("6-digit code") },
                    leadingIcon = { Icon(Icons.Filled.Pin, contentDescription = null) },
                    placeholder = { Text("• • • • • •") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, letterSpacing = 8.sp,
                    ),
                )
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New password") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("Minimum 8 characters", fontSize = 11.sp) },
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm new password") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                )
                Spacer(Modifier.height(28.dp))

                PrimaryButton(
                    text = if (state is ResetState.Loading) "Updating…" else "Reset Password",
                    onClick = { vm.resetPassword(email, code, newPassword, confirmPassword) },
                    enabled = state !is ResetState.Loading && code.length == 6
                        && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { navController.navigate(Routes.FORGOT_PASSWORD) }) {
                    Text("Resend code", color = Green700, fontSize = 14.sp)
                }
            }
        }
    }
}
