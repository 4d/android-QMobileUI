/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail

import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomRelation
import com.qmobile.qmobiledatasync.viewmodel.EntityViewModel
import com.qmobile.qmobileui.activity.BaseObserver
import timber.log.Timber

class EntityDetailFragmentObserver(
    private val fragment: EntityDetailFragment,
    private val entityViewModel: EntityViewModel<EntityModel>
) : BaseObserver {

    override fun initObservers() {
        observeEntity()
    }

    // Observe entity list
    private fun observeEntity() {
        entityViewModel.entity.observe(
            fragment.viewLifecycleOwner,
            { entity ->
                Timber.d("Observed entity from Room, json = ${Gson().toJson(entity)}")
                entity?.let {
                    val relationKeysMap = entityViewModel.getRelationsInfo(entity)
                    observeRelations(relationKeysMap)
                }
            }
        )
    }

    private fun observeRelations(relationKeysMap: Map<String, LiveData<RoomRelation>>) {
        for ((relationName, liveDataListRoomRelation) in relationKeysMap) {
            liveDataListRoomRelation.observe(
                fragment.viewLifecycleOwner,
                { roomRelation ->
                    roomRelation?.let {
                        entityViewModel.setRelationToLayout(relationName, roomRelation)
                    }
                }
            )
        }
    }
}
