package com.example.takaful.ui.screens.donation

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

/**
 * شاشة إدارة التبرعات الدورية — عرض/إيقاف/استئناف/إلغاء
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringDonationScreen(
    viewModel: TakafulViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val donations by viewModel.donations.collectAsState()
    val recurringDonations = donations.filter {
        it.isRecurring && it.recurringStatus in listOf("active", "paused")
    }

    var showCancelDialog by remember { mutableStateOf<DonationRecord?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.EventRepeat, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("التبرعات الدورية", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, "رجوع", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand600)
                )
            }
        ) { paddingValues ->
            if (recurringDonations.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            color = Brand100,
                            shape = CircleShape,
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.EventRepeat,
                                    null,
                                    tint = Brand600,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        Text(
                            "لا توجد تبرعات دورية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Neutral900
                        )
                        Text(
                            "يمكنك جدولة تبرعات دورية من شاشة التبرع\nليتم خصمها تلقائياً بشكل يومي أو أسبوعي أو شهري",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = Neutral500,
                            lineHeight = 22.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Summary card
                    item {
                        RecurringSummaryCard(recurringDonations)
                    }

                    // Active donations
                    val activeDonations = recurringDonations.filter { it.recurringStatus == "active" }
                    if (activeDonations.isNotEmpty()) {
                        item {
                            Text(
                                "تبرعات نشطة (${activeDonations.size})",
                                fontWeight = FontWeight.Bold,
                                color = Brand600,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(activeDonations, key = { it.id }) { donation ->
                            RecurringDonationCard(
                                donation = donation,
                                onPause = {
                                    viewModel.pauseRecurringDonation(
                                        donationId = donation.id,
                                        onSuccess = {
                                            Toast.makeText(context, "تم إيقاف التبرع الدوري مؤقتاً", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = {
                                            Toast.makeText(context, "حدث خطأ: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                onResume = null,
                                onCancel = { showCancelDialog = donation }
                            )
                        }
                    }

                    // Paused donations
                    val pausedDonations = recurringDonations.filter { it.recurringStatus == "paused" }
                    if (pausedDonations.isNotEmpty()) {
                        item {
                            Text(
                                "تبرعات متوقفة (${pausedDonations.size})",
                                fontWeight = FontWeight.Bold,
                                color = Gold700,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(pausedDonations, key = { it.id }) { donation ->
                            RecurringDonationCard(
                                donation = donation,
                                onPause = null,
                                onResume = {
                                    viewModel.resumeRecurringDonation(
                                        donationId = donation.id,
                                        recurringInterval = donation.recurringInterval,
                                        onSuccess = {
                                            Toast.makeText(context, "تم استئناف التبرع الدوري", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = {
                                            Toast.makeText(context, "حدث خطأ: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                onCancel = { showCancelDialog = donation }
                            )
                        }
                    }
                }
            }

            // Cancel confirmation dialog
            if (showCancelDialog != null) {
                AlertDialog(
                    onDismissRequest = { showCancelDialog = null },
                    icon = { Icon(Icons.Outlined.Warning, null, tint = SemanticError) },
                    title = { Text("إلغاء التبرع الدوري", fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            "هل أنت متأكد من إلغاء التبرع الدوري لـ \"${showCancelDialog!!.caseTitle}\"؟\n\nلن يتم خصم أي مبالغ مستقبلية.",
                            textAlign = TextAlign.Center
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val donation = showCancelDialog!!
                                viewModel.cancelRecurringDonation(
                                    donationId = donation.id,
                                    onSuccess = {
                                        showCancelDialog = null
                                        Toast.makeText(context, "تم إلغاء التبرع الدوري نهائياً", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = {
                                        showCancelDialog = null
                                        Toast.makeText(context, "حدث خطأ: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SemanticError)
                        ) {
                            Text("نعم، إلغاء نهائي")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCancelDialog = null }) {
                            Text("تراجع")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RecurringSummaryCard(donations: List<DonationRecord>) {
    val activeCount = donations.count { it.recurringStatus == "active" }
    val totalMonthly = donations
        .filter { it.recurringStatus == "active" }
        .sumOf { donation ->
            when (donation.recurringInterval) {
                "daily" -> donation.effectiveAmount * 30
                "weekly" -> donation.effectiveAmount * 4
                "monthly" -> donation.effectiveAmount
                else -> 0.0
            }
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Brand600, Brand800)
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isSaudi = donations.any { it.isSaudiPayment }
                val currency = if (isSaudi) "ر.س" else "ج.س"
                Column {
                    Text("إجمالي شهري تقديري", fontSize = 12.sp, color = Color.White.copy(0.7f))
                    Text(
                        "${DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(totalMonthly)} $currency",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$activeCount تبرع نشط",
                        fontSize = 13.sp,
                        color = Gold400,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Surface(
                    color = Color.White.copy(0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.AutoAwesome, null, tint = Gold400, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringDonationCard(
    donation: DonationRecord,
    onPause: (() -> Unit)?,
    onResume: (() -> Unit)?,
    onCancel: () -> Unit
) {
    val isActive = donation.recurringStatus == "active"
    val intervalLabel = when (donation.recurringInterval) {
        "daily" -> "يومياً"
        "weekly" -> "أسبوعياً"
        "monthly" -> "شهرياً"
        else -> donation.recurringInterval
    }
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val nextDateStr = if (donation.recurringNextDate > 0)
        dateFormat.format(Date(donation.recurringNextDate))
    else "—"

    val borderColor = if (isActive) Brand500 else Gold500

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Brand50 else Gold50
        ),
        border = BorderStroke(1.dp, borderColor.copy(0.3f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(borderColor.copy(0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.VolunteerActivism,
                            null,
                            tint = borderColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(donation.caseTitle.ifBlank { "تبرع عام" }, fontWeight = FontWeight.Bold, color = Neutral900, fontSize = 15.sp)
                        val cur = if (donation.isSaudiPayment) "ر.س" else "ج.س"
                        Text(
                            "${DecimalFormat("#,###", DecimalFormatSymbols(Locale.ENGLISH)).format(donation.effectiveAmount)} $cur — $intervalLabel",
                            fontSize = 13.sp,
                            color = Neutral600
                        )
                    }
                }

                // Status chip
                Surface(
                    color = if (isActive) Brand500.copy(0.15f) else Gold500.copy(0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (isActive) "نشط" else "متوقف",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Brand700 else Gold700
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("طريقة الدفع", fontSize = 11.sp, color = Neutral400)
                    Text(donation.paymentMethod, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Neutral700)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("الموعد القادم", fontSize = 11.sp, color = Neutral400)
                    Text(
                        if (isActive) nextDateStr else "متوقف",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) Brand600 else Gold700
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Neutral200)
            Spacer(Modifier.height(10.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onPause != null) {
                    OutlinedButton(
                        onClick = onPause,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Gold500),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold700)
                    ) {
                        Icon(Icons.Outlined.PauseCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("إيقاف مؤقت", fontSize = 12.sp)
                    }
                }
                if (onResume != null) {
                    Button(
                        onClick = onResume,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand600)
                    ) {
                        Icon(Icons.Outlined.PlayCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("استئناف", fontSize = 12.sp)
                    }
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SemanticError.copy(0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SemanticError)
                ) {
                    Icon(Icons.Outlined.Cancel, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("إلغاء", fontSize = 12.sp)
                }
            }
        }
    }
}
