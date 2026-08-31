package com.example.takaful.ui.screens.volunteer

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takaful.data.model.CaseItem
import com.example.takaful.ui.theme.*
import com.example.takaful.viewmodel.TakafulViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCompletionScreen(
    caseId: String,
    viewModel: TakafulViewModel,
    onNavigateBack: () -> Unit,
    onCompletionSuccess: () -> Unit
) {
    val context = LocalContext.current
    val cases by viewModel.cases.collectAsState()
    val caseItem = remember(cases, caseId) { cases.find { it.id == caseId } }
    
    var reportText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    if (caseItem == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("لم يتم العثور على تفاصيل هذه المهمة.")
        }
        return
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("إنجاز المهمة الميدانية", fontWeight = FontWeight.Bold, color = Color.White)
                            Text(caseItem.title, fontSize = 11.sp, color = Color.White.copy(0.8f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "رجوع", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Brand600
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Case Brief Card ──────────────────────────────────────
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Brand100, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        when (caseItem.category) {
                                            "طبي" -> Icons.Outlined.MedicalServices
                                            "عيني" -> Icons.Outlined.Inventory
                                            else -> Icons.Outlined.MonetizationOn
                                        },
                                        contentDescription = null,
                                        tint = Brand600,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = caseItem.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = caseItem.location,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "وصف الحالة:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = caseItem.effectiveDescription,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // ── Completion Report Input ──────────────────────────────────
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.RateReview, null, tint = Brand600, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تقرير التسليم الميداني",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = reportText,
                                onValueChange = { reportText = it },
                                label = { Text("اكتب تفاصيل الإنجاز والتسليم...") },
                                placeholder = { Text("مثال: تم زيارة المستفيد وتسليمه المستلزمات الطبية والتأكد من سلامتها...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 5,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Brand600,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "الحد الأدنى 10 أحرف للتأكيد.",
                                fontSize = 10.sp,
                                color = if (reportText.trim().length >= 10) Brand600 else MaterialTheme.colorScheme.onSurface.copy(0.4f)
                            )
                        }
                    }

                    // ── Proof of Delivery (Image Attachment) ──────────────────────────
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.PhotoCamera, null, tint = Brand600, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إثبات التسليم (اختياري)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (selectedImageUri != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                ) {
                                    coil.compose.AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = "إثبات الإنجاز المختار",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { selectedImageUri = null },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(0.5f), CircleShape)
                                            .size(36.dp)
                                    ) {
                                        Icon(Icons.Outlined.Delete, "حذف الصورة", tint = Color.White)
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Brand50)
                                        .border(1.dp, Brand100, RoundedCornerShape(12.dp))
                                        .clickable { pickImageLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Outlined.AddPhotoAlternate, "إضافة صورة", tint = Brand600, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("أضف صورة الفاتورة أو التسليم الميداني", fontSize = 12.sp, color = Brand600, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }

                // ── Bottom Action Button ─────────────────────────────────────
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .shadow(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Button(
                        onClick = {
                            if (reportText.trim().length < 10) {
                                Toast.makeText(context, "الرجاء كتابة تقرير لا يقل عن 10 أحرف", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSubmitting = true
                            viewModel.submitVolunteerReport(
                                caseId = caseItem.id,
                                report = reportText.trim(),
                                imageUri = selectedImageUri,
                                context = context,
                                onSuccess = {
                                    isSubmitting = false
                                    Toast.makeText(context, "تم تسجيل الإنجاز وإغلاق الحالة بنجاح!", Toast.LENGTH_LONG).show()
                                    onCompletionSuccess()
                                },
                                onFailure = { e ->
                                    isSubmitting = false
                                    Toast.makeText(context, "حدث خطأ: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Brand600,
                            disabledContainerColor = Brand400
                        ),
                        enabled = reportText.trim().length >= 10 && !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تأكيد إنجاز المهمة وإغلاق الحالة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // ── Loading Dialog Overlay ──────────────────────────────────
                if (isSubmitting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = Brand600)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "جاري رفع التقرير الميداني وصور الإثبات...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
