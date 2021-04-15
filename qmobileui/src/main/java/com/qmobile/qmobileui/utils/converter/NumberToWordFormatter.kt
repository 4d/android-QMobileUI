/*
 * Created by Quentin Marciset on 23/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils.converter

import java.math.RoundingMode
import java.text.DecimalFormat

internal object NumberToWordFormatter {

    fun convertNumberToWord(number: String): String {
        val string: String
        if (number.matches(Regex("^\\d+\\.\\d+"))) {
            val decimalFormat = DecimalFormat("#.##")
            decimalFormat.roundingMode = RoundingMode.CEILING
            // val foramtedStringNumber = decimalFormat.format(number.toDouble())
            val regex = decimalFormat.format(number.toDouble()).split(".")
            string =
                "${NumberToWordHelperFunction.convertFromDoubleToWord(regex[0].toDouble())} dot ${
                NumberToWordHelperFunction.convertFromDoubleToWord(
                    regex[1].toDouble()
                )
                }"
        } else {
            string = NumberToWordHelperFunction.convertFromDoubleToWord(number.toDouble())
        }
        return string
    }
}
