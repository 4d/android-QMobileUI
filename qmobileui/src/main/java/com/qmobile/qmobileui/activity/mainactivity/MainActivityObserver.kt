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
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.EntityListViewModelFactory
import com.qmobile.qmobileui.activity.getViewModels
import com.qmobile.qmobileui.activity.observe
import timber.log.Timber

fun MainActivity.setupViewModels() {
    this.getViewModels()
    getEntityListViewModelList()
}

fun MainActivity.setupObservers() {
    this.observe()
    observeEntityListViewModelList()
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
