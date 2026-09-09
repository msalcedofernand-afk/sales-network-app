package com.salesnetwork.avon.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionIntro(kicker: String, title: String, description: String) {
    val compact = LocalConfiguration.current.screenHeightDp < 500
    Surface(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.background(Color(0xFF123D49)).padding(if (compact) 12.dp else 20.dp)) {
            if (!compact) Text(kicker.uppercase(), color = Color(0xFFB8EAD4), fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(title, color = Color.White, fontSize = if (compact) 18.sp else 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (!compact) Text(description, color = Color(0xFFD5E7E5), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
@Composable
fun EmptyPanel(title: String, description: String) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
