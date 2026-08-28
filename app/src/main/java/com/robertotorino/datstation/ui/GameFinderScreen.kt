package com.robertotorino.datstation.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import com.robertotorino.datstation.BuildConfig
import com.robertotorino.datstation.GameRepository
import com.robertotorino.datstation.GameResult
import com.robertotorino.datstation.R
import com.robertotorino.datstation.ui.theme.DataStationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun statusKeyForTotal(total: Int): String = when {
    total >= 500 -> "Diamond"
    total >= 250 -> "Platinum"
    total >= 100 -> "Gold"
    total >= 25 -> "Silver"
    total >= 10 -> "Bronze"
    else -> "None"
}

@StringRes
private fun statusLabelRes(statusKey: String): Int = when (statusKey) {
    "Diamond" -> R.string.status_diamond
    "Platinum" -> R.string.status_platinum
    "Gold" -> R.string.status_gold
    "Silver" -> R.string.status_silver
    "Bronze" -> R.string.status_bronze
    else -> R.string.status_none
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GameFinderApp(
    repository: GameRepository,
    onOpenGame: () -> Unit,
    onOpenRacer: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenHighScores: () -> Unit,
    onOpenNews: () -> Unit,
    themeMode: Int,
    onThemeSelect: (Int) -> Unit,
    onEventToggle: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    val currentColors = LocalAppColors.current

    val filterAll = stringResource(R.string.filter_all)
    val filterFavorites = stringResource(R.string.filter_favorites)
    val filterHave = stringResource(R.string.filter_have)

    val prefs = context.getSharedPreferences("user_status", Context.MODE_PRIVATE)
    var userStatusKey by remember {
        mutableStateOf(prefs.getString("current_status", "None") ?: "None")
    }
    val userStatus = stringResource(statusLabelRes(userStatusKey))

    var showStatusDialog by remember { mutableStateOf<String?>(null) }
    var showLargeStatus by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCharSelection by remember { mutableStateOf(false) }
    var showProfilesDialog by remember { mutableStateOf(false) }
    var showIndividualProfile by remember { mutableStateOf<String?>(null) }
    var onboardingVideoResId by remember { mutableStateOf<Int?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var platformOptions by remember { mutableStateOf(listOf(filterAll, filterFavorites, filterHave)) }
    var regionOptions by remember { mutableStateOf(listOf(filterAll)) }
    var selectedPlatform by remember { mutableStateOf(filterAll) }
    var selectedRegion by remember { mutableStateOf(filterAll) }
    var searchResults by remember { mutableStateOf(listOf<GameResult>()) }
    var platformExpanded by remember { mutableStateOf(false) }
    var regionExpanded by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var globalStats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var randomGame by remember { mutableStateOf<GameResult?>(null) }

    var showSettingsMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    var distFilter by remember { mutableStateOf(filterAll) }
    var typeFilter by remember { mutableStateOf(filterAll) }

    val filterOptions = listOf(
        filterAll,
        "APP",
        "APP_UPDATE",
        "ARCADE",
        "AVATAR",
        "BETA",
        "BETA_UPDATE",
        "BUNDLE",
        "DEMO",
        "DEMO_UPDATE",
        "DLC",
        "GAME",
        "KIOSK_IDU",
        "LICENSE",
        "MANUAL",
        "MINI",
        "MUSIC",
        "NOT_APPROVED",
        "PS_CLASSIC",
        "PS2_CLASSIC",
        "PRESS_KIT",
        "PROMO",
        "PSM",
        "PSM_DEMO",
        "SOUNDTRACK",
        "THEME",
        "TRIAL",
        "TRIAL_UPDATE",
        "UMD_MUSIC",
        "UMD_VIDEO",
        "UPDATE",
        "VIRTUAL_COIN",
        "VIDEO",
        "VR_DEMO",
        "VR_DEMO_UPDATE",
        "VR_GAME",
        "VR_GAME_UPDATE",
        "WALLPAPER",
        "WEBTV",
    )
    val filterLabels = mapOf(
        "APP" to stringResource(R.string.filter_app),
        "APP_UPDATE" to stringResource(R.string.filter_appu),
        "ARCADE" to stringResource(R.string.filter_arc),
        "AVATAR" to stringResource(R.string.filter_av),
        "BETA" to stringResource(R.string.filter_bet),
        "BETA_UPDATE" to stringResource(R.string.filter_betu),
        "BUNDLE" to stringResource(R.string.filter_bun),
        "DEMO" to stringResource(R.string.filter_dem),
        "DEMO_UPDATE" to stringResource(R.string.filter_demu),
        "DLC" to stringResource(R.string.filter_dlc),
        "GAME" to stringResource(R.string.filter_gam),
        "KIOSK_IDU" to stringResource(R.string.filter_kio),
        "LICENSE" to stringResource(R.string.filter_lic),
        "MANUAL" to stringResource(R.string.filter_man),
        "MINI" to stringResource(R.string.filter_mini),
        "MUSIC" to stringResource(R.string.filter_music),
        "NOT_APPROVED" to stringResource(R.string.filter_noa),
        "PS_CLASSIC" to stringResource(R.string.filter_p1c),
        "PS2_CLASSIC" to stringResource(R.string.filter_p2c),
        "PRESS_KIT" to stringResource(R.string.filter_prk),
        "PROMO" to stringResource(R.string.filter_prm),
        "PSM" to stringResource(R.string.filter_psm),
        "PSM_DEMO" to stringResource(R.string.filter_psmd),
        "SOUNDTRACK" to stringResource(R.string.filter_stk),
        "THEME" to stringResource(R.string.filter_thm),
        "TRIAL" to stringResource(R.string.filter_trl),
        "TRIAL_UPDATE" to stringResource(R.string.filter_trlu),
        "UMD_MUSIC" to stringResource(R.string.filter_umdm),
        "UMD_VIDEO" to stringResource(R.string.filter_umdv),
        "UPDATE" to stringResource(R.string.filter_upd),
        "VIRTUAL_COIN" to stringResource(R.string.filter_vc),
        "VIDEO" to stringResource(R.string.filter_vdo),
        "VR_DEMO" to stringResource(R.string.filter_vrd),
        "VR_DEMO_UPDATE" to stringResource(R.string.filter_vrdu),
        "VR_GAME" to stringResource(R.string.filter_vrg),
        "VR_GAME_UPDATE" to stringResource(R.string.filter_vrgu),
        "WALLPAPER" to stringResource(R.string.filter_wal),
        "WEBTV" to stringResource(R.string.filter_web),
    )
    var filterExpanded by remember { mutableStateOf(false) }

    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = currentColors.boxText,
        unfocusedTextColor = currentColors.boxText,
        focusedBorderColor = currentColors.boxBorder,
        unfocusedBorderColor = currentColors.boxBorder,
        focusedLabelColor = currentColors.boxText,
        unfocusedLabelColor = currentColors.boxText,
        cursorColor = currentColors.boxText,
        focusedTrailingIconColor = currentColors.boxText,
        unfocusedTrailingIconColor = currentColors.boxText,
        focusedContainerColor = currentColors.boxBackground,
        unfocusedContainerColor = currentColors.boxBackground
    )

    @Composable
    fun CompactToolButton(
        icon: ImageVector,
        contentDescription: String,
        size: Dp = 44.dp,
        onClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(currentColors.boxBackground, RoundedCornerShape(AppCornerRadius))
                .border(1.dp, currentColors.boxBorder, RoundedCornerShape(AppCornerRadius))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = currentColors.boxText,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }

    LaunchedEffect(Unit) {
        val total = withContext(Dispatchers.IO) { repository.getTotalUserItems() }
        val calculatedStatus = statusKeyForTotal(total)

        if (userStatusKey != calculatedStatus) {
            userStatusKey = calculatedStatus
            prefs.edit { putString("current_status", calculatedStatus) }
        }

        val savedChar = withContext(Dispatchers.IO) { repository.getUserProfileValue("user_character") }
        if (savedChar == null) {
            showCharSelection = true
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (spokenText != null) {
                val command = spokenText.lowercase()
                when {
                    command.contains("store") -> context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://store.playstation.com/".toUri()
                        )
                    )

                    command.contains("app") -> launchApp(context, "com.scee.psxandroid")
                    command.contains("remote") -> launchApp(context, "com.playstation.remoteplay")
                    command.contains("psp") || command.contains("ppsspp") -> {
                        if (!launchApp(context, "org.ppsspp.ppssppgold")) launchApp(
                            context,
                            "org.ppsspp.ppsspp"
                        )
                    }

                    command.contains("duck") -> launchApp(context, "com.github.stenzek.duckstation")
                    command.contains("aether") -> launchApp(context, "xyz.aethersx2.android")
                    command.contains("vita") -> launchApp(context, "org.vita3k.emulator")
                    command.contains("play game") || command.contains("start game") || command.contains(
                        "neon"
                    ) -> onOpenGame()
                    command.contains("race") || command.contains("car") -> onOpenRacer()
                    command.contains("score") || command.contains("leaderboard") || command.contains("hall of fame") -> onOpenHighScores()
                    command.contains("news") -> onOpenNews()
                    command.contains("quiz") -> onOpenQuiz()

                    else -> {
                        searchQuery = spokenText
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                searchResults = repository.searchGames(
                                    searchQuery,
                                    platformOptions.filter { it != filterAll && it != filterFavorites && it != filterHave },
                                    selectedPlatform,
                                    selectedRegion,
                                    distFilter,
                                    typeFilter,
                                    filterAll,
                                    filterFavorites,
                                    filterHave
                                )
                            }
                            Toast.makeText(
                                context,
                                if (searchResults.isEmpty()) context.getString(R.string.no_results) else context.getString(
                                    R.string.found_results,
                                    searchResults.size
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val platforms = repository.getTablesWithColumns()
            val regions = repository.getAllRegions(platforms)
            platformOptions = listOf(filterAll, filterFavorites, filterHave) + platforms
            regionOptions = listOf(filterAll) + regions
            randomGame = repository.getRandomGame(platforms)
            globalStats = repository.getGlobalStats(platforms)
        }
    }

    if (showStatsDialog) {
        StatsDialog(stats = globalStats, onDismiss = { showStatsDialog = false })
    }

    if (showInfoDialog) RegionCodesInfoDialog(onDismiss = { showInfoDialog = false })

    showStatusDialog?.let { status ->
        StatusAchievementDialog(status = status, onDismiss = { showStatusDialog = null })
    }

    if (showLargeStatus) {
        LargeStatusDialog(status = userStatus, onDismiss = { showLargeStatus = false })
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    if (showHistoryDialog) {
        HistoryDialog(onDismiss = { showHistoryDialog = false })
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentThemeMode = themeMode,
            onThemeSelected = onThemeSelect,
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showCharSelection) {
        CharacterSelectionDialog { choice ->
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    repository.setUserProfileValue("user_character", choice)
                }
                showCharSelection = false
                
                // Trigger onboarding video
                onboardingVideoResId = if (choice == "moonshie") R.raw.moonshie_profile_video else R.raw.roberto_profile_video
            }
        }
    }

    if (onboardingVideoResId != null) {
        FullscreenVideoPlayer(videoResId = onboardingVideoResId!!) {
            onboardingVideoResId = null
            // Show welcome dialog after video finishes
            val hasSeenWelcome = prefs.getBoolean("has_seen_welcome", false)
            if (!hasSeenWelcome) {
                showStatusDialog = "Welcome"
                prefs.edit { putBoolean("has_seen_welcome", true) }
            }
        }
    }

    if (showProfilesDialog) {
        CharacterProfilesDialog(
            onSelectionChange = { newChar ->
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        repository.setUserProfileValue("user_character", newChar)
                    }
                }
            },
            onViewProfile = { character ->
                showIndividualProfile = character
            },
            onDismiss = { showProfilesDialog = false }
        )
    }

    if (showIndividualProfile != null) {
        IndividualProfileDialog(
            character = showIndividualProfile!!,
            onDismiss = { showIndividualProfile = null }
        )
    }

    if (showSettingsMenu) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsMenu = false },
            sheetState = sheetState,
            containerColor = currentColors.background,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .background(currentColors.boxBorder, RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(R.string.settings_button),
                    style = MaterialTheme.typography.headlineSmall,
                    color = currentColors.textPrimary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            showSettingsMenu = false
                            showThemeDialog = true 
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(currentColors.boxBackground, RoundedCornerShape(AppCornerRadius))
                            .border(1.dp, currentColors.boxBorder, RoundedCornerShape(AppCornerRadius))
                            .combinedClickable(
                                onClick = { onThemeSelect((themeMode + 1) % 4) },
                                onLongClick = {
                                    if (BuildConfig.DEBUG) {
                                        onEventToggle()
                                        Toast.makeText(
                                            context,
                                            "Debug: Event Toggled",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = currentColors.boxText
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        stringResource(R.string.switch_theme),
                        color = currentColors.textPrimary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                SettingsItem(
                    text = stringResource(R.string.about_button),
                    drawableRes = R.drawable.platinum_status,
                    showBox = false,
                    currentColors = currentColors,
                    onClick = {
                        showSettingsMenu = false
                        showAboutDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(currentColors.background)
            .systemBarsPadding()
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Search spans columns 1 and 2
            Box(modifier = Modifier.weight(2f)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { 
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            color = Color.White.copy(alpha = 0.8f)
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(AppCornerRadius),
                    colors = customTextFieldColors.copy(
                        unfocusedPlaceholderColor = Color.White,
                        focusedPlaceholderColor = Color.White
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(
                                    RecognizerIntent.EXTRA_PROMPT,
                                    context.getString(R.string.search_placeholder)
                                )
                            }
                            try {
                                voiceLauncher.launch(intent)
                            } catch (_: Exception) {
                            }
                        }) {
                            Icon(
                                Icons.Default.Mic,
                                stringResource(R.string.voice_search),
                                tint = currentColors.boxText
                            )
                        }
                    }
                )
            }

            // Tools occupy column 3
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactToolButton(
                    icon = Icons.Default.RestartAlt,
                    contentDescription = stringResource(R.string.clear_button),
                    size = 56.dp, 
                    onClick = {
                        searchQuery = ""
                        selectedPlatform = filterAll
                        selectedRegion = filterAll
                        distFilter = filterAll
                        typeFilter = filterAll
                        searchResults = emptyList()
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                randomGame = repository.getRandomGame(
                                    platformOptions.filter {
                                        it != filterAll && it != filterFavorites && it != filterHave
                                    }
                                )
                            }
                        }
                    }
                )

                StatusIcon(
                    status = userStatus,
                    modifier = Modifier.size(56.dp),
                    onClick = { showLargeStatus = true },
                    onLongClick = {
                        val statusKeys =
                            listOf("None", "Bronze", "Silver", "Gold", "Platinum", "Diamond")
                        val currentKey = prefs.getString("current_status", "None") ?: "None"
                        val nextIndex = (statusKeys.indexOf(currentKey) + 1) % statusKeys.size
                        val nextKey = statusKeys[nextIndex]

                        userStatusKey = nextKey
                        prefs.edit { putString("current_status", nextKey) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = platformExpanded,
                onExpandedChange = { platformExpanded = !platformExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedPlatform,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(stringResource(R.string.platform_label), fontSize = 10.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(platformExpanded) },
                    modifier = Modifier
                        .menuAnchor(
                            ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            true
                        )
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(AppCornerRadius),
                    colors = customTextFieldColors,
                    maxLines = 1,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                )
                ExposedDropdownMenu(
                    expanded = platformExpanded,
                    onDismissRequest = { platformExpanded = false },
                    modifier = Modifier
                        .background(currentColors.boxBackground)
                        .border(1.dp, currentColors.boxBorder, RoundedCornerShape(AppCornerRadius))
                ) {
                    platformOptions.forEach {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    it,
                                    color = currentColors.boxText,
                                    fontSize = 11.sp
                                )
                            },
                            onClick = { selectedPlatform = it; platformExpanded = false },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = regionExpanded,
                onExpandedChange = { regionExpanded = !regionExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedRegion,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(stringResource(R.string.region_label), fontSize = 10.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(regionExpanded) },
                    modifier = Modifier
                        .menuAnchor(
                            ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            true
                        )
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(AppCornerRadius),
                    colors = customTextFieldColors,
                    maxLines = 1,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                )
                ExposedDropdownMenu(
                    expanded = regionExpanded,
                    onDismissRequest = { regionExpanded = false },
                    modifier = Modifier
                        .background(currentColors.boxBackground)
                        .border(1.dp, currentColors.boxBorder, RoundedCornerShape(AppCornerRadius))
                ) {
                    regionOptions.forEach {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    it,
                                    color = currentColors.boxText,
                                    fontSize = 11.sp
                                )
                            },
                            onClick = { selectedRegion = it; regionExpanded = false },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = filterExpanded,
                onExpandedChange = { filterExpanded = !filterExpanded },
                modifier = Modifier.weight(1f)
            ) {
                val currentFilter = if (typeFilter != filterAll) typeFilter else distFilter
                val labelText = if (distFilter == filterAll && typeFilter == filterAll) {
                    "-"
                } else {
                    filterLabels[currentFilter] ?: currentFilter
                }

                OutlinedTextField(
                    value = labelText,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("-", fontSize = 10.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(filterExpanded) },
                    modifier = Modifier
                        .menuAnchor(
                            ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            true
                        )
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(AppCornerRadius),
                    colors = customTextFieldColors,
                    maxLines = 1,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                )
                ExposedDropdownMenu(
                    expanded = filterExpanded,
                    onDismissRequest = { filterExpanded = false },
                    modifier = Modifier
                        .background(currentColors.boxBackground)
                        .border(1.dp, currentColors.boxBorder, RoundedCornerShape(AppCornerRadius))
                ) {
                    filterOptions.forEach { option ->
                        val itemLabel = if (option == filterAll) option else filterLabels[option] ?: option
                        DropdownMenuItem(
                            text = {
                                Text(
                                    itemLabel,
                                    color = currentColors.boxText,
                                    fontSize = 11.sp
                                )
                            },
                            onClick = {
                                when (option) {
                                    filterAll -> {
                                        distFilter = filterAll
                                        typeFilter = filterAll
                                    }
                                    "DISC", "PSN" -> {
                                        distFilter = option
                                        typeFilter = filterAll
                                    }
                                    else -> {
                                        typeFilter = option
                                        distFilter = filterAll
                                    }
                                }
                                filterExpanded = false
                            },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactToolButton(
                icon = Icons.Default.Search,
                contentDescription = stringResource(R.string.search_placeholder),
                onClick = {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            searchResults = repository.searchGames(
                                searchQuery,
                                platformOptions.filter {
                                    it != filterAll && it != filterFavorites && it != filterHave
                                },
                                selectedPlatform,
                                selectedRegion,
                                distFilter,
                                typeFilter,
                                filterAll,
                                filterFavorites,
                                filterHave
                            )
                        }
                        
                        val isRandom = searchQuery.isBlank()
                        val message = if (searchResults.isEmpty()) {
                            context.getString(R.string.no_results)
                        } else if (isRandom) {
                            val totalCount = when (selectedPlatform) {
                                filterAll -> globalStats["TOTAL"] ?: 0
                                else -> globalStats[selectedPlatform] ?: searchResults.size // Fallback
                            }
                            context.getString(R.string.random_results, totalCount)
                        } else {
                            context.getString(R.string.found_results, searchResults.size)
                        }
                        
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            )

            CompactToolButton(
                icon = Icons.Default.VideogameAsset,
                contentDescription = stringResource(R.string.play_neon_disc),
                onClick = { onOpenGame() }
            )

            CompactToolButton(
                icon = Icons.Default.DirectionsCar,
                contentDescription = stringResource(R.string.play_neon_racer),
                onClick = { onOpenRacer() }
            )

            CompactToolButton(
                icon = Icons.Default.QuestionAnswer,
                contentDescription = "Quiz",
                onClick = { onOpenQuiz() }
            )

            CompactToolButton(
                icon = Icons.Default.EmojiEvents,
                contentDescription = stringResource(R.string.high_scores),
                onClick = { onOpenHighScores() }
            )

            CompactToolButton(
                icon = Icons.AutoMirrored.Filled.Help,
                contentDescription = stringResource(R.string.help_button),
                onClick = { showHelpDialog = true }
            )

            CompactToolButton(
                icon = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings_button),
                onClick = { showSettingsMenu = true }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsShortcut(drawableRes = R.drawable.no_status) { showStatsDialog = true }
            SettingsShortcut(icon = Icons.Default.History) { showHistoryDialog = true }
            SettingsShortcut(icon = Icons.Default.History) {
                context.startActivity(Intent(context, com.robertotorino.datstation.SystemSoftwareHistoryActivity::class.java))
            }
            SettingsShortcut(icon = Icons.Default.Info) { showInfoDialog = true }
            SettingsShortcut(icon = Icons.AutoMirrored.Filled.MenuBook) {
                openUrl(context, "https://manuals.playstation.net/document/en/products.html")
            }
            SettingsShortcut(icon = Icons.Default.VideogameAsset) {
                context.startActivity(Intent(context, com.robertotorino.datstation.AppCatalogActivity::class.java))
            }
            SettingsShortcut(drawableRes = R.drawable.moonshie_blue_quiz) { showProfilesDialog = true }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppShortcut(
                letter = "P",
                label = stringResource(R.string.label_psn_store),
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://store.playstation.com/".toUri()
                        )
                    )
                })
            AppShortcut(
                letter = "L",
                label = "PSN App",
                onClick = { launchApp(context, "com.scee.psxandroid") })
            AppShortcut(
                letter = "A",
                label = "PS News",
                onClick = { onOpenNews() })
            AppShortcut(
                letter = "Y",
                label = stringResource(R.string.label_aether),
                onClick = { launchApp(context, "xyz.aethersx2.android") })
            AppShortcut(
                letter = "I",
                label = stringResource(R.string.label_duck),
                onClick = { launchApp(context, "com.github.stenzek.duckstation") })
            AppShortcut(
                letter = "N",
                label = stringResource(R.string.label_ppsspp),
                onClick = {
                    if (!launchApp(context, "org.ppsspp.ppssppgold")) launchApp(
                        context,
                        "org.ppsspp.ppsspp"
                    )
                })
            AppShortcut(
                letter = "G",
                label = stringResource(R.string.label_vita3k),
                onClick = { launchApp(context, "org.vita3k.emulator") })
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (searchResults.isEmpty() && randomGame != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.featured_game),
                    color = currentColors.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val currentKey = userStatusKey
                val statusResId = when (currentKey) {
                    "Bronze" -> R.drawable.bronze_status
                    "Silver" -> R.drawable.silver_status
                    "Gold" -> R.drawable.gold_status
                    "Platinum" -> R.drawable.platinum_status
                    "Diamond" -> R.drawable.diamond_status
                    else -> null
                }

                if (statusResId != null) {
                    Image(
                        painter = painterResource(id = statusResId),
                        contentDescription = stringResource(R.string.status_trophy),
                        modifier = Modifier
                            .size(240.dp)
                            .padding(bottom = 16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.VideogameAsset,
                        contentDescription = stringResource(R.string.random_game_icon),
                        tint = currentColors.accent.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(120.dp)
                            .padding(bottom = 16.dp)
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { searchResults = listOf(randomGame!!) },
                    shape = RoundedCornerShape(AppCornerRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = currentColors.cardBackground,
                        contentColor = currentColors.textPrimary
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = randomGame!!.gameTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = currentColors.textPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${randomGame!!.table} | ${randomGame!!.region} | ${randomGame!!.titleId}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = currentColors.accent,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.tap_details),
                    color = currentColors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        val filteredResults = searchResults.filter { game ->
            (distFilter == filterAll || game.distribution.contains(distFilter, ignoreCase = true)) &&
                    (typeFilter == filterAll || game.contentType.contains(
                        typeFilter,
                        ignoreCase = true
                    ))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(filteredResults) { index, game ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppCornerRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = currentColors.cardBackground,
                        contentColor = currentColors.textPrimary
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = game.gameTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = currentColors.textPrimary,
                                modifier = Modifier.clickable {
                                    val searchUrl =
                                        "https://www.google.com/search?q=${java.net.URLEncoder.encode(game.gameTitle, "UTF-8")}"
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            searchUrl.toUri()
                                        )
                                    )
                                }
                            )
                            Row {
                                Text(
                                    text = game.titleId,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = currentColors.accent,
                                    modifier = Modifier.clickable {
                                        coroutineScope.launch {
                                            clipboard.setClipEntry(
                                                ClipEntry(
                                                    ClipData.newPlainText(
                                                        context.getString(R.string.clip_label_game_id),
                                                        game.titleId
                                                    )
                                                )
                                            )
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.id_copied, game.titleId),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                                val vDisp =
                                    if (game.version.isBlank() || game.version == "—") "v1.00" else "v${game.version}"
                                Text(
                                    text = " | ${game.region} | $vDisp | ${stringResource(R.string.distribution_label)}: ${game.distribution}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = currentColors.textSecondary
                                )
                            }
                            Text(
                                text = "${stringResource(R.string.platform_label)}: ${game.table} | ${stringResource(R.string.type_label)}: ${game.contentType} | ${stringResource(R.string.cid_label)}: ${game.contentId}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = currentColors.textSecondary
                            )
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (game.url.isNotBlank() && game.url != "—") {
                                    Text(
                                        text = stringResource(R.string.tap_to_open),
                                        color = currentColors.accent,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.clickable {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    game.url.toUri()
                                                )
                                            )
                                        })
                                    Text(
                                        text = stringResource(R.string.copy_url),
                                        color = currentColors.textSecondary,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.clickable {
                                            coroutineScope.launch {
                                                clipboard.setClipEntry(
                                                    ClipEntry(
                                                        ClipData.newPlainText(
                                                            context.getString(R.string.clip_label_game_url),
                                                            game.url
                                                        )
                                                    )
                                                )
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.url_copied),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        })
                                }
                                Text(
                                    text = stringResource(R.string.share),
                                    color = currentColors.accent,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.clickable {
                                        val shareText = context.getString(
                                            R.string.share_format,
                                            game.gameTitle,
                                            game.titleId,
                                            game.region,
                                            game.version,
                                            game.contentType,
                                            game.url
                                        )
                                        context.startActivity(
                                            Intent.createChooser(
                                                Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                    type = "text/plain"
                                                },
                                                null
                                            )
                                        )
                                    })
                            }
                        }
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val wasHave = game.isHave
                                        withContext(Dispatchers.IO) { repository.toggleHave(game) }
                                        searchResults = searchResults.map {
                                            if (it.titleId == game.titleId && it.table == game.table) it.copy(
                                                isHave = !wasHave
                                            ) else it
                                        }
                                        Toast.makeText(
                                            context,
                                            if (!wasHave) context.getString(R.string.added_to_collection) else context.getString(
                                                R.string.removed_from_collection
                                            ),
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        val total =
                                            withContext(Dispatchers.IO) { repository.getTotalUserItems() }
                                        val newStatusKey = statusKeyForTotal(total)
                                        val currentKey =
                                            prefs.getString("current_status", "None") ?: "None"
                                        if (newStatusKey != currentKey) {
                                            userStatusKey = newStatusKey
                                            prefs.edit { putString("current_status", newStatusKey) }
                                            if (newStatusKey != "None") showStatusDialog = newStatusKey
                                        }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (game.isHave) Icons.Filled.Album else Icons.Outlined.Album,
                                    contentDescription = stringResource(R.string.have_label),
                                    tint = if (game.isHave) currentColors.accent else currentColors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val wasFavorite = game.isFavorite
                                        withContext(Dispatchers.IO) { repository.toggleFavorite(game) }
                                        searchResults = searchResults.map {
                                            if (it.titleId == game.titleId && it.table == game.table) it.copy(
                                                isFavorite = !wasFavorite
                                            ) else it
                                        }
                                        Toast.makeText(
                                            context,
                                            if (!wasFavorite) context.getString(R.string.added_to_favorites) else context.getString(
                                                R.string.removed_from_favorites
                                            ),
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        val total =
                                            withContext(Dispatchers.IO) { repository.getTotalUserItems() }
                                        val newStatusKey = statusKeyForTotal(total)
                                        val currentKey =
                                            prefs.getString("current_status", "None") ?: "None"
                                        if (newStatusKey != currentKey) {
                                            userStatusKey = newStatusKey
                                            prefs.edit { putString("current_status", newStatusKey) }
                                            if (newStatusKey != "None") showStatusDialog = newStatusKey
                                        }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (game.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = stringResource(R.string.favorite_label),
                                    tint = if (game.isFavorite) Color(0xFFFFD700) else currentColors.textPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                if (index < filteredResults.size - 1) {
                    HorizontalDivider(
                        color = Color.DarkGray.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    text: String,
    icon: ImageVector? = null,
    drawableRes: Int? = null,
    showBox: Boolean = true,
    currentColors: AppThemeColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBox) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(currentColors.boxBackground, RoundedCornerShape(AppCornerRadius))
                    .border(1.dp, currentColors.boxBorder, RoundedCornerShape(AppCornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = currentColors.boxText
                    )
                } else if (drawableRes != null) {
                    Image(
                        painter = painterResource(id = drawableRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            if (drawableRes != null) {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = currentColors.textPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = currentColors.textPrimary,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun launchApp(context: Context, packageName: String): Boolean {
    val pm = context.packageManager
    val launchIntent = pm.getLaunchIntentForPackage(packageName)
    return if (launchIntent != null) {
        context.startActivity(launchIntent)
        true
    } else {
        Toast.makeText(context, context.getString(R.string.app_not_installed), Toast.LENGTH_SHORT)
            .show()
        false
    }
}

@Preview(name = "English", locale = "en")
@Preview(name = "Spanish", locale = "es")
@Preview(name = "Chinese", locale = "zh")
@Preview(name = "Russian", locale = "ru")
@Composable
fun GameFinderScreenPreview() {
    DataStationTheme {
        val colors = ClassicTheme
        CompositionLocalProvider(LocalAppColors provides colors) {
            // Placeholder preview container
        }
    }
}
