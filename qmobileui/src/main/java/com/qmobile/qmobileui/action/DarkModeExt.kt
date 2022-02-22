package com.qmobile.qmobileui.action

import android.content.Context
import android.content.res.Configuration
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.qmobile.qmobileui.R

fun TextView.handleDarkMode(context: Context) {
    when (context.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
        Configuration.UI_MODE_NIGHT_YES -> {
            setBackgroundColor(ContextCompat.getColor(context, R.color.grey_dark_4))
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
        }
        Configuration.UI_MODE_NIGHT_NO,
        Configuration.UI_MODE_NIGHT_UNDEFINED -> {
        }
    }
}