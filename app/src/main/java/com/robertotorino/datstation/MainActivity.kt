package com.robertotorino.datstation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.sqlite.db.SupportSQLiteDatabase
import com.robertotorino.datstation.EventManager.SpecialEvent
import com.robertotorino.datstation.ui.AppCornerRadius
import com.robertotorino.datstation.ui.BlueTheme
import com.robertotorino.datstation.ui.BronzeTheme
import com.robertotorino.datstation.ui.ChristmasTheme
import com.robertotorino.datstation.ui.ClassicTheme
import com.robertotorino.datstation.ui.GameFinderApp
import com.robertotorino.datstation.ui.LocalAppColors
import com.robertotorino.datstation.ui.RedTheme
import com.robertotorino.datstation.ui.SilverTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

private const val TAG = "MainActivity"

/** Screens reachable from the main activity. Kept as strings so they survive in saved state. */
private object Screen {
    const val GREETING = "Greeting"
    const val MAIN = "Main"
    const val GAME = "Game"
    const val RACER = "Racer"
    const val HIGH_SCORES = "HighScores"
    const val NEWS = "News"
    const val QUIZ = "Quiz"
}

/** Everything the UI needs once the database is open. */
private class AppSession(
    val db: SupportSQLiteDatabase,
    val repository: GameRepository,
    val characterChosen: Boolean
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)

        setContent {
            var currentEvent by remember { mutableStateOf(EventManager.getCurrentEvent()) }
            val isChristmasPeriod = currentEvent == SpecialEvent.CHRISTMAS

            var themeMode by remember {
                val savedMode = themePrefs.getInt("theme_mode", -1)
                mutableIntStateOf(if (savedMode == -1) (if (isChristmasPeriod) 3 else 2) else savedMode)
            }

            val currentColors = when (themeMode) {
                0 -> BronzeTheme
                1 -> ClassicTheme
                2 -> BlueTheme
                3 -> ChristmasTheme
                4 -> RedTheme
                5 -> SilverTheme
                else -> BlueTheme
            }

            // Opening the database copies the catalogue out of the APK on first
            // launch, so it runs off the main thread behind a loading screen.
            var session by remember { mutableStateOf<AppSession?>(null) }
            var openError by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                if (session != null) return@LaunchedEffect
                try {
                    session = withContext(Dispatchers.IO) {
                        val db = DatabaseManager.getDatabase(applicationContext)
                        val repository = GameRepository(db)
                        AppSession(
                            db = db,
                            repository = repository,
                            characterChosen = repository.getUserProfileValue("user_character") != null
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Database failed to open", e)
                    openError = e.message ?: e.javaClass.simpleName
                }
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
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = currentColors.background
                    ) {
                        val readySession = session
                        when {
                            openError != null -> StartupErrorScreen(openError!!)
                            readySession == null -> StartupLoadingScreen()
                            else -> AppContent(
                                session = readySession,
                                themeMode = themeMode,
                                onThemeSelect = { selectedMode ->
                                    themeMode = selectedMode
                                    themePrefs.edit { putInt("theme_mode", selectedMode) }
                                },
                                onEventToggle = {
                                    val events = SpecialEvent.entries
                                    val nextEvent = events[(currentEvent.ordinal + 1) % events.size]
                                    EventManager.debugEvent = nextEvent
                                    currentEvent = nextEvent
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppContent(
    session: AppSession,
    themeMode: Int,
    onThemeSelect: (Int) -> Unit,
    onEventToggle: () -> Unit
) {
    var currentScreen by rememberSaveable {
        mutableStateOf(if (session.characterChosen) Screen.GREETING else Screen.MAIN)
    }

    // Back from a game or sub-screen returns to the finder instead of leaving the app.
    BackHandler(enabled = currentScreen != Screen.MAIN) { currentScreen = Screen.MAIN }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            Screen.GREETING -> GreetingScreen(
                repository = session.repository,
                onContinue = { currentScreen = Screen.MAIN }
            )

            Screen.MAIN -> GameFinderApp(
                repository = session.repository,
                onOpenGame = { currentScreen = Screen.GAME },
                onOpenRacer = { currentScreen = Screen.RACER },
                onOpenQuiz = { currentScreen = Screen.QUIZ },
                onOpenHighScores = { currentScreen = Screen.HIGH_SCORES },
                onOpenNews = { currentScreen = Screen.NEWS },
                themeMode = themeMode,
                onThemeSelect = onThemeSelect,
                onEventToggle = onEventToggle
            )

            Screen.GAME -> NeonDiscGame(db = session.db, onExit = { currentScreen = Screen.MAIN })
            Screen.RACER -> NeonRacerGame(db = session.db, onExit = { currentScreen = Screen.MAIN })
            Screen.HIGH_SCORES -> HighScoresScreen(
                db = session.db,
                onBackClicked = { currentScreen = Screen.MAIN }
            )

            Screen.NEWS -> PlayStationUpdatesScreen(onBack = { currentScreen = Screen.MAIN })
            Screen.QUIZ -> QuizScreen(db = session.db, onExit = { currentScreen = Screen.MAIN })
        }
    }
}

@Composable
private fun StartupLoadingScreen() {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = colors.accent)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.loading_database),
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StartupErrorScreen(message: String) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.database_error_title),
            color = colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = colors.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GreetingScreen(
    repository: GameRepository,
    onContinue: () -> Unit
) {
    val currentColors = LocalAppColors.current
    var charKey by remember { mutableStateOf("moonshie") }
    LaunchedEffect(Unit) {
        charKey = withContext(Dispatchers.IO) {
            repository.getUserProfileValue("user_character") ?: "moonshie"
        }
    }
    val charRes =
        if (charKey == "roberto") R.drawable.roberto_profile_default else R.drawable.moonshie_profile_default

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingPrefix = when (hour) {
        in 0..11 -> stringResource(R.string.good_morning)
        in 12..16 -> stringResource(R.string.good_afternoon)
        in 17..20 -> stringResource(R.string.good_evening)
        else -> stringResource(R.string.good_night)
    }

    val charName = if (charKey == "roberto") "Roberto" else "Moonshie"

    val event = EventManager.getCurrentEvent()
    val finalGreeting = when (event) {
        SpecialEvent.MOONSHIE_BIRTHDAY -> stringResource(R.string.happy_birthday, "Moonshie")
        SpecialEvent.ROBERTO_BIRTHDAY -> stringResource(R.string.happy_birthday, "Roberto")
        SpecialEvent.MOONSHIE_BIRTHDAY_NEAR -> stringResource(R.string.birthday_reminder, "Moonshie", "May 1st")
        SpecialEvent.ROBERTO_BIRTHDAY_NEAR -> stringResource(R.string.birthday_reminder, "Roberto", "October 23rd")
        else -> "$greetingPrefix $charName!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(currentColors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = charRes),
            contentDescription = stringResource(R.string.profile_character),
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = finalGreeting,
            color = currentColors.textPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(containerColor = currentColors.boxBackground),
            shape = RoundedCornerShape(AppCornerRadius),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp)
        ) {
            Text(
                stringResource(R.string.continue_button),
                color = currentColors.boxText,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
