package com.robertotorino.datstation

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.robertotorino.datstation.EventManager.SpecialEvent
import com.robertotorino.datstation.ui.AppCornerRadius
import com.robertotorino.datstation.ui.BlueTheme
import com.robertotorino.datstation.ui.BronzeTheme
import com.robertotorino.datstation.ui.ChristmasTheme
import com.robertotorino.datstation.ui.ClassicTheme
import com.robertotorino.datstation.ui.GameFinderApp
import com.robertotorino.datstation.ui.LocalAppColors
import java.util.Calendar

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = DatabaseManager.getDatabase(this)
        val repository = GameRepository(db)

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
                4 -> com.robertotorino.datstation.ui.RedTheme
                5 -> com.robertotorino.datstation.ui.SilverTheme
                else -> BlueTheme
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
                        val charChosen = remember {
                            repository.getUserProfileValue("user_character") != null
                        }
                        var currentScreen by remember {
                            mutableStateOf(if (charChosen) "Greeting" else "Main")
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            when (currentScreen) {
                                "Greeting" -> {
                                    GreetingScreen(
                                        repository = repository,
                                        onContinue = { currentScreen = "Main" }
                                    )
                                }

                                "Main" -> {
                                    GameFinderApp(
                                        repository = repository,
                                        onOpenGame = { currentScreen = "Game" },
                                        onOpenRacer = { currentScreen = "Racer" },
                                        onOpenQuiz = { currentScreen = "Quiz" },
                                        onOpenHighScores = { currentScreen = "HighScores" },
                                        onOpenNews = { currentScreen = "News" },
                                        themeMode = themeMode,
                                        onThemeSelect = { selectedMode ->
                                            themeMode = selectedMode
                                            themePrefs.edit { putInt("theme_mode", themeMode) }
                                        },
                                        onEventToggle = {
                                            val events =
                                                SpecialEvent.entries.toTypedArray()
                                            val nextEvent =
                                                events[(currentEvent.ordinal + 1) % events.size]
                                            EventManager.debugEvent = nextEvent
                                            currentEvent = nextEvent
                                        }
                                    )
                                }

                                "Game" -> {
                                    NeonDiscGame(db = db, onExit = { currentScreen = "Main" })
                                }

                                "Racer" -> {
                                    NeonRacerGame(db = db, onExit = { currentScreen = "Main" })
                                }

                                "HighScores" -> {
                                    HighScoresScreen(
                                        db = db,
                                        onBackClicked = { currentScreen = "Main" }
                                    )
                                }

                                "News" -> {
                                    PlayStationUpdatesScreen(
                                        onBack = { currentScreen = "Main" }
                                    )
                                }

                                "Quiz" -> {
                                    QuizScreen(db = db, onExit = { currentScreen = "Main" })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GreetingScreen(
    repository: GameRepository,
    onContinue: () -> Unit
) {
    val currentColors = LocalAppColors.current
    val charKey = remember { repository.getUserProfileValue("user_character") ?: "moonshie" }
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
        SpecialEvent.MOONSHIE_BIRTHDAY -> {
            stringResource(R.string.happy_birthday, "Moonshie")
        }
        SpecialEvent.ROBERTO_BIRTHDAY -> {
            stringResource(R.string.happy_birthday, "Roberto")
        }
        SpecialEvent.MOONSHIE_BIRTHDAY_NEAR -> {
            stringResource(R.string.birthday_reminder, "Moonshie", "May 1st")
        }
        SpecialEvent.ROBERTO_BIRTHDAY_NEAR -> {
            stringResource(R.string.birthday_reminder, "Roberto", "October 23rd")
        }
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
            contentDescription = "Profile Character",
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
