/*
 * Created by qmarciset on 25/6/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity

import androidx.lifecycle.Lifecycle
import com.qmobile.qmobiledatasync.utils.launchAndCollectIn
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
        loginViewModel.authenticationState.launchAndCollectIn(
            activity,
            Lifecycle.State.STARTED
        ) { authenticationState ->
            Timber.i("[AuthenticationState : $authenticationState]")
            activity.handleAuthenticationState(authenticationState)
        }
//        activity.collectWhenStarted(loginViewModel.authenticationState) { authenticationState ->
//            Timber.i("[AuthenticationState : $authenticationState]")
//            activity.handleAuthenticationState(authenticationState)
//        }
    }

    // Observe network status
    private fun observeNetworkState() {
        connectivityViewModel.networkStateMonitor.observe(
            activity
        ) { networkState ->
            Timber.i("[NetworkState : $networkState]")
            activity.handleNetworkState(networkState)
        }
    }

    private fun observeConnectivityToastMessage() {
        connectivityViewModel.toastMessage.message.launchAndCollectIn(activity, Lifecycle.State.STARTED) { event ->
            activity.handleEvent(event)
        }
//        activity.collectWhenStarted(connectivityViewModel.toastMessage.message) { event ->
//            activity.handleEvent(event)
//        }
    }

    private fun observeLoginToastMessage() {
        loginViewModel.toastMessage.message.launchAndCollectIn(activity, Lifecycle.State.STARTED) { event ->
            activity.handleEvent(event)
        }
//        activity.collectWhenStarted(loginViewModel.toastMessage.message) { event ->
//            activity.handleEvent(event)
//        }
    }
}
