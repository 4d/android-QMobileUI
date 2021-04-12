/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.annotation.SuppressLint
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.sqlite.db.SupportSQLiteQuery
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.EntityListViewModelFactory
import com.qmobile.qmobiledatasync.viewmodel.factory.LoginViewModelFactory
import kotlinx.android.synthetic.main.fragment_list.*
import timber.log.Timber

/**
 * Retrieve viewModels from MainActivity lifecycle
 */
fun EntityListFragment.getViewModel() {
    getEntityListViewModel()
    getLoginViewModel()
}

/**
 * Setup observers
 */
fun EntityListFragment.setupObservers() {
    observeDataSynchronized()
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
            Timber.d(
                "[DataSyncState : $dataSyncState, " +
                    "Table : ${entityListViewModel.getAssociatedTableName()}, " +
                    "Instance : $entityListViewModel]"
            )
        }
    )
}
