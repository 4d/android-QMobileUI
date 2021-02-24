/*
 * Created by Quentin Marciset on 23/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils.converter

import com.qmobile.qmobileui.model.NumberToWordConstants
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_10
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_100
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_12
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_20
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_3
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_6
import com.qmobile.qmobileui.model.QMobileUiConstants.INT_9
import java.text.DecimalFormat

internal class NumberToWordHelperFunction {
    companion object {
        private fun convertLessThanOneThousand(_number: Int): String {
            var number: Int = _number
            var soFar: String
            when {
                (number % INT_100 < INT_20) -> {
                    soFar = NumberToWordConstants.numNames[number % INT_100]
                    number /= INT_100
                }
                else -> {
                    soFar = NumberToWordConstants.numNames[number % INT_10]
                    number /= INT_10
                    soFar = NumberToWordConstants.tensNames[number % INT_10] + soFar
                    number /= INT_10
                }
            }
            return if (number == 0) soFar else NumberToWordConstants.numNames[number] + " hundred" + soFar
        }

        fun convertFromDoubleToWord(number: Double): String {
            // 0 to 999 999 999 999
            if (number.toInt() == 0) return "zero"
            val mask = "000000000000"
            val df = DecimalFormat(mask)
            val snumber = df.format(number)
            val billions = snumber.substring(0, INT_3).toInt()
            val millions = snumber.substring(INT_3, INT_6).toInt()
            val hundredThousands = snumber.substring(INT_6, INT_9).toInt()
            val thousands = snumber.substring(INT_9, INT_12).toInt()
            val tradBillions: String
            tradBillions = when (billions) {
                0 -> ""
                1 -> convertLessThanOneThousand(billions) + " billion "
                else -> convertLessThanOneThousand(billions) + " billion "
            }
            var result = tradBillions
            val tradMillions: String
            tradMillions = when (millions) {
                0 -> ""
                1 -> convertLessThanOneThousand(millions) + " million "
                else -> convertLessThanOneThousand(millions) + " million "
            }
            result += tradMillions
            val tradHundredThousands: String
            tradHundredThousands = when (hundredThousands) {
                0 -> ""
                1 -> "one thousand "
                else -> convertLessThanOneThousand(hundredThousands) + " thousand "
            }
            result += tradHundredThousands
            val tradThousand: String
            tradThousand = convertLessThanOneThousand(thousands)
            result += tradThousand
            // remove extra spaces!
            return result.replace("^\\s+".toRegex(), "").replace("\\b\\s{2,}\\b".toRegex(), " ")
        }
    }
}
