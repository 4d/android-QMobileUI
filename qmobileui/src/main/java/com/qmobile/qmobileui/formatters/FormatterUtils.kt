/*
 * Created by qmarciset on 9/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.formatters

import com.qmobile.qmobileapi.utils.parseToType
import com.qmobile.qmobiledatasync.app.BaseApp
import org.json.JSONObject
import java.text.DateFormat
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.roundToInt

object FormatterUtils {

    private const val DECIMAL_DIGITS = 3
    private const val INT_3600 = 3600
    private const val INT_60: Int = 60
    const val ROUND_MULTIPLIER = 10
    const val INT_100: Int = 100
    const val INT_20: Int = 20
    const val INT_10: Int = 10
    const val INT_3: Int = 3
    const val INT_6: Int = 6
    const val INT_9: Int = 9
    const val INT_12: Int = 12

    private val dateFormat: Map<String, Int> = mapOf(
        "shortDate" to DateFormat.SHORT,
        "mediumDate" to DateFormat.MEDIUM,
        "longDate" to DateFormat.LONG,
        "fullDate" to DateFormat.FULL
    )

    private val timeFormat: Map<String, Int> = mapOf(
        "shortTime" to DateFormat.SHORT,
        "mediumTime" to DateFormat.MEDIUM,
        "duration" to DateFormat.MEDIUM,
    )

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
                timeFormat[format]?.let {
                    DateFormat.getTimeInstance(it).format(getTimeFromString(baseText).time)
                } ?: ""
            }
            "mediumTime" -> {
                timeFormat[format]?.let {
                    DateFormat.getTimeInstance(it).format(getTimeFromString(baseText).time)
                } ?: ""
            }
            "duration" -> {
                timeFormat[format]?.let {
                    val timeFromString = getTimeFromString(baseText).time
                    val df = DateFormat.getTimeInstance(it, Locale.getDefault())
                    val time = df.format(timeFromString)
                    time.removeSuffix("AM").removeSuffix("PM")
                } ?: ""
            }
            "fullDate" -> {
                dateFormat[format]?.let {
                    DateFormat.getDateInstance(it, Locale.getDefault())
                        .format(getDateFromString(baseText).time)
                } ?: ""
            }
            "longDate" -> {
                dateFormat[format]?.let {
                    DateFormat.getDateInstance(it, Locale.getDefault())
                        .format(getDateFromString(baseText).time)
                } ?: ""
            }
            "mediumDate" -> {
                dateFormat[format]?.let {
                    DateFormat.getDateInstance(it, Locale.getDefault())
                        .format(getDateFromString(baseText).time)
                } ?: ""
            }
            "shortDate" -> {
                dateFormat[format]?.let {
                    DateFormat.getDateInstance(it, Locale.getDefault())
                        .format(getDateFromString(baseText).time)
                } ?: ""
            }
            "currencyEuro" -> {
                DecimalFormat("0.00").format(baseText.toDouble()) + "€"
            }
            "currencyYen" -> {
                "¥ " + baseText.toDouble().toInt()
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
                NumberToWord.convertNumberToWord(baseText)
            }
            "integer" -> {
                (baseText.toFloat()).toInt().toString()
            }
            "real" -> {
                baseText
            }
            "decimal" -> {
                baseText.toDouble().round(DECIMAL_DIGITS).toString()
            }
            "jsonPrettyPrinted" -> {
                BaseApp.mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(baseText.toJsonMap())
            }
            "json" -> {
                JSONObject(baseText).toString()
            }
            "jsonValues" -> {
                baseText.toJsonMap().values.joinToString(
                    System.lineSeparator()
                )
            }
            else -> {
                baseText
            }
        }
    }
}

private fun String.toJsonMap(): Map<String, Any> =
    BaseApp.mapper.parseToType(JSONObject(this).toString()) ?: mapOf()

fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= FormatterUtils.ROUND_MULTIPLIER }
    return (this * multiplier).roundToInt() / multiplier
}
