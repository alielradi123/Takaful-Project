package com.example.takaful.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasesScreen(viewModel: TakafulViewModel, onCaseClick: (CaseItem) -> Unit) {
    val cases     by viewModel.cases.collectAsState()
    val isLoading by viewModel.isLoadingCases.collectAsState()

    var selectedCategory by remember { mutableStateOf("الكل") }
    var searchQuery      by remember { mutableStateOf("") }

    val filtered = cases.filter {
        (selectedCategory == "الكل" || it.category == selectedCategory) &&
        (searchQuery.isEmpty() || it.title.contains(searchQuery, true) || it.location.contains(searchQuery, true))
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

            // ── Header ──────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Brand800, Brand600)))) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 20.dp)) {
                    Text("الحالات", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("ابحث وادعم من يحتاج مساعدتك", fontSize = 13.sp, color = Color.White.copy(0.75f))

                    Spacer(Modifier.height(16.dp))

                    // Search Bar
                    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 0.dp) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث عن حالة...", color = Neutral400, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Neutral400, modifier = Modifier.size(20.dp)) },
                            trailingIcon = if (searchQuery.isNotEmpty()) ({
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Outlined.Close, null, tint = Neutral400, modifier = Modifier.size(16.dp))
                                }
                            }) else null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor     = Neutral900,
                                unfocusedTextColor   = Neutral900,
                                cursorColor          = Brand600,
                                focusedContainerColor    = Color.White,
                                unfocusedContainerColor  = Color.White
                            )
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Category Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val cats = listOf("الكل" to cases.size, "مالي" to cases.count { it.category == "مالي" },
                            "عيني" to cases.count { it.category == "عيني" }, "طبي" to cases.count { it.category == "طبي" })
                        items(cats) { (cat, count) ->
                            val sel = selectedCategory == cat
                            Surface(
                                modifier = Modifier.clickable { selectedCategory = cat },
                                shape = RoundedCornerShape(20.dp),
                                color = if (sel) Color.White else Color.White.copy(0.18f)
                            ) {
                                Text(
                                    "$cat ($count)",
                                    color = if (sel) Brand700 else Color.White,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── List ────────────────────────────────────────────────────────
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Brand600)
                }
                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.SearchOff, null, tint = Neutral300, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("لا توجد حالات مطابقة", color = Neutral400, fontSize = 16.sp)
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filtered) { case ->
                        CaseListCard(case, onClick = { onCaseClick(case) })
                    }
                }
            }
        }
    }
}

@Composable
fun CaseListCard(caseItem: CaseItem, onClick: () -> Unit) {
    val nf = NumberFormat.getNumberInstance(Locale("ar", "SA"))
    val progress = caseItem.progress.coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape  = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column {
            // صورة
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                val firstImage = caseItem.imageUrls.firstOrNull().takeIf { !it.isNullOrBlank() } ?: caseItem.imageUrl
                if (firstImage.isNotEmpty()) {
                    AsyncImage(firstImage, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)

                } else {
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Brand100, Teal100))), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.VolunteerActivism, null, tint = Brand500, modifier = Modifier.size(52.dp))
                    }
                }
                // Gradient overlay
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.3f)))))
                // Badges
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MiniChip(caseItem.category, Color.White, Brand700)
                    if (caseItem.urgencyLevel == "عاجل") MiniChip("🔴 عاجل", Color(0xFFFEE2E2), SemanticError)
                }
            }

            // Content
            Column(modifier = Modifier.padding(16.dp)) {
                Text(caseItem.title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Neutral900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(caseItem.description, fontSize = 13.sp, color = Neutral500, maxLines = 2, lineHeight = 20.sp, overflow = TextOverflow.Ellipsis)

                if (caseItem.category == "مالي") {
                    Spacer(Modifier.height(12.dp))
                    val nf = NumberFormat.getInstance(Locale.ENGLISH)
                    nf.maximumFractionDigits = 0
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("تم جمع: ${nf.format(caseItem.amountRaised)} ج.س", color = Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${(progress * 100).toInt()}%", color = Neutral500, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                        color      = Brand500,
                        trackColor = Brand100
                    )
                }

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick  = onClick,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Brand600)
                ) {
                    Icon(Icons.Outlined.VolunteerActivism, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تبرع الآن", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// HeaderChip للتوافق
@Composable
fun HeaderChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color.White else Color.White.copy(0.2f)
    ) {
        Text(text, color = if (isSelected) Brand700 else Color.White, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

// DonationDarkCard alias للتوافق مع الكود القديم
@Composable
fun DonationDarkCard(caseItem: CaseItem, onClick: (CaseItem) -> Unit) {
    CaseListCard(caseItem = caseItem, onClick = { onClick(caseItem) })
}
