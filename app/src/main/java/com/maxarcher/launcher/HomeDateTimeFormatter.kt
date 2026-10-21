package com.maxarcher.launcher

import android.content.Context
import android.text.format.DateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object HomeDateTimeFormatter {
    fun time(context: Context, value: ZonedDateTime): String {
        val locale = context.resources.configuration.locales[0]
        return time(locale, DateFormat.is24HourFormat(context), value)
    }

    fun dateAndWeekday(context: Context, value: ZonedDateTime): String =
        dateAndWeekday(context.resources.configuration.locales[0], value)

    internal fun time(locale: Locale, is24Hour: Boolean, value: ZonedDateTime): String =
        format(locale, value, if (is24Hour) "Hm" else "hm")

    internal fun dateAndWeekday(locale: Locale, value: ZonedDateTime): String =
        format(locale, value, "MMMEd")

    private fun format(locale: Locale, value: ZonedDateTime, skeleton: String): String {
        val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
        return DateTimeFormatter.ofPattern(pattern, locale).format(value)
    }
}
