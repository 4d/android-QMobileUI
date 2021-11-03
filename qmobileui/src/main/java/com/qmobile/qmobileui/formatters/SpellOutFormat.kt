/*
 * Created by qmarciset on 13/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.formatters

import java.math.RoundingMode
import java.text.DecimalFormat

object SpellOutFormat {

    private const val INT_10: Int = 10
    private const val INT_20: Int = 20
    private const val INT_3: Int = 3
    private const val INT_6: Int = 6
    private const val INT_9: Int = 9
    private const val INT_12: Int = 12
    private const val INT_100: Int = 100

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
        val billions = sNumber.substring(0, INT_3).toInt()
        val millions = sNumber.substring(INT_3, INT_6).toInt()
        val hundredThousands = sNumber.substring(INT_6, INT_9).toInt()
        val thousands = sNumber.substring(INT_9, INT_12).toInt()
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
            (pnumber % INT_100 < INT_20) -> {
                soFar = numNames[pnumber % INT_100]
                pnumber /= INT_100
            }
            else -> {
                soFar = numNames[pnumber % INT_10]
                pnumber /= INT_10
                soFar = tensNames[pnumber % INT_10] + soFar
                pnumber /= INT_10
            }
        }
        return if (pnumber == 0) soFar else numNames[pnumber] + " hundred" + soFar
    }
}
