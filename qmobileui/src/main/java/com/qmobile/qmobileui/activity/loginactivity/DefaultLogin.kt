/*
 * Created by qmarciset on 23/11/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.loginactivity

import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import androidx.databinding.DataBindingUtil
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable
import com.qmobile.qmobileapi.model.error.AuthorizedStatus
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.utils.LoginForm
import com.qmobile.qmobiledatasync.utils.LoginHandler
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.binding.bindImageFromDrawable
import com.qmobile.qmobileui.databinding.ActivityLoginBinding
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import com.qmobile.qmobileui.ui.setOnVeryLongClickListener
import com.qmobile.qmobileui.utils.hideKeyboard

@LoginForm
class DefaultLogin(private val activity: LoginActivity) : LoginHandler {

    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!

    override val ensureValidMail = true
    private lateinit var shakeAnimation: Animation
    private lateinit var progressIndicatorDrawable: IndeterminateDrawable<CircularProgressIndicatorSpec>

    init {
        _binding = DataBindingUtil.setContentView<ActivityLoginBinding?>(activity, R.layout.activity_login).apply {
            lifecycleOwner = activity
        }
    }

    override fun initLayout() {
        val spec = CircularProgressIndicatorSpec(
            activity,
            null,
            0,
            R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall
        )
        progressIndicatorDrawable = IndeterminateDrawable.createCircularDrawable(activity, spec)

        bindImageFromDrawable(binding.loginLogo, BaseApp.loginLogoDrawable)

        // Login button
        binding.loginButtonAuth.setOnSingleClickListener {
            login()
        }

        // Define a shake animation for when input is not valid
        shakeAnimation = AnimationUtils.loadAnimation(activity, R.anim.shake)

        binding.loginEmailInput.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.loginEmailContainer.error = null
            } else {
                validateText(true)
            }
        }

        binding.loginEmailInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Nothing to do
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nothing to do
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { validateText(false) }
            }
        })

        binding.loginEmailInput.setOnEditorActionListener { textView, actionId, keyEvent ->
            if ((keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_UP) ||
                actionId == EditorInfo.IME_ACTION_DONE
            ) {
                hideKeyboard(activity)
                textView.clearFocus()
                if (validateText(true)) {
                    login()
                }
            }
            true
        }

        binding.loginLogo.setOnVeryLongClickListener {
            activity.showRemoteUrlDialog()
        }

        BaseApp.sharedPreferencesHolder.lastLoginMail.let {
            if (it.isNotEmpty()) {
                binding.loginEmailInput.setText(BaseApp.sharedPreferencesHolder.lastLoginMail)
                binding.loginButtonAuth.isEnabled = true
            }
        }
    }

    override fun validate(input: String): Boolean {
        return true
    }

    override fun onInputInvalid() {
        binding.loginEmailInput.startAnimation(shakeAnimation)
        binding.loginEmailContainer.error = activity.resources.getString(R.string.login_invalid_email)
    }

    override fun onLoginInProgress(inProgress: Boolean) {
        binding.loginButtonAuth.icon = if (inProgress) progressIndicatorDrawable else null
    }

    override fun onLoginSuccessful() {
        // Nothing to do
    }

    override fun onLoginUnsuccessful() {
        binding.loginButtonAuth.isEnabled = true
    }

    override fun onLogout() {
        // Nothing to do
    }

    private fun login() {
        binding.loginButtonAuth.isEnabled = false
        activity.login(binding.loginEmailInput.text.toString())
    }

    private fun validateText(displayError: Boolean): Boolean {
        return if (activity.validateMail(binding.loginEmailInput.text.toString())) {
            binding.loginButtonAuth.isEnabled = true
            binding.loginEmailContainer.error = null
            true
        } else {
            if (displayError) {
                onInputInvalid()
            }
            binding.loginButtonAuth.isEnabled = false
            false
        }
    }
}
