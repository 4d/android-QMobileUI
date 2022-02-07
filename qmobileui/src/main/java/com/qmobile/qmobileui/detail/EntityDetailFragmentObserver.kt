/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail

import androidx.lifecycle.LiveData
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.parseToString
import com.qmobile.qmobiledatastore.data.RoomData
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
            fragment.viewLifecycleOwner
        ) { entity ->
            Timber.d("Observed entity from Room, json = ${BaseApp.mapper.parseToString(entity)}")
            entity?.let {

                setupObserver(entity)

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
    }

    private fun setupObserver(entity: EntityModel) {
        BaseApp.genericRelationHelper.getManyToOneRelationsInfo(fragment.tableName, entity)
            .let { relationMap ->
                if (relationMap.isNotEmpty()) {
                    observeRelations(relationMap, entity)
                }
            }
        BaseApp.genericRelationHelper.getOneToManyRelationsInfo(fragment.tableName, entity)
            .let { relationMap ->
                if (relationMap.isNotEmpty()) {
                    observeRelations(relationMap)
                }
            }
    }

    private fun observeRelations(relations: Map<String, LiveData<RoomRelation>>, entity: EntityModel? = null) {
        for ((relationName, liveDataRelatedEntity) in relations) {
            liveDataRelatedEntity.observe(
                requireNotNull(fragment.viewLifecycleOwner)
            ) { roomRelation ->
                roomRelation?.let {
                    entityViewModel.setRelationToLayout(relationName, roomRelation)
                    entity?.let {
                        roomRelation.toOne?.let {
                            refreshOneToManyNavForNavbarTitle(entity, it)
                        }
                    }
                }
            }
        }
    }

    private fun refreshOneToManyNavForNavbarTitle(entity: EntityModel, anyRelatedEntity: RoomData) {
        entity.__KEY?.let { parentItemId ->
            BaseApp.runtimeDataHolder.oneToManyRelations[fragment.tableName]?.forEach { relationName ->
                if (relationName.contains(".")) {
                    BaseApp.genericNavigationResolver.setupOneToManyRelationButtonOnClickActionForDetail(
                        viewDataBinding = fragment.binding,
                        relationName = relationName,
                        parentItemId = parentItemId,
                        entity = entity,
                        anyRelatedEntity = anyRelatedEntity
                    )
                }
            }
        }
    }
}
