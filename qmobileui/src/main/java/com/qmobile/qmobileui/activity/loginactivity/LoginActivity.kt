/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.loginactivity

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.databinding.DataBindingUtil
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
import com.qmobile.qmobileui.utils.QMobileUiUtil
import com.qmobile.qmobileui.utils.ToastHelper
import com.qmobile.qmobileui.utils.hideKeyboard

class LoginActivity : BaseActivity() {

    private var loggedOut = false

    lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve bundled parameter to know if we are coming from a logout action
        loggedOut = intent.getBooleanExtra(LOGGED_OUT, false)

        val authInfoHelper = AuthInfoHelper.getInstance(BaseApp.instance)

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

            setupViewModels()
            initLayout()
            setupObservers()
        }
    }

    /**
     * Initializes layout components
     */
    private fun initLayout() {

        bindImageFromDrawable(binding.loginLogo, BaseApp.loginLogoDrawable)

        if (loggedOut) {
            ToastHelper.show(this, resources.getString(R.string.login_logged_out), MessageType.SUCCESS)
        }

        // Login button
        binding.loginButtonAuth.setOnClickListener {
            if (connectivityViewModel.isConnected()) {
                binding.loginButtonAuth.isEnabled = false
                loginViewModel.login(email = binding.loginEmailInput.text.toString()) { }
            } else {
                ToastHelper.show(this, resources.getString(R.string.no_internet), MessageType.WARNING)
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
                    binding.loginEmailContainer.error = resources.getString(R.string.login_invalid_email)
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
    }

    /**
     * Goes to MainActivity, and finishes LoginActivity
     */
    fun startMainActivity(skipAnimation: Boolean, loginStatusText: String = "") {
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
        // Nothing to do
    }
}
