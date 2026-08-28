package com.robertotorino.datstation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.robertotorino.datstation.ui.AppCornerRadius
import com.robertotorino.datstation.ui.LocalAppColors
import org.json.JSONArray

class AppCatalogActivity : ComponentActivity() {
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
                        PlayStationAppCatalogScreen(
                            onBack = { finish() }
                        )
                    }
                }
            }
        }
    }
}

data class ConsoleAppInfo(
    val app: String,
    val category: String,
    val platforms: Map<String, String>,
    val availabilityRegion: String,
    val overallStatus: String,
    val documentationLevel: String,
    val notes: String
)

private fun loadPlayStationAppsFromAssets(context: android.content.Context): List<ConsoleAppInfo> {
    val json = context.assets.open("playstation_apps.json")
        .bufferedReader()
        .use { it.readText() }

    val array = JSONArray(json)
    val result = mutableListOf<ConsoleAppInfo>()

    for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val platformsObj = obj.getJSONObject("platforms")
        val platforms = mutableMapOf<String, String>()

        platformsObj.keys().forEach { key ->
            platforms[key] = platformsObj.getString(key)
        }

        result += ConsoleAppInfo(
            app = obj.optString("app"),
            category = obj.optString("category"),
            platforms = platforms,
            availabilityRegion = obj.optString("availabilityRegion"),
            overallStatus = obj.optString("overallStatus"),
            documentationLevel = obj.optString("documentationLevel"),
            notes = obj.optString("notes")
        )
    }

    return result.sortedWith(compareBy<ConsoleAppInfo> { it.category }.thenBy { it.app })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayStationAppCatalogScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentColors = LocalAppColors.current
    var apps by remember { mutableStateOf<List<ConsoleAppInfo>>(emptyList()) }
    var searchText by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("All") }

    val platforms = listOf("All", "PSP", "PS Vita", "PS3", "PS4", "PS5")

    LaunchedEffect(Unit) {
        apps = loadPlayStationAppsFromAssets(context)
    }

    val filteredApps = apps.filter { item ->
        val platformMatch = selectedPlatform == "All" || item.platforms.containsKey(selectedPlatform.toPlatformKey())
        val query = searchText.trim().lowercase()
        val searchMatch = query.isBlank() || listOf(
            item.app,
            item.category,
            item.availabilityRegion,
            item.overallStatus,
            item.documentationLevel,
            item.notes,
            item.platforms.values.joinToString(" ")
        ).any { it.lowercase().contains(query) }

        platformMatch && searchMatch
    }

    Scaffold(
        containerColor = currentColors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "PlayStation Apps",
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
            CatalogHeader(
                totalCount = apps.size,
                visibleCount = filteredApps.size,
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                platforms = platforms,
                selectedPlatform = selectedPlatform,
                onPlatformSelected = { selectedPlatform = it }
            )

            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No apps found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = currentColors.textPrimary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredApps,
                        key = { it.app + it.platforms.keys.joinToString() }
                    ) { item ->
                        AppCatalogCard(item = item)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogHeader(
    totalCount: Int,
    visibleCount: Int,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    platforms: List<String>,
    selectedPlatform: String,
    onPlatformSelected: (String) -> Unit
) {
    val currentColors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "$visibleCount of $totalCount apps",
            style = MaterialTheme.typography.labelLarge,
            color = currentColors.textSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = currentColors.boxText
                )
            },
            label = { Text("Search apps", color = currentColors.textPrimary) },
            shape = RoundedCornerShape(AppCornerRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = currentColors.boxBackground,
                unfocusedContainerColor = currentColors.boxBackground,
                focusedBorderColor = currentColors.boxBorder,
                unfocusedBorderColor = currentColors.boxBorder,
                focusedTextColor = currentColors.boxText,
                unfocusedTextColor = currentColors.boxText,
                cursorColor = currentColors.accent
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            platforms.forEach { platform ->
                FilterChip(
                    selected = selectedPlatform == platform,
                    onClick = { onPlatformSelected(platform) },
                    label = { Text(platform, color = if (selectedPlatform == platform) currentColors.background else currentColors.boxText) },
                    shape = RoundedCornerShape(AppCornerRadius),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = currentColors.boxBackground,
                        selectedContainerColor = currentColors.accent,
                        labelColor = currentColors.boxText,
                        selectedLabelColor = currentColors.background
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedPlatform == platform,
                        borderColor = currentColors.boxBorder,
                        selectedBorderColor = currentColors.boxBorder
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppCatalogCard(item: ConsoleAppInfo) {
    val currentColors = LocalAppColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppCornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = currentColors.cardBackground
        ),
        border = BorderStroke(1.5.dp, currentColors.boxBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = item.app,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = currentColors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.category,
                style = MaterialTheme.typography.bodyMedium,
                color = currentColors.accent,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item.platforms.forEach { (platformKey, value) ->
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = "${platformKey.toPlatformLabel()}: $value",
                                color = currentColors.boxText
                            )
                        },
                        shape = RoundedCornerShape(AppCornerRadius),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = currentColors.boxBackground
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = currentColors.boxBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(label = "Status", value = item.overallStatus)
            InfoRow(label = "Region", value = item.availabilityRegion)
            InfoRow(label = "Documentation", value = item.documentationLevel)

            if (item.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = currentColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    if (value.isBlank()) return
    val currentColors = LocalAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = currentColors.textSecondary,
            modifier = Modifier.width(112.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = currentColors.textPrimary
        )
    }
}

private fun String.toPlatformKey(): String = when (this) {
    "PSP" -> "psp"
    "PS Vita" -> "ps_vita"
    "PS3" -> "ps3"
    "PS4" -> "ps4"
    "PS5" -> "ps5"
    else -> this
}

private fun String.toPlatformLabel(): String = when (this) {
    "psp" -> "PSP"
    "ps_vita" -> "PS Vita"
    "ps3" -> "PS3"
    "ps4" -> "PS4"
    "ps5" -> "PS5"
    else -> this
}
