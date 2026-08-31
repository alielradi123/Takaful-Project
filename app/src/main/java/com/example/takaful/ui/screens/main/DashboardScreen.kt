package com.example.takaful.ui.screens.main

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.takaful.data.model.CaseItem
import com.example.takaful.ui.screens.cases.BeneficiaryRequestsScreen
import com.example.takaful.ui.screens.cases.CaseDetailsScreen
import com.example.takaful.ui.screens.cases.SubmitCaseScreen
import com.example.takaful.ui.screens.donation.DonationFormScreen
import com.example.takaful.ui.theme.*
import com.example.takaful.viewmodel.TakafulViewModel

data class BottomNavItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector = icon)

@Composable
fun DashboardScreen(navController: NavController, viewModel: TakafulViewModel) {
    val context       = LocalContext.current
    val userProfile   by viewModel.userProfile.collectAsState()
    val assignedCases by viewModel.assignedCases.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initializeListeners()
        viewModel.loadUserProfile()
    }

    var selectedTab     by remember { mutableIntStateOf(0) }

    // Inner navigation states
    var showCaseDetails     by remember { mutableStateOf<CaseItem?>(null) }
    var showDonationForm    by remember { mutableStateOf<CaseItem?>(null) }
    var showGeneralDonation by remember { mutableStateOf(false) }
    var showMyRequests      by remember { mutableStateOf(false) }

    // ── Determine Role ──────────────────────────────────────────────────────
    val isVolunteer   = userProfile.isVolunteer
    val isBeneficiary = userProfile.isBeneficiary
    val isDonor       = !isVolunteer && !isBeneficiary  // donor or member (default)

    // ── Build role-specific bottom nav items ────────────────────────────────
    val bottomItems = buildList {
        when {
            isBeneficiary -> {
                add(BottomNavItem("الرئيسية", Icons.Outlined.Home))
                add(BottomNavItem("طلباتي", Icons.Outlined.Inbox))
                add(BottomNavItem("تقديم طلب", Icons.Outlined.AddCircleOutline))
                add(BottomNavItem("الدعم", Icons.Outlined.ChatBubbleOutline))
                add(BottomNavItem("ملفي", Icons.Outlined.Person))
            }
            isVolunteer -> {
                add(BottomNavItem("المهام المتاحة", Icons.AutoMirrored.Outlined.List))
                add(BottomNavItem("مهامي", Icons.Outlined.Task))
                add(BottomNavItem("المحادثات", Icons.Outlined.ChatBubbleOutline))
                add(BottomNavItem("ملفي", Icons.Outlined.Person))
            }
            else -> { // Donor
                add(BottomNavItem("الرئيسية", Icons.Outlined.Home))
                add(BottomNavItem("الحالات", Icons.Outlined.People))
                add(BottomNavItem("تبرع", Icons.Outlined.VolunteerActivism))
                add(BottomNavItem("الدعم", Icons.Outlined.ChatBubbleOutline))
                add(BottomNavItem("ملفي", Icons.Outlined.Person))
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor   = Neutral400,
                tonalElevation = 0.dp,
                modifier       = Modifier.shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                bottomItems.forEachIndexed { index, item ->
                    val isSelected    = selectedTab == index
                    
                    // Profile tab index is the last item for all roles now
                    val profileIndex = bottomItems.size - 1
                    
                    // Badges for volunteer's "My Tasks"
                    val badgeCount = if (isVolunteer && index == 1) assignedCases.count { !it.isCompleted } else 0

                    NavigationBarItem(
                        icon = {
                            BadgedBox(badge = { if (badgeCount > 0) Badge { Text("$badgeCount") } }) {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(if (isSelected) 26.dp else 24.dp)
                                )
                            }
                        },
                        label    = { Text(item.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        selected = isSelected,
                        onClick  = {

                            showCaseDetails = null
                            showDonationForm    = null
                            showGeneralDonation = false
                            showMyRequests      = false

                            // Profile tab navigates away
                            if (index == profileIndex) {
                                navController.navigate("profile")
                                return@NavigationBarItem
                            }

                            // Sub-actions based on role
                            if (isBeneficiary && index == 2) {
                                // Instead of navigating away, we can just change tab and render SubmitCaseScreen inline
                                selectedTab = 2
                                return@NavigationBarItem
                            }
                            if (isDonor && index == 2) {
                                showGeneralDonation = true
                                selectedTab = 2
                                return@NavigationBarItem
                            }

                            selectedTab = index
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = Brand600,
                            selectedTextColor   = Brand600,
                            indicatorColor      = Brand100,
                            unselectedIconColor = Neutral400,
                            unselectedTextColor = Neutral400
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // ── Overlay screens ──────────────────────────────────────
                showCaseDetails != null -> {
                    val caseItem = showCaseDetails!!
                    CaseDetailsScreen(
                        caseItem       = caseItem,
                        onNavigateBack = { showCaseDetails = null },
                        onDonateClick  = { showCaseDetails = null; showDonationForm = caseItem }
                    )
                }
                showDonationForm != null -> {
                    val caseItem = showDonationForm!!
                    DonationFormScreen(
                        viewModel         = viewModel,
                        caseId            = caseItem.id,
                        caseTitle         = caseItem.title,
                        category          = caseItem.category,
                        onNavigateBack    = { showDonationForm = null },
                        onDonationSuccess = { donationId ->
                            showDonationForm = null
                            navController.navigate("donation_tracking/$donationId")
                            Toast.makeText(context, "تم تسجيل تبرعك بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                showGeneralDonation -> {
                    DonationFormScreen(
                        viewModel         = viewModel,
                        caseId            = null,
                        caseTitle         = "تبرع عام",
                        category          = "مالي",
                        onNavigateBack    = { showGeneralDonation = false; selectedTab = 0 },
                        onDonationSuccess = { donationId ->
                            showGeneralDonation = false; selectedTab = 0
                            navController.navigate("donation_tracking/$donationId")
                            Toast.makeText(context, "تم تسجيل تبرعك بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                
                // ── Tab content ──────────────────────────────────────────
                else -> {
                    when {
                        isBeneficiary -> {
                            when (selectedTab) {
                                0 -> BeneficiaryHomeScreen(
                                        viewModel = viewModel,
                                        onViewMyRequests = { selectedTab = 1 }
                                     )
                                1 -> BeneficiaryRequestsScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { selectedTab = 0 },
                                        onCaseClick = { caseId ->
                                            val case = viewModel.cases.value.find { it.id == caseId }
                                            case?.let { showCaseDetails = it }
                                        }
                                     )
                                2 -> SubmitCaseScreen(
                                        viewModel = viewModel,
                                        onCaseSubmitted = { selectedTab = 1 }
                                     )
                                3 -> ChatsScreen(viewModel = viewModel)
                            }
                        }
                        isVolunteer -> {
                            when (selectedTab) {
                                0 -> CasesScreen(
                                        viewModel   = viewModel,
                                        onCaseClick = { showCaseDetails = it }
                                     ) // "المهام المتاحة"
                                1 -> VolunteerTasksScreen(
                                        viewModel = viewModel,
                                        onNavigateToCompletion = { caseId ->
                                            navController.navigate("task_completion/$caseId")
                                        }
                                     ) // "مهامي"
                                2 -> ChatsScreen(viewModel = viewModel) // "المحادثات"
                            }
                        }
                        else -> { // Donor
                            when (selectedTab) {
                                0 -> DonorHomeScreen( // which acts as DonorHomeScreen
                                        viewModel            = viewModel,
                                        onCaseClick          = { showCaseDetails = it },
                                        onDonationClick      = { navController.navigate("donation_tracking/${it.id}") },
                                        onViewAllCases       = { selectedTab = 1 },
                                        onViewAllDonations   = { navController.navigate("donations_history") },
                                        onNotificationsClick = { navController.navigate("notifications") }
                                     )
                                1 -> CasesScreen(
                                        viewModel   = viewModel,
                                        onCaseClick = { showCaseDetails = it }
                                     )
                                3 -> ChatsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
