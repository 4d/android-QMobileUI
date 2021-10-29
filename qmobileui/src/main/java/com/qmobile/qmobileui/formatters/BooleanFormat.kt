/*
 * Created by qmarciset on 29/10/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.formatters

object BooleanFormat {

    fun applyFormat(format: String, baseText: String): String {
        return when (format) {
            "noOrYes" -> {
                if (baseText.toBoolean()) "Yes" else "No"
            }
            "falseOrTrue" -> {
                if (baseText.toBoolean()) "True" else "False"
            }
            "boolInteger" -> {
                if (baseText.toBoolean()) "1" else "0"
            }
            else -> {
                baseText
            }
        }
    }
}
