/*
 * Created by Quentin Marciset on 23/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */
@file:JvmName("Formate")
package com.qmobile.qmobileui.utils

import java.util.Locale

// FormatNumber
fun number(format: String, value: String): String {
    return FormatterUtil.formatNumber(format, value).toString()
}

// FormatBoolean
fun boolean(format: String, value: Boolean): String {
    return FormatterUtil.formatBoolean(format, value).toString()
}

// FormatTime
fun time(format: String, time: String) = FormatterUtil.formatTime(
    format,
    Locale.getDefault(),
    time
).toString()

// FormatDate
fun date(format: String, date: String) =
    FormatterUtil.formatDate(format, Locale.getDefault(), date).toString()

fun getTime(): String = "00:26:89" // Only For Test
fun getDate(): String = "24-02-2021" // Only For Test
fun getNumber(): String = "24.468" // Only For Test
