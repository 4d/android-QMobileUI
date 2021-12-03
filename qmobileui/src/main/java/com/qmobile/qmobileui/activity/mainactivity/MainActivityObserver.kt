/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */
@file:Suppress("TooManyFunctions")

package com.qmobile.qmobileui.activity.mainactivity

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.sync.DataSyncStateEnum
import com.qmobile.qmobiledatasync.toast.Event
import com.qmobile.qmobiledatasync.toast.ToastMessageHolder
import com.qmobile.qmobiledatasync.utils.collectWhenStarted
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobileui.activity.BaseObserver
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
            observeEntityListToastMessage(entityListViewModel)
        }
    }

    // Observe any toast message from Entity Detail
    fun observeEntityToastMessage(message: LiveData<Event<ToastMessageHolder>>) {
        message.observe(
            activity,
            { event ->
                activity.handleEvent(event)
            }
        )
    }

    // Observe when data are synchronized
    @SuppressLint("BinaryOperationInTimber")
    private fun observeDataSynchronized(entityListViewModel: EntityListViewModel<EntityModel>) {

        var job: Job? = null

        activity.collectWhenStarted(entityListViewModel.dataSynchronized) { dataSyncState ->
            Timber.d(
                "[DataSyncState : $dataSyncState, " +
                        "Table : ${entityListViewModel.getAssociatedTableName()}, " +
                        "Instance : $entityListViewModel]"
            )
            when (dataSyncState) {
                DataSyncStateEnum.SYNCHRONIZING -> {
                    if (entityListViewModel.isToSync.getAndSet(false)) {
                        job?.cancel()
                        job = activity.lifecycleScope.launch {
                            activity.mainActivityDataSync.dataSync.isDataSync = true
                            entityListViewModel.getEntities {
                                Timber.v("Requested data for ${entityListViewModel.getAssociatedTableName()}")
                                activity.mainActivityDataSync.dataSync.observe(entityListViewModel)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // Observe when there is a new related entity to be inserted in a dao
    private fun observeNewRelatedEntity(entityListViewModel: EntityListViewModel<EntityModel>) {
        activity.collectWhenStarted(entityListViewModel.newRelatedEntity) { manyToOneRelation ->
            manyToOneRelation?.let { activity.dispatchNewRelatedEntity(it) }
        }
    }

    // Observe when there is a related entities to be inserted in a dao
    private fun observeNewRelatedEntities(entityListViewModel: EntityListViewModel<EntityModel>) {
        activity.collectWhenStarted(entityListViewModel.newRelatedEntities) { oneToManyRelation ->
            oneToManyRelation?.let { activity.dispatchNewRelatedEntities(it) }
        }
    }

    // Observe any toast message from EntityList
    private fun observeEntityListToastMessage(entityListViewModel: EntityListViewModel<EntityModel>) {
        entityListViewModel.toastMessage.message.observe(
            activity,
            { event ->
                activity.handleEvent(event)
            }
        )
    }
}
