/*
 * Created by Quentin Marciset on 22/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.model

import java.text.DateFormat

object QMobileFormatterConstants {
    val dateFormat: Map<String, Int> = mapOf(
        "shortDate" to DateFormat.SHORT,
        "mediumDate" to DateFormat.MEDIUM,
        "longDate" to DateFormat.LONG,
        "fullDate" to DateFormat.FULL
    )

    val timeFormat: Map<String, Int> = mapOf(
        "shortTime" to DateFormat.SHORT,
        "mediumTime" to DateFormat.MEDIUM,
        "duration" to DateFormat.MEDIUM,
    )
}
