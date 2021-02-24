/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.factory.EntityViewModelFactory
import timber.log.Timber

fun EntityDetailFragment.getViewModel() {
    getEntityViewModel()
    observeEntity()
}

// Get EntityViewModel
fun EntityDetailFragment.getEntityViewModel() {
    val clazz = BaseApp.fromTableForViewModel.entityViewModelClassFromTable(tableName)
    entityViewModel = ViewModelProvider(
        this,
        EntityViewModelFactory(
            tableName,
            itemId,
            delegate.apiService
        )
    )[clazz]
}

// Observe entity list
fun EntityDetailFragment.observeEntity() {
    entityViewModel.entity.observe(
        viewLifecycleOwner,
        Observer { entity ->
            Timber.d("Observed entity from Room, json = ${Gson().toJson(entity)}")

            val relationKeysMap = entityViewModel.getManyToOneRelationKeysFromEntity(entity)
            for ((relationName, liveDateListRoomRelation) in relationKeysMap) {
                liveDateListRoomRelation.observe(
                    viewLifecycleOwner,
                    Observer { roomRelation ->
                        roomRelation?.let {
                            entityViewModel.setRelationToLayout(relationName, roomRelation)
                        }
                    }
                )
            }
        }
    )
}
