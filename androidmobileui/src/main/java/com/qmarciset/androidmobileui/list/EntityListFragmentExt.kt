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

    // Get EntityListViewModel
    val clazz = BaseApp.fromTableForViewModel.entityListViewModelClassFromTable(tableName)
    entityListViewModel = activity?.run {
        ViewModelProvider(
            this,
            EntityListViewModelFactory(
                tableName,
                delegate.apiService
            )
        )[clazz]
    } ?: throw IllegalStateException("Invalid Activity")

    // Get ConnectivityViewModel
    if (NetworkUtils.sdkNewerThanKitKat) {
        connectivityViewModel = activity?.run {
            ViewModelProvider(
                this,
                ConnectivityViewModelFactory(
                    BaseApp.instance,
                    delegate.connectivityManager
                )
            )[ConnectivityViewModel::class.java]
        } ?: throw IllegalStateException("Invalid Activity")
    }

    // Get LoginViewModel
    // We need this ViewModel to know when MainActivity has performed its $authenticate so that
    // we don't trigger the initial sync if we are not authenticated yet
    loginViewModel = activity?.run {
        ViewModelProvider(
            this,
            LoginViewModelFactory(BaseApp.instance, delegate.loginApiService)
        )[LoginViewModel::class.java]
    } ?: throw IllegalStateException("Invalid Activity")
}
