/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.sync.DataSync
import com.qmobile.qmobiledatasync.toast.Event
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobiledatasync.utils.launchAndCollectIn
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.TaskViewModel
import com.qmobile.qmobileui.activity.BaseObserver
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivityObserver(
    private val activity: MainActivity,
    private val entityListViewModelList: List<EntityListViewModel<EntityModel>>,
    private val taskViewModel: TaskViewModel
) : BaseObserver {

    override fun initObservers() {
        activity.initObservers()
        entityListViewModelList.forEach { entityListViewModel ->
            observeDataSynchronized(entityListViewModel)
            observeJSONRelation(entityListViewModel)
            observeEntityListToastMessage(entityListViewModel)
        }
        observePendingTasks()
    }

    // Observe any toast message from Entity Detail
    fun observeEntityToastMessage(message: SharedFlow<Event<ToastMessage.Holder>>) {
        message.launchAndCollectIn(activity, Lifecycle.State.STARTED) { event ->
            activity.handleEvent(event)
        }
    }

    // Observe when data are synchronized
    private fun observeDataSynchronized(entityListViewModel: EntityListViewModel<EntityModel>) {
        var job: Job? = null

        entityListViewModel.dataSynchronized.launchAndCollectIn(activity, Lifecycle.State.STARTED) { dataSyncState ->
            Timber.d(
                "[DataSyncState : $dataSyncState, " +
                    "Table : ${entityListViewModel.getAssociatedTableName()}, " +
                    "Instance : $entityListViewModel]"
            )
            when (dataSyncState) {
                DataSync.State.SYNCHRONIZING, DataSync.State.RESYNC -> {
                    if (entityListViewModel.isToSync.getAndSet(false)) {
                        job?.cancel()
                        job = activity.lifecycleScope.launch {
                            entityListViewModel.getEntities {
                                Timber.v("Requested data for ${entityListViewModel.getAssociatedTableName()}")
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // Observe when there is a new relation to be inserted in a dao
    private fun observeJSONRelation(entityListViewModel: EntityListViewModel<EntityModel>) {
        entityListViewModel.jsonRelation.launchAndCollectIn(activity, Lifecycle.State.STARTED) { jsonRelation ->
            entityListViewModelList.find { it.getAssociatedTableName() == jsonRelation.getDestinationTable() }
                ?.insertRelation(jsonRelation)
        }
    }

    // Observe any toast message from EntityList
    private fun observeEntityListToastMessage(entityListViewModel: EntityListViewModel<EntityModel>) {
        entityListViewModel.toastMessage.message.launchAndCollectIn(activity, Lifecycle.State.STARTED) { event ->
            activity.handleEvent(event)
        }
    }

    private fun observePendingTasks() {
        taskViewModel.pendingTasks.observe(activity) { pendingTasks ->
            Timber.d("Pending tasks list updated, size : ${pendingTasks.size}")
        }
    }
}
