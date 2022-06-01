/*
 * Created by htemanni on 1/6/2022.
 * 4D SAS
 * Copyright (c) 2022 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui.action.fragment

import com.qmobile.qmobiledatastore.dao.STATUS
import com.qmobile.qmobileui.action.fragment.TasksFragment.Companion.MAX_PENDING_TASKS
import com.qmobile.qmobileui.activity.BaseObserver
import timber.log.Timber

class TasksFragmentObserver(
    private val fragment: TasksFragment
) : BaseObserver {

    override fun initObservers() {
        observeAllTasks()
    }

    private fun observeAllTasks() {

        // remove old history tasks if we have more than 10
        fragment.actionActivity.getTaskViewModel().allTasks.observe(fragment.viewLifecycleOwner) { allTasks ->
            val allHistory = allTasks
                .filter { ((it.actionInfo.tableName == fragment.tableName) && (it.status == STATUS.SUCCESS || it.status == STATUS.ERROR_SERVER)) }
                .sortedByDescending { it.date }
            if (allHistory.size > 10) {
                val historyToDelete = allHistory.subList(MAX_PENDING_TASKS - 1, allHistory.size - 1)
                fragment.actionActivity.getTaskViewModel()
                    .deleteList(historyToDelete.map { actionTask -> actionTask.id })
            }
        }

                fragment.actionActivity.getTaskViewModel().allTasks.observe(fragment.viewLifecycleOwner) { allTasks ->
            Timber.d("Tasks list updated, size : ${allTasks.size}")
            val filteredList = allTasks.filter {
                if (fragment.tableName.isEmpty()) {
                    // When we come from settings (no filter on table name)
                    true
                } else {
                    it.actionInfo.tableName == fragment.tableName
                }
            }.filter {
                if (fragment.currentItemId.isNotEmpty()) {
                    // From detail fragment (show only current entity tasks)
                    it.relatedItemId == fragment.currentItemId
                } else {
                    true // From EntityListFragment
                }
            }
            val pendingTasks = filteredList.filter { it.status == STATUS.PENDING }.sortedByDescending { it.date }
            val history = filteredList.filter { it.status == STATUS.SUCCESS || it.status == STATUS.ERROR_SERVER }
                .takeLast(MAX_PENDING_TASKS).sortedByDescending { it.date }


            fragment.setupAdapter(pendingTasks, history)
        }
    }
}
