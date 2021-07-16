/*
 * Created by Quentin Marciset on 24/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.formatters

import android.annotation.SuppressLint
import com.qmobile.qmobileapi.utils.safeParse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@SuppressLint("SimpleDateFormat")
fun getTimeFromString(time: String): Calendar = Calendar.getInstance().apply {
    val longTime: String = getTimeFromLong(time.toLong())
    val dateFormat = SimpleDateFormat("hh:mm:ss")
    dateFormat.safeParse(longTime)?.let { date ->
        setTime(date)
    }
}

@SuppressLint("SimpleDateFormat")
fun getDateFromString(date: String): Calendar = Calendar.getInstance().apply {
    val dateFormat = SimpleDateFormat("dd!MM!yyyy")
    dateFormat.safeParse(date)?.let { date ->
        time = date
    }
}

@SuppressLint("SimpleDateFormat")
fun getTimeFromLong(timestamp: Long) =
    SimpleDateFormat("hh:mm:ss").format(Date(timestamp)).toString()
