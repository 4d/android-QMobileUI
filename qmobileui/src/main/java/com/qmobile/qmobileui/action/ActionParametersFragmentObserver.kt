/*
 * Created by qmarciset on 24/5/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action

import android.net.Uri
import com.qmobile.qmobileui.activity.BaseObserver
import org.json.JSONArray

class ActionParametersFragmentObserver(
    private val fragment: ActionParametersFragment
) : BaseObserver {

    override fun initObservers() {
        observeTask()
    }

    private fun observeTask() {
        fragment.taskId?.let { id ->
            fragment.actionActivity.getTaskViewModel().getTask(id).observeOnce(fragment.viewLifecycleOwner) { task ->
                task.actionInfo.validationMap?.let { map -> fragment.validationMap = map }
                task.actionInfo.paramsToSubmit?.let { params -> fragment.paramsToSubmit = params }
                fragment.imagesToUpload =
                    task.actionInfo.imagesToUpload?.mapValues { entry ->
                    Uri.parse(entry.value)
                } as HashMap<String, Uri>
                fragment.allParameters = JSONArray(task.actionInfo.allParameters)
                fragment.currentTask = task
                fragment.setupRecyclerView()
            }
        }
    }
}
