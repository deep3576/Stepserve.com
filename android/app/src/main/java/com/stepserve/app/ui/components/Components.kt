package com.stepserve.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepserve.app.data.api.Service
import com.stepserve.app.ui.theme.*

// ── Loading ──────────────────────────────────────────────────────────────────

@Composable
fun FullScreenLoader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Green700)
    }
}

// ── Error / empty states ─────────────────────────────────────────────────────

@Composable
fun ErrorBanner(message: String, onRetry: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFfdecea)),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Error, contentDescription = null, tint = RedError)
            Spacer(Modifier.width(8.dp))
            Text(message, color = RedError, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (onRetry != null) {
                TextButton(onClick = onRetry) { Text("Retry", color = RedError) }
            }
        }
    }
}

@Composable
fun SuccessBanner(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFe1f5ee)),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Green700)
            Spacer(Modifier.width(8.dp))
            Text(message, color = Green700, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun EmptyState(message: String, icon: ImageVector = Icons.Filled.Inbox) {
    Column(
        Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = Gray300, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, color = Gray500, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action, color = Green700, fontSize = 13.sp)
            }
        }
    }
}

// ── Service card ──────────────────────────────────────────────────────────────

@Composable
fun ServiceCard(service: Service, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(GreenLight, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = service.title.take(1).uppercase(),
                        color = Green700,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        service.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!service.providerName.isNullOrBlank()) {
                        Text(
                            service.providerName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    "CAD ${service.price.toInt()}/hr",
                    color = Green700,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            if (!service.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!service.location.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Gray500, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(service.location, style = MaterialTheme.typography.bodySmall, color = Gray500)
                }
            }
        }
    }
}

// ── Category chip ─────────────────────────────────────────────────────────────

@Composable
fun CategoryChip(name: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(name, fontSize = 13.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Green700,
            selectedLabelColor = White,
        ),
    )
}

// ── Stat card ─────────────────────────────────────────────────────────────────

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, tint: Color = Green700) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(tint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Status badge ──────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(status: String) {
    val (bg, fg) = when (status.lowercase()) {
        "paid", "confirmed", "active" -> Color(0xFFe1f5ee) to Green700
        "pending" -> Color(0xFFfff8e1) to AmberWarning
        "completed" -> Color(0xFFe3f2fd) to BlueInfo
        "inactive", "cancelled" -> Color(0xFFfdecea) to RedError
        else -> Color(0xFFf5f5f5) to Gray700
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            status.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Primary green button ──────────────────────────────────────────────────────

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Green700),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

// ── Top app bar with back ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Green700,
            titleContentColor = White,
            navigationIconContentColor = White,
        ),
    )
}
