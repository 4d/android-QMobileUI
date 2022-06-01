/*
 * Created by qmarciset on 14/3/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.utils

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
