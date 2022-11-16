/*
 * Created by qmarciset on 29/10/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.formatters

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("MagicNumber")
object TimeFormat {

    const val INT_1000 = 1000

    fun applyFormat(format: String, baseText: String): String {
        val longText = baseText.toLongOrNull() ?: return ""
        return when (format) {
            "timeInteger" -> longText.toString()
            "shortTime" -> getShortAMPMTime(longText)
            "mediumTime" -> getLongAMPMTime(longText)
            "duration" -> toShortDuration(longText)
            else -> ""
        }
    }

    fun getShortAMPMTime(millis: Long): String {
        val totalSecs = millis / INT_1000
        val days = TimeUnit.SECONDS.toDays(totalSecs).toInt()
        val hours = (TimeUnit.SECONDS.toHours(totalSecs) - days * 24).toInt()
        val minutes = (TimeUnit.SECONDS.toMinutes(totalSecs) - TimeUnit.SECONDS.toHours(totalSecs) * 60).toInt()
        val timeString = when {
            usesAmPm() && hours >= 12 -> "${hours - 12}:${getMinutes(minutes)} PM"
            usesAmPm() -> "$hours:${getMinutes(minutes)} AM"
            else -> "$hours:${getMinutes(minutes)}"
        }
        return timeString
    }

    private fun getLongAMPMTime(millis: Long): String {
        val totalSecs = millis / INT_1000
        val days = TimeUnit.SECONDS.toDays(totalSecs).toInt()
        val hours = (TimeUnit.SECONDS.toHours(totalSecs) - days * 24).toInt()
        val minutes = (TimeUnit.SECONDS.toMinutes(totalSecs) - TimeUnit.SECONDS.toHours(totalSecs) * 60).toInt()
        val seconds = (TimeUnit.SECONDS.toSeconds(totalSecs) - TimeUnit.SECONDS.toMinutes(totalSecs) * 60).toInt()
        val timeString = when {
            usesAmPm() && hours >= 12 -> "${hours - 12}:${getMinutes(minutes)}:${getSeconds(seconds)} PM"
            usesAmPm() -> "$hours:${getMinutes(minutes)}:${getSeconds(seconds)} AM"
            else -> "$hours:${getMinutes(minutes)}:${getSeconds(seconds)}"
        }
        return timeString
    }

    fun toVerboseDuration(millis: Long): String {
        val totalSecs = millis / INT_1000
        val days = TimeUnit.SECONDS.toDays(totalSecs).toInt()
        val hours = (TimeUnit.SECONDS.toHours(totalSecs) - days * 24).toInt()
        val minutes = (TimeUnit.SECONDS.toMinutes(totalSecs) - TimeUnit.SECONDS.toHours(totalSecs) * 60).toInt()
        val seconds = (TimeUnit.SECONDS.toSeconds(totalSecs) - TimeUnit.SECONDS.toMinutes(totalSecs) * 60).toInt()

        var timeString = ""
        if (days > 0) timeString = "$days ${getDayWord(days)}"
        if (hours > 0) timeString += " $hours ${getHourWord(hours)}"
        if (minutes > 0) timeString += " $minutes ${getMinuteWord(minutes)}"
        if (seconds > 0) timeString += " $seconds ${getSecondWord(seconds)}"
        if (timeString.isEmpty()) timeString = "0 second"
        return timeString.removePrefix(" ")
    }

    @Suppress("UnnecessaryVariable")
    private fun toShortDuration(millis: Long): String {
        val totalSecs = millis / INT_1000
        val hours = TimeUnit.SECONDS.toHours(totalSecs)
        val minutes = TimeUnit.SECONDS.toMinutes(totalSecs) - TimeUnit.SECONDS.toHours(totalSecs) * 60
        val seconds = TimeUnit.SECONDS.toSeconds(totalSecs) - TimeUnit.SECONDS.toMinutes(totalSecs) * 60
        val timeString = String.format(
            locale = Locale.getDefault(),
            format = "%02d:%02d:%02d",
            hours,
            minutes,
            seconds
        )
        return timeString
    }

    fun convertToMillis(hour: Int, minute: Int, seconds: Int = 0): Int =
        (hour * 3600 + minute * 60 + seconds) * INT_1000

    fun getElapsedTime(date: Date): String {
        val diff: Long = Date().time - date.time
        val totalSecs = diff / INT_1000
        val days = TimeUnit.SECONDS.toDays(totalSecs).toInt()
        val hours = TimeUnit.SECONDS.toHours(totalSecs).toInt()
        val minutes = TimeUnit.SECONDS.toMinutes(totalSecs).toInt()
        val seconds = TimeUnit.SECONDS.toSeconds(totalSecs).toInt()

        return when {
            days > 0 -> "$days ${getDayWord(days)} ago"
            hours > 0 -> "$hours ${getHourWord(hours)} ago"
            minutes > 0 -> "$minutes ${getMinuteWord(minutes)} ago"
            seconds > 0 -> "$seconds ${getSecondWord(seconds)} ago"
            seconds == 0 -> "1 ${getSecondWord(1)} ago"
            else -> ""
        }
    }

    private fun getDayWord(days: Int): String = if (days <= 1) "day" else "days"
    private fun getHourWord(hours: Int): String = if (hours <= 1) "hour" else "hours"
    private fun getMinuteWord(minutes: Int): String = if (minutes <= 1) "minute" else "minutes"
    private fun getSecondWord(seconds: Int): String = if (seconds <= 1) "second" else "seconds"
    private fun getMinutes(minutes: Int): String = if (minutes < 10) "0$minutes" else "$minutes"
    private fun getSeconds(seconds: Int): String = if (seconds < 10) "0$seconds" else "$seconds"

    private fun usesAmPm(): Boolean {
        val df: DateFormat = DateFormat.getTimeInstance(DateFormat.FULL, Locale.getDefault())
        return df is SimpleDateFormat && df.toPattern().contains("a")
    }
}
