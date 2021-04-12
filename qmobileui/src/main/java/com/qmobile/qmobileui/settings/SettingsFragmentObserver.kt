/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.settings

import androidx.lifecycle.Observer
import com.qmobile.qmobileapi.connectivity.NetworkStateEnum
import com.qmobile.qmobileapi.connectivity.sdkNewerThanKitKat
import com.qmobile.qmobiledatasync.viewmodel.factory.getConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getLoginViewModel

fun SettingsFragment.getViewModel() {
    loginViewModel = getLoginViewModel(activity, delegate.loginApiService)
    connectivityViewModel = getConnectivityViewModel(
        activity,
        delegate.connectivityManager,
        delegate.accessibilityApiService
    )
}

fun SettingsFragment.setupObservers() {
    observeNetworkStatus()
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
