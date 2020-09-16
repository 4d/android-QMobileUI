/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.detail

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.qmarciset.androidmobiledatasync.app.BaseApp
import com.qmarciset.androidmobiledatasync.viewmodel.factory.EntityViewModelFactory
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
                    Observer { list ->
                        for (item in list) {
                            val jsonValue = Gson().toJson(item.first)
                            Timber.d("Many-to-one relation fetched, relationName is $relationName and relation json = $jsonValue")
                        }
                        entityViewModel.setRelationToLayout(relationName, list.first())
                    }
                )
            }
        }
    )
}
