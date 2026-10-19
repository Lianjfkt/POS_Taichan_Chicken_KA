package com.kapos.mobile.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateRangeFilterTest {

    @Test
    fun testTodayTimeRange_startsAtMidnight_endsAtEndOfDay() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 19, 14, 30, 0)
        val testNow = cal.timeInMillis

        val filter = DateRangeFilter(preset = DateFilterPreset.TODAY)
        val (start, end) = filter.getTimeRange(testNow)

        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, startCal.get(Calendar.MONTH))
        assertEquals(19, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))
        assertEquals(0, startCal.get(Calendar.SECOND))

        assertEquals(19, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))
    }

    @Test
    fun testCustomDateRange_singleDay_coversWholeDay() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 15, 10, 0, 0)
        val selectedDay = cal.timeInMillis

        val filter = DateRangeFilter(
            preset = DateFilterPreset.CUSTOM,
            customStartMillis = selectedDay,
            customEndMillis = selectedDay
        )
        val (start, end) = filter.getTimeRange()

        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(15, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testCustomDateRange_multiDays_correctBoundaries() {
        val startDay = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 10, 12, 0) }.timeInMillis
        val endDay = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 14, 8, 0) }.timeInMillis

        val filter = DateRangeFilter(
            preset = DateFilterPreset.CUSTOM,
            customStartMillis = startDay,
            customEndMillis = endDay
        )
        val (start, end) = filter.getTimeRange()

        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(10, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(14, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
    }

    @Test
    fun testGetDisplayLabel() {
        assertEquals("Hari Ini", DateRangeFilter(DateFilterPreset.TODAY).getDisplayLabel())
        assertEquals("7 Hari", DateRangeFilter(DateFilterPreset.LAST_7_DAYS).getDisplayLabel())
        assertEquals("Bulan Ini", DateRangeFilter(DateFilterPreset.THIS_MONTH).getDisplayLabel())
        assertEquals("Semua", DateRangeFilter(DateFilterPreset.ALL).getDisplayLabel())
    }
}
