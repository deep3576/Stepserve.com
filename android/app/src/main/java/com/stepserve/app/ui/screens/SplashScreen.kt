package com.stepserve.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.stepserve.app.data.auth.TokenManager
import com.stepserve.app.ui.navigation.Routes
import com.stepserve.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(1000)
        val destination = when {
            !TokenManager.isLoggedIn() -> Routes.HOME
            else -> when (TokenManager.getRole()) {
                "provider" -> Routes.PROVIDER_DASHBOARD
                "admin" -> Routes.ADMIN_OVERVIEW
                else -> Routes.HOME
            }
        }
        navController.navigate(destination) {
            popUpTo(Routes.SPLASH) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Green700),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "StepServe",
                color = White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Text(
                "Local services marketplace",
                color = White.copy(alpha = 0.75f),
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(40.dp))
            CircularProgressIndicator(color = White.copy(alpha = 0.7f), strokeWidth = 2.dp)
        }
    }
}
