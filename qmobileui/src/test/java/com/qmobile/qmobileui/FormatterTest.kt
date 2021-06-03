/*
 * Created by Maturi Karthik on 31/3/2021.
 * 4D SAS
 * Copyright (c) 2021 Maturi Karthik. All rights reserved.
 */

package com.qmobile.qmobileui

import com.qmobile.qmobileui.utils.FieldMapping
import com.qmobile.qmobileui.utils.applyFormat
import com.qmobile.qmobileui.utils.buildCustomFormatterBinding
import com.qmobile.qmobileui.utils.getChoiceListString
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.Locale

class FormatterTest {

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun testDateFormat() {
        val dateInput = "23!11!1937"
        if (Locale.getDefault() == Locale.US) {
            dateFormatTest(dateInput, expectedResult = "11/23/37", typeChoice = "shortDate")
            dateFormatTest(
                dateInput,
                expectedResult = "Nov 23, 1937",
                typeChoice = "mediumDate"
            )
            dateFormatTest(
                dateInput,
                expectedResult = "November 23, 1937",
                typeChoice = "longDate"
            )
            dateFormatTest(
                dateInput,
                expectedResult = "Tuesday, November 23, 1937",
                typeChoice = "fullDate"
            )
        }
        if (Locale.getDefault() == Locale.FRENCH) {
            dateFormatTest(dateInput, expectedResult = "23/11/37", typeChoice = "shortDate")
            dateFormatTest(
                dateInput,
                expectedResult = "23 nov. 1937",
                typeChoice = "mediumDate"
            )
            dateFormatTest(
                dateInput,
                expectedResult = "23 novembre 1937",
                typeChoice = "longDate"
            )
            dateFormatTest(
                dateInput,
                expectedResult = "mardi 23 novembre 1937",
                typeChoice = "fullDate"
            )
        }
    }

    @Test
    fun testBooleanFormat() {
        val inputIsTrue = true.toString()
        val inputIsFalse = false.toString()
        booleanFormatTest(
            inputIsTrue,
            expectedResult = 1.toString(),
            typeChoice = "boolInteger"
        )
        booleanFormatTest(inputIsTrue, expectedResult = "Yes", typeChoice = "noOrYes")
        booleanFormatTest(inputIsTrue, expectedResult = "True", typeChoice = "falseOrTrue")
        booleanFormatTest(
            inputIsFalse,
            expectedResult = 0.toString(),
            typeChoice = "boolInteger"
        )
        booleanFormatTest(inputIsFalse, expectedResult = "No", typeChoice = "noOrYes")
        booleanFormatTest(inputIsFalse, expectedResult = "False", typeChoice = "falseOrTrue")
    }

    @Test
    fun testTimeFormat() {
        val inputTime = "946693832"
        timeFormatTest(inputTime, "113480208800", "timeInteger")
        timeFormatTest(inputTime, "11:58:13 AM", "mediumTime")
        timeFormatTest(inputTime, "11:58 AM", "shortTime")
        timeFormatTest(inputTime, "11:58:13", "duration")
    }

    @Test
    fun testNumberFormat() {
        val number = 24.46812
        numberFormatTest(number.toString(), "24.468", "decimal")
        numberFormatTest(number.toString(), "24", "integer")
        numberFormatTest(number.toString(), "24.46812", "real")
        numberFormatTest(number.toString(), "twenty four dot forty seven", "spellOut")
        numberFormatTest(number.toString(), "24.47th", "ordinal")
        numberFormatTest(number.toString(), "2447.0%", "percent")
        numberFormatTest(number.toString(), "24.47€", "currencyEuro")
        numberFormatTest(number.toString(), "$24.47", "currencyDollar")
        numberFormatTest(number.toString(), "¥24.47", "currencyYen")
    }

    @Test
    fun testCustomFormat() {
        val customFormattersJsonObj = JSONObject(customFormattersJson)
        val customFormatters: Map<String, Map<String, FieldMapping>> =
            buildCustomFormatterBinding(customFormattersJsonObj)

        var tableName = "Table_3"
        var fieldName = "field_x"
        customFormatters[tableName]?.get(fieldName)?.let { fieldMapping ->
            Assert.assertEquals("localizedText", fieldMapping.binding)
            Assert.assertEquals("UX designers", getChoiceListString(fieldMapping, "0"))
            Assert.assertNull(getChoiceListString(fieldMapping, "AnyRandomText"))
        } ?: kotlin.run { Assert.fail() }

        tableName = "Table_1"
        fieldName = "field_1"

        customFormatters[tableName]?.get(fieldName)?.let { fieldMapping ->
            Assert.assertEquals("imageNamed", fieldMapping.binding)
            Assert.assertEquals("todo.png", getChoiceListString(fieldMapping, "abc"))
            Assert.assertEquals("pending.png", getChoiceListString(fieldMapping, "2"))
            Assert.assertNull(getChoiceListString(fieldMapping, "AnyRandomText"))
        } ?: kotlin.run { Assert.fail() }
    }

    private fun dateFormatTest(inputDate: String, expectedResult: String, typeChoice: String) =
        Assert.assertEquals(expectedResult, applyFormat(typeChoice, inputDate))

    private fun booleanFormatTest(
        inputDate: String,
        expectedResult: String,
        typeChoice: String
    ) = Assert.assertEquals(expectedResult, applyFormat(typeChoice, inputDate))

    private fun timeFormatTest(inputDate: String, expectedResult: String, typeChoice: String) =
        Assert.assertEquals(expectedResult, applyFormat(typeChoice, inputDate).trim())

    private fun numberFormatTest(input: String, expectedResult: String, typeChoice: String) =
        Assert.assertEquals(expectedResult, applyFormat(typeChoice, input))
}
