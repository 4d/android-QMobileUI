/*
 * Created by qmarciset on 29/10/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.formatters

import android.annotation.SuppressLint
import com.qmobile.qmobileapi.utils.safeParse
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@SuppressLint("SimpleDateFormat")
object TimeFormat {

    private const val INT_3600 = 3600
    private const val INT_60: Int = 60

    private val formatNameMap: Map<String, Int> = mapOf(
        "shortTime" to DateFormat.SHORT,
        "mediumTime" to DateFormat.MEDIUM,
        "duration" to DateFormat.MEDIUM,
    )

    fun applyFormat(format: String, baseText: String): String {
        return when (format) {
            "timeInteger" -> {

                val newTimeArray = getTimeFromLong(baseText.toLong()).split(":")
                (
                    newTimeArray[0] + (Integer.parseInt(newTimeArray[1]) * INT_60) + Integer.parseInt(
                        newTimeArray[1]
                    ) * INT_3600
                    )
            }
            "shortTime" -> {
                formatNameMap[format]?.let {
                    DateFormat.getTimeInstance(it)
                        .format(getTimeFromString(baseText).time)
                } ?: ""
            }
            "mediumTime" -> {
                formatNameMap[format]?.let {
                    DateFormat.getTimeInstance(it)
                        .format(getTimeFromString(baseText).time)
                } ?: ""
            }
            "duration" -> {
                formatNameMap[format]?.let {
                    val timeFromString = getTimeFromString(baseText).time
                    val df = DateFormat.getTimeInstance(it, Locale.getDefault())
                    val time = df.format(timeFromString)
                    time.removeSuffix("AM").removeSuffix("PM")
                } ?: ""
            }
            else -> {
                baseText
            }
        }
    }

    private fun getTimeFromString(time: String): Calendar = Calendar.getInstance().apply {
        val longTime: String = getTimeFromLong(time.toLong())
        val dateFormat = SimpleDateFormat("hh:mm:ss")
        dateFormat.safeParse(longTime)?.let { date ->
            setTime(date)
        }
    }

    private fun getTimeFromLong(timestamp: Long) =
        SimpleDateFormat("hh:mm:ss").format(Date(timestamp)).toString()
}
