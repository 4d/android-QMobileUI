/*
 * Created by Maturi Karthik on 31/3/2021.
 * 4D SAS
 * Copyright (c) 2021 Maturi Karthik. All rights reserved.
 */

package com.qmobile.qmobileui

import com.qmobile.qmobileui.utils.TypeChoice
import com.qmobile.qmobileui.utils.date
import com.qmobile.qmobileui.utils.formatBoolean
import com.qmobile.qmobileui.utils.number
import com.qmobile.qmobileui.utils.time
import org.junit.Assert
import org.junit.Test


class FormatterTest {

    @Test
    fun testDateFormat() {
        val dateInput = "23!11!1937"
        dateFormatTest(dateInput, expectedResult = "11/23/37", typeChoice = TypeChoice.ShortDate)
        dateFormatTest(
            dateInput,
            expectedResult = "Nov 23, 1937",
            typeChoice = TypeChoice.MediumDate
        )
        dateFormatTest(
            dateInput,
            expectedResult = "November 23, 1937",
            typeChoice = TypeChoice.LongDate
        )
        dateFormatTest(
            dateInput,
            expectedResult = "Tuesday, November 23, 1937",
            typeChoice = TypeChoice.FullDate
        )
    }

    @Test
    fun testBooleanFormat() {
        val inputIsTrue = true.toString()
        val inputIsFalse = false.toString()
        booleanFormatTest(
            inputIsTrue,
            expectedResult = 1.toString(),
            typeChoice = TypeChoice.Number
        )
        booleanFormatTest(inputIsTrue, expectedResult = "Yes", typeChoice = TypeChoice.YesNo)
        booleanFormatTest(inputIsTrue, expectedResult = "True", typeChoice = TypeChoice.TrueFalse)
        booleanFormatTest(
            inputIsFalse,
            expectedResult = 0.toString(),
            typeChoice = TypeChoice.Number
        )
        booleanFormatTest(inputIsFalse, expectedResult = "No", typeChoice = TypeChoice.YesNo)
        booleanFormatTest(inputIsFalse, expectedResult = "False", typeChoice = TypeChoice.TrueFalse)
    }

    @Test
    fun testTimeFormat() {
        val inputTime = "946693832"
        timeFormatTest(inputTime, "113480208800", TypeChoice.Number)
        timeFormatTest(inputTime, "11:58:13 AM", TypeChoice.Time)
        timeFormatTest(inputTime, "11:58 AM", TypeChoice.ShortTime)
        timeFormatTest(inputTime, "11:58:13", TypeChoice.Duration)
    }

    @Test
    fun testNumberFormat() {
        val number = 24.46812
        numberFormatTest(number.toString(), "24.468", TypeChoice.Decimal)
        numberFormatTest(number.toString(), "24", TypeChoice.Number)
        numberFormatTest(number.toString(), "24.46812", TypeChoice.Real)
        numberFormatTest(number.toString(), " twenty four dot   forty seven", TypeChoice.SpellOut)
        numberFormatTest(number.toString(), "24.47th", TypeChoice.Ordinal)
        numberFormatTest(number.toString(), "2447.0%", TypeChoice.Percentage)
        numberFormatTest(number.toString(), "24.47€", TypeChoice.EuroCurrency)
        numberFormatTest(number.toString(), "$24.47", TypeChoice.USCurrency)
        numberFormatTest(number.toString(), "¥24.47", TypeChoice.JapanCurrency)
    }

    private fun dateFormatTest(inputDate: String, expectedResult: String, typeChoice: TypeChoice) =
        Assert.assertEquals(expectedResult, date(typeChoice.key, inputDate))

    private fun booleanFormatTest(
        inputDate: String,
        expectedResult: String,
        typeChoice: TypeChoice
    ) = Assert.assertEquals(expectedResult, formatBoolean(typeChoice.key, inputDate))

    private fun timeFormatTest(inputDate: String, expectedResult: String, typeChoice: TypeChoice) =
        Assert.assertEquals(expectedResult, time(typeChoice.key, inputDate).trim())

    private fun numberFormatTest(input: String, expectedResult: String, typeChoice: TypeChoice) =
        Assert.assertEquals(expectedResult, number(typeChoice.key, input))


}