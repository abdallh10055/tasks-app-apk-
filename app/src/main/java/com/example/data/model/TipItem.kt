package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TipItem(
    @Json(name = "id") val id: Int,
    @Json(name = "nutrition_tip") val nutritionTip: String,
    @Json(name = "stealth_exercise") val stealthExercise: String,
    @Json(name = "habit_tracker_focus") val habitTrackerFocus: String
) {
    // Helper to get pure titles or content parsed from [Theme] details
    val nutritionTitle: String
        get() = nutritionTip.substringAfter("[").substringBefore("]", "Nutrition Tip")

    val nutritionBody: String
        get() = if (nutritionTip.contains("] ")) nutritionTip.substringAfter("] ") else nutritionTip

    val exerciseTitle: String
        get() = stealthExercise.substringAfter("[").substringBefore("]", "Stealth Workout")

    val exerciseBody: String
        get() = if (stealthExercise.contains("] ")) stealthExercise.substringAfter("] ") else stealthExercise

    val habitTitle: String
        get() = habitTrackerFocus.substringAfter("[").substringBefore("]", "Daily Focus")

    val habitBody: String
        get() = if (habitTrackerFocus.contains("] ")) habitTrackerFocus.substringAfter("] ") else habitTrackerFocus
}
