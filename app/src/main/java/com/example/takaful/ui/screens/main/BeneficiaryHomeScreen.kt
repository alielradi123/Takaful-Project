package com.example.takaful.ui.screens.main

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.example.takaful.data.model.CaseItem
import com.example.takaful.ui.theme.*
import com.example.takaful.viewmodel.TakafulViewModel
import java.util.Calendar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiaryHomeScreen(
    viewModel: TakafulViewModel,
    onViewMyRequests: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val cases by viewModel.cases.collectAsState()
    
    // Filter cases that belong to this beneficiary
    val myCases = cases.filter { it.beneficiaryId == viewModel.currentUser?.uid }
    val activeRequests = myCases.count { it.status == "approved" }
    val pendingRequests = myCases.count { it.status == "pending" }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11  -> "صباح الخير"
        in 12..17 -> "مساء الخير"
        else      -> "طابت مساؤك"
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.initializeListeners()
                viewModel.loadUserProfile()
                coroutineScope.launch {
                    delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                // ── Hero Header ─────────────────────────────────────────────────────────
                item {
                    BeneficiaryHeroHeader(
                        greeting = greeting,
                        userName = userProfile.name.ifBlank { "مستخدم تكافل" },
                        avatarUrl = userProfile.photoURL,
                        activeCount = activeRequests,
                        pendingCount = pendingRequests,
                        onViewRequests = onViewMyRequests
                    )
                }

                // ── Recent Requests ───────────────────────────────────────────────
                if (myCases.isNotEmpty()) {
                    item {
                        SectionHeader(title = "طلباتي الأخيرة", actionLabel = "عرض الكل", onAction = onViewMyRequests)
                    }
                    items(myCases.take(3)) { case ->
                        AnimatedVisibility(visible = true, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 40 }) {
                            BeneficiaryRequestCard(caseItem = case)
                        }
                    }
                } else {
                    item {
                        EmptyBeneficiaryState()
                    }
                }
            }
        }
    }
}

@Composable
private fun BeneficiaryHeroHeader(
    greeting: String,
    userName: String,
    avatarUrl: String,
    activeCount: Int,
    pendingCount: Int,
    onViewRequests: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Brand800, Brand600)))
    ) {
        Box(Modifier.size(200.dp).offset((-40).dp, (-40).dp).background(Brush.radialGradient(listOf(Color.White.copy(0.05f), Color.Transparent)), CircleShape))
        Box(Modifier.size(120.dp).align(Alignment.TopEnd).offset(30.dp, 10.dp).background(Brush.radialGradient(listOf(Gold500.copy(0.12f), Color.Transparent)), CircleShape))

        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 24.dp)) {
            // Greeting row with avatar
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("$greeting،", color = Color.White.copy(0.8f), fontSize = 14.sp)
                    Text(userName.split(" ").firstOrNull() ?: userName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                
                // Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(0.2f), CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Outlined.Person, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active Requests Card
                Surface(
                    modifier = Modifier.weight(1f).clickable { onViewRequests() },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(0.15f),
                    border = BorderStroke(1.dp, Color.White.copy(0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(32.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.CheckCircle, null, tint = Gold400, modifier = Modifier.size(18.dp))
                            }
                            Text("الطلبات النشطة", color = Color.White.copy(0.9f), fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("$activeCount", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                // Pending Requests Card
                Surface(
                    modifier = Modifier.weight(1f).clickable { onViewRequests() },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(0.15f),
                    border = BorderStroke(1.dp, Color.White.copy(0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(32.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.HourglassEmpty, null, tint = Color(0xFF93C5FD), modifier = Modifier.size(18.dp))
                            }
                            Text("قيد المراجعة", color = Color.White.copy(0.9f), fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("$pendingCount", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyBeneficiaryState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.AutoMirrored.Outlined.Assignment, null, tint = Neutral300, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text("ليس لديك طلبات حالية", color = Neutral500, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text("انقر على 'تقديم طلب' لبدء طلب مساعدة", color = Neutral400, fontSize = 13.sp)
    }
}

@Composable
fun BeneficiaryRequestCard(caseItem: CaseItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Brand50, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (caseItem.category) {
                        "طبي" -> Icons.Outlined.MedicalServices
                        "عيني" -> Icons.Outlined.Inventory
                        else -> Icons.Outlined.MonetizationOn
                    },
                    null, tint = Brand500
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = caseItem.title,
                    fontWeight = FontWeight.Bold,
                    color = Neutral900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(caseItem.amountFormatted, color = Brand600, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    val (statusText, statusColor) = when(caseItem.status) {
                        "approved" -> "مقبول" to Color(0xFF10B981)
                        "pending" -> "قيد المراجعة" to Gold500
                        "rejected" -> "مرفوض" to SemanticError
                        else -> caseItem.status to Neutral500
                    }
                    Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
