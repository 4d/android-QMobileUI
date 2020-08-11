/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.list

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.qmarciset.androidmobileapi.auth.AuthenticationState
import com.qmarciset.androidmobileapi.connectivity.NetworkState
import com.qmarciset.androidmobileapi.connectivity.NetworkUtils
import com.qmarciset.androidmobiledatasync.app.BaseApp
import com.qmarciset.androidmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.LoginViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.factory.ConnectivityViewModelFactory
import com.qmarciset.androidmobiledatasync.viewmodel.factory.EntityListViewModelFactory
import com.qmarciset.androidmobiledatasync.viewmodel.factory.LoginViewModelFactory
import com.qmarciset.androidmobileui.utils.fetchResourceString
import timber.log.Timber

/**
 * Retrieve viewModels from MainActivity lifecycle
 */
fun EntityListFragment.getViewModel() {
    getEntityListViewModel()
    getConnectivityViewModel()
    getLoginViewModel()
}

/**
 * Setup observers
 */
fun EntityListFragment.setupObservers() {
    observeEntityList()
    observeToastMessage()
    observeDataSynchronized()
    observeAuthenticationState()
    observeNetworkStatus()
}

// Get EntityListViewModel
fun EntityListFragment.getEntityListViewModel() {
    val clazz = BaseApp.fromTableForViewModel.entityListViewModelClassFromTable(tableName)
    entityListViewModel = ViewModelProvider(
        this,
        EntityListViewModelFactory(
            tableName,
            delegate.apiService
        )
    )[clazz]
}

// Get ConnectivityViewModel
fun EntityListFragment.getConnectivityViewModel() {
    activity?.run {
        if (NetworkUtils.sdkNewerThanKitKat) {
            connectivityViewModel = ViewModelProvider(
                this,
                ConnectivityViewModelFactory(
                    BaseApp.instance,
                    delegate.connectivityManager
                )
            )[ConnectivityViewModel::class.java]
        }
    } ?: throw IllegalStateException("Invalid Activity")
}

// Get LoginViewModel
fun EntityListFragment.getLoginViewModel() {
    // We need this ViewModel to know when MainActivity has performed its $authenticate so that
    // we don't trigger the initial sync if we are not authenticated yet
    activity?.run {
        loginViewModel = ViewModelProvider(
            this,
            LoginViewModelFactory(BaseApp.instance, delegate.loginApiService)
        )[LoginViewModel::class.java]
    } ?: throw IllegalStateException("Invalid Activity")
}

// Observe entity list
fun EntityListFragment.observeEntityList() {
    entityListViewModel.entityList.observe(
        viewLifecycleOwner,
        Observer { entities ->
            entities?.let {
                adapter.setEntities(it)
            }
        }
    )
}

// Observe any toast message
fun EntityListFragment.observeToastMessage() {
    entityListViewModel.toastMessage.observe(
        viewLifecycleOwner,
        Observer { message ->
            val toastMessage = context?.fetchResourceString(message) ?: ""
            if (toastMessage.isNotEmpty()) {
                activity?.let {
                    Toast.makeText(it, toastMessage, Toast.LENGTH_LONG).show()
                }
                // To avoid the error toast to be displayed without performing a refresh again
                entityListViewModel.toastMessage.postValue("")
            }
        }
    )
}

// Observe when data are synchronized
@SuppressLint("BinaryOperationInTimber")
fun EntityListFragment.observeDataSynchronized() {
    entityListViewModel.dataSynchronized.observe(
        viewLifecycleOwner,
        Observer { dataSyncState ->
            Timber.i(
                "[DataSyncState : $dataSyncState, " +
                    "Table : ${entityListViewModel.getAssociatedTableName()}, " +
                    "Instance : $entityListViewModel]"
            )
        }
    )
}

// Observe authentication state
fun EntityListFragment.observeAuthenticationState() {
    loginViewModel.authenticationState.observe(
        viewLifecycleOwner,
        Observer { authenticationState ->
            when (authenticationState) {
                AuthenticationState.AUTHENTICATED -> {
                    if (isReady()) {
                        syncData()
                    } else {
                        syncDataRequested.set(true)
                    }
                }
                else -> {
                }
            }
        }
    )
}

// Observe network status
fun EntityListFragment.observeNetworkStatus() {
    if (NetworkUtils.sdkNewerThanKitKat) {
        connectivityViewModel.networkStateMonitor.observe(
            viewLifecycleOwner,
            Observer { networkState ->
                when (networkState) {
                    NetworkState.CONNECTED -> {
                        if (isReady()) {
                            syncData()
                        } else {
                            syncDataRequested.set(true)
                        }
                    }
                    else -> {
                    }
                }
            }
        )
    }
}
