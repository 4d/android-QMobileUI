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
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileapi.auth.AuthenticationState
import com.qmobile.qmobileapi.auth.isEmailValid
import com.qmobile.qmobileapi.network.ApiClient
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.network.NetworkState
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.BaseActivity
import com.qmobile.qmobileui.activity.mainactivity.MainActivity
import com.qmobile.qmobileui.binding.bindImageFromDrawable
import com.qmobile.qmobileui.databinding.ActivityLoginBinding
import com.qmobile.qmobileui.network.RemoteUrlChanger
import com.qmobile.qmobileui.ui.SnackbarHelper
import com.qmobile.qmobileui.ui.clearViewInParent
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import com.qmobile.qmobileui.ui.setOnVeryLongClickListener
import com.qmobile.qmobileui.utils.hideKeyboard

class LoginActivity : BaseActivity(), RemoteUrlChanger {

    private var loggedOut = false

    lateinit var binding: ActivityLoginBinding
    private var remoteUrl = ""
    private var serverAccessibleDrawable: Drawable? = null
    private var serverNotAccessibleDrawable: Drawable? = null
    private lateinit var shakeAnimation: Animation

    // UI strings
    private lateinit var noInternetString: String
    private lateinit var serverAccessibleString: String
    private lateinit var serverNotAccessibleString: String

    // Views
    private lateinit var remoteUrlDisplayDialog: View
    private lateinit var imageNetworkStatus: ImageView
    private lateinit var remoteUrlMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve bundled parameter to know if we are coming from a logout action
        loggedOut = intent.getBooleanExtra(LOGGED_OUT, false)

        // If guest or already logged in, skip LoginActivity
        if (isAlreadyLoggedIn() || BaseApp.runtimeDataHolder.guestLogin) {
            startMainActivity(true)
        } else {
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

            binding =
                DataBindingUtil.setContentView(this, R.layout.activity_login)
            binding.lifecycleOwner = this

            initViewModels()
            initLayout()
            LoginActivityObserver(this, loginViewModel).initObservers()
        }
    }

    /**
     * Initializes layout components
     */
    private fun initLayout() {
        bindImageFromDrawable(binding.loginLogo, BaseApp.loginLogoDrawable)

        if (loggedOut) {
            SnackbarHelper.show(this, getString(R.string.login_logged_out), ToastMessage.Type.SUCCESS)
        }

        // Login button
        binding.loginButtonAuth.setOnSingleClickListener {
            login()
        }

        // Define a shake animation for when input is not valid
        shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake)

        binding.loginEmailInput.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateText()
            } else {
                binding.loginEmailContainer.error = null
            }
        }

        binding.loginEmailInput.setOnEditorActionListener { textView, actionId, keyEvent ->
            if ((keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_UP) ||
                actionId == EditorInfo.IME_ACTION_DONE
            ) {
                hideKeyboard(this)
                textView.clearFocus()
                if (validateText()) {
                    login()
                }
            }
            true
        }

        this.remoteUrl = BaseApp.sharedPreferencesHolder.remoteUrl
        initRemoteUrlDisplayDialog()

        binding.loginLogo.setOnVeryLongClickListener {
            checkNetwork(this)
            showRemoteUrlDisplayDialog()
        }
    }

    private fun validateText(): Boolean =
        if (binding.loginEmailInput.text.toString().isEmailValid()) {
            loginViewModel.setEmailValidState(true)
            binding.loginEmailContainer.error = null
            true
        } else {
            binding.loginEmailInput.startAnimation(shakeAnimation)
            binding.loginEmailContainer.error = getString(R.string.login_invalid_email)
            loginViewModel.setEmailValidState(false)
            false
        }

    private fun login() {
        if (connectivityViewModel.isConnected()) {
            binding.loginButtonAuth.isEnabled = false
            loginViewModel.login(email = binding.loginEmailInput.text.toString()) { }
        } else {
            SnackbarHelper.show(this, getString(R.string.no_internet), ToastMessage.Type.WARNING)
        }
    }

    private fun showRemoteUrlDisplayDialog() {
        remoteUrlDisplayDialog.clearViewInParent()

        MaterialAlertDialogBuilder(this)
            .setView(remoteUrlDisplayDialog)
            .setTitle(getString(R.string.pref_remote_url_title))
            .setPositiveButton(getString(R.string.remote_url_dialog_edit)) { _, _ ->
                showRemoteUrlEditDialog(remoteUrl, this@LoginActivity)
            }
            .setNegativeButton(getString(R.string.remote_url_dialog_done), null)
            .show()
    }

    private fun initRemoteUrlDisplayDialog() {
        serverAccessibleDrawable = ContextCompat.getDrawable(this, R.drawable.network_ok_circle)
        serverNotAccessibleDrawable =
            ContextCompat.getDrawable(this, R.drawable.network_nok_circle)
        noInternetString = getString(R.string.no_internet)
        serverAccessibleString = getString(R.string.server_accessible)
        serverNotAccessibleString = getString(R.string.server_not_accessible)

        remoteUrlDisplayDialog = LayoutInflater.from(this)
            .inflate(R.layout.login_remote_url_display_dialog, findViewById(android.R.id.content), false)
        imageNetworkStatus = remoteUrlDisplayDialog.findViewById(R.id.image_network_status)
        remoteUrlMessage = remoteUrlDisplayDialog.findViewById(R.id.remote_url_message)

        serverNotAccessibleDrawable?.let { imageNetworkStatus.setImageDrawable(it) }
        remoteUrlMessage.text = getString(R.string.remote_url_placeholder, remoteUrl, serverNotAccessibleString)
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
        when (authenticationState) {
            AuthenticationState.AUTHENTICATED -> {
                startMainActivity(false, loginViewModel.statusMessage)
            }
            AuthenticationState.INVALID_AUTHENTICATION -> {
                binding.loginButtonAuth.isEnabled = true
            }
            else -> {
                // Default state in LoginActivity
                binding.loginButtonAuth.isEnabled = true
            }
        }
    }

    override fun handleNetworkState(networkState: NetworkState) {
        // Checking network for remote Url dialog
        checkNetwork(this)
    }

    override fun onServerAccessible() {
        remoteUrlMessage.text = getString(R.string.remote_url_placeholder, remoteUrl, serverAccessibleString)
        serverAccessibleDrawable?.let { imageNetworkStatus.setImageDrawable(it) }
    }

    override fun onServerInaccessible() {
        remoteUrlMessage.text = getString(R.string.remote_url_placeholder, remoteUrl, serverNotAccessibleString)
        serverNotAccessibleDrawable?.let { imageNetworkStatus.setImageDrawable(it) }
    }

    override fun onNoInternet() {
        remoteUrlMessage.text = getString(R.string.remote_url_placeholder, remoteUrl, noInternetString)
        serverNotAccessibleDrawable?.let { imageNetworkStatus.setImageDrawable(it) }
    }

    override fun onValidRemoteUrlChange(newRemoteUrl: String) {
        BaseApp.sharedPreferencesHolder.remoteUrl = newRemoteUrl
        remoteUrl = newRemoteUrl
        refreshApiClients()
    }
}
