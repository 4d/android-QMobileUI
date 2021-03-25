/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.annotation.SuppressLint
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.sqlite.db.SupportSQLiteQuery
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.connectivity.NetworkStateEnum
import com.qmobile.qmobileapi.connectivity.sdkNewerThanKitKat
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.ConnectivityViewModelFactory
import com.qmobile.qmobiledatasync.viewmodel.factory.EntityListViewModelFactory
import com.qmobile.qmobiledatasync.viewmodel.factory.LoginViewModelFactory
import kotlinx.android.synthetic.main.fragment_list.*
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
    observeDataSynchronized()
    observeAuthenticationState()
    observeNetworkStatus()
    observeDataLoading()
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
        if (sdkNewerThanKitKat) {
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

// Sql Dynamic Query Support
fun EntityListFragment.observeEntityListDynamicSearch(sqLiteQuery: SupportSQLiteQuery) {
    entityListViewModel.getAllDynamicQuery(sqLiteQuery).observe(
        viewLifecycleOwner,
        Observer {
            it.let {

                // Map<entityKey, Map<relationName, LiveData<RoomRelation>>>
                val relationMap = entityListViewModel.getManyToOneRelationKeysFromEntityList(it)

                adapter.setEntities(it, relationMap)
                if (!it.isNullOrEmpty()) {
                    fragment_list_no_data_tv.visibility = View.GONE
                }
            }
        }
    )
}

// Observe dataLoading
fun EntityListFragment.observeDataLoading() {
    entityListViewModel.dataLoading.observe(
        viewLifecycleOwner,
        Observer { dataLoading ->
            if (dataLoading != true && adapter.itemCount == 0) {
                fragment_list_no_data_tv.visibility = View.VISIBLE
            } else {
                fragment_list_no_data_tv.visibility = View.GONE
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
                AuthenticationStateEnum.AUTHENTICATED -> {
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
    if (sdkNewerThanKitKat) {
        connectivityViewModel.networkStateMonitor.observe(
            viewLifecycleOwner,
            Observer { networkState ->
                when (networkState) {
                    NetworkStateEnum.CONNECTED -> {
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
