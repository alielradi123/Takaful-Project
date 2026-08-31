package com.example.takaful.ui.screens.donation

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.takaful.ui.theme.*
import com.example.takaful.R
import androidx.compose.ui.res.painterResource
import com.example.takaful.data.model.SystemSettings
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.concurrent.Executors

/**
 * بيانات الحسابات البنكية الحقيقية لمنظمات تكافل
 */
data class BankAccount(
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String = "منظمة تكافل الإنسانية",
    val bankLogo: String = "",
    val primaryColor: Color,
    val secondaryColor: Color
)


fun getDynamicBankAccounts(sysSettings: SystemSettings?): List<BankAccount> {
    return listOf(
        BankAccount(
            bankName = sysSettings?.bankName?.takeIf { it.isNotBlank() } ?: "بنك الخرطوم (بنكك)",
            accountNumber = sysSettings?.bankAccount?.takeIf { it.isNotBlank() } ?: "04037418080001",
            accountHolder = sysSettings?.bankHolder?.takeIf { it.isNotBlank() } ?: "منظمة تكافل الإنسانية",
            primaryColor = Color(0xFF005C3E),
            secondaryColor = Color(0xFFD4AF37)
        ),
        BankAccount(
            bankName = "صح (Sah)",
            accountNumber = "07748513148999",
            primaryColor = Color(0xFF1A237E),
            secondaryColor = Color(0xFF42A5F5)
        )
    )
}

val TAKAFUL_BANK_ACCOUNTS = getDynamicBankAccounts(null)

/**
 * شاشة الدفع عبر QR Code — تدعم عرض رمز QR حقيقي + مسح QR بالكاميرا
 */
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRPaymentScreen(
    amount: Double,
    caseTitle: String,
    selectedBank: BankAccount = TAKAFUL_BANK_ACCOUNTS[0],
    onDismiss: () -> Unit,
    onPaymentConfirmed: (accountNumber: String, transactionRef: String, bankName: String, receiptUri: Uri?) -> Unit
) {
    val context = LocalContext.current
    val formattedAmount = remember { DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(amount) }

    var currentMode by remember { mutableStateOf("display") } // "display" | "scan"
    var scannedData by remember { mutableStateOf<String?>(null) }
    var transactionId by remember { mutableStateOf("") }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    
    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        receiptUri = uri
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            currentMode = "scan"
        } else {
            Toast.makeText(context, "يجب السماح بصلاحية الكاميرا لمسح رمز QR", Toast.LENGTH_LONG).show()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.QrCode2, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("الدفع عبر QR Code", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, "رجوع", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = selectedBank.primaryColor)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                selectedBank.primaryColor.copy(alpha = 0.05f),
                                Color(0xFFF8FAFC)
                            )
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ─── معلومات التبرع ───
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("التبرع لصالح", fontSize = 12.sp, color = Neutral500)
                                Text(caseTitle, fontWeight = FontWeight.Bold, color = Neutral900)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("المبلغ", fontSize = 12.sp, color = Neutral500)
                                Text(
                                    "$formattedAmount ج.س",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    color = selectedBank.primaryColor
                                )
                            }
                        }
                        HorizontalDivider(color = Neutral200)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(selectedBank.primaryColor.copy(0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.AccountBalance,
                                    null,
                                    tint = selectedBank.primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(selectedBank.bankName, fontWeight = FontWeight.Bold, color = Neutral900, fontSize = 14.sp)
                                Text("حساب: ${selectedBank.accountNumber}", fontSize = 12.sp, color = Neutral500)
                            }
                        }
                    }
                }

                // ─── أزرار التبديل ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = currentMode == "display",
                        onClick = { currentMode = "display" },
                        label = { Text("عرض رمز QR", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Outlined.QrCode, null, Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = selectedBank.primaryColor,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = Neutral100,
                            labelColor = Neutral600
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = currentMode == "display",
                            borderColor = Neutral200
                        )
                    )
                    FilterChip(
                        selected = currentMode == "scan",
                        onClick = {
                            if (hasCameraPermission) {
                                currentMode = "scan"
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        label = { Text("مسح رمز QR", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Outlined.CameraAlt, null, Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = selectedBank.primaryColor,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = Neutral100,
                            labelColor = Neutral600
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = currentMode == "scan",
                            borderColor = Neutral200
                        )
                    )
                }

                when (currentMode) {
                    "display" -> {
                        // ─── عرض رمز QR حقيقي ───
                        QRCodeDisplaySection(
                            accountNumber = selectedBank.accountNumber,
                            amount = amount,
                            bankName = selectedBank.bankName,
                            caseTitle = caseTitle,
                            primaryColor = selectedBank.primaryColor
                        )

                        // ─── تعليمات ───
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Gold50),
                            border = BorderStroke(1.dp, Gold400.copy(0.3f))
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Info, null, tint = Gold700)
                                    Spacer(Modifier.width(8.dp))
                                    Text("كيفية الدفع", fontWeight = FontWeight.Bold, color = Gold700)
                                }
                                Spacer(Modifier.height(12.dp))
                                val steps = listOf(
                                    "افتح تطبيق بنكك أو صح على هاتفك",
                                    "اختر \"دفع\" أو \"PAY\" من القائمة",
                                    "وجّه الكاميرا نحو رمز QR أعلاه",
                                    "تأكد من المبلغ واضغط \"تأكيد الدفع\"",
                                    "احتفظ برقم المعاملة كإيصال"
                                )
                                steps.forEachIndexed { index, step ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .background(Gold500, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${index + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(step, fontSize = 13.sp, color = Neutral700)
                                    }
                                }
                            }
                        }

                        // ─── إدخال الرقم المرجعي وإرفاق الإيصال ───
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = transactionId,
                            onValueChange = { transactionId = it },
                            label = { Text("الرقم المرجعي للعملية (Transaction ID)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { receiptPickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (receiptUri != null) Color(0xFF4CAF50) else Neutral200,
                                contentColor = if (receiptUri != null) Color.White else Neutral700
                            )
                        ) {
                            Icon(if (receiptUri != null) Icons.Outlined.CheckCircle else Icons.Outlined.UploadFile, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (receiptUri != null) "تم إرفاق الإيصال" else "إرفاق إيصال التحويل (مطلوب)",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(24.dp))
                        // ─── زر التأكيد بعد الدفع ───
                        Button(
                            onClick = {
                                if (transactionId.isBlank() || receiptUri == null) {
                                    Toast.makeText(context, "الرجاء إدخال الرقم المرجعي وإرفاق الإيصال", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onPaymentConfirmed(selectedBank.accountNumber, transactionId, selectedBank.bankName, receiptUri)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = selectedBank.primaryColor)
                        ) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("لقد أتممت التحويل", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        }
                    }

                    "scan" -> {
                        // ─── مسح QR حقيقي بالكاميرا ───
                        if (scannedData != null) {
                            // عرض نتيجة المسح
                            QRScanResultCard(
                                scannedData = scannedData!!,
                                primaryColor = selectedBank.primaryColor,
                                onConfirm = {
                                    val ref = "QRSCAN-${System.currentTimeMillis()}"
                                    onPaymentConfirmed(
                                        selectedBank.accountNumber,
                                        ref,
                                        selectedBank.bankName,
                                        null
                                    )
                                },
                                onRescan = { scannedData = null }
                            )
                        } else if (hasCameraPermission) {
                            QRScannerView(
                                onQRCodeScanned = { data ->
                                    scannedData = data
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * عرض رمز QR حقيقي مُولَّد من بيانات الحساب
 */
@Composable
fun QRCodeDisplaySection(
    accountNumber: String,
    amount: Double,
    bankName: String,
    caseTitle: String,
    primaryColor: Color
) {
    val formattedAmount = DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(amount)
    // بيانات QR تحتوي معلومات التحويل الحقيقية
    val qrData = buildString {
        append("TAKAFUL_PAY|")
        append("ACC:$accountNumber|")
        append("AMT:$amount|")
        append("BANK:$bankName|")
        append("DESC:$caseTitle|")
        append("CUR:SDG")
    }

    val qrBitmap = remember(qrData) { generateQRCode(qrData, 600) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "امسح هذا الرمز عبر تطبيق بنكك أو صح",
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                textAlign = TextAlign.Center
            )

            // QR Code Image
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "رمز QR للدفع",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.QrCode,
                        contentDescription = "رمز QR",
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        tint = Neutral300
                    )
                }
            }

            // Payment info below QR
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = primaryColor.copy(0.06f))
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("رقم الحساب:", fontSize = 12.sp, color = Neutral500)
                        Text(accountNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Neutral900)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المبلغ:", fontSize = 12.sp, color = Neutral500)
                        Text("$formattedAmount ج.س", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = primaryColor)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المستفيد:", fontSize = 12.sp, color = Neutral500)
                        Text("منظمة تكافل الإنسانية", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Neutral900)
                    }
                }
            }
        }
    }
}

/**
 * عرض الكاميرا لمسح رمز QR حقيقي باستخدام ML Kit
 */
@Composable
fun QRScannerView(
    onQRCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isProcessing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "وجّه الكاميرا نحو رمز QR",
            fontWeight = FontWeight.Bold,
            color = Neutral900,
            textAlign = TextAlign.Center
        )

        // Camera Preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        @OptIn(ExperimentalGetImage::class)
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                    if (!isProcessing) {
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            isProcessing = true
                                            val inputImage = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            val scanner = BarcodeScanning.getClient()
                                            scanner.process(inputImage)
                                                .addOnSuccessListener { barcodes ->
                                                    for (barcode in barcodes) {
                                                        if (barcode.valueType == Barcode.TYPE_TEXT ||
                                                            barcode.valueType == Barcode.TYPE_UNKNOWN
                                                        ) {
                                                            barcode.rawValue?.let { value ->
                                                                onQRCodeScanned(value)
                                                            }
                                                        }
                                                    }
                                                    isProcessing = false
                                                }
                                                .addOnFailureListener {
                                                    isProcessing = false
                                                }
                                        }
                                    }
                                    imageProxy.close()
                                }
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.Info, null, tint = Neutral400, modifier = Modifier.size(16.dp))
            Text(
                "يتم المسح تلقائياً عند التعرف على رمز QR",
                fontSize = 12.sp,
                color = Neutral500
            )
        }
    }
}

/**
 * عرض نتيجة مسح رمز QR
 */
@Composable
fun QRScanResultCard(
    scannedData: String,
    primaryColor: Color,
    onConfirm: () -> Unit,
    onRescan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success icon
            Surface(
                color = Brand500.copy(0.1f),
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = Brand500, modifier = Modifier.size(48.dp))
                }
            }

            Text(
                "تم مسح رمز QR بنجاح!",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = Brand600
            )

            // Parsed data
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Neutral100)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("بيانات الرمز:", fontWeight = FontWeight.Bold, color = Neutral700, fontSize = 13.sp)

                    // Parse TAKAFUL_PAY format
                    if (scannedData.startsWith("TAKAFUL_PAY")) {
                        val parts = scannedData.split("|").drop(1)
                        parts.forEach { part ->
                            val kv = part.split(":")
                            if (kv.size == 2) {
                                val label = when (kv[0]) {
                                    "ACC" -> "رقم الحساب"
                                    "AMT" -> "المبلغ"
                                    "BANK" -> "البنك"
                                    "DESC" -> "الوصف"
                                    "CUR" -> "العملة"
                                    else -> kv[0]
                                }
                                val value = when (kv[0]) {
                                    "CUR" -> if (kv[1] == "SDG") "جنيه سوداني" else kv[1]
                                    "AMT" -> "${DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(kv[1].toDoubleOrNull() ?: 0.0)} ج.س"
                                    else -> kv[1]
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("$label:", fontSize = 12.sp, color = Neutral500)
                                    Text(value, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Neutral900)
                                }
                            }
                        }
                    } else {
                        // Generic QR data
                        Text(scannedData, fontSize = 12.sp, color = Neutral700, maxLines = 5)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRescan,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Neutral300)
                ) {
                    Icon(Icons.Outlined.Refresh, null, tint = Neutral600)
                    Spacer(Modifier.width(6.dp))
                    Text("إعادة المسح", color = Neutral600)
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("تأكيد الدفع", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * توليد رمز QR حقيقي من نص — باستخدام خوارزمية بسيطة بدون مكتبة خارجية
 * يستخدم Android's built-in QR encoding
 */
fun generateQRCode(content: String, size: Int): Bitmap? {
    return try {
        val writer = com.google.zxing.MultiFormatWriter()
        val bitMatrix = writer.encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
