/*
 * Created by Quentin Marciset on 22/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.model

import java.text.DateFormat

object QMobileFormatterConstants {
    val dateFormat: HashMap<String, Int> = hashMapOf(
        "shortDate" to DateFormat.SHORT,
        Pair("mediumDate", DateFormat.MEDIUM),
        "longDate" to DateFormat.LONG,
        "fullDate" to DateFormat.FULL
    )

    val timeFormat = hashMapOf<String, Int>(
        "shortTime" to DateFormat.SHORT,
        "mediumTime" to DateFormat.MEDIUM,
        "duration" to DateFormat.MEDIUM,
    )
    val booleanFormat = hashMapOf<String, String>(
        "localizedText.no" to "No",
        "localizedText.Yes" to "Yes",
        "localizedText.false" to "False",
        "localizedText.true" to "True"
    )
}
