/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */
@file:Suppress("TooManyFunctions")

package com.qmobile.qmobileui.activity.mainactivity

import android.annotation.SuppressLint
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.connectivity.NetworkStateEnum
import com.qmobile.qmobileapi.connectivity.sdkNewerThanKitKat
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.ConnectivityViewModelFactory
import com.qmobile.qmobiledatasync.viewmodel.factory.EntityListViewModelFactory
import com.qmobile.qmobiledatasync.viewmodel.factory.LoginViewModelFactory
import timber.log.Timber

fun MainActivity.getViewModel() {
    getLoginViewModel()
    getConnectivityViewModel()
    getEntityListViewModelList()
}

fun MainActivity.setupObservers() {
    observeAuthenticationState()
    observeNetworkStatus()
    observeEntityListViewModelList()
}

// Get LoginViewModel
fun MainActivity.getLoginViewModel() {
    loginViewModel = ViewModelProvider(
        this,
        LoginViewModelFactory(BaseApp.instance, loginApiService)
    )[LoginViewModel::class.java]
}

// Get ConnectivityViewModel
fun MainActivity.getConnectivityViewModel() {
    if (sdkNewerThanKitKat) {
        connectivityViewModel = ViewModelProvider(
            this,
            ConnectivityViewModelFactory(BaseApp.instance, connectivityManager)
        )[ConnectivityViewModel::class.java]
    }
}

// Get EntityListViewModel list
fun MainActivity.getEntityListViewModelList() {
    entityListViewModelList = mutableListOf()
    for (tableName in BaseApp.fromTableForViewModel.tableNames()) {
        val clazz = BaseApp.fromTableForViewModel.entityListViewModelClassFromTable(tableName)

        entityListViewModelList.add(
            ViewModelProvider(
                this,
                EntityListViewModelFactory(
                    tableName,
                    apiService
                )
            )[clazz]
        )
    }
}

// Observe authentication state
fun MainActivity.observeAuthenticationState() {
    loginViewModel.authenticationState.observe(
        this,
        Observer { authenticationState ->
            Timber.i("[AuthenticationState : $authenticationState]")
            when (authenticationState) {
                AuthenticationStateEnum.AUTHENTICATED -> {
                    if (shouldDelayOnForegroundEvent.compareAndSet(true, false)) {
                        applyOnForegroundEvent()
                    }
                }
                AuthenticationStateEnum.LOGOUT -> {
                    // Logout performed
                    if (!authInfoHelper.guestLogin)
                        startLoginActivity()
                }
                else -> {
                }
            }
        }
    )
}

// Observe network status
fun MainActivity.observeNetworkStatus() {
    if (sdkNewerThanKitKat) {
        connectivityViewModel.networkStateMonitor.observe(
            this,
            Observer { networkState ->
                Timber.i("[NetworkState : $networkState]")
                when (networkState) {
                    NetworkStateEnum.CONNECTED -> {
                        // Setting the authenticationState to its initial value
                        if (authInfoHelper.sessionToken.isNotEmpty())
                            loginViewModel.authenticationState.postValue(AuthenticationStateEnum.AUTHENTICATED)

                        // If guest and not yet logged in, auto login
                        if (authInfoHelper.sessionToken.isEmpty() &&
                            authInfoHelper.guestLogin &&
                            authenticationRequested
                        ) {
                            authenticationRequested = false
                            tryAutoLogin()
                        }
                    }
                    else -> {
                    }
                }
            }
        )
    }
}

fun MainActivity.observeEntityListViewModelList() {
    for (entityListViewModel in entityListViewModelList) {
        observeDataSynchronized(entityListViewModel)
        observeNewRelatedEntity(entityListViewModel)
        observeNewRelatedEntities(entityListViewModel)
    }
}

// Observe when data are synchronized
@SuppressLint("BinaryOperationInTimber")
fun MainActivity.observeDataSynchronized(entityListViewModel: EntityListViewModel<EntityModel>) {
    entityListViewModel.dataSynchronized.observe(
        this,
        Observer { dataSyncState ->
            Timber.i(
                "[DataSyncState : $dataSyncState, " +
                    "Table : ${entityListViewModel.getAssociatedTableName()}, " +
                    "Instance : $entityListViewModel]"
            )
        }
    )
}

// Observe when there is a new related entity to be inserted in a dao
fun MainActivity.observeNewRelatedEntity(entityListViewModel: EntityListViewModel<EntityModel>) {
    entityListViewModel.newRelatedEntity.observe(
        this,
        Observer { manyToOneRelation ->
            dispatchNewRelatedEntity(manyToOneRelation)
        }
    )
}

// Observe when there is a related entities to be inserted in a dao
fun MainActivity.observeNewRelatedEntities(entityListViewModel: EntityListViewModel<EntityModel>) {
    entityListViewModel.newRelatedEntities.observe(
        this,
        Observer { oneToManyRelation ->
            dispatchNewRelatedEntities(oneToManyRelation)
        }
    )
}
