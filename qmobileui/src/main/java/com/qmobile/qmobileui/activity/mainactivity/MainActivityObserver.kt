/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */
@file:Suppress("TooManyFunctions")

package com.qmobile.qmobileui.activity.mainactivity

import android.annotation.SuppressLint
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobileui.activity.BaseObserver
import timber.log.Timber

class MainActivityObserver(
    private val activity: MainActivity,
    private val entityListViewModelList: List<EntityListViewModel<EntityModel>>
) : BaseObserver {

    override fun initObservers() {
        activity.initObservers()
        entityListViewModelList.forEach { entityListViewModel ->
            observeDataSynchronized(entityListViewModel)
            observeNewRelatedEntity(entityListViewModel)
            observeNewRelatedEntities(entityListViewModel)
            observeEntityToastMessage(entityListViewModel)
        }
    }

    // Observe when data are synchronized
    @SuppressLint("BinaryOperationInTimber")
    private fun observeDataSynchronized(entityListViewModel: EntityListViewModel<EntityModel>) {
        entityListViewModel.dataSynchronized.observe(
            activity,
            { dataSyncState ->
                Timber.d(
                    "[DataSyncState : $dataSyncState, " +
                        "Table : ${entityListViewModel.getAssociatedTableName()}, " +
                        "Instance : $entityListViewModel]"
                )
            }
        )
    }

    // Observe when there is a new related entity to be inserted in a dao
    private fun observeNewRelatedEntity(entityListViewModel: EntityListViewModel<EntityModel>) {
        entityListViewModel.newRelatedEntity.observe(
            activity,
            { manyToOneRelation ->
                activity.dispatchNewRelatedEntity(manyToOneRelation)
            }
        )
    }

    // Observe when there is a related entities to be inserted in a dao
    private fun observeNewRelatedEntities(entityListViewModel: EntityListViewModel<EntityModel>) {
        entityListViewModel.newRelatedEntities.observe(
            activity,
            { oneToManyRelation ->
                activity.dispatchNewRelatedEntities(oneToManyRelation)
            }
        )
    }

    // Observe any toast message from EntityList
    private fun observeEntityToastMessage(entityListViewModel: EntityListViewModel<EntityModel>) {
        entityListViewModel.toastMessage.message.observe(
            activity,
            { event ->
                activity.handleEvent(event)
            }
        )
    }
}
