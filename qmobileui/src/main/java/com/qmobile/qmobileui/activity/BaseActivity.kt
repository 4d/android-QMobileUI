/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity

import androidx.appcompat.app.AppCompatActivity
import com.qmobile.qmobiledatasync.toast.Event
import com.qmobile.qmobiledatasync.toast.ToastMessageHolder
import com.qmobile.qmobileui.utils.ToastHelper
import com.qmobile.qmobileui.utils.fetchResourceString

/**
 * Base AppCompatActivity for activities
 */
abstract class BaseActivity : AppCompatActivity() {

    companion object {
        // Constant used when returning to LoginActivity to display a toast message about logout
        const val LOGGED_OUT = "logged_out"
        // Constant used when going to MainActivity after a successful login from LoginActivity
        const val LOGIN_STATUS_TEXT = "loginStatusText"
    }

    fun handleEvent(event: Event<ToastMessageHolder>) {
        event.getContentIfNotHandled()?.let { toastMessageHolder: ToastMessageHolder ->
            val message = this.baseContext.fetchResourceString(toastMessageHolder.message)
            ToastHelper.show(this, message, toastMessageHolder.type)
        }
    }
}
