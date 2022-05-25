/*
 * Created by qmarciset on 4/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui

import android.view.Menu
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.viewmodel.TaskViewModel
import com.qmobile.qmobileui.action.Action
import com.qmobile.qmobileui.action.ActionNavigable
import okhttp3.RequestBody

interface ActionActivity {

    fun setupActionsMenu(
        menu: Menu,
        actions: List<Action>,
        actionNavigable: ActionNavigable,
        isEntityAction: Boolean
    )

    fun onActionClick(action: Action, actionNavigable: ActionNavigable, isEntityAction: Boolean)

    fun sendAction(
        actionContent: MutableMap<String, Any>,
        actionTask: ActionTask,
        tableName: String,
        onActionSent: () -> Unit
    )

    fun uploadImage(
        bodies: Map<String, RequestBody?>,
        tableName: String,
        isFromAction: Boolean = false,
        onImageUploaded: (parameterName: String, receivedId: String) -> Unit,
        onAllUploadFinished: () -> Unit
    )

    fun getSelectedAction(): Action

    fun getSelectedEntity(): RoomEntity?

    fun setCurrentEntityModel(roomEntity: RoomEntity?)

    fun getTaskViewModel(): TaskViewModel

    fun sendPendingTasks()
}
