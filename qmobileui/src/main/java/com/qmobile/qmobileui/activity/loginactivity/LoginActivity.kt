/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.loginactivity

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileapi.auth.AuthenticationState
import com.qmobile.qmobileapi.auth.isEmailValid
import com.qmobile.qmobileapi.model.error.AuthorizedStatus
import com.qmobile.qmobileapi.network.ApiClient
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.network.NetworkState
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobiledatasync.utils.LoginHandler
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.BaseActivity
import com.qmobile.qmobileui.activity.mainactivity.ActivityResultController
import com.qmobile.qmobileui.activity.mainactivity.ActivityResultControllerImpl
import com.qmobile.qmobileui.activity.mainactivity.MainActivity
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.network.RemoteUrlChanger
import com.qmobile.qmobileui.ui.SnackbarHelper
import com.qmobile.qmobileui.ui.clearViewInParent
import com.qmobile.qmobileui.utils.PermissionChecker
import com.qmobile.qmobileui.utils.PermissionCheckerImpl
import com.qmobile.qmobileui.utils.hideKeyboard

class LoginActivity : BaseActivity(), RemoteUrlChanger, PermissionChecker, ActivityResultController {

    private var loggedOut = false
    private var authorizedStatus = AuthorizedStatus.AUTHORIZED

    private var remoteUrl = ""
    private var serverAccessibleDrawable: Drawable? = null
    private var serverNotAccessibleDrawable: Drawable? = null
    private lateinit var loginHandler: LoginHandler

    // Views
    private lateinit var remoteUrlDisplayDialog: View
    private lateinit var imageNetworkStatus: ImageView
    private lateinit var remoteUrlMessage: TextView

    override val activityResultControllerImpl = ActivityResultControllerImpl(this)
    override val permissionCheckerImpl = PermissionCheckerImpl(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // If guest or already logged in, skip LoginActivity
        if (isAlreadyLoggedIn() || BaseApp.runtimeDataHolder.guestLogin) {
            startMainActivity(true)
        } else {
            // Retrieve bundled parameter to know if we are coming from a logout action
            loggedOut = intent.getBooleanExtra(LOGGED_OUT, false)
            authorizedStatus =
                (intent.getSerializableExtra(AUTHORIZED_STATUS) as? AuthorizedStatus?) ?: AuthorizedStatus.AUTHORIZED

            // Init system services in onCreate()
            connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // Init Api service
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

            loginHandler = BaseApp.genericResourceHelper.getLoginForm(this)

            // Init ApiClients
            refreshApiClients()

            initViewModels()
            initLayout()
            LoginActivityObserver(this, loginViewModel, loginHandler).initObservers()
        }
    }

    /**
     * Initializes layout components
     */
    private fun initLayout() {
        when {
            loggedOut && authorizedStatus == AuthorizedStatus.UNAUTHORIZED -> SnackbarHelper.show(
                this,
                getString(R.string.missing_session_file_logout),
                ToastMessage.Type.WARNING
            )
            loggedOut && authorizedStatus == AuthorizedStatus.UNAUTHORIZED_WITH_TASKS -> SnackbarHelper.show(
                this,
                getString(R.string.missing_session_file_logout_with_pending_tasks),
                ToastMessage.Type.WARNING
            )
            loggedOut && authorizedStatus == AuthorizedStatus.AUTHORIZED -> SnackbarHelper.show(
                this,
                getString(R.string.login_logged_out),
                ToastMessage.Type.SUCCESS
            )
        }

        if (loggedOut) {
            loginHandler.onLogout()
        }

        loginHandler.initLayout()

        this.remoteUrl = BaseApp.sharedPreferencesHolder.remoteUrl
        initRemoteUrlDisplayDialog()
    }

    fun login(input: String) {
        if ((validateMail(input) || !loginHandler.ensureValidMail) && loginHandler.validate(input)) {
            queryNetwork(object : NetworkChecker {
                override fun onServerAccessible() {
                    loginViewModel.login(email = input) { success, _ ->
                        if (success) {
                            checkIfShouldClearPreviousUserData(input)
                            loginHandler.onLoginSuccessful()
                        } else {
                            loginHandler.onLoginUnsuccessful()
                        }
                    }
                }

                override fun onServerInaccessible() {
                    SnackbarHelper.show(
                        this@LoginActivity,
                        getString(R.string.server_not_accessible),
                        ToastMessage.Type.WARNING
                    )
                    loginHandler.onLoginUnsuccessful()
                }

                override fun onNoInternet() {
                    SnackbarHelper.show(this@LoginActivity, getString(R.string.no_internet), ToastMessage.Type.WARNING)
                    loginHandler.onLoginUnsuccessful()
                }
            })
        } else {
            loginHandler.onInputInvalid()
        }
    }

    fun validateMail(input: String): Boolean {
        return input.isEmailValid()
    }

    private fun showRemoteUrlDisplayDialog() {
        remoteUrlDisplayDialog.clearViewInParent()

        MaterialAlertDialogBuilder(this)
            .setView(remoteUrlDisplayDialog)
            .setTitle(getString(R.string.pref_remote_url_title))
            .setPositiveButton(getString(R.string.remote_url_dialog_edit)) { _, _ ->
                showRemoteUrlEditDialog(remoteUrl, this@LoginActivity) {
                    showRemoteUrlDisplayDialog()
                }
            }
            .setNegativeButton(getString(R.string.remote_url_dialog_done), null)
            .show()
    }

    private fun initRemoteUrlDisplayDialog() {
        serverAccessibleDrawable = ContextCompat.getDrawable(this, R.drawable.network_ok_circle)
        serverNotAccessibleDrawable =
            ContextCompat.getDrawable(this, R.drawable.network_nok_circle)

        remoteUrlDisplayDialog = LayoutInflater.from(this)
            .inflate(R.layout.login_remote_url_display_dialog, findViewById(android.R.id.content), false)
        imageNetworkStatus = remoteUrlDisplayDialog.findViewById(R.id.image_network_status)
        remoteUrlMessage = remoteUrlDisplayDialog.findViewById(R.id.remote_url_message)

        serverNotAccessibleDrawable?.let { imageNetworkStatus.setImageDrawable(it) }
        remoteUrlMessage.text =
            getString(R.string.remote_url_placeholder, remoteUrl, getString(R.string.server_not_accessible))
    }

    /**
     * Goes to MainActivity, and finishes LoginActivity
     */
    private fun startMainActivity(skipAnimation: Boolean, loginStatusText: String = "") {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(LOGIN_STATUS_TEXT, loginStatusText)
        if (skipAnimation) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
        finish()
    }

    /**
     * Hides keyboard layout when user touches outside input text area
     */
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event != null && event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            v?.let currentFocus@{
                if (v !is EditText) return@currentFocus
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    hideKeyboard(this)
                    v.clearFocus()
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    // Observe authentication state
    override fun handleAuthenticationState(authenticationState: AuthenticationState) {
        if (authenticationState == AuthenticationState.AUTHENTICATED) {
            startMainActivity(false, loginViewModel.statusMessage)
        }
    }

    private fun checkIfShouldClearPreviousUserData(newEmail: String) {
        val lastLoginMail = BaseApp.sharedPreferencesHolder.lastLoginMail
        if (lastLoginMail != newEmail) {
            clearSpecificData()
            BaseApp.sharedPreferencesHolder.lastLoginMail = newEmail
        }
    }

    override fun handleNetworkState(networkState: NetworkState) {
        // Checking network for remote Url dialog
        queryNetwork(this)
    }

    override fun onServerAccessible() {
        remoteUrlMessage.text =
            getString(R.string.remote_url_placeholder, remoteUrl, getString(R.string.server_accessible))
        serverAccessibleDrawable?.let { imageNetworkStatus.setImageDrawable(it) }
    }

    override fun onServerInaccessible() {
        remoteUrlMessage.text =
            getString(R.string.remote_url_placeholder, remoteUrl, getString(R.string.server_not_accessible))
        serverNotAccessibleDrawable?.let { imageNetworkStatus.setImageDrawable(it) }
    }

    override fun onNoInternet() {
        remoteUrlMessage.text = getString(R.string.remote_url_placeholder, remoteUrl, getString(R.string.no_internet))
        serverNotAccessibleDrawable?.let { imageNetworkStatus.setImageDrawable(it) }
    }

    override fun onValidRemoteUrlChange(newRemoteUrl: String) {
        BaseApp.sharedPreferencesHolder.remoteUrl = newRemoteUrl
        remoteUrl = newRemoteUrl
        remoteUrlMessage.text = getString(R.string.remote_url_checking)
        refreshApiClients()
    }

    fun showRemoteUrlDialog() {
        queryNetwork(this)
        showRemoteUrlDisplayDialog()
    }
}
