package com.qmarciset.androidmobileui.utils

import android.app.Activity
import android.view.View
import com.google.android.material.snackbar.Snackbar

/**
 * Displays a simple SnackBar
 */
fun displaySnackBar(activity: Activity?, message: String) {
    activity?.let {
        buildSnackBar(it, message).show()
    }
}

/**
 * Builds a simple SnackBar
 */
fun buildSnackBar(activity: Activity, message: String): Snackbar {
    return Snackbar.make(activity.findViewById<View>(android.R.id.content), message, Snackbar.LENGTH_LONG)
}
