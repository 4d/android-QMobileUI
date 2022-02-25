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
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.relation.RelationTypeEnum
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
                RelationHelper.setupRelationNavigation(fragment.tableName, fragment.binding, entity)
            }
        }
    }

    private fun setupObserver(entity: EntityModel) {
        RelationHelper.getRelationsLiveDataMap(fragment.tableName, entity).let { relationMap ->
            if (relationMap.isNotEmpty()) {
                observeRelations(relationMap, entity)
            }
        }
    }

    private fun observeRelations(relations: Map<Relation, LiveData<List<RoomData>>>, entity: EntityModel) {
        for ((relation, liveDataRelatedEntity) in relations) {
            liveDataRelatedEntity.observe(requireNotNull(fragment.viewLifecycleOwner)) { roomRelation ->
                roomRelation?.let {
                    entityViewModel.setRelationToLayout(relation.name, roomRelation)
                    if (relation.type == RelationTypeEnum.MANY_TO_ONE) {
                        roomRelation.firstOrNull()?.let {
                            RelationHelper.refreshOneToManyNavForNavbarTitle(
                                fragment.tableName, fragment.binding, entity, it
                            )
                        }
                    }
                }
            }
        }
    }
}
