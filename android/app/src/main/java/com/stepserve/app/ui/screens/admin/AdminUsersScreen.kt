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

class AdminUsersViewModel : ViewModel() {
    private val _users = MutableStateFlow<List<AdminUser>>(emptyList())
    val users: StateFlow<List<AdminUser>> = _users

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val r = safeApiCall { RetrofitClient.api.adminUsers() }) {
                is ApiResult.Success -> _users.value = r.data
                is ApiResult.Error -> _error.value = r.message
            }
            _isLoading.value = false
        }
    }

    fun toggleStatus(userId: Int, currentActive: Boolean) {
        viewModelScope.launch {
            safeApiCall { RetrofitClient.api.adminUpdateUserStatus(userId, !currentActive) }
            load()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(navController: NavController, vm: AdminUsersViewModel = viewModel()) {
    val users by vm.users.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter { it.email.contains(searchQuery, ignoreCase = true) || it.role.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Management", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Green700, titleContentColor = White, navigationIconContentColor = White),
                actions = { IconButton(onClick = { vm.load() }) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = White) } },
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by email or role…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp),
                singleLine = true,
            )

            when {
                isLoading -> FullScreenLoader()
                error != null -> ErrorBanner(error!!, onRetry = { vm.load() })
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text("${filtered.size} user${if (filtered.size != 1) "s" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(filtered) { user ->
                        AdminUserCard(user, onToggle = { vm.toggleStatus(user.id, user.isActive) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUserCard(user: AdminUser, onToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(user.email, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(user.role)
                    StatusBadge(if (user.isActive) "active" else "inactive")
                }
                if (!user.createdAt.isNullOrBlank()) {
                    Text("Joined ${user.createdAt.take(10)}", style = MaterialTheme.typography.bodySmall, color = Gray500)
                }
            }
            Switch(
                checked = user.isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = Green700),
            )
        }
    }
}
