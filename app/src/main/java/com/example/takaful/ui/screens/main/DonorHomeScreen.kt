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
import com.example.takaful.data.model.DonationRecord
import com.example.takaful.ui.theme.*
import com.example.takaful.viewmodel.TakafulViewModel
import java.util.Calendar
import kotlin.math.roundToInt

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorHomeScreen(
    viewModel: TakafulViewModel,
    onCaseClick: (CaseItem) -> Unit,
    onDonationClick: (DonationRecord) -> Unit,
    onViewAllCases: () -> Unit,
    onViewAllDonations: () -> Unit,
    onNotificationsClick: () -> Unit = {}
) {
    val userProfile    by viewModel.userProfile.collectAsState()
    val notifications  by viewModel.notifications.collectAsState()
    val unreadCount    = notifications.count { !it.isRead }
    val casesHelped    = viewModel.getCasesHelpedCount()
    val recommended    = viewModel.getSmartRecommendations()
    val donations      by viewModel.donations.collectAsState()
    val recentDonations = donations.take(3)
    val sliderCases = remember(recommended) {
        recommended.filter { it.urgencyLevel == "عاجل" || it.urgencyLevel == "urgent" }
            .ifEmpty { recommended }
    }

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
                // ── Header ─────────────────────────────────────────────────────────
                item {
                    HomeHeader(
                        greeting        = greeting,
                        userName        = userProfile.name.ifBlank { "مستخدم تكافل" },
                        unreadCount     = unreadCount,
                        totalDonations  = viewModel.getTotalDonationsFormatted(),
                        casesHelped     = casesHelped,
                        onNotifications = onNotificationsClick,
                        onViewDonations = onViewAllDonations
                    )
                }

                // ── Smart Image Slider ─────────────────────────────────────────────
                
                if (sliderCases.isNotEmpty()) {
                    item {
                        SmartImageSlider(
                            cases = sliderCases.take(5),
                            onClick = { onCaseClick(it) }
                        )
                    }
                }

                // ── Recent Donations ───────────────────────────────────────────────
                if (recentDonations.isNotEmpty()) {
                    item {
                        SectionHeader(title = "آخر تبرعاتك", actionLabel = "عرض الكل", onAction = onViewAllDonations)
                    }
                    items(recentDonations) { donation ->
                        AnimatedVisibility(visible = true, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 40 }) {
                            HomeDonationCard(donation, onClick = { onDonationClick(donation) })
                        }
                    }
                }

                // ── Section title ───────────────────────────────────────────────────
                if (recommended.isNotEmpty()) {
                    item {
                        SectionHeader(title = "حالات مقترحة لك", actionLabel = "عرض الكل", onAction = onViewAllCases)
                    }
                    items(recommended.take(4)) { case ->
                        AnimatedVisibility(visible = true, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 40 }) {
                            HomeCaseCard(case, onClick = { onCaseClick(case) })
                        }
                    }
                } else {
                    item {
                        SectionHeader(title = "الحالات المتاحة", actionLabel = "عرض الكل", onAction = onViewAllCases)
                    }
                    item { EmptyState() }
                }
            }
        }
    }
}

// ── Header ─────────────────────────────────────────────────────────────────
@Composable
private fun HomeHeader(
    greeting: String, userName: String, unreadCount: Int,
    totalDonations: String, casesHelped: Int,
    onNotifications: () -> Unit, onViewDonations: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Brand800, Brand600)))
    ) {
        // نقاط زخرفية
        Box(Modifier.size(200.dp).offset((-40).dp, (-40).dp).background(Brush.radialGradient(listOf(Color.White.copy(0.05f), Color.Transparent)), CircleShape))
        Box(Modifier.size(120.dp).align(Alignment.TopEnd).offset(30.dp, 10.dp).background(Brush.radialGradient(listOf(Gold500.copy(0.12f), Color.Transparent)), CircleShape))

        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 24.dp)) {
            // Greeting row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("$greeting،", color = Color.White.copy(0.8f), fontSize = 14.sp)
                    Text(userName.split(" ").firstOrNull() ?: userName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                // Notification Bell
                Box(modifier = Modifier.clickable { onNotifications() }) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(0.15f), CircleShape)
                            .border(1.dp, Color.White.copy(0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Notifications, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .background(SemanticError, CircleShape)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (unreadCount > 9) "9+" else "$unreadCount", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Stats Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape  = RoundedCornerShape(20.dp),
                color  = Color.White.copy(0.12f),
                border = BorderStroke(1.dp, Color.White.copy(0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(label = "إجمالي تبرعاتك", value = "$totalDonations", icon = Icons.Outlined.MonetizationOn, onClick = onViewDonations)
                    Box(Modifier.width(1.dp).height(40.dp).background(Color.White.copy(0.3f)))
                    StatItem(label = "حالات دعمتها", value = "$casesHelped حالة", icon = Icons.Outlined.Favorite)
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = Gold400, modifier = Modifier.size(16.dp))
            Text(label, color = Color.White.copy(0.8f), fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// ── Section Header ─────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(4.dp).height(20.dp).background(Brush.verticalGradient(listOf(Brand600, Teal600)), RoundedCornerShape(2.dp)))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Neutral900)
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = Brand600, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = Brand600, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Case Card ──────────────────────────────────────────────────────────────
@Composable
fun HomeCaseCard(caseItem: CaseItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // صورة الحالة
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brand50),
                contentAlignment = Alignment.Center
            ) {
                if (!caseItem.imageUrl.isNullOrEmpty()) {
                    AsyncImage(caseItem.imageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Outlined.VolunteerActivism, null, tint = Brand500, modifier = Modifier.size(32.dp))
                }
            }

            // معلومات الحالة
            Column(modifier = Modifier.weight(1f)) {
                // تاغات
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniChip(caseItem.category, Brand100, Brand700)
                    if (caseItem.urgencyLevel == "عاجل") MiniChip("عاجل", Color(0xFFFEE2E2), SemanticError)
                    else if (caseItem.urgencyLevel == "متوسط") MiniChip("متوسط", Gold50, Gold700)
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    caseItem.title,
                    fontSize  = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color     = Neutral900,
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(10.dp))

                val progress = (caseItem.progress ?: 0f).coerceIn(0f, 1f)
                val pct = (progress * 100).roundToInt()

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color        = Brand500,
                    trackColor   = Brand100,
                )

                Spacer(Modifier.height(4.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$pct% مكتمل", fontSize = 11.sp, color = Neutral500)
                    Text("${String.format(java.util.Locale.ENGLISH, "%,d", caseItem.amountRaised.toLong())} ج.س", fontSize = 11.sp, color = Brand600, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MiniChip(label: String, bg: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 10.sp, color = textColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.VolunteerActivism, null, tint = Neutral300, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text("لا توجد حالات حالياً", color = Neutral500, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun HomeDonationCard(donation: DonationRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
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
                Icon(Icons.Outlined.VolunteerActivism, null, tint = Brand500)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = donation.caseTitle.ifBlank { "تبرع عام" },
                    fontWeight = FontWeight.Bold,
                    color = Neutral900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(donation.amountFormatted, color = Brand600, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    val statusColor = when(donation.status) {
                        "تم التوزيع" -> Color(0xFF10B981)
                        "تم الاستلام" -> Color(0xFF3B82F6)
                        else -> Gold500
                    }
                    Text(donation.status, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = Neutral400, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SmartImageSlider(cases: List<CaseItem>, onClick: (CaseItem) -> Unit) {
    if (cases.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { cases.size })
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(3500)
            val nextPage = (pagerState.currentPage + 1) % cases.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp
        ) { page ->
            val caseItem = cases[page]
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onClick(caseItem) },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val firstImg = if (!caseItem.imageUrls.isNullOrEmpty()) caseItem.imageUrls.first() else caseItem.imageUrl
                    if (!firstImg.isNullOrEmpty()) {
                        AsyncImage(
                            model = firstImg,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Brand100),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.VolunteerActivism, null, tint = Brand500, modifier = Modifier.size(48.dp))
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (caseItem.urgencyLevel == "عاجل" || caseItem.urgencyLevel == "urgent") {
                                MiniChip("عاجل", Color(0xFFEF4444), Color.White)
                            }
                            MiniChip(caseItem.category, Brand500, Color.White)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = caseItem.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(cases.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) Brand500 else Neutral300
                val width = if (pagerState.currentPage == iteration) 20.dp else 8.dp
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .height(8.dp)
                        .width(width)
                )
            }
        }
    }
}
