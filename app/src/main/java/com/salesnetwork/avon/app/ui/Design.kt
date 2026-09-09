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

object SalesDesignTokens {
    val Petroleum900 = Color(0xFF123D49)
    val Petroleum700 = Color(0xFF165C59)
    val Mint200 = Color(0xFFD5EEE3)
    val Mint100 = Color(0xFFEAF6F0)
    val Paper = Color(0xFFF3F6F5)
    val Ink = Color(0xFF192D2C)
    val InkSecondary = Color(0xFF405552)

    val RadiusSmall = 10.dp
    val RadiusMedium = 14.dp
    val RadiusLarge = 20.dp
    val RadiusHero = 24.dp

    val SpaceSmall = 8.dp
    val SpaceMedium = 16.dp
    val SpaceLarge = 24.dp
}

@Composable
fun SectionIntro(kicker: String, title: String, description: String) {
    val compact = LocalConfiguration.current.screenHeightDp < 500
    Surface(shape = RoundedCornerShape(SalesDesignTokens.RadiusHero), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .background(SalesDesignTokens.Petroleum900)
                .padding(if (compact) 12.dp else 20.dp)
        ) {
            if (!compact) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(SalesDesignTokens.Mint200, RoundedCornerShape(50))
                    )
                    Spacer(Modifier.width(SalesDesignTokens.SpaceSmall))
                    Box(
                        Modifier
                            .width(28.dp)
                            .height(2.dp)
                            .background(SalesDesignTokens.Petroleum700)
                    )
                    Spacer(Modifier.width(SalesDesignTokens.SpaceSmall))
                    Text(
                        kicker.uppercase(),
                        color = SalesDesignTokens.Mint200,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(title, color = Color.White, fontSize = if (compact) 18.sp else 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (!compact) Text(description, color = Color(0xFFD5E7E5), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
@Composable
fun EmptyPanel(title: String, description: String) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(SalesDesignTokens.RadiusLarge)) {
        Column(
            Modifier.padding(SalesDesignTokens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(SalesDesignTokens.SpaceSmall)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
