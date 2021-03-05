/*
 * Created by Quentin Marciset on 24/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils.converter

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@SuppressLint("SimpleDateFormat")
fun getTimeFromString(time: String): Calendar = Calendar.getInstance().apply {
    setTime(SimpleDateFormat("hh:mm:ss").parse(getTimeFromLong(time.toLong()))!!)
}

@SuppressLint("SimpleDateFormat")
fun getDateFromString(date: String): Calendar = Calendar.getInstance().apply {
    time = SimpleDateFormat("dd!MM!yyyy").parse(date)!!
}

@SuppressLint("SimpleDateFormat")
fun getTimeFromLong(timestamp: Long) =
    SimpleDateFormat("hh:mm:ss").format(Date(timestamp)).toString()