/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.activity.loginactivity

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.qmarciset.androidmobileapi.auth.AuthInfoHelper
import com.qmarciset.androidmobileapi.auth.AuthenticationState
import com.qmarciset.androidmobileapi.auth.isEmailValid
import com.qmarciset.androidmobileapi.connectivity.NetworkUtils
import com.qmarciset.androidmobileapi.network.ApiClient
import com.qmarciset.androidmobileapi.network.LoginApiService
import com.qmarciset.androidmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.LoginViewModel
import com.qmarciset.androidmobileui.R
import com.qmarciset.androidmobileui.activity.BaseActivity
import com.qmarciset.androidmobileui.activity.mainactivity.MainActivity
import com.qmarciset.androidmobileui.app.BaseApp
import com.qmarciset.androidmobileui.binding.bindImageFromDrawable
import com.qmarciset.androidmobileui.databinding.ActivityLoginBinding
import com.qmarciset.androidmobileui.utils.displaySnackBar
import kotlinx.android.synthetic.main.activity_login.*
import timber.log.Timber

class LoginActivity : BaseActivity() {

    private var loggedOut = false
    val appInstance: Application = BaseApp.instance
    lateinit var connectivityManager: ConnectivityManager
    lateinit var loginApiService: LoginApiService

    // ViewModels
    lateinit var loginViewModel: LoginViewModel
    lateinit var connectivityViewModel: ConnectivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve bundled parameter to know if we are coming from a logout action
        loggedOut = intent.getBooleanExtra(LOGGED_OUT, false)

        val authInfoHelper = AuthInfoHelper.getInstance(appInstance)

        // If guest or already logged in, skip LoginActivity
        if (authInfoHelper.sessionToken.isNotEmpty() || authInfoHelper.guestLogin) {
            startMainActivity(true)
        } else {

            // Init system services in onCreate()
            connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // Init Api service
            loginApiService = ApiClient.getLoginApiService(this)

            val binding: ActivityLoginBinding =
                DataBindingUtil.setContentView(this, R.layout.activity_login)
            binding.lifecycleOwner = this

            getViewModel()
            initLayout()
            setupObservers()
        }
    }

    /**
     * Initializes layout components
     */
    private fun initLayout() {

        bindImageFromDrawable(login_logo, BaseApp.loginLogoDrawable)

        if (loggedOut) {
            displaySnackBar(this, resources.getString(R.string.login_logged_out_snackbar))
        }

        // Login button
        login_button_auth.setOnClickListener {
            if (NetworkUtils.isConnected(
                connectivityViewModel.networkStateMonitor.value,
                connectivityManager
            )
            ) {
                login_button_auth.isEnabled = false
                loginViewModel.login(email = login_email_input.text.toString()) { }
            } else {
                displaySnackBar(
                    this,
                    resources.getString(R.string.no_internet)
                )
            }
        }

        // Define a shake animation for when mail is not valid
        val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake)

        login_email_input.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                if (login_email_input.text.toString().isEmailValid()) {
                    loginViewModel.emailValid.postValue(true)
                    login_email_container.error = null
                } else {
                    login_email_input.startAnimation(shakeAnimation)
                    login_email_container.error = resources.getString(R.string.login_invalid_email)
                    loginViewModel.emailValid.postValue(false)
                }
            } else {
                login_email_container.error = null
            }
        }
    }

    override fun getViewModel() {
        getLoginActivityViewModel()
    }

    override fun setupObservers() {

        // Observe authentication state
        loginViewModel.authenticationState.observe(
            this,
            Observer { authenticationState ->
                Timber.i("[AuthenticationState : $authenticationState]")
                when (authenticationState) {
                    AuthenticationState.AUTHENTICATED -> {
                        startMainActivity(false)
                    }
                    AuthenticationState.INVALID_AUTHENTICATION -> {
                        login_button_auth.isEnabled = true
                        displaySnackBar(this, resources.getString(R.string.login_fail_snackbar))
                    }
                    else -> {
                        // Default state in LoginActivity
                        login_button_auth.isEnabled = true
                    }
                }
            }
        )

        // Observe if email is valid
        loginViewModel.emailValid.observe(
            this,
            Observer { emailValid ->
                login_button_auth.isEnabled = emailValid
            }
        )

        // Observe network status
        if (NetworkUtils.sdkNewerThanKitKat) {
            connectivityViewModel.networkStateMonitor.observe(
                this,
                Observer {
                }
            )
        }
    }

    /**
     * Goes to MainActivity, and finishes LoginActivity
     */
    private fun startMainActivity(skipAnimation: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        if (skipAnimation)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
        finish()
    }

    /**
     * Hides keyboard layout when user touches outside input text area
     */
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            if (it.action == MotionEvent.ACTION_DOWN) {
                val v = currentFocus
                v?.let {
                    if (v is EditText) {
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
            }
        }
        return super.dispatchTouchEvent(event)
    }
}
