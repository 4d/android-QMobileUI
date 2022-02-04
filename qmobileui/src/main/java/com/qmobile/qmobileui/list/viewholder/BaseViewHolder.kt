/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list.viewholder

import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomData
import com.qmobile.qmobiledatastore.data.RoomRelation
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.BR
import com.qmobile.qmobileui.ui.setOnSingleClickListener

class BaseViewHolder(
    private val dataBinding: ViewDataBinding,
    private val tableName: String,
    private val onItemClick: (ViewDataBinding, String) -> Unit,
    private val onItemLongClick: (EntityModel) -> Unit
) :
    RecyclerView.ViewHolder(dataBinding.root) {

    // Applies DataBinding
    fun bind(entity: EntityModel?) {
        entity?.let {
            dataBinding.setVariable(BR.entityData, entity)
            dataBinding.executePendingBindings()

            setupClickListeners(entity)
            setupRelationNavigation(entity)

            // unbind because of issue : item at position 11 receives binding of item 0,
            // item at position 12 receives binding of item at position 1, etc.
            unbindRelations()
            setupObserver(entity)
        }
    }

    private fun setupClickListeners(entity: EntityModel) {
        entity.__KEY?.let { key ->
            itemView.setOnSingleClickListener {
                onItemClick(dataBinding, key)
            }
            itemView.setOnLongClickListener {
                onItemLongClick(entity)
                true
            }
        }
    }

    private fun setupRelationNavigation(entity: EntityModel) {
        entity.__KEY?.let { parentItemId ->
            BaseApp.runtimeDataHolder.oneToManyRelations[tableName]?.forEach { relationName ->
                BaseApp.genericNavigationResolver.setupOneToManyRelationButtonOnClickActionForCell(
                    viewDataBinding = dataBinding,
                    relationName = relationName,
                    parentItemId = parentItemId,
                    entity = entity
                )
            }
            BaseApp.runtimeDataHolder.manyToOneRelations[tableName]?.forEach { relationName ->
                BaseApp.genericNavigationResolver.setupManyToOneRelationButtonOnClickActionForCell(
                    viewDataBinding = dataBinding,
                    relationName = relationName,
                    entity = entity
                )
            }
        }
    }

    private fun setupObserver(entity: EntityModel) {
        BaseApp.genericRelationHelper.getManyToOneRelationsInfo(tableName, entity).let { relationMap ->
            if (relationMap.isNotEmpty()) {
                observeManyToOneRelations(relationMap, entity)
            }
        }

        BaseApp.genericRelationHelper.getOneToManyRelationsInfo(tableName, entity).let { relationMap ->
            if (relationMap.isNotEmpty()) {
                observeOneToManyRelations(relationMap)
            }
        }
    }

    private fun unbindRelations() {
        BaseApp.genericTableFragmentHelper.unsetRelationBinding(dataBinding)
    }

    private fun observeManyToOneRelations(relations: Map<String, LiveData<RoomRelation>>, entity: EntityModel) {
        for ((relationName, liveDataRelatedEntity) in relations) {
            liveDataRelatedEntity.observe(
                requireNotNull(dataBinding.lifecycleOwner)
            ) { roomRelation ->
                roomRelation?.toOne?.let { relatedEntity ->
                    BaseApp.genericTableFragmentHelper.setRelationBinding(
                        dataBinding,
                        relationName,
                        relatedEntity
                    )
                    dataBinding.executePendingBindings()
                    refreshOneToManyNavForNavbarTitle(entity, relatedEntity)
                }
            }
        }
    }

    private fun refreshOneToManyNavForNavbarTitle(entity: EntityModel, anyRelatedEntity: RoomData) {
        entity.__KEY?.let { parentItemId ->
            BaseApp.runtimeDataHolder.oneToManyRelations[tableName]?.forEach { relationName ->
                if (relationName.contains(".")) {
                    BaseApp.genericNavigationResolver.setupOneToManyRelationButtonOnClickActionForCell(
                        viewDataBinding = dataBinding,
                        relationName = relationName,
                        parentItemId = parentItemId,
                        entity = entity,
                        anyRelatedEntity = anyRelatedEntity
                    )
                }
            }
        }
    }

    private fun observeOneToManyRelations(relations: Map<String, LiveData<RoomRelation>>) {
        for ((relationName, liveDataRelatedEntities) in relations) {
            liveDataRelatedEntities.observe(
                requireNotNull(dataBinding.lifecycleOwner)
            ) { roomRelation ->
                roomRelation?.toMany?.let { toMany ->
                    BaseApp.genericTableFragmentHelper.setRelationBinding(
                        dataBinding,
                        relationName,
                        toMany
                    )
                    dataBinding.executePendingBindings()
                }
            }
        }
    }
}
