/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.activity.mainactivity

import com.qmarciset.androidmobiledatasync.relation.ManyToOneRelation
import com.qmarciset.androidmobiledatasync.relation.OneToManyRelation
import com.qmarciset.androidmobiledatasync.viewmodel.insert

/**
 * Commands the appropriate EntityListViewModel to add the related entity in its dao
 */
fun MainActivity.dispatchNewRelatedEntity(manyToOneRelation: ManyToOneRelation) {
    val entityListViewModel =
        entityListViewModelList.first { it.getAssociatedTableName() == manyToOneRelation.className }
    entityListViewModel.insert(manyToOneRelation.entity)
}

/**
 * Commands the appropriate EntityListViewModel to add the related entities in its dao
 */
fun MainActivity.dispatchNewRelatedEntities(oneToManyRelation: OneToManyRelation) {
    val entityListViewModel =
        entityListViewModelList.first { it.getAssociatedTableName() == oneToManyRelation.className }
    entityListViewModel.decodeEntityModel(oneToManyRelation.entities, true)
}
