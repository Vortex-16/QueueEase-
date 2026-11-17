package com.example.domain.engine

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt

data class CrowdConfidenceResult(
    val confidencePercent: Int,
    val freshnessMinutes: Int,
    val freshnessLabel: String,
    val confidenceGrade: String,
    val isStale: Boolean
)

object CrowdConfidenceEngine {

    /**
     * Calculates exponential time-decay confidence score for community crowd reports.
     * Half-life is 30 minutes. Staff verification sets confidence to 100%.
     */
    fun computeConfidence(
        minutesAgo: Int,
        isStaffVerified: Boolean = false,
        upvotes: Int = 1
    ): CrowdConfidenceResult {
        if (isStaffVerified) {
            return CrowdConfidenceResult(
                confidencePercent = 100,
                freshnessMinutes = minutesAgo,
                freshnessLabel = if (minutesAgo <= 1) "Verified just now" else "Staff verified ${minutesAgo}m ago",
                confidenceGrade = "Official Verified",
                isStale = minutesAgo > 90
            )
        }

        // Half-life decay: lambda = ln(2) / 30 = 0.0231
        val lambda = 0.0231
        val decay = exp(-lambda * minutesAgo.toDouble())
        val upvoteBonus = (upvotes.coerceAtMost(10) - 1) * 0.02
        val rawScore = (decay * 90.0 + upvoteBonus * 100.0).coerceIn(15.0, 98.0)
        val score = rawScore.roundToInt()

        val freshnessLabel = when {
            minutesAgo <= 1 -> "Updated just now"
            minutesAgo < 60 -> "Updated ${minutesAgo} min ago"
            minutesAgo < 120 -> "Updated 1 hour ago"
            else -> "Updated ${minutesAgo / 60} hours ago"
        }

        val (grade, isStale) = when {
            score >= 80 -> Pair("High Confidence", false)
            score >= 50 -> Pair("Moderate Confidence", false)
            else -> Pair("May Be Outdated", true)
        }

        return CrowdConfidenceResult(
            confidencePercent = score,
            freshnessMinutes = minutesAgo,
            freshnessLabel = freshnessLabel,
            confidenceGrade = grade,
            isStale = isStale
        )
    }
}
