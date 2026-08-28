package com.robertotorino.datstation

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

class GameRepository(private val db: SupportSQLiteDatabase) {

    fun getTablesWithColumns(): List<String> {
        val validTables = mutableListOf<String>()
        val requiredColumns = listOf("title_id", "game_title", "region", "url")

        val tableCursor = db.query("SELECT name FROM sqlite_master WHERE type='table';")
        while (tableCursor.moveToNext()) {
            val tableName = tableCursor.getString(0)
            if (tableName.startsWith("android_") || tableName.startsWith("sqlite_") ||
                tableName == "favorites" || tableName == "have" || tableName == "dummy_table"
            ) continue

            val pragmaCursor = db.query("PRAGMA table_info(`$tableName`);")
            val columns = mutableListOf<String>()
            while (pragmaCursor.moveToNext()) {
                columns.add(pragmaCursor.getString(1))
            }
            pragmaCursor.close()

            if (columns.containsAll(requiredColumns)) {
                validTables.add(tableName)
            }
        }
        tableCursor.close()
        return validTables
    }

    fun getAllRegions(tables: List<String>): List<String> {
        val regions = mutableSetOf<String>()
        for (table in tables) {
            val cursor =
                db.query("SELECT DISTINCT region FROM `$table` WHERE region IS NOT NULL AND region != ''")
            while (cursor.moveToNext()) {
                val region = cursor.getString(0)
                if (region != null) regions.add(region)
            }
            cursor.close()
        }
        return regions.sorted()
    }

    fun searchGames(
        query: String,
        tables: List<String>,
        tableFilter: String,
        regionFilter: String,
        distFilter: String = "All",
        typeFilter: String = "All",
        filterAll: String = "All",
        filterFavorites: String = "Favorites",
        filterHave: String = "Have"
    ): List<GameResult> {
        val results = mutableListOf<GameResult>()

        val searchTables = when (tableFilter) {
            filterFavorites -> tables
            filterHave -> tables
            filterAll -> tables
            else -> listOf(tableFilter)
        }

        val isRandomRequested = query.isBlank()

        for (table in searchTables) {
            val bindArgs: MutableList<Any?> = mutableListOf()

            // JOIN with user_db.favorites and user_db.have
            val selectClause = """
                SELECT '$table', T.title_id, T.region, T.game_title, T.url, T.version, T.content_id, T.content_type, T.distribution,
                CASE WHEN F.gameId IS NOT NULL THEN 1 ELSE 0 END as isFav,
                CASE WHEN H.gameId IS NOT NULL THEN 1 ELSE 0 END as isHave
                FROM `$table` T
                LEFT JOIN user_db.favorites F ON F.tableName = '$table' AND F.gameId = T.title_id
                LEFT JOIN user_db.have H ON H.tableName = '$table' AND H.gameId = T.title_id
            """.trimIndent()

            val conditions = mutableListOf<String>()

            if (query.isNotBlank()) {
                conditions.add("(T.game_title LIKE ? OR T.title_id LIKE ?)")
                bindArgs.add("%$query%")
                bindArgs.add("%$query%")
            }

            if (regionFilter != filterAll) {
                conditions.add("T.region = ?")
                bindArgs.add(regionFilter)
            }

            if (distFilter != filterAll) {
                if (distFilter == "Disc") {
                    conditions.add("T.distribution LIKE '%Disc%'")
                } else {
                    conditions.add("T.distribution = ?")
                    bindArgs.add(distFilter)
                }
            }

            if (typeFilter != filterAll) {
                conditions.add("T.content_type = ?")
                bindArgs.add(typeFilter)
            }

            if (tableFilter == filterFavorites) {
                conditions.add("F.gameId IS NOT NULL")
            } else if (tableFilter == filterHave) {
                conditions.add("H.gameId IS NOT NULL")
            }

            val whereClause =
                if (conditions.isNotEmpty()) " WHERE " + conditions.joinToString(" AND ") else ""
            var sql = selectClause + whereClause

            if (isRandomRequested) {
                sql += " ORDER BY RANDOM() LIMIT 10"
            }

            val cursor = db.query(sql, bindArgs.toTypedArray())
            while (cursor.moveToNext()) {
                results.add(
                    GameResult(
                        table = cursor.getString(0) ?: "Unknown",
                        titleId = cursor.getString(1) ?: "",
                        region = cursor.getString(2) ?: "",
                        gameTitle = cursor.getString(3) ?: "",
                        url = cursor.getString(4) ?: "",
                        version = cursor.getString(5) ?: "",
                        contentId = cursor.getString(6) ?: "",
                        contentType = cursor.getString(7) ?: "",
                        distribution = cursor.getString(8) ?: "",
                        isFavorite = cursor.getInt(9) == 1,
                        isHave = cursor.getInt(10) == 1
                    )
                )
            }
            cursor.close()
        }
        return if (isRandomRequested) results.shuffled().take(10) else results
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
        val favCursor = db.query("SELECT COUNT(*) FROM user_db.favorites")
        if (favCursor.moveToFirst()) count += favCursor.getInt(0)
        favCursor.close()

        val haveCursor = db.query("SELECT COUNT(*) FROM user_db.have")
        if (haveCursor.moveToFirst()) count += haveCursor.getInt(0)
        haveCursor.close()

        return count
    }

    fun getRandomGame(tables: List<String>): GameResult? {
        if (tables.isEmpty()) return null
        val randomTable = tables.random()
        val sql = """
            SELECT '$randomTable', T.title_id, T.region, T.game_title, T.url, T.version, T.content_id, T.content_type, T.distribution,
            CASE WHEN F.gameId IS NOT NULL THEN 1 ELSE 0 END as isFav,
            CASE WHEN H.gameId IS NOT NULL THEN 1 ELSE 0 END as isHave
            FROM `$randomTable` T
            LEFT JOIN user_db.favorites F ON F.tableName = '$randomTable' AND F.gameId = T.title_id
            LEFT JOIN user_db.have H ON H.tableName = '$randomTable' AND H.gameId = T.title_id
            ORDER BY RANDOM() LIMIT 1
        """.trimIndent()

        val cursor = db.query(sql)
        var result: GameResult? = null
        if (cursor.moveToFirst()) {
            result = GameResult(
                table = cursor.getString(0) ?: "Unknown",
                titleId = cursor.getString(1) ?: "",
                region = cursor.getString(2) ?: "",
                gameTitle = cursor.getString(3) ?: "",
                url = cursor.getString(4) ?: "",
                version = cursor.getString(5) ?: "",
                contentId = cursor.getString(6) ?: "",
                contentType = cursor.getString(7) ?: "",
                distribution = cursor.getString(8) ?: "",
                isFavorite = cursor.getInt(9) == 1,
                isHave = cursor.getInt(10) == 1
            )
        }
        cursor.close()
        return result
    }

    fun getGlobalStats(tables: List<String>): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        var totalGames = 0
        var discCount = 0
        var digitalCount = 0

        for (table in tables) {
            val cursor =
                db.query("SELECT COUNT(*), SUM(CASE WHEN distribution LIKE '%Disc%' THEN 1 ELSE 0 END) FROM `$table`")
            if (cursor.moveToFirst()) {
                val tableTotal = cursor.getInt(0)
                val tableDisc = cursor.getInt(1)
                totalGames += tableTotal
                discCount += tableDisc
                digitalCount += (tableTotal - tableDisc)
                stats[table] = tableTotal
            }
            cursor.close()
        }
        stats["TOTAL"] = totalGames
        stats["DISC"] = discCount
        stats["DIGITAL"] = digitalCount

        return stats
    }

    fun getUserProfileValue(key: String): String? {
        val cursor = db.query("SELECT value FROM user_db.user_profile WHERE key = ?", arrayOf(key))
        var value: String? = null
        if (cursor.moveToFirst()) {
            value = cursor.getString(0)
        }
        cursor.close()
        return value
    }

    fun setUserProfileValue(key: String, value: String) {
        db.execSQL(
            "INSERT OR REPLACE INTO user_db.user_profile (key, value) VALUES (?, ?)",
            arrayOf(key, value)
        )
    }
}
