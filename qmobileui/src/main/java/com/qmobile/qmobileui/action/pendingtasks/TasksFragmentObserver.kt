/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.pendingtasks

import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.activity.BaseObserver
import timber.log.Timber

class TasksFragmentObserver(
    private val fragment: TasksFragment
) : BaseObserver {

    override fun initObservers() {
        observeAllTasks()
    }

    private fun observeAllTasks() {
        fragment.actionActivity.getTaskVM().allTasks.observe(fragment.viewLifecycleOwner) { allTasks ->
            Timber.d("Tasks list updated, size : ${allTasks.size}")

            if (fragment.tableName.isNotEmpty()) {
                cleanObsoleteHistory(allTasks)
            }

            val filteredList = allTasks
                .filter { it.filterTableTasks(fragment.tableName) }
                .filter { it.filterEntityTasks(fragment.currentItemId) }

            val pendingTasks = filteredList
                .filter { it.isPending() }
                .sortedByDescending { it.date }

            val history = filteredList
                .filter { it.isHistory() }
                .takeLast(BaseApp.runtimeDataHolder.maxPendingActionTask)
                .sortedByDescending { it.date }

            fragment.pendingAdapter.updateItems(pendingTasks)
            fragment.completedAdapter.updateItems(history)
        }
    }

    // Remove old history tasks if we have more than 10
    private fun cleanObsoleteHistory(allTasks: List<ActionTask>) {
        val allHistory = allTasks
            .filter { it.actionInfo.tableName == fragment.tableName }
            .filter { it.isHistory() }
            .sortedByDescending { it.date }

        if (allHistory.size > BaseApp.runtimeDataHolder.maxPendingActionTask) {
            val idToDelete = allHistory.subList(BaseApp.runtimeDataHolder.maxPendingActionTask - 1, allHistory.size - 1).map { it.id }
            fragment.actionActivity.getTaskVM().deleteList(idToDelete)
        }
    }
}
