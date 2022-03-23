package com.qmobile.qmobileui.action

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.R

fun TextView.handleDarkMode() {
    if (BaseApp.nightMode()) {
        setBackgroundColor(ContextCompat.getColor(context, R.color.grey_dark_4))
        setTextColor(ContextCompat.getColor(context, android.R.color.white))
    }
}
