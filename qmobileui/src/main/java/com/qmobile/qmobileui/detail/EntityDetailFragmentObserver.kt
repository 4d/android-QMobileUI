/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail

import androidx.lifecycle.Observer
import com.google.gson.Gson
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityViewModel
import timber.log.Timber

/**
 * Retrieve viewModels from MainActivity lifecycle
 */
fun EntityDetailFragment.getViewModel() {
    entityViewModel = getEntityViewModel(this, tableName, itemId, delegate.apiService)
}

/**
 * Setup observers
 */
fun EntityDetailFragment.setupObservers() {
    observeEntity()
}

// Observe entity list
fun EntityDetailFragment.observeEntity() {
    entityViewModel.entity.observe(
        viewLifecycleOwner,
        Observer { entity ->
            Timber.d("Observed entity from Room, json = ${Gson().toJson(entity)}")

            entity?.let {
                val relationKeysMap = entityViewModel.getManyToOneRelationKeysFromEntity(entity)
                for ((relationName, liveDataListRoomRelation) in relationKeysMap) {
                    liveDataListRoomRelation.observe(
                        viewLifecycleOwner,
                        Observer { roomRelation ->
                            roomRelation?.let {
                                entityViewModel.setRelationToLayout(relationName, roomRelation)
                            }
                        }
                    )
                }
            }
        }
    )
}
