/*
 * Created by Quentin Marciset on 23/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import com.qmobile.qmobileui.model.QMobileFormatterConstants
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_0
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_100
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_2
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_3600
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_60
import com.qmobile.qmobileui.utils.converter.getDateFromString
import com.qmobile.qmobileui.utils.converter.getTimeFromLong
import com.qmobile.qmobileui.utils.converter.getTimeFromString
import java.text.DateFormat
import java.text.DecimalFormat
import java.util.*

fun applyFormat(format: String, baseText: String): String {
    when (format) {
        "noOrYes" -> {
            if (baseText.toBoolean())
                return "Yes"
            else
                return "No"
        }
        "falseOrTrue" -> {
            if (baseText.toBoolean())
                return "True"
            else
                return "False"
        }
        "boolInteger" -> {
            if (baseText.toBoolean())
                return "1"
            else
                return "0"
        }
        "timeInteger" -> {
            val newTimeArray = getTimeFromLong(baseText.toLong()).split(":")
            return (newTimeArray[0] + (Integer.parseInt(newTimeArray[1]) * INT_60) + Integer.parseInt(newTimeArray[1]) * INT_3600)
        }
        "shortTime" -> {
            QMobileFormatterConstants.timeFormat[format]?.let {
                return DateFormat.getTimeInstance(it).format(getTimeFromString(baseText).time)
            } ?: return ""
        }
        "mediumTime" -> {
            QMobileFormatterConstants.timeFormat[format]?.let {
                return DateFormat.getTimeInstance(it).format(getTimeFromString(baseText).time)
            } ?: return ""
        }
        "duration" -> {
            QMobileFormatterConstants.timeFormat[format]?.let {
                return DateFormat.getTimeInstance(it, Locale.getDefault()).format(getTimeFromString(baseText).time)
            } ?: return ""
        }
        "fullDate" -> {
            QMobileFormatterConstants.dateFormat[format]?.let {
                return DateFormat.getDateInstance(it, Locale.getDefault()).format(getDateFromString(baseText).time)
            } ?: return ""
        }
        "longDate" -> {
            QMobileFormatterConstants.dateFormat[format]?.let {
                return DateFormat.getDateInstance(it, Locale.getDefault()).format(getDateFromString(baseText).time)
            } ?: return ""
        }
        "mediumDate" -> {
            QMobileFormatterConstants.dateFormat[format]?.let {
                return DateFormat.getDateInstance(it, Locale.getDefault()).format(getDateFromString(baseText).time)
            } ?: return ""
        }
        "shortDate" -> {
            QMobileFormatterConstants.dateFormat[format]?.let {
                return DateFormat.getDateInstance(it, Locale.getDefault()).format(getDateFromString(baseText).time)
            } ?: return ""
        }
        "currencyEuro" -> {
            return DecimalFormat("0.00").format(baseText.toDouble()) + "€"
        }
        "currencyYen" -> {
            return "¥" + DecimalFormat("0.00").format(baseText.toDouble())
        }
        "currencyDollar" -> {
            return "$" + DecimalFormat("0.00").format(baseText.toDouble())
        }
        "percent" -> {
            return ((DecimalFormat("0.00").format(baseText.toDouble()).toDouble()) * INT_100).toString() + "%"
        }
        "ordinal" -> {
            return DecimalFormat("0.00").format(baseText.toDouble()) + "th"
        }
        "spellOut" -> {
            return QMobileUiUtil.WordFormatter(baseText)
        }
        "integer" -> {
            return (baseText.toFloat()).toInt().toString()
        }
        "real" -> {
            return baseText
        }
        "decimal" -> {
            return DecimalFormat("0.000").format(baseText.toDouble())
        }
        else -> return ""
    }
}
