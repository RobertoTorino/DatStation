package com.robertotorino.datstation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Close database first to release file locks
        DatabaseManager.closeDatabase()
        // Delete databases to ensure a clean state
        context.deleteDatabase("datstation.db")
        context.deleteDatabase("user_data.db")
    }

    @Test
    fun testDatabaseInitializationAndTables() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = DatabaseManager.getDatabase(context)

        // Verify that we can query the attached user_db tables
        val favoritesCursor = db.query("SELECT name FROM user_db.sqlite_master WHERE type='table' AND name='favorites'")
        assertTrue("Favorites table should exist", favoritesCursor.moveToFirst())
        favoritesCursor.close()

        val haveCursor = db.query("SELECT name FROM user_db.sqlite_master WHERE type='table' AND name='have'")
        assertTrue("Have table should exist", haveCursor.moveToFirst())
        haveCursor.close()

        val scoresCursor = db.query("SELECT name FROM user_db.sqlite_master WHERE type='table' AND name='scores'")
        assertTrue("Scores table should exist", scoresCursor.moveToFirst())
        scoresCursor.close()
    }

    @Test
    fun testFavoritesInsertionAndQuery() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = DatabaseManager.getDatabase(context)

        val tableName = "ps1_games"
        val gameId = "123"

        // Insert a favorite
        db.execSQL("INSERT INTO user_db.favorites (tableName, gameId) VALUES ('$tableName', '$gameId')")

        // Query it back
        val cursor = db.query("SELECT * FROM user_db.favorites WHERE tableName = '$tableName' AND gameId = '$gameId'")
        assertTrue("Should find the inserted favorite", cursor.moveToFirst())
        assertEquals(tableName, cursor.getString(cursor.getColumnIndexOrThrow("tableName")))
        assertEquals(gameId, cursor.getString(cursor.getColumnIndexOrThrow("gameId")))
        cursor.close()
    }

    @Test
    fun testScoresInsertionAndQuery() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = DatabaseManager.getDatabase(context)

        val tableName = "ps1_games"
        val score = 1000

        // Insert a score
        db.execSQL("INSERT INTO user_db.scores (tableName, score) VALUES ('$tableName', $score)")

        // Query it back
        val cursor = db.query("SELECT * FROM user_db.scores WHERE tableName = '$tableName'")
        assertTrue("Should find the inserted score", cursor.moveToFirst())
        assertEquals(score, cursor.getInt(cursor.getColumnIndexOrThrow("score")))
        cursor.close()
    }
}