package com.example.takaful.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
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
fun MaintenanceScreen(contactEmail: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Build,
            contentDescription = "Maintenance",
            tint = Brand600,
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "تطبيق تكافل في وضع الصيانة",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "نحن نقوم حالياً بتحديثات وتطويرات هامة على النظام لتقديم خدمة أفضل. نعتذر عن الإزعاج ونرجو المحاولة لاحقاً.",
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
    }
}
