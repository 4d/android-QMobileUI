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
import java.util.Locale

@SuppressLint("SimpleDateFormat")
object DateFormat {

    private val formatNameMap: Map<String, Int> = mapOf(
        "shortDate" to DateFormat.SHORT,
        "mediumDate" to DateFormat.MEDIUM,
        "longDate" to DateFormat.LONG,
        "fullDate" to DateFormat.FULL
    )

    fun applyFormat(format: String, baseText: String): String {
        return when (format) {

            "fullDate" -> {
                formatNameMap[format]?.let {
                    DateFormat.getDateInstance(it, Locale.getDefault())
                        .format(getDateFromString(baseText).time)
                } ?: ""
            }
            "longDate" -> {
                formatNameMap[format]?.let {
                    DateFormat.getDateInstance(it, Locale.getDefault())
                        .format(getDateFromString(baseText).time)
                } ?: ""
            }
            "mediumDate" -> {
                formatNameMap[format]?.let {
                    DateFormat.getDateInstance(it, Locale.getDefault())
                        .format(getDateFromString(baseText).time)
                } ?: ""
            }
            "shortDate" -> {
                formatNameMap[format]?.let {
                    DateFormat.getDateInstance(it, Locale.getDefault())
                        .format(getDateFromString(baseText).time)
                } ?: ""
            }
            else -> {
                baseText
            }
        }
    }

    private fun getDateFromString(date: String): Calendar = Calendar.getInstance().apply {
        val dateFormat = SimpleDateFormat("dd!MM!yyyy")
        dateFormat.safeParse(date)?.let { date ->
            time = date
        }
    }
}
