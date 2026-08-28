package com.robertotorino.datstation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robertotorino.datstation.ui.AppCornerRadius
import com.robertotorino.datstation.ui.LocalAppColors

data class PlayStationPage(
    val title: String,
    val assetName: String
)

class SystemSoftwareHistoryActivity : ComponentActivity() {

    private val pages = listOf(
        PlayStationPage(
            title = "PlayStation 6 System Software",
            assetName = "ps6_systemsoftware.json"
        ),
        PlayStationPage(
            title = "PlayStation 5 System Software",
            assetName = "ps5_systemsoftware.json"
        ),
        PlayStationPage(
            title = "PlayStation Portal System Software",
            assetName = "psportal_systemsoftware.json"
        ),
        PlayStationPage(
            title = "PlayStation 4 System Software",
            assetName = "ps4_systemsoftware.json"
        ),
        PlayStationPage(
            title = "PlayStation 3 System Software",
            assetName = "ps3_systemsoftware.json"
        ),
        PlayStationPage(
            title = "PlayStation Vita System Software",
            assetName = "psvita_systemsoftware.json"
        ),
        PlayStationPage(
            title = "PlayStation Portable System Software",
            assetName = "psp_systemsoftware.json"
        ),
        PlayStationPage(
            title = "PlayStation 2 System Software",
            assetName = "ps2_systemsoftware.json"
        ),
        PlayStationPage(
            title = "PlayStation 1 System Software",
            assetName = "ps1_systemsoftware.json"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)

        setContent {
            val savedMode = remember { themePrefs.getInt("theme_mode", 2) }
            val currentColors = when (savedMode) {
                0 -> com.robertotorino.datstation.ui.BronzeTheme
                1 -> com.robertotorino.datstation.ui.ClassicTheme
                2 -> com.robertotorino.datstation.ui.BlueTheme
                else -> com.robertotorino.datstation.ui.ChristmasTheme
            }

            CompositionLocalProvider(LocalAppColors provides currentColors) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = currentColors.boxBorder,
                        background = currentColors.background,
                        surface = currentColors.cardBackground,
                        onBackground = currentColors.textPrimary,
                        onSurface = currentColors.textPrimary
                    )
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = currentColors.background) {
                        PlayStationSystemHistoryScreen(
                            pages = pages,
                            onBack = { finish() },
                            onPageSelected = { openDetailPage(it) }
                        )
                    }
                }
            }
        }
    }

    private fun openDetailPage(page: PlayStationPage) {
        val intent = Intent(this, SystemSoftwareActivity::class.java).apply {
            putExtra(SystemSoftwareActivity.EXTRA_TITLE, page.title)
            putExtra(SystemSoftwareActivity.EXTRA_ASSET_NAME, page.assetName)
        }
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayStationSystemHistoryScreen(
    pages: List<PlayStationPage>,
    onBack: () -> Unit,
    onPageSelected: (PlayStationPage) -> Unit
) {
    val currentColors = LocalAppColors.current

    Scaffold(
        containerColor = currentColors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "System Version History",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = currentColors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = currentColors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentColors.background,
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = currentColors.textPrimary,
                    titleContentColor = currentColors.textPrimary,
                    actionIconContentColor = Color.Unspecified
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Text(
                text = "PlayStation\nSystem Version History",
                color = currentColors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pages) { page ->
                    Button(
                        onClick = { onPageSelected(page) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(AppCornerRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentColors.boxBackground,
                            contentColor = currentColors.boxText
                        ),
                        border = BorderStroke(1.5.dp, currentColors.boxBorder)
                    ) {
                        Text(
                            text = page.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
