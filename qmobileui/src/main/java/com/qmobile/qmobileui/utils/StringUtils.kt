/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import com.qmobile.qmobileui.R

/**
 * Gets a resource string to display to the user. As it's called in a ViewModel, there is no
 * context available
 */
fun Context.fetchResourceString(string: String): String {
    return when (string) {
        "try_refresh_data" -> this.getString(R.string.try_refresh_data)
        else -> ""
    }
}
