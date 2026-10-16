package com.robertotorino.datstation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryBuilderTest {

    private val labels = FilterLabels(all = "All", favorites = "Favorites", have = "Have")

    private fun build(
        query: String = "",
        tableFilter: String = "All",
        region: String = "All",
        dist: String = "All",
        type: String = "All",
        limit: Int? = null
    ) = SearchQueryBuilder.build("PS2", query, tableFilter, region, dist, type, labels, limit)

    @Test
    fun `blank query returns random rows and ignores the limit`() {
        val statement = build(query = "   ", limit = 50)

        assertTrue(statement.sql.endsWith("ORDER BY RANDOM() LIMIT ${SearchQueryBuilder.RANDOM_LIMIT}"))
        assertFalse(statement.sql.contains("WHERE"))
        assertTrue(statement.args.isEmpty())
    }

    @Test
    fun `text query binds title and id patterns and applies the limit`() {
        val statement = build(query = "gran turismo", limit = 25)

        assertTrue(statement.sql.contains("(T.game_title LIKE ? OR T.title_id LIKE ?)"))
        assertTrue(statement.sql.endsWith(" LIMIT 25"))
        assertEquals(listOf("%gran turismo%", "%gran turismo%"), statement.args)
    }

    @Test
    fun `filters are combined with AND and bound in order`() {
        val statement = build(query = "x", region = "EU", dist = "PSN", type = "GAME")

        val where = statement.sql.substringAfter(" WHERE ")
        assertEquals(
            "(T.game_title LIKE ? OR T.title_id LIKE ?) AND T.region = ? AND T.distribution = ? AND T.content_type = ?",
            where
        )
        assertEquals(listOf("%x%", "%x%", "EU", "PSN", "GAME"), statement.args)
    }

    @Test
    fun `disc filter matches any disc distribution without a bind argument`() {
        val statement = build(query = "x", dist = "Disc")

        assertTrue(statement.sql.contains("T.distribution LIKE '%Disc%'"))
        assertEquals(listOf("%x%", "%x%"), statement.args)
    }

    @Test
    fun `favorites and have filters restrict on the joined user tables`() {
        assertTrue(build(query = "x", tableFilter = "Favorites").sql.contains("F.gameId IS NOT NULL"))
        assertTrue(build(query = "x", tableFilter = "Have").sql.contains("H.gameId IS NOT NULL"))
        assertFalse(build(query = "x", tableFilter = "PS2").sql.contains("IS NOT NULL AND"))
    }

    @Test
    fun `pseudo platforms search every table and a real platform only itself`() {
        val tables = listOf("PS1", "PS2", "PS3")

        assertEquals(tables, SearchQueryBuilder.tablesToSearch("All", tables, labels))
        assertEquals(tables, SearchQueryBuilder.tablesToSearch("Favorites", tables, labels))
        assertEquals(tables, SearchQueryBuilder.tablesToSearch("Have", tables, labels))
        assertEquals(listOf("PS2"), SearchQueryBuilder.tablesToSearch("PS2", tables, labels))
    }

    @Test
    fun `projection reads the table name and user flags for the cursor mapping`() {
        val sql = build(query = "x").sql

        assertTrue(sql.startsWith("SELECT 'PS2', T.title_id, T.region, T.game_title"))
        assertTrue(sql.contains("END as isFav"))
        assertTrue(sql.contains("END as isHave"))
        assertTrue(sql.contains("LEFT JOIN user_db.favorites F ON F.tableName = 'PS2'"))
    }
}
