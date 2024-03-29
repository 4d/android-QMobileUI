/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import androidx.lifecycle.Lifecycle
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.sync.DataSync
import com.qmobile.qmobiledatasync.toast.Event
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobiledatasync.utils.launchAndCollectIn
import com.qmobile.qmobiledatasync.viewmodel.ActionViewModel
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.PushViewModel
import com.qmobile.qmobiledatasync.viewmodel.TaskViewModel
import com.qmobile.qmobileui.activity.BaseObserver
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber

class MainActivityObserver(
    private val activity: MainActivity,
    private val entityListViewModelList: List<EntityListViewModel<EntityModel>>,
    private val actionViewModel: ActionViewModel,
    private val pushViewModel: PushViewModel,
    private val taskViewModel: TaskViewModel
) : BaseObserver {

    override fun initObservers() {
        activity.initObservers()
        entityListViewModelList.forEach { entityListViewModel ->
            observeDataSynchronized(entityListViewModel)
            observeJSONRelation(entityListViewModel)
            observeEntityListToastMessage(entityListViewModel)
            observeEntityListIsUnauthorized(entityListViewModel)
        }
        observeActionToastMessage()
        observeActionIsUnauthorized()
        observePushToastMessage()
        observePushIsUnauthorized()
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
        entityListViewModel.dataSynchronized.launchAndCollectIn(activity, Lifecycle.State.STARTED) { dataSyncState ->
            Timber.d(
                "[DataSyncState : $dataSyncState, " +
                    "Table : ${entityListViewModel.getAssociatedTableName()}, " +
                    "Instance : $entityListViewModel]"
            )
            when (dataSyncState) {
                DataSync.State.SYNCHRONIZING, DataSync.State.RESYNC -> {
                    if (entityListViewModel.isToSync.getAndSet(false)) {
                        entityListViewModel.getEntities(true) { _, _ ->
                            Timber.v("Requested data for ${entityListViewModel.getAssociatedTableName()}")
                        }
                    }
                }
                else -> {
                    if (activity.pushDataSyncRequested.getAndSet(false)) {
                        activity.cancelPushDataSync()
                    }
                }
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

    private fun observeEntityListIsUnauthorized(entityListViewModel: EntityListViewModel<EntityModel>) {
        entityListViewModel.isUnauthorized.launchAndCollectIn(activity, Lifecycle.State.STARTED) { isUnauthorized ->
            if (isUnauthorized) {
                activity.logout(true)
            }
        }
    }

    private fun observeActionToastMessage() {
        actionViewModel.toastMessage.message.launchAndCollectIn(activity, Lifecycle.State.STARTED) { event ->
            activity.handleEvent(event)
        }
    }

    private fun observeActionIsUnauthorized() {
        actionViewModel.isUnauthorized.launchAndCollectIn(activity, Lifecycle.State.STARTED) { isUnauthorized ->
            if (isUnauthorized) {
                activity.logout(true)
            }
        }
    }

    private fun observePushToastMessage() {
        pushViewModel.toastMessage.message.launchAndCollectIn(activity, Lifecycle.State.STARTED) { event ->
            activity.handleEvent(event)
        }
    }

    private fun observePushIsUnauthorized() {
        pushViewModel.isUnauthorized.launchAndCollectIn(activity, Lifecycle.State.STARTED) { isUnauthorized ->
            if (isUnauthorized) {
                activity.logout(true)
            }
        }
    }

    private fun observePendingTasks() {
        taskViewModel.pendingTasks.observe(activity) { pendingTasks ->
            Timber.d("Pending tasks list updated, size : ${pendingTasks.size}")
        }
    }
}
