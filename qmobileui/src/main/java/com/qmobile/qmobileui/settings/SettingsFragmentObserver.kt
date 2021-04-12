/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.settings

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.qmobile.qmobileapi.connectivity.NetworkStateEnum
import com.qmobile.qmobileapi.connectivity.sdkNewerThanKitKat
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.ConnectivityViewModelFactory
import com.qmobile.qmobiledatasync.viewmodel.factory.LoginViewModelFactory

fun SettingsFragment.getViewModel() {
    getLoginViewModel()
    getConnectivityViewModel()
}

fun SettingsFragment.setupObservers() {
    observeNetworkStatus()
}

// LoginViewModel
fun SettingsFragment.getLoginViewModel() {
    activity?.run {
        loginViewModel = ViewModelProvider(
            this,
            LoginViewModelFactory(BaseApp.instance, delegate.loginApiService)
        )[LoginViewModel::class.java]
    } ?: throw IllegalStateException("Invalid Activity")
}

// ConnectivityViewModel
fun SettingsFragment.getConnectivityViewModel() {
    activity?.run {
        connectivityViewModel = ViewModelProvider(
            this,
            ConnectivityViewModelFactory(
                BaseApp.instance,
                delegate.connectivityManager,
                delegate.accessibilityApiService
            )
        )[ConnectivityViewModel::class.java]
    } ?: throw IllegalStateException("Invalid Activity")
}

// Observe network status
fun SettingsFragment.observeNetworkStatus() {
    if (sdkNewerThanKitKat) {
        connectivityViewModel.networkStateMonitor.observe(
            viewLifecycleOwner,
            Observer { networkState ->
                if (firstTime || !firstTime && networkState == NetworkStateEnum.CONNECTED) {
                    firstTime = false
                    checkNetwork()
                }
            }
        )
    }
}
