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
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.relation.RelationTypeEnum
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
    fun bind(entityModel: EntityModel?) {
        entityModel?.let { entity ->
            dataBinding.setVariable(BR.entityData, entity)
            dataBinding.executePendingBindings()

            setupClickListeners(entity)
            RelationHelper.setupRelationNavigation(tableName, dataBinding, entity)

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

    private fun setupObserver(entity: EntityModel) {
        RelationHelper.getRelationsLiveData(tableName, entity).let { relationMap ->
            if (relationMap.isNotEmpty()) {
                observeRelations(relationMap, entity)
            }
        }
    }

    private fun observeRelations(relations: Map<Relation, LiveData<List<RoomData>>>, entity: EntityModel) {
        for ((relation, liveDataRelatedEntity) in relations) {
            liveDataRelatedEntity.observe(requireNotNull(dataBinding.lifecycleOwner)) { roomRelation ->
                if (relation.type == RelationTypeEnum.MANY_TO_ONE)
                    handleManyToOne(roomRelation?.firstOrNull(), relation.name, entity)
                else
                    handleOneToMany(roomRelation, relation.name)
            }
        }
    }

    private fun handleManyToOne(relatedEntity: RoomData?, relationName: String, entity: EntityModel) {
        relatedEntity?.let {
            BaseApp.genericTableFragmentHelper.setRelationBinding(
                dataBinding,
                relationName,
                relatedEntity
            )
            dataBinding.executePendingBindings()
            RelationHelper.refreshOneToManyNavForNavbarTitle(tableName, dataBinding, entity, relatedEntity)
        }
    }

    private fun handleOneToMany(toMany: List<RoomData>?, relationName: String) {
        toMany?.let {
            BaseApp.genericTableFragmentHelper.setRelationBinding(
                dataBinding,
                relationName,
                toMany
            )
            dataBinding.executePendingBindings()
        }
    }

    private fun unbindRelations() {
        BaseApp.genericTableFragmentHelper.unsetRelationBinding(dataBinding)
    }
}
