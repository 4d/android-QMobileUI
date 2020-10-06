/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.loginactivity

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.qmobile.qmobileapi.auth.AuthenticationState
import com.qmobile.qmobileapi.connectivity.NetworkUtils
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.ConnectivityViewModelFactory
import com.qmobile.qmobiledatasync.viewmodel.factory.LoginViewModelFactory
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.utils.displaySnackBar
import kotlinx.android.synthetic.main.activity_login.*
import timber.log.Timber

fun LoginActivity.getViewModel() {
    getLoginViewModel()
    getConnectivityViewModel()
}

fun LoginActivity.setupObservers() {
    observeAuthenticationState()
    observeEmailValid()
    observeNetworkStatus()
}

// Get LoginViewModel
fun LoginActivity.getLoginViewModel() {
    loginViewModel = ViewModelProvider(
        this,
        LoginViewModelFactory(BaseApp.instance, loginApiService)
    )[LoginViewModel::class.java]
}

// Get ConnectivityViewModel
fun LoginActivity.getConnectivityViewModel() {
    if (NetworkUtils.sdkNewerThanKitKat) {
        connectivityViewModel = ViewModelProvider(
            this,
            ConnectivityViewModelFactory(BaseApp.instance, connectivityManager)
        )[ConnectivityViewModel::class.java]
    }
}

// Observe authentication state
fun LoginActivity.observeAuthenticationState() {
    loginViewModel.authenticationState.observe(
        this,
        Observer { authenticationState ->
            Timber.i("[AuthenticationState : $authenticationState]")
            when (authenticationState) {
                AuthenticationState.AUTHENTICATED -> {
                    startMainActivity(false)
                }
                AuthenticationState.INVALID_AUTHENTICATION -> {
                    login_button_auth.isEnabled = true
                    displaySnackBar(this, resources.getString(R.string.login_fail_snackbar))
                }
                else -> {
                    // Default state in LoginActivity
                    login_button_auth.isEnabled = true
                }
            }
        }
    )
}

// Observe if email is valid
fun LoginActivity.observeEmailValid() {
    loginViewModel.emailValid.observe(
        this,
        Observer { emailValid ->
            login_button_auth.isEnabled = emailValid
        }
    )
}

// Observe network status
fun LoginActivity.observeNetworkStatus() {
    if (NetworkUtils.sdkNewerThanKitKat) {
        connectivityViewModel.networkStateMonitor.observe(
            this,
            Observer {
            }
        )
    }
}
