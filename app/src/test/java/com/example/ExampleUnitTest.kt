package com.example

import com.example.data.model.TipItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testDayCalculation_exactFormula() {
        val totalTips = 1000
        val startDateEpoch = 10000L

        // Day 0 elapsed -> Day 1
        var todayEpoch = 10000L
        var daysDiff = todayEpoch - startDateEpoch
        var dayNumber = ((daysDiff % totalTips).toInt()) + 1
        assertEquals(1, dayNumber)

        // Day 1 elapsed -> Day 2
        todayEpoch = 10001L
        daysDiff = todayEpoch - startDateEpoch
        dayNumber = ((daysDiff % totalTips).toInt()) + 1
        assertEquals(2, dayNumber)

        // Day 999 elapsed -> Day 1000
        todayEpoch = 10000L + 999L
        daysDiff = todayEpoch - startDateEpoch
        dayNumber = ((daysDiff % totalTips).toInt()) + 1
        assertEquals(1000, dayNumber)

        // Day 1000 elapsed -> wraps back to Day 1
        todayEpoch = 10000L + 1000L
        daysDiff = todayEpoch - startDateEpoch
        dayNumber = ((daysDiff % totalTips).toInt()) + 1
        assertEquals(1, dayNumber)
    }

    @Test
    fun testTipItem_parsingTitles() {
        val tip = TipItem(
            id = 42,
            nutritionTip = "[Hydration & Focus] Drink water with lemon. Sip slowly.",
            stealthExercise = "[Seated Core Vacuum] Hold for 15s. Breathe gently.",
            habitTrackerFocus = "[20-20-20 Vision Defense] Look 20 feet away."
        )

        assertEquals("Hydration & Focus", tip.nutritionTitle)
        assertEquals("Drink water with lemon. Sip slowly.", tip.nutritionBody)
        assertEquals("Seated Core Vacuum", tip.exerciseTitle)
        assertEquals("Hold for 15s. Breathe gently.", tip.exerciseBody)
        assertEquals("20-20-20 Vision Defense", tip.habitTitle)
        assertEquals("Look 20 feet away.", tip.habitBody)
    }
}
