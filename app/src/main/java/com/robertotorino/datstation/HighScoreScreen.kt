package com.robertotorino.datstation

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import com.robertotorino.datstation.ui.AppCornerRadius
import java.text.SimpleDateFormat

// ==========================================
// MODULE 1: DATA MODELS
// ==========================================

data class ScoreEntry(
    val id: Int,
    val tableName: String,
    val score: Int,
    val timestamp: String
)

data class GameSummary(
    val gameName: String,
    val topScore: Int,
    val maxStage: Int = 0
)

data class TrophySummary(
    val gameName: String,
    val trophyType: String,
    val count: Int,
    val iconRes: Int
)

// ==========================================
// MODULE 2: DATABASE LOGIC
// ==========================================

fun fetchTopScores(db: SupportSQLiteDatabase?): List<ScoreEntry> {
    val topScores = mutableListOf<ScoreEntry>()
    if (db == null) return topScores
    try {
        val cursor = db.query(SimpleSQLiteQuery("SELECT id, tableName, score, timestamp FROM user_db.scores ORDER BY score DESC LIMIT 10"))
        val idIdx = cursor.getColumnIndex("id")
        val tableIdx = cursor.getColumnIndex("tableName")
        val scoreIdx = cursor.getColumnIndex("score")
        val timeIdx = cursor.getColumnIndex("timestamp")

        if (cursor.moveToFirst()) {
            do {
                topScores.add(ScoreEntry(
                    id = if (idIdx != -1) cursor.getInt(idIdx) else 0,
                    tableName = (if (tableIdx != -1) cursor.getString(tableIdx) else "Unknown") ?: "Unknown",
                    score = if (scoreIdx != -1) cursor.getInt(scoreIdx) else 0,
                    timestamp = (if (timeIdx != -1) cursor.getString(timeIdx) else "") ?: ""
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
    } catch (e: Exception) { Log.e("HighScores", "Error fetching scores", e) }
    return topScores
}

fun fetchGameSummaries(db: SupportSQLiteDatabase?): List<GameSummary> {
    val summaries = mutableListOf<GameSummary>()
    if (db == null) return summaries
    
    // 1. Neon Disc
    try {
        val cursor = db.query(SimpleSQLiteQuery("SELECT MAX(score), MAX(CAST(REPLACE(tableName, 'NeonDisc_Lvl_', '') AS INTEGER)) FROM user_db.scores WHERE tableName LIKE 'NeonDisc_Lvl_%'"))
        if (cursor.moveToFirst()) {
            summaries.add(GameSummary("Neon Disc", cursor.getInt(0), cursor.getInt(1)))
        }
        cursor.close()
    } catch (_: Exception) { }

    // 2. Neon Racer
    try {
        val cursor = db.query(SimpleSQLiteQuery("SELECT MAX(score), MAX(CAST(REPLACE(tableName, 'NeonRacer_Lvl_', '') AS INTEGER)) FROM user_db.scores WHERE tableName LIKE 'NeonRacer_Lvl_%'"))
        if (cursor.moveToFirst()) {
            summaries.add(GameSummary("Neon Racer", cursor.getInt(0), cursor.getInt(1)))
        }
        cursor.close()
    } catch (_: Exception) { }

    // 3. Quiz
    try {
        val cursor = db.query(SimpleSQLiteQuery("SELECT MAX(score) FROM user_db.scores WHERE tableName = 'Quiz'"))
        if (cursor.moveToFirst()) {
            summaries.add(GameSummary("PlayStation Quiz", cursor.getInt(0), 0))
        }
        cursor.close()
    } catch (_: Exception) { }

    return summaries
}

fun fetchTrophySummaries(db: SupportSQLiteDatabase?): List<TrophySummary> {
    val results = mutableListOf<TrophySummary>()
    if (db == null) return results
    
    // Pre-defined list of all possible trophies
    val trophyDefinitions = listOf(
        TrophyDef("NeonDisc", "Blue", R.drawable.neon_disc_blue),
        TrophyDef("NeonDisc", "Bronze", R.drawable.neon_disc_bronze),
        TrophyDef("NeonDisc", "Gold", R.drawable.neon_disc_gold),
        TrophyDef("NeonDisc", "Green", R.drawable.neon_disc_green),
        TrophyDef("NeonDisc", "Silver", R.drawable.neon_disc_silver),
        TrophyDef("NeonDisc", "Red", R.drawable.neon_disc_red),
        TrophyDef("NeonRacer", "Bronze", R.drawable.neon_racer_bronze),
        TrophyDef("NeonRacer", "Silver", R.drawable.neon_racer_silver),
        TrophyDef("NeonRacer", "Gold", R.drawable.neon_racer_gold),
        TrophyDef("Quiz", "Perfect", R.drawable.diamond_status) // Placeholder or specific quiz icon
    )

    for (def in trophyDefinitions) {
        try {
            val cursor = db.query(SimpleSQLiteQuery("SELECT COUNT(*) FROM user_db.trophies WHERE gameName = '${def.game}' AND trophyType = '${def.type}'"))
            if (cursor.moveToFirst()) {
                val count = cursor.getInt(0)
                results.add(TrophySummary(def.game, def.type, count, def.icon))
            }
            cursor.close()
        } catch (e: Exception) { }
    }
    return results
}

data class TrophyDef(val game: String, val type: String, val icon: Int)

// ==========================================
// MODULE 3: HALL OF FAME UI
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighScoresScreen(
    db: SupportSQLiteDatabase?,
    onBackClicked: () -> Unit
) {
    var scores by remember { mutableStateOf<List<ScoreEntry>>(emptyList()) }
    var summaries by remember { mutableStateOf<List<GameSummary>>(emptyList()) }
    var trophies by remember { mutableStateOf<List<TrophySummary>>(emptyList()) }
    LaunchedEffect(db) {
        withContext(Dispatchers.IO) {
            scores = fetchTopScores(db)
            summaries = fetchGameSummaries(db)
            trophies = fetchTrophySummaries(db)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hall of Fame", color = Color(0xFF00FFFF), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // --- TOP SCORES SECTION ---
            item {
                Text(
                    "Global Top Scores",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
                summaries.forEach { summary ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(summary.gameName, color = Color(0xFF00CCFF), fontWeight = FontWeight.Bold)
                        val extraInfo = if (summary.maxStage > 0) " (Max Lvl: ${summary.maxStage})" else ""
                        Text("Top Score: ${summary.topScore}$extraInfo", color = Color.Green, fontWeight = FontWeight.ExtraBold)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.DarkGray)
            }

            // --- TROPHY GALLERY SECTION ---
            item {
                Text(
                    "TROPHY GALLERY",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            item {
                // Show trophies in a grid
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    trophies.chunked(3).forEach { rowTrophies ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowTrophies.forEach { trophy ->
                                TrophyItem(trophy)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = Color.DarkGray)
            }

            // --- RECENT TOP 10 LIST ---
            item {
                Text(
                    "LATEST RECORD BREAKERS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            if (scores.isEmpty()) {
                item {
                    Text("NO RECORDS YET", color = Color.Gray, modifier = Modifier.padding(32.dp))
                }
            } else {
                itemsIndexed(scores) { index, scoreEntry ->
                    ScoreCard(rank = index + 1, entry = scoreEntry)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun TrophyItem(trophy: TrophySummary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(AppCornerRadius))
                .border(1.dp, if (trophy.count > 0) Color.Yellow else Color.DarkGray, RoundedCornerShape(AppCornerRadius)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = trophy.iconRes),
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                alpha = if (trophy.count > 0) 1.0f else 0.1f
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "x${trophy.count}",
            color = if (trophy.count > 0) Color.Yellow else Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = trophy.trophyType,
            color = Color.White,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ScoreCard(rank: Int, entry: ScoreEntry) {
    val locale = LocalLocale.current.platformLocale
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color(0xFF3333FF)
    }

    val formattedDate = try {
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale)
        val formatter = SimpleDateFormat("MMM dd, yyyy", locale)
        parser.parse(entry.timestamp)?.let { formatter.format(it) } ?: entry.timestamp
    } catch (_: Exception) { entry.timestamp }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppCornerRadius),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "#$rank", color = rankColor, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(48.dp))
                Column {
                    Text(text = entry.tableName.replace("_", " ").uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = formattedDate, color = Color.Gray, fontSize = 10.sp)
                }
            }
            Text(
                text = entry.score.toString().padStart(6, '0'),
                color = Color(0xFF00FF00),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
