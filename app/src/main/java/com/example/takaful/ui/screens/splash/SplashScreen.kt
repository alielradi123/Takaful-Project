package com.example.takaful.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.example.takaful.R
import com.example.takaful.ui.screens.auth.findFragmentActivity
import com.example.takaful.ui.screens.auth.showBiometricPrompt
import com.example.takaful.ui.theme.*
import com.example.takaful.utils.SharedPrefsHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val auth    = FirebaseAuth.getInstance()
    val prefs   = SharedPrefsHelper(context)

    LaunchedEffect(Unit) {
        delay(2600)
        if (prefs.isFirstTimeLaunch) {
            navController.navigate("onboarding") { popUpTo("splash") { inclusive = true } }
        } else {
            val user = auth.currentUser
            if (user != null) {
                if (prefs.isBiometricEnabled) {
                    val activity = context.findFragmentActivity()
                    if (activity != null) {
                        val biometricOk = suspendCancellableCoroutine { cont ->
                            showBiometricPrompt(activity) { ok ->
                                if (cont.isActive) cont.resume(ok)
                            }
                        }
                        // نجاح: الداشبورد | فشل أو إلغاء: شاشة الدخول مع إمكانية إدخال كلمة المرور
                        navController.navigate(if (biometricOk) "dashboard" else "login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        // تعذّر الحصول على FragmentActivity – تجاوز مباشرة
                        navController.navigate("dashboard") { popUpTo("splash") { inclusive = true } }
                    }
                } else {
                    navController.navigate("dashboard") { popUpTo("splash") { inclusive = true } }
                }
            } else {
                navController.navigate("login") { popUpTo("splash") { inclusive = true } }
            }
        }
    }

    // خلفية متدرجة من أعمق الزمرد إلى الفيروزي
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // دوائر زخرفية خلفية
        DecorativeCircles()
        // المحتوى الرئيسي
        SplashContent()
    }
}

@Composable
private fun DecorativeCircles() {
    val inf = rememberInfiniteTransition(label = "deco")
    val pulse by inf.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-80).dp)
                .graphicsLayer { alpha = 0.08f * pulse; scaleX = pulse; scaleY = pulse }
                .background(Brush.radialGradient(listOf(Gold500, Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = 60.dp)
                .graphicsLayer { alpha = 0.10f * pulse }
                .background(Brush.radialGradient(listOf(Teal400, Color.Transparent)))
        )
    }
}

@Composable
private fun SplashContent() {
    val scaleAnim   = remember { Animatable(0.4f) }
    val alphaAnim   = remember { Animatable(0f) }
    val slideAnim   = remember { Animatable(60f) }

    LaunchedEffect(Unit) {
        launch { scaleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        launch { alphaAnim.animateTo(1f, tween(1000)) }
        launch { slideAnim.animateTo(0f, tween(900, easing = FastOutSlowInEasing)) }
    }

    val inf = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by inf.animateFloat(
        initialValue = -600f, targetValue = 1600f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "shimmerX"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Brand600.copy(alpha = 0.8f),
            Brand500.copy(alpha = 1.0f),
            Brand800.copy(alpha = 1.0f),
            Brand500.copy(alpha = 1.0f),
            Brand600.copy(alpha = 0.8f),
        ),
        start = Offset(shimmerX, 0f),
        end   = Offset(shimmerX + 500f, 300f)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(alphaAnim.value)
            .graphicsLayer { translationY = slideAnim.value }
    ) {
        // اللوغو
        Box(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer { scaleX = scaleAnim.value; scaleY = scaleAnim.value },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.takaful_logo),
                contentDescription = "تكافل",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(28.dp))

        // اسم التطبيق بتأثير shimmer ذهبي
        Text(
            text = "تـكـافـل",
            style = TextStyle(
                brush = shimmerBrush,
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "نبض العطاء · إشراقة الأمل",
            style = TextStyle(
                color = Neutral700,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        )

        Spacer(Modifier.height(60.dp))

        // مؤشر تحميل أنيق
        LoadingDots()
    }
}

@Composable
private fun LoadingDots() {
    val inf = rememberInfiniteTransition(label = "dots")
    val dots = (0..2).map { i ->
        inf.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(600, delayMillis = i * 180, easing = EaseInOutSine),
                RepeatMode.Reverse
            ),
            label = "dot$i"
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dots.forEach { anim ->
            val a by anim
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { alpha = a; scaleX = a; scaleY = a }
                    .background(
                        Brush.radialGradient(listOf(Gold400, Gold500)),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}
