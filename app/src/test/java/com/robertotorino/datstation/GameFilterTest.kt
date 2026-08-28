package com.robertotorino.datstation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameFilterTest {

    private val testGames = listOf(
        GameResult(table="T1", titleId="ID1", region="EU", gameTitle="Game A", url="", version="", contentId="", distribution="Disc", contentType="Game"),
        GameResult(table="T1", titleId="ID2", region="US", gameTitle="Game B", url="", version="", contentId="", distribution="PSN", contentType="DLC"),
        GameResult(table="T1", titleId="ID3", region="JP", gameTitle="Game C", url="", version="", contentId="", distribution="Disc", contentType="Theme"),
        GameResult(table="T1", titleId="ID4", region="EU", gameTitle="Game D", url="", version="", contentId="", distribution="PSN", contentType="Game")
    )

    @Test
    fun `test filtering by distribution`() {
        val distFilter = "Disc"
        val filtered = testGames.filter { game ->
            game.distribution.contains(distFilter, ignoreCase = true)
        }
        
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.distribution == "Disc" })
    }

    @Test
    fun `test filtering by content type`() {
        val typeFilter = "Game"
        val filtered = testGames.filter { game ->
            game.contentType.contains(typeFilter, ignoreCase = true)
        }
        
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.contentType == "Game" })
    }

    @Test
    fun `test multiple filters applied together`() {
        val distFilter = "PSN"
        val typeFilter = "DLC"
        
        val filtered = testGames.filter { game ->
            (game.distribution.contains(distFilter, ignoreCase = true)) &&
            (game.contentType.contains(typeFilter, ignoreCase = true))
        }
        
        assertEquals(1, filtered.size)
        assertEquals("Game B", filtered[0].gameTitle)
    }

    @Test
    fun `test All filter returns everything`() {

        val filtered = testGames.filter { _ ->
            true
        }
        
        assertEquals(4, filtered.size)
    }
}
