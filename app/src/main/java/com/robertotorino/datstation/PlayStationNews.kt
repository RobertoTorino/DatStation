package com.robertotorino.datstation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

private const val PLAYSTATION_BLOG_FEED =
    "https://blog.playstation.com/feed/"
private const val PLAYSTATION_UPCOMING_GAMES =
    "https://www.playstation.com/en-us/ps5/games/"
private const val PLAYSTATION_NEW_RELEASES =
    "https://store.playstation.com/en-us/category/024029c7-f61b-4bef-a4d7-06270ed12b56/1"

private val ALLOWED_HOSTS = setOf(
    "blog.playstation.com",
    "www.playstation.com",
    "playstation.com",
    "store.playstation.com"
)

data class NewsItem(
    val title: String,
    val link: String,
    val pubDateRaw: String
)

sealed interface PlayStationUiState {
    data object Loading : PlayStationUiState
    data class Success(val items: List<NewsItem>) : PlayStationUiState
    data class Error(val message: String) : PlayStationUiState
}

/**
 * Optional button to place anywhere in your app.
 * Example:
 * PlayStationNewsButton { navController.navigate("PlayStation_updates") }
 */
//@Composable
//fun PlayStationNewsButton(
//    onClick: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Button(
//        onClick = onClick,
//        modifier = modifier.fillMaxWidth()
//    ) {
//        Text("PlayStation Updates")
//    }
//}

/**
 * The screen you navigate to from your button.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlayStationUpdatesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    var uiState by remember { mutableStateOf<PlayStationUiState>(PlayStationUiState.Loading) }

    fun openAllowedUrl(url: String) {
        val uri = runCatching { url.toUri() }.getOrNull()
        val scheme = uri?.scheme?.lowercase(Locale.US)
        val host = uri?.host?.lowercase(Locale.US)

        if (scheme != "https" || host !in ALLOWED_HOSTS) {
            scope.launch {
                snackBarHostState.showSnackbar("Blocked unexpected link.")
            }
            return
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (_: ActivityNotFoundException) {
            scope.launch {
                snackBarHostState.showSnackbar("No browser available to open this link.")
            }
        }
    }

    suspend fun loadNews() {
        uiState = PlayStationUiState.Loading
        uiState = try {
            val items = fetchPlayStationBlogFeed(PLAYSTATION_BLOG_FEED)
            PlayStationUiState.Success(items)
        } catch (t: Throwable) {
            PlayStationUiState.Error(t.message ?: "Failed to load PlayStation updates.")
        }
    }

    LaunchedEffect(Unit) {
        loadNews()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("PlayStation Updates") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Announcements, release-date news, and new PlayStation releases",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { openAllowedUrl(PLAYSTATION_UPCOMING_GAMES) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                    ) {
                        Text("Upcoming Games", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { openAllowedUrl(PLAYSTATION_NEW_RELEASES) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                    ) {
                        Text("New Releases", color = Color.White)
                    }
                }
            }

            item {
                HorizontalDivider()
            }

            item {
                Text(
                    text = "Latest Announcements",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            when (val state = uiState) {
                is PlayStationUiState.Loading -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading official PlayStation blog updates...")
                        }
                    }
                }

                is PlayStationUiState.Error -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF011641),
                                contentColor = Color.White
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Could not load announcements",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(state.message)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        scope.launch { loadNews() }
                                    }
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                is PlayStationUiState.Success -> {
                    if (state.items.isEmpty()) {
                        item {
                            Text("No announcements found.")
                        }
                    } else {
                        items(
                            items = state.items,
                            key = { it.link }
                        ) { item ->
                            NewsCard(
                                item = item,
                                onClick = { openAllowedUrl(item.link) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsCard(
    item: NewsItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF011641),
            contentColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatPubDate(item.pubDateRaw),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.link,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private suspend fun fetchPlayStationBlogFeed(feedUrl: String): List<NewsItem> =
    withContext(Dispatchers.IO) {
        val url = URL(feedUrl)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml")
            setRequestProperty("User-Agent", "PlayStationUpdatesApp/1.0")
        }

        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code while loading PlayStation feed.")
            }

            connection.inputStream.use { input ->
                parseRss(input).take(20)
            }
        } finally {
            connection.disconnect()
        }
    }

private fun parseRss(inputStream: InputStream): List<NewsItem> {
    val items = mutableListOf<NewsItem>()

    val factory = XmlPullParserFactory.newInstance().apply {
        isNamespaceAware = false
    }

    val parser = factory.newPullParser().apply {
        setInput(inputStream, null)
    }

    var eventType = parser.eventType
    var inItem = false

    var title = ""
    var link = ""
    var pubDate = ""

    while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
            XmlPullParser.START_TAG -> {
                when (parser.name.lowercase(Locale.US)) {
                    "item" -> {
                        inItem = true
                        title = ""
                        link = ""
                        pubDate = ""
                    }

                    "title" -> if (inItem) title = cleanText(parser.nextText())
                    "link" -> if (inItem) link = cleanText(parser.nextText())
                    "pubdate" -> if (inItem) pubDate = cleanText(parser.nextText())
                }
            }

            XmlPullParser.END_TAG -> {
                if (parser.name.equals("item", ignoreCase = true) && inItem) {
                    if (title.isNotBlank() && link.isNotBlank()) {
                        items += NewsItem(
                            title = title,
                            link = link,
                            pubDateRaw = pubDate
                        )
                    }
                    inItem = false
                }
            }
        }
        eventType = parser.next()
    }

    return items
}

private fun cleanText(value: String): String {
    return HtmlCompat.fromHtml(value, HtmlCompat.FROM_HTML_MODE_LEGACY)
        .toString()
        .trim()
}

private fun formatPubDate(raw: String): String {
    if (raw.isBlank()) return ""

    val parser = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
    val date = runCatching { parser.parse(raw) }.getOrNull() ?: return raw

    val relative = DateUtils.getRelativeTimeSpanString(
        date.time,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()

    val display = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
    return "$display • $relative"
}