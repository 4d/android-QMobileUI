/*
 * Created by Quentin Marciset on 23/1/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.model.QMobileUiConstants

// Generic Layout Inflater
fun layoutFromTable(
    context: Context,
    tableName: String,
    defType: String = QMobileUiConstants.LAYOUT
): Int {
    return when ((tableName.split("_"))[0]) {
        "recyclerview" -> {
            context.resources.getIdentifier(tableName, defType, context.packageName)
        }
        "fragment" -> {
            context.resources.getIdentifier(tableName, defType, context.packageName)
        }
        else -> 0
    }
}

// custom snackbar
/*fun customSnackBar(
    activity: Activity,
    message: String,
    clickListener: View.OnClickListener? = null,
    actionName: String = "UNDO"
): Snackbar {
    val newSnackbar =
        Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
    clickListener?.let {
        newSnackbar.setAction(actionName, clickListener)
    }
    return newSnackbar
}*/

/**
 * Gets a resource string to display to the user. As it's called in a ViewModel, there is no
 * context available
 */
fun Context.fetchResourceString(string: String): String {
    return when (string) {
        "try_refresh_data" -> this.getString(R.string.try_refresh_data)
        else -> string
    }
}
