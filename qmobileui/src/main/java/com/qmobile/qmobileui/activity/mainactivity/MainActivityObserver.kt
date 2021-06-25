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
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.EntityListViewModelFactory
import com.qmobile.qmobiledatasync.viewmodel.factory.getConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getLoginViewModel
import com.qmobile.qmobileui.utils.ToastHelper
import timber.log.Timber

fun MainActivity.getViewModel() {
    loginViewModel = getLoginViewModel(this, loginApiService)
    connectivityViewModel =
        getConnectivityViewModel(this, connectivityManager, accessibilityApiService)
    getEntityListViewModelList()
}

fun MainActivity.setupObservers() {
    observeAuthenticationState()
    observeLoginToastMessage()
    observeNetworkStatus()
    observeEntityListViewModelList()
    observeConnectivityToastMessage()
}

// Get EntityListViewModel list
fun MainActivity.getEntityListViewModelList() {
    entityListViewModelList = mutableListOf()
    for (tableName in BaseApp.genericTableHelper.tableNames()) {
        val clazz = BaseApp.genericTableHelper.entityListViewModelClassFromTable(tableName)

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
            Timber.d("[AuthenticationState : $authenticationState]")
            when (authenticationState) {
                AuthenticationStateEnum.AUTHENTICATED -> {
                    if (loginStatusText.isNotEmpty()) {
                        ToastHelper.show(this, loginStatusText, MessageType.SUCCESS)
                        loginStatusText = ""
                    }
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

// Observe any toast message
fun MainActivity.observeLoginToastMessage() {
    loginViewModel.toastMessage.message.observe(
        this,
        Observer { event ->
            handleEvent(event)
        }
    )
}

fun MainActivity.observeConnectivityToastMessage() {
    connectivityViewModel.toastMessage.message.observe(
        this,
        Observer { event ->
            handleEvent(event)
        }
    )
}

// Observe network status
fun MainActivity.observeNetworkStatus() {
    connectivityViewModel.networkStateMonitor.observe(
        this,
        Observer { networkState ->
            Timber.d("[NetworkState : $networkState]")
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

fun MainActivity.observeEntityListViewModelList() {
    for (entityListViewModel in entityListViewModelList) {
        observeDataSynchronized(entityListViewModel)
        observeNewRelatedEntity(entityListViewModel)
        observeNewRelatedEntities(entityListViewModel)
        observeEntityToastMessage(entityListViewModel)
    }
}

// Observe when data are synchronized
@SuppressLint("BinaryOperationInTimber")
fun MainActivity.observeDataSynchronized(entityListViewModel: EntityListViewModel<EntityModel>) {
    entityListViewModel.dataSynchronized.observe(
        this,
        Observer { dataSyncState ->
            Timber.d(
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

// Observe any toast message from EntityList
fun MainActivity.observeEntityToastMessage(entityListViewModel: EntityListViewModel<EntityModel>) {
    entityListViewModel.toastMessage.message.observe(
        this,
        Observer { event ->
            handleEvent(event)
        }
    )
}
