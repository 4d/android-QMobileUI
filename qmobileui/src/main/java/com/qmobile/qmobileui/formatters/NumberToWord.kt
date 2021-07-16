/*
 * Created by qmarciset on 13/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.formatters

import java.math.RoundingMode
import java.text.DecimalFormat

object NumberToWord {

    private val tensNames: List<String> = listOf(
        "", " ten", " twenty", " thirty", " forty",
        " fifty", " sixty", " seventy", " eighty", " ninety"
    )
    private val numNames: List<String> = listOf(
        "", " one", " two", " three", " four", " five",
        " six", " seven", " eight", " nine", " ten", " eleven", " twelve", " thirteen",
        " fourteen", " fifteen", " sixteen", " seventeen", " eighteen", " nineteen"
    )

    fun convertNumberToWord(number: String): String {
        val string: String
        if (number.matches(Regex("^\\d+\\.\\d+"))) {
            val decimalFormat = DecimalFormat("#.##")
            decimalFormat.roundingMode = RoundingMode.CEILING
            val regex = decimalFormat.format(number.toDouble()).split(".")
            string = if (regex.size > 1) {
                "${convertFromDoubleToWord(regex[0].toDouble())} dot ${
                convertFromDoubleToWord(
                    regex[1].toDouble()
                )
                }"
            } else {
                convertFromDoubleToWord(regex[0].toDouble())
            }
        } else {
            string = convertFromDoubleToWord(number.toDouble())
        }
        return string
    }

    private fun convertFromDoubleToWord(number: Double): String {
        // 0 to 999 999 999 999
        if (number.toInt() == 0) return "zero"
        val mask = "000000000000"
        val df = DecimalFormat(mask)
        val sNumber = df.format(number)
        val billions = sNumber.substring(0, FormatterUtils.INT_3).toInt()
        val millions = sNumber.substring(FormatterUtils.INT_3, FormatterUtils.INT_6).toInt()
        val hundredThousands = sNumber.substring(FormatterUtils.INT_6, FormatterUtils.INT_9).toInt()
        val thousands = sNumber.substring(FormatterUtils.INT_9, FormatterUtils.INT_12).toInt()
        val tradBillions: String = when (billions) {
            0 -> ""
            1 -> convertLessThanOneThousand(billions) + " billion "
            else -> convertLessThanOneThousand(billions) + " billion "
        }
        var result = tradBillions
        val tradMillions: String = when (millions) {
            0 -> ""
            1 -> convertLessThanOneThousand(millions) + " million "
            else -> convertLessThanOneThousand(millions) + " million "
        }
        result += tradMillions
        val tradHundredThousands: String = when (hundredThousands) {
            0 -> ""
            1 -> "one thousand "
            else -> convertLessThanOneThousand(hundredThousands) + " thousand "
        }
        result += tradHundredThousands
        val tradThousand: String = convertLessThanOneThousand(thousands)
        result += tradThousand
        // remove extra spaces!
        return result.replace("^\\s+".toRegex(), "").replace("\\b\\s{2,}\\b".toRegex(), " ")
    }

    private fun convertLessThanOneThousand(nb: Int): String {
        var pnumber: Int = nb
        var soFar: String
        when {
            (pnumber % FormatterUtils.INT_100 < FormatterUtils.INT_20) -> {
                soFar = numNames[pnumber % FormatterUtils.INT_100]
                pnumber /= FormatterUtils.INT_100
            }
            else -> {
                soFar = numNames[pnumber % FormatterUtils.INT_10]
                pnumber /= FormatterUtils.INT_10
                soFar = tensNames[pnumber % FormatterUtils.INT_10] + soFar
                pnumber /= FormatterUtils.INT_10
            }
        }
        return if (pnumber == 0) soFar else numNames[pnumber] + " hundred" + soFar
    }
}
