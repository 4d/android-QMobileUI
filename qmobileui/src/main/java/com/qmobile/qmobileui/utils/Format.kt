/*
 * Created by Quentin Marciset on 23/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */
@file:JvmName("Format")
package com.qmobile.qmobileui.utils

import java.util.Locale

// FormatNumber
fun number(format: String, number: String?): String = when {
    (number == null) -> "null"
    else -> FormatterUtil.formatNumber(format, number).toString()
}

// FormatBoolean
fun formatBoolean(format: String, value: String?): String = when {
    (value == null) -> "null"
    else -> FormatterUtil.formatBoolean(format, value.toBoolean()).toString()
}

// FormatTime
fun time(format: String, time: String?) = when {
    (time == null) -> "null"
    else -> FormatterUtil.formatTime(
        format,
        Locale.getDefault(),
        time
    ).toString()
}


// FormatDate
fun date(format: String, date: String?) = when {
    (date == null) -> "null"
    else -> FormatterUtil.formatDate(
        format,
        Locale.getDefault(), date
    ).toString()
}

