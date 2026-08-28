package com.robertotorino.datstation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameResultTest {

    @Test
    fun `test GameResult initialization and property access`() {
        val game = GameResult(
            table = "PS3_GAMES",
            titleId = "BLES01234",
            region = "EU",
            gameTitle = "Test Game",
            url = "http://example.com",
            version = "1.00",
            contentId = "CID123",
            contentType = "Game",
            distribution = "Digital",
            isFavorite = false
        )

        assertEquals("PS3_GAMES", game.table)
        assertEquals("BLES01234", game.titleId)
        assertEquals("EU", game.region)
        assertEquals("Test Game", game.gameTitle)
        assertEquals("http://example.com", game.url)
        assertEquals("1.00", game.version)
        assertEquals("CID123", game.contentId)
        assertEquals("Game", game.contentType)
        assertEquals("Digital", game.distribution)
        assertFalse(game.isFavorite)
    }

    @Test
    fun `test GameResult copy handles favorite toggle correctly`() {
        val game = GameResult(
            table = "PS3_GAMES",
            titleId = "BLES01234",
            region = "EU",
            gameTitle = "Test Game",
            url = "http://example.com",
            version = "1.00",
            contentId = "CID123",
            contentType = "Game",
            distribution = "Digital",
            isFavorite = false
        )

        val favoritedGame = game.copy(isFavorite = true)

        assertTrue(favoritedGame.isFavorite)
        // Ensure other properties remain the same
        assertEquals(game.titleId, favoritedGame.titleId)
        assertEquals(game.gameTitle, favoritedGame.gameTitle)
    }
}
