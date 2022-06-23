/*
 * Created by qmarciset on 29/10/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.formatters

import com.qmobile.qmobileapi.utils.safeParse
import com.qmobile.qmobileui.action.actionparameters.viewholder.AM_KEY
import com.qmobile.qmobileui.action.actionparameters.viewholder.PM_KEY
import java.lang.StringBuilder
import java.sql.Time
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeFormat {

    private const val INT_3600 = 3600
    private const val INT_60: Int = 60
    const val INT_1000: Int = 1000
    private const val INT_12: Int = 12

    private val formatNameMap: Map<String, Int> = mapOf(
        "shortTime" to DateFormat.SHORT,
        "mediumTime" to DateFormat.MEDIUM,
        "duration" to DateFormat.MEDIUM,
    )

    fun applyFormat(format: String, baseText: String): String {
        val longText = baseText.toLongOrNull() ?: return ""

        return when (format) {
            "timeInteger" -> {
                longText.toString()
            }
            "shortTime" -> {
                formatNameMap[format]?.let {
                    DateFormat.getTimeInstance(it)
                        .format(getTimeFromString(longText).time)
                } ?: ""
            }
            "mediumTime" -> {
                getAmPmFormattedTime(longText, TimeUnit.MILLISECONDS)
            }
            "duration" -> {
                formatNameMap[format]?.let {
                    val totalSeconds: Long = longText / INT_1000
                    val seconds = totalSeconds.toInt() % INT_60
                    val minutes = (totalSeconds / INT_60).toInt() % INT_60
                    val hours = totalSeconds.toInt() / INT_3600
//                    val days = totalSeconds.toInt() / (INT_24 * INT_3600)

                    val builder = StringBuilder()
                    val minutesSeconds = String.format(
                        locale = Locale.getDefault(),
                        format = "%02d:%02d:%02d",
                        hours,
                        minutes,
                        seconds
                    )
                    /*if (days > 0) {
                        val daysStr = String.format("%02d", days)
                        builder.append(daysStr).append(":")
                    }*/
                    builder.append(minutesSeconds)
                    builder.toString()
                } ?: ""
            }
            else -> {
                ""
            }
        }
    }

    private fun getTimeFromString(time: Long): Calendar = Calendar.getInstance().apply {
        val simpleDateFormat = SimpleDateFormat("hh:mm:ss", Locale.getDefault())
        val timeString = simpleDateFormat.format(Date(time)).toString()
        simpleDateFormat.safeParse(timeString)?.let { date ->
            setTime(date)
        }
    }

    fun getAmPmFormattedTime(time: Long, timeUnit: TimeUnit): String {
        val totalSecs = when (timeUnit) {
            TimeUnit.SECONDS -> time
            TimeUnit.MILLISECONDS -> time / INT_1000
            else -> return ""
        }
        val hours = totalSecs / INT_3600
        val minutes = (totalSecs % INT_3600) / INT_60

        return if (hours >= INT_12) {
            "${hours - INT_12}:$minutes $PM_KEY"
        } else {
            "$hours:$minutes $AM_KEY"
        }
    }
}
