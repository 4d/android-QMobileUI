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
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.qmobile.qmobileapi.auth.AuthInfoHelper
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.auth.isEmailValid
import com.qmobile.qmobileapi.connectivity.NetworkStateEnum
import com.qmobile.qmobileapi.network.ApiClient
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.BaseActivity
import com.qmobile.qmobileui.activity.mainactivity.MainActivity
import com.qmobile.qmobileui.binding.bindImageFromDrawable
import com.qmobile.qmobileui.databinding.ActivityLoginBinding
import com.qmobile.qmobileui.ui.setOnVeryLongClickListener
import com.qmobile.qmobileui.utils.QMobileUiUtil
import com.qmobile.qmobileui.utils.ToastHelper
import com.qmobile.qmobileui.utils.hideKeyboard

class LoginActivity : BaseActivity() {

    private var loggedOut = false

    lateinit var binding: ActivityLoginBinding
    private var remoteUrl = ""
    private var serverAccessibleDrawable: Drawable? = null
    private var serverNotAccessibleDrawable: Drawable? = null
    private lateinit var remoteUrlAlertDialogBuilder: MaterialAlertDialogBuilder
    private lateinit var remoteUrlEditAlertDialogBuilder: MaterialAlertDialogBuilder
    private lateinit var authInfoHelper: AuthInfoHelper

    // UI strings
    private lateinit var noInternetString: String
    private lateinit var serverAccessibleString: String
    private lateinit var serverNotAccessibleString: String

    // Views
    private lateinit var loginRemoteUrlDialog: View
    private lateinit var loginRemoteUrlEditDialog: View
    private lateinit var imageNetworkStatus: ImageView
    private lateinit var remoteUrlMessage: TextView
    private lateinit var remoteUrlEditText: TextInputLayout
    private lateinit var rootViewGroup: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve bundled parameter to know if we are coming from a logout action
        loggedOut = intent.getBooleanExtra(LOGGED_OUT, false)

        authInfoHelper = AuthInfoHelper.getInstance(BaseApp.instance)

        // If guest or already logged in, skip LoginActivity
        if (authInfoHelper.sessionToken.isNotEmpty() || authInfoHelper.guestLogin) {
            startMainActivity(true)
        } else {

            // Init system services in onCreate()
            connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // Init Api service
            loginApiService = ApiClient.getLoginApiService(
                context = this,
                logBody = QMobileUiUtil.appUtilities.logLevel <= Log.VERBOSE
            )

            accessibilityApiService = ApiClient.getAccessibilityApiService(
                context = this,
                logBody = QMobileUiUtil.appUtilities.logLevel <= Log.VERBOSE
            )

            binding =
                DataBindingUtil.setContentView(this, R.layout.activity_login)
            binding.lifecycleOwner = this

            rootViewGroup = findViewById(android.R.id.content)

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
            ToastHelper.show(
                this,
                resources.getString(R.string.login_logged_out),
                MessageType.SUCCESS
            )
        }

        // Login button
        binding.loginButtonAuth.setOnClickListener {
            if (connectivityViewModel.isConnected()) {
                binding.loginButtonAuth.isEnabled = false
                loginViewModel.login(email = binding.loginEmailInput.text.toString()) { }
            } else {
                ToastHelper.show(
                    this,
                    resources.getString(R.string.no_internet),
                    MessageType.WARNING
                )
            }
        }

        // Define a shake animation for when mail is not valid
        val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake)

        binding.loginEmailInput.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                if (binding.loginEmailInput.text.toString().isEmailValid()) {
                    loginViewModel.emailValid.postValue(true)
                    binding.loginEmailContainer.error = null
                } else {
                    binding.loginEmailInput.startAnimation(shakeAnimation)
                    binding.loginEmailContainer.error =
                        resources.getString(R.string.login_invalid_email)
                    loginViewModel.emailValid.postValue(false)
                }
            } else {
                binding.loginEmailContainer.error = null
            }
        }

        binding.loginEmailInput.setOnEditorActionListener { textView, actionId, keyEvent ->
            if ((keyEvent != null && (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) ||
                (actionId == EditorInfo.IME_ACTION_DONE)
            ) {
                hideKeyboard(this)
                textView.clearFocus()
            }
            true
        }

        this.remoteUrl = authInfoHelper.remoteUrl
        initRemoteUrlDialog()
        initRemoteUrlEditDialog()

        remoteUrlAlertDialogBuilder = MaterialAlertDialogBuilder(
            this,
            R.style.TitleThemeOverlay_MaterialComponents_MaterialAlertDialog
        )
        remoteUrlEditAlertDialogBuilder = MaterialAlertDialogBuilder(
            this,
            R.style.TitleThemeOverlay_MaterialComponents_MaterialAlertDialog
        )

        binding.loginLogo.setOnVeryLongClickListener {
            checkNetwork()
            clearViewInParent(loginRemoteUrlDialog)
            remoteUrlAlertDialogBuilder
                .setView(loginRemoteUrlDialog)
                .setTitle(getString(R.string.pref_remote_url_title))
                .setPositiveButton(getString(R.string.remote_url_dialog_edit)) { _, _ ->

                    clearViewInParent(loginRemoteUrlEditDialog)
                    remoteUrlEditText.hint = getString(R.string.https_hint)

                    remoteUrlEditAlertDialogBuilder
                        .setView(loginRemoteUrlEditDialog)
                        .setTitle(getString(R.string.pref_remote_url_title))
                        .setPositiveButton(getString(R.string.remote_url_dialog_positive)) { _, _ ->
                            val newRemoteUrl = remoteUrlEditText.editText?.text.toString()
                            authInfoHelper.remoteUrl = newRemoteUrl
                            this.remoteUrl = newRemoteUrl
                            super.refreshApiClients()
                            checkNetwork()
                        }
                        .setNegativeButton(getString(R.string.remote_url_dialog_negative), null)
                        .show()
                }
                .setNegativeButton(getString(R.string.remote_url_dialog_negative), null)
                .show()
            true
        }
    }

    private fun initRemoteUrlDialog() {
        serverAccessibleDrawable = ContextCompat.getDrawable(this, R.drawable.network_ok_circle)
        serverNotAccessibleDrawable =
            ContextCompat.getDrawable(this, R.drawable.network_nok_circle)
        noInternetString = resources.getString(R.string.no_internet)
        serverAccessibleString = resources.getString(R.string.server_accessible)
        serverNotAccessibleString = resources.getString(R.string.server_not_accessible)

        loginRemoteUrlDialog = LayoutInflater.from(this)
            .inflate(R.layout.login_remote_url_dialog, rootViewGroup, false)
        imageNetworkStatus = loginRemoteUrlDialog.findViewById(R.id.image_network_status)
        remoteUrlMessage = loginRemoteUrlDialog.findViewById(R.id.remote_url_message)

        serverNotAccessibleDrawable?.let { imageNetworkStatus.setImageDrawable(it) }
        remoteUrlMessage.text =
            getString(R.string.remote_url_placeholder, remoteUrl, serverNotAccessibleString)
    }

    private fun initRemoteUrlEditDialog() {
        loginRemoteUrlEditDialog = LayoutInflater.from(this)
            .inflate(R.layout.login_remote_url_edit_dialog, rootViewGroup, false)
        remoteUrlEditText = loginRemoteUrlEditDialog.findViewById(R.id.remote_url_edit)
        remoteUrlEditText.editText?.setText(remoteUrl)
    }

    private fun clearViewInParent(view: View) {
        if (view.parent != null)
            (view.parent as ViewGroup).removeView(view)
    }

    /**
     * Checks network state, and adjust the indicator icon color
     */
    private fun checkNetwork() {
        if (connectivityViewModel.isConnected()) {
            connectivityViewModel.isServerConnectionOk(toastError = false) { isAccessible ->
                if (isAccessible) {
                    setLayoutServerAccessible()
                } else {
                    setLayoutServerNotAccessible()
                }
            }
        } else {
            setLayoutNoInternet()
        }
    }

    /**
     * Sets the indicator icon color and text to no Internet status
     */
    private fun setLayoutNoInternet() {
        remoteUrlMessage.text =
            getString(R.string.remote_url_placeholder, remoteUrl, noInternetString)
        serverNotAccessibleDrawable?.let { imageNetworkStatus.setImageDrawable(it) }
    }

    /**
     * Sets the indicator icon color and text to server not accessible
     */
    private fun setLayoutServerNotAccessible() {
        remoteUrlMessage.text =
            getString(R.string.remote_url_placeholder, remoteUrl, serverNotAccessibleString)
        serverNotAccessibleDrawable?.let { imageNetworkStatus.setImageDrawable(it) }
    }

    /**
     * Sets the indicator icon color and text to server accessible
     */
    private fun setLayoutServerAccessible() {
        remoteUrlMessage.text =
            getString(R.string.remote_url_placeholder, remoteUrl, serverAccessibleString)
        serverAccessibleDrawable?.let { imageNetworkStatus.setImageDrawable(it) }
    }

    /**
     * Goes to MainActivity, and finishes LoginActivity
     */
    private fun startMainActivity(skipAnimation: Boolean, loginStatusText: String = "") {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(LOGIN_STATUS_TEXT, loginStatusText)
        if (skipAnimation)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
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
                    v.clearFocus()
                    val imm: InputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    // Observe authentication state
    override fun handleAuthenticationState(authenticationState: AuthenticationStateEnum) {
        when (authenticationState) {
            AuthenticationStateEnum.AUTHENTICATED -> {
                startMainActivity(false, loginViewModel.statusMessage)
            }
            AuthenticationStateEnum.INVALID_AUTHENTICATION -> {
                binding.loginButtonAuth.isEnabled = true
            }
            else -> {
                // Default state in LoginActivity
                binding.loginButtonAuth.isEnabled = true
            }
        }
    }

    override fun handleNetworkState(networkState: NetworkStateEnum) {
        // Checking network for remote Url dialog
        checkNetwork()
    }
}
