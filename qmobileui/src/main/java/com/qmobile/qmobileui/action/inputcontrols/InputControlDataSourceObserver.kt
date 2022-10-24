/*
 * Created by qmarciset on 21/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.inputcontrols

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.utils.launchAndCollectIn
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobileui.activity.BaseObserver

class InputControlDataSourceObserver(
    private val fragment: Fragment,
    private val entityListViewModel: EntityListViewModel<EntityModel>,
    private val dataRetrievedCallback: (entities: List<RoomEntity>) -> Unit
) : BaseObserver {

    override fun initObservers() {
        observeEntityList()
    }

    private fun observeEntityList() {
        entityListViewModel.entityListSharedFlow.launchAndCollectIn(
            fragment.viewLifecycleOwner,
            Lifecycle.State.STARTED
        ) {
            dataRetrievedCallback(it)
        }
    }
}
