package com.example.takaful.ui.screens.donation

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takaful.ui.theme.*
import com.example.takaful.viewmodel.TakafulViewModel
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationFormScreen(
    viewModel: TakafulViewModel,
    caseId: String?,           // String Firestore ID
    caseTitle: String,
    category: String,
    onNavigateBack: () -> Unit,
    onDonationSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val sysSettings by viewModel.sysSettings.collectAsState()
    val dynamicBankAccounts = remember(sysSettings) { getDynamicBankAccounts(sysSettings) }

    val approvedCases by viewModel.cases.collectAsState()
    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedCaseId by remember { mutableStateOf(caseId ?: "") }
    var selectedCaseTitle by remember { mutableStateOf(caseTitle) }

    var donationType    by remember { mutableStateOf(category) }
    var amountText      by remember { mutableStateOf("") }
    var amountOrItem    by remember { mutableStateOf("") }     // for عيني
    var selectedPayment by remember { mutableStateOf("") }
    var selectedBankIndex by remember { mutableIntStateOf(0) }
    var isRecurring     by remember { mutableStateOf(false) }
    var recurringInterval by remember { mutableStateOf("monthly") }
    var isLoading       by remember { mutableStateOf(false) }

    // Payment flow states
    var showCardPayment by remember { mutableStateOf(false) }
    var showBankPayment by remember { mutableStateOf(false) }

    var donorMessage    by remember { mutableStateOf("") }
    var receiptUrl      by remember { mutableStateOf("") }
    var receiptUri      by remember { mutableStateOf<Uri?>(null) }
    
    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        receiptUri = uri
    }

    // طرق الدفع المتاحة حالياً
    val paymentMethods = remember(donationType) {
        if (donationType == "مالي") {
            listOf("تحويل مباشر — بنك الخرطوم (mBok)", "تحويل مباشر — بنك النيل (صح)", "بطاقة بنكية — بنك الخرطوم")
        } else {
            emptyList()
        }
    }


    val recurringOptions = listOf(
        "daily" to "يومي",
        "weekly" to "أسبوعي",
        "monthly" to "شهري"
    )

    // Smart Amount Suggestion
    LaunchedEffect(Unit) {
        if (donationType == "مالي" && amountText.isBlank()) {
            val suggested = viewModel.getSuggestedDonationAmount()
            amountText = suggested.toInt().toString()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        when {
            showCardPayment -> {
                val amount = amountText.toDoubleOrNull() ?: 0.0
                CardPaymentScreen(
                    amount = amount,
                    caseTitle = selectedCaseTitle.ifBlank { "تبرع عام" },
                    onDismiss = { showCardPayment = false },
                    onPaymentSuccess = { txnId ->
                        showCardPayment = false
                        isLoading = true
                        viewModel.addDonation(
                            caseId            = selectedCaseId,
                            caseTitle         = selectedCaseTitle,
                            amount            = amount,
                            category          = donationType,
                            paymentMethod     = "بطاقة بنكية — بنك الخرطوم",
                            isRecurring       = isRecurring && donationType == "مالي",
                            recurringInterval = recurringInterval,
                            receiptUrl        = "",
                            donorMessage      = donorMessage,
                            bbanAccountNumber = "",
                            qrTransactionRef  = "",
                            bankName          = "بنك الخرطوم",
                            paymentRef        = txnId,
                            paymentGatewayName = "AutoDebitAPI",
                            onSuccess         = { donationId ->
                                isLoading = false
                                onDonationSuccess(donationId)
                            },
                            onFailure         = { e ->
                                isLoading = false
                                Toast.makeText(context, "خطأ: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                )
            }

            showBankPayment -> {
                val amount = amountText.toDoubleOrNull() ?: 0.0
                BankPaymentFlow(
                    amount = amount,
                    paymentMethod = selectedPayment,
                    caseTitle = selectedCaseTitle.ifBlank { "تبرع عام" },
                    selectedBank = if (selectedPayment.contains("صح")) dynamicBankAccounts[1] else dynamicBankAccounts[0],
                    onDismiss = { showBankPayment = false },
                    onSuccess = { transactionId, bankName, accountNumber, returnedReceiptUri ->
                        showBankPayment = false
                        isLoading = true
                        viewModel.uploadReceiptAndAddDonation(
                            context           = context,
                            receiptUri        = returnedReceiptUri,
                            caseId            = selectedCaseId,
                            caseTitle         = selectedCaseTitle,
                            amount            = amount,
                            category          = donationType,
                            paymentMethod     = "$selectedPayment ($transactionId)",
                            isRecurring       = isRecurring && donationType == "مالي",
                            recurringInterval = recurringInterval,
                            donorMessage      = donorMessage,
                            bbanAccountNumber = accountNumber,
                            qrTransactionRef  = "",
                            bankName          = bankName,
                            paymentRef        = transactionId,
                            paymentGatewayName = "bban",
                            onSuccess         = { donationId ->
                                isLoading = false
                                onDonationSuccess(donationId)
                            },
                            onFailure         = { e ->
                                isLoading = false
                                Toast.makeText(context, "خطأ: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                )
            }

            else -> {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        title = { Text("تقديم تبرع", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "رجوع")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Brand600,
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
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // ─── Case info card or selector dropdown ───
                    if (caseId == null) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { dropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Neutral200),
                                colors = CardDefaults.cardColors(containerColor = Neutral100)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.VolunteerActivism, null, tint = Gold500)
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text("تبرع لصالح *", style = MaterialTheme.typography.bodySmall, color = Neutral500)
                                            Text(selectedCaseTitle.ifBlank { "تبرع عام" }, fontWeight = FontWeight.Bold, color = Neutral900)
                                        }
                                    }
                                    Icon(Icons.Outlined.ArrowDropDown, null, tint = Gold500)
                                }
                            }

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f).background(Neutral100)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("تبرع عام (مساهمة عامة في تكافل)", color = Neutral900) },
                                    onClick = {
                                        selectedCaseId = ""
                                        selectedCaseTitle = "تبرع عام"
                                        dropdownExpanded = false
                                    }
                                )
                                approvedCases.filter { it.isActive }.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.title, color = Neutral900) },
                                        onClick = {
                                            selectedCaseId = c.id
                                            selectedCaseTitle = c.title
                                            if (c.category.isNotBlank()) {
                                                donationType = c.category
                                            }
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Neutral200),
                            colors = CardDefaults.cardColors(containerColor = Neutral100)
                        ) {
                            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.background(Brand600.copy(alpha = 0.2f), CircleShape).padding(12.dp)) {
                                    Icon(Icons.Outlined.VolunteerActivism, null, tint = Brand600)
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(selectedCaseTitle, fontWeight = FontWeight.Bold, color = Neutral900, fontSize = 18.sp)
                                    Text("التصنيف: $donationType", style = MaterialTheme.typography.bodyMedium, color = Neutral500)
                                }
                            }
                        }
                    }

                    // ─── نوع التبرع ───
                    Text("نوع التبرع", fontWeight = FontWeight.Bold, color = Neutral900)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("مالي" to "مبلغ مالي", "عيني" to "تبرع عيني", "طبي" to "مساعدة طبية").forEach { (type, label) ->
                            val isSel = donationType == type
                            FilterChip(
                                selected = isSel,
                                onClick = { donationType = type },
                                label = { Text(label, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Brand600,
                                    selectedLabelColor = Color.White,
                                    containerColor = Neutral100,
                                    labelColor = Neutral500
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSel,
                                    borderColor = if(isSel) Brand600 else Neutral200
                                )
                            )
                        }
                    }

                    // ─── Amount / Item ───
                    if (donationType == "عيني") {
                        OutlinedTextField(
                            value = amountOrItem,
                            onValueChange = { amountOrItem = it },
                            label = { Text("اسم العنصر والكمية *", color = Neutral500) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            trailingIcon = { Icon(Icons.Outlined.Inventory, null, tint = Brand600) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Brand600,
                                unfocusedBorderColor = Neutral200,
                                focusedTextColor = Neutral900,
                                unfocusedTextColor = Neutral900
                            )
                        )
                    } else {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("المبلغ (ر.س) *", color = Neutral500) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            trailingIcon = { Icon(Icons.Outlined.MonetizationOn, null, tint = Brand600) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Brand600,
                                unfocusedBorderColor = Neutral200,
                                focusedTextColor = Neutral900,
                                unfocusedTextColor = Neutral900
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        )
                        // Quick amount chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("50", "100", "500", "1000").forEach { amt ->
                                AssistChip(
                                    onClick = { amountText = amt },
                                    label = { Text(amt, fontSize = 14.sp) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (amountText == amt) Brand600.copy(0.2f) else Neutral100,
                                        labelColor = if (amountText == amt) Brand600 else Neutral400
                                    ),
                                    border = AssistChipDefaults.assistChipBorder(
                                        enabled = true,
                                        borderColor = if (amountText == amt) Brand600 else Neutral200
                                    )
                                )
                            }
                        }
                    }

            // ─── طريقة الدفع ───
                    Text(
                        "اختر طريقة الدفع (غير متاحة حالياً)",
                        fontWeight = FontWeight.Bold,
                        color = Neutral900
                    )
                    if (paymentMethods.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Neutral100),
                            border = BorderStroke(1.dp, SemanticError.copy(alpha = 0.6f))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "تم إلغاء خيارات الدفع بالعملة السودانية عبر البنوك وكذلك بطاقات Visa/Mastercard و مدى و Apple Pay و STC Pay.",
                                    color = Neutral700,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                                Text(
                                    "يمكنك حالياً فقط إدخال تبرع عيني (إن كان متاحاً).",
                                    color = SemanticError,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            paymentMethods.forEach { method ->

                            val methodIcon = when {
                                method.contains("ائتمان") || method.contains("Visa") -> Icons.Outlined.CreditCard
                                method.contains("Mada") || method.contains("مدى") -> Icons.Outlined.CreditCard
                                method.contains("Apple") -> Icons.Outlined.PhoneAndroid
                                method.contains("STC") -> Icons.Outlined.Wallet
                                else -> Icons.Outlined.Payment
                            }
                            Card(
                                onClick = { selectedPayment = method },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedPayment == method) Brand600.copy(alpha = 0.15f) else Neutral100
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedPayment == method) Brand600 else Neutral200
                                )
                            ) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = selectedPayment == method,
                                        onClick = { selectedPayment = method },
                                        colors = RadioButtonDefaults.colors(selectedColor = Brand600, unselectedColor = Neutral500)
                                    )
                                    Icon(methodIcon, null, tint = if (selectedPayment == method) Brand600 else Neutral400, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(method, color = Neutral900, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } // Added missing closing brace for else block


                    // ─── تبرع دوري ───
                    if (donationType == "مالي") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if(isRecurring) Brand600 else Neutral200),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRecurring) Brand600.copy(0.1f) else Neutral100
                            )
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("تبرع دوري (أمر دفع دائم)", fontWeight = FontWeight.Bold, color = Neutral900)
                                    Text("يتم تكرار التبرع تلقائياً كل فترة", fontSize = 12.sp, color = Neutral500)
                                }
                                Switch(
                                    checked = isRecurring,
                                    onCheckedChange = { isRecurring = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Brand600,
                                        uncheckedThumbColor = Neutral500,
                                        uncheckedTrackColor = TakafulSoftWhite
                                    )
                                )
                            }
                            if (isRecurring) {
                                Row(
                                    Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    recurringOptions.forEach { (key, label) ->
                                        val isSel = recurringInterval == key
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { recurringInterval = key },
                                            label = { Text(label, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Brand600,
                                                selectedLabelColor = Color.White,
                                                containerColor = Neutral100,
                                                labelColor = Neutral500
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isSel,
                                                borderColor = if(isSel) Brand600 else Neutral200
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ─── زر التبرع ───
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            val item   = amountOrItem.trim()

                            if (donationType == "مالي") {
                                if (amount <= 0) {
                                    Toast.makeText(context, "يرجى إدخال مبلغ صحيح", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedPayment.isBlank()) {
                                    Toast.makeText(context, "يرجى اختيار طريقة الدفع", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                
                                when {
                                    selectedPayment.contains("تحويل مباشر") -> {
                                        showBankPayment = true
                                    }
                                    selectedPayment.contains("بطاقة") -> {
                                        showCardPayment = true
                                    }
                                }
                            } else {
                                // Non-monetary donation (عيني / طبي)
                                if (donationType == "عيني" && item.isBlank()) {
                                    Toast.makeText(context, "يرجى وصف التبرع العيني", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isLoading = true
                                viewModel.addDonation(
                                    caseId            = selectedCaseId,
                                    caseTitle         = selectedCaseTitle,
                                    amount            = 0.0,
                                    category          = donationType,
                                    paymentMethod     = if (donationType == "عيني") "تبرع عيني ($item)" else "مساعدة طبية",
                                    isRecurring       = false,
                                    recurringInterval = "none",
                                    donorMessage      = donorMessage,
                                    onSuccess         = { donationId ->
                                        isLoading = false
                                        onDonationSuccess(donationId)
                                    },
                                    onFailure         = { e ->
                                        isLoading = false
                                        Toast.makeText(context, "خطأ: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand600),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Outlined.VolunteerActivism, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("تأكيد التبرع الآن", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        }
                    }
                }
            }
        }
        }
    }
}
