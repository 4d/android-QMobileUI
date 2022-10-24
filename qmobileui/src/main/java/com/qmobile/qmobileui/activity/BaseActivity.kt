/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity

import android.app.AlertDialog
import android.net.ConnectivityManager
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.qmobile.qmobileapi.auth.AuthenticationState
import com.qmobile.qmobileapi.auth.isUrlValid
import com.qmobile.qmobileapi.network.AccessibilityApiService
import com.qmobile.qmobileapi.network.ApiClient
import com.qmobile.qmobileapi.network.LoginApiService
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.network.NetworkState
import com.qmobile.qmobiledatasync.toast.Event
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getLoginViewModel
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.mainactivity.MainActivity
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.network.RemoteUrlChanger
import com.qmobile.qmobileui.ui.SnackbarHelper
import com.qmobile.qmobileui.ui.clearViewInParent
import com.qmobile.qmobileui.ui.getShakeAnimation
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import com.qmobile.qmobileui.utils.ResourcesHelper

/**
 * Base AppCompatActivity for activities
 */
abstract class BaseActivity : AppCompatActivity() {

    companion object {
        // Constant used when returning to LoginActivity to display a toast message about logout
        const val LOGGED_OUT = "logged_out"

        // Constant used when going to MainActivity after a successful login from LoginActivity
        const val LOGIN_STATUS_TEXT = "loginStatusText"
    }

    lateinit var loginViewModel: LoginViewModel
    lateinit var connectivityViewModel: ConnectivityViewModel
    lateinit var connectivityManager: ConnectivityManager
    lateinit var accessibilityApiService: AccessibilityApiService
    lateinit var loginApiService: LoginApiService

    abstract fun handleAuthenticationState(authenticationState: AuthenticationState)
    abstract fun handleNetworkState(networkState: NetworkState)

    fun initViewModels() {
        loginViewModel = getLoginViewModel(this, loginApiService)
        connectivityViewModel =
            getConnectivityViewModel(this, connectivityManager, accessibilityApiService)
    }

    fun initObservers() {
        BaseActivityObserver(this, loginViewModel, connectivityViewModel).initObservers()
    }

    fun refreshApiClients() {
        ApiClient.clearApiClients()
        loginApiService = ApiClient.getLoginApiService(
            sharedPreferencesHolder = BaseApp.sharedPreferencesHolder,
            logBody = BaseApp.runtimeDataHolder.logLevel <= Log.VERBOSE,
            mapper = BaseApp.mapper
        )
        accessibilityApiService = ApiClient.getAccessibilityApiService(
            sharedPreferencesHolder = BaseApp.sharedPreferencesHolder,
            logBody = BaseApp.runtimeDataHolder.logLevel <= Log.VERBOSE,
            mapper = BaseApp.mapper
        )
        if (::loginViewModel.isInitialized) {
            loginViewModel.refreshAuthRepository(loginApiService)
        }
        if (::connectivityViewModel.isInitialized) {
            connectivityViewModel.refreshAccessibilityRepository(accessibilityApiService)
        }
    }

    fun showRemoteUrlEditDialog(remoteUrl: String, remoteUrlChanger: RemoteUrlChanger, onDialogDismiss: (() -> Unit)?) {
        val remoteUrlEditDialog = LayoutInflater.from(this)
            .inflate(R.layout.remote_url_edit_dialog, findViewById(android.R.id.content), false)
        val remoteUrlEditLayout = remoteUrlEditDialog.findViewById<TextInputLayout>(R.id.remote_url_edit_layout)
        val remoteUrlEditEditText = remoteUrlEditDialog.findViewById<TextInputEditText>(R.id.remote_url_edit_edittext)

        remoteUrlEditDialog.clearViewInParent()
        remoteUrlEditLayout.editText?.setText(remoteUrl)
        remoteUrlEditLayout.error = null

        MaterialAlertDialogBuilder(this)
            .setView(remoteUrlEditDialog)
            .setTitle(getString(R.string.pref_remote_url_title))
            .setPositiveButton(getString(R.string.remote_url_dialog_positive), null)
            .setNegativeButton(getString(R.string.remote_url_dialog_cancel)) { _, _ ->
                onDialogDismiss?.invoke()
            }
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnSingleClickListener {
                        val newRemoteUrl = remoteUrlEditLayout.editText?.text.toString()
                        if (newRemoteUrl.isUrlValid()) {
                            remoteUrlChanger.onValidRemoteUrlChange(newRemoteUrl)
                            queryNetwork(remoteUrlChanger)
                            dismiss()
                            onDialogDismiss?.invoke()
                        } else {
                            remoteUrlEditEditText.startAnimation(getShakeAnimation(this@BaseActivity))
                            remoteUrlEditLayout.error = getString(R.string.remote_url_invalid)
                        }
                    }
                }
                if (remoteUrlEditEditText.requestFocus()) {
                    window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
            }.show()
    }

    fun queryNetwork(networkChecker: NetworkChecker, toastError: Boolean = false) {
        if (connectivityViewModel.isConnected()) {
            if (this is MainActivity) {
                setCheckInProgress(true)
            }
            connectivityViewModel.isServerConnectionOk(toastError) { isAccessible ->
                if (this is MainActivity) {
                    setCheckInProgress(false)
                }
                if (isAccessible) {
                    networkChecker.onServerAccessible()
                } else {
                    networkChecker.onServerInaccessible()
                }
            }
        } else {
            networkChecker.onNoInternet()
        }
    }

    fun isAlreadyLoggedIn() = BaseApp.sharedPreferencesHolder.sessionToken.isNotEmpty()

    fun handleEvent(event: Event<ToastMessage.Holder>) {
        event.getContentIfNotHandled()?.let { toastMessageHolder: ToastMessage.Holder ->
            val message = ResourcesHelper.fetchResourceString(this.baseContext, toastMessageHolder.message)
            SnackbarHelper.show(this, message, toastMessageHolder.type)
        }
    }
}
