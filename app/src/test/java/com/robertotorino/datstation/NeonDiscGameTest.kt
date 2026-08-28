package com.robertotorino.datstation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NeonDiscGameTest {

    private val columns = 10
    private val rows = 16

    // Helper function to generate an empty grid for testing
    private fun createEmptyGrid(): List<MutableList<ConsoleDisc?>> {
        return List(rows) { MutableList(columns) { null } }
    }

    @Test
    fun `Win Condition - 8 GOOD discs horizontally and vertically returns true`() {
        val grid = createEmptyGrid()

        // Place 8 PS1 (GOOD) discs in the bottom row
        for (x in 0 until 8) {
            grid[rows - 1][x] = ConsoleDisc.PS1
        }
        // Place 8 PS1 (GOOD) discs in the first column
        for (y in 0 until 8) {
            grid[y][0] = ConsoleDisc.PS1
        }

        val isWin = checkWinCondition(grid, columns, rows, StageTheme.NEON_DISCS)
        assertTrue("Game should register a win for 8 good discs horizontally AND vertically", isWin)
    }

    @Test
    fun `Win Condition - 7 GOOD discs horizontally returns false`() {
        val grid = createEmptyGrid()

        // Place only 7 PS1 (GOOD) discs in the bottom row
        for (x in 0 until 7) {
            grid[rows - 1][x] = ConsoleDisc.PS1
        }

        val isWin = checkWinCondition(grid, columns, rows, StageTheme.NEON_DISCS)
        assertFalse("Game should NOT register a win for only 7 discs", isWin)
    }

    @Test
    fun `Loss Condition - Standard Theme - 2 ENEMY discs touching horizontally returns true`() {
        val grid = createEmptyGrid()

        // Place 2 XBOX (ENEMY) discs next to each other
        grid[15][0] = ConsoleDisc.XBOX
        grid[15][1] = ConsoleDisc.XBOX

        val isLoss = checkLossCondition(grid, StageTheme.NEON_DISCS, isGodMode = false)
        assertTrue("Game should register a loss if 2 enemies touch", isLoss)
    }

    @Test
    fun `Loss Condition - Box Fill Theme - Single ENEMY disc returns true`() {
        val grid = createEmptyGrid()

        // Place just ONE enemy disc anywhere
        grid[5][5] = ConsoleDisc.GAMECUBE

        // Using the BOX_FILL theme where ANY enemy is an instant loss
        val isLoss = checkLossCondition(grid, StageTheme.BOX_FILL, isGodMode = false)
        assertTrue("In Box Fill stage, a single enemy should trigger Game Over", isLoss)
    }
}