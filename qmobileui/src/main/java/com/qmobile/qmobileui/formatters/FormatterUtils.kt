/*
 * Created by qmarciset on 9/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.formatters

object FormatterUtils {

    fun applyFormat(format: String, baseText: Any): String {
        return when (format) {
            "noOrYes", "falseOrTrue", "boolInteger" -> {
                BooleanFormat.applyFormat(format, baseText.toString())
            }
            "timeInteger", "shortTime", "mediumTime", "duration" -> {
                TimeFormat.applyFormat(format, baseText.toString())
            }
            "fullDate", "longDate", "mediumDate", "shortDate" -> {
                DateFormat.applyFormat(format, baseText.toString())
            }
            "currencyEuro", "currencyYen", "currencyDollar", "percent", "ordinal", "integer", "real", "decimal" -> {
                NumberFormat.applyFormat(format, baseText.toString())
            }
            "spellOut" -> {
                SpellOutFormat.convertNumberToWord(baseText.toString())
            }
            "yaml", "jsonPrettyPrinted", "json", "jsonValues" -> {
                JsonYamlFormat.applyFormat(format, baseText)
            }
            else -> {
                baseText.toString()
            }
        }
    }
}
