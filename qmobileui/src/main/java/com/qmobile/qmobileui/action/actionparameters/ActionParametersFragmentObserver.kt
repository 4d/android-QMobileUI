/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters

import com.qmobile.qmobileapi.utils.parseToString
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityViewModel
import com.qmobile.qmobileui.action.inputcontrols.InputControl.Format.getInputControlFormatHolders
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
            fragment.actionActivity.getTaskVM().getTask(id).observe(fragment.viewLifecycleOwner) { task ->
                task?.let { // task can be null after deletion
                    task.actionInfo.validationMap?.let { map -> fragment.validationMap = map }
                    task.actionInfo.paramsToSubmit?.let { params -> fragment.paramsToSubmit = params }
                    task.actionInfo.errors?.let { params -> fragment.errorsByParameter.putAll(params) }
                    fragment.inputControlFormatHolders.putAll(task.getInputControlFormatHolders())
                    fragment.imagesToUpload = task.actionInfo.imagesToUpload?.stringToUri() ?: hashMapOf()
                    task.actionInfo.allParameters?.let { allParameters ->
                        fragment.allParameters = JSONArray(allParameters)
                    }
                    fragment.currentTask = task
                    fragment.activity?.invalidateOptionsMenu()

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
    }

    // Observe entity
    private fun observeEntity() {
        fragment.entityViewModel?.entity?.observe(fragment.viewLifecycleOwner) { entity ->
            // entity can be null, therefore this code is always executed
            Timber.d("Observed entity from Room, json = ${BaseApp.mapper.parseToString(entity)}")
            fragment.selectedEntity = entity
            fragment.setupAdapter()
        }
    }
}
