/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list.viewholder

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomData
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.relation.RelationHelper.setupNavManyToOne
import com.qmobile.qmobiledatasync.relation.RelationHelper.setupNavOneToMany
import com.qmobile.qmobileui.BR
import com.qmobile.qmobileui.ui.setOnSingleClickListener

class BaseViewHolder(
    private val dataBinding: ViewDataBinding,
    private val tableName: String,
    private val onItemClick: (ViewDataBinding, String) -> Unit,
    private val onItemLongClick: (RoomEntity) -> Unit
) :
    RecyclerView.ViewHolder(dataBinding.root) {

    // Applies DataBinding
    fun bind(entityModel: RoomEntity?) {
        entityModel?.let { entity ->
            dataBinding.setVariable(BR.entityData, entity)
            dataBinding.executePendingBindings()

            setupClickListeners(entity)
            RelationHelper.setupRelationNavigation(tableName, dataBinding, entity)

            // unbind because of issue : item at position 11 receives binding of item 0,
            // item at position 12 receives binding of item at position 1, etc.
//            unbindRelations()
//            setupObserver(entity)
        }
    }

    private fun setupClickListeners(entity: RoomEntity) {
        (entity.__entity as EntityModel).__KEY?.let { key ->
            itemView.setOnSingleClickListener {
                onItemClick(dataBinding, key)
            }
            itemView.setOnLongClickListener {
                onItemLongClick(entity)
                true
            }
        }
    }

//    private fun setupObserver(entity: EntityModel) {
//        RelationHelper.getRelationsLiveDataMap(tableName, entity).let { relationMap ->
//            if (relationMap.isNotEmpty()) {
//                observeRelations(relationMap, entity)
//            }
//        }
//    }
//
//    private fun observeRelations(relations: Map<Relation, Relation.QueryResult>, entity: EntityModel) {
//        for ((relation, queryResult) in relations) {
//            queryResult.liveData.observe(requireNotNull(dataBinding.lifecycleOwner)) { roomRelation ->
//                if (relation.type == Relation.Type.MANY_TO_ONE) {
//                    bindManyToOne(roomRelation, relation.name)
//                    dataBinding.setupNavManyToOne(roomRelation, relation.name)
//                } else {
//                    bindOneToMany(roomRelation, relation.name)
//                    dataBinding.setupNavOneToMany(queryResult.query, relation.name, entity)
//                }
//                dataBinding.executePendingBindings()
//            }
//        }
//    }
//
//    private fun bindManyToOne(roomRelation: List<RoomEntity>, relationName: String) {
//        roomRelation.firstOrNull()?.let { relatedEntity ->
//            BaseApp.genericTableFragmentHelper.setRelationBinding(
//                dataBinding,
//                relationName,
//                relatedEntity
//            )
////            RelationHelper.refreshOneToManyNavForNavbarTitle(tableName, dataBinding, entity, relatedEntity)
//        }
//    }
//
//    private fun bindOneToMany(toMany: List<RoomEntity>?, relationName: String) {
//        toMany?.let {
//            BaseApp.genericTableFragmentHelper.setRelationBinding(
//                dataBinding,
//                relationName,
//                toMany
//            )
//        }
//    }
//
//    private fun unbindRelations() {
//        BaseApp.genericTableFragmentHelper.unsetRelationBinding(dataBinding)
//    }
}
