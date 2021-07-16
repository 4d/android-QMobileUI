/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.loginactivity

import android.app.AlertDialog
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
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
import com.qmobile.qmobileui.ui.RemoteUrlChange
import com.qmobile.qmobileui.ui.clearViewInParent
import com.qmobile.qmobileui.ui.setOnVeryLongClickListener
import com.qmobile.qmobileui.utils.ToastHelper
import com.qmobile.qmobileui.utils.hideKeyboard

class LoginActivity : BaseActivity(), RemoteUrlChange {

    private var loggedOut = false

    lateinit var binding: ActivityLoginBinding
    private var remoteUrl = ""
    private var serverAccessibleDrawable: Drawable? = null
    private var serverNotAccessibleDrawable: Drawable? = null
    private lateinit var remoteUrlDisplayDialogBuilder: MaterialAlertDialogBuilder
    private lateinit var shakeAnimation: Animation

    // UI strings
    private lateinit var noInternetString: String
    private lateinit var serverAccessibleString: String
    private lateinit var serverNotAccessibleString: String

    // Views
    private lateinit var remoteUrlDisplayDialog: View
    private lateinit var imageNetworkStatus: ImageView
    private lateinit var remoteUrlMessage: TextView
    private lateinit var rootViewGroup: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve bundled parameter to know if we are coming from a logout action
        loggedOut = intent.getBooleanExtra(LOGGED_OUT, false)

        // If guest or already logged in, skip LoginActivity
        if (BaseApp.sharedPreferencesHolder.sessionToken.isNotEmpty() || BaseApp.runtimeDataHolder.guestLogin) {
            startMainActivity(true)
        } else {

            // Init system services in onCreate()
            connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // Init Api service
            loginApiService = ApiClient.getLoginApiService(
                context = this,
                logBody = BaseApp.runtimeDataHolder.logLevel <= Log.VERBOSE
            )

            accessibilityApiService = ApiClient.getAccessibilityApiService(
                context = this,
                logBody = BaseApp.runtimeDataHolder.logLevel <= Log.VERBOSE
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

        // Define a shake animation for when input is not valid
        shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake)

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

        this.remoteUrl = BaseApp.sharedPreferencesHolder.remoteUrl
        initRemoteUrlDisplayDialog()

        remoteUrlDisplayDialogBuilder = MaterialAlertDialogBuilder(
            this,
            R.style.TitleThemeOverlay_MaterialComponents_MaterialAlertDialog
        )

        binding.loginLogo.setOnVeryLongClickListener {
            checkNetwork(this)
            showRemoteUrlDisplayDialog()
        }
    }

    private fun showRemoteUrlDisplayDialog() {
        remoteUrlDisplayDialog.clearViewInParent()

        remoteUrlDisplayDialogBuilder
            .setView(remoteUrlDisplayDialog)
            .setTitle(getString(R.string.pref_remote_url_title))
            .setPositiveButton(getString(R.string.remote_url_dialog_edit), null)
            .setNegativeButton(getString(R.string.remote_url_dialog_done), null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        showRemoteUrlEditDialog(remoteUrl, this@LoginActivity)
                    }
                }
            }.show()
    }

    private fun initRemoteUrlDisplayDialog() {
        serverAccessibleDrawable = ContextCompat.getDrawable(this, R.drawable.network_ok_circle)
        serverNotAccessibleDrawable =
            ContextCompat.getDrawable(this, R.drawable.network_nok_circle)
        noInternetString = resources.getString(R.string.no_internet)
        serverAccessibleString = resources.getString(R.string.server_accessible)
        serverNotAccessibleString = resources.getString(R.string.server_not_accessible)

        remoteUrlDisplayDialog = LayoutInflater.from(this)
            .inflate(R.layout.login_remote_url_display_dialog, rootViewGroup, false)
        imageNetworkStatus = remoteUrlDisplayDialog.findViewById(R.id.image_network_status)
        remoteUrlMessage = remoteUrlDisplayDialog.findViewById(R.id.remote_url_message)

        serverNotAccessibleDrawable?.let { imageNetworkStatus.setImageDrawable(it) }
        remoteUrlMessage.text =
            getString(R.string.remote_url_placeholder, remoteUrl, serverNotAccessibleString)
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
        checkNetwork(this)
    }

    override fun onServerAccessible() {
        remoteUrlMessage.text =
            getString(R.string.remote_url_placeholder, remoteUrl, serverAccessibleString)
        serverAccessibleDrawable?.let { imageNetworkStatus.setImageDrawable(it) }
    }

    override fun onServiceInaccessible() {
        remoteUrlMessage.text =
            getString(R.string.remote_url_placeholder, remoteUrl, serverNotAccessibleString)
        serverNotAccessibleDrawable?.let { imageNetworkStatus.setImageDrawable(it) }
    }

    override fun onNoInternet() {
        remoteUrlMessage.text =
            getString(R.string.remote_url_placeholder, remoteUrl, noInternetString)
        serverNotAccessibleDrawable?.let { imageNetworkStatus.setImageDrawable(it) }
    }

    override fun onValidRemoteUrlChange(newRemoteUrl: String) {
        BaseApp.sharedPreferencesHolder.remoteUrl = newRemoteUrl
        remoteUrl = newRemoteUrl
        refreshApiClients()
    }
}
