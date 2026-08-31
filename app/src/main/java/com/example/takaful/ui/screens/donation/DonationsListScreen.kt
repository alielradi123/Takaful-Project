package com.example.takaful.ui.screens.donation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takaful.data.model.DonationRecord
import com.example.takaful.ui.theme.*
import com.example.takaful.viewmodel.TakafulViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationsListScreen(
    viewModel: TakafulViewModel,
    onNavigateBack: () -> Unit,
    onDonationClick: (String) -> Unit
) {
    val donations by viewModel.donations.collectAsState()

    var selectedCategory by remember { mutableStateOf("الكل") }
    var selectedStatus by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf("الكل", "مالي", "عيني", "طبي")
    val statuses = listOf("الكل", "قيد الجمع", "تم الاستلام", "تم التوزيع")

    // Filter logic
    val filteredDonations = remember(donations, selectedCategory, selectedStatus, searchQuery) {
        donations.filter { donation ->
            val matchCat = selectedCategory == "الكل" || donation.category == selectedCategory
            val matchStatus = selectedStatus == "الكل" || donation.status == selectedStatus
            val matchSearch = searchQuery.isBlank() ||
                    donation.caseTitle.contains(searchQuery, ignoreCase = true) ||
                    donation.paymentMethod.contains(searchQuery, ignoreCase = true) ||
                    donation.amountFormatted.contains(searchQuery, ignoreCase = true)

            matchCat && matchStatus && matchSearch
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("سجل تبرعاتي", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "رجوع")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Brand600,
                        titleContentColor = MaterialTheme.colorScheme.surface,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // --- 1. Search Box ---
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث باسم الحالة أو الوسيلة البنكية...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    shape = RoundedCornerShape(18.dp),
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Brand600) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Clear, null)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Brand600,
                        unfocusedBorderColor = Color.LightGray.copy(0.5f)
                    )
                )

                // --- 2. Category Filters ---
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "التصنيف",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            val isSel = selectedCategory == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Brand600,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // --- 3. Status Filters ---
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "المرحلة والحالة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(statuses) { stat ->
                            val isSel = selectedStatus == stat
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedStatus = stat },
                                label = { Text(stat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Brand600,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // --- 4. Donations List ---
                if (filteredDonations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.HistoryToggleOff,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray.copy(0.3f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "لا توجد تبرعات تطابق المعايير",
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "أجرِ بحثاً آخر أو قم بتعديل خيارات الفلترة لعرض سجل التبرعات.",
                                fontSize = 12.sp,
                                color = Color.Gray.copy(0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 30.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredDonations, key = { it.id }) { donation ->
                            DonationListItemCard(donation = donation) {
                                onDonationClick(donation.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonationListItemCard(donation: DonationRecord, onClick: () -> Unit) {
    val statusColor = when (donation.status) {
        "تم التوزيع" -> Brand600
        "تم الاستلام" -> Gold500
        else -> Color(0xFFF57C00) // قيد الجمع (Orange)
    }

    val icon = when (donation.category) {
        "عيني" -> Icons.Outlined.Inventory
        "طبي" -> Icons.Outlined.LocalHospital
        else -> Icons.Outlined.MonetizationOn
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            Surface(
                color = Brand600.copy(0.08f),
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Brand600, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    donation.caseTitle.ifBlank { "تبرع عام" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Neutral900
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        donation.amountFormatted,
                        color = Brand600,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        formatTs(donation.effectiveTimestamp),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Status Badge
            Surface(
                color = statusColor.copy(0.12f),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text(
                    donation.status,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun formatTs(ts: Long): String {
    if (ts == 0L) return "—"
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ts))
}

