/*
 * Created by htemanni on 22/6/2022.
 * 4D SAS
 * Copyright (c) 2022 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui.action.utils

import com.qmobile.qmobileui.formatters.TimeFormat
import java.util.Date
import java.util.concurrent.TimeUnit

const val MILLISECONDS_IN_SECOND = 1000
const val SECONDS_IN_MINUTE = 60
const val SECONDS_IN_HOUR = 3600
const val MINUTES_IN_HOUR = 60
const val HOURS_IN_DAY = 24
const val HOURS_IN_MID_DAY_FORMAT = 12

object DateTimeHelper {

    fun getFormattedTime(numberOfSeconds: Double, format: String?): String {
        val hours: Int = (numberOfSeconds / SECONDS_IN_HOUR).toInt()
        val minutes: Int = (numberOfSeconds % SECONDS_IN_HOUR / SECONDS_IN_MINUTE).toInt()
        return if (format == "duration") {
            "$hours hours $minutes minutes"
        } else {
            TimeFormat.getAmPmFormattedTime(numberOfSeconds.toLong(), TimeUnit.SECONDS)
        }
    }

    fun getFormattedDate(date: Date): String {
        val diff: Long = Date().time - date.time
        val seconds = diff / MILLISECONDS_IN_SECOND
        val minutes = seconds / SECONDS_IN_MINUTE
        val hours = minutes / MINUTES_IN_HOUR
        val days = hours / HOURS_IN_DAY

        return when {
            days > 0 -> "$days ${getDayWord(days)} ago"
            hours > 0 -> "$hours ${getHourWord(hours)} ago"
            minutes > 0 -> "$minutes ${getMinuteWord(minutes)} ago"
            seconds > 0 -> "$seconds ${getSecondWord(seconds)} ago"
            seconds == 0L -> "1 ${getSecondWord(1)} ago" // case of click on error server task without
            // parameters , show always one second ago(instead of 0)
            else -> ""
        }
    }

    fun getDayWord(days: Long): String = if (days <= 1) "day" else "days"
    fun getHourWord(hours: Long): String = if (hours <= 1) "hour" else "hours"
    fun getMinuteWord(minutes: Long): String = if (minutes <= 1) "minute" else "minutes"
    fun getSecondWord(seconds: Long): String = if (seconds <= 1) "seconds" else "seconds"
}
