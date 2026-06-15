package com.iranconnection.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iranconnection.app.ui.theme.AppColors

@Composable
fun ProfileScreen() {
    Column(
        Modifier.fillMaxSize().background(AppColors.ScreenBg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AppColors.Teal),
            contentAlignment = Alignment.Center,
        ) {
            Text("IC", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
        Text("IranConnection", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
        Text("Version 1.0", fontSize = 13.sp, color = AppColors.TextMuted)
    }
}
