package com.example.takaful.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takaful.R
import com.example.takaful.ui.theme.*

/** شعار تكافل المُستخدَم في شاشة تسجيل الدخول والتسجيل */
@Composable
fun LogoSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(Color.White, CircleShape)
                .border(2.dp, GoldBright, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter            = painterResource(id = R.drawable.takaful_logo),
                contentDescription = "تكافل",
                modifier           = Modifier.size(64.dp).clip(CircleShape),
                contentScale       = ContentScale.Fit
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text       = "تـكـافـل",
            fontSize   = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Color.White
        )
        Text(
            text     = "منصة العطاء الشفاف",
            fontSize = 13.sp,
            color    = Color.White.copy(0.75f)
        )
    }
}
