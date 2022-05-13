package com.qmobile.qmobileui.utils

import com.qmobile.qmobileui.action.AM_KEY
import com.qmobile.qmobileui.action.PM_KEY

fun getAmPmFormattedTime(baseText: String): String {
    val totalSecs = baseText.toLong() / 1000
    val hours = totalSecs / 3600;
    val minutes = (totalSecs % 3600) / 60;

    return if (hours >= 12) {
        "${hours - 12}:$minutes $PM_KEY"
    } else {
        "$hours:$minutes $AM_KEY"
    }

}