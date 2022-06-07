/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters

import com.qmobile.qmobileapi.utils.parseToString
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.utils.observeOnce
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityViewModel
import com.qmobile.qmobileui.action.utils.UriHelper.stringToUri
import com.qmobile.qmobileui.activity.BaseObserver
import org.json.JSONArray
import timber.log.Timber

class ActionParametersFragmentObserver(
    private val fragment: ActionParametersFragment
) : BaseObserver {

    override fun initObservers() {
        observeTask()
        observeEntity()
    }

    private fun observeTask() {
        // If from pendingTasks
        fragment.taskId?.let { id ->
            // ObserveOnce is used here to prevent the tasks observation done in TasksFragment from triggering events on
            // a fragment not displayed
            fragment.actionActivity.getTaskViewModel().getTask(id).observeOnce(fragment.viewLifecycleOwner) { task ->
                task.actionInfo.validationMap?.let { map -> fragment.validationMap = map }
                task.actionInfo.paramsToSubmit?.let { params -> fragment.paramsToSubmit = params }
                fragment.imagesToUpload = task.actionInfo.imagesToUpload?.stringToUri() ?: hashMapOf()
                fragment.allParameters = JSONArray(task.actionInfo.allParameters)
                fragment.currentTask = task

                task.relatedItemId?.let {
                    if (fragment.entityViewModel == null) {
                        fragment.entityViewModel =
                            getEntityViewModel(fragment, fragment.tableName, it, fragment.delegate.apiService)
                        observeEntity()
                    }
                } ?: kotlin.run {
                    fragment.setupAdapter()
                }
            }
        }
    }

    // Observe entity
    private fun observeEntity() {
        fragment.entityViewModel?.entity?.observe(fragment.viewLifecycleOwner) { entity ->
            Timber.d("Observed entity from Room, json = ${BaseApp.mapper.parseToString(entity)}")
            fragment.selectedEntity = entity
            fragment.setupAdapter()
        }
    }
}
