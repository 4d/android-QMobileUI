/*
 * Created by qmarciset on 25/6/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity

import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import timber.log.Timber

class BaseActivityObserver(
    private val activity: BaseActivity,
    private val loginViewModel: LoginViewModel,
    private val connectivityViewModel: ConnectivityViewModel
) : BaseObserver {

    override fun initObservers() {
        observeAuthenticationState()
        observeNetworkState()
        observeConnectivityToastMessage()
        observeLoginToastMessage()
    }

    // Observe authentication state
    private fun observeAuthenticationState() {
        loginViewModel.authenticationState.observe(
            activity,
            { authenticationState ->
                Timber.i("[AuthenticationState : $authenticationState]")
                activity.handleAuthenticationState(authenticationState)
            }
        )
    }

    // Observe network status
    private fun observeNetworkState() {
        connectivityViewModel.networkStateMonitor.observe(
            activity,
            { networkState ->
                Timber.i("[NetworkState : $networkState]")
                activity.handleNetworkState(networkState)
            }
        )
    }

    private fun observeConnectivityToastMessage() {
        connectivityViewModel.toastMessage.message.observe(
            activity,
            { event ->
                activity.handleEvent(event)
            }
        )
    }

    private fun observeLoginToastMessage() {
        loginViewModel.toastMessage.message.observe(
            activity,
            { event ->
                activity.handleEvent(event)
            }
        )
    }
}
