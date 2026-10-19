package com.kapos.mobile.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateFilterPreset(val displayName: String) {
    TODAY("Hari Ini"),
    LAST_7_DAYS("7 Hari"),
    THIS_MONTH("Bulan Ini"),
    CUSTOM("Pilih Tanggal"),
    ALL("Semua")
}

data class DateRangeFilter(
    val preset: DateFilterPreset = DateFilterPreset.TODAY,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null
) {
    fun getTimeRange(nowMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (preset) {
            DateFilterPreset.TODAY -> {
                cal.timeInMillis = nowMillis
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            DateFilterPreset.LAST_7_DAYS -> {
                cal.timeInMillis = nowMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                val start = end - (7L * 24 * 60 * 60 * 1000 - 1000)
                Pair(start, end)
            }
            DateFilterPreset.THIS_MONTH -> {
                cal.timeInMillis = nowMillis
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            DateFilterPreset.ALL -> {
                Pair(0L, Long.MAX_VALUE)
            }
            DateFilterPreset.CUSTOM -> {
                val startRaw = customStartMillis ?: nowMillis
                val endRaw = customEndMillis ?: startRaw

                val startCal = Calendar.getInstance().apply {
                    timeInMillis = minOf(startRaw, endRaw)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val endCal = Calendar.getInstance().apply {
                    timeInMillis = maxOf(startRaw, endRaw)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(startCal.timeInMillis, endCal.timeInMillis)
            }
        }
    }

    fun getDisplayLabel(): String {
        return when (preset) {
            DateFilterPreset.TODAY -> "Hari Ini"
            DateFilterPreset.LAST_7_DAYS -> "7 Hari"
            DateFilterPreset.THIS_MONTH -> "Bulan Ini"
            DateFilterPreset.ALL -> "Semua"
            DateFilterPreset.CUSTOM -> {
                if (customStartMillis == null) return "Pilih Tanggal"
                val start = customStartMillis
                val end = customEndMillis ?: start
                val sdfSameYear = SimpleDateFormat("dd MMM", Locale("id", "ID"))
                val sdfFull = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

                val calStart = Calendar.getInstance().apply { timeInMillis = start }
                val calEnd = Calendar.getInstance().apply { timeInMillis = end }

                if (calStart.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR) &&
                    calStart.get(Calendar.DAY_OF_YEAR) == calEnd.get(Calendar.DAY_OF_YEAR)) {
                    sdfFull.format(Date(start))
                } else if (calStart.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR)) {
                    "${sdfSameYear.format(Date(start))} - ${sdfFull.format(Date(end))}"
                } else {
                    "${sdfFull.format(Date(start))} - ${sdfFull.format(Date(end))}"
                }
            }
        }
    }
}
