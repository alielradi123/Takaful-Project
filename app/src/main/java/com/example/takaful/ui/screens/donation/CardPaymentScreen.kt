package com.example.takaful.ui.screens.donation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.takaful.ui.theme.Brand600
import com.example.takaful.ui.theme.Neutral100
import com.example.takaful.ui.theme.Neutral200
import com.example.takaful.ui.theme.Neutral500
import com.example.takaful.ui.theme.Neutral900
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CardPaymentScreen(
    amount: Double,
    caseTitle: String,
    onDismiss: () -> Unit,
    onPaymentSuccess: (String) -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    
    var isProcessing by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    Dialog(
        onDismissRequest = { if (!isProcessing && !isSuccess) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Brand600)
                    Spacer(Modifier.height(16.dp))
                    Text("جاري معالجة الدفع...", fontWeight = FontWeight.Bold)
                } else if (isSuccess) {
                    Icon(
                        Icons.Outlined.CreditCard,
                        contentDescription = "Success",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("تم الدفع بنجاح!", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                } else {
                    Text(
                        "الدفع ببطاقة بنك الخرطوم",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Neutral900
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "مبلغ التبرع: $amount ج.س",
                        fontSize = 16.sp,
                        color = Brand600,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { if (it.length <= 16) cardNumber = it },
                        label = { Text("رقم البطاقة (16 رقم)") },
                        leadingIcon = { Icon(Icons.Outlined.CreditCard, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { if (it.length <= 5) expiryDate = it },
                            label = { Text("تاريخ الانتهاء (MM/YY)") },
                            leadingIcon = { Icon(Icons.Outlined.DateRange, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { if (it.length <= 3) cvv = it },
                            label = { Text("CVV") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = cardHolder,
                        onValueChange = { cardHolder = it },
                        label = { Text("اسم حامل البطاقة") },
                        leadingIcon = { Icon(Icons.Outlined.Person, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إلغاء", color = Neutral900)
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    val apiService = com.example.takaful.data.network.MockAutoDebitApiService()
                                    val result = apiService.processCardPayment(
                                        cardNumber = cardNumber,
                                        expiryDate = expiryDate,
                                        cvv = cvv,
                                        cardHolder = cardHolder,
                                        amount = amount,
                                        isRecurring = false // We handle recurring on viewModel side
                                    )
                                    isProcessing = false
                                    when (result) {
                                        is com.example.takaful.data.network.PaymentResult.Success -> {
                                            isSuccess = true
                                            delay(1000)
                                            onPaymentSuccess(result.transactionId)
                                        }
                                        is com.example.takaful.data.network.PaymentResult.Error -> {
                                            // Optional: Handle error UI state
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Brand600),
                            enabled = cardNumber.length == 16 && cvv.length == 3 && expiryDate.length == 5 && cardHolder.isNotBlank()
                        ) {
                            Text("دفع الآن")
                        }
                    }
                }
            }
        }
    }
}
