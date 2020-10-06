/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import com.qmobile.qmobileapi.utils.getStringList
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.ManyToOneRelation
import com.qmobile.qmobiledatasync.relation.OneToManyRelation
import com.qmobile.qmobiledatasync.viewmodel.insert

/**
 * Commands the appropriate EntityListViewModel to add the related entity in its dao
 */
fun MainActivity.dispatchNewRelatedEntity(manyToOneRelation: ManyToOneRelation) {
    val entityListViewModel =
        entityListViewModelList.first { it.getAssociatedTableName() == manyToOneRelation.className }
    val entity = BaseApp.fromTableForViewModel.parseEntityFromTable(
        manyToOneRelation.className,
        manyToOneRelation.entity.toString(),
        true
    )
    entity?.let {
        entityListViewModel.insert(entity)
    }
}

/**
 * Commands the appropriate EntityListViewModel to add the related entities in its dao
 */
fun MainActivity.dispatchNewRelatedEntities(oneToManyRelation: OneToManyRelation) {
    val entityListViewModel =
        entityListViewModelList.first { it.getAssociatedTableName() == oneToManyRelation.className }
    for (entityString in oneToManyRelation.entities.getStringList()) {
        val entity = BaseApp.fromTableForViewModel.parseEntityFromTable(
            oneToManyRelation.className,
            entityString,
            true
        )
        entity?.let {
            entityListViewModel.insert(entity)
        }
    }
}
