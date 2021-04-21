/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import android.content.Intent
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.BaseActivity
import com.qmobile.qmobileui.activity.loginactivity.LoginActivity
import com.qmobile.qmobileui.utils.ToastHelper
import timber.log.Timber

/**
 * Goes back to login page
 */
fun MainActivity.startLoginActivity() {
    val intent = Intent(this, LoginActivity::class.java)
    intent.putExtra(BaseActivity.LOGGED_OUT, true)
    intent.addFlags(
        Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_NEW_TASK
    )
    startActivity(intent)
    finish()
}

/**
 * Tries to login while in guest mode. Might fail if no Internet connection
 */
fun MainActivity.tryAutoLogin() {
    if (connectivityViewModel.isConnected()) {
        loginViewModel.login { }
    } else {
        authenticationRequested = true
        Timber.d("No Internet connection, authenticationRequested")
        ToastHelper.show(this, resources.getString(R.string.no_internet_auto_login), MessageType.WARNING)
    }
}
