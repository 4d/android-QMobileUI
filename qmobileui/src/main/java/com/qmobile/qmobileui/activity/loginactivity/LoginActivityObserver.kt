/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.loginactivity

import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.connectivity.sdkNewerThanKitKat
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.ConnectivityViewModelFactory
import com.qmobile.qmobiledatasync.viewmodel.factory.LoginViewModelFactory
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.mainactivity.observeToastMessage
import com.qmobile.qmobileui.utils.customSnackBar
import com.qmobile.qmobileui.utils.fetchResourceString
import kotlinx.android.synthetic.main.activity_login.*
import timber.log.Timber

fun LoginActivity.getViewModel() {
    getLoginViewModel()
    getConnectivityViewModel()
}

fun LoginActivity.setupObservers() {
    observeAuthenticationState()
    observeToastMessage()
    observeEmailValid()
    observeNetworkStatus()
}

// Get LoginViewModel
fun LoginActivity.getLoginViewModel() {
    loginViewModel = ViewModelProvider(
        this,
        LoginViewModelFactory(BaseApp.instance, loginApiService)
    )[LoginViewModel::class.java]
}

// Get ConnectivityViewModel
fun LoginActivity.getConnectivityViewModel() {
    if (sdkNewerThanKitKat) {
        connectivityViewModel = ViewModelProvider(
            this,
            ConnectivityViewModelFactory(BaseApp.instance, connectivityManager)
        )[ConnectivityViewModel::class.java]
    }
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
fun LoginActivity.observeToastMessage() {
    loginViewModel.toastMessage.message.observe(
        this,
        Observer { event ->
            event.getContentIfNotHandled()?.let { message ->
                val toastMessage = this.baseContext.fetchResourceString(message)
                if (toastMessage.isNotEmpty()) {
                    Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
                }
            }

//            val toastMessage = this.baseContext.fetchResourceString(message)
//            if (toastMessage.isNotEmpty()) {
//                Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
//                // To avoid the error toast to be displayed without performing a refresh again
//                loginViewModel.toastMessage.postValue("")
//            }
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
