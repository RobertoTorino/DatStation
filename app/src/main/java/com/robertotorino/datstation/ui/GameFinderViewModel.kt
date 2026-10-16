package com.robertotorino.datstation.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robertotorino.datstation.FilterLabels
import com.robertotorino.datstation.GameRepository
import com.robertotorino.datstation.GameResult
import com.robertotorino.datstation.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds the search state of the finder screen so it survives rotation and
 * other configuration changes. All repository calls run on the IO dispatcher.
 */
class GameFinderViewModel(
    private val repository: GameRepository,
    private val labels: FilterLabels
) : ViewModel() {

    var searchQuery by mutableStateOf("")
    var selectedPlatform by mutableStateOf(labels.all)
    var selectedRegion by mutableStateOf(labels.all)
    var distFilter by mutableStateOf(labels.all)
    var typeFilter by mutableStateOf(labels.all)

    var platformOptions by mutableStateOf(listOf(labels.all, labels.favorites, labels.have))
        private set
    var regionOptions by mutableStateOf(listOf(labels.all))
        private set

    var searchResults by mutableStateOf(listOf<GameResult>())
    var lastSearchTruncated by mutableStateOf(false)
        private set
    var globalStats by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var randomGame by mutableStateOf<GameResult?>(null)
        private set

    /** Real catalogue tables, without the All / Favorites / Have pseudo-entries. */
    val catalogueTables: List<String>
        get() = platformOptions.filter { it != labels.all && it != labels.favorites && it != labels.have }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val platforms = repository.getTablesWithColumns()
            val regions = repository.getAllRegions(platforms)
            platformOptions = listOf(labels.all, labels.favorites, labels.have) + platforms
            regionOptions = listOf(labels.all) + regions
            randomGame = repository.getRandomGame(platforms)
            globalStats = repository.getGlobalStats(platforms)
        }
    }

    /** Runs the search with the current filters and publishes the results. */
    suspend fun search(): SearchResult {
        val result = withContext(Dispatchers.IO) {
            repository.searchGames(
                query = searchQuery,
                tables = catalogueTables,
                tableFilter = selectedPlatform,
                regionFilter = selectedRegion,
                distFilter = distFilter,
                typeFilter = typeFilter,
                labels = labels
            )
        }
        searchResults = result.games
        lastSearchTruncated = result.truncated
        return result
    }

    /** Clears the query, all filters and the results, and draws a new featured game. */
    fun reset() {
        searchQuery = ""
        selectedPlatform = labels.all
        selectedRegion = labels.all
        distFilter = labels.all
        typeFilter = labels.all
        searchResults = emptyList()
        lastSearchTruncated = false
        viewModelScope.launch(Dispatchers.IO) {
            randomGame = repository.getRandomGame(catalogueTables)
        }
    }

    fun showFeaturedGame() {
        randomGame?.let { searchResults = listOf(it) }
    }

    /**
     * Toggles favorite or collection membership for [game], updates the list
     * in place and returns the new total of user items (for status ranks).
     */
    suspend fun toggle(game: GameResult, favorite: Boolean): Int = withContext(Dispatchers.IO) {
        if (favorite) repository.toggleFavorite(game) else repository.toggleHave(game)
        searchResults = searchResults.map {
            if (it.titleId == game.titleId && it.table == game.table) {
                if (favorite) it.copy(isFavorite = game.isFavorite) else it.copy(isHave = game.isHave)
            } else it
        }
        repository.getTotalUserItems()
    }

    suspend fun totalUserItems(): Int = withContext(Dispatchers.IO) { repository.getTotalUserItems() }

    suspend fun userProfileValue(key: String): String? =
        withContext(Dispatchers.IO) { repository.getUserProfileValue(key) }

    suspend fun setUserProfileValue(key: String, value: String) =
        withContext(Dispatchers.IO) { repository.setUserProfileValue(key, value) }
}
