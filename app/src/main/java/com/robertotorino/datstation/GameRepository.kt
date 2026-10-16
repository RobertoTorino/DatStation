package com.robertotorino.datstation

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase

data class GameResult(
    val table: String,
    val titleId: String,
    val region: String,
    val gameTitle: String,
    val url: String,
    val version: String,
    val contentId: String,
    val contentType: String,
    val distribution: String,
    var isFavorite: Boolean = false,
    var isHave: Boolean = false
)

/** Result of a catalogue search. [truncated] is true when [maxResults] was hit. */
data class SearchResult(
    val games: List<GameResult>,
    val truncated: Boolean
)

class GameRepository(private val db: SupportSQLiteDatabase) {

    companion object {
        /** Upper bound on rows a text search returns; keeps a one-letter query from loading the whole catalogue. */
        const val MAX_RESULTS = 500
    }

    fun getTablesWithColumns(): List<String> {
        val validTables = mutableListOf<String>()
        val requiredColumns = listOf("title_id", "game_title", "region", "url")

        db.query("SELECT name FROM sqlite_master WHERE type='table';").use { tableCursor ->
            while (tableCursor.moveToNext()) {
                val tableName = tableCursor.getString(0)
                if (tableName.startsWith("android_") || tableName.startsWith("sqlite_") ||
                    tableName == "favorites" || tableName == "have"
                ) continue

                val columns = mutableListOf<String>()
                db.query("PRAGMA table_info(`$tableName`);").use { pragmaCursor ->
                    while (pragmaCursor.moveToNext()) {
                        columns.add(pragmaCursor.getString(1))
                    }
                }
                if (columns.containsAll(requiredColumns)) {
                    validTables.add(tableName)
                }
            }
        }
        return validTables
    }

    fun getAllRegions(tables: List<String>): List<String> {
        val regions = mutableSetOf<String>()
        for (table in tables) {
            db.query("SELECT DISTINCT region FROM `$table` WHERE region IS NOT NULL AND region != ''")
                .use { cursor ->
                    while (cursor.moveToNext()) {
                        cursor.getString(0)?.let { regions.add(it) }
                    }
                }
        }
        return regions.sorted()
    }

    fun searchGames(
        query: String,
        tables: List<String>,
        tableFilter: String,
        regionFilter: String,
        distFilter: String,
        typeFilter: String,
        labels: FilterLabels,
        maxResults: Int = MAX_RESULTS
    ): SearchResult {
        val results = mutableListOf<GameResult>()
        val isRandomRequested = query.isBlank()
        var truncated = false

        for (table in SearchQueryBuilder.tablesToSearch(tableFilter, tables, labels)) {
            val remaining = maxResults - results.size
            if (!isRandomRequested && remaining <= 0) {
                truncated = true
                break
            }
            val statement = SearchQueryBuilder.build(
                table = table,
                query = query,
                tableFilter = tableFilter,
                regionFilter = regionFilter,
                distFilter = distFilter,
                typeFilter = typeFilter,
                labels = labels,
                // Ask for one extra row so a full page can be told apart from an exact fit.
                limit = remaining + 1
            )
            db.query(statement.sql, statement.args.toTypedArray()).use { cursor ->
                while (cursor.moveToNext()) {
                    if (!isRandomRequested && results.size >= maxResults) {
                        truncated = true
                        break
                    }
                    results.add(cursor.toGameResult())
                }
            }
        }
        return if (isRandomRequested) {
            SearchResult(results.shuffled().take(SearchQueryBuilder.RANDOM_LIMIT), truncated = false)
        } else {
            SearchResult(results, truncated)
        }
    }

    fun toggleFavorite(game: GameResult) {
        if (game.isFavorite) {
            db.execSQL(
                "DELETE FROM user_db.favorites WHERE tableName = ? AND gameId = ?",
                arrayOf(game.table, game.titleId)
            )
            game.isFavorite = false
        } else {
            db.execSQL(
                "INSERT OR REPLACE INTO user_db.favorites (tableName, gameId) VALUES (?, ?)",
                arrayOf(game.table, game.titleId)
            )
            game.isFavorite = true
        }
    }

    fun toggleHave(game: GameResult) {
        if (game.isHave) {
            db.execSQL(
                "DELETE FROM user_db.have WHERE tableName = ? AND gameId = ?",
                arrayOf(game.table, game.titleId)
            )
            game.isHave = false
        } else {
            db.execSQL(
                "INSERT OR REPLACE INTO user_db.have (tableName, gameId) VALUES (?, ?)",
                arrayOf(game.table, game.titleId)
            )
            game.isHave = true
        }
    }

    fun getTotalUserItems(): Int {
        var count = 0
        db.query("SELECT COUNT(*) FROM user_db.favorites").use { if (it.moveToFirst()) count += it.getInt(0) }
        db.query("SELECT COUNT(*) FROM user_db.have").use { if (it.moveToFirst()) count += it.getInt(0) }
        return count
    }

    fun getRandomGame(tables: List<String>): GameResult? {
        if (tables.isEmpty()) return null
        val randomTable = tables.random()
        return db.query(SearchQueryBuilder.randomGame(randomTable)).use { cursor ->
            if (cursor.moveToFirst()) cursor.toGameResult() else null
        }
    }

    fun getGlobalStats(tables: List<String>): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        var totalGames = 0
        var discCount = 0
        var digitalCount = 0

        for (table in tables) {
            db.query("SELECT COUNT(*), SUM(CASE WHEN distribution LIKE '%Disc%' THEN 1 ELSE 0 END) FROM `$table`")
                .use { cursor ->
                    if (cursor.moveToFirst()) {
                        val tableTotal = cursor.getInt(0)
                        val tableDisc = cursor.getInt(1)
                        totalGames += tableTotal
                        discCount += tableDisc
                        digitalCount += (tableTotal - tableDisc)
                        stats[table] = tableTotal
                    }
                }
        }
        stats["TOTAL"] = totalGames
        stats["DISC"] = discCount
        stats["DIGITAL"] = digitalCount
        return stats
    }

    fun getUserProfileValue(key: String): String? =
        db.query("SELECT value FROM user_db.user_profile WHERE key = ?", arrayOf(key)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    fun setUserProfileValue(key: String, value: String) {
        db.execSQL(
            "INSERT OR REPLACE INTO user_db.user_profile (key, value) VALUES (?, ?)",
            arrayOf(key, value)
        )
    }

    private fun Cursor.toGameResult() = GameResult(
        table = getString(0) ?: "Unknown",
        titleId = getString(1) ?: "",
        region = getString(2) ?: "",
        gameTitle = getString(3) ?: "",
        url = getString(4) ?: "",
        version = getString(5) ?: "",
        contentId = getString(6) ?: "",
        contentType = getString(7) ?: "",
        distribution = getString(8) ?: "",
        isFavorite = getInt(9) == 1,
        isHave = getInt(10) == 1
    )
}
