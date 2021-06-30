/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.loginactivity

import android.view.View
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobileui.activity.BaseObserver

class LoginActivityObserver(
    private val activity: LoginActivity,
    private val loginViewModel: LoginViewModel
) : BaseObserver {

    override fun initObservers() {
        activity.initObservers()
        observeEmailValid()
        observeDataLoading()
    }

    // Observe if email is valid
    private fun observeEmailValid() {
        loginViewModel.emailValid.observe(
            activity,
            { emailValid ->
                activity.binding.loginButtonAuth.isEnabled = emailValid
            }
        )
    }

    // Observe if login request in progress
    private fun observeDataLoading() {
        loginViewModel.dataLoading.observe(
            activity,
            { dataLoading ->
                activity.binding.loginProgressbar.visibility =
                    if (dataLoading == true) View.VISIBLE else View.GONE
            }
        )
    }
}
