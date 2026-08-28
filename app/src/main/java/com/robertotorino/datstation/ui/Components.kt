package com.robertotorino.datstation.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robertotorino.datstation.R
import com.robertotorino.datstation.ui.theme.ChristmasGold
import com.robertotorino.datstation.ui.theme.ChristmasGreen
import com.robertotorino.datstation.ui.theme.ChristmasRed
import com.robertotorino.datstation.ui.theme.SnowWhite

data class AppThemeColors(
    val background: Color,
    val cardBackground: Color,
    val boxBackground: Color,
    val boxBorder: Color,
    val boxText: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
)

val BronzeTheme = AppThemeColors(
    background = Color(0xFF000814),
    cardBackground = Color(0xFF011641),
    boxBackground = Color(0xFF011641),
    boxBorder = Color(0xFF9F603D),
    boxText = Color(0xFF9F603D),
    accent = Color(0xFF4FC3F7),
    textPrimary = Color.White,
    textSecondary = Color.LightGray
)

val ClassicTheme = AppThemeColors(
    background = Color(0xFF0A0B0C),
    cardBackground = Color(0xFF191A1C),
    boxBackground = Color(0xFF191A1C),
    boxBorder = Color(0xFF0255BB),
    boxText = Color.White,
    accent = Color(0xFF4FC3F7),
    textPrimary = Color.White,
    textSecondary = Color.LightGray
)

val BlueTheme = AppThemeColors(
    background = Color(0xFF003791), // PS Blue
    cardBackground = Color(0xFF0072CE).copy(alpha = 0.8f),
    boxBackground = Color(0xFF0072CE), // PS Light Blue
    boxBorder = Color.White,
    boxText = Color.White,
    accent = Color(0xFF4FC3F7),
    textPrimary = Color.White,
    textSecondary = Color.White.copy(alpha = 0.7f)
)

val ChristmasTheme = AppThemeColors(
    background = Color(0xFF011601), // Very dark green
    cardBackground = ChristmasGreen,
    boxBackground = Color(0xFF0A361E),
    boxBorder = ChristmasRed,
    boxText = SnowWhite,
    accent = ChristmasGold,
    textPrimary = SnowWhite,
    textSecondary = Color.LightGray
)

val RedTheme = AppThemeColors(
    background = Color(0xFF2B0000),
    cardBackground = Color(0xFF4D0000),
    boxBackground = Color(0xFF4D0000),
    boxBorder = Color(0xFFFF4444),
    boxText = Color.White,
    accent = Color(0xFFFF8888),
    textPrimary = Color.White,
    textSecondary = Color.LightGray
)

val SilverTheme = AppThemeColors(
    background = Color(0xFF1A1C1E),
    cardBackground = Color(0xFF2C2F33),
    boxBackground = Color(0xFF2C2F33),
    boxBorder = Color(0xFFB0BEC5),
    boxText = Color.White,
    accent = Color(0xFFCFD8DC),
    textPrimary = Color.White,
    textSecondary = Color.LightGray
)

val LocalAppColors = staticCompositionLocalOf { ClassicTheme }

val AppCornerRadius = 12.dp

//@Composable
//fun SnowfallEffect() {
//    val snowflakes = remember { mutableStateListOf<Snowflake>() }
//
//    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
//        val width = constraints.maxWidth.toFloat()
//        val height = constraints.maxHeight.toFloat()
//
//        LaunchedEffect(key1 = Unit) {
//            while (true) {
//                withFrameNanos {
//                    if (snowflakes.size < 50 && Random.nextFloat() < 0.1f) {
//                        snowflakes.add(
//                            Snowflake(
//                                x = Random.nextFloat() * width,
//                                y = -10f,
//                                size = (Random.nextFloat() * 5f) + 2f,
//                                speed = Random.nextFloat() * 2f + 1f
//                            )
//                        )
//                    }
//                    val toRemove = mutableListOf<Snowflake>()
//                    snowflakes.forEach { snowflake ->
//                        snowflake.y += snowflake.speed
//                        if (snowflake.y > height) {
//                            toRemove.add(snowflake)
//                        }
//                    }
//                    snowflakes.removeAll(toRemove)
//                }
//            }
//        }
//
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            snowflakes.forEach { snowflake ->
//                drawCircle(
//                    color = Color.White.copy(alpha = 0.7f),
//                    radius = snowflake.size,
//                    center = Offset(snowflake.x, snowflake.y)
//                )
//            }
//        }
//    }
//}
//
//private class Snowflake(
//    var x: Float,
//    var y: Float,
//    val size: Float,
//    val speed: Float
//)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusIcon(
    status: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val statusBronze = stringResource(R.string.status_bronze)
    val statusSilver = stringResource(R.string.status_silver)
    val statusGold = stringResource(R.string.status_gold)
    val statusPlatinum = stringResource(R.string.status_platinum)
    val statusDiamond = stringResource(R.string.status_diamond)
    val statusNone = stringResource(R.string.status_none)

    val (resId, localizedStatus) = when (status) {
        "Bronze", statusBronze -> R.drawable.bronze_status to statusBronze
        "Silver", statusSilver -> R.drawable.silver_status to statusSilver
        "Gold", statusGold -> R.drawable.gold_status to statusGold
        "Platinum", statusPlatinum -> R.drawable.platinum_status to statusPlatinum
        "Diamond", statusDiamond -> R.drawable.diamond_status to statusDiamond
        else -> R.drawable.no_status to statusNone
    }

    Image(
        painter = painterResource(id = resId),
        contentDescription = localizedStatus,
        modifier = modifier
            .size(56.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    )
}

//@Composable
//fun ControlCharButton(
//    modifier: Modifier = Modifier,
//    text: String? = null,
//    icon: ImageVector? = null,
//    width: androidx.compose.ui.unit.Dp = 56.dp,
//    onClick: () -> Unit
//) {
//    val colors = LocalAppColors.current
//    Box(
//        modifier = modifier
//            .size(width = width, height = 56.dp)
//            .background(colors.boxBackground, RoundedCornerShape(8.dp))
//            .border(1.dp, colors.boxBorder, RoundedCornerShape(8.dp))
//            .clickable { onClick() },
//        contentAlignment = Alignment.Center
//    ) {
//        if (icon != null) {
//            Icon(
//                imageVector = icon,
//                contentDescription = text,
//                tint = colors.boxText,
//                modifier = Modifier.size(24.dp)
//            )
//        } else if (text != null) {
//            Text(
//                text = text,
//                color = colors.boxText,
//                fontWeight = FontWeight.Bold,
//                fontSize = 20.sp
//            )
//        }
//    }
//}

@Composable
fun HelpItem(icon: ImageVector, label: String, description: String) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, contentDescription = null, tint = colors.boxText, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.boxText)
            Text(description, fontSize = 12.sp, color = colors.textSecondary)
        }
    }
}

//@Composable
//fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
//    val colors = LocalAppColors.current
//    Surface(
//        modifier = Modifier.clickable { onClick() },
//        shape = RoundedCornerShape(16.dp),
//        color = if (selected) colors.boxBorder else colors.boxBackground,
//        border = androidx.compose.foundation.BorderStroke(1.dp, colors.boxBorder)
//    ) {
//        Text(
//            text = label,
//            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
//            fontSize = 10.sp,
//            color = if (selected) colors.boxBackground else colors.boxText
//        )
//    }
//}

@Composable
fun AppShortcut(letter: String, label: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(colors.boxBackground, RoundedCornerShape(AppCornerRadius))
                .border(1.dp, colors.boxBorder, RoundedCornerShape(AppCornerRadius)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = letter, color = colors.boxText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Text(text = label, fontSize = 8.sp, color = colors.textSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun SettingsShortcut(
    icon: ImageVector? = null,
    drawableRes: Int? = null,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(colors.boxBackground, RoundedCornerShape(AppCornerRadius))
            .border(1.dp, colors.boxBorder, RoundedCornerShape(AppCornerRadius))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.boxText,
                modifier = Modifier.size(24.dp)
            )
        } else if (drawableRes != null) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
