package com.robertotorino.datstation

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robertotorino.datstation.ui.AppCornerRadius
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- PLAYSTATION THEME CONSTANTS ---
val PS_BLUE = Color(0xFF003791)
val PS_LIGHT_BLUE = Color(0xFF0072CE)
val PS_WHITE = Color.White
val PS_GREEN = Color(0xFF2E7D32)
val PS_RED = Color(0xFFC62828)
val PS_GRAY = Color(0xFF424242)

// --- DATA MODELS ---
enum class QuestionType { MC, TF }

private fun getStatusSuffix(round: Int): String {
    return when (round) {
        1 -> "default_quiz"
        2 -> "blue_quiz"
        3 -> "green_quiz"
        4 -> "blue_stars_quiz"
        5 -> "green_stars_quiz"
        6 -> "purple_stars_quiz"
        7 -> "red_stars_quiz"
        8 -> "bronze_quiz"
        9 -> "silver_quiz"
        10 -> "gold_quiz"
        11 -> "gold_stars_quiz"
        12 -> "platinum_quiz"
        else -> if (round >= 13) "diamond_quiz" else "default_quiz"
    }
}

private fun getQuizResource(name: String?, status: String = "default_quiz"): Int {
    if (name == null) return 0
    val prefix = name.lowercase()
    val suffix = status.lowercase()

    // Roberto (Male), Moonshie (Female)
    return when ("${prefix}_$suffix") {
        "roberto_default_quiz" -> R.drawable.roberto_profile_default
        "moonshie_default_quiz" -> R.drawable.moonshie_profile_default
        "roberto_blue_quiz" -> R.drawable.roberto_blue_quiz
        "moonshie_blue_quiz" -> R.drawable.moonshie_blue_quiz
        "roberto_green_quiz" -> R.drawable.roberto_green_quiz
        "moonshie_green_quiz" -> R.drawable.moonshie_green_quiz
        "roberto_blue_stars_quiz" -> R.drawable.roberto_blue_stars_quiz
        "moonshie_blue_stars_quiz" -> R.drawable.moonshie_blue_stars_quiz
        "roberto_green_stars_quiz" -> R.drawable.roberto_green_stars_quiz
        "moonshie_green_stars_quiz" -> R.drawable.moonshie_green_stars_quiz
        "roberto_purple_stars_quiz" -> R.drawable.roberto_purple_stars_quiz
        "moonshie_purple_stars_quiz" -> R.drawable.moonshie_purple_stars_quiz
        "roberto_red_stars_quiz" -> R.drawable.roberto_red_stars_quiz
        "moonshie_red_stars_quiz" -> R.drawable.moonshie_red_stars_quiz
        "roberto_bronze_quiz" -> R.drawable.roberto_bronze_quiz
        "moonshie_bronze_quiz" -> R.drawable.moonshie_bronze_quiz
        "roberto_silver_quiz" -> R.drawable.roberto_silver_quiz
        "moonshie_silver_quiz" -> R.drawable.moonshie_silver_quiz
        "roberto_gold_quiz" -> R.drawable.roberto_gold_quiz
        "moonshie_gold_quiz" -> R.drawable.moonshie_gold_quiz
        "roberto_gold_stars_quiz" -> R.drawable.roberto_gold_stars_quiz
        "moonshie_gold_stars_quiz" -> R.drawable.moonshie_gold_stars_quiz
        "roberto_platinum_quiz" -> R.drawable.roberto_platinum_quiz
        "moonshie_platinum_quiz" -> R.drawable.moonshie_platinum_quiz
        "roberto_diamond_quiz" -> R.drawable.roberto_diamond_quiz
        "moonshie_diamond_quiz" -> R.drawable.moonshie_diamond_quiz
        else -> 0
    }
}

data class Question(
    val id: String,
    val text: String,
    val type: QuestionType,
    val options: List<String>,
    val correctAnswer: String,
    val points: Int = 10,
    val category: String,
    val explanation: String = ""
)

enum class QuizGameState { SELECTING_CATEGORY, PLAYING, RESULTS, LOADING }

// --- VIEW MODEL ---
class QuizViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DatabaseProvider.getDatabase(application)
    private val dao = db.questionDao()
    private val prefs = application.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)

    // We need GameRepository for user_profile
    private val mainDb = DatabaseManager.getDatabase(application)
    private val repository = GameRepository(mainDb)

    // Bump whenever assets/questions.json changes so installed apps reload the questions.
    private val currentQuestionVersion = 16

    private val _gameState = MutableStateFlow(QuizGameState.LOADING)
    val gameState: StateFlow<QuizGameState> = _gameState.asStateFlow()

    private val _currentQuestion = MutableStateFlow<Question?>(null)
    val currentQuestion: StateFlow<Question?> = _currentQuestion.asStateFlow()

    private val _timeLeft = MutableStateFlow(30)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _questionsInRound = MutableStateFlow(10)
    val questionsInRound: StateFlow<Int> = _questionsInRound.asStateFlow()

    private val _questionsAnswered = MutableStateFlow(0)
    val questionsAnswered: StateFlow<Int> = _questionsAnswered.asStateFlow()

    private val _correctAnswers = MutableStateFlow(0)
    val correctAnswers: StateFlow<Int> = _correctAnswers.asStateFlow()

    private val _wrongAnswers = MutableStateFlow(0)
    val wrongAnswers: StateFlow<Int> = _wrongAnswers.asStateFlow()

    private val _selectedCategory = MutableStateFlow("ALL")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _feedback = MutableStateFlow<Pair<String?, Boolean?>>(null to null)
    val feedback: StateFlow<Pair<String?, Boolean?>> = _feedback.asStateFlow()

    private val _isPerfectRound = MutableStateFlow(true)
    val isPerfectRound: StateFlow<Boolean> = _isPerfectRound.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isGodMode = MutableStateFlow(false)
    val isGodMode: StateFlow<Boolean> = _isGodMode.asStateFlow()

    private val _roundCount = MutableStateFlow(1)
    val roundCount: StateFlow<Int> = _roundCount.asStateFlow()

    private val _characterChoice = MutableStateFlow<String?>(null)
    val characterChoice: StateFlow<String?> = _characterChoice.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            setupDatabaseIfNecessary()

            // Load character from DB
            val savedChar =
                withContext(Dispatchers.IO) { repository.getUserProfileValue("user_character") }
            _characterChoice.value = savedChar

            _gameState.value = QuizGameState.SELECTING_CATEGORY
        }
    }

    private suspend fun setupDatabaseIfNecessary() = withContext(Dispatchers.IO) {
        val savedVersion = prefs.getInt("questions_version", 0)
        if (savedVersion < currentQuestionVersion || dao.getQuestionCount() == 0) {
            dao.deleteAllQuestions()
            val entities = JsonParserHelper.loadQuestionsFromAssets(getApplication())
            dao.insertQuestions(entities)
            prefs.edit { putInt("questions_version", currentQuestionVersion) }
        }
    }

    fun startQuiz(category: String) {
        _selectedCategory.value = category
        _score.value = 0
        _questionsAnswered.value = 0
        _correctAnswers.value = 0
        _wrongAnswers.value = 0
        _timeLeft.value = 42
        _isPerfectRound.value = true
        _feedback.value = null to null
        _isPaused.value = false
        _roundCount.value = 1
        _questionsInRound.value = if (category == "QUICK") 6 else 10
        _gameState.value = QuizGameState.PLAYING
        viewModelScope.launch {
            dao.resetAllQuestions()
            loadNextQuestion()
        }
    }

    private suspend fun loadNextQuestion() = withContext(Dispatchers.IO) {
        if (_questionsAnswered.value >= _questionsInRound.value) {
            _gameState.value = QuizGameState.RESULTS
            timerJob?.cancel()
            saveQuizResults(mainDb, _score.value, _isPerfectRound.value)
            return@withContext
        }

        val limit = 1
        val category = _selectedCategory.value
        val entities = when (category) {
            "ALL", "QUICK" -> {
                dao.getRandomUnaskedQuestions(limit)
            }
            "TRUE/FALSE" -> {
                dao.getRandomUnaskedQuestionsByType("TF", limit)
            }
            else -> {
                val dbCategory = when (category) {
                    "PS1" -> "PlayStation1"
                    "PS2" -> "PlayStation2"
                    "PS3" -> "PlayStation3"
                    "PS4" -> "PlayStation4"
                    "PS5" -> "PlayStation5"
                    "PSP" -> "PlayStationPortable"
                    "PSVITA" -> "PlayStationVita"
                    "CONTROLLER" -> "Controller"
                    "DIALOGUE" -> "Dialogue"
                    "GENERAL" -> "General"
                    "APPS" -> "Apps"
                    else -> category
                }
                dao.getRandomUnaskedQuestionsByCategory(dbCategory, limit)
            }
        }

        if (entities.isNotEmpty()) {
            val entity = entities[0]
            dao.markQuestionAsAsked(entity.id)
            val type = try {
                QuestionType.valueOf(entity.type)
            } catch (_: Exception) {
                QuestionType.MC
            }
            val options = Converters().fromString(entity.optionsJson).shuffled()
            val nextQ = Question(
                id = entity.id,
                text = entity.text,
                type = type,
                options = options,
                correctAnswer = entity.correctAnswer,
                points = entity.points,
                category = entity.category
            )
            _feedback.value = null to null
            _currentQuestion.value = nextQ
            startTimer()
        } else {
            _gameState.value = QuizGameState.RESULTS
            timerJob?.cancel()
        }
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (_timeLeft.value > 0 && _gameState.value == QuizGameState.PLAYING) {
                delay(1000L)
                if (!_isPaused.value && _feedback.value.first == null) _timeLeft.value -= 1
            }
            if (_timeLeft.value == 0) {
                _isPerfectRound.value = false
                _gameState.value = QuizGameState.RESULTS
                saveQuizResults(mainDb, _score.value, false)
            }
        }
    }

    fun submitAnswer(selectedOption: String) {
        val currentQ = _currentQuestion.value ?: return
        if (_feedback.value.first != null || _isPaused.value) return
        val isCorrect = selectedOption.trim().equals(currentQ.correctAnswer.trim(), ignoreCase = true)
        _feedback.value = selectedOption to isCorrect
        viewModelScope.launch {
            if (isCorrect) {
                _score.value += currentQ.points
                _timeLeft.value += 5
                _correctAnswers.value += 1
                delay(2000L)
            } else {
                if (!_isGodMode.value) {
                    _timeLeft.value = (_timeLeft.value - 5).coerceAtLeast(0)
                }
                _isPerfectRound.value = false
                _wrongAnswers.value += 1
                delay(4000L)
            }
            _questionsAnswered.value += 1
            loadNextQuestion()
        }
    }

    fun skipQuestion() {
        if (_feedback.value.first != null || _isPaused.value) return
        _wrongAnswers.value += 1
        _isPerfectRound.value = false
        viewModelScope.launch {
            if (!_isGodMode.value) {
                _timeLeft.value = (_timeLeft.value - 5).coerceAtLeast(0)
            }
            _questionsAnswered.value += 1
            loadNextQuestion()
        }
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleGodMode() {
        _isGodMode.value = !_isGodMode.value
    }

    fun exitToCategorySelection() {
        timerJob?.cancel(); _gameState.value = QuizGameState.SELECTING_CATEGORY
    }

    fun nextRound() {
        _roundCount.value += 1
        _questionsInRound.value += 5
        _questionsAnswered.value = 0
        _correctAnswers.value = 0
        _wrongAnswers.value = 0
        _isPerfectRound.value = true
        _feedback.value = null to null
        _gameState.value = QuizGameState.PLAYING
        viewModelScope.launch { loadNextQuestion() }
    }

    override fun onCleared() {
        super.onCleared(); timerJob?.cancel()
    }
}

// --- UI COMPONENTS ---

@Composable
fun QuizScreen(db: androidx.sqlite.db.SupportSQLiteDatabase?, onExit: () -> Unit) {
    val viewModel: QuizViewModel = viewModel()
    val context = LocalContext.current
    val gameState by viewModel.gameState.collectAsState()
    val charChoice by viewModel.characterChoice.collectAsState()
    val roundCount by viewModel.roundCount.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    var showLargeChar by remember { mutableStateOf(false) }

    val bgmPlayer = remember {
        try {
            MediaPlayer.create(context, R.raw.sea_breeze)
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        bgmPlayer?.isLooping = true
        onDispose {
            bgmPlayer?.release()
        }
    }

    LaunchedEffect(gameState, isPaused, isMuted) {
        if (gameState != QuizGameState.LOADING && !isPaused && !isMuted) {
            if (bgmPlayer?.isPlaying == false) bgmPlayer.start()
        } else {
            if (bgmPlayer?.isPlaying == true) bgmPlayer.pause()
        }
    }

    if (showLargeChar && charChoice != null) {
        Dialog(
            onDismissRequest = { showLargeChar = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showLargeChar = false },
                contentAlignment = Alignment.Center
            ) {
                // Use current round's status for the large image
                val statusSuffix = getStatusSuffix(roundCount)
                val resId = getQuizResource(charChoice, statusSuffix)
                if (resId != 0) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = "Large Character",
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (charChoice == "roberto") "Roberto Torino" else "Moonshie du Marchee",
                            color = PS_WHITE,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(PS_BLUE)
        .systemBarsPadding()) {
        when (gameState) {
            QuizGameState.LOADING -> CircularProgressIndicator(
                Modifier.align(Alignment.Center),
                color = PS_WHITE
            )

            QuizGameState.SELECTING_CATEGORY -> CategorySelectionScreen(viewModel, onExit) { showLargeChar = true }
            QuizGameState.PLAYING -> PlayingScreen(viewModel) { showLargeChar = true }
            QuizGameState.RESULTS -> ResultsScreen(viewModel)
        }
    }
}

@Composable
fun CategorySelectionScreen(viewModel: QuizViewModel, onExit: () -> Unit, onShowLargeChar: () -> Unit) {
    val categories = listOf(
        "ALL",
        "TRUE/FALSE",
        "PS1",
        "PS2",
        "PS3",
        "PS4",
        "PS5",
        "PSVITA",
        "PSP",
        "CONTROLLER",
        "GENERAL",
        "DIALOGUE",
        "APPS"
    )
    val context = LocalContext.current
    val charChoice by viewModel.characterChoice.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isGodMode by viewModel.isGodMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(48.dp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.quiz_header),
                    color = PS_WHITE,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable {
                        // Secret way to toggle God Mode for debugging
                        viewModel.toggleGodMode()
                        Toast.makeText(context, "Debug Mode: ${if (!isGodMode) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
                    }
                )
                Text(
                    stringResource(R.string.select_category),
                    color = PS_WHITE.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }

            IconButton(onClick = { /* Help or Info could go here */ }, enabled = false) {
                if (isGodMode) Icon(Icons.Default.Build, null, tint = PS_GREEN)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Row with Quick button and character image
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.startQuiz("QUICK") },
                colors = ButtonDefaults.buttonColors(containerColor = PS_LIGHT_BLUE),
                shape = RoundedCornerShape(AppCornerRadius),
                modifier = Modifier
                    .weight(2f) // Takes 2/3 of space
                    .height(70.dp)
                    .border(1.dp, PS_WHITE.copy(alpha = 0.3f), RoundedCornerShape(AppCornerRadius))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.quick_quiz_label),
                        color = PS_WHITE,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        stringResource(R.string.quick_quiz_disclaimer),
                        color = PS_WHITE.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            // Character image on the right (No box/border now, takes 1/3 of space)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                charChoice?.let { choice ->
                    val statusSuffix = getStatusSuffix(1) // Default for selection screen
                    val resId = getQuizResource(choice, statusSuffix)
                    if (resId != 0) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = "Selected Character",
                            modifier = Modifier
                                .size(70.dp)
                                .clickable { onShowLargeChar() }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(categories) { category ->
                Button(
                    onClick = {
                        if (category == "PS6") Toast.makeText(
                            context,
                            "Coming soon...",
                            Toast.LENGTH_SHORT
                        ).show() else viewModel.startQuiz(category)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PS_LIGHT_BLUE),
                    shape = RoundedCornerShape(AppCornerRadius),
                    modifier = Modifier
                        .height(60.dp)
                        .border(1.dp, PS_WHITE.copy(alpha = 0.3f), RoundedCornerShape(AppCornerRadius))
                ) {
                    Text(
                        text = category,
                        color = PS_WHITE,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        IconButton(onClick = { viewModel.toggleMute() }) {
            Icon(
                if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Mute",
                tint = PS_WHITE
            )
        }
        Button(
            onClick = onExit,
            colors = ButtonDefaults.buttonColors(containerColor = PS_LIGHT_BLUE),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .border(1.dp, PS_WHITE.copy(alpha = 0.3f), RoundedCornerShape(AppCornerRadius)),
            shape = RoundedCornerShape(AppCornerRadius)
        ) {
            Text(
                stringResource(R.string.back_to_main),
                color = PS_WHITE,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun PlayingScreen(viewModel: QuizViewModel, onShowLargeChar: () -> Unit) {
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val score by viewModel.score.collectAsState()
    val answeredCount by viewModel.questionsAnswered.collectAsState()
    val questionsInRound by viewModel.questionsInRound.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val roundCount by viewModel.roundCount.collectAsState()
    val charChoice by viewModel.characterChoice.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    stringResource(R.string.score_label),
                    color = PS_WHITE.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ); Text(
                "$score",
                color = PS_WHITE,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.round_label),
                    color = PS_WHITE.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ); Text(
                "$roundCount",
                color = PS_WHITE,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.question_label),
                    color = PS_WHITE.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ); Text(
                "${answeredCount + 1}/$questionsInRound",
                color = PS_WHITE,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            }
            Column(horizontalAlignment = Alignment.End) {
                charChoice?.let { choice ->
                    val statusSuffix = getStatusSuffix(roundCount)
                    val resId = getQuizResource(choice, statusSuffix)
                    if (resId != 0) Image(
                        painter = painterResource(id = resId),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onShowLargeChar() }
                    )
                }
                Text(
                    "${timeLeft}s",
                    color = if (timeLeft < 10) PS_RED else PS_WHITE,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { (timeLeft / 42f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .border(0.5.dp, PS_WHITE.copy(alpha = 0.2f), RoundedCornerShape(AppCornerRadius)),
            color = if (timeLeft < 10) PS_RED else PS_WHITE,
            trackColor = PS_GRAY.copy(alpha = 0.3f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(32.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (isPaused) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.paused_label),
                        color = PS_WHITE,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    ); Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.togglePause() },
                        colors = ButtonDefaults.buttonColors(containerColor = PS_LIGHT_BLUE),
                        shape = RoundedCornerShape(AppCornerRadius)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            null
                        ); Spacer(modifier = Modifier.width(8.dp)); Text(stringResource(R.string.resume_button))
                    }
                    Spacer(modifier = Modifier.height(16.dp)); TextButton(onClick = { viewModel.exitToCategorySelection() }) {
                    Text(
                        stringResource(R.string.exit_quiz_button),
                        color = PS_WHITE.copy(alpha = 0.7f)
                    )
                }
                }
            } else {
                currentQuestion?.let { question ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PS_WHITE.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(AppCornerRadius),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    PS_WHITE.copy(alpha = 0.15f),
                                    RoundedCornerShape(AppCornerRadius)
                                )
                        ) {
                            Text(
                                text = question.text,
                                color = PS_WHITE,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            question.options.forEachIndexed { index, option ->
                                val letter = ('A' + index)
                                // val isSel = feedback.first == option
                                // val isCor = question.correctAnswer == option

                                Button(
                                    onClick = { viewModel.submitAnswer(option) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PS_LIGHT_BLUE),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .border(
                                            1.dp,
                                            PS_WHITE.copy(alpha = 0.2f),
                                            RoundedCornerShape(AppCornerRadius)
                                        ),
                                    shape = RoundedCornerShape(AppCornerRadius),
                                    enabled = feedback.first == null
                                ) {
                                    Text(
                                        "$letter: ${option.uppercase()}",
                                        color = PS_WHITE,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.SansSerif,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (feedback.first != null) {
                            val sIdx = question.options.indexOf(feedback.first)
                            val cIdx = question.options.indexOfFirst {
                                it.trim().equals(question.correctAnswer.trim(), ignoreCase = true)
                            }
                            val sLet = ('A' + sIdx).toChar()
                            val cLet = ('A' + cIdx).toChar()
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (feedback.second == true) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        tint = PS_GREEN,
                                        modifier = Modifier.size(64.dp)
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Cancel,
                                                null,
                                                tint = PS_RED,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            
                                            val incorrectText = if (question.type == QuestionType.TF) {
                                                stringResource(R.string.tf_answer_reply, question.correctAnswer)
                                            } else {
                                                stringResource(R.string.incorrect_answer_reply, sLet)
                                            }

                                            Text(
                                                incorrectText,
                                                color = PS_WHITE,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                        if (question.type == QuestionType.MC) {
                                            Text(
                                                stringResource(R.string.correct_answer_reply, cLet, question.correctAnswer),
                                                color = PS_WHITE,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        if (question.explanation.isNotBlank()) {
                                            Text(
                                                question.explanation,
                                                color = PS_WHITE.copy(alpha = 0.85f),
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.togglePause() }) {
                                Icon(
                                    Icons.Default.Pause,
                                    null,
                                    tint = PS_WHITE
                                )
                            }
                            Button(
                                onClick = { viewModel.skipQuestion() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                modifier = Modifier.border(
                                    1.dp,
                                    PS_WHITE.copy(alpha = 0.5f),
                                    RoundedCornerShape(AppCornerRadius)
                                ),
                                shape = RoundedCornerShape(AppCornerRadius),
                                enabled = feedback.first == null
                            ) {
                                Icon(
                                    Icons.Default.SkipNext,
                                    null,
                                    tint = PS_WHITE
                                ); Spacer(modifier = Modifier.width(8.dp)); Text(
                                "SKIP (-5s)",
                                color = PS_WHITE,
                                fontWeight = FontWeight.Bold
                            )
                            }
                            IconButton(onClick = { viewModel.exitToCategorySelection() }) {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    tint = PS_WHITE
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultsScreen(viewModel: QuizViewModel) {
    val score by viewModel.score.collectAsState()
    val category by viewModel.selectedCategory.collectAsState()
    val isPerfect by viewModel.isPerfectRound.collectAsState()
    val correct by viewModel.correctAnswers.collectAsState()
    val wrong by viewModel.wrongAnswers.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val questionsInRound by viewModel.questionsInRound.collectAsState()
    val answeredCount by viewModel.questionsAnswered.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isPerfect && answeredCount >= questionsInRound) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = "Trophy",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp)); Text(
                stringResource(R.string.perfect_round),
                color = Color(0xFFFFD700),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif
            ); Spacer(modifier = Modifier.height(24.dp))
        }
        val title =
            if (timeLeft == 0 && answeredCount < questionsInRound) stringResource(R.string.round_not_finished) else stringResource(R.string.round_complete)
        Text(
            title,
            color = PS_WHITE.copy(alpha = 0.7f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        ); Text(
        category,
        color = PS_WHITE,
        fontSize = 32.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.SansSerif
    )
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem(stringResource(R.string.correct_stat), "$correct", PS_GREEN); StatItem(
            stringResource(R.string.wrong_stat),
            "$wrong",
            PS_RED
        ); StatItem(stringResource(R.string.time_left_stat), "${timeLeft}s", if (timeLeft > 0) PS_WHITE else PS_RED)
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            stringResource(R.string.total_score_label),
            color = PS_WHITE.copy(alpha = 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        val percentage = if (questionsInRound > 0) (correct.toFloat() / questionsInRound * 100).toInt() else 0
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                "$score",
                color = PS_WHITE,
                fontSize = 80.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "($percentage%)",
                color = PS_WHITE.copy(alpha = 0.7f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (category != "QUICK" && answeredCount >= questionsInRound) {
            Spacer(modifier = Modifier.height(16.dp))
            val reviewText = when {
                percentage < 25 -> stringResource(R.string.review_score_1)
                percentage < 50 -> stringResource(R.string.review_score_2)
                percentage < 75 -> stringResource(R.string.review_score_3)
                percentage < 95 -> stringResource(R.string.review_score_4)
                else -> stringResource(R.string.review_score_5)
            }
            val starColor = when {
                percentage < 50 -> null
                percentage < 75 -> Color(0xFFCD7F32) // Bronze
                percentage < 95 -> Color(0xFFC0C0C0) // Silver
                else -> Color(0xFFFFD700) // Gold
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = reviewText,
                    color = PS_WHITE,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.SansSerif
                )
                if (starColor != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Review Star",
                        tint = starColor,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    if (wrong > 0 || timeLeft == 0 || category == "QUICK") viewModel.startQuiz(
                        category
                    ) else viewModel.nextRound()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PS_WHITE,
                    contentColor = PS_BLUE
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(AppCornerRadius)
            ) {
                Icon(
                    if (wrong > 0 || timeLeft == 0 || category == "QUICK") Icons.Default.Refresh else Icons.Default.SkipNext,
                    null
                ); Spacer(modifier = Modifier.width(8.dp)); Text(
                if (category == "QUICK") stringResource(R.string.retry_button) else if (wrong > 0 || timeLeft == 0) stringResource(R.string.retry_button) else stringResource(R.string.next_round_button),
                fontWeight = FontWeight.Bold
            )
            }
            Button(
                onClick = { viewModel.exitToCategorySelection() },
                colors = ButtonDefaults.buttonColors(containerColor = PS_LIGHT_BLUE),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(AppCornerRadius)
            ) { Text(stringResource(R.string.change_button), fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(16.dp)); TextButton(onClick = { viewModel.exitToCategorySelection() }) {
        Text(
            stringResource(R.string.quit_to_menu),
            color = PS_WHITE.copy(alpha = 0.7f)
        )
    }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            color = PS_WHITE.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

fun saveQuizResults(db: androidx.sqlite.db.SupportSQLiteDatabase?, score: Int, isPerfect: Boolean) {
    if (db == null) return
    try {
        db.execSQL("INSERT INTO user_db.scores (tableName, score) VALUES ('Quiz', $score)")
        if (isPerfect) {
            db.execSQL("INSERT INTO user_db.trophies (gameName, trophyType) VALUES ('Quiz', 'Perfect')")
        }
    } catch (e: Exception) {
        android.util.Log.e("Quiz", "Error saving results", e)
    }
}
