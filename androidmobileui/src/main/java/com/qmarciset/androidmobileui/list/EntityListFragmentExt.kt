/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.list

import androidx.lifecycle.ViewModelProvider
import com.qmarciset.androidmobileapi.connectivity.NetworkUtils
import com.qmarciset.androidmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.LoginViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.factory.ConnectivityViewModelFactory
import com.qmarciset.androidmobiledatasync.viewmodel.factory.EntityListViewModelFactory
import com.qmarciset.androidmobiledatasync.viewmodel.factory.LoginViewModelFactory

fun EntityListFragment.getEntityListFragmentViewModel() {

    // Get EntityListViewModel
    val kClazz = delegate.fromTableInterface.entityListViewModelClassFromTable(tableName)
    entityListViewModel = activity?.run {
        ViewModelProvider(
            this,
            EntityListViewModelFactory(
                delegate.appInstance,
                tableName,
                delegate.appDatabaseInterface,
                delegate.apiService,
                delegate.fromTableForViewModel
            )
        )[kClazz.java]
    } ?: throw IllegalStateException("Invalid Activity")

    // Get ConnectivityViewModel
    if (NetworkUtils.sdkNewerThanKitKat) {
        connectivityViewModel = activity?.run {
            ViewModelProvider(
                this,
                ConnectivityViewModelFactory(
                    delegate.appInstance,
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
            LoginViewModelFactory(delegate.appInstance, delegate.loginApiService)
        )[LoginViewModel::class.java]
    } ?: throw IllegalStateException("Invalid Activity")
}
