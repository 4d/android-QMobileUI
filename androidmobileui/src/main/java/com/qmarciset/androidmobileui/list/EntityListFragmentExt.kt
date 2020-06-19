/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.list

import androidx.lifecycle.ViewModelProvider
import com.qmarciset.androidmobileapi.connectivity.NetworkUtils
import com.qmarciset.androidmobiledatasync.app.BaseApp
import com.qmarciset.androidmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.LoginViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.factory.ConnectivityViewModelFactory
import com.qmarciset.androidmobiledatasync.viewmodel.factory.EntityListViewModelFactory
import com.qmarciset.androidmobiledatasync.viewmodel.factory.LoginViewModelFactory

fun EntityListFragment.getEntityListFragmentViewModel() {
    activity?.run {
        // Get EntityListViewModel
        val clazz = BaseApp.fromTableForViewModel.entityListViewModelClassFromTable(tableName)
        entityListViewModel = ViewModelProvider(
            this,
            EntityListViewModelFactory(
                tableName,
                delegate.apiService
            )
        )[clazz]

        // Get ConnectivityViewModel
        if (NetworkUtils.sdkNewerThanKitKat) {
            connectivityViewModel = ViewModelProvider(
                this,
                ConnectivityViewModelFactory(
                    BaseApp.instance,
                    delegate.connectivityManager
                )
            )[ConnectivityViewModel::class.java]
        }

        // Get LoginViewModel
        // We need this ViewModel to know when MainActivity has performed its $authenticate so that
        // we don't trigger the initial sync if we are not authenticated yet
        loginViewModel = ViewModelProvider(
            this,
            LoginViewModelFactory(BaseApp.instance, delegate.loginApiService)
        )[LoginViewModel::class.java]
    } ?: throw IllegalStateException("Invalid Activity")
}
