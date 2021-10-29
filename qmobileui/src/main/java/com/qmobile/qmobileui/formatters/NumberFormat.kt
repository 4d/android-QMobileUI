/*
 * Created by qmarciset on 29/10/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.formatters

import java.text.DecimalFormat
import kotlin.math.roundToInt

object NumberFormat {

    private const val DECIMAL_DIGITS = 3
    private const val ROUND_MULTIPLIER = 10
    private const val INT_100: Int = 100

    fun applyFormat(format: String, baseText: String): String {
        return when (format) {
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
            "integer" -> {
                (baseText.toFloat()).toInt().toString()
            }
            "real" -> {
                baseText
            }
            "decimal" -> {
                baseText.toDouble().round(DECIMAL_DIGITS).toString()
            }
            else -> {
                baseText
            }
        }
    }

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= ROUND_MULTIPLIER }
        return (this * multiplier).roundToInt() / multiplier
    }
}
