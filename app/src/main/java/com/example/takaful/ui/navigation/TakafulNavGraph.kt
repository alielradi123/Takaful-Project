package com.example.takaful.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.takaful.ui.screens.auth.LoginScreen
import com.example.takaful.ui.screens.auth.RegisterScreen
import com.example.takaful.ui.screens.main.DashboardScreen
import com.example.takaful.ui.screens.profile.ProfileScreen
import com.example.takaful.ui.screens.splash.OnboardingScreen
import com.example.takaful.ui.screens.splash.SplashScreen
import com.example.takaful.ui.screens.splash.MaintenanceScreen
import com.example.takaful.ui.screens.splash.UpdateRequiredScreen
import com.example.takaful.ui.screens.splash.AccountSuspendedScreen
import com.example.takaful.ui.screens.splash.PendingVerificationScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.takaful.ui.screens.donation.DonationsListScreen
import com.example.takaful.ui.screens.donation.DonationTrackingScreen
import com.example.takaful.ui.screens.donation.RecurringDonationScreen
import com.example.takaful.ui.screens.cases.BeneficiaryRequestsScreen
import com.example.takaful.ui.screens.volunteer.TaskCompletionScreen
import com.example.takaful.viewmodel.TakafulViewModel

@Composable
fun TakafulNavGraph() {
    val navController = rememberNavController()
    val viewModel: TakafulViewModel = viewModel()
    
    val sysSettings by viewModel.sysSettings.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val context = LocalContext.current
    val currentVersion = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }

    fun compareVersions(v1: String, v2: String): Int {
        val p1 = v1.split(".").mapNotNull { it.toIntOrNull() }
        val p2 = v2.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(p1.size, p2.size)
        for (i in 0 until maxLen) {
            val num1 = p1.getOrElse(i) { 0 }
            val num2 = p2.getOrElse(i) { 0 }
            if (num1 != num2) return num1.compareTo(num2)
        }
        return 0
    }

    val isUpdateRequired = sysSettings.forceAppUpdate && compareVersions(currentVersion, sysSettings.minAppVersion) < 0

    if (sysSettings.appMaintenanceMode) {
        MaintenanceScreen(contactEmail = sysSettings.contactEmail)
    } else if (isUpdateRequired) {
        UpdateRequiredScreen(playStoreLink = sysSettings.playStoreLink)
    } else if (viewModel.isLoggedIn && userProfile.status == "suspended") {
        AccountSuspendedScreen(contactEmail = sysSettings.contactEmail, onLogout = {
            viewModel.logout(context)
            // Navigation to splash or login handles itself as we rely on isLoggedIn state
            // But since NavHost is removed, we just let recomposition handle it.
        })
    } else if (viewModel.isLoggedIn && userProfile.status == "pending_verification") {
        PendingVerificationScreen(onLogout = {
            viewModel.logout(context)
        })
    } else {
        NavHost(
            navController = navController,
            startDestination = "splash"
        ) {
            // --- 0. Splash & Onboarding ---
            composable("splash") {
                SplashScreen(navController = navController)
            }
            composable("onboarding") {
                OnboardingScreen(navController = navController)
            }

            // --- 1. شاشة تسجيل الدخول ---
            composable("login") {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onRegisterClick = {
                        navController.navigate("register")
                    }
                )
            }

            // --- 2. شاشة إنشاء حساب جديد ---
            composable("register") {
                RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = {
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onBackToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            // --- 3. الشاشة الرئيسية (لوحة التحكم Dashboard) ---
            composable("dashboard") {
                DashboardScreen(navController = navController, viewModel = viewModel)
            }

            // --- 4. شاشة الملف الشخصي ---
            composable("profile") {
                ProfileScreen(navController = navController, viewModel = viewModel)
            }

            // --- 5. شاشة سجل التبرعات ---
            composable("donations_history") {
                DonationsListScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onDonationClick = { donationId ->
                        navController.navigate("donation_tracking/$donationId")
                    }
                )
            }

            // --- 6. شاشة تتبع حالة التبرع ---
            composable("donation_tracking/{donationId}") { backStackEntry ->
                val donationId = backStackEntry.arguments?.getString("donationId") ?: ""
                DonationTrackingScreen(
                    viewModel = viewModel,
                    donationId = donationId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // --- 7. شاشة الإشعارات ---
            composable("notifications") {
                com.example.takaful.ui.screens.profile.NotificationsScreen(
                    navController = navController,
                    viewModel = viewModel
                )
            }

            // --- 8. شاشة طلبات المستفيد ---
            composable("beneficiary_requests") {
                BeneficiaryRequestsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onCaseClick = { caseId ->
                        // Can show case details or action
                    }
                )
            }

            // --- 9. شاشة إنجاز المهمة للمتطوع ---
            composable("task_completion/{caseId}") { backStackEntry ->
                val caseId = backStackEntry.arguments?.getString("caseId") ?: ""
                TaskCompletionScreen(
                    caseId = caseId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onCompletionSuccess = {
                        navController.popBackStack()
                    }
                )
            }

            // --- 10. شاشة التبرعات الدورية ---
            composable("recurring_donations") {
                RecurringDonationScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
