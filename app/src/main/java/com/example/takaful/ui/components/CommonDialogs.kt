package com.example.takaful.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takaful.ui.theme.*
import com.example.takaful.viewmodel.TakafulViewModel

@Composable
fun InteractiveDialog(
    showDialog: Boolean,
    title: String,
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    if (!showDialog) return
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                null,
                tint = if (isError) SemanticError else Brand600,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(title, fontWeight = FontWeight.ExtraBold, color = if (isError) SemanticError else Brand700,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = { Text(message, color = Neutral600, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = if (isError) SemanticError else Brand600),
                shape  = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("موافق", fontWeight = FontWeight.Bold) }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

@Composable
fun ForgotPasswordDialog(
    showDialog: Boolean,
    viewModel: TakafulViewModel,
    onDismiss: () -> Unit
) {
    if (!showDialog) return

    var email         by remember { mutableStateOf("") }
    var isLoading     by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var errorMessage  by remember { mutableStateOf("") }
    var isSuccess     by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            email = ""; isLoading = false; statusMessage = ""; errorMessage = ""; isSuccess = false
            onDismiss()
        },
        title = {
            Text("استعادة كلمة المرور", fontWeight = FontWeight.ExtraBold, color = Brand700,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("أدخل بريدك الإلكتروني لإرسال رابط إعادة تعيين كلمة المرور.",
                    fontSize = 14.sp, color = Neutral500, textAlign = TextAlign.Center)

                if (statusMessage.isNotEmpty()) {
                    Surface(color = Brand50, shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = Brand600, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(statusMessage, color = Brand700, fontSize = 13.sp)
                        }
                    }
                }
                if (errorMessage.isNotEmpty()) {
                    Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, null, tint = SemanticError, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(errorMessage, color = SemanticError, fontSize = 13.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني") },
                    leadingIcon = { Icon(Icons.Outlined.Email, null, tint = Brand600) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading && !isSuccess,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Brand600, unfocusedBorderColor = Neutral200,
                        focusedLabelColor = Brand600, cursorColor = Brand600
                    )
                )

                Button(
                    onClick = {
                        if (email.trim().contains("@")) {
                            isLoading = true; errorMessage = ""; statusMessage = ""
                            viewModel.sendPasswordResetEmail(email.trim(),
                                onSuccess = { isLoading = false; statusMessage = "تم إرسال رابط الاستعادة بنجاح."; isSuccess = true },
                                onFailure = { msg -> isLoading = false; errorMessage = msg }
                            )
                        } else errorMessage = "يرجى إدخال بريد إلكتروني صالح."
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand600),
                    enabled = !isLoading && !isSuccess
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("إرسال رابط الاستعادة", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                email = ""; isLoading = false; statusMessage = ""; errorMessage = ""; isSuccess = false
                onDismiss()
            }) { Text("إغلاق", color = Brand600, fontWeight = FontWeight.SemiBold) }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}
