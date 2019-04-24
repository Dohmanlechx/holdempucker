package com.dohman.holdempucker.util

import androidx.appcompat.widget.AppCompatImageView
import com.dohman.holdempucker.cards.Card

class Constants {
    companion object {

        // Tags
        const val TAG_MAINACTIVITY = "DBG: MainActivity.kt"
        const val TAG_GAMEACTIVITY = "DBG: GameActivity.kt"
        const val TAG_GAMEVIEWMODEL = "DBG: GameViewModel.kt"

        // Booleans
        var isAnimationRunning = false
        var isOngoingGame = false // Set to true when all cards are laid out
        var restoringPlayers = false // Set to true when a team need to lay out new cards to fulfill
        var areTeamsReadyToStartPeriod = false // Set to true as soon as both teams are full in the very beginning

        // Objects
        val teamTop = arrayOfNulls<Card>(6)
        val teamBottom = arrayOfNulls<Card>(6)

        /*  Index 0 = Left forward | 1 = Center | 2 = Right forward
                        3 = Left defender | 4 = Right defender
                                    5 = Goalie                          */

        // Integers
        var period = 1
        var teamTopScore = 0
        var teamBottomScore = 0
        var possibleMovesIndexes = mutableListOf<Int>()

        // Whose turn
        var whoseTurn = WhoseTurn.TOP
        var whoseTeamStartedLastPeriod = WhoseTurn.BOTTOM

        // Views
        val teamBottomViews = mutableListOf<AppCompatImageView>()
        val teamTopViews = mutableListOf<AppCompatImageView>()

        // Cases
        val cases = mutableListOf<List<Int>>().apply {
            add(0, listOf(0, 1, 2, 3, 4, 5)) // ( 0, 1, 2)
            add(1, listOf(1, 2, 3, 4, 5)) // (1, 2, 3)
            add(2, listOf(0, 2, 3, 4, 5)) // (0, 2)
            add(3, listOf(0, 1, 3, 4, 5)) // (0, 1)
            add(4, listOf(0, 3, 4, 5)) // (0, 4)
            add(5, listOf(1, 3, 4, 5)) // (1)
            add(6, listOf(2, 3, 4, 5)) // (2, 3)
            add(7, listOf(3, 4, 5)) // (3, 4)
            // Below cases are not being checked, but needed so the attacker
            // can continue playing his turn.
            add(8, listOf(3, 5))
            add(9, listOf(4, 5))
            add(10, listOf(0, 3, 5))
            add(11, listOf(2, 4, 5))
            add(12, listOf(5))
        }

    }

    // Enums
    enum class WhoseTurn {
        BOTTOM, TOP;

        companion object {
            fun toggleTurn() {
                whoseTurn = if (whoseTurn == BOTTOM) TOP else BOTTOM
            }
        }
    }
}