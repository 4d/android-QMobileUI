/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity

import android.net.ConnectivityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.connectivity.NetworkStateEnum
import com.qmobile.qmobileapi.connectivity.sdkNewerThanKitKat
import com.qmobile.qmobileapi.network.LoginApiService
import com.qmobile.qmobiledatasync.Event
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getLoginViewModel
import com.qmobile.qmobileui.utils.fetchResourceString
import timber.log.Timber

/**
 * Base AppCompatActivity for activities
 */
abstract class BaseActivity : AppCompatActivity() {

    companion object {
        // Constant used when returning to LoginActivity to display a toast message about logout
        const val LOGGED_OUT = "logged_out"
    }

    lateinit var loginApiService: LoginApiService

    lateinit var loginViewModel: LoginViewModel
    lateinit var connectivityViewModel: ConnectivityViewModel
    lateinit var connectivityManager: ConnectivityManager

    fun loginViewModelInitialized() = this::loginViewModel.isInitialized

    open fun getViewModels() {
        loginViewModel = getLoginViewModel(this, loginApiService)
        getConnectivityViewModel(this, connectivityManager)?.let { connectivityViewModel = it }
    }

    open fun observe() {
        observeMessage(loginViewModel.toastMessage.message)
        observeAuthenticationState()
        observeNetworkState()
    }

    // Observe authentication state
    private fun observeAuthenticationState() {
        loginViewModel.authenticationState.observe(
            this,
            Observer { authenticationState ->
                Timber.i("[AuthenticationState : $authenticationState]")
                handleAuthenticationState(authenticationState)
            }
        )
    }

    // Observe network status
    private fun observeNetworkState() {
        if (sdkNewerThanKitKat) {
            connectivityViewModel.networkStateMonitor.observe(
                this,
                Observer { networkState ->
                    Timber.i("[NetworkState : $networkState]")
                    handleNetworkState(networkState)
                }
            )
        }
    }

    abstract fun handleAuthenticationState(authenticationState: AuthenticationStateEnum)
    abstract fun handleNetworkState(networkState: NetworkStateEnum)

    // Observe any toast message
    fun observeMessage(message: LiveData<Event<String>>) {
        message.observe(this) { event -> showMessage(event) }
    }

    private fun showMessage(event: Event<String>) {
        event.getContentIfNotHandled()?.let { message ->
            val toastMessage = this.baseContext.fetchResourceString(message)
            if (toastMessage.isNotEmpty()) {
                Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
}
