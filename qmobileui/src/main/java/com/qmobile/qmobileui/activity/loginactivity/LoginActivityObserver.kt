/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.loginactivity

import androidx.lifecycle.Observer
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.connectivity.sdkNewerThanKitKat
import com.qmobile.qmobiledatasync.viewmodel.factory.getConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getLoginViewModel
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.utils.customSnackBar
import kotlinx.android.synthetic.main.activity_login.*
import timber.log.Timber

fun LoginActivity.getViewModel() {
    loginViewModel = getLoginViewModel(this, loginApiService)
    connectivityViewModel =
        getConnectivityViewModel(this, connectivityManager, accessibilityApiService)
}

fun LoginActivity.setupObservers() {
    observeAuthenticationState()
    observeLoginToastMessage()
    observeEmailValid()
    observeNetworkStatus()
    observeConnectivityToastMessage()
}

// Observe authentication state
fun LoginActivity.observeAuthenticationState() {
    loginViewModel.authenticationState.observe(
        this,
        Observer { authenticationState ->
            Timber.d("[AuthenticationState : $authenticationState]")
            when (authenticationState) {
                AuthenticationStateEnum.AUTHENTICATED -> {
                    startMainActivity(false)
                }
                AuthenticationStateEnum.INVALID_AUTHENTICATION -> {
                    login_button_auth.isEnabled = true
                    customSnackBar(this, resources.getString(R.string.login_fail_snackbar), null)
                }
                else -> {
                    // Default state in LoginActivity
                    login_button_auth.isEnabled = true
                }
            }
        }
    )
}

// Observe any toast message
fun LoginActivity.observeLoginToastMessage() {
    loginViewModel.toastMessage.message.observe(
        this,
        Observer { event ->
            handleEvent(event)
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

fun LoginActivity.observeConnectivityToastMessage() {
    connectivityViewModel.toastMessage.message.observe(
        this,
        Observer { event ->
            handleEvent(event)
        }
    )
}

// Observe network status
fun LoginActivity.observeNetworkStatus() {
    if (sdkNewerThanKitKat) {
        connectivityViewModel.networkStateMonitor.observe(
            this,
            Observer {
            }
        )
    }
}
