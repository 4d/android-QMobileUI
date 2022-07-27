/*
 * Created by qmarciset on 13/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.formatters

import java.math.RoundingMode
import java.text.DecimalFormat

@Suppress("MagicNumber")
object SpellOutFormat {

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
        val doubleText = number.toDoubleOrNull() ?: return ""
        var string = ""
        if (number.matches(Regex("^\\d+\\.\\d+"))) {
            val decimalFormat = DecimalFormat("#.##")
            decimalFormat.roundingMode = RoundingMode.CEILING
            val regex = decimalFormat.format(doubleText).split(".")
            if (regex.all { it.toDoubleOrNull() != null }) {
                string = if (regex.size > 1) {
                    "${convertFromDoubleToWord(regex[0].toDouble())} dot ${
                    convertFromDoubleToWord(
                        regex[1].toDouble()
                    )
                    }"
                } else {
                    convertFromDoubleToWord(regex[0].toDouble())
                }
            }
        } else {
            string = convertFromDoubleToWord(doubleText)
        }
        return string
    }

    private fun convertFromDoubleToWord(number: Double): String {
        // 0 to 999 999 999 999
        if (number.toInt() == 0) return "zero"
        val mask = "000000000000"
        val df = DecimalFormat(mask)
        val sNumber = df.format(number)
        val billions = sNumber.substring(0, 3).toInt()
        val millions = sNumber.substring(3, 6).toInt()
        val hundredThousands = sNumber.substring(6, 9).toInt()
        val thousands = sNumber.substring(9, 12).toInt()
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
            (pnumber % 100 < 20) -> {
                soFar = numNames[pnumber % 100]
                pnumber /= 100
            }
            else -> {
                soFar = numNames[pnumber % 10]
                pnumber /= 10
                soFar = tensNames[pnumber % 10] + soFar
                pnumber /= 10
            }
        }
        return if (pnumber == 0) soFar else numNames[pnumber] + " hundred" + soFar
    }
}
