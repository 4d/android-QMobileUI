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
        if (baseText == nullDate) return ""
        val calendar = getDateFromString(baseText).time
        return when (format) {
            "fullDate" -> {
                DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(calendar)
            }
            "longDate" -> {
                DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(calendar)
            }
            "mediumDate" -> {
                DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(calendar)
            }
            "shortDate" -> {
                DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(calendar)
            }
            else -> {
                baseText
            }
        }
    }

    private fun getDateFromString(date: String): Calendar = Calendar.getInstance().apply {
        val dateFormat = SimpleDateFormat("dd!MM!yyyy", Locale.getDefault())
        dateFormat.safeParse(date)?.let { date ->
            time = date
        }
    }
}
