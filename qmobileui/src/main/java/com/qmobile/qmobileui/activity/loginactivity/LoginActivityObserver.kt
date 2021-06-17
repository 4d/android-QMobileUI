/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.loginactivity

import android.view.View
import androidx.lifecycle.Observer
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobiledatasync.viewmodel.factory.getConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getLoginViewModel
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
    observeDataLoading()
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
                    startMainActivity(false, loginViewModel.statusMessage)
                }
                AuthenticationStateEnum.INVALID_AUTHENTICATION -> {
                    binding.loginButtonAuth.isEnabled = true
                }
                else -> {
                    // Default state in LoginActivity
                    binding.loginButtonAuth.isEnabled = true
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
            binding.loginButtonAuth.isEnabled = emailValid
        }
    )
}

// Observe if login request in progress
fun LoginActivity.observeDataLoading() {
    loginViewModel.dataLoading.observe(
        this,
        Observer { dataLoading ->
            binding.loginProgressbar.visibility = if (dataLoading == true) View.VISIBLE else View.GONE
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
    connectivityViewModel.networkStateMonitor.observe(
        this,
        Observer {
        }
    )
}
