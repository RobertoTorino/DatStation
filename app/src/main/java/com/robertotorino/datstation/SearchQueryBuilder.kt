package com.robertotorino.datstation

/** The three pseudo-platform labels shown in the platform dropdown. */
data class FilterLabels(
    val all: String,
    val favorites: String,
    val have: String
)

/** A SQL statement plus its bind arguments, ready for `db.query`. */
data class SearchStatement(val sql: String, val args: List<Any?>)

/**
 * Builds the per-table catalogue search. Pure Kotlin so it can be unit tested
 * without a device.
 */
object SearchQueryBuilder {
    /** Number of rows per table when no search text is given ("random picks"). */
    const val RANDOM_LIMIT = 10

    /** How a result row is projected; used by [GameRepository] to read cursors. */
    private fun selectClause(table: String): String = """
        SELECT '$table', T.title_id, T.region, T.game_title, T.url, T.version, T.content_id, T.content_type, T.distribution,
        CASE WHEN F.gameId IS NOT NULL THEN 1 ELSE 0 END as isFav,
        CASE WHEN H.gameId IS NOT NULL THEN 1 ELSE 0 END as isHave
        FROM `$table` T
        LEFT JOIN user_db.favorites F ON F.tableName = '$table' AND F.gameId = T.title_id
        LEFT JOIN user_db.have H ON H.tableName = '$table' AND H.gameId = T.title_id
    """.trimIndent()

    /** Which tables a search over [tableFilter] touches. */
    fun tablesToSearch(tableFilter: String, tables: List<String>, labels: FilterLabels): List<String> =
        when (tableFilter) {
            labels.all, labels.favorites, labels.have -> tables
            else -> listOf(tableFilter)
        }

    /**
     * @param limit maximum rows to return from this table; `null` means no limit.
     *   Ignored when [query] is blank, where [RANDOM_LIMIT] random rows are returned.
     */
    fun build(
        table: String,
        query: String,
        tableFilter: String,
        regionFilter: String,
        distFilter: String,
        typeFilter: String,
        labels: FilterLabels,
        limit: Int? = null
    ): SearchStatement {
        val args = mutableListOf<Any?>()
        val conditions = mutableListOf<String>()

        if (query.isNotBlank()) {
            conditions.add("(T.game_title LIKE ? OR T.title_id LIKE ?)")
            args.add("%$query%")
            args.add("%$query%")
        }
        if (regionFilter != labels.all) {
            conditions.add("T.region = ?")
            args.add(regionFilter)
        }
        if (distFilter != labels.all) {
            if (distFilter == "Disc") {
                conditions.add("T.distribution LIKE '%Disc%'")
            } else {
                conditions.add("T.distribution = ?")
                args.add(distFilter)
            }
        }
        if (typeFilter != labels.all) {
            conditions.add("T.content_type = ?")
            args.add(typeFilter)
        }
        when (tableFilter) {
            labels.favorites -> conditions.add("F.gameId IS NOT NULL")
            labels.have -> conditions.add("H.gameId IS NOT NULL")
        }

        val where = if (conditions.isEmpty()) "" else " WHERE " + conditions.joinToString(" AND ")
        val tail = when {
            query.isBlank() -> " ORDER BY RANDOM() LIMIT $RANDOM_LIMIT"
            limit != null -> " LIMIT $limit"
            else -> ""
        }
        return SearchStatement(selectClause(table) + where + tail, args)
    }

    fun randomGame(table: String): String = selectClause(table) + " ORDER BY RANDOM() LIMIT 1"
}
