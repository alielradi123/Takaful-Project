package com.example.takaful.ui.screens.donation

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takaful.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * عملية الدفع الحقيقية عبر التطبيقات البنكية السودانية
 * يعرض معلومات الحساب الحقيقي ويوجه المستخدم لإتمام التحويل عبر تطبيقه البنكي
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankPaymentFlow(
    amount: Double,
    paymentMethod: String,
    caseTitle: String,
    selectedBank: BankAccount = getDynamicBankAccounts(null)[0],
    onDismiss: () -> Unit,
    onSuccess: (transactionId: String, bankName: String, accountNumber: String, receiptUri: Uri?) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        receiptUri = uri
    }

    // Bank Branding Colors
    val primaryColor = when {
        paymentMethod.contains("mBok") || paymentMethod.contains("بنكك") -> Color(0xFF005C3E)
        paymentMethod.contains("فوري") || paymentMethod.contains("Fawry") -> Color(0xFF1E824C)
        paymentMethod.contains("صح") || paymentMethod.contains("Sah") -> Color(0xFF1A237E)
        paymentMethod.contains("أوكاش") || paymentMethod.contains("Ocash") -> Color(0xFFE65100)
        else -> Color(0xFF263238)
    }

    val secondaryColor = when {
        paymentMethod.contains("mBok") || paymentMethod.contains("بنكك") -> Color(0xFFD4AF37)
        paymentMethod.contains("فوري") || paymentMethod.contains("Fawry") -> Color(0xFFFFD54F)
        paymentMethod.contains("صح") || paymentMethod.contains("Sah") -> Color(0xFF42A5F5)
        paymentMethod.contains("أوكاش") || paymentMethod.contains("Ocash") -> Color(0xFFFF9800)
        else -> Brand600
    }

    val bankLogoText = when {
        paymentMethod.contains("mBok") || paymentMethod.contains("بنكك") -> "mBok - بنك الخرطوم"
        paymentMethod.contains("فوري") || paymentMethod.contains("Fawry") -> "Fawry - بنك فيصل الإسلامي"
        paymentMethod.contains("صح") || paymentMethod.contains("Sah") -> "صح - Sah"
        paymentMethod.contains("أوكاش") || paymentMethod.contains("Ocash") -> "أوكاش - Ocash"
        else -> "بوابة الدفع — تكافل"
    }

    val appInstructions = when {
        paymentMethod.contains("mBok") || paymentMethod.contains("بنكك") -> listOf(
            "افتح تطبيق mBok (بنكك) على هاتفك",
            "اختر 'تحويلات' من القائمة الرئيسية",
            "أدخل رقم الحساب: ${selectedBank.accountNumber}",
            "أدخل المبلغ: ${DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(amount)} ج.س",
            "تأكد من البيانات واضغط 'تأكيد التحويل'",
            "أدخل رمز OTP المرسل لهاتفك",
            "احفظ رقم المعاملة كإيصال"
        )
        paymentMethod.contains("صح") || paymentMethod.contains("Sah") -> listOf(
            "افتح تطبيق صح (Sah) على هاتفك",
            "اختر 'تحويل' من القائمة",
            "أدخل رقم حساب المستفيد: ${selectedBank.accountNumber}",
            "أدخل المبلغ: ${DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(amount)} ج.س",
            "راجع بيانات التحويل واضغط 'إرسال'",
            "أكد العملية عبر البصمة أو رمز PIN",
            "احتفظ بإيصال العملية"
        )
        paymentMethod.contains("فوري") || paymentMethod.contains("Fawry") -> listOf(
            "افتح تطبيق فوري على هاتفك",
            "اختر 'تحويل لحساب' من القائمة",
            "أدخل رقم الحساب: ${selectedBank.accountNumber}",
            "حدد المبلغ: ${DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(amount)} ج.س",
            "أكد العملية بالرقم السري",
            "احتفظ برقم الإيصال"
        )
        paymentMethod.contains("أوكاش") || paymentMethod.contains("Ocash") -> listOf(
            "افتح تطبيق أوكاش (Ocash) على هاتفك",
            "اختر 'تحويل أموال'",
            "أدخل رقم الحساب: ${selectedBank.accountNumber}",
            "أدخل المبلغ: ${DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(amount)} ج.س",
            "أكد عبر رمز PIN الخاص بك",
            "احتفظ بإيصال المعاملة"
        )
        else -> listOf(
            "افتح تطبيقك البنكي",
            "اختر 'تحويل' أو 'تحويلات'",
            "أدخل رقم الحساب: ${selectedBank.accountNumber}",
            "أدخل المبلغ وأكد العملية"
        )
    }

    // State
    var currentStep by remember { mutableIntStateOf(1) } // 1: Instructions, 2: Confirm, 3: Success
    var transactionRef by remember { mutableStateOf("") }
    val txId = remember { "TKF-${System.currentTimeMillis().toString().takeLast(6)}" }
    val formattedAmount = remember { DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(amount) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(bankLogoText, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, "رجوع", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor)
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(0.03f), Color(0xFFF8FAFC))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // --- Header: Info Summary ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("تفاصيل التبرع", fontSize = 12.sp, color = Color.Gray)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(caseTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                                Text("$formattedAmount ج.س", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = primaryColor)
                            }
                            HorizontalDivider(color = Neutral200)
                            // Account info with copy
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("رقم الحساب (BBAN)", fontSize = 11.sp, color = Neutral500)
                                    Text(
                                        selectedBank.accountNumber,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = primaryColor,
                                        letterSpacing = 1.sp
                                    )
                                    Text("${selectedBank.bankName} — ${selectedBank.accountHolder}", fontSize = 11.sp, color = Neutral500)
                                }
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(selectedBank.accountNumber))
                                    Toast.makeText(context, "تم نسخ رقم الحساب", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Outlined.ContentCopy, "نسخ", tint = primaryColor)
                                }
                            }
                        }
                    }

                    // --- Steps Content ---
                    when (currentStep) {
                        1 -> { // Instructions
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Instructions Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Column(Modifier.padding(18.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(primaryColor.copy(0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.AutoMirrored.Outlined.ListAlt, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Text("خطوات إتمام التحويل", fontWeight = FontWeight.Bold, color = Neutral900)
                                        }
                                        Spacer(Modifier.height(16.dp))

                                        appInstructions.forEachIndexed { index, instruction ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .background(
                                                            if (index < 3) primaryColor else primaryColor.copy(0.6f),
                                                            CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "${index + 1}",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Text(
                                                    instruction,
                                                    fontSize = 14.sp,
                                                    color = Neutral700,
                                                    lineHeight = 20.sp,
                                                    modifier = Modifier.padding(top = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Security notice
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Brand50, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Outlined.VerifiedUser, null, tint = Brand600, modifier = Modifier.size(18.dp))
                                    Text(
                                        "تبرعك آمن ومحمي. رقم الحساب حقيقي وموثق لمنظمة تكافل الإنسانية.",
                                        fontSize = 12.sp,
                                        color = Brand700
                                    )
                                }

                                // Action buttons
                                Button(
                                    onClick = { currentStep = 2 },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("لقد أتممت التحويل — التالي", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                }

                                // Try opening the banking app
                                OutlinedButton(
                                    onClick = {
                                        // Try to open the banking app
                                        val packageName = when {
                                            paymentMethod.contains("mBok") || paymentMethod.contains("بنكك") -> "com.bok.mbok"
                                            paymentMethod.contains("صح") || paymentMethod.contains("Sah") -> "com.sah.app"
                                            paymentMethod.contains("فوري") || paymentMethod.contains("Fawry") -> "com.fib.fawry"
                                            paymentMethod.contains("أوكاش") || paymentMethod.contains("Ocash") -> "com.ocash.app"
                                            else -> null
                                        }
                                        if (packageName != null) {
                                            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                                            if (intent != null) {
                                                context.startActivity(intent)
                                            } else {
                                                // App not installed — open Play Store
                                                try {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                                                } catch (_: Exception) {
                                                    Toast.makeText(context, "التطبيق غير مثبت. يرجى تحميله من المتجر.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, primaryColor)
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = primaryColor)
                                    Spacer(Modifier.width(8.dp))
                                    Text("فتح تطبيق $bankLogoText", color = primaryColor, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        2 -> { // Confirmation — enter transaction reference
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "هل أتممت التحويل بنجاح؟",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Neutral900,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "أدخل رقم المعاملة/الإيصال وأرفق صورة التحويل من تطبيقك البنكي للتوثيق (مطلوب)",
                                    fontSize = 14.sp,
                                    color = Neutral500,
                                    textAlign = TextAlign.Center
                                )

                                OutlinedTextField(
                                    value = transactionRef,
                                    onValueChange = { transactionRef = it },
                                    label = { Text("رقم المعاملة / الإيصال (مطلوب)") },
                                    placeholder = { Text("مثال: TXN-123456") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    leadingIcon = { Icon(Icons.Outlined.Receipt, null, tint = primaryColor) }
                                )

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

                                Button(
                                    onClick = {
                                        if (transactionRef.isBlank() || receiptUri == null) {
                                            Toast.makeText(context, "الرجاء إدخال الرقم المرجعي وإرفاق صورة الإيصال للاستمرار", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        currentStep = 3
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Brand600),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Outlined.CheckCircle, null, tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("نعم، تأكيد التبرع", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                }

                                TextButton(onClick = { currentStep = 1 }) {
                                    Text("رجوع للخطوات", color = primaryColor)
                                }
                            }
                        }

                        3 -> { // Success Screen with Receipt
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Animated Check Circle
                                Surface(
                                    color = Brand600.copy(0.1f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(90.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.CheckCircle,
                                            null,
                                            tint = Brand600,
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                }

                                Text(
                                    "شكراً لتبرعك الكريم!",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    color = Brand600
                                )

                                // Receipt Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(4.dp, RoundedCornerShape(24.dp)),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("إيصال تبرع تكافل", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 14.sp)
                                        HorizontalDivider(color = Color(0xFFEEEEEE))

                                        ReceiptRow("الحالة المستفيدة", caseTitle)
                                        ReceiptRow("المبلغ المدفوع", "$formattedAmount ج.س")
                                        ReceiptRow("وسيلة الدفع", paymentMethod)
                                        ReceiptRow("البنك", selectedBank.bankName)
                                        ReceiptRow("رقم الحساب", selectedBank.accountNumber)
                                        ReceiptRow("رقم العملية", transactionRef.ifBlank { txId })
                                        ReceiptRow("التاريخ والوقت", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))

                                        Spacer(Modifier.height(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Brand600.copy(0.05f), RoundedCornerShape(10.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                "شكراً لمساهمتك الكريمة. تبرعك في أمان تام وسيتم توزيعه بشفافية كاملة.",
                                                fontSize = 11.sp,
                                                color = Brand600,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        val ref = transactionRef.ifBlank { txId }
                                        onSuccess(ref, selectedBank.bankName, selectedBank.accountNumber, receiptUri)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("إكمال وحفظ التبرع", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 13.sp)
    }
}
