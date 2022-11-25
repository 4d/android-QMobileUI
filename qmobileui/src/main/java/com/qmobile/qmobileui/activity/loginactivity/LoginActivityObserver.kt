/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.loginactivity

import androidx.lifecycle.Lifecycle
import com.qmobile.qmobiledatasync.utils.LoginHandler
import com.qmobile.qmobiledatasync.utils.launchAndCollectIn
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobileui.activity.BaseObserver

class LoginActivityObserver(
    private val activity: LoginActivity,
    private val loginViewModel: LoginViewModel,
    private val loginHandler: LoginHandler
) : BaseObserver {

    override fun initObservers() {
        activity.initObservers()
        observeDataLoading()
    }

    // Observe if login request in progress
    private fun observeDataLoading() {
        loginViewModel.dataLoading.launchAndCollectIn(activity, Lifecycle.State.STARTED) { dataLoading ->
            loginHandler.onLoginInProgress(dataLoading)
        }
    }
}
