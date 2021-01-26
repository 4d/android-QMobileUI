/*
 * Created by Quentin Marciset on 23/1/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.model.QMobileUiConstants

// Generic Layout Inflater
fun layoutFromTable(
    context: Context,
    tableName: String,
    defType: String = QMobileUiConstants.LAYOUT
): Int {
    when ((tableName.split("_"))[0]) {
        "recyclerview" -> {
            return context.resources.getIdentifier(tableName, defType, context.packageName)
        }
        "fragment" -> {
            return context.resources.getIdentifier(tableName, defType, context.packageName)
        }
    }
    return 0
}

// custom snackbar
fun customSnackBar(
    activity: Activity,
    message: String,
    clickListener: View.OnClickListener?,
    actionName: String = "UNDO"
) {
    Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        .apply {
            clickListener ?: this.show() // If listener is null
            setAction(actionName, clickListener)
            setActionTextColor(
                ContextCompat.getColor(
                    BaseApp.instance,
                    R.color.colorAccent
                )
            )
            show()
        }
}

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
