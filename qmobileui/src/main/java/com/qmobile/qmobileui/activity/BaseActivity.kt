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
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.auth.isUrlValid
import com.qmobile.qmobileapi.network.AccessibilityApiService
import com.qmobile.qmobileapi.network.ApiClient
import com.qmobile.qmobileapi.network.LoginApiService
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.network.NetworkStateEnum
import com.qmobile.qmobiledatasync.toast.Event
import com.qmobile.qmobiledatasync.toast.ToastMessageHolder
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getLoginViewModel
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.network.RemoteUrlChanger
import com.qmobile.qmobileui.ui.clearViewInParent
import com.qmobile.qmobileui.ui.getShakeAnimation
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import com.qmobile.qmobileui.utils.ResourcesHelper
import com.qmobile.qmobileui.utils.ToastHelper

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

    fun handleEvent(event: Event<ToastMessageHolder>) {
        event.getContentIfNotHandled()?.let { toastMessageHolder: ToastMessageHolder ->
            val message = ResourcesHelper.fetchResourceString(this.baseContext, toastMessageHolder.message)
            ToastHelper.show(this, message, toastMessageHolder.type)
        }
    }

    lateinit var loginViewModel: LoginViewModel
    lateinit var connectivityViewModel: ConnectivityViewModel
    lateinit var connectivityManager: ConnectivityManager
    lateinit var accessibilityApiService: AccessibilityApiService
    lateinit var loginApiService: LoginApiService

    private lateinit var remoteUrlEditDialogBuilder: MaterialAlertDialogBuilder

    abstract fun handleAuthenticationState(authenticationState: AuthenticationStateEnum)
    abstract fun handleNetworkState(networkState: NetworkStateEnum)

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

    fun showRemoteUrlEditDialog(remoteUrl: String, remoteUrlChanger: RemoteUrlChanger) {
        val remoteUrlEditDialog = LayoutInflater.from(this)
            .inflate(R.layout.remote_url_edit_dialog, findViewById(android.R.id.content), false)
        val remoteUrlEditLayout = remoteUrlEditDialog.findViewById<TextInputLayout>(R.id.remote_url_edit_layout)
        val remoteUrlEditEditText = remoteUrlEditDialog.findViewById<TextInputEditText>(R.id.remote_url_edit_edittext)
        remoteUrlEditDialogBuilder = MaterialAlertDialogBuilder(
            this,
            R.style.TitleThemeOverlay_MaterialComponents_MaterialAlertDialog
        )

        remoteUrlEditDialog.clearViewInParent()
        remoteUrlEditLayout.editText?.setText(remoteUrl)
        remoteUrlEditLayout.error = null

        remoteUrlEditDialogBuilder
            .setView(remoteUrlEditDialog)
            .setTitle(getString(R.string.pref_remote_url_title))
            .setPositiveButton(getString(R.string.remote_url_dialog_positive), null)
            .setNegativeButton(getString(R.string.remote_url_dialog_cancel), null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnSingleClickListener {
                        val newRemoteUrl = remoteUrlEditLayout.editText?.text.toString()
                        if (newRemoteUrl.isUrlValid()) {
                            remoteUrlChanger.onValidRemoteUrlChange(newRemoteUrl)
                            checkNetwork(remoteUrlChanger)
                            dismiss()
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

    fun checkNetwork(networkChecker: NetworkChecker) {
        if (connectivityViewModel.isConnected()) {
            connectivityViewModel.isServerConnectionOk(toastError = false) { isAccessible ->
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
}
