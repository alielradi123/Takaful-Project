package com.example.takaful.ui.screens.donation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takaful.data.model.DonationRecord
import com.example.takaful.ui.theme.*
import com.example.takaful.viewmodel.TakafulViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationTrackingScreen(
    viewModel: TakafulViewModel,
    donationId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val donations by viewModel.donations.collectAsState()
    val donation  = donations.find { it.id == donationId }
    val userProfile by viewModel.userProfile.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("تتبع حالة التبرع", fontWeight = FontWeight.Bold) },
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
            }
        ) { paddingValues ->
            if (donation == null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.SearchOff, null, tint = Color.Gray.copy(0.3f), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("لم يتم العثور على التبرع", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    }
                }
            } else {
                if (showEditDialog) {
                    EditDonationDialog(
                        donation = donation,
                        onDismiss = { showEditDialog = false },
                        onSave = { updatedFields ->
                            showEditDialog = false
                            viewModel.updateDonation(
                                donationId = donation.id,
                                fields = updatedFields,
                                onSuccess = {
                                    Toast.makeText(context, "تم تعديل التبرع بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { e ->
                                    Toast.makeText(context, "خطأ أثناء التعديل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    )
                }

                if (showDeleteDialog) {
                    DeleteConfirmationDialog(
                        onDismiss = { showDeleteDialog = false },
                        onConfirm = {
                            showDeleteDialog = false
                            viewModel.deleteDonation(
                                donationId = donation.id,
                                onSuccess = {
                                    Toast.makeText(context, "تم إلغاء التبرع بنجاح", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                },
                                onFailure = { e ->
                                    Toast.makeText(context, "خطأ أثناء الإلغاء: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 18.dp),
                    contentPadding = PaddingValues(top = 18.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { DonationDetailCard(donation) }
                    item { TrackingStepsCard(donation) }

                    // Admin / Volunteer actions
                    val hasAdminAccess = userProfile.isAdmin
                    if (donation.status == "قيد الجمع") {
                        item {
                            AdminActionsCard(
                                donation = donation,
                                viewModel = viewModel,
                                hasAccess = hasAdminAccess
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun AdminActionsCard(
    donation: DonationRecord,
    viewModel: TakafulViewModel,
    hasAccess: Boolean
) {
    val context = LocalContext.current
    var isUpdating by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasAccess) Brand600.copy(0.03f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("لوحة تحكم حالة التبرع", fontWeight = FontWeight.Bold, color = Neutral900, fontSize = 14.sp)
            }

            if (!hasAccess) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Gray.copy(0.08f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        "هذه اللوحة خاصة بإدارة تكافل لتحديث مراحل وصول التبرع وتوزيعه.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            } else {
                if (isUpdating) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Brand600)
                }

                when (donation.status) {
                    "قيد الجمع" -> {
                        val canConfirmReceived = donation.paymentStatus == "verified" || donation.receiptUrl.isNotBlank()
                        Button(
                            onClick = {
                                if (!canConfirmReceived) {
                                    Toast.makeText(context, "لا يمكن تأكيد الاستلام بدون تحقق/إيصال", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                isUpdating = true
                                viewModel.confirmDonationReceived(
                                    donationId = donation.id,
                                    onSuccess = {
                                        isUpdating = false
                                        Toast.makeText(context, "تم تأكيد استلام التبرع بنجاح!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { e ->
                                        isUpdating = false
                                        Toast.makeText(context, "خطأ: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isUpdating
                        ) {
                            Icon(Icons.Outlined.CheckCircle, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (canConfirmReceived) "تأكيد الاستلام من المتبرع" else "بانتظار تحقق الدفع/الإيصال",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    "تم الاستلام" -> {
                        Button(
                            onClick = {
                                isUpdating = true
                                viewModel.confirmDonationDistributed(
                                    donationId = donation.id,
                                    caseId = donation.effectiveCaseId,
                                    amount = donation.amount,
                                    onSuccess = {
                                        isUpdating = false
                                        Toast.makeText(context, "تم تأكيد توزيع التبرع وتحديث تقدم الحالة!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { e ->
                                        isUpdating = false
                                        Toast.makeText(context, "خطأ: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Brand600),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isUpdating
                        ) {
                            Icon(Icons.Outlined.LocalActivity, null)
                            Spacer(Modifier.width(8.dp))
                            Text("تأكيد التوزيع على المستفيد", fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brand600.copy(0.1f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Verified, null, tint = Brand600)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "اكتملت جميع مراحل التبرع بنجاح وتم التوصيل للمستفيدين.",
                                    fontWeight = FontWeight.Bold,
                                    color = Brand600,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDonationDialog(
    donation: DonationRecord,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any?>) -> Unit
) {
    var amountText by remember { mutableStateOf(donation.amount.toString()) }
    var itemText by remember { mutableStateOf(donation.amountOrItem) }
    var paymentMethod by remember { mutableStateOf(donation.paymentMethod) }

    val paymentMethods = listOf(
        "بطاقة ائتمان (Visa/Mastercard)",
        "مدى (Mada)",
        "Apple Pay",
        "STC Pay"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل بيانات التبرع المعلق", fontWeight = FontWeight.Bold, color = Brand600) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (donation.category == "عيني") {
                    OutlinedTextField(
                        value = itemText,
                        onValueChange = { itemText = it },
                        label = { Text("اسم العنصر والكمية") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("المبلغ") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { 
                            val isSaudi = paymentMethod.contains("مدى") || paymentMethod.contains("Mada") ||
                                    paymentMethod.contains("Apple") || paymentMethod.contains("STC") ||
                                    paymentMethod.contains("ائتمان") || paymentMethod.contains("Visa") ||
                                    paymentMethod.contains("Mastercard")
                            Text(if (isSaudi) "ر.س" else "ج.س")
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("طريقة التحويل", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    paymentMethods.forEach { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { paymentMethod = method }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = paymentMethod.startsWith(method),
                                onClick = { paymentMethod = method },
                                colors = RadioButtonDefaults.colors(selectedColor = Brand600)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(method, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fields = mutableMapOf<String, Any?>()
                    if (donation.category == "عيني") {
                        fields["amountOrItem"] = itemText
                    } else {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        fields["amount"] = amt
                        val isSaudi = paymentMethod.contains("مدى") || paymentMethod.contains("Mada") ||
                                paymentMethod.contains("Apple") || paymentMethod.contains("STC") ||
                                paymentMethod.contains("ائتمان") || paymentMethod.contains("Visa") ||
                                paymentMethod.contains("Mastercard")
                        val cur = if (isSaudi) "ر.س" else "ج.س"
                        fields["amountOrItem"] = "${DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(amt)} $cur"
                        fields["paymentMethod"] = paymentMethod
                    }
                    onSave(fields)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Brand600)
            ) {
                Text("حفظ التعديلات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = Color.Gray) }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إلغاء التبرع", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
        text = {
            Text("هل أنت متأكد من رغبتك في إلغاء وتراجع هذا التبرع؟ سيتم حذف السجل المصرفي نهائياً من النظام ولا يمكن التراجع عن هذا الإجراء.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("نعم، إلغاء وحذف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("تراجع", color = Color.Gray) }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun DonationDetailCard(donation: DonationRecord) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(30.dp)),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Brand400, shape = CircleShape, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.VolunteerActivism, null, tint = Brand600, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        donation.caseTitle.ifBlank { "تبرع عام" },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "المساهمة: ${donation.amountFormatted}",
                        color = Brand600,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = TakafulLightGray, thickness = 1.dp)
            Spacer(Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoColumn("رقم العملية", "#TK-${donation.id.take(6).uppercase()}")
                InfoColumn("طريقة التحويل", donation.paymentMethod.ifBlank { "—" })
                InfoColumn("التاريخ", formatTs(donation.effectiveTimestamp))
            }

            if (donation.isRecurring) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Repeat, null, tint = Brand600, modifier = Modifier.size(14.dp))
                    Text(
                        "تبرع دوري — ${when(donation.recurringInterval){ "daily"->"يومي"; "weekly"->"أسبوعي"; "monthly"->"شهري"; else->"سنوي" }}",
                        fontSize = 12.sp,
                        color = Brand600,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
    }
}

@Composable
private fun TrackingStepsCard(donation: DonationRecord) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(30.dp)),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text("مراحل مسار التبرع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Brand600)
            Spacer(Modifier.height(20.dp))

            val s = donation.status
            val receivedTimeStr = donation.receivedAt?.let { formatTs(it) }
                ?: if (s == "تم الاستلام" || s == "تم التوزيع") "✓ مؤكد" else "في الانتظار"
            val distributedTimeStr = donation.distributedAt?.let { formatTs(it) }
                ?: if (s == "تم التوزيع") "✓ مكتمل" else "في الانتظار"

            TrackingStep(
                title       = "قيد الجمع والتحقق",
                description = "تم استلام معلومات المساهمة وهي الآن قيد المراجعة والجمع ضمن التكافل.",
                timestamp   = formatTs(donation.effectiveTimestamp),
                isDone      = true,
                isActive    = s == "قيد الجمع",
                isLast      = false
            )
            TrackingStep(
                title       = "تم الاستلام من الإدارة",
                description = "استلمت إدارة تكافل التبرع وجارٍ تجهيزه للتوزيع على المستفيدين.",
                timestamp   = receivedTimeStr,
                isDone      = s == "تم الاستلام" || s == "تم التوزيع",
                isActive    = s == "تم الاستلام",
                isLast      = false
            )
            TrackingStep(
                title       = "تم التوزيع على المستفيد",
                description = "وصل التبرع بنجاح للأسرة أو الطفل المستفيد وتم التوثيق المالي في النظام.",
                timestamp   = distributedTimeStr,
                isDone      = s == "تم التوزيع",
                isActive    = s == "تم التوزيع",
                isLast      = true
            )
        }
    }
}

@Composable
private fun TrackingStep(
    title: String,
    description: String,
    timestamp: String,
    isDone: Boolean,
    isActive: Boolean,
    isLast: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            val targetColor = when {
                isDone && !isActive -> Brand600
                isActive            -> Gold500
                else                -> Brand400
            }
            val circleColor by animateColorAsState(targetValue = targetColor, animationSpec = tween(500))
            
            Surface(color = circleColor, shape = CircleShape, modifier = Modifier.size(24.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (isDone && !isActive) {
                        Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    } else if (isActive) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                    }
                }
            }
            if (!isLast) {
                val lineWeight by animateFloatAsState(targetValue = if (isDone) 1f else 0f, animationSpec = tween(1000))
                Box(modifier = Modifier.width(3.dp).height(60.dp).background(Brand400)) {
                    Box(modifier = Modifier.width(3.dp).fillMaxHeight(lineWeight).background(Brand600))
                }
            }
        }

        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = if (isDone || isActive) Neutral900 else Neutral500,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                timestamp,
                fontSize = 11.sp,
                color = if (isDone) Brand600 else Neutral500,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                lineHeight = 18.sp
            )
        }
    }
}

private fun formatTs(ts: Long): String {
    if (ts == 0L) return "—"
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ts))
}

