/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.qmobile.qmobiledatasync.Event
import com.qmobile.qmobileui.utils.fetchResourceString

/**
 * Base AppCompatActivity for activities
 */
abstract class BaseActivity : AppCompatActivity() {

    companion object {
        // Constant used when returning to LoginActivity to display a toast message about logout
        const val LOGGED_OUT = "logged_out"
    }

    fun handleEvent(event: Event<String>) {
        event.getContentIfNotHandled()?.let { message ->
            val toastMessage = this.baseContext.fetchResourceString(message)
            if (toastMessage.isNotEmpty()) {
                Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
}
