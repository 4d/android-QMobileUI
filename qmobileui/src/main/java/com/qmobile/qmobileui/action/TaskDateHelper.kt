package com.qmobile.qmobileui.action



fun getDayWord(days: Long): String = if (days <= 1) "day" else "days"
fun getHourWord(hours: Long): String = if (hours <= 1) "hour" else "hours"
fun getMinuteWord(minutes: Long): String = if (minutes <= 1) "minute" else "minutes"
fun getSecondWord(seconds: Long): String = if (seconds <= 1) "seconds" else "seconds"
