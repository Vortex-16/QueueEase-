package com.example.domain.engine

import java.util.Calendar
import kotlin.math.max
import kotlin.math.roundToInt

data class WaitTimeEstimate(
    val minMinutes: Int,
    val maxMinutes: Int,
    val formattedRange: String,
    val peopleAhead: Int,
    val activeCounters: Int,
    val congestionFactor: Double
)

object WaitTimeEstimator {

    fun estimateWait(
        peopleAhead: Int,
        activeCounters: Int,
        avgServiceDurationMinutes: Int = 6,
        serviceComplexityMultiplier: Double = 1.0
    ): WaitTimeEstimate {
        val safeCounters = max(1, activeCounters)
        
        // Time of day multiplier (e.g. morning and post-lunch rush)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeOfDayMultiplier = when (hour) {
            in 10..12 -> 1.25 // Morning peak
            in 13..14 -> 1.35 // Lunch rush / shift change
            in 15..17 -> 1.10 // Afternoon steady
            else -> 1.0
        }

        val effectiveFactor = timeOfDayMultiplier * serviceComplexityMultiplier
        val rawBase = (peopleAhead.toDouble() * avgServiceDurationMinutes / safeCounters) * effectiveFactor

        val minMin = max(1, (rawBase * 0.85).roundToInt())
        val maxMin = max(minMin + 2, (rawBase * 1.25).roundToInt())

        val formatted = if (peopleAhead <= 0) {
            "Next in line (< 3 min)"
        } else if (minMin == maxMin) {
            "~$minMin min"
        } else {
            "$minMin–$maxMin min"
        }

        return WaitTimeEstimate(
            minMinutes = minMin,
            maxMinutes = maxMin,
            formattedRange = formatted,
            peopleAhead = peopleAhead,
            activeCounters = safeCounters,
            congestionFactor = effectiveFactor
        )
    }
}
