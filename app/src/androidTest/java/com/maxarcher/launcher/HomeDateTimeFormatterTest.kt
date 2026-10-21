package com.maxarcher.launcher

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeDateTimeFormatterTest {
    private val value = ZonedDateTime.of(
        2026,
        2,
        1,
        13,
        5,
        42,
        0,
        ZoneId.of("Asia/Hong_Kong"),
    )

    @Test
    fun twentyFourHourTimeOmitsSeconds() {
        val result = HomeDateTimeFormatter.time(Locale.US, true, value)

        assertTrue(result.contains("13"))
        assertFalse(result.contains("42"))
    }

    @Test
    fun twelveHourTimeUsesLocalizedPeriodAndOmitsSeconds() {
        val result = HomeDateTimeFormatter.time(Locale.US, false, value)

        assertTrue(result.contains("1"))
        assertFalse(result.contains("42"))
    }

    @Test
    fun dateIncludesDayAndWeekday() {
        val result = HomeDateTimeFormatter.dateAndWeekday(Locale.SIMPLIFIED_CHINESE, value)

        assertTrue(result.contains("1"))
        assertTrue(result.contains("日") || result.contains("周") || result.contains("星期"))
    }
}
