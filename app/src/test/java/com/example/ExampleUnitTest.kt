package com.example

import com.example.data.CampaignCalendar
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testCampaignCalendarCalculation() {
    // A potion takes 4 days to craft and starts on September 17, 2023 at Noon.
    val startYear = 2023
    val startMonth = 9
    val startDay = 17
    val startPeriod = "NOON"
    
    // Get absolute start periods
    val startAbs = CampaignCalendar.getAbsolutePeriods(startYear, startMonth, startDay, startPeriod)
    
    // 4 days is 20 periods
    val durationDays = 4.0
    val durationPeriods = Math.round(durationDays * 5.0).toLong()
    
    // Calculate final period
    val finishAbs = startAbs + durationPeriods
    val finishState = CampaignCalendar.getCalendarFromAbsolutePeriods(finishAbs)
    
    // Verify it finishes exactly 4 days later on September 21 at Noon
    assertEquals(2023, finishState.year)
    assertEquals(9, finishState.month)
    assertEquals(21, finishState.day)
    assertEquals("NOON", finishState.period)
  }
}
