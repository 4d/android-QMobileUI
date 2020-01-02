package com.qmarciset.androidmobileui.utils

import android.content.Context
import com.qmarciset.androidmobileui.R

fun Context.fetchResourceString(string: String): String {
    return when (string) {
        "try_refresh_data" -> this.getString(R.string.try_refresh_data)
        else -> ""
    }
}
