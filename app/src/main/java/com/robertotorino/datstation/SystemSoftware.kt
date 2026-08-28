package com.robertotorino.datstation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.robertotorino.datstation.ui.AppCornerRadius
import com.robertotorino.datstation.ui.LocalAppColors
import org.json.JSONArray
import org.json.JSONObject

data class SoftwareSeries(
    val id: String,
    val name: String,
    val entries: List<SoftwareVersionItem>
)

data class SoftwareVersionItem(
    val version: String,
    val releaseDate: String,
    val size: String?,
    val model: String? = null,
    val region: String? = null,
    val regionCode: String? = null,
    val color: String? = null,
    val notes: List<String>,
    val regionFree: String? = null,
    val cdrCopies: String? = null,
    val avDirectOut: String? = null,
    val parallelPort: String? = null,
    val serialPort: String? = null
)

class SystemSoftwareActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ASSET_NAME = "extra_asset_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "System Software"
        val assetName = intent.getStringExtra(EXTRA_ASSET_NAME)

        val seriesList = if (assetName != null) {
            loadSoftwareVersions(assetName)
        } else {
            emptyList()
        }

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
                        SystemSoftwareDetailScreen(
                            title = title,
                            seriesList = seriesList,
                            onBack = { finish() }
                        )
                    }
                }
            }
        }
    }

    private fun loadSoftwareVersions(assetName: String): List<SoftwareSeries> {
        val jsonText = try {
            assets.open(assetName)
                .bufferedReader()
                .use { it.readText() }
        } catch (_: Exception) {
            return emptyList()
        }

        try {
            val root = JSONObject(jsonText)
            if (root.has("series")) {
                val seriesArray = root.getJSONArray("series")
                return parseSeriesArray(seriesArray)
            }
        } catch (_: Exception) {
            // Not a series-based root object
        }

        return try {
            val jsonArray = JSONArray(jsonText)
            if (jsonArray.length() > 0) {
                val firstObj = jsonArray.optJSONObject(0)
                if (firstObj != null && firstObj.has("entries")) {
                    // It's an array of series
                    return parseSeriesArray(jsonArray)
                }
            }
            // It's an array of software items
            val entries = parseEntries(jsonArray)
            listOf(SoftwareSeries("default", "", entries))
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseSeriesArray(seriesArray: JSONArray): List<SoftwareSeries> {
        val result = mutableListOf<SoftwareSeries>()
        for (i in 0 until seriesArray.length()) {
            val sObj = seriesArray.getJSONObject(i)
            val entries = parseEntries(sObj.getJSONArray("entries"))
            result.add(
                SoftwareSeries(
                    id = sObj.optString("id"),
                    name = sObj.optString("name"),
                    entries = entries
                )
            )
        }
        return result
    }

    private fun parseEntries(jsonArray: JSONArray): List<SoftwareVersionItem> {
        val result = mutableListOf<SoftwareVersionItem>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            val notes = mutableListOf<String>()
            when (val notesValue = obj.opt("notes")) {
                is JSONArray -> {
                    for (j in 0 until notesValue.length()) {
                        notes.add(notesValue.getString(j))
                    }
                }

                is String -> {
                    if (notesValue.isNotBlank()) {
                        notes.add(notesValue)
                    }
                }
            }

            result.add(
                SoftwareVersionItem(
                    version = obj.optString("version"),
                    releaseDate = obj.optString("releaseDate")
                        .takeIf { it.isNotBlank() } ?: obj.optString("release_date"),
                    size = obj.optString("size").takeIf { it.isNotBlank() },
                    model = obj.optString("model").takeIf { it.isNotBlank() },
                    region = obj.optString("region").takeIf { it.isNotBlank() },
                    regionCode = obj.optString("region_code").takeIf { it.isNotBlank() },
                    color = obj.optString("color").takeIf { it.isNotBlank() },
                    notes = notes,
                    regionFree = obj.optString("region_free").takeIf { it.isNotBlank() },
                    cdrCopies = obj.optString("cdr_copies").takeIf { it.isNotBlank() },
                    avDirectOut = obj.optString("av_direct_out").takeIf { it.isNotBlank() },
                    parallelPort = obj.optString("parallel_port").takeIf { it.isNotBlank() },
                    serialPort = obj.optString("serial_port").takeIf { it.isNotBlank() }
                )
            )
        }
        return result
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemSoftwareDetailScreen(
    title: String,
    seriesList: List<SoftwareSeries>,
    onBack: () -> Unit
) {
    val currentColors = LocalAppColors.current

    Scaffold(
        containerColor = currentColors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
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
        if (seriesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No system software data available.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = currentColors.textPrimary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                seriesList.forEach { series ->
                    if (series.name.isNotBlank()) {
                        item {
                            Text(
                                text = series.name.replace("_", " "),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = currentColors.accent,
                                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                            )
                        }
                    }
                    items(series.entries) { item ->
                        SoftwareVersionCard(item)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SoftwareVersionCard(item: SoftwareVersionItem) {
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
                text = item.version,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = currentColors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Release Date: ${item.releaseDate}",
                style = MaterialTheme.typography.bodyMedium,
                color = currentColors.accent,
                fontWeight = FontWeight.Bold
            )

            if (!item.region.isNullOrBlank()) {
                val regionText = buildString {
                    append("Region: ")
                    append(item.region)
                    if (!item.regionCode.isNullOrBlank()) {
                        append(" [")
                        append(item.regionCode)
                        append("]")
                    }
                    if (!item.model.isNullOrBlank()) {
                        append(" (")
                        append(item.model)
                        append(")")
                    }
                }
                Text(
                    text = regionText,
                    style = MaterialTheme.typography.bodySmall,
                    color = currentColors.textPrimary
                )
            }

            if (!item.color.isNullOrBlank()) {
                Text(
                    text = "Color: ${item.color}",
                    style = MaterialTheme.typography.bodySmall,
                    color = currentColors.textSecondary
                )
            }

            if (!item.size.isNullOrBlank()) {
                Text(
                    text = "Size: ${item.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = currentColors.textSecondary
                )
            }

            // Hardware Specs
            val specs = mutableListOf<String>()
            if (!item.regionFree.isNullOrBlank()) specs.add("Region Free: ${item.regionFree}")
            if (!item.cdrCopies.isNullOrBlank()) specs.add("CD-R Copies: ${item.cdrCopies}")
            if (!item.avDirectOut.isNullOrBlank()) specs.add("A/V Direct Out: ${item.avDirectOut}")
            if (!item.parallelPort.isNullOrBlank()) specs.add("Parallel Port: ${item.parallelPort}")
            if (!item.serialPort.isNullOrBlank()) specs.add("Serial Port: ${item.serialPort}")

            if (specs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                specs.forEach { spec ->
                    Text(
                        text = spec,
                        style = MaterialTheme.typography.bodySmall,
                        color = currentColors.textSecondary
                    )
                }
            }

            if (item.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                item.notes.forEach { note ->
                    Text(
                        text = "• $note",
                        style = MaterialTheme.typography.bodySmall,
                        color = currentColors.textPrimary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
