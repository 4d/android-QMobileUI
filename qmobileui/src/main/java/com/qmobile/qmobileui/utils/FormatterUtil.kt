/*
 * Created by Quentin Marciset on 23/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.annotation.SuppressLint
import com.qmobile.qmobileui.model.QMobileFormatterConstants
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_100
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_3600
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_60
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@SuppressLint("SimpleDateFormat")
internal object FormatterUtil {

    fun formatDate(format: String, local: Locale, date: String) =
        QMobileFormatterConstants.dateFormat[format]?.let {
            DateFormat.getDateInstance(it, local).format(getDateFromString(date).time)
        }

    fun formatTime(format: String, local: Locale, time: String) = when (format) {

        "duration" -> QMobileFormatterConstants.timeFormat[format]?.let {
            DateFormat.getTimeInstance(it, local).format(getTimeFromString(time).time)
        }
        "integer" -> {
            val newTimeArray = time.split(":")
            (
                newTimeArray[0] + (Integer.parseInt(newTimeArray[1]) * INT_60) + Integer.parseInt(
                    newTimeArray[1]
                ) * INT_3600
                )
        }
        else -> QMobileFormatterConstants.timeFormat[format]?.let {
            DateFormat.getTimeInstance(it).format(getTimeFromString(time).time)
        }
    }

    fun formatBoolean(format: String, value: Boolean) = when (format) {
        "integer" -> if (value) 1 else 0
        else -> QMobileFormatterConstants.booleanFormat[format]
    }

    fun formatNumber(format: String, value: String): Any = when (format) {
        "decimal" -> DecimalFormat("0.000").format(value.toDouble())
        "real" -> value
        "integer" -> (value.toFloat()).toInt()
        "ordinal" -> DecimalFormat("0.00").format(value.toDouble()) + "th"
        "percent" -> (
            (
                DecimalFormat("0.00").format(value.toDouble())
                    .toDouble()
                ) * INT_100
            ).toString() + "%"
        "currencyDollar" -> "$" + DecimalFormat("0.00").format(value.toDouble())
        "currencyEuro" -> DecimalFormat("0.00").format(value.toDouble()) + "€"
        "currencyYen" -> "¥" + DecimalFormat("0.00").format(value.toDouble())
        "spellOut" -> QMobileUiUtil.WordFormatter(value)
        else -> "test"
    }

    private fun getTimeFromString(time: String): Calendar = Calendar.getInstance().apply {
        setTime(SimpleDateFormat("hh:mm:ss").parse(time)!!)
    }

    private fun getDateFromString(date: String): Calendar = Calendar.getInstance().apply {
        time = SimpleDateFormat("MM-dd-yyyy").parse(date)!!
    }
}
