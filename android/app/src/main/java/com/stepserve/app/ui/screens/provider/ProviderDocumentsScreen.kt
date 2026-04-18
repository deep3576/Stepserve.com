package com.stepserve.app.ui.screens.provider

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

class ProviderDocumentsViewModel : ViewModel() {
    private val _uploads = MutableStateFlow<List<ProviderUpload>>(emptyList())
    val uploads: StateFlow<List<ProviderUpload>> = _uploads

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val r = safeApiCall { RetrofitClient.api.listUploads() }) {
                is ApiResult.Success -> _uploads.value = r.data
                is ApiResult.Error -> _error.value = r.message
            }
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDocumentsScreen(navController: NavController, vm: ProviderDocumentsViewModel = viewModel()) {
    val uploads by vm.uploads.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Documents", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Green700, titleContentColor = White, navigationIconContentColor = White),
                actions = { IconButton(onClick = { vm.load() }) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = White) } },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GreenLight),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = Green700)
                            Spacer(Modifier.width(8.dp))
                            Text("Upload Documents", fontWeight = FontWeight.Bold, color = Green700)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Upload certifications (PDF, JPG, PNG — max 10 MB). Use the + button in your mobile file manager or share from your files app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray700,
                        )
                    }
                }
            }

            if (isLoading) {
                item { Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Green700) } }
            } else if (error != null) {
                item { ErrorBanner(error!!, onRetry = { vm.load() }) }
            } else if (uploads.isEmpty()) {
                item { EmptyState("No documents uploaded yet.", Icons.Filled.Description) }
            } else {
                item { Text("${uploads.size} document${if (uploads.size != 1) "s" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(uploads) { upload ->
                    UploadCard(upload)
                }
            }
        }
    }
}

@Composable
private fun UploadCard(upload: ProviderUpload) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            val icon = when {
                upload.contentType.contains("pdf") -> Icons.Filled.PictureAsPdf
                upload.contentType.contains("image") -> Icons.Filled.Image
                else -> Icons.Filled.Description
            }
            Icon(icon, contentDescription = null, tint = Green700, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(upload.fileName, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1)
                Text(
                    "${upload.contentType} · ${upload.fileSize / 1024} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                )
                if (!upload.createdAt.isNullOrBlank()) {
                    Text(upload.createdAt.take(10), style = MaterialTheme.typography.bodySmall, color = Gray500)
                }
            }
        }
    }
}
