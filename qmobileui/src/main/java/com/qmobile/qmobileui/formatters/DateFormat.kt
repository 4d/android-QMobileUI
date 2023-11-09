/*
 * Created by qmarciset on 29/10/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.formatters

import com.qmobile.qmobileapi.utils.safeParse
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateFormat {

    private const val nullDate = "0!0!0"

    fun applyFormat(format: String, baseText: String): String {
        val calendar = getDateFromString(baseText) ?: return ""
        val time = calendar.time
        return when (format) {
            "fullDate" -> {
                DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(time)
            }
            "longDate" -> {
                DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(time)
            }
            "mediumDate" -> {
                DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(time)
            }
            "shortDate" -> {
                DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(time)
            }
            else -> {
                baseText
            }
        }
    }

    fun getDateFromString(date: String): Calendar? = Calendar.getInstance().apply {
        if (date == nullDate) return null
        val dateFormat = SimpleDateFormat("dd!MM!yyyy", Locale.getDefault())
        dateFormat.safeParse(date)?.let { date ->
            time = date
        }
    }
}
