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
import com.qmobile.qmobiledatastore.data.RoomRelation
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.BR
import timber.log.Timber

class BaseViewHolder(
    private val dataBinding: ViewDataBinding,
    private val tableName: String
) :
    RecyclerView.ViewHolder(dataBinding.root) {

    // Applies DataBinding
    fun bind(entity: Any?, position: Int) {
        (entity as? EntityModel)?.__KEY?.let { parentItemId ->
            dataBinding.setVariable(BR.entityData, entity)
            dataBinding.executePendingBindings()
            itemView.setOnClickListener {
                BaseApp.genericNavigationResolver.navigateFromListToViewPager(
                    dataBinding,
                    position
                )
            }
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

    fun unbindRelations() {
        BaseApp.genericTableFragmentHelper.unsetRelationBinding(dataBinding)
    }

    // Map<relationName, LiveData<RoomRelation>>
    fun observeRelations(
        relations: Map<String, LiveData<RoomRelation>>, /* only for logs */
        position: Int
    ) {
        for ((relationName, liveDataRelatedEntity) in relations) {
            liveDataRelatedEntity.observe(
                requireNotNull(dataBinding.lifecycleOwner),
                { roomRelation ->
                    roomRelation?.toOne?.let { relatedEntity ->
                        Timber.d("[$tableName] Relation named \"$relationName\" retrieved for position $position")
                        BaseApp.genericTableFragmentHelper.setRelationBinding(
                            dataBinding,
                            relationName,
                            relatedEntity
                        )
                        dataBinding.executePendingBindings()
                    }
                }
            )
        }
    }
}
