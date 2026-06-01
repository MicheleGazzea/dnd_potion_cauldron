package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "settings")
data class CampaignSettings(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "current_year") val currentYear: Int = 2023,
    @ColumnInfo(name = "current_month") val currentMonth: Int = 9,
    @ColumnInfo(name = "current_day") val currentDay: Int = 17,
    @ColumnInfo(name = "current_period") val currentPeriod: String = "NOON"
)

fun getReagentCost(name: String, craftingCost: Int): Int {
    return if (name in listOf(
        "Potion of Bear Endurance",
        "Elixir of Health",
        "Potion of Fire Breath",
        "Potion of Fire Giant Strength",
        "Potion of Hill Giant Strength",
        "Potion of Invulnerability",
        "Potion of Resistance",
        "Potion of Superior Healing"
    )) craftingCost / 2 else craftingCost
}

@Entity(tableName = "potions_library")
data class PotionRecipe(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rarity: String,
    @ColumnInfo(name = "crafting_cost") val craftingCost: Int,
    @ColumnInfo(name = "reagent_cost") val reagentCost: Int = getReagentCost(name, craftingCost),
    @ColumnInfo(name = "min_level") val minLevel: Int,
    val effects: String
)

@Entity(tableName = "active_brews")
data class ActiveBrew(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "potion_name") val potionName: String,
    @ColumnInfo(name = "start_year") val startYear: Int = 2023,
    @ColumnInfo(name = "start_month") val startMonth: Int = 9,
    @ColumnInfo(name = "start_day") val startDay: Int = 17,
    @ColumnInfo(name = "start_period") val startPeriod: String = "NOON",
    @ColumnInfo(name = "duration_days") val durationDays: Double,
    @ColumnInfo(name = "batch_quantity") val batchQuantity: Int,
    @ColumnInfo(name = "applied_residue_id") val appliedResidueId: Int? = null,
    @ColumnInfo(name = "applied_residue_name") val appliedResidueName: String? = null,
    @ColumnInfo(name = "applied_residue_type") val appliedResidueType: String? = null,
    @ColumnInfo(name = "d20_roll") val d20Roll: Int? = null,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    
    // Additional fields for rich details and logs
    @ColumnInfo(name = "d100_roll") val d100Roll: Int? = null,
    @ColumnInfo(name = "d100_effect") val d100Effect: String? = null,
    @ColumnInfo(name = "actual_duration_days") val actualDurationDays: Double = durationDays,
    @ColumnInfo(name = "final_quantity") val finalQuantity: Int? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

object CampaignCalendar {
    val periods = listOf("MORNING", "NOON", "AFTERNOON", "EVENING", "NIGHT")
    val periodLabels = listOf("Morning", "Noon", "Afternoon", "Evening", "Night")
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    fun getMonthName(monthValue: Int): String {
        return monthNames.getOrNull(monthValue - 1) ?: "September"
    }
    
    fun getPeriodLabel(period: String): String {
        return when (period.uppercase()) {
            "MORNING" -> "Morning"
            "NOON" -> "Noon"
            "AFTERNOON" -> "Afternoon"
            "EVENING" -> "Evening"
            "NIGHT" -> "Night"
            else -> period.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
    }

    fun getPeriodIndex(period: String): Int {
        val idx = periods.indexOf(period.uppercase())
        return if (idx == -1) 1 else idx
    }

    fun getLocalDate(year: Int, month: Int, day: Int): java.time.LocalDate {
        return try {
            java.time.LocalDate.of(year, month, day)
        } catch (e: Exception) {
            java.time.LocalDate.of(2023, 9, 17)
        }
    }

    fun getAbsolutePeriods(year: Int, month: Int, day: Int, period: String): Long {
        val localDate = getLocalDate(year, month, day)
        val epochDay = localDate.toEpochDay()
        val periodIdx = getPeriodIndex(period)
        return epochDay * 5 + periodIdx
    }

    fun getCalendarFromAbsolutePeriods(totalPeriods: Long): DateTimeState {
        // Handle negative absolute periods safely
        val epochDay = if (totalPeriods >= 0) {
            totalPeriods / 5
        } else {
            (totalPeriods - 4) / 5
        }
        val remainder = (totalPeriods % 5).toInt()
        val periodIdx = if (remainder >= 0) remainder else remainder + 5
        
        val localDate = java.time.LocalDate.ofEpochDay(epochDay)
        val period = periods.getOrElse(periodIdx) { "NOON" }
        return DateTimeState(localDate.year, localDate.monthValue, localDate.dayOfMonth, period)
    }
}

data class DateTimeState(
    val year: Int,
    val month: Int,
    val day: Int,
    val period: String
)

@Entity(tableName = "residue_inventory")
data class ResidueItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "potion_source") val potionSource: String,
    @ColumnInfo(name = "residue_type") val residueType: String, // "Standard" or "Masterwork Catalyst"
    @ColumnInfo(name = "is_used") val isUsed: Boolean = false,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)
