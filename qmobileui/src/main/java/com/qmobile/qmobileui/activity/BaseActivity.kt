/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity

import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.connectivity.NetworkStateEnum
import com.qmobile.qmobileapi.network.AccessibilityApiService
import com.qmobile.qmobileapi.network.LoginApiService
import com.qmobile.qmobiledatasync.toast.Event
import com.qmobile.qmobiledatasync.toast.ToastMessageHolder
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
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

    lateinit var loginViewModel: LoginViewModel
    lateinit var connectivityViewModel: ConnectivityViewModel
    lateinit var connectivityManager: ConnectivityManager
    lateinit var accessibilityApiService: AccessibilityApiService
    lateinit var loginApiService: LoginApiService

    fun loginViewModelInitialized() = this::loginViewModel.isInitialized
    fun connectivityViewModelInitialized() = this::connectivityViewModel.isInitialized

    abstract fun handleAuthenticationState(authenticationState: AuthenticationStateEnum)
    abstract fun handleNetworkState(networkState: NetworkStateEnum)
}
