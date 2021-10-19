/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail

import androidx.lifecycle.LiveData
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.parseToString
import com.qmobile.qmobiledatastore.data.RoomRelation
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.EntityViewModel
import com.qmobile.qmobileui.activity.BaseObserver
import timber.log.Timber

class EntityDetailFragmentObserver(
    private val fragment: EntityDetailFragment,
    private val entityViewModel: EntityViewModel<EntityModel>
) : BaseObserver {

    override fun initObservers() {
        observeEntity()
        fragment.delegate.observeEntityToastMessage(entityViewModel.toastMessage.message)
    }

    // Observe entity list
    private fun observeEntity() {
        entityViewModel.entity.observe(
            fragment.viewLifecycleOwner,
            { entity ->
                Timber.d("Observed entity from Room, json = ${BaseApp.mapper.parseToString(entity)}")
                entity?.let {
                    val relationKeysMap =
                        BaseApp.genericTableHelper.getManyToOneRelationsInfo(fragment.tableName, entity)
                    observeRelations(relationKeysMap)

                    entity.__KEY?.let { parentItemId ->
                        BaseApp.runtimeDataHolder.oneToManyRelations[fragment.tableName]?.forEach { relationName ->
                            BaseApp.genericNavigationResolver.setupOneToManyRelationButtonOnClickActionForDetail(
                                viewDataBinding = fragment.binding,
                                relationName = relationName,
                                parentItemId = parentItemId,
                                entity = entity
                            )
                        }
                        BaseApp.runtimeDataHolder.manyToOneRelations[fragment.tableName]?.forEach { relationName ->
                            BaseApp.genericNavigationResolver.setupManyToOneRelationButtonOnClickActionForDetail(
                                viewDataBinding = fragment.binding,
                                relationName = relationName,
                                entity = entity
                            )
                        }
                    }
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
