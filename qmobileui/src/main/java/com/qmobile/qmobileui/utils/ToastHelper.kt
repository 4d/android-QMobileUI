/*
 * Created by Quentin Marciset on 20/4/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import android.widget.Toast
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import com.qmobile.qmobiledatasync.toast.ToastMessage

object ToastHelper {

    fun show(context: Context, message: String, type: ToastMessage.Type = ToastMessage.Type.NEUTRAL) {
        if (message.isNotEmpty()) {
            when (type) {
                ToastMessage.Type.NEUTRAL -> showNeutral(context, message)
                ToastMessage.Type.SUCCESS -> showSuccess(context, message)
                ToastMessage.Type.WARNING -> showWarning(context, message)
                ToastMessage.Type.ERROR -> showError(context, message)
            }
        }
    }

    private fun showNeutral(context: Context, message: String) {
        DynamicToast.make(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(context: Context, message: String) {
        DynamicToast.makeSuccess(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showWarning(context: Context, message: String) {
        DynamicToast.makeWarning(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showError(context: Context, message: String) {
        DynamicToast.makeError(context, message, Toast.LENGTH_LONG).show()
    }
}
