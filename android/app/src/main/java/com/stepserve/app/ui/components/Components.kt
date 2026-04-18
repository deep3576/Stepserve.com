package com.stepserve.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepserve.app.data.api.Category
import com.stepserve.app.data.api.Service
import com.stepserve.app.ui.theme.*

// ── StepServe Logo — exact staircase + checkmark from website SVG ─────────────
@Composable
fun StepServeLogo(tint: Color = White, textSize: TextUnit = 22.sp, iconSize: Dp = 26.dp) {
    val iconH = (textSize.value * 1.55f).dp
    val iconW = iconH * (51f / 44f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(width = iconW, height = iconH)) {
            val sX = size.width / 51f
            val sY = size.height / 44f

            // 4 ascending bars (shortest left → tallest right)
            listOf(
                floatArrayOf(2f, 28f, 10f, 14f),
                floatArrayOf(15f, 20f, 10f, 22f),
                floatArrayOf(28f, 12f, 10f, 30f),
                floatArrayOf(41f, 4f, 10f, 38f),
            ).forEach { (x, y, w, h) ->
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(x * sX, y * sY),
                    size = Size(w * sX, h * sY),
                    cornerRadius = CornerRadius(2.5f * sX, 2.5f * sY),
                )
            }
            // Soft circle halo behind checkmark
            drawCircle(
                color = tint.copy(alpha = 0.22f),
                radius = 7f * sX,
                center = Offset(46f * sX, 8f * sY),
            )
            // Checkmark: matches SVG polyline 42.5,8 → 45.5,11 → 50,5
            drawPath(
                path = Path().apply {
                    moveTo(42.5f * sX, 8f * sY)
                    lineTo(45.5f * sX, 11f * sY)
                    lineTo(50f * sX, 5f * sY)
                },
                color = tint,
                style = Stroke(width = 2f * sX, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        Spacer(Modifier.width(8.dp))
        Row {
            Text("Step",  color = tint,                   fontSize = textSize, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
            Text("Serve", color = tint.copy(alpha = 0.85f), fontSize = textSize, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
        }
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────
@Composable
fun FullScreenLoader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Green700, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(14.dp))
            Text("Loading…", color = Gray500, fontSize = 14.sp)
        }
    }
}

// ── Banners ───────────────────────────────────────────────────────────────────
@Composable
fun ErrorBanner(message: String, onRetry: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFfdecea), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Error, null, tint = RedError, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, color = RedError, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (onRetry != null) {
            TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Text("Retry", color = RedError, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SuccessBanner(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFe1f5ee), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = Green700, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, color = Green700, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
fun EmptyState(message: String, icon: ImageVector = Icons.Filled.Inbox) {
    Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).background(GreenLight, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Green700, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(message, color = Gray500, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

// ── Section header ────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(20.dp).background(Green700, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(action, color = Green700, fontSize = 12.sp)
            }
        }
    }
}

// ── Category card (list-style, full width) ────────────────────────────────────
@Composable
fun CategoryCard(category: Category, selected: Boolean = false, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Green700 else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (!selected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)) else null,
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(46.dp).background(
                    if (selected) White.copy(alpha = 0.2f) else GreenLight, RoundedCornerShape(12.dp),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(categoryIcon(category.name), null, tint = if (selected) White else Green700, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    category.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = if (selected) White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                val count = category.servicesCount ?: 0
                Text(
                    "$count listing${if (count != 1) "s" else ""}",
                    fontSize = 12.sp, color = if (selected) White.copy(alpha = 0.75f) else Gray500,
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = if (selected) White.copy(alpha = 0.6f) else Gray300, modifier = Modifier.size(18.dp))
        }
    }
}

private fun categoryIcon(name: String): ImageVector = when {
    name.contains("clean", true) || name.contains("maid", true) || name.contains("housekeep", true) -> Icons.Filled.CleaningServices
    name.contains("landscape", true) || name.contains("lawn", true) || name.contains("garden", true) || name.contains("yard", true) -> Icons.Filled.Park
    name.contains("plumb", true) || name.contains("pipe", true) || name.contains("drain", true) -> Icons.Filled.Plumbing
    name.contains("electric", true) || name.contains("wiring", true) || name.contains("outlet", true) -> Icons.Filled.Bolt
    name.contains("carpen", true) || name.contains("wood", true) || name.contains("furniture", true) || name.contains("cabinet", true) || name.contains("joiner", true) -> Icons.Filled.Handyman
    name.contains("paint", true) || name.contains("colour", true) || name.contains("color", true) -> Icons.Filled.FormatPaint
    name.contains("hvac", true) || name.contains("heating", true) || name.contains("cooling", true) || name.contains("air", true) || name.contains("furnace", true) -> Icons.Filled.AcUnit
    name.contains("moving", true) || name.contains("movers", true) || name.contains("reloc", true) || name.contains("hauling", true) -> Icons.Filled.LocalShipping
    name.contains("pet", true) || name.contains("dog", true) || name.contains("cat", true) || name.contains("animal", true) -> Icons.Filled.Pets
    name.contains("window", true) || name.contains("glass", true) -> Icons.Filled.Window
    name.contains("renovat", true) || name.contains("remodel", true) || name.contains("construct", true) -> Icons.Filled.Construction
    name.contains("roof", true) || name.contains("gutter", true) -> Icons.Filled.Roofing
    name.contains("lock", true) || name.contains("security", true) || name.contains("key", true) -> Icons.Filled.Lock
    name.contains("pest", true) || name.contains("extermina", true) || name.contains("insect", true) -> Icons.Filled.BugReport
    else -> Icons.Filled.MiscellaneousServices
}

// ── Service card (list) ───────────────────────────────────────────────────────
@Composable
fun ServiceCard(service: Service, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(52.dp).background(
                    Brush.linearGradient(listOf(Green700, Color(0xFF43A047))), RoundedCornerShape(13.dp),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(service.title.take(1).uppercase(), color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(service.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                if (!service.providerName.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, null, tint = Gray500, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(service.providerName, fontSize = 12.sp, color = Gray500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (!service.location.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = Gray500, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(service.location, fontSize = 12.sp, color = Gray500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (!service.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(service.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(8.dp))
            Surface(color = GreenLight, shape = RoundedCornerShape(8.dp)) {
                Text("CAD ${service.price.toInt()}/hr", color = Green700, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
            }
        }
    }
}

// ── Compact service card (horizontal scroll) ──────────────────────────────────
@Composable
fun ServiceCardCompact(service: Service, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = modifier.width(190.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(Modifier.size(40.dp).background(GreenLight, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.HomeWork, null, tint = Green700, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(service.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            if (!service.providerName.isNullOrBlank()) {
                Text(service.providerName, fontSize = 11.sp, color = Gray500, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Surface(color = GreenLight, shape = RoundedCornerShape(6.dp)) {
                Text("CAD ${service.price.toInt()}/hr", color = Green700, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
            }
        }
    }
}

// ── Stat card ─────────────────────────────────────────────────────────────────
@Composable
fun StatCard(label: String, value: String, icon: ImageVector, tint: Color = Green700) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(tint.copy(alpha = 0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text(label, fontSize = 12.sp, color = Gray500)
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
    Surface(color = bg, shape = RoundedCornerShape(5.dp)) {
        Text(status.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

// ── Primary button ────────────────────────────────────────────────────────────
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick, modifier = modifier.fillMaxWidth().height(52.dp), enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Green700, disabledContainerColor = Gray300),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

// ── Top bar with back ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Green700, titleContentColor = White, navigationIconContentColor = White),
    )
}

// ── Category chip ─────────────────────────────────────────────────────────────
@Composable
fun CategoryChip(name: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick,
        label = { Text(name, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Green700, selectedLabelColor = White),
        shape = RoundedCornerShape(20.dp),
    )
}
