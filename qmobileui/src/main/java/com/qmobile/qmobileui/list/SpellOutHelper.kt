package com.qmobile.qmobileui.list

import java.text.DecimalFormat

@Suppress( "MagicNumber")

object SpellOutHelper {
    private val tensNames = arrayOf(
        "",
        " ten",
        " twenty",
        " thirty",
        " forty",
        " fifty",
        " sixty",
        " seventy",
        " eighty",
        " ninety"
    )
    private val numNames = arrayOf(
        "",
        " one",
        " two",
        " three",
        " four",
        " five",
        " six",
        " seven",
        " eight",
        " nine",
        " ten",
        " eleven",
        " twelve",
        " thirteen",
        " fourteen",
        " fifteen",
        " sixteen",
        " seventeen",
        " eighteen",
        " nineteen"
    )

    private fun convertLessThanOneThousand(number: Int): String {
        var number = number
        var soFar: String
        if (number % 100 < 20) {
            soFar = numNames[number % 100]
            number /= 100
        } else {
            soFar = numNames[number % 10]
            number /= 10
            soFar = tensNames[number % 10] + soFar
            number /= 10
        }
        return if (number == 0) soFar else numNames[number] + " hundred" + soFar
    }

    fun convert(number: Long): String {
        // 0 to 999 999 999 999
        if (number == 0L) {
            return "zero"
        }
        var snumber = number.toString()

        // pad with "0"
        val mask = "000000000000"
        val df = DecimalFormat(mask)
        snumber = df.format(number)

        // XXXnnnnnnnnn
        val billions = snumber.substring(0, 3).toInt()
        // nnnXXXnnnnnn
        val millions = snumber.substring(3, 6).toInt()
        // nnnnnnXXXnnn
        val hundredThousands = snumber.substring(6, 9).toInt()
        // nnnnnnnnnXXX
        val thousands = snumber.substring(9, 12).toInt()
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

}