package com.example.takaful.ui.screens.donation

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takaful.ui.theme.*
import kotlinx.coroutines.delay
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// ══════════════════════════════════════════════════════════════════════════
//  ثوابت وألوان الهوية لوسائل الدفع السعودية
// ══════════════════════════════════════════════════════════════════════════
val SaudiGreen = Color(0xFF00875A)
val MadaPrimary = Color(0xFF003F88)
val MadaSecondary = Color(0xFF009639)
val ApplePayBlack = Color(0xFF000000)
val STCPayPurple = Color(0xFF4F008C)
val STCPayPink = Color(0xFFFF007F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaudiPaymentFlow(
    amount: Double,
    paymentMethod: String,
    caseTitle: String,
    onDismiss: () -> Unit,
    onSuccess: (transactionId: String, method: String) -> Unit,
    onCancel: () -> Unit
) {
    var currentStep by remember { mutableStateOf("initial") } // "initial", "processing", "success", "cancelled"
    var transactionId by remember { mutableStateOf("") }
    var cancelledReason by remember { mutableStateOf("") }

    val formattedAmount = remember { DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(amount) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            when (currentStep) {
                "initial" -> {
                    when {
                        paymentMethod.contains("ائتمان") || paymentMethod.contains("Mada") || paymentMethod.contains("مدى") -> {
                            CardPaymentSheet(
                                amount = amount,
                                isMada = paymentMethod.contains("Mada") || paymentMethod.contains("مدى"),
                                caseTitle = caseTitle,
                                onCancel = {
                                    cancelledReason = "تم إلغاء عملية إدخال بيانات البطاقة"
                                    currentStep = "cancelled"
                                },
                                onPay = {
                                    transactionId = "TXN-CC-${100000 + Random.nextInt(900000)}"
                                    currentStep = "processing"
                                }
                            )
                        }
                        paymentMethod.contains("Apple") -> {
                            ApplePayBottomSheet(
                                amount = amount,
                                caseTitle = caseTitle,
                                onCancel = {
                                    cancelledReason = "تم إلغاء الدفع عبر Apple Pay"
                                    currentStep = "cancelled"
                                },
                                onPay = {
                                    transactionId = "TXN-AP-${100000 + Random.nextInt(900000)}"
                                    currentStep = "processing"
                                }
                            )
                        }
                        paymentMethod.contains("STC") -> {
                            STCPaySheet(
                                amount = amount,
                                caseTitle = caseTitle,
                                onCancel = {
                                    cancelledReason = "تم إلغاء الدفع عبر STC Pay"
                                    currentStep = "cancelled"
                                },
                                onPay = {
                                    transactionId = "TXN-STC-${100000 + Random.nextInt(900000)}"
                                    currentStep = "processing"
                                }
                            )
                        }
                        else -> {
                            onDismiss()
                        }
                    }
                }

                "processing" -> {
                    LaunchedEffect(Unit) {
                        delay(2000)
                        currentStep = "success"
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Brand600,
                                modifier = Modifier.size(64.dp),
                                strokeWidth = 5.dp
                            )
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "جاري معالجة عملية الدفع بأمان...",
                                fontWeight = FontWeight.Bold,
                                color = Neutral900,
                                fontSize = 18.sp
                            )
                            Text(
                                "يرجى عدم إغلاق التطبيق أو الضغط على زر الرجوع",
                                fontSize = 13.sp,
                                color = Neutral500,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                "success" -> {
                    PaymentSuccessScreen(
                        amount = amount,
                        caseTitle = caseTitle,
                        paymentMethod = paymentMethod,
                        transactionId = transactionId,
                        onComplete = {
                            onSuccess(transactionId, paymentMethod)
                        }
                    )
                }

                "cancelled" -> {
                    LaunchedEffect(Unit) {
                        delay(2500)
                        onCancel()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Surface(
                                color = SemanticError.copy(0.12f),
                                shape = CircleShape,
                                modifier = Modifier.size(90.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Cancel,
                                        null,
                                        tint = SemanticError,
                                        modifier = Modifier.size(54.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "تم إلغاء عملية الدفع",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = SemanticError
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                cancelledReason,
                                fontSize = 15.sp,
                                color = Neutral600,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "سيتم إعادتك لتحديد المبلغ وطريقة التبرع...",
                                fontSize = 13.sp,
                                color = Neutral400,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
//  1. شاشة الدفع بالبطاقة الائتمانية ومدى
// ══════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardPaymentSheet(
    amount: Double,
    isMada: Boolean,
    caseTitle: String,
    onCancel: () -> Unit,
    onPay: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var cardNumber by remember { mutableStateOf("") }
    var cardName by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var isCvvFocused by remember { mutableStateOf(false) }

    val formattedAmount = remember { DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(amount) }

    // Card Rotation logic (for CVV focus card flip)
    val cardRotationY by animateFloatAsState(
        targetValue = if (isCvvFocused) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isMada) "دفع آمن عبر مدى" else "دفع آمن بالبطاقة الائتمانية", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "إلغاء")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isMada) MadaPrimary else Brand900,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Premium Animated Virtual Card ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .graphicsLayer {
                        rotationY = cardRotationY
                        cameraDistance = 12f * density
                    }
                    .shadow(12.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isMada) {
                            Brush.linearGradient(
                                colors = listOf(MadaPrimary, MadaPrimary.copy(alpha = 0.8f), MadaSecondary.copy(0.9f))
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF1E2640), Color(0xFF0F1424), Color(0xFF2E3A5F))
                            )
                        }
                    )
                    .padding(24.dp)
            ) {
                if (cardRotationY <= 90f || cardRotationY >= 270f) {
                    // CARD FRONT SIDE
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "تـكـافـل • TAKAFUL",
                                color = Color.White.copy(0.7f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (isMada) {
                                Text(
                                    "مدى / mada",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Visa", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                    Text("MC", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                }
                            }
                        }

                        // Card number formatted
                        val dispNum = cardNumber.padEnd(16, '•')
                            .chunked(4)
                            .joinToString("   ")
                        Text(
                            dispNum,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("صاحب البطاقة", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                Text(
                                    cardName.ifBlank { "NAME SURNAME" }.uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("تاريخ الانتهاء", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                Text(
                                    expiryDate.ifBlank { "MM/YY" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    // CARD BACK SIDE (flipped representation)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f },
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Magnetic stripe
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .background(Color.Black)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .background(Color.White.copy(0.7f))
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    "•••• •••• •••• ••••",
                                    color = Color.DarkGray,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(30.dp)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    cvv.ifBlank { "•••" },
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black
                                )
                            }
                        }
                        Text(
                            "التوقيع المعتمد - غير قابل للتحويل",
                            color = Color.White.copy(0.4f),
                            fontSize = 8.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // --- Form Inputs ---
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { input: String ->
                    val clean = input.filter { it.isDigit() }
                    if (clean.length <= 16) {
                        cardNumber = clean
                    }
                },
                label = { Text("رقم البطاقة (16 رقم)") },
                placeholder = { Text("4000 1234 5678 9010") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                leadingIcon = { Icon(Icons.Outlined.CreditCard, null, tint = Brand600) }
            )

            OutlinedTextField(
                value = cardName,
                onValueChange = { cardName = it },
                label = { Text("اسم صاحب البطاقة") },
                placeholder = { Text("Mohammed Al-Otaibi") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                leadingIcon = { Icon(Icons.Outlined.Person, null, tint = Brand600) }
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { input: String ->
                        val clean = input.filter { it.isDigit() }
                        expiryDate = when {
                            clean.length <= 2 -> clean
                            clean.length <= 4 -> "${clean.substring(0, 2)}/${clean.substring(2)}"
                            else -> expiryDate
                        }
                    },
                    label = { Text("تاريخ الانتهاء (MM/YY)") },
                    placeholder = { Text("12/28") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    leadingIcon = { Icon(Icons.Outlined.DateRange, null, tint = Brand600) }
                )

                OutlinedTextField(
                    value = cvv,
                    onValueChange = { input: String ->
                        val clean = input.filter { it.isDigit() }
                        if (clean.length <= 3) cvv = clean
                    },
                    label = { Text("الرمز السري (CVV)") },
                    placeholder = { Text("•••") },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isCvvFocused = it.isFocused },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = Brand600) }
                )
            }

            Spacer(Modifier.height(10.dp))

            // Info Notice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brand50, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.VerifiedUser, null, tint = SaudiGreen, modifier = Modifier.size(18.dp))
                Text(
                    "بوابة الدفع مشفرة بنسبة 100% ومتوافقة مع معايير PCI-DSS السعودية.",
                    fontSize = 11.sp,
                    color = Brand700
                )
            }

            Spacer(Modifier.weight(1f))

            // Action Buttons
            Button(
                onClick = {
                    if (cardNumber.length < 16) {
                        Toast.makeText(context, "الرجاء إدخال رقم بطاقة صحيح مكون من 16 خانة", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (cardName.isBlank()) {
                        Toast.makeText(context, "الرجاء إدخال اسم صاحب البطاقة", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (expiryDate.length < 5) {
                        Toast.makeText(context, "الرجاء إدخال تاريخ انتهاء صحيح (الشهر / السنة)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (cvv.length < 3) {
                        Toast.makeText(context, "الرجاء إدخال رمز التحقق CVV صحيح خلف البطاقة", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onPay()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isMada) MadaPrimary else Brand600)
            ) {
                Icon(Icons.Outlined.Lock, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("دفع آمن $formattedAmount ر.س", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }

            // Cancel Button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SemanticError)
            ) {
                Icon(Icons.Outlined.Cancel, null, tint = SemanticError)
                Spacer(Modifier.width(8.dp))
                Text("إلغاء العملية والتراجع", color = SemanticError, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
//  2. شاشة الدفع بالـ Apple Pay
// ══════════════════════════════════════════════════════════════════════════
@Composable
fun ApplePayBottomSheet(
    amount: Double,
    caseTitle: String,
    onCancel: () -> Unit,
    onPay: () -> Unit
) {
    var scanState by remember { mutableStateOf("waiting") } // "waiting", "scanning", "done"
    val formattedAmount = remember { DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(amount) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCancel() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {} // consume click
                .shadow(16.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Sheet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text("إلغاء", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text(" Pay", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Spacer(Modifier.width(50.dp)) // balancing
                }

                HorizontalDivider(color = Color.White.copy(0.12f))

                // Case and Amount rows
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المستفيد", color = Color.Gray, fontSize = 14.sp)
                        Text("منظمة تكافل الإنسانية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تبرع لـ", color = Color.Gray, fontSize = 14.sp)
                        Text(caseTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("البطاقة", color = Color.Gray, fontSize = 14.sp)
                        Text("Visa •••• 4242", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    HorizontalDivider(color = Color.White.copy(0.12f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المجموع", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("$formattedAmount ر.س", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Face ID scanning circle simulation
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.06f))
                        .clickable {
                            if (scanState == "waiting") {
                                scanState = "scanning"
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when (scanState) {
                        "waiting" -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.Face,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("اضغط للمسح", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                        "scanning" -> {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(72.dp))
                            Icon(
                                Icons.Outlined.Face,
                                null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            LaunchedEffect(Unit) {
                                delay(1500)
                                scanState = "done"
                                delay(1000)
                                onPay()
                            }
                        }
                        "done" -> {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                null,
                                tint = SaudiGreen,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }

                Text(
                    text = when (scanState) {
                        "waiting" -> "انقر للمصادقة عبر Face ID وإتمام الدفع"
                        "scanning" -> "جاري التحقق عبر معرف الوجه Face ID..."
                        else -> "تم التحقق بنجاح ✓"
                    },
                    color = if (scanState == "done") SaudiGreen else Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                // Additional Cancel Button for strict android cancellation compliance
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SemanticError.copy(alpha = 0.7f))
                ) {
                    Text("إلغاء عملية الدفع بالكامل", color = SemanticError, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
//  3. شاشة الدفع بالـ STC Pay
// ══════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun STCPaySheet(
    amount: Double,
    caseTitle: String,
    onCancel: () -> Unit,
    onPay: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf("phone") } // "phone" | "otp"
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    val formattedAmount = remember { DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(amount) }

    // Simulated SMS Banner
    var showSmsBanner by remember { mutableStateOf(false) }

    LaunchedEffect(step) {
        if (step == "otp") {
            delay(1500)
            showSmsBanner = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بوابة الدفع STC Pay", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = STCPayPurple,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // STC Pay mock branding header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = STCPayPurple.copy(0.04f))
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "stc pay",
                            color = STCPayPurple,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            "المستفيد: منظمة تكافل الخيرية",
                            fontSize = 12.sp,
                            color = Neutral500,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            "قيمة التبرع: $formattedAmount ر.س",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = STCPayPink,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                if (step == "phone") {
                    // Step 1: Input Phone Number
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "أدخل رقم الجوال المسجل في STC Pay",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Neutral900
                        )

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { input: String ->
                                val clean = input.filter { it.isDigit() }
                                if (clean.length <= 10) phoneNumber = clean
                            },
                            label = { Text("رقم الجوال") },
                            placeholder = { Text("05xxxxxxxx") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = { Icon(Icons.Default.Phone, null, tint = STCPayPurple) }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (phoneNumber.length < 10 || !phoneNumber.startsWith("05")) {
                                Toast.makeText(context, "الرجاء إدخال رقم جوال سعودي صحيح (مثال: 05xxxxxxxx)", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            step = "otp"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = STCPayPurple)
                    ) {
                        Text("إرسال رمز التحقق", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                } else {
                    // Step 2: Input OTP
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "أدخل رمز التحقق (OTP) المرسل إلى ${phoneNumber.take(4)}•••${phoneNumber.takeLast(3)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Neutral900,
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { input: String ->
                                val clean = input.filter { it.isDigit() }
                                if (clean.length <= 4) otpCode = clean
                            },
                            label = { Text("رمز التحقق مكون من 4 أرقام") },
                            placeholder = { Text("••••") },
                            modifier = Modifier.fillMaxWidth(0.6f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 8.sp,
                                textAlign = TextAlign.Center
                            )
                        )

                        TextButton(onClick = {
                            otpCode = ""
                            showSmsBanner = false
                            Toast.makeText(context, "تم إعادة إرسال رمز التحقق", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("إعادة إرسال الرمز", color = STCPayPurple, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (otpCode != "7392") {
                                Toast.makeText(context, "رمز التحقق غير صحيح، يرجى إدخال 7392 كما هو وارد بالإشعار", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            onPay()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = STCPayPurple)
                    ) {
                        Text("تأكيد ودفع $formattedAmount ر.س", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }

                    TextButton(onClick = { step = "phone"; otpCode = "" }) {
                        Text("تعديل رقم الجوال", color = Neutral500)
                    }
                }

                // Global Cancel Button (إلغاء العملية)
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SemanticError)
                ) {
                    Icon(Icons.Outlined.Cancel, null, tint = SemanticError)
                    Spacer(Modifier.width(8.dp))
                    Text("إلغاء العملية والتراجع", color = SemanticError, fontWeight = FontWeight.SemiBold)
                }
            }

            // Animated SMS Notification Banner Mockup
            AnimatedVisibility(
                visible = showSmsBanner,
                enter = slideInVertically(initialOffsetY = { height -> -height }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { height -> -height }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3F9)),
                    border = BorderStroke(1.dp, STCPayPurple.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(STCPayPurple, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Sms, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("stc pay", fontWeight = FontWeight.Bold, color = STCPayPurple, fontSize = 14.sp)
                            Text(
                                "رمز التحقق المؤقت لتبرع منظمة تكافل هو ( 7392 ). لا تشاركه مع أحد.",
                                color = Neutral700,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                        IconButton(onClick = { showSmsBanner = false }) {
                            Icon(Icons.Outlined.Clear, "إغلاق", tint = Neutral500)
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
//  4. شاشة نجاح الدفع وإصدار الفاتورة الرقمية
// ══════════════════════════════════════════════════════════════════════════
@Composable
fun PaymentSuccessScreen(
    amount: Double,
    caseTitle: String,
    paymentMethod: String,
    transactionId: String,
    onComplete: () -> Unit
) {
    val formattedAmount = remember { DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(amount) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Brand900.copy(0.04f), Color(0xFFF8FAFC))
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // Success animation circle
            Surface(
                color = SaudiGreen.copy(0.1f),
                shape = CircleShape,
                modifier = Modifier.size(90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        null,
                        tint = SaudiGreen,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Text(
                "شكراً لتبرعك الكريم!",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = SaudiGreen
            )

            Text(
                "تمت عملية الدفع بنجاح واقتطاع المبلغ لمصلحة الحالة الإنسانية.",
                fontSize = 14.sp,
                color = Neutral600,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            // Premium Digital Receipt Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("إيصال دفع تكافل الرقمي", fontWeight = FontWeight.Bold, color = Brand900, fontSize = 14.sp)
                    HorizontalDivider(color = Neutral200)

                    SuccessReceiptRow("الحالة المستفيدة", caseTitle)
                    SuccessReceiptRow("المبلغ المقتطع", "$formattedAmount ر.س")
                    SuccessReceiptRow("طريقة الدفع", paymentMethod)
                    SuccessReceiptRow("رقم العملية", transactionId)
                    SuccessReceiptRow("التاريخ والوقت", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))

                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SaudiGreen.copy(0.06f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            "تم إرسال نسخة من الفاتورة إلى جوالك وبريدك الإلكتروني. شكراً لعطائك.",
                            fontSize = 12.sp,
                            color = SaudiGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand600),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("إكمال وحفظ التبرع", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun SuccessReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Bold, color = Neutral900, fontSize = 13.sp)
    }
}
