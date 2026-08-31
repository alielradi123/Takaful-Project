package com.example.takaful.ui.screens.cases

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takaful.data.model.CaseItem
import com.example.takaful.ui.theme.*
import com.example.takaful.viewmodel.TakafulViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiaryRequestsScreen(
    viewModel: TakafulViewModel,
    onNavigateBack: () -> Unit,
    onCaseClick: (String) -> Unit
) {
    val allCases by viewModel.cases.collectAsState()
    val currentUser = viewModel.currentUser

    // Filter cases submitted by the current beneficiary user
    val myCases = remember(allCases, currentUser) {
        allCases.filter { it.beneficiaryId == currentUser?.uid || it.createdBy == currentUser?.uid }
    }

    var visibleItems by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visibleItems = true }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("طلباتي", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("تتبّع حالة طلباتك المقدّمة", fontSize = 12.sp, color = Color.White.copy(0.75f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Brand600
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->

            if (myCases.isEmpty()) {
                // ── Empty State ────────────────────────────────────────────
                Box(
                    modifier        = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(40.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(Brand50, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Inbox,
                                null,
                                tint     = Brand400,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "لا توجد طلبات بعد",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "يمكنك تقديم طلب مساعدة جديد من خلال الضغط على زر \"طلب مساعدة\" في الشاشة الرئيسية.",
                            fontSize  = 13.sp,
                            color     = MaterialTheme.colorScheme.onSurface.copy(0.55f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier        = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding  = PaddingValues(top = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // ── Stats Header ───────────────────────────────────
                        RequestsStatsHeader(myCases)
                    }

                    itemsIndexed(myCases, key = { _, c -> c.id }) { index, case ->
                        AnimatedVisibility(
                            visible     = visibleItems,
                            enter       = fadeIn(tween(300 + index * 80)) +
                                          slideInVertically(tween(350 + index * 80)) { it / 2 }
                        ) {
                            BeneficiaryRequestCard(
                                case     = case,
                                onClick  = { onCaseClick(case.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestsStatsHeader(cases: List<CaseItem>) {
    val pending   = cases.count { it.status == "pending" }
    val approved  = cases.count { it.status == "approved" }
    val completed = cases.count { it.isCompleted }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Brand800, Brand600)))
                .padding(20.dp)
        ) {
            Column {
                Text("ملخص طلباتك", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatusStatBubble("قيد المراجعة", pending,  Color(0xFFFFC107))
                    StatusStatBubble("نشطة",         approved, Brand400)
                    StatusStatBubble("مكتملة",       completed, Color(0xFF4CAF50))
                }
            }
        }
    }
}

@Composable
private fun StatusStatBubble(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier         = Modifier
                .size(52.dp)
                .background(Color.White.copy(0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(count.toString(), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = Color.White.copy(0.85f), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BeneficiaryRequestCard(
    case: CaseItem,
    onClick: () -> Unit
) {
    val (statusLabel, statusColor, statusIcon) = when {
        case.isCompleted         -> Triple("مكتملة",        Color(0xFF4CAF50),  Icons.Outlined.CheckCircle)
        case.status == "approved"-> Triple("نشطة وتجمع تبرعات", Brand600,       Icons.AutoMirrored.Outlined.TrendingUp)
        case.status == "rejected"-> Triple("مرفوضة",        MaterialTheme.colorScheme.error, Icons.Outlined.Cancel)
        else                     -> Triple("قيد المراجعة",  Color(0xFFF57C00),  Icons.Outlined.HourglassTop)
    }

    Card(
        onClick   = onClick,
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(3.dp, RoundedCornerShape(18.dp)),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status Icon
                Box(
                    modifier         = Modifier
                        .size(44.dp)
                        .background(statusColor.copy(0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(case.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(case.location, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
                Surface(
                    color = statusColor.copy(0.12f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        statusLabel,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = statusColor,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Progress bar for approved/active cases
            if (case.status == "approved" && !case.isCompleted) {
                val raised   = case.amountRaised   ?: 0.0
                val required = case.amountRequired ?: 1.0
                val progress = (raised / required.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("تم جمع: ${raised.toLong()} ج.س", fontSize = 12.sp, color = Brand600, fontWeight = FontWeight.SemiBold)
                    Text("الهدف: ${required.toLong()} ج.س", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier  = Modifier.fillMaxWidth().height(6.dp).padding(0.dp),
                    color     = Brand600,
                    trackColor = Brand50,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Category, null, tint = Gold500, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(case.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AccessTime, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(formatDate(case.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
                }
            }
        }
    }
}

private fun formatDate(ts: Long): String {
    if (ts == 0L) return "—"
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ts))
}
