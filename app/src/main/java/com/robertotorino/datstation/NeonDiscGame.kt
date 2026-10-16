package com.robertotorino.datstation

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.robertotorino.datstation.ui.AppCornerRadius
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

// ==========================================
// MODULE 1: DATA MODELS
// ==========================================
enum class Faction { GOOD, ENEMY }

enum class ConsoleDisc(val faction: Faction, val color: Color) {
    PS1(Faction.GOOD, Color(0xFF00FFFF)),
    PS2(Faction.GOOD, Color(0xFFFF00FF)),
    PS3(Faction.GOOD, Color(0xFFFF3333)),
    PS4(Faction.GOOD, Color(0xFF3333FF)),
    PS5(Faction.GOOD, Color(0xFFEEEEEE)),
    PS6(Faction.GOOD, Color(0xFFFF9900)),
    GAMECUBE(Faction.ENEMY, Color(0xFFCCFF00)),
    WII(Faction.ENEMY, Color(0xFFFF66B2)),
    SWITCH(Faction.ENEMY, Color(0xFFFF0000)),
    XBOX(Faction.ENEMY, Color(0xFF00FF00))
}

data class ActiveDisc(var x: Int, var y: Int, val console: ConsoleDisc)

// ==========================================
// MODULE 2: THEME & RULE ENGINE
// ==========================================
enum class StageTheme { NEON_DISCS, PACKAGES, CARTRIDGES, BOX_FILL, BLACK_AND_WHITE }

fun getStageTheme(stage: Int): StageTheme {
    return when (stage) {
        1 -> StageTheme.NEON_DISCS
        2 -> StageTheme.PACKAGES
        3 -> StageTheme.CARTRIDGES
        4 -> StageTheme.BOX_FILL
        else -> StageTheme.BLACK_AND_WHITE
    }
}

fun getPlayfieldColor(theme: StageTheme): Color {
    return when (theme) {
        StageTheme.NEON_DISCS -> Color.Black
        StageTheme.PACKAGES -> Color(0xFF001122)
        StageTheme.CARTRIDGES -> Color(0xFF112200)
        StageTheme.BOX_FILL -> Color(0xFF220000)
        StageTheme.BLACK_AND_WHITE -> Color.Black
    }
}

// ==========================================
// MODULE 3: GAME ENGINE & UI
// ==========================================
fun getNeonTrophy(wonLevel: Int): Int? {
    return when (wonLevel) {
        1 -> R.drawable.neon_disc_blue
        2 -> R.drawable.neon_disc_bronze
        3 -> R.drawable.neon_disc_gold
        4 -> R.drawable.neon_disc_green
        5 -> R.drawable.neon_disc_silver
        7 -> R.drawable.neon_disc_red
        else -> null
    }
}

@Composable
fun NeonDiscGame(db: androidx.sqlite.db.SupportSQLiteDatabase?, onExit: () -> Unit) {
    val columns = 8
    val rows = 12
    val context = LocalContext.current
    val resources = LocalResources.current
    val buttonShape = RoundedCornerShape(AppCornerRadius)

    // Core Game States
    var grid by remember { mutableStateOf(List(rows) { MutableList<ConsoleDisc?>(columns) { null } }) }
    var scoredGrid by remember { mutableStateOf(List(rows) { MutableList(columns) { false } }) }

    var level by remember { mutableIntStateOf(1) }
    var enemiesShot by remember { mutableIntStateOf(0) }

    // Derived States
    val stage = ((level - 1) / 3) + 1
    val isEndLevel = (level % 3 == 0)
    val currentTheme = getStageTheme(stage)
    val playfieldColor = getPlayfieldColor(currentTheme)

    var activeDisc by remember { mutableStateOf(spawnDisc(columns, isEndLevel)) }
    var isGameOver by remember { mutableStateOf(false) }
    var gameOverReason by remember { mutableStateOf<String?>(null) }
    var isLevelWon by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(45) }
    var isMuted by remember { mutableStateOf(true) }

    var inGameNotification by remember { mutableStateOf<String?>(null) }
    var wonLevels by remember { mutableStateOf(emptySet<Int>()) }

    // UI Dialog & God Mode States
    var showLargeTrophy by remember { mutableStateOf(false) }
    var showTrophyGallery by remember { mutableStateOf(false) }
    var isGodMode by remember { mutableStateOf(false) }

    val formattedStage = stage.toString().padStart(2, '0')
    val formattedScore = score.toString().padStart(6, '0')
    val formattedTime = "00:${timeLeft.toString().padStart(2, '0')}"

    // ==========================================
    // ZERO-LATENCY SOUND-POOL ENGINE
    // ==========================================
    val soundPool = remember {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
    }
    val soundMap = remember { mutableMapOf<Int, Int>() }

    val bgmPlayer = remember {
        try {
            MediaPlayer.create(context, R.raw.neon_disc_bgm)
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        soundMap[R.raw.blob] = soundPool.load(context, R.raw.blob, 1)
        soundMap[R.raw.enemy_fall] = soundPool.load(context, R.raw.enemy_fall, 1)
        soundMap[R.raw.game_over] = soundPool.load(context, R.raw.game_over, 1)
        soundMap[R.raw.game_over_voice] = soundPool.load(context, R.raw.game_over_voice, 1)
        soundMap[R.raw.neon_disc_bgm] = soundPool.load(context, R.raw.neon_disc_bgm, 1)
        soundMap[R.raw.sea_breeze] = soundPool.load(context, R.raw.sea_breeze, 1)
        soundMap[R.raw.shoot_laser] = soundPool.load(context, R.raw.shoot_laser, 1)

        bgmPlayer?.isLooping = true

        onDispose {
            soundPool.release()
            try {
                 if (bgmPlayer?.isPlaying == true) bgmPlayer.stop()
                 bgmPlayer?.release()
            } catch (_: Exception) {
            }
        }
    }

    val playSfx = { resId: Int ->
        if (!isMuted) {
            soundMap[resId]?.let { soundId ->
                soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
            }
        }
    }

    LaunchedEffect(isGameOver, isLevelWon, isPaused, isMuted) {
        try {
            if (isGameOver || isLevelWon || isPaused || isMuted) {
                if (bgmPlayer?.isPlaying == true) bgmPlayer.pause()
            } else {
                if (bgmPlayer?.isPlaying == false) bgmPlayer.start()
            }
        } catch (_: Exception) {
        }
    }

    // Helper function to cleanly reset the board
    val resetBoard = {
        grid = List(rows) { MutableList(columns) { null } }
        scoredGrid = List(rows) { MutableList(columns) { false } }
        isGameOver = false
        isLevelWon = false
        isPaused = false
        timeLeft = 45
        enemiesShot = 0
        gameOverReason = null
        inGameNotification = null
        activeDisc = spawnDisc(columns, isEndLevel)
    }

    LaunchedEffect(inGameNotification) {
        if (inGameNotification != null) {
            delay(2000L)
            inGameNotification = null
        }
    }

    LaunchedEffect(isGameOver) {
        if (isGameOver) {
            playSfx(R.raw.game_over)
            playSfx(R.raw.game_over_voice)
            saveNeonDiscResults(db, level, score, false)
        }
    }

    LaunchedEffect(isGameOver, isLevelWon, isPaused, level) {
        while (!isGameOver && !isLevelWon && !isPaused && timeLeft > 0) {
            delay(1000L)
            timeLeft--
            if (timeLeft <= 0) {
                isGameOver = true
                gameOverReason = resources.getString(R.string.times_up)
            }
        }
    }

    // The Game Loop (Movement)
    LaunchedEffect(isGameOver, isLevelWon, level, isPaused) {
        while (!isGameOver && !isLevelWon && !isPaused) {
            val baseSpeed = maxOf(150L, 500L - (level * 20L))

            val currentSpeed =
                if ((isEndLevel || currentTheme == StageTheme.BOX_FILL) && activeDisc.console.faction == Faction.ENEMY) {
                    (baseSpeed * 0.6).toLong()
                } else {
                    baseSpeed
                }

            delay(currentSpeed)

            if (activeDisc.y + 1 < rows && grid[activeDisc.y + 1][activeDisc.x] == null) {
                activeDisc = activeDisc.copy(y = activeDisc.y + 1)

                if (activeDisc.y + 1 == rows || grid[activeDisc.y + 1][activeDisc.x] != null) {
                    if (activeDisc.console.faction == Faction.ENEMY) playSfx(R.raw.enemy_fall)
                    else playSfx(R.raw.blob)
                }

            } else {
                // Lock piece
                val newGrid = grid.map { it.toMutableList() }.toMutableList()
                newGrid[activeDisc.y][activeDisc.x] = activeDisc.console
                grid = newGrid
                score += 10

                // Calculate Time Bonuses (NOW APPLIES TO BOSS LEVELS TOO)
                val bonusResult = calculateTimeBonus(grid, scoredGrid)
                if (bonusResult.bonusSeconds > 0) {
                    timeLeft += bonusResult.bonusSeconds
                    scoredGrid = bonusResult.newScoredGrid
                    Toast.makeText(
                        context,
                        "+${bonusResult.bonusSeconds} SECONDS!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Check Win/Loss
                if (checkLossCondition(grid, currentTheme, isGodMode)) {
                    isGameOver = true
                    gameOverReason = if (currentTheme == StageTheme.BOX_FILL)
                        resources.getString(R.string.enemy_in_box) else resources.getString(R.string.enemies_connected)
                } else if (!isEndLevel && checkWinCondition(grid, columns, rows, currentTheme)) {
                    isLevelWon = true
                    wonLevels = wonLevels + level
                    saveNeonDiscResults(db, level, score, true)
                } else {
                    activeDisc = spawnDisc(columns, isEndLevel)

                    if (grid[activeDisc.y][activeDisc.x] != null || activeDisc.y + 1 == rows || grid[activeDisc.y + 1][activeDisc.x] != null) {
                        if (activeDisc.console.faction == Faction.ENEMY) playSfx(R.raw.enemy_fall)
                        else playSfx(R.raw.blob)
                    }

                    if (grid[activeDisc.y][activeDisc.x] != null) {
                        if (currentTheme == StageTheme.BOX_FILL) {
                            isLevelWon = true
                            wonLevels = wonLevels + level
                            score += 1000
                            saveNeonDiscResults(db, level, score, true)
                        } else {
                            isGameOver = true
                            gameOverReason = resources.getString(R.string.board_full)
                        }
                    }
                }
            }
        }
    }

    // --- OVERLAYS / DIALOGS ---

    if (isGameOver) {
        Dialog(
            onDismissRequest = { /* Forced to use restart button */ },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(Color(0xEE1A1A1A), RoundedCornerShape(AppCornerRadius))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.game_over),
                    color = Color.Red,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(16.dp))

                gameOverReason?.let {
                    Text(
                        it,
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // NEW: Icons only for Restart and Exit with half width
                Row(
                    modifier = Modifier.fillMaxWidth(0.6f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { level = 1; score = 0; resetBoard() },
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Restart",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Button(
                        onClick = { onExit() },
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Exit",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

    if (showLargeTrophy) {
        Dialog(onDismissRequest = { showLargeTrophy = false }) {
            val latestLevel = wonLevels.maxOrNull() ?: 0
            val resId = getNeonTrophy(latestLevel)
            
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(Color(0xFF222222), RoundedCornerShape(AppCornerRadius))
                    .clickable { showTrophyGallery = true },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (resId != null) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = "Large Trophy",
                            modifier = Modifier.size(140.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "No Trophy",
                            tint = Color.Gray,
                            modifier = Modifier.size(100.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (resId != null) "Level $latestLevel Trophy" else "No Trophies Yet",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text("(Tap to view Gallery)", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }

    if (showTrophyGallery) {
        Dialog(onDismissRequest = { showTrophyGallery = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(AppCornerRadius))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.trophy_gallery),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                val trophyLevels = listOf(1, 2, 3, 4, 5, 7)
                
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    trophyLevels.chunked(3).forEach { chunk ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            chunk.forEach { lvl ->
                                val resId = getNeonTrophy(lvl)
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Color.DarkGray, RoundedCornerShape(AppCornerRadius)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (wonLevels.contains(lvl) && resId != null) {
                                        Image(
                                            painter = painterResource(id = resId),
                                            contentDescription = "Trophy Lvl $lvl",
                                            modifier = Modifier.size(48.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = "Locked",
                                            tint = Color.Black.copy(alpha = 0.3f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showTrophyGallery = false },
                    shape = buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC))
                ) { Text("Close", color = Color.White) }
            }
        }
    }

    // --- MAIN UI WRAPPED IN A BOX ---
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF121212))
        .systemBarsPadding()) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(42.dp))

            Text(
                text = when {
                    isLevelWon -> stringResource(R.string.level_cleared)
                    isGameOver -> stringResource(R.string.game_over)
                    isPaused -> stringResource(R.string.paused)
                    currentTheme == StageTheme.BOX_FILL -> "BOSS: Fill the box in 45s!\nNO Enemies allowed in box!"
                    isEndLevel -> "BOSS: Shoot 5 Enemies ($enemiesShot/5)\nDo NOT shoot friends!"
                    currentTheme == StageTheme.PACKAGES -> "Goal: Perfect Row AND Stack of Packages\nAvoid: 2 Enemies touching"
                    else -> "Goal: Full Row AND Stack of Friends\nAvoid: 2 Enemies touching"
                },
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Time: $formattedTime",
                color = if (timeLeft <= 10) Color.Red else Color.Cyan,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .width(340.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // FIXED: Explicitly replacing Lvl/Scr with Level/Score
                Text(
                    text = "Stage $formattedStage - Level: $level - Score: $formattedScore",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF333333), RoundedCornerShape(AppCornerRadius))
                        .clickable { showLargeTrophy = true },
                    contentAlignment = Alignment.Center
                ) {
                    val latestTrophyLvl = listOf(1, 2, 3, 4, 5, 7).filter { wonLevels.contains(it) }.maxOrNull()
                    val resId = if (latestTrophyLvl != null) getNeonTrophy(latestTrophyLvl) else null
                    
                    if (resId != null) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = "Latest Trophy",
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "No Trophy",
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .width(340.dp)
                    .height(430.dp)
                    .background(playfieldColor)
                    .border(BorderStroke(2.dp, Color(0xFF0066CC))) // Button Blue Border
                    .padding(4.dp)
                    .clipToBounds()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { if (!isGameOver && !isLevelWon) isPaused = !isPaused }
            ) {
                val cellWidth = size.width / columns
                val cellHeight = size.height / rows
                val itemSize = min(cellWidth, cellHeight) / 2.2f

                if (currentTheme == StageTheme.BOX_FILL) drawRect(
                    color = Color.Red,
                    style = Stroke(width = 8f)
                )

                for (y in 0 until rows) {
                    for (x in 0 until columns) {
                        val disc = grid[y][x]
                        if (disc != null) {
                            val center = Offset(
                                x * cellWidth + cellWidth / 2,
                                y * cellHeight + cellHeight / 2
                            )
                            drawThemedItem(center, itemSize, disc, currentTheme)
                        }
                    }
                }

                if (!isGameOver && !isLevelWon) {
                    val center = Offset(
                        activeDisc.x * cellWidth + cellWidth / 2,
                        activeDisc.y * cellHeight + cellHeight / 2
                    )
                    drawThemedItem(center, itemSize, activeDisc.console, currentTheme)
                }
            }

            // --- NOTIFICATION SPACE ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = inGameNotification != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    inGameNotification?.let { message ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF0066CC).copy(alpha = 0.85f), RoundedCornerShape(AppCornerRadius))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = message,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Controls
            if (isLevelWon) {
                Button(
                    onClick = { level++; resetBoard() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp),
                    shape = buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CC66))
                ) {
                    Text(
                        "NEXT LEVEL",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT (Arrow)
                    Button(
                        onClick = {
                            if (!isPaused && activeDisc.x > 0 && grid[activeDisc.y][activeDisc.x - 1] == null) activeDisc =
                                activeDisc.copy(x = activeDisc.x - 1)
                        },
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Left",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // DROP (Arrow Down)
                    Button(
                        onClick = {
                            if (!isPaused) {
                                var dropY = activeDisc.y
                                while (dropY + 1 < rows && grid[dropY + 1][activeDisc.x] == null) dropY++
                                activeDisc = activeDisc.copy(y = dropY)

                                if (activeDisc.console.faction == Faction.ENEMY) playSfx(R.raw.enemy_fall)
                                else playSfx(R.raw.blob)
                            }
                        },
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Drop",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // RIGHT (Arrow)
                    Button(
                        onClick = {
                            if (!isPaused && activeDisc.x < columns - 1 && grid[activeDisc.y][activeDisc.x + 1] == null) activeDisc =
                                activeDisc.copy(x = activeDisc.x + 1)
                        },
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Right",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // SHOOT (Water Pistol)
                    Button(
                        onClick = {
                            if (!isPaused && !isGameOver) {
                                if (level >= 2 || isGodMode) {
                                    playSfx(R.raw.shoot_laser)

                                    if (activeDisc.console.faction == Faction.GOOD) {
                                        isGameOver = true
                                        gameOverReason = resources.getString(R.string.shot_friendly)
                                    } else {
                                        if (isEndLevel) {
                                            timeLeft += 5
                                            enemiesShot++
                                            inGameNotification =
                                                "+5 SECONDS! ($enemiesShot/5 Defeated)"

                                            if (enemiesShot >= 5) {
                                                isLevelWon = true
                                                wonLevels = wonLevels + level
                                                score += 500
                                                saveNeonDiscResults(db, level, score, true)
                                            } else {
                                                activeDisc = spawnDisc(columns, true)
                                            }
                                        } else {
                                            activeDisc = spawnDisc(columns, false)
                                        }
                                    }
                                } else {
                                    inGameNotification =
                                        resources.getString(R.string.shoot_unlock_msg)
                                }
                            }
                        },
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("🔫", fontSize = 24.sp) }

                    // RESTART (Recycle Icon)
                    Button(
                        onClick = { level = 1; score = 0; resetBoard() },
                        shape = buttonShape,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Restart",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Tools Row (Sound + Wrench + God Mode block)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // MUTE
                Button(
                    onClick = { isMuted = !isMuted },
                    shape = buttonShape,
                    modifier = Modifier.size(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMuted) Color(
                            0xFFCC0000
                        ) else Color(0xFF0066CC)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Mute",
                        tint = Color.White
                    )
                }

                // GOD MODE WRENCH
                Button(
                    onClick = { isGodMode = !isGodMode },
                    shape = buttonShape,
                    modifier = Modifier.size(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGodMode) Color(
                            0xFF9900FF
                        ) else Color.DarkGray
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) { Icon(Icons.Filled.Build, contentDescription = "God Mode", tint = Color.White) }

                // Middle area (God Mode Selection)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isGodMode) {
                        Card(
                            shape = buttonShape,
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF330066)),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .fillMaxHeight()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "LVL:",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Button(
                                        onClick = { level = max(1, level - 1); resetBoard() },
                                        shape = buttonShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(28.dp)
                                    ) { Text("-", fontSize = 16.sp) }
                                    Text(
                                        "$level",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Button(
                                        onClick = { level++; resetBoard() },
                                        shape = buttonShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(28.dp)
                                    ) { Text("+", fontSize = 16.sp) }
                                }
                            }
                        }
                    }
                }

                // EXIT (Door Icon) - Moved 1 row down
                Button(
                    onClick = { onExit() },
                    shape = buttonShape,
                    modifier = Modifier.size(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Exit",
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ==========================================
// MODULE 4: CANVAS DRAWING EXTENSIONS
// ==========================================
fun DrawScope.drawThemedItem(
    center: Offset,
    radius: Float,
    console: ConsoleDisc,
    theme: StageTheme
) {
    val itemColor = if (theme == StageTheme.BLACK_AND_WHITE) {
        if (console.faction == Faction.GOOD) Color.White else Color.Gray
    } else console.color

    val shapeOverride = if (theme == StageTheme.BOX_FILL) {
        when (console.ordinal % 3) {
            0 -> StageTheme.NEON_DISCS
            1 -> StageTheme.PACKAGES
            else -> StageTheme.CARTRIDGES
        }
    } else theme

    when (shapeOverride) {
        StageTheme.PACKAGES -> {
            val rectSize = radius * 1.8f
            drawRect(
                color = itemColor,
                topLeft = Offset(center.x - rectSize / 2, center.y - rectSize / 2),
                size = Size(rectSize, rectSize)
            )
            drawRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(center.x - rectSize / 2, center.y - rectSize / 2),
                size = Size(rectSize, rectSize),
                style = Stroke(width = 2f)
            )
        }

        StageTheme.CARTRIDGES -> {
            val width = radius * 1.8f
            val height = radius * 1.2f
            drawRoundRect(
                color = itemColor,
                topLeft = Offset(center.x - width / 2, center.y - height / 2),
                size = Size(width, height),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(center.x - width / 2, center.y - height / 2),
                size = Size(width, height),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 2f)
            )
        }

        else -> {
            drawCircle(color = itemColor, radius = radius, center = center)
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = 2f)
            )
            if (theme != StageTheme.BLACK_AND_WHITE) drawCircle(
                color = itemColor.copy(alpha = 0.5f),
                radius = radius * 1.2f,
                center = center,
                style = Stroke(width = 4f)
            )
        }
    }

    val faceColor = Color(0x99000000)
    val eyeRadius = radius * 0.15f
    val eyeOffsetX = radius * 0.35f
    val eyeOffsetY = radius * 0.2f
    drawCircle(faceColor, eyeRadius, Offset(center.x - eyeOffsetX, center.y - eyeOffsetY))
    drawCircle(faceColor, eyeRadius, Offset(center.x + eyeOffsetX, center.y - eyeOffsetY))

    if (console.faction == Faction.GOOD) {
        drawArc(
            color = faceColor,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.5f, center.y - radius * 0.2f),
            size = Size(radius, radius * 0.8f),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    } else {
        drawLine(
            faceColor,
            Offset(center.x - eyeOffsetX * 1.5f, center.y - eyeOffsetY * 1.8f),
            Offset(center.x - eyeOffsetX * 0.5f, center.y - eyeOffsetY * 1.2f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        drawLine(
            faceColor,
            Offset(center.x + eyeOffsetX * 1.5f, center.y - eyeOffsetY * 1.8f),
            Offset(center.x + eyeOffsetX * 0.5f, center.y - eyeOffsetY * 1.2f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        drawArc(
            color = faceColor,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.5f, center.y + radius * 0.2f),
            size = Size(radius, radius * 0.6f),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }
}

// ==========================================
// MODULE 5: GAMEPLAY LOGIC & RULES
// ==========================================
fun spawnDisc(columns: Int, isEndLevel: Boolean): ActiveDisc {
    val enemyProbability = if (isEndLevel) 50 else 35
    val isEnemy = kotlin.random.Random.nextInt(100) < enemyProbability

    val availableDiscs = ConsoleDisc.entries.filter {
        if (isEnemy) it.faction == Faction.ENEMY else it.faction == Faction.GOOD
    }
    return ActiveDisc(x = columns / 2, y = 0, console = availableDiscs.random())
}

data class BonusResult(val bonusSeconds: Int, val newScoredGrid: List<MutableList<Boolean>>)

fun calculateTimeBonus(
    grid: List<List<ConsoleDisc?>>,
    scoredGrid: List<MutableList<Boolean>>
): BonusResult {
    val rows = grid.size
    val cols = grid[0].size
    var totalBonus = 0
    val nextScoredGrid = scoredGrid.map { it.toMutableList() }

    for (y in 0 until rows) {
        if (grid[y].all { it != null } && !nextScoredGrid[y].all { it }) {
            totalBonus += 20
            for (x in 0 until cols) nextScoredGrid[y][x] = true
        }
    }

    for (x in 0 until cols) {
        var isFullCol = true
        var isAlreadyScored = true
        for (y in 0 until rows) {
            if (grid[y][x] == null) isFullCol = false
            if (!nextScoredGrid[y][x]) isAlreadyScored = false
        }
        if (isFullCol && !isAlreadyScored) {
            totalBonus += 20
            for (y in 0 until rows) nextScoredGrid[y][x] = true
        }
    }

    for (y in 0 until rows) {
        var count = 0
        var lastDisc: ConsoleDisc? = null
        var startX = 0
        for (x in 0 until cols) {
            val current = grid[y][x]
            if (current != null && current == lastDisc && current.faction == Faction.GOOD) {
                count++
            } else {
                if (count >= 2) {
                    val unscored = (startX until x).any { !nextScoredGrid[y][it] }
                    if (unscored) {
                        totalBonus += if (count >= 4) 10 else 5
                        for (i in startX until x) nextScoredGrid[y][i] = true
                    }
                }
                count = if (current != null && current.faction == Faction.GOOD) 1 else 0
                lastDisc = current
                startX = x
            }
        }
        if (count >= 2) {
            val unscored = (startX until cols).any { !nextScoredGrid[y][it] }
            if (unscored) {
                totalBonus += if (count >= 4) 10 else 5
                for (i in startX until cols) nextScoredGrid[y][i] = true
            }
        }
    }

    for (x in 0 until cols) {
        var count = 0
        var lastDisc: ConsoleDisc? = null
        var startY = 0
        for (y in 0 until rows) {
            val current = grid[y][x]
            if (current != null && current == lastDisc && current.faction == Faction.GOOD) {
                count++
            } else {
                if (count >= 2) {
                    val unscored = (startY until y).any { !nextScoredGrid[it][x] }
                    if (unscored) {
                        totalBonus += if (count >= 4) 10 else 5
                        for (i in startY until y) nextScoredGrid[i][x] = true
                    }
                }
                count = if (current != null && current.faction == Faction.GOOD) 1 else 0
                lastDisc = current
                startY = y
            }
        }
        if (count >= 2) {
            val unscored = (startY until rows).any { !nextScoredGrid[it][x] }
            if (unscored) {
                totalBonus += if (count >= 4) 10 else 5
                for (i in startY until rows) nextScoredGrid[i][x] = true
            }
        }
    }

    return BonusResult(totalBonus, nextScoredGrid)
}

fun checkWinCondition(
    grid: List<List<ConsoleDisc?>>,
    cols: Int,
    rows: Int,
    theme: StageTheme
): Boolean {
    if (theme == StageTheme.BOX_FILL) return false

    var hasHorizontal8 = false
    var hasVertical8 = false

    for (y in 0 until rows) {
        var count = 0
        for (x in 0 until cols) {
            if (grid[y][x]?.faction == Faction.GOOD) count++ else count = 0
            if (count >= 8) hasHorizontal8 = true
        }
    }

    for (x in 0 until cols) {
        var count = 0
        for (y in 0 until rows) {
            if (grid[y][x]?.faction == Faction.GOOD) count++ else count = 0
            if (count >= 8) hasVertical8 = true
        }
    }

    return hasHorizontal8 && hasVertical8
}

fun checkLossCondition(
    grid: List<List<ConsoleDisc?>>,
    theme: StageTheme,
    isGodMode: Boolean
): Boolean {
    if (isGodMode) return false

    val rows = grid.size
    val cols = grid[0].size
    if (theme == StageTheme.BOX_FILL) {
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                if (grid[y][x]?.faction == Faction.ENEMY) return true
            }
        }
    }
    for (y in 0 until rows) {
        for (x in 0 until cols) {
            val current = grid[y][x]
            if (current != null && current.faction == Faction.ENEMY) {
                if (x + 1 < cols && grid[y][x + 1]?.faction == Faction.ENEMY) return true
                if (y + 1 < rows && grid[y + 1][x]?.faction == Faction.ENEMY) return true
            }
        }
    }
    return false
}

fun saveNeonDiscResults(db: androidx.sqlite.db.SupportSQLiteDatabase?, level: Int, score: Int, wonLevel: Boolean) {
    if (db == null) return
    try {
        db.execSQL("INSERT INTO user_db.scores (tableName, score) VALUES ('NeonDisc_Lvl_$level', $score)")
        if (wonLevel) {
            val trophyType = when (level) {
                1 -> "Blue"
                2 -> "Bronze"
                3 -> "Gold"
                4 -> "Green"
                5 -> "Silver"
                7 -> "Red"
                else -> null
            }
            if (trophyType != null) {
                db.execSQL("INSERT INTO user_db.trophies (gameName, trophyType) VALUES ('NeonDisc', '$trophyType')")
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("NeonDisc", "Error saving results", e)
    }
}
