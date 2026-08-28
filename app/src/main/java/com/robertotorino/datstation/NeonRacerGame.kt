package com.robertotorino.datstation

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import com.robertotorino.datstation.ui.AppCornerRadius
import com.robertotorino.datstation.ui.LocalAppColors
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max

// ==========================================
// MODULE 1: DATA MODELS
// ==========================================
enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0),
    IDLE(0, 0);

    fun opposite(): Direction = when (this) {
        UP -> DOWN; DOWN -> UP; LEFT -> RIGHT; RIGHT -> LEFT; IDLE -> IDLE
    }
}

data class Car(
    val gridX: Int,
    val gridY: Int,
    val moveProgress: Float,
    val direction: Direction,
    val bufferedDirection: Direction,
    val color: Color
)

// ==========================================
// MODULE 2: PROCEDURAL MAZE ENGINE
// ==========================================
fun generateProceduralMaze(cols: Int = 21, rows: Int = 21): Array<IntArray> {
    val grid = Array(rows) { IntArray(cols) { 1 } }
    val stack = mutableListOf<Pair<Int, Int>>()
    grid[1][1] = 0
    stack.add(Pair(1, 1))

    val dirs = listOf(Pair(0, -2), Pair(0, 2), Pair(-2, 0), Pair(2, 0))

    while (stack.isNotEmpty()) {
        val (x, y) = stack.last()
        val validDirs = dirs.filter { (dx, dy) ->
            val nx = x + dx
            val ny = y + dy
            nx > 0 && nx < cols - 1 && ny > 0 && ny < rows - 1 && grid[ny][nx] == 1
        }

        if (validDirs.isNotEmpty()) {
            val (dx, dy) = validDirs.random()
            val nx = x + dx
            val ny = y + dy
            grid[y + dy / 2][x + dx / 2] = 0
            grid[ny][nx] = 0
            stack.add(Pair(nx, ny))
        } else {
            stack.removeAt(stack.size - 1)
        }
    }

    val totalCells = rows * cols
    repeat(totalCells / 10) {
        val rx = (2 until cols - 2).random()
        val ry = (2 until rows - 2).random()
        if (grid[ry][rx] == 1) {
            val horizontal = grid[ry][rx-1] == 0 && grid[ry][rx+1] == 0
            val vertical = grid[ry-1][rx] == 0 && grid[ry+1][rx] == 0
            if (horizontal || vertical) grid[ry][rx] = 0
        }
    }

    var finishPlaced = false
    for (y in rows - 2 downTo 1) {
        for (x in cols - 2 downTo 1) {
            if (grid[y][x] == 0) {
                grid[y][x] = 9
                finishPlaced = true
                break
            }
        }
        if (finishPlaced) break
    }

    val emptyCells = mutableListOf<Pair<Int, Int>>()
    for (y in 1 until rows - 1) {
        for (x in 1 until cols - 1) {
            if (grid[y][x] == 0 && (x > 3 || y > 3)) {
                emptyCells.add(Pair(x, y))
            }
        }
    }
    emptyCells.shuffle()

    var idx = 0
    repeat(6) { if (idx < emptyCells.size) { val (x,y) = emptyCells[idx++]; grid[y][x] = 2 } }
    repeat(4) { if (idx < emptyCells.size) { val (x,y) = emptyCells[idx++]; grid[y][x] = 3 } }
    repeat(10) { if (idx < emptyCells.size) { val (x,y) = emptyCells[idx++]; grid[y][x] = 4 } }

    return grid
}

fun findEnemySpawn(grid: Array<IntArray>): Pair<Int, Int> {
    for (y in grid.size - 2 downTo 1) {
        for (x in grid[0].size - 2 downTo 1) {
            if (grid[y][x] == 0 || grid[y][x] == 9) return Pair(x, y)
        }
    }
    return Pair(1, 1)
}

fun getNeonRacerTrophy(level: Int): Int? {
    // Trophies are awarded based on stage completion (every 3 levels)
    val stage = ((level - 1) / 3) + 1
    return when (stage % 3) {
        1 -> R.drawable.neon_racer_bronze
        2 -> R.drawable.neon_racer_silver
        0 -> R.drawable.neon_racer_gold
        else -> null
    }
}

// ==========================================
// MODULE 3: (DEPRECATED) SOUND MANAGER
// ==========================================
// Moved inside Composable for better lifecycle management

// ==========================================
// MODULE 4: GAME ENGINE & UI
// ==========================================
@Composable
fun NeonRacerGame(db: androidx.sqlite.db.SupportSQLiteDatabase?, onExit: () -> Unit) {
    val context = LocalContext.current
    val currentColors = LocalAppColors.current
    val prefs = remember { context.getSharedPreferences("neon_racer_prefs", android.content.Context.MODE_PRIVATE) }

    var level by remember { mutableIntStateOf(prefs.getInt("max_level_reached", 1)) }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var fuel by remember { mutableIntStateOf(100) }
    
    // Track won levels for trophies
    var wonLevels by remember { 
        val wonStr = prefs.getString("won_levels", "") ?: ""
        mutableStateOf(if (wonStr.isEmpty()) emptySet() else wonStr.split(",").map { it.toInt() }.toSet())
    }

    var mazeGrid by remember { mutableStateOf(generateProceduralMaze(21, 21)) }
    val rows = mazeGrid.size
    val columns = mazeGrid[0].size

    val isWall = { x: Int, y: Int -> x !in 0..<columns || y < 0 || y >= rows || mazeGrid[y][x] == 1 }
    val initialEnemyPos = findEnemySpawn(mazeGrid)

    // Find valid initial directions
    val initialPStartDir = if (!isWall(2, 1)) Direction.RIGHT else Direction.DOWN
    val initialEStartDir = when {
        !isWall(initialEnemyPos.first - 1, initialEnemyPos.second) -> Direction.LEFT
        !isWall(initialEnemyPos.first + 1, initialEnemyPos.second) -> Direction.RIGHT
        !isWall(initialEnemyPos.first, initialEnemyPos.second - 1) -> Direction.UP
        else -> Direction.DOWN
    }

    var player by remember { mutableStateOf(Car(1, 1, 0f, initialPStartDir, initialPStartDir, Color(0xFF2255FF))) }
    var enemy by remember { mutableStateOf(Car(initialEnemyPos.first, initialEnemyPos.second, 0f, initialEStartDir, initialEStartDir, Color(0xFFFF2222))) }

    var isGameOver by remember { mutableStateOf(false) }
    var isLevelWon by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isGodMode by remember { mutableStateOf(false) }
    var showLargeTrophy by remember { mutableStateOf<Int?>(null) }

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

    val enginePlayer = remember {
        try {
            MediaPlayer.create(context, R.raw.engines)
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        soundMap[R.raw.blob] = soundPool.load(context, R.raw.blob, 1)
        soundMap[R.raw.enemy_fall] = soundPool.load(context, R.raw.enemy_fall, 1)
        soundMap[R.raw.game_over] = soundPool.load(context, R.raw.game_over, 1)
        soundMap[R.raw.game_over_voice] = soundPool.load(context, R.raw.game_over_voice, 1)
        soundMap[R.raw.shoot_laser] = soundPool.load(context, R.raw.shoot_laser, 1)

        bgmPlayer?.isLooping = true
        enginePlayer?.isLooping = true

        onDispose {
            soundPool.release()
            try {
                if (bgmPlayer?.isPlaying == true) bgmPlayer.stop()
                bgmPlayer?.release()
                if (enginePlayer?.isPlaying == true) enginePlayer.stop()
                enginePlayer?.release()
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
                if (enginePlayer?.isPlaying == true) enginePlayer.pause()
            } else {
                if (bgmPlayer?.isPlaying == false) bgmPlayer.start()
                if (enginePlayer?.isPlaying == false) enginePlayer.start()
            }
        } catch (_: Exception) {
        }
    }

    val loadLevel = { targetLevel: Int ->
        level = max(1, targetLevel)
        mazeGrid = generateProceduralMaze(21, 21)
        val ePos = findEnemySpawn(mazeGrid)
        
        // Find valid starting directions
        val pStartDir = if (!isWall(2, 1)) Direction.RIGHT else Direction.DOWN
        val eStartDir = when {
            !isWall(ePos.first - 1, ePos.second) -> Direction.LEFT
            !isWall(ePos.first + 1, ePos.second) -> Direction.RIGHT
            !isWall(ePos.first, ePos.second - 1) -> Direction.UP
            else -> Direction.DOWN
        }

        player = Car(1, 1, 0f, pStartDir, pStartDir, player.color)
        enemy = Car(ePos.first, ePos.second, 0f, eStartDir, eStartDir, enemy.color)
        fuel = 100
        lives = 3 // Every level starts with 3 lives
        isGameOver = false
        isLevelWon = false
    }

    val handleSteering = { newDir: Direction ->
        if (newDir == player.direction.opposite()) {
            val targetX = player.gridX + player.direction.dx
            val targetY = player.gridY + player.direction.dy
            // Only allow immediate reverse if not already in/moving to a wall
            if (!isWall(targetX, targetY)) {
                player = player.copy(
                    gridX = targetX,
                    gridY = targetY,
                    moveProgress = 1.0f - player.moveProgress,
                    direction = newDir,
                    bufferedDirection = newDir
                )
            }
        } else {
            player = player.copy(bufferedDirection = newDir)
        }
    }

    LaunchedEffect(wonLevels) {
        prefs.edit { 
            putString("won_levels", wonLevels.joinToString(","))
            putInt("max_level_reached", (wonLevels.maxOrNull() ?: 0) + 1)
        }
    }

    LaunchedEffect(isGameOver, isLevelWon, isPaused, level) {
        while (!isGameOver && !isLevelWon && !isPaused) {
            delay(16)
            // Speed stays the same as level 3 in subsequent levels
            val speed = 0.05f + (minOf(level, 3) * 0.005f)

            // --- 1. PLAYER LOGIC ---
            var pProg = player.moveProgress + speed
            var pX = player.gridX
            var pY = player.gridY
            var pDir = player.direction
            var pBufDir = player.bufferedDirection

            // If we are currently moving towards a wall, stop!
            if (isWall(pX + pDir.dx, pY + pDir.dy)) {
                pProg = 0f
                // Force a turn check immediately
                val options = when (pDir) {
                    Direction.UP, Direction.DOWN -> listOf(Direction.LEFT, Direction.RIGHT)
                    Direction.LEFT, Direction.RIGHT -> listOf(Direction.UP, Direction.DOWN)
                    else -> emptyList()
                }
                for (opt in options) {
                    if (!isWall(pX + opt.dx, pY + opt.dy)) {
                        pDir = opt
                        pBufDir = opt
                        break
                    }
                }
            }

            if (pProg >= 1.0f) {
                pX += pDir.dx
                pY += pDir.dy
                pProg -= 1.0f
                fuel -= 1

                if (pBufDir != pDir && !isWall(pX + pBufDir.dx, pY + pBufDir.dy)) {
                    pDir = pBufDir
                }

                if (isWall(pX + pDir.dx, pY + pDir.dy)) {
                    val options = when (pDir) {
                        Direction.UP, Direction.DOWN -> listOf(Direction.LEFT, Direction.RIGHT)
                        Direction.LEFT, Direction.RIGHT -> listOf(Direction.UP, Direction.DOWN)
                        else -> emptyList()
                    }
                    var turned = false
                    for (opt in options) {
                        if (!isWall(pX + opt.dx, pY + opt.dy)) {
                            pDir = opt
                            pBufDir = opt
                            turned = true
                            break
                        }
                    }
                    if (!turned) {
                        pDir = pDir.opposite()
                        pBufDir = pDir
                    }
                }

                val currentCell = mazeGrid[pY][pX]
                when (currentCell) {
                    2 -> {
                        fuel = minOf(100, fuel + 30)
                        score += 100
                        mazeGrid[pY][pX] = 0
                        playSfx(R.raw.blob)
                    }
                    3 -> {
                        score += 500
                        mazeGrid[pY][pX] = 0
                        playSfx(R.raw.blob)
                        if (score % 5000 == 0) {
                            lives++
                            playSfx(R.raw.blob)
                        }
                    }
                    4 -> {
                        if (lives > 0) {
                            lives -= 1
                            mazeGrid[pY][pX] = 0
                            playSfx(R.raw.enemy_fall)
                            Toast.makeText(context, "Obstacle!", Toast.LENGTH_SHORT).show()
                        } else {
                            isGameOver = true
                            playSfx(R.raw.game_over)
                            playSfx(R.raw.game_over_voice)
                            saveNeonRacerResults(db, level, score, false)
                        }
                    }
                    9 -> {
                        isLevelWon = true
                        wonLevels = wonLevels + level
                        playSfx(R.raw.shoot_laser)
                        saveNeonRacerResults(db, level, score, true)
                    }
                }
            }

            player = player.copy(gridX = pX, gridY = pY, moveProgress = pProg, direction = pDir, bufferedDirection = pBufDir)

            // --- 2. ENEMY LOGIC ---
            var eProg = enemy.moveProgress + speed * 0.85f
            var eX = enemy.gridX
            var eY = enemy.gridY
            var eDir = enemy.direction

            if (eProg >= 1.0f) {
                eX += eDir.dx
                eY += eDir.dy
                eProg -= 1.0f

                var bestDist = Float.MAX_VALUE
                var bestDir = eDir
                val options = Direction.entries.filter { it != Direction.IDLE && it != eDir.opposite() }
                var validOptions = options.filter { !isWall(eX + it.dx, eY + it.dy) }
                if (validOptions.isEmpty()) validOptions = listOf(eDir.opposite())

                for (opt in validOptions) {
                    val nx = eX + opt.dx
                    val ny = eY + opt.dy
                    val dist = abs(nx - pX) + abs(ny - pY)
                    if (dist < bestDist) {
                        bestDist = dist.toFloat()
                        bestDir = opt
                    }
                }
                eDir = bestDir
            }
            enemy = enemy.copy(gridX = eX, gridY = eY, moveProgress = eProg, direction = eDir)

            // --- 3. COLLISION ---
            val pRealX = pX + pDir.dx * pProg
            val pRealY = pY + pDir.dy * pProg
            val eRealX = eX + eDir.dx * eProg
            val eRealY = eY + eDir.dy * eProg

            val collisionDist = abs(pRealX - eRealX) + abs(pRealY - eRealY)
            if (collisionDist < 0.8f) {
                if (lives > 0) {
                    lives -= 1
                    
                    val pStartDir = if (!isWall(2, 1)) Direction.RIGHT else Direction.DOWN
                    player = Car(1, 1, 0f, pStartDir, pStartDir, player.color)
                    
                    val respawn = findEnemySpawn(mazeGrid)
                    val eStartDir = when {
                        !isWall(respawn.first - 1, respawn.second) -> Direction.LEFT
                        !isWall(respawn.first + 1, respawn.second) -> Direction.RIGHT
                        !isWall(respawn.first, respawn.second - 1) -> Direction.UP
                        else -> Direction.DOWN
                    }
                    enemy = Car(respawn.first, respawn.second, 0f, eStartDir, eStartDir, enemy.color)

                    playSfx(R.raw.enemy_fall)
                    Toast.makeText(context, "Crashed!", Toast.LENGTH_SHORT).show()
                } else {
                    isGameOver = true
                    playSfx(R.raw.game_over)
                    playSfx(R.raw.game_over_voice)
                    saveNeonRacerResults(db, level, score, false)
                }
            }

            if (fuel <= 0 && !isGameOver) {
                isGameOver = true
                playSfx(R.raw.game_over)
                playSfx(R.raw.game_over_voice)
                saveNeonRacerResults(db, level, score, false)
            }
        }
    }

    val baseWallColor = when (level % 5) {
        1 -> Color(0xFF001133)
        2 -> Color(0xFF1A0033)
        3 -> Color(0xFF003311)
        4 -> Color(0xFF331100)
        else -> Color(0xFF330011)
    }

    val highlightWallColor = when (level % 5) {
        1 -> Color(0xFF002266)
        2 -> Color(0xFF330066)
        3 -> Color(0xFF006622)
        4 -> Color(0xFF662200)
        else -> Color(0xFF660022)
    }

    // --- UI Layout ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(currentColors.background)
            .padding(vertical = 32.dp)
    ) {
        // --- TOP HUD & MINIMAP ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("STAGE $level", color = currentColors.accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("SCORE: $score", color = currentColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("LIVES: $lives", color = Color(0xFFFF4444), fontSize = 16.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(6.dp))

                Text("FUEL", color = Color(0xFFFFEE00), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                RetroFuelBar(fuel = fuel)
            }

            // TROPHY (Much Bigger, Clickable, with Counter)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val highestWonLevel = wonLevels.maxOrNull() ?: 0
                val trophyRes = if (highestWonLevel >= 3) {
                    getNeonRacerTrophy(highestWonLevel) ?: R.drawable.neon_racer_placeholder
                } else {
                    R.drawable.neon_racer_placeholder
                }

                Image(
                    painter = painterResource(id = trophyRes),
                    contentDescription = "Trophy",
                    modifier = Modifier
                        .size(100.dp)
                        .clickable { if (trophyRes != R.drawable.neon_racer_placeholder) showLargeTrophy = trophyRes }
                )
            }

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(baseWallColor) // Radar matches the level color!
                    .border(2.dp, currentColors.boxBorder)
                    .clickable { isGodMode = !isGodMode }
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                    val mapCellW = size.width / columns
                    val mapCellH = size.height / rows

                    for (y in 0 until rows) {
                        for (x in 0 until columns) {
                            val topLeft = Offset(x * mapCellW, y * mapCellH)
                            val cellSize = Size(mapCellW, mapCellH)
                            if (mazeGrid[y][x] == 2 || mazeGrid[y][x] == 3) drawRect(Color.Yellow, topLeft, cellSize)
                            if (mazeGrid[y][x] == 9) drawRect(Color.Green, topLeft, cellSize)
                        }
                    }

                    val playerDot = Size(mapCellW * 2f, mapCellH * 2f)
                    val enemyDot = Size(mapCellW * 2f, mapCellH * 2f)
                    val pX = player.gridX + player.direction.dx * player.moveProgress
                    val pY = player.gridY + player.direction.dy * player.moveProgress
                    val eX = enemy.gridX + enemy.direction.dx * enemy.moveProgress
                    val eY = enemy.gridY + enemy.direction.dy * enemy.moveProgress

                    drawRect(Color.Cyan, Offset(pX * mapCellW - mapCellW/2, pY * mapCellH - mapCellH/2), playerDot)
                    drawRect(Color.Red, Offset(eX * mapCellW - mapCellW/2, eY * mapCellH - mapCellH/2), enemyDot)
                }
            }
        }

        // --- MAIN GAME CANVAS ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(2.dp, currentColors.boxBorder)
                .clipToBounds()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures {
                        if (!isGameOver && !isLevelWon) isPaused = !isPaused
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val blockSize = 100f
                val playerPixelX = (player.gridX + player.direction.dx * player.moveProgress) * blockSize + (blockSize / 2)
                val playerPixelY = (player.gridY + player.direction.dy * player.moveProgress) * blockSize + (blockSize / 2)

                val maxOffsetX = 0f
                val minOffsetX = minOf(0f, size.width - (columns * blockSize))
                val cameraOffsetX = ((size.width / 2) - playerPixelX).coerceIn(minOffsetX, maxOffsetX)

                val maxOffsetY = 0f
                val minOffsetY = minOf(0f, size.height - (rows * blockSize))
                val cameraOffsetY = ((size.height / 2) - playerPixelY).coerceIn(minOffsetY, maxOffsetY)

                translate(left = cameraOffsetX, top = cameraOffsetY) {
                    drawRect(Color(0xFFB8C0C8), size = Size(columns * blockSize, rows * blockSize))

                    for (y in 0 until rows) {
                        for (x in 0 until columns) {
                            val topLeft = Offset(x * blockSize, y * blockSize)
                            val cellSize = Size(blockSize, blockSize)
                            val center = Offset(topLeft.x + blockSize / 2, topLeft.y + blockSize / 2)

                            when (mazeGrid[y][x]) {
                                1 -> {
                                    val wallInset = 6f
                                    drawRect(baseWallColor, topLeft, cellSize)
                                    drawRoundRect(
                                        color = highlightWallColor,
                                        topLeft = Offset(topLeft.x + wallInset, topLeft.y + wallInset),
                                        size = Size(blockSize - wallInset * 2, blockSize - wallInset * 2),
                                        cornerRadius = CornerRadius(8f, 8f),
                                        style = Stroke(width = 4f)
                                    )
                                }
                                2 -> {
                                    drawRect(Color.Red, Offset(center.x - 10f, center.y - 15f), Size(20f, 30f))
                                    drawRect(Color.Yellow, Offset(center.x - 6f, center.y - 10f), Size(12f, 15f))
                                    drawLine(Color.Black, Offset(center.x + 10f, center.y), Offset(center.x + 20f, center.y + 10f), strokeWidth = 4f)
                                }
                                3 -> {
                                    drawLine(Color.White, Offset(center.x - 15f, center.y - 20f), Offset(center.x - 15f, center.y + 20f), strokeWidth = 6f)
                                    val flagPath = Path().apply { moveTo(center.x - 12f, center.y - 20f); lineTo(center.x + 20f, center.y - 5f); lineTo(center.x - 12f, center.y + 10f); close() }
                                    drawPath(flagPath, Color.Yellow)
                                }
                                4 -> {
                                    drawCircle(Color.DarkGray, radius = 15f, center = Offset(center.x - 10f, center.y - 10f))
                                    drawCircle(Color.Gray, radius = 18f, center = Offset(center.x + 5f, center.y + 5f))
                                    drawCircle(Color(0xFF555555), radius = 12f, center = Offset(center.x - 5f, center.y + 15f))
                                }
                                9 -> {
                                    drawRect(Color.Green.copy(alpha = 0.3f), topLeft, cellSize)
                                    drawRect(Color.White, Offset(topLeft.x, center.y - 10f), Size(blockSize, 20f))
                                    for(i in 0..4) {
                                        drawRect(Color.Black, Offset(topLeft.x + (i * blockSize / 5), center.y - 10f), Size(blockSize / 5, 10f))
                                        drawRect(Color.Black, Offset(topLeft.x + (i * blockSize / 5) + (blockSize/10), center.y), Size(blockSize / 5, 10f))
                                    }
                                }
                            }
                        }
                    }

                    drawTopDownCar(center = Offset(playerPixelX, playerPixelY), carSize = blockSize * 0.7f, carColor = player.color, direction = player.direction)

                    val enemyPixelX = (enemy.gridX + enemy.direction.dx * enemy.moveProgress) * blockSize + (blockSize / 2)
                    val enemyPixelY = (enemy.gridY + enemy.direction.dy * enemy.moveProgress) * blockSize + (blockSize / 2)
                    drawTopDownCar(center = Offset(enemyPixelX, enemyPixelY), carSize = blockSize * 0.7f, carColor = enemy.color, direction = enemy.direction)
                }
            }

            if (isPaused && !isGameOver && !isLevelWon) Text("PAUSED", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha=0.7f), RoundedCornerShape(AppCornerRadius)).padding(16.dp))
            if (isGameOver) Text("GAME OVER", color = Color.Red, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha=0.7f), RoundedCornerShape(AppCornerRadius)).padding(16.dp))
            if (isLevelWon) Text("CLEARED!", color = Color.Green, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha=0.7f), RoundedCornerShape(AppCornerRadius)).padding(16.dp))
        }

        // --- TROPHY DIALOG ---
        if (showLargeTrophy != null) {
            Dialog(onDismissRequest = { showLargeTrophy = null }) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(Color(0xFF222222), RoundedCornerShape(AppCornerRadius))
                        .clickable { showLargeTrophy = null },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = showLargeTrophy!!),
                            contentDescription = "Large Trophy",
                            modifier = Modifier.size(160.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            when (showLargeTrophy) {
                                R.drawable.neon_racer_bronze -> "Bronze Racer Trophy"
                                R.drawable.neon_racer_silver -> "Silver Racer Trophy"
                                R.drawable.neon_racer_gold -> "Gold Racer Trophy"
                                else -> "Winner Trophy"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        // --- BOTTOM CONTROLS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp), // Pushes left block left, right block!
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // LEFT SIDE: D-Pad exactly 152dp tall
            Box(
                modifier = Modifier
                    .width(168.dp)
                    .height(152.dp)
            ) {
                // Top button pinned to the top edge
                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    GameControlButton(icon = Icons.Default.KeyboardArrowUp) { handleSteering(Direction.UP) }
                }

                // Left & Right in the center
                Box(modifier = Modifier.align(Alignment.CenterStart)) {
                    GameControlButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft) { handleSteering(Direction.LEFT) }
                }
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    GameControlButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowRight) { handleSteering(Direction.RIGHT) }
                }

                // Bottom button pinned to the bottom edge
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    GameControlButton(icon = Icons.Default.KeyboardArrowDown) { handleSteering(Direction.DOWN) }
                }
            }

            // RIGHT SIDE: Actions exactly 152dp tall
            Column(
                modifier = Modifier.height(152.dp),
                verticalArrangement = Arrangement.SpaceBetween, // Evenly spaces items to fill 152dp height
                horizontalAlignment = Alignment.End
            ) {
                // 1. Top Row: STAGE SELECTOR (Visible if progressed or in God Mode)
                val maxSelectableLevel = if (isGodMode) 100 else (wonLevels.maxOrNull() ?: 0) + 1
                if (maxSelectableLevel > 1 || isGodMode) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isGodMode) Color(0xFF330066) else Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(AppCornerRadius),
                        modifier = Modifier.width(156.dp).height(50.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { if (level > 1) loadLevel(level - 1) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isGodMode) Color.Magenta else Color.DarkGray),
                                modifier = Modifier.size(36.dp),
                                contentPadding = PaddingValues(0.dp),
                                enabled = level > 1
                            ) { Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) }

                            Text("$level", color = if (isGodMode) Color.White else currentColors.accent, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)

                            Button(
                                onClick = { if (level < maxSelectableLevel) loadLevel(level + 1) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isGodMode) Color.Magenta else Color.DarkGray),
                                modifier = Modifier.size(36.dp),
                                contentPadding = PaddingValues(0.dp),
                                enabled = level < maxSelectableLevel
                            ) { Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                        }
                    }
                } else {
                    Box(modifier = Modifier.size(56.dp))
                }

                // 2. Middle Row: Next Stage (Visible ONLY if stage clear)
                if (isLevelWon) {
                    Button(
                        onClick = { loadLevel(level + 1) },
                        modifier = Modifier.width(156.dp).height(48.dp),
                        shape = RoundedCornerShape(AppCornerRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CC66))
                    ) { Text("NEXT STAGE", color = Color.Black, fontWeight = FontWeight.Bold) }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }

                // 3. Bottom Row: Tools (Restart, Exit)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // MUTE
                    Button(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(AppCornerRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isMuted) Color(0xFF990000) else Color(0xFF0066CC)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) { Icon(if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Mute", tint = Color.White, modifier = Modifier.size(24.dp)) }

                    // RESTART (Recycle Icon)
                    Button(
                        onClick = { score = 0; lives = 3; loadLevel(1) },
                        shape = RoundedCornerShape(AppCornerRadius),
                        modifier = Modifier.size(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Restart",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // EXIT (Door Icon)
                    Button(
                        onClick = onExit,
                        shape = RoundedCornerShape(AppCornerRadius),
                        modifier = Modifier.size(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = currentColors.boxBackground),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Exit",
                            tint = currentColors.boxText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameControlButton(icon: ImageVector, onClick: () -> Unit) {
    val currentColors = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(currentColors.boxBackground, RoundedCornerShape(AppCornerRadius))
            .border(1.dp, currentColors.boxBorder, RoundedCornerShape(AppCornerRadius))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Icon(imageVector = icon, contentDescription = null, tint = currentColors.boxText, modifier = Modifier.size(32.dp)) }
}

// ==========================================
// MODULE 5: CANVAS DRAWING EXTENSIONS & CUSTOM UI
// ==========================================
@Composable
fun RetroFuelBar(fuel: Int) {
    val totalSegments = 10
    val activeSegments = (fuel / 10f).toInt().coerceIn(0, totalSegments)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("E", color = Color.Red, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))

        Row(
            modifier = Modifier
                .border(2.dp, Color.DarkGray)
                .background(Color.Black)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (i in 0 until totalSegments) {
                val isActive = i < activeSegments
                val segmentColor = when {
                    !isActive -> Color(0xFF222222)
                    i < 2 -> Color.Red
                    i < 5 -> Color.Yellow
                    else -> Color.Green
                }

                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(14.dp)
                        .background(segmentColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))
        Text("F", color = Color.Green, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
    }
}

fun DrawScope.drawTopDownCar(center: Offset, carSize: Float, carColor: Color, direction: Direction) {
    val angle = when (direction) {
        Direction.UP -> 0f
        Direction.RIGHT -> 90f
        Direction.DOWN -> 180f
        Direction.LEFT -> 270f
        Direction.IDLE -> 90f
    }

    withTransform({ rotate(angle, center) }) {
        val width = carSize * 0.5f
        val length = carSize * 0.8f
        val wheelW = carSize * 0.15f
        val wheelH = carSize * 0.25f
        val wheelOffsetX = width / 2 + wheelW / 2
        val wheelOffsetY = length / 4

        drawRect(Color.Black, Offset(center.x - wheelOffsetX - wheelW / 2, center.y - wheelOffsetY - wheelH / 2), Size(wheelW, wheelH))
        drawRect(Color.Black, Offset(center.x + wheelOffsetX - wheelW / 2, center.y - wheelOffsetY - wheelH / 2), Size(wheelW, wheelH))
        drawRect(Color.Black, Offset(center.x - wheelOffsetX - wheelW / 2, center.y + wheelOffsetY - wheelH / 2), Size(wheelW, wheelH))
        drawRect(Color.Black, Offset(center.x + wheelOffsetX - wheelW / 2, center.y + wheelOffsetY - wheelH / 2), Size(wheelW, wheelH))

        drawRoundRect(color = carColor, topLeft = Offset(center.x - width / 2, center.y - length / 2), size = Size(width, length), cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))
        drawRoundRect(color = Color(0xFFDDDDDD), topLeft = Offset(center.x - width / 4, center.y - length / 6), size = Size(width / 2, length / 3), cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()))

        val nosePath = Path().apply { moveTo(center.x - width / 2, center.y - length / 2); lineTo(center.x + width / 2, center.y - length / 2); lineTo(center.x, center.y - length / 1.1f); close() }
        drawPath(nosePath, carColor)
        drawRect(color = Color.DarkGray, topLeft = Offset(center.x - width * 0.6f, center.y - length / 1.15f), size = Size(width * 1.2f, carSize * 0.08f))
    }
}

fun saveNeonRacerResults(db: androidx.sqlite.db.SupportSQLiteDatabase?, level: Int, score: Int, wonLevel: Boolean) {
    if (db == null) return
    try {
        db.execSQL("INSERT INTO user_db.scores (tableName, score) VALUES ('NeonRacer_Lvl_$level', $score)")
        if (wonLevel && level % 3 == 0) {
            val stage = level / 3
            val trophyType = when (stage % 3) {
                1 -> "Bronze"
                2 -> "Silver"
                0 -> "Gold"
                else -> "Bronze"
            }
            db.execSQL("INSERT INTO user_db.trophies (gameName, trophyType) VALUES ('NeonRacer', '$trophyType')")
        }
    } catch (e: Exception) {
        android.util.Log.e("NeonRacer", "Error saving results", e)
    }
}
