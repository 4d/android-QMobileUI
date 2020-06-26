/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.settings

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.qmarciset.androidmobileapi.auth.AuthenticationState
import com.qmarciset.androidmobileapi.connectivity.NetworkUtils
import com.qmarciset.androidmobiledatasync.app.BaseApp
import com.qmarciset.androidmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.LoginViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.factory.ConnectivityViewModelFactory
import com.qmarciset.androidmobiledatasync.viewmodel.factory.LoginViewModelFactory

fun SettingsFragment.getViewModel() {
    activity?.run {
        getLoginViewModel()
        getConnectivityViewModel()
    } ?: throw IllegalStateException("Invalid Activity")
}

fun SettingsFragment.setupObservers() {
    observeNetworkStatus()
    observeServerAccessible()
    observeAuthenticationState()
}

// LoginViewModel
fun SettingsFragment.getLoginViewModel() {
    loginViewModel = ViewModelProvider(
        this,
        LoginViewModelFactory(BaseApp.instance, delegate.loginApiService)
    )[LoginViewModel::class.java]
}

// ConnectivityViewModel
fun SettingsFragment.getConnectivityViewModel() {
    connectivityViewModel = ViewModelProvider(
        this,
        ConnectivityViewModelFactory(
            BaseApp.instance,
            delegate.connectivityManager
        )
    )[ConnectivityViewModel::class.java]
}

// Observe network status
fun SettingsFragment.observeNetworkStatus() {
    if (NetworkUtils.sdkNewerThanKitKat) {
        connectivityViewModel.networkStateMonitor.observe(
            viewLifecycleOwner,
            Observer {
                if (!firstTime) {
                    // first time, checkNetwork() is called in authentication observer event
                    checkNetwork()
                } else {
                    firstTime = false
                }
            }
        )
    }
}

// Observe if server is accessible
fun SettingsFragment.observeServerAccessible() {
    connectivityViewModel.serverAccessible.observe(
        viewLifecycleOwner,
        Observer { isAccessible ->
            if (delegate.isConnected()) {
                if (isAccessible) {
                    setLayoutServerAccessible()
                } else {
                    setLayoutServerNotAccessible()
                }
            } else {
                setLayoutNoInternet()
            }
        }
    )
}

// Observe authentication state
fun SettingsFragment.observeAuthenticationState() {
    loginViewModel.authenticationState.observe(
        viewLifecycleOwner,
        Observer { authenticationState ->
            when (authenticationState) {
                AuthenticationState.AUTHENTICATED -> {
                    checkNetwork()
                }
                else -> {
                }
            }
        }
    )
}
