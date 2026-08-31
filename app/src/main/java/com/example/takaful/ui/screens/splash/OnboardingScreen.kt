package com.example.takaful.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.takaful.R
import com.example.takaful.ui.theme.*
import com.example.takaful.utils.SharedPrefsHelper
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class Page(val title: String, val desc: String, val image: Int, val accent: Color)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs   = SharedPrefsHelper(context)
    val scope   = rememberCoroutineScope()

    val pages = listOf(
        Page("أصالة التكافل",     "منصة تكافل تُجسّد أسمى معاني الإنسانية وتبني جسوراً من الأمل لمن هم في أمس الحاجة.",     R.drawable.onboarding1, Brand600),
        Page("شفافية بلا حدود",  "نظام متكامل يضمن وصول مساهماتك لمستحقيها بكل شفافية ووضوح، خطوة بخطوة.",                 R.drawable.onboarding2, Teal600),
        Page("كن شريكاً في التغيير", "بلمسة واحدة يمكنك إضاءة حياة بأكملها. انضم إلى ركب المبادرين.",                         R.drawable.onboarding3, Gold700),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val currentPage = pagerState.currentPage
    val accent = pages[currentPage].accent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // خلفية متدرجة ناعمة تتغير مع الصفحة
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pos ->
            val off = abs(((pagerState.currentPage - pos) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f))
            OnboardingPage(
                page = pages[pos],
                modifier = Modifier.graphicsLayer {
                    alpha = 1f - off * 0.4f
                    translationX = off * 80f
                }
            )
        }

        // زر تخطي
        if (currentPage < pages.size - 1) {
            TextButton(
                onClick = { prefs.isFirstTimeLaunch = false; navController.navigate("login") { popUpTo("onboarding") { inclusive = true } } },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 20.dp)
            ) {
                Text("تخطي", color = accent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }

        // Controls bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dots indicator
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pages.forEachIndexed { i, page ->
                    val selected = i == currentPage
                    val w by animateDpAsState(if (selected) 28.dp else 8.dp, spring(stiffness = Spring.StiffnessMedium), label = "w$i")
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(w)
                            .clip(CircleShape)
                            .background(if (selected) page.accent else Neutral300)
                            .clickable { scope.launch { pagerState.animateScrollToPage(i) } }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            if (currentPage == pages.size - 1) {
                Button(
                    onClick = { prefs.isFirstTimeLaunch = false; navController.navigate("login") { popUpTo("onboarding") { inclusive = true } } },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand600)
                ) {
                    Text("ابدأ رحلتك الآن", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(currentPage + 1) } },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, accent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                    ) {
                        Text("التالي", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(page: Page, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))

        // بطاقة الصورة
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .shadow(16.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .border(2.dp, Brush.linearGradient(listOf(page.accent, Gold500.copy(0.6f))), RoundedCornerShape(28.dp))
        ) {
            Image(
                painter = painterResource(page.image),
                contentDescription = page.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Overlay gradient للنص
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.35f)))
                    )
            )
        }

        Spacer(Modifier.height(36.dp))

        // شريط لوني تحت العنوان
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(page.accent, Gold500)))
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = page.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Neutral900,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = page.desc,
            fontSize = 16.sp,
            color = Neutral500,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )

        Spacer(Modifier.height(160.dp))
    }
}
