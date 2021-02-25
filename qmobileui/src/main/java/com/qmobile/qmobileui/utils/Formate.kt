/*
 * Created by Quentin Marciset on 23/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */
@file:JvmName("Format")
package com.qmobile.qmobileui.utils

import java.util.Locale

// FormatNumber
fun number(format: String, value: String): String {
    return FormatterUtil.formatNumber(format, value).toString()
}

// FormatBoolean
fun formatBoolean(format: String, value: String): String {
    return FormatterUtil.formatBoolean(format, value.toBoolean()).toString()
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
