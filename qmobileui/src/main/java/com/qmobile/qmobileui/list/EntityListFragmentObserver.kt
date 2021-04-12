/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.annotation.SuppressLint
import androidx.lifecycle.Observer
import androidx.sqlite.db.SupportSQLiteQuery
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getLoginViewModel
import kotlinx.android.synthetic.main.fragment_list.*
import timber.log.Timber

/**
 * Retrieve viewModels from MainActivity lifecycle
 */
fun EntityListFragment.getViewModel() {
    entityListViewModel = getEntityListViewModel(activity, tableName, delegate.apiService)
    // We need this ViewModel to know when MainActivity has performed its $authenticate so that
    // we don't trigger the initial sync if we are not authenticated yet
    loginViewModel = getLoginViewModel(activity, delegate.loginApiService)
}

/**
 * Setup observers
 */
fun EntityListFragment.setupObservers() {
    observeDataSynchronized()
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
