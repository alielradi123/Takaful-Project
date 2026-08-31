package com.example.takaful.ui.screens.main

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takaful.data.model.CaseItem
import com.example.takaful.ui.theme.Brand600
import com.example.takaful.ui.theme.Gold500
import com.example.takaful.ui.theme.Neutral500
import com.example.takaful.viewmodel.TakafulViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerTasksScreen(
    viewModel: TakafulViewModel,
    onNavigateToCompletion: (String) -> Unit
) {
    val assignedCases by viewModel.assignedCases.collectAsState()
    val allCases by viewModel.cases.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currentUser = viewModel.currentUser

    var isAvailable by remember { mutableStateOf(userProfile.isAvailable) }
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // ضمان بدء مستمع المهام المسندة عند فتح الشاشة
    LaunchedEffect(Unit) {
        val uid = currentUser?.uid
        if (uid != null) {
            viewModel.casesRepository.startVolunteerListener(uid)
        }
    }

    val availableTasks = allCases.filter { it.status == "approved" && !it.isAssigned && it.category == "عيني" }
    val currentTasks = assignedCases.filter { !it.isCompleted }
    val completedTasks = assignedCases.filter { it.isCompleted }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Header
            Surface(
                color = Brand600,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("مهامي الميدانية", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White)
                            Text("مرحباً ${userProfile.displayName}", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Switch(
                                checked = isAvailable,
                                onCheckedChange = { v -> isAvailable = v; viewModel.updateAvailability(v) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Brand600, checkedTrackColor = Color.White, uncheckedThumbColor = Color.White.copy(0.5f), uncheckedTrackColor = Color.White.copy(0.2f))
                            )
                            Text(if (isAvailable) "متاح" else "غير متاح", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VolunteerStatChip("المتاحة", availableTasks.size, Color.White, Modifier.weight(1f))
                        VolunteerStatChip("النشطة", currentTasks.size, Color.White, Modifier.weight(1f))
                        VolunteerStatChip("المكتملة", completedTasks.size, Color.White, Modifier.weight(1f))
                    }
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Brand600,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Brand600
                        )
                    }
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("المهام المتاحة", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("المهام الحالية", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("المهام المنجزة", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) })
            }

            // List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
                val listToShow = when (selectedTab) {
                    0 -> availableTasks
                    1 -> currentTasks
                    else -> completedTasks
                }

                if (listToShow.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.Inbox, null, Modifier.size(64.dp), Neutral500.copy(alpha = 0.4f))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = when (selectedTab) {
                                    0 -> "لا توجد مهام ميدانية متاحة حالياً"
                                    1 -> "ليس لديك مهام قيد التنفيذ"
                                    else -> "لم تقم بإنجاز أي مهمة بعد"
                                },
                                fontWeight = FontWeight.Bold, color = Neutral500
                            )
                        }
                    }
                } else {
                    items(listToShow, key = { it.id }) { case ->
                        if (selectedTab == 0) {
                            AvailableTaskCard(case = case, onAccept = {
                                currentUser?.uid?.let { uid ->
                                    viewModel.assignVolunteer(case.id, uid, onSuccess = {
                                        Toast.makeText(context, "تم قبول المهمة بنجاح", Toast.LENGTH_SHORT).show()
                                        selectedTab = 1 // Switch to Current Tasks
                                    }, onFailure = { e ->
                                        Toast.makeText(context, "فشل قبول المهمة: ${e.message}", Toast.LENGTH_SHORT).show()
                                    })
                                }
                            })
                        } else {
                            VolunteerTaskCard(case = case, onReport = { onNavigateToCompletion(case.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VolunteerStatChip(label: String, count: Int, color: Color, modifier: Modifier) {
    Box(
        modifier = modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = color)
            Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun AvailableTaskCard(case: CaseItem, onAccept: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(case.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.LocationOn, null, Modifier.size(13.dp), Brand600)
                Text(case.location, fontSize = 12.sp, color = Brand600, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(case.effectiveDescription, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), maxLines = 3, overflow = TextOverflow.Ellipsis)
            
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Brand600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("قبول المهمة", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VolunteerTaskCard(case: CaseItem, onReport: () -> Unit) {
    val isCompleted = case.isCompleted
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isCompleted) Brand600.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(if (isCompleted) 0.dp else 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(case.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.LocationOn, null, Modifier.size(13.dp), Neutral500)
                        Text(case.location, fontSize = 12.sp, color = Neutral500)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(50.dp), color = if (isCompleted) Brand600.copy(0.15f) else Gold500.copy(0.15f)) {
                    Text(if (isCompleted) "✓ مكتملة" else "⏳ نشطة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isCompleted) Brand600 else Gold500, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(case.effectiveDescription, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (case.effectiveTarget > 0) {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("تم جمعه: ${case.raisedFormatted}", fontSize = 11.sp, color = Brand600)
                    Text("الهدف: ${case.amountFormatted}", fontSize = 11.sp, color = Neutral500)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(progress = { case.progressPercent }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50.dp)), color = Brand600, trackColor = Brand600.copy(alpha = 0.1f))
            }
            if (isCompleted && case.volunteerReport.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = Brand600.copy(alpha = 0.05f)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(16.dp), Brand600)
                        Text(case.volunteerReport, fontSize = 12.sp, color = Brand600.copy(alpha = 0.8f))
                    }
                }
            }
            if (!isCompleted) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onReport, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand600), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("الإبلاغ عن إنجاز", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
