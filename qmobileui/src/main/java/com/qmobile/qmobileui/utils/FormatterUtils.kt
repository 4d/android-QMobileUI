/*
 * Created by Quentin Marciset on 23/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import com.qmobile.qmobileui.model.QMobileFormatterConstants
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_100
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_3600
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_60
import com.qmobile.qmobileui.utils.converter.getDateFromString
import com.qmobile.qmobileui.utils.converter.getTimeFromLong
import com.qmobile.qmobileui.utils.converter.getTimeFromString
import java.text.DateFormat
import java.text.DecimalFormat
import java.util.Locale

fun applyFormat(format: String, baseText: String): String {
    return when (format) {
        "noOrYes" -> {
            if (baseText.toBoolean()) "Yes" else "No"
        }
        "falseOrTrue" -> {
            if (baseText.toBoolean()) "True" else "False"
        }
        "boolInteger" -> {
            if (baseText.toBoolean()) "1" else "0"
        }
        "timeInteger" -> {
            val newTimeArray = getTimeFromLong(baseText.toLong()).split(":")
            (
                newTimeArray[0] + (Integer.parseInt(newTimeArray[1]) * INT_60) + Integer.parseInt(
                    newTimeArray[1]
                ) * INT_3600
                )
        }
        "shortTime" -> {
            QMobileFormatterConstants.timeFormat[format]?.let {
                DateFormat.getTimeInstance(it).format(getTimeFromString(baseText).time)
            } ?: ""
        }
        "mediumTime" -> {
            QMobileFormatterConstants.timeFormat[format]?.let {
                DateFormat.getTimeInstance(it).format(getTimeFromString(baseText).time)
            } ?: ""
        }
        "duration" -> {
            QMobileFormatterConstants.timeFormat[format]?.let {
                val timeFromString = getTimeFromString(baseText).time
                val df = DateFormat.getTimeInstance(it, Locale.getDefault())
                val time = df.format(timeFromString)
                time.removeSuffix("AM").removeSuffix("PM")
            } ?: ""
        }
        "fullDate" -> {
            QMobileFormatterConstants.dateFormat[format]?.let {
                DateFormat.getDateInstance(it, Locale.getDefault())
                    .format(getDateFromString(baseText).time)
            } ?: ""
        }
        "longDate" -> {
            QMobileFormatterConstants.dateFormat[format]?.let {
                DateFormat.getDateInstance(it, Locale.getDefault())
                    .format(getDateFromString(baseText).time)
            } ?: ""
        }
        "mediumDate" -> {
            QMobileFormatterConstants.dateFormat[format]?.let {
                DateFormat.getDateInstance(it, Locale.getDefault())
                    .format(getDateFromString(baseText).time)
            } ?: ""
        }
        "shortDate" -> {
            QMobileFormatterConstants.dateFormat[format]?.let {
                DateFormat.getDateInstance(it, Locale.getDefault())
                    .format(getDateFromString(baseText).time)
            } ?: ""
        }
        "currencyEuro" -> {
            DecimalFormat("0.00").format(baseText.toDouble()) + "€"
        }
        "currencyYen" -> {
            "¥" + DecimalFormat("0.00").format(baseText.toDouble())
        }
        "currencyDollar" -> {
            "$" + DecimalFormat("0.00").format(baseText.toDouble())
        }
        "percent" -> {
            (
                (
                    DecimalFormat("0.00").format(baseText.toDouble())
                        .toDouble()
                    ) * INT_100
                ).toString() + "%"
        }
        "ordinal" -> {
            DecimalFormat("0.00").format(baseText.toDouble()) + "th"
        }
        "spellOut" -> {
            QMobileUiUtil.WordFormatter(baseText)
        }
        "integer" -> {
            (baseText.toFloat()).toInt().toString()
        }
        "real" -> {
            baseText
        }
        "decimal" -> {
            DecimalFormat("0.000").format(baseText.toDouble())
        }
        else -> ""
    }
}
