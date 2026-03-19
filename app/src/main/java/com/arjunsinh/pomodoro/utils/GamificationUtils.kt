package com.arjunsinh.pomodoro.utils

import kotlin.math.pow

object GamificationUtils {

    fun calculateXpEarned(durationMillis: Long): Int {
        val minutes = durationMillis / 60000.0
        val basePoints = minutes * Constants.BASE_POINTS_PER_MINUTE
        // Decay system removed: XP earned no longer decreases with level.
        return basePoints.toInt().coerceAtLeast(1)
    }

    /**
     * Returns the total XP required to reach the start of a given level.
     * Level 1 starts at 0 XP.
     */
    fun getTotalXpForLevel(level: Int): Int {
        if (level <= 1) return 0
        // Sum of arithmetic progression: n/2 * [2a + (n-1)d]
        // where a = LEVEL_BASE_COST, d = LEVEL_SLOPE, n = level - 1
        val n = level - 1
        return (n * (2 * Constants.LEVEL_BASE_COST + (n - 1) * Constants.LEVEL_SLOPE)) / 2
    }

    /**
     * Returns the cost (XP required) to progress THROUGH the current level to the next.
     */
    fun getXpRequiredForNextLevel(currentLevel: Int): Int {
        return Constants.LEVEL_BASE_COST + (currentLevel - 1) * Constants.LEVEL_SLOPE
    }

    fun calculateLevelFromTotalXp(totalXp: Int): Int {
        var level = 1
        while (getTotalXpForLevel(level + 1) <= totalXp) {
            level++
        }
        return level
    }
}
