package com.example.takaful.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takaful.ui.theme.Brand600

@Composable
fun AccountSuspendedScreen(contactEmail: String, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = "Suspended",
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "حسابك معلق",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "تم تعليق حسابك من قبل الإدارة. لم يعد بإمكانك استخدام التطبيق في الوقت الحالي.",
            fontSize = 16.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        if (contactEmail.isNotBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "للتواصل العاجل: $contactEmail",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF334155),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Brand600),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(text = "تسجيل الخروج", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
