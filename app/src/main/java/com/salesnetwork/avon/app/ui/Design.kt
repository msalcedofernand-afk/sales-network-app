package com.salesnetwork.avon.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object S {
    // ── Spacing ──
    val XXS = 2.dp
    val XS = 4.dp
    val S = 8.dp
    val SM = 12.dp
    val M = 16.dp
    val ML = 20.dp
    val L = 24.dp
    val XL = 32.dp
    val XXL = 48.dp

    // ── Radius ──
    val RNone = 0.dp
    val RXS = 6.dp
    val RS = 10.dp
    val RM = 14.dp
    val RCard = 16.dp
    val RL = 20.dp
    val RHero = 24.dp
    val RPill = 100.dp

    // ── Elevation ──
    val ElevationNone = 0.dp
    val ElevationLow = 1.dp
    val ElevationMed = 2.dp

    // ── Icon Sizes ──
    val IconXS = 14.dp
    val IconS = 16.dp
    val IconM = 20.dp
    val IconL = 24.dp
    val IconXL = 32.dp
    val IconHero = 48.dp
    val IconEmpty = 64.dp

    // ── Typography Scale ──
    val TextCaption = 11.sp
    val TextSmall = 12.sp
    val TextBody = 14.sp
    val TextBodyBold = 14.sp
    val TextSubtitle = 16.sp
    val TextTitle = 20.sp
    val TextHeadline = 24.sp
    val TextDisplay = 32.sp
}

object C {
    // ── Brand ──
    val Petroleo900 = Color(0xFF123D49)
    val Petroleo700 = Color(0xFF165C59)
    val Petroleo600 = Color(0xFF21716C)
    val Menta200 = Color(0xFFD5EEE3)
    val Menta100 = Color(0xFFEAF6F0)
    val Papel50 = Color(0xFFF3F6F5)
    val Papel0 = Color.White
    val Tinta900 = Color(0xFF192D2C)
    val Tinta700 = Color(0xFF405552)
    val Tinta500 = Color(0xFF60736F)

    // ── Semantic ──
    val Success = Color(0xFF2E7D32)
    val SuccessLight = Color(0xFFE8F5E9)
    val Warning = Color(0xFFE65100)
    val WarningLight = Color(0xFFFFF3E0)
    val Error = Color(0xFFC62828)
    val ErrorLight = Color(0xFFFFEBEE)
    val Info = Color(0xFF1565C0)
    val InfoLight = Color(0xFFE3F2FD)

    // ── External ──
    val WhatsApp = Color(0xFF25D366)

    // ── Semantic from scheme ──
    @Composable
    fun successContainer() = SuccessLight
    @Composable
    fun onSuccessContainer() = Success
    @Composable
    fun errorContainer() = ErrorLight
    @Composable
    fun onErrorContainer() = Error
    @Composable
    fun warningContainer() = WarningLight
    @Composable
    fun onWarningContainer() = Warning
    @Composable
    fun infoContainer() = InfoLight
    @Composable
    fun onInfoContainer() = Info
}

object B {
    // ── Borders ──
    @Composable
    fun cardBorder() = BorderStroke(S.ElevationLow, MaterialTheme.colorScheme.outlineVariant)

    @Composable
    fun subtleBorder() = BorderStroke(S.ElevationLow, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

object SH {
    // ── Shapes ──
    val Card = RoundedCornerShape(S.RCard)
    val CardSmall = RoundedCornerShape(S.RM)
    val Chip = RoundedCornerShape(S.RS)
    val Badge = RoundedCornerShape(S.RXS)
    val Dialog = RoundedCornerShape(S.RHero)
    val BottomSheet = RoundedCornerShape(topStart = S.RHero, topEnd = S.RHero)
    val Button = RoundedCornerShape(S.RL)
    val ButtonSmall = RoundedCornerShape(S.RM)
    val Input = RoundedCornerShape(S.RM)
    val Avatar = CircleShape
    val Pill = RoundedCornerShape(S.RPill)
}

// ── Reusable Composables ──

@Composable
fun SectionIntro(kicker: String, title: String, description: String) {
    val compact = LocalConfiguration.current.screenHeightDp < 500
    Surface(shape = SH.Dialog, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .background(C.Petroleo900)
                .padding(if (compact) S.M else S.L)
        ) {
            if (!compact) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(S.XS)
                            .background(C.Menta200, CircleShape)
                    )
                    Spacer(Modifier.width(S.S))
                    Box(
                        Modifier
                            .width(28.dp)
                            .height(S.XXS)
                            .background(C.Petroleo700)
                    )
                    Spacer(Modifier.width(S.S))
                    Text(
                        kicker.uppercase(),
                        color = C.Menta200,
                        fontSize = S.TextCaption,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(S.SM))
            Text(
                title,
                color = Color.White,
                fontSize = if (compact) S.TextTitle else S.TextHeadline,
                lineHeight = if (compact) 24.sp else 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(S.S))
            if (!compact) Text(
                description,
                color = C.Menta200.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(S.XXL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(S.M)
    ) {
        Surface(
            modifier = Modifier.size(S.IconEmpty),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(S.IconXL),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(S.XS))
            Button(
                onClick = onAction,
                shape = SH.Button,
                contentPadding = PaddingValues(horizontal = S.L, vertical = S.SM)
            ) {
                Text(actionLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    color: Color,
    backgroundColor: Color
) {
    Surface(
        color = backgroundColor,
        shape = SH.Pill
    ) {
        Text(
            text = text,
            fontSize = S.TextCaption,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = S.SM, vertical = S.XS)
        )
    }
}

@Composable
fun KpiCard(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val cardMod = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Card(
        modifier = cardMod.fillMaxWidth(),
        shape = SH.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = B.cardBorder(),
        elevation = CardDefaults.cardElevation(S.ElevationNone)
    ) {
        Column(
            modifier = Modifier.padding(S.M),
            verticalArrangement = Arrangement.spacedBy(S.XS)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(S.S)
            ) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(S.IconS),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    label,
                    fontSize = S.TextCaption,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = S.TextTitle,
                color = valueColor
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (count != null) "$title ($count)" else title,
            fontSize = S.TextSubtitle,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun Modifier.clickable(onClick: () -> Unit): Modifier =
    this.clickable { onClick() }
