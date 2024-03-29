/*
 * Created by Maturi Karthik on 31/3/2021.
 * 4D SAS
 * Copyright (c) 2021 Maturi Karthik. All rights reserved.
 */

package com.qmobile.qmobileui

import com.qmobile.qmobiledatasync.utils.FieldMapping
import com.qmobile.qmobileui.formatters.FormatterUtils
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
        timeFormatTest(inputTime, "946693832", "timeInteger")
        timeFormatTest(inputTime, "10:58 PM", "shortTime")
        timeFormatTest(inputTime, "262:58:13", "duration")
        timeFormatTest(inputTime, "10:58:13 PM", "mediumTime")
    }

    @Test
    fun testAmPmFormat() {
        val inputTime = "9540000"
        timeFormatTest(inputTime, "2:39:00 AM", "mediumTime")
        timeFormatTest(inputTime, "9540000", "timeInteger")
        timeFormatTest(inputTime, "2:39 AM", "shortTime")
        timeFormatTest(inputTime, "02:39:00", "duration")
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
        numberFormatTest(number.toString(), "¥ 24", "currencyYen")
    }

    @Test
    fun testCustomFormat() {
        val customFormattersJsonObj = JSONObject(customFormattersJson)
        val customFormatters: Map<String, Map<String, FieldMapping>> =
            FieldMapping.buildCustomFormatterBinding(customFormattersJsonObj)

        var tableName = "Table_3"
        var fieldName = "field_x"
        customFormatters[tableName]?.get(fieldName)?.let { fieldMapping ->
            Assert.assertEquals("localizedText", fieldMapping.binding)
            Assert.assertEquals("UX designers", fieldMapping.getStringInChoiceList("0"))
            Assert.assertNull(fieldMapping.getStringInChoiceList("AnyRandomText"))
        } ?: kotlin.run { Assert.fail() }

        tableName = "Table_1"
        fieldName = "field_1"
        customFormatters[tableName]?.get(fieldName)?.let { fieldMapping ->
            Assert.assertEquals("imageNamed", fieldMapping.binding)
            Assert.assertEquals("todo.png", fieldMapping.getStringInChoiceList("abc"))
            Assert.assertEquals("pending.png", fieldMapping.getStringInChoiceList("2"))
            Assert.assertNull(fieldMapping.getStringInChoiceList("AnyRandomText"))
        } ?: kotlin.run { Assert.fail() }

        tableName = "Table_4"
        fieldName = "relationField.field_1"
        customFormatters[tableName]?.get(fieldName)?.let { fieldMapping ->
            Assert.assertEquals("localizedText", fieldMapping.binding)
            Assert.assertEquals("UX designers", fieldMapping.getStringInChoiceList("0"))
            Assert.assertNull(fieldMapping.getStringInChoiceList("AnyRandomText"))
        } ?: kotlin.run { Assert.fail() }
    }

    private fun dateFormatTest(inputDate: String, expectedResult: String, typeChoice: String) =
        Assert.assertEquals(expectedResult, FormatterUtils.applyFormat(typeChoice, inputDate))

    private fun booleanFormatTest(
        inputDate: String,
        expectedResult: String,
        typeChoice: String
    ) = Assert.assertEquals(expectedResult, FormatterUtils.applyFormat(typeChoice, inputDate))

    private fun timeFormatTest(inputDate: String, expectedResult: String, typeChoice: String) =
        Assert.assertEquals(expectedResult, FormatterUtils.applyFormat(typeChoice, inputDate).trim())

    private fun numberFormatTest(input: String, expectedResult: String, typeChoice: String) =
        Assert.assertEquals(expectedResult, FormatterUtils.applyFormat(typeChoice, input))
}
