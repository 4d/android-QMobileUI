/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.loginactivity

import android.view.View
import androidx.lifecycle.Observer
import com.qmobile.qmobileui.activity.getViewModels
import com.qmobile.qmobileui.activity.observe

fun LoginActivity.setupViewModels() {
    this.getViewModels()
}

fun LoginActivity.setupObservers() {
    this.observe()
    observeEmailValid()
    observeDataLoading()
}

// Observe if email is valid
fun LoginActivity.observeEmailValid() {
    loginViewModel.emailValid.observe(
        this,
        Observer { emailValid ->
            binding.loginButtonAuth.isEnabled = emailValid
        }
    )
}

// Observe if login request in progress
fun LoginActivity.observeDataLoading() {
    loginViewModel.dataLoading.observe(
        this,
        Observer { dataLoading ->
            binding.loginProgressbar.visibility = if (dataLoading == true) View.VISIBLE else View.GONE
        }
    )
}
