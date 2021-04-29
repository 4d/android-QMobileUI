/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.annotation.SuppressLint
import androidx.lifecycle.Observer
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getLoginViewModel
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
    observeCurrentQuery()
}

// Sql Dynamic Query Support
fun EntityListFragment.observeEntityListDynamicSearch() {
    entityListViewModel.entityListLiveData.observe(
        viewLifecycleOwner,
        Observer {
            adapter.submitList(it)
            adapter.notifyDataSetChanged()
        }
    )
//    entityListViewModel.getAllDynamicQuery(sqLiteQuery).observe(
//        viewLifecycleOwner,
//        Observer { pagedList ->
//            adapter.submitList(pagedList)
//        }
//    )
}

fun EntityListFragment.observeCurrentQuery() {
    entityListViewModel.currentQuery.observe(
        viewLifecycleOwner,
        Observer { currentQuery ->
            if (currentQuery.isNullOrEmpty())
                entityListViewModel.setSearchQuery(sqlQueryBuilderUtil.getAll())
            else
                entityListViewModel.setSearchQuery(sqlQueryBuilderUtil.sortQuery(currentQuery))
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
