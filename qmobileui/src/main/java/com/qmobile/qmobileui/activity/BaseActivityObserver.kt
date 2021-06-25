/*
 * Created by qmarciset on 25/6/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity

import androidx.lifecycle.Observer
import com.qmobile.qmobiledatasync.viewmodel.factory.getConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getLoginViewModel
import timber.log.Timber

fun BaseActivity.getViewModels() {
    loginViewModel = getLoginViewModel(this, loginApiService)
    connectivityViewModel =
        getConnectivityViewModel(this, connectivityManager, accessibilityApiService)
}

fun BaseActivity.observe() {
    observeAuthenticationState()
    observeNetworkState()
    observeConnectivityToastMessage()
    observeLoginToastMessage()
}

// Observe authentication state
fun BaseActivity.observeAuthenticationState() {
    loginViewModel.authenticationState.observe(
        this,
        Observer { authenticationState ->
            Timber.i("[AuthenticationState : $authenticationState]")
            handleAuthenticationState(authenticationState)
        }
    )
}

// Observe network status
fun BaseActivity.observeNetworkState() {
    connectivityViewModel.networkStateMonitor.observe(
        this,
        Observer { networkState ->
            Timber.i("[NetworkState : $networkState]")
            handleNetworkState(networkState)
        }
    )
}

fun BaseActivity.observeConnectivityToastMessage() {
    connectivityViewModel.toastMessage.message.observe(
        this,
        Observer { event ->
            handleEvent(event)
        }
    )
}

fun BaseActivity.observeLoginToastMessage() {
    loginViewModel.toastMessage.message.observe(
        this,
        Observer { event ->
            handleEvent(event)
        }
    )
}
