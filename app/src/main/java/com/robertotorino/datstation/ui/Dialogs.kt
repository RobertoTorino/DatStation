package com.robertotorino.datstation.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.robertotorino.datstation.BuildConfig
import com.robertotorino.datstation.DatabaseManager
import com.robertotorino.datstation.GameRepository
import com.robertotorino.datstation.R
import com.robertotorino.datstation.ui.theme.DataStationTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun LargeStatusDialog(status: String, onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    val sBronze = stringResource(R.string.status_bronze)
    val sSilver = stringResource(R.string.status_silver)
    val sGold = stringResource(R.string.status_gold)
    val sPlatinum = stringResource(R.string.status_platinum)
    val sDiamond = stringResource(R.string.status_diamond)
    val sNone = stringResource(R.string.status_none)

    val displayStatus = when (status) {
        "Bronze", sBronze -> sBronze
        "Silver", sSilver -> sSilver
        "Gold", sGold -> sGold
        "Platinum", sPlatinum -> sPlatinum
        "Diamond", sDiamond -> sDiamond
        else -> status
    }

    val resId = when (status) {
        "Bronze", sBronze -> R.drawable.bronze_status
        "Silver", sSilver -> R.drawable.silver_status
        "Gold", sGold -> R.drawable.gold_status
        "Platinum", sPlatinum -> R.drawable.platinum_status
        "Diamond", sDiamond -> R.drawable.diamond_status
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.close),
                    color = colors.accent
                )
            }
        },
        title = {
            if (status == "None" || status == sNone) {
                Text(
                    stringResource(R.string.no_achievement),
                    color = colors.textPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (status != "None" && status != sNone) {
                    Text(
                        stringResource(R.string.reached_status, displayStatus),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700),
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }

                if (resId != null) {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = status,
                        modifier = Modifier.size(320.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(320.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                val desc = when (status) {
                    "Bronze", sBronze -> stringResource(R.string.bronze_desc)
                    "Silver", sSilver -> stringResource(R.string.silver_desc)
                    "Gold", sGold -> stringResource(R.string.gold_desc)
                    "Platinum", sPlatinum -> stringResource(R.string.platinum_desc)
                    "Diamond", sDiamond -> stringResource(R.string.diamond_desc)
                    else -> stringResource(R.string.none_desc)
                }
                Text(desc, color = colors.textSecondary, textAlign = TextAlign.Center)
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(AppCornerRadius)
    )
}

@Preview(name = "English", locale = "en")
@Composable
fun LargeStatusDialogEnglishPreview() {
    DataStationTheme {
        val colors = ClassicTheme
        CompositionLocalProvider(LocalAppColors provides colors) {
            LargeStatusDialog(status = "Gold", onDismiss = {})
        }
    }
}

@Preview(name = "Spanish", locale = "es")
@Composable
fun LargeStatusDialogSpanishPreview() {
    DataStationTheme {
        val colors = ClassicTheme
        CompositionLocalProvider(LocalAppColors provides colors) {
            LargeStatusDialog(status = "Gold", onDismiss = {})
        }
    }
}

@Preview(name = "Russian", locale = "ru")
@Composable
fun LargeStatusDialogRussianPreview() {
    DataStationTheme {
        val colors = ClassicTheme
        CompositionLocalProvider(LocalAppColors provides colors) {
            LargeStatusDialog(status = "Gold", onDismiss = {})
        }
    }
}

@Preview(name = "Chinese", locale = "zh")
@Composable
fun LargeStatusDialogChinesePreview() {
    DataStationTheme {
        val colors = ClassicTheme
        CompositionLocalProvider(LocalAppColors provides colors) {
            LargeStatusDialog(status = "Gold", onDismiss = {})
        }
    }
}

@Preview(name = "Japanese", locale = "ja")
@Composable
fun LargeStatusDialogJapanesePreview() {
    DataStationTheme {
        val colors = ClassicTheme
        CompositionLocalProvider(LocalAppColors provides colors) {
            LargeStatusDialog(status = "Gold", onDismiss = {})
        }
    }
}

@Preview(name = "Spanish", locale = "es")
@Composable
fun RegionInfoSpanishPreview() {
    DataStationTheme {
        val colors = ClassicTheme
        CompositionLocalProvider(LocalAppColors provides colors) {
            RegionCodesInfoDialog(onDismiss = {})
        }
    }
}

@Composable
fun StatusAchievementDialog(status: String, onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val sBronze = stringResource(R.string.status_bronze)
    val sSilver = stringResource(R.string.status_silver)
    val sGold = stringResource(R.string.status_gold)
    val sPlatinum = stringResource(R.string.status_platinum)
    val sDiamond = stringResource(R.string.status_diamond)

    // Determine the rank string to display in "You reached X status!"
    val displayStatus = when (status) {
        "Bronze" -> sBronze
        "Silver" -> sSilver
        "Gold" -> sGold
        "Platinum" -> sPlatinum
        "Diamond" -> sDiamond
        else -> status
    }

    val resId = when (status) {
        "Bronze", sBronze -> R.drawable.bronze_status
        "Silver", sSilver -> R.drawable.silver_status
        "Gold", sGold -> R.drawable.gold_status
        "Platinum", sPlatinum -> R.drawable.platinum_status
        "Diamond", sDiamond -> R.drawable.diamond_status
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    if (status == "Welcome") stringResource(R.string.lets_go)
                    else stringResource(R.string.awesome),
                    color = colors.accent
                )
            }
        },
        title = {
            if (status == "Welcome") {
                val db = DatabaseManager.getDatabase(context)
                val repo = GameRepository(db)
                val charKey = repo.getUserProfileValue("user_character") ?: "moonshie"
                val charName = if (charKey == "roberto") "Roberto" else "Moonshie"

                Text(
                    stringResource(R.string.welcome_title, charName),
                    color = colors.textPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (status == "Welcome") {
                    Text(
                        text = stringResource(R.string.welcome_desc),
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.rank_list),
                        color = colors.accent,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    val db = DatabaseManager.getDatabase(context)
                    val repo = GameRepository(db)
                    val charKey = repo.getUserProfileValue("user_character") ?: "moonshie"
                    val charRes =
                        if (charKey == "roberto") R.drawable.roberto_profile_default else R.drawable.moonshie_profile_default

                    Image(
                        painter = painterResource(id = charRes),
                        contentDescription = "Profile Character",
                        modifier = Modifier.size(100.dp)
                    )
                } else {
                    Text(
                        stringResource(R.string.reached_status, displayStatus),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700),
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                    if (resId != null) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = status,
                            modifier = Modifier.size(320.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    val desc = when (status) {
                        "Bronze", sBronze -> stringResource(R.string.bronze_desc)
                        "Silver", sSilver -> stringResource(R.string.silver_desc)
                        "Gold", sGold -> stringResource(R.string.gold_desc)
                        "Platinum", sPlatinum -> stringResource(R.string.platinum_desc)
                        "Diamond", sDiamond -> stringResource(R.string.diamond_desc)
                        else -> ""
                    }
                    Text(
                        desc,
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(AppCornerRadius)
    )
}

@Preview(name = "Achievement English", locale = "en")
@Composable
fun StatusAchievementDialogEnglishPreview() {
    DataStationTheme {
        val colors = ClassicTheme
        CompositionLocalProvider(LocalAppColors provides colors) {
            StatusAchievementDialog(status = "Gold", onDismiss = {})
        }
    }
}

@Preview(name = "Achievement Russian", locale = "ru")
@Composable
fun StatusAchievementDialogRussianPreview() {
    DataStationTheme {
        val colors = ClassicTheme
        CompositionLocalProvider(LocalAppColors provides colors) {
            StatusAchievementDialog(status = "Gold", onDismiss = {})
        }
    }
}

@Preview(name = "Achievement Japanese", locale = "ja")
@Composable
fun StatusAchievementDialogJapanesePreview() {
    DataStationTheme {
        val colors = ClassicTheme
        CompositionLocalProvider(LocalAppColors provides colors) {
            StatusAchievementDialog(status = "Gold", onDismiss = {})
        }
    }
}

@Preview(name = "Achievement Chinese", locale = "zh")
@Composable
fun StatusAchievementDialogChinesePreview() {
    DataStationTheme {
        val colors = ClassicTheme
        CompositionLocalProvider(LocalAppColors provides colors) {
            StatusAchievementDialog(status = "Gold", onDismiss = {})
        }
    }
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.got_it),
                    color = colors.accent
                )
            }
        },
        title = { Text(stringResource(R.string.help_title), color = colors.textPrimary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.web_search_help),
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HelpItem(
                    Icons.Default.Star,
                    stringResource(R.string.gold_star),
                    stringResource(R.string.gold_star_desc)
                )
                HelpItem(
                    Icons.Default.Album,
                    stringResource(R.string.blue_disc),
                    stringResource(R.string.blue_disc_desc)
                )
                HelpItem(
                    Icons.Default.Mic,
                    stringResource(R.string.microphone),
                    stringResource(R.string.microphone_desc)
                )
                HelpItem(
                    Icons.Default.Info,
                    stringResource(R.string.info_icon),
                    stringResource(R.string.info_icon_desc)
                )
                HelpItem(
                    Icons.Default.VideogameAsset,
                    stringResource(R.string.controller),
                    stringResource(R.string.controller_desc)
                )
                HelpItem(
                    Icons.Default.DirectionsCar,
                    stringResource(R.string.car),
                    stringResource(R.string.car_desc)
                )
                HelpItem(
                    Icons.Default.QuestionAnswer,
                    stringResource(R.string.quiz_label),
                    stringResource(R.string.quiz_desc)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.playing_label),
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
                Text(
                    stringResource(R.string.playing_desc),
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.progression_system),
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
                Text(
                    stringResource(R.string.rank_list),
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.voice_commands),
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
                Text(
                    stringResource(R.string.voice_commands_desc),
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(AppCornerRadius)
    )
}

@Preview(name = "Russian", locale = "ru")
@Composable
fun HelpDialogRussianPreview() {
    DataStationTheme {
        val colors = ClassicTheme
        CompositionLocalProvider(LocalAppColors provides colors) {
            HelpDialog(onDismiss = {})
        }
    }
}

@Preview(name = "Chinese", locale = "zh")
@Composable
fun HelpDialogChinesePreview() {
    DataStationTheme {
        val colors = ClassicTheme
        CompositionLocalProvider(LocalAppColors provides colors) {
            HelpDialog(onDismiss = {})
        }
    }
}

@Composable
fun StatsDialog(stats: Map<String, Int>, onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.close),
                    color = colors.accent
                )
            }
        },
        title = { Text(stringResource(R.string.db_stats), color = colors.textPrimary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                StatCard(
                    label = stringResource(R.string.total_games),
                    value = stats["TOTAL"]?.toString() ?: "0",
                    color = colors.accent
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard(
                            label = stringResource(R.string.physical),
                            value = stats["DISC"]?.toString() ?: "0",
                            color = Color(0xFF4FC3F7)
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard(
                            label = stringResource(R.string.digital),
                            value = stats["DIGITAL"]?.toString() ?: "0",
                            color = Color(0xFF81C784)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.platform_breakdown),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = colors.textSecondary.copy(alpha = 0.2f)
                )

                stats.filter { it.key != "TOTAL" && it.key != "DISC" && it.key != "DIGITAL" }
                    .forEach { (platform, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(platform, color = colors.textPrimary, fontSize = 14.sp)
                            Text(
                                count.toString(),
                                color = colors.accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(AppCornerRadius)
    )
}

@Composable
fun StatCard(label: String, value: String, color: Color) {
    val appColors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(appColors.boxBackground, RoundedCornerShape(AppCornerRadius))
            .border(1.dp, appColors.boxBorder.copy(alpha = 0.3f), RoundedCornerShape(AppCornerRadius))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 12.sp, color = appColors.textSecondary)
    }
}

@Composable
fun HistoryDialog(onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    var enlargedImageResId by remember { mutableStateOf<Int?>(null) }
    var showSearchError by remember { mutableStateOf(false) }

    if (enlargedImageResId != null) {
        EnlargedImageDialog(resId = enlargedImageResId!!) {
            enlargedImageResId = null
        }
    }

    if (showSearchError) {
        AlertDialog(
            onDismissRequest = { showSearchError = false },
            confirmButton = {
                TextButton(onClick = { showSearchError = false }) {
                    Text(stringResource(R.string.close), color = colors.accent)
                }
            },
            title = { Text("Search failed", color = colors.textPrimary) },
            text = {
                Text(
                    "Search failed. For this feature to show results you need an active internet connection.",
                    color = colors.textSecondary
                )
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(AppCornerRadius)
        )
    }

    val onSearchClick: (String) -> Unit = { query ->
        if (isNetworkAvailable(context)) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "https://www.google.com/search?q=${Uri.encode(query)}&tbm=isch".toUri()
            }
            context.startActivity(intent)
        } else {
            showSearchError = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close), color = colors.accent)
            }
        },
        title = {
            Text(stringResource(R.string.ps_history), color = colors.textPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.lifetime_totals),
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )

                Spacer(modifier = Modifier.height(8.dp))

                ConsoleStatRow(
                    "PlayStation (1994)",
                    "102.4M sold",
                    "3,168 titles",
                    "[ps1, ps1_netyaroze, ps_one]",
                    onImageClick = { enlargedImageResId = it }
                )
                ConsoleStatRow(
                    "PlayStation 2 (2000)",
                    "160M sold",
                    "3,482 titles,\nNatively backward compatible with more than 99% of the entire physical PS1 disc library.",
                    "[ps2, ps2_blue, ps2_blue_network_adapter, ps2_gold, ps2_white, ps2_transparent_blue, ps2_slim]",
                    onImageClick = { enlargedImageResId = it }
                )
                ConsoleStatRow(
                    "Sony PSX (2003)",
                    "160M sold",
                    "3,482 titles,\nNatively backward compatible with more than 99% of the entire physical PS1 disc library.",
                    "psx_desr",
                    onImageClick = { enlargedImageResId = it }
                )
                ConsoleStatRow(
                    "PlayStation Portable (2004)",
                    "82.5M sold",
                    "2,277 titles\nDigital library of PS1 Classics with over 1,300 digital PS1 legacy games.",
                    "[psp, pspgo, pspgo_white]",
                    onImageClick = { enlargedImageResId = it }
                )
                ConsoleStatRow(
                    "PlayStation 3 (2006)",
                    "87.4M sold",
                    "5,156 titles\nNatively runs almost 100% of physical PlayStation 1 discs. Launch 20GB and 60GB models with 4 front USB ports run over 99% of the PS2's title catalog.",
                    "[ps3, ps3_slim, ps3_superslim]",
                    onImageClick = { enlargedImageResId = it }
                )
                ConsoleStatRow(
                    "PlayStation Vita (2011)",
                    "13.5M sold",
                    "1,732 titles\nBackward compatibility with digital PSP games and digital PS1 Classics (PSN).",
                    "[psv, psvita_blue, psvtv]",
                    onImageClick = { enlargedImageResId = it }
                )
                ConsoleStatRow(
                    "PlayStation 4 (2013)",
                    "117M sold",
                    "11,947 titles",
                    "[ps4, ps4_pro, ps4_slim]",
                    onImageClick = { enlargedImageResId = it }
                )
                ConsoleStatRow(
                    "PlayStation 5 (2020)",
                    "95.1M sold",
                    "4,761 titles,\n12,160 backward compatible titles",
                    "[ps5, ps5_digital, ps5_pro, ps5_pro_box]",
                    onImageClick = { enlargedImageResId = it }
                )
                ConsoleStatRow(
                    "PSPortal (2023)",
                    "2M sold (2024)",
                    "Has streaming capabilities for PS5 console games",
                    "psportal",
                    onImageClick = { enlargedImageResId = it }
                )

                Spacer(modifier = Modifier.height(6.dp))

                HistoryItem("Update Info", "Source: Internet. Updated 2026-08-23.")

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(
                    thickness = 1.dp,
                    color = colors.textSecondary.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    stringResource(R.string.legacy_overview),
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )

                Spacer(modifier = Modifier.height(8.dp))

                HistoryItem(
                    "PlayStation (1994)",
                    "The console that started it all, bringing CD-ROM gaming to the mainstream and launching iconic series like Resident Evil, Tekken, and Final Fantasy VII.",
                    onTitleClick = onSearchClick
                )

                HistoryItem(
                    "PlayStation 2 (2000)",
                    "The best-selling console ever made. It helped popularize DVD playback and built one of the most influential game libraries in history.",
                    onTitleClick = onSearchClick
                )

                HistoryItem(
                    "Sony PSX (2003)",
                    "A hybrid consumer electronics device released by Sony in Japan on December 13, 2003. It uniquely combined a fully functional PlayStation 2 console with a digital video recorder (DVR) and television tuner. PS2's Emotion Engine CPU and Graphics Synthesizer was integrated into the device. With an internal IDE hard drives of 160GB for the 5000 series and 250GB for the 7000 series, allowing users to record television broadcasts and burn them onto writable DVDs. It was the very first Sony device to feature the XrossMediaBar (XMB) graphical user interface, which later became the standard menu system for the PlayStation Portable (PSP) and PlayStation 3 (PS3). Japan Exclusive: High launch prices (79,800 to 99,800 yen) and hardware reliability issues led to sluggish long-term sales, prompting Sony to cancel planned international releases.",
                    onTitleClick = onSearchClick
                )

                HistoryItem(
                    "PlayStation Portable (2004)",
                    "Sony's first handheld delivered console-style experiences on the go, multimedia features, and standout titles like God of War: Chains of Olympus and Crisis Core.",
                    onTitleClick = onSearchClick
                )

                HistoryItem(
                    "PlayStation 3 (2006)",
                    "A major leap into the HD era, introducing Blu-ray, the PlayStation Network, and landmark games such as Uncharted, Demon’s Souls, and The Last of Us.",
                    onTitleClick = onSearchClick
                )

                HistoryItem(
                    "PlayStation Vita (2011)",
                    "A powerful handheld with an OLED screen, dual analog sticks, and a passionate fanbase. It became especially loved for indie games, JRPGs, and remote play.",
                    onTitleClick = onSearchClick
                )

                HistoryItem(
                    "PlayStation 4 (2013)",
                    "A hugely successful generation focused on players, strong first-party exclusives, and a refined online ecosystem with hits like Bloodborne, God of War, and Spider-Man.",
                    onTitleClick = onSearchClick
                )

                HistoryItem(
                    "PlayStation 5 (2020)",
                    "The current generation, defined by ultra-fast SSD storage, haptic feedback, adaptive triggers, and visually advanced games with ray tracing and high frame rates.",
                    onTitleClick = onSearchClick
                )

                HistoryItem(
                    "PlayStation Portal (2023)",
                    "A handheld remote-play accessory designed exclusively for the PlayStation 5. Streams games directly from a local PS5 console over Wi-Fi, rather than running games natively on the device itself. Features an 8-inch LCD screen capable of 1080p resolution at 60 frames per second. It requires a stable internet connection, an active PS5 console to host the game and does not support Bluetooth audio requiring PlayStation Link wireless or a 3.5mm wired jack.",
                    onTitleClick = onSearchClick
                )
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(AppCornerRadius)
    )
}

@Composable
fun ConsoleStatRow(
    name: String,
    sold: String,
    titles: String,
    imageResourceName: String? = null,
    onImageClick: (Int) -> Unit = {}
) {
    val colors = LocalAppColors.current

    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = name,
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )

        if (imageResourceName != null) {
            val names = when {
                imageResourceName.startsWith("[") && imageResourceName.endsWith("]") -> {
                    imageResourceName.substring(1, imageResourceName.length - 1).split(",").map { it.trim() }
                }
                imageResourceName.startsWith("[") -> {
                    imageResourceName.substring(1).split(",").map { it.trim() }
                }
                else -> listOf(imageResourceName.trim())
            }

            val resIds = names.mapNotNull { getConsoleImageResourceId(it) }

            if (resIds.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(resIds) { resId ->
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = name,
                            modifier = Modifier
                                .width(if (resIds.size == 1) 300.dp else 180.dp)
                                .height(120.dp)
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(AppCornerRadius))
                                .clickable { onImageClick(resId) },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(vertical = 4.dp)
                        .background(
                            colors.boxBackground.copy(alpha = 0.5f),
                            RoundedCornerShape(AppCornerRadius)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Image: $imageResourceName",
                        color = colors.textSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Text(
            text = "$sold • $titles",
            color = colors.textSecondary,
            fontSize = 13.sp
        )
    }
}

private fun getConsoleImageResourceId(name: String): Int? {
    val resId = when (name) {
        "ps1" -> R.drawable.ps1
        "ps1_netyaroze" -> R.drawable.ps1_netyaroze
        "ps_one" -> R.drawable.ps_one
        "ps2" -> R.drawable.ps2
        "ps2_blue" -> R.drawable.ps2_blue
        "ps2_blue_network_adapter" -> R.drawable.ps2_blue_network_adapter
        "ps2_gold" -> R.drawable.ps2_gold
        "ps2_transparent_blue" -> R.drawable.ps2_transparent_blue
        "ps2_white" -> R.drawable.ps2_white
        "ps2_slim" -> R.drawable.ps2_slim
        "psx_desr" -> R.drawable.psx_desr
        "ps3" -> R.drawable.ps3
        "ps3_slim" -> R.drawable.ps3_slim
        "ps3_superslim", "ps3_super_slim" -> R.drawable.ps3_superslim
        "psp" -> R.drawable.psp
        "pspgo" -> R.drawable.pspgo
        "pspgo_white" -> R.drawable.pspgo_white
        "ps4" -> R.drawable.ps4
        "ps4_pro" -> R.drawable.ps4_pro
        "ps4_slim" -> R.drawable.ps4_slim
        "psv", "psvita" -> R.drawable.psv
        "psvita_blue", "psv_psvita_blue" -> R.drawable.psvita_blue
        "psvtv" -> R.drawable.psvtv
        "ps5" -> R.drawable.ps5
        "ps5_digital" -> R.drawable.ps5_digital
        "ps5_pro" -> R.drawable.ps5_pro
        "ps5_pro_box" -> R.drawable.ps5_pro_box
        "psportal" -> R.drawable.psportal
        else -> 0
    }
    return if (resId != 0) resId else null
}

@Composable
private fun HistoryItem(
    title: String,
    description: String,
    onTitleClick: ((String) -> Unit)? = null
) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (onTitleClick != null) colors.accent else colors.textPrimary,
            modifier = if (onTitleClick != null) Modifier.clickable { onTitleClick(title) } else Modifier
        )
        Text(description, fontSize = 12.sp, color = colors.textSecondary)
    }
}

@Composable
private fun EnlargedImageDialog(resId: Int, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = "Enlarged Image",
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

private fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
        else -> false
    }
}

@Composable
fun RegionCodesInfoDialog(onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.close),
                    color = colors.accent
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.region_info_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
        },
        text = {
            Column(modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())) {
                InfoCategory("PlayStation 6", stringResource(R.string.coming_soon))
                InfoCategory("PlayStation 5") {
                    Text(
                        stringResource(R.string.ps5_desc),
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoTable(
                        headers = listOf(
                            stringResource(R.string.prefix),
                            stringResource(R.string.market),
                            stringResource(R.string.description)
                        ),
                        rows = listOf(
                            listOf("PPSA", "US / EU", "Retail + digital"),
                            listOf("ELJM", "Japan", "Physical Retail"),
                            listOf("ECAS", "Asia", "Physical Retail"),
                            listOf("ELAS", "Asia", "Physical Retail"),
                            listOf("NPXS", "NoRegion", "System Reserved")
                        )
                    )
                }
                InfoCategory("PlayStation 4") {
                    InfoTable(
                        headers = listOf(
                            stringResource(R.string.prefix),
                            "Reg_S",
                            stringResource(R.string.region),
                            stringResource(R.string.sony_entity)
                        ),
                        rows = listOf(
                            listOf("EP", "EU", "Europe", "SIEE"),
                            listOf("HP", "HK", "Asia", "SIE Asia"),
                            listOf("IP", "IP", "Proto", "Internal"),
                            listOf("JP", "JP", "Japan", "SIEI"),
                            listOf("KP", "KO", "Korea", "SIEK"),
                            listOf("UP", "US", "N. Am", "SIEA")
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.ps4_desc),
                        fontSize = 12.sp,
                        color = colors.textSecondary.copy(alpha = 0.6f)
                    )
                }
                InfoCategory("PlayStation 3") {
                    Text(
                        stringResource(R.string.ps3_desc),
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoTable(
                        headers = listOf(
                            stringResource(R.string.prefix),
                            "Reg",
                            stringResource(R.string.description)
                        ),
                        rows = listOf(
                            listOf("BCAS", "Asia", "First-Party"),
                            listOf("BCES", "Europe", "First-Party"),
                            listOf("BCJS", "Japan", "First-Party"),
                            listOf("BCUS", "USA", "First-Party"),
                            listOf("BLES", "Europe", "Third-Party"),
                            listOf("BLUS", "USA", "Third-Party"),
                            listOf("NPEB", "Europe", "Digital 3rd"),
                            listOf("NPUB", "USA", "Digital 3rd"),
                            listOf("NYMPH", "HK", "Digital Classics"),
                            listOf("NPIA", "NoRegion", "HOME")
                        )
                    )
                }
                InfoCategory("PlayStation 2") {
                    Text(
                        stringResource(R.string.ps2_desc),
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoTable(
                        headers = listOf(
                            stringResource(R.string.prefix),
                            stringResource(R.string.region),
                            stringResource(R.string.description)
                        ),
                        rows = listOf(
                            listOf("SCPS", "Japan", "First-Party"),
                            listOf("SLPS", "Japan", "Third-Party"),
                            listOf("SCUS", "USA", "First-Party"),
                            listOf("SLUS", "USA", "Third-Party"),
                            listOf("SCES", "Europe", "First-Party"),
                            listOf("SLES", "Europe", "Third-Party"),
                            listOf("SCKA", "Korea", "First-Party"),
                            listOf("SLKA", "Korea", "Third-Party"),
                            listOf("SCED", "Europe", "Demo"),
                            listOf("PAPX / PBX", "NoRegion", "Promo / Utility")
                        )
                    )
                }
                InfoCategory("PlayStation 1") {
                    Text(
                        stringResource(R.string.ps1_desc),
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoTable(
                        headers = listOf(
                            stringResource(R.string.prefix),
                            stringResource(R.string.region),
                            stringResource(R.string.description)
                        ),
                        rows = listOf(
                            listOf("SCPS", "Japan", "First-Party"),
                            listOf("SLPS", "Japan", "Third-Party"),
                            listOf("SCUS", "USA", "First-Party"),
                            listOf("SLUS", "USA", "Third-Party"),
                            listOf("SCES", "Europe", "First-Party"),
                            listOf("SLES", "Europe", "Third-Party"),
                            listOf("SCKA", "Korea", "First-Party"),
                            listOf("SLKA", "Korea", "Third-Party"),
                            listOf("SCED", "Europe", "Demo"),
                            listOf("PBPX", "NoRegion", "Promo / Utility")
                        )
                    )
                    Text(
                        stringResource(R.string.ps_mapping_footer),
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(AppCornerRadius)
    )
}

@Composable
fun ThemeSelectionDialog(
    currentThemeMode: Int,
    onThemeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val themes = listOf(
        Triple(0, "Bronze", "A warm, metallic theme with deep blue accents."),
        Triple(1, "Classic", "The original dark interface for a clean look."),
        Triple(2, "PlayStation", "The iconic blue and white aesthetic."),
        Triple(3, "Christmas", "A festive green and red theme."),
        Triple(4, "Red", "Bold red theme with dark accents."),
        Triple(5, "Silver", "Sleek silver and grey theme.")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close), color = colors.accent)
            }
        },
        title = {
            Text(
                stringResource(R.string.switch_theme),
                color = colors.textPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                themes.forEach { (mode, name, desc) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(mode); onDismiss() },
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentThemeMode == mode) colors.accent.copy(alpha = 0.2f) else Color.Transparent
                        ),
                        border = if (currentThemeMode == mode) androidx.compose.foundation.BorderStroke(
                            2.dp,
                            colors.accent
                        ) else null,
                        shape = RoundedCornerShape(AppCornerRadius)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                name,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontSize = 16.sp
                            )
                            Text(desc, color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(AppCornerRadius)
    )
}

@Composable
fun CharacterSelectionDialog(onCharacterSelected: (String) -> Unit) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = {}, // Force selection
        confirmButton = {},
        title = {
            Text(
                stringResource(R.string.character_selection_title),
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CharacterOptionItem("Moonshie", R.drawable.moonshie_profile_default) {
                    onCharacterSelected("moonshie")
                }
                CharacterOptionItem("Roberto", R.drawable.roberto_profile_default) {
                    onCharacterSelected("roberto")
                }
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(AppCornerRadius)
    )
}

@Composable
fun CharacterOptionItem(name: String, resId: Int, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = name,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(name, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CharacterProfilesDialog(
    onSelectionChange: (String) -> Unit,
    onViewProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button), color = colors.accent)
            }
        },
        title = {
            Text(
                stringResource(R.string.character_profiles),
                color = colors.textPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ProfileOptionRow(
                    "Moonshie", R.drawable.moonshie_profile_default,
                    onSelect = { onSelectionChange("moonshie"); onDismiss() },
                    onView = { onViewProfile("moonshie") }
                )
                ProfileOptionRow(
                    "Roberto", R.drawable.roberto_profile_default,
                    onSelect = { onSelectionChange("roberto"); onDismiss() },
                    onView = { onViewProfile("roberto") }
                )
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(AppCornerRadius)
    )
}

@Composable
fun ProfileOptionRow(name: String, resId: Int, onSelect: () -> Unit, onView: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onSelect() }) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = name,
                modifier = Modifier.size(50.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(name, color = colors.textPrimary, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onView) {
            Text(stringResource(R.string.view_profile_button), color = colors.accent, fontSize = 12.sp)
        }
    }
}

@Composable
fun FullscreenVideoPlayer(videoResId: Int, onFinished: () -> Unit) {
    Dialog(
        onDismissRequest = onFinished,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        setVideoURI("android.resource://${context.packageName}/$videoResId".toUri())
                        setOnCompletionListener { onFinished() }
                        start()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Optional: Add a skip/close button
            IconButton(
                onClick = onFinished,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun IndividualProfileDialog(character: String, onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    val isMoonshie = character == "moonshie"
    var showVideo by remember { mutableStateOf<Int?>(null) }

    if (showVideo != null) {
        FullscreenVideoPlayer(videoResId = showVideo!!, onFinished = { showVideo = null })
    }

    val profileData = if (isMoonshie) {
        listOf(
            stringResource(R.string.moonshie_full_name),
            stringResource(R.string.moonshie_nick_name),
            stringResource(R.string.moonshie_birthday),
            stringResource(R.string.moonshie_sports),
            stringResource(R.string.moonshie_born),
            stringResource(R.string.moonshie_height),
            stringResource(R.string.moonshie_weight),
            stringResource(R.string.moonshie_eyes),
            stringResource(R.string.moonshie_hair),
            stringResource(R.string.moonshie_likes),
            stringResource(R.string.moonshie_dislikes),
            stringResource(R.string.moonshie_gender),
            stringResource(R.string.moonshie_age),
            stringResource(R.string.moonshie_relation),
            stringResource(R.string.moonshie_occupation)
        )
    } else {
        listOf(
            stringResource(R.string.roberto_full_name),
            stringResource(R.string.roberto_nick_name),
            stringResource(R.string.roberto_birthday),
            stringResource(R.string.roberto_sports),
            stringResource(R.string.roberto_born),
            stringResource(R.string.roberto_height),
            stringResource(R.string.roberto_weight),
            stringResource(R.string.roberto_eyes),
            stringResource(R.string.roberto_hair),
            stringResource(R.string.roberto_likes),
            stringResource(R.string.roberto_dislikes),
            stringResource(R.string.roberto_gender),
            stringResource(R.string.roberto_age),
            stringResource(R.string.roberto_relation),
            stringResource(R.string.roberto_occupation)
        )
    }

    val images = if (isMoonshie) {
        listOf(
            R.drawable.moonshie_full_size_diamond,
            R.drawable.moonshie_full_size_gold,
            R.drawable.moonshie_monaco_home_wide,
            R.drawable.activities_small,
            R.drawable.monaco_at_night_casino,
            R.drawable.monaco_at_night_harbor,
            R.drawable.combined_full_diamond_01,
            R.drawable.combined_full_diamond_02
        )
    } else {
        listOf(
            R.drawable.roberto_full_size_diamond,
            R.drawable.roberto_full_size_gold,
            R.drawable.monaco_dj_booth,
            R.drawable.activities_small,
            R.drawable.monaco_at_night_casino,
            R.drawable.monaco_at_night_harbor,
            R.drawable.combined_full_diamond_01,
            R.drawable.combined_full_diamond_02
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = colors.accent) }
        },
        title = {
            Text(
                if (isMoonshie) stringResource(R.string.moonshie_profile) else stringResource(R.string.roberto_profile),
                color = colors.textPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showVideo = if (isMoonshie) R.raw.moonshie_profile_video else R.raw.roberto_profile_video },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.boxBackground)
                    ) {
                        Text(stringResource(R.string.watch_profile_video), color = colors.boxText, fontSize = 11.sp, maxLines = 1)
                    }
                    Button(
                        onClick = { showVideo = R.raw.momo_toto },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.boxBackground)
                    ) {
                        Text(stringResource(R.string.watch_special_video), color = colors.boxText, fontSize = 11.sp, maxLines = 1)
                    }
                }

                profileData.forEach { line ->
                    Text(line, color = colors.textPrimary, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                images.forEach { resId ->
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(vertical = 8.dp)
                    )
                }
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(AppCornerRadius),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val colors = LocalAppColors.current

    val buildDate = remember {
        try {
            val millis = BuildConfig.BUILD_TIME.toLong()
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
        } catch (_: Exception) {
            BuildConfig.BUILD_TIME
        }
    }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val ownerText = stringResource(R.string.about_owner, currentYear)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.about_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.about_app_name),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary
                )
                Text(
                    "Version: ${BuildConfig.VERSION_NAME} ($buildDate)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary
                )
                Text(
                    ownerText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.got_it), color = colors.accent)
            }
        },
        containerColor = colors.background,
        textContentColor = colors.textPrimary,
        shape = RoundedCornerShape(AppCornerRadius)
    )
}

@Composable
fun InfoCategory(title: String, content: String? = null, slot: @Composable (() -> Unit)? = null) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.accent,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = colors.textSecondary.copy(alpha = 0.2f)
        )
        if (content != null) Text(content, fontSize = 14.sp, color = colors.textPrimary)
        slot?.invoke()
    }
}

@Composable
fun InfoTable(headers: List<String>, rows: List<List<String>>) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.textSecondary.copy(alpha = 0.1f))
                .padding(2.dp)
        ) {
            headers.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
            }
        }
        rows.forEach { row ->
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)) {
                row.forEach {
                    Text(
                        it,
                        modifier = Modifier.weight(1f),
                        fontSize = 10.sp,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}
