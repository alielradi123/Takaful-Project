package com.example.takaful.ui.screens.cases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takaful.data.model.CaseItem
import com.example.takaful.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailsScreen(
    caseItem: CaseItem,
    onNavigateBack: () -> Unit,
    onDonateClick: () -> Unit
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("تفاصيل الحالة", fontWeight = FontWeight.Bold) },
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
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onDonateClick,
                    containerColor = Brand600,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Outlined.VolunteerActivism, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تبرع الآن", fontWeight = FontWeight.Bold)
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Image
                item {
                    val firstImage = caseItem.imageUrls.firstOrNull().takeIf { !it.isNullOrBlank() } ?: caseItem.imageUrl
                    if (!firstImage.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = firstImage,
                            contentDescription = "صورة الحالة",

                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
                                .background(Brush.verticalGradient(listOf(Brand800, Brand600))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                when (caseItem.category) {
                                    "طبي" -> Icons.Outlined.MedicalServices
                                    "عيني" -> Icons.Outlined.Inventory
                                    else -> Icons.Outlined.MonetizationOn
                                },
                                contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(80.dp)
                            )
                        }
                    }
                }

                // بطاقة المعلومات الأساسية
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).shadow(6.dp, RoundedCornerShape(30.dp)),
                        shape = RoundedCornerShape(30.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Brand400,
                                    shape = CircleShape,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            when (caseItem.category) {
                                                "طبي" -> Icons.Outlined.MedicalServices
                                                "عيني" -> Icons.Outlined.Inventory
                                                else -> Icons.Outlined.MonetizationOn
                                            },
                                            contentDescription = null, tint = Brand600, modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(caseItem.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${caseItem.category} • ${caseItem.location}", color = Brand600, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = TakafulLightGray, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            // Progress
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("تم جمع ${caseItem.raisedFormatted}", fontWeight = FontWeight.Bold, color = Brand600)
                                Text("من ${caseItem.amountFormatted}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { caseItem.progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = Brand600,
                                trackColor = Brand600.copy(alpha = 0.15f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(caseItem.progress * 100).toInt()}% مكتمل",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gold500,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }

                // القصة
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).shadow(6.dp, RoundedCornerShape(30.dp)),
                        shape = RoundedCornerShape(30.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Text("قصة الحالة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Brand600)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = caseItem.story,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                lineHeight = 24.sp
                            )
                        }
                    }
                }

                // معلومات إضافية
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).shadow(6.dp, RoundedCornerShape(30.dp)),
                        shape = RoundedCornerShape(30.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Text("معلومات إضافية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Brand600)
                            Spacer(modifier = Modifier.height(14.dp))
                            DetailRow("الموقع", caseItem.location)
                            DetailRow("التصنيف", caseItem.category)
                            DetailRow("المبلغ المطلوب", caseItem.amountFormatted)
                            DetailRow("المبلغ المتبقي", caseItem.remainingFormatted)
                        }
                    }
                }

                // مشاركة الحملة
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).shadow(6.dp, RoundedCornerShape(30.dp)),
                        shape = RoundedCornerShape(30.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Text("شارك الحملة مع أصدقائك", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Brand600)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("الدال على الخير كفاعله، ساهم بنشر هذه الحالة الإنسانية.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(16.dp))
                            val context = LocalContext.current
                            val shareText = "ساهم في دعم حالة إنسانية عاجلة: ${caseItem.title}\nالمبلغ المطلوب: ${caseItem.amountFormatted}\nالموقع: ${caseItem.location}\n\nتطبيق تكافل للعمل الخيري والتطوعي."
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ShareIconButton(
                                    label = "واتساب",
                                    icon = Icons.Outlined.Share,
                                    color = Color(0xFF25D366),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                            setPackage("com.whatsapp")
                                        }
                                        try {
                                            context.startActivity(sendIntent)
                                        } catch (e: Exception) {
                                            val generalIntent = android.content.Intent.createChooser(
                                                android.content.Intent().apply {
                                                    action = android.content.Intent.ACTION_SEND
                                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                                    type = "text/plain"
                                                },
                                                "مشاركة عبر"
                                            )
                                            context.startActivity(generalIntent)
                                        }
                                    }
                                )
                                
                                ShareIconButton(
                                    label = "إكس / تويتر",
                                    icon = Icons.Outlined.PostAdd,
                                    color = Color(0xFF111111),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                            setPackage("com.twitter.android")
                                        }
                                        try {
                                            context.startActivity(sendIntent)
                                        } catch (e: Exception) {
                                            val generalIntent = android.content.Intent.createChooser(
                                                android.content.Intent().apply {
                                                    action = android.content.Intent.ACTION_SEND
                                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                                    type = "text/plain"
                                                },
                                                "مشاركة عبر"
                                            )
                                            context.startActivity(generalIntent)
                                        }
                                    }
                                )

                                ShareIconButton(
                                    label = "نسخ النص",
                                    icon = Icons.Outlined.ContentCopy,
                                    color = Brand600,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Takaful Case Link", shareText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "تم نسخ تفاصيل الحملة!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ShareIconButton(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

