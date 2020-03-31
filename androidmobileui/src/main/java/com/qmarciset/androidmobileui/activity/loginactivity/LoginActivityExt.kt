/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.activity.loginactivity

import androidx.lifecycle.ViewModelProvider
import com.qmarciset.androidmobileapi.connectivity.NetworkUtils
import com.qmarciset.androidmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.LoginViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.factory.ConnectivityViewModelFactory
import com.qmarciset.androidmobiledatasync.viewmodel.factory.LoginViewModelFactory

fun LoginActivity.getLoginActivityViewModel() {

    // Get LoginViewModel
    loginViewModel = ViewModelProvider(
        this,
        LoginViewModelFactory(appInstance, loginApiService)
    )[LoginViewModel::class.java]

    // Get ConnectivityViewModel
    if (NetworkUtils.sdkNewerThanKitKat) {
        connectivityViewModel = ViewModelProvider(
            this,
            ConnectivityViewModelFactory(appInstance, connectivityManager)
        )[ConnectivityViewModel::class.java]
    }
}
