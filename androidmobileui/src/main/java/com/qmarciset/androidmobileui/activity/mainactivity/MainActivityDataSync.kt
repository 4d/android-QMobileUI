/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.activity.mainactivity

import com.qmarciset.androidmobiledatasync.sync.EntityViewModelIsToSync

fun MainActivity.getEntityListViewModelsForSync() {
    entityViewModelIsToSyncList = mutableListOf()

    for (entityListViewModel in entityListViewModelList) {
        entityViewModelIsToSyncList.add(
            EntityViewModelIsToSync(
                entityListViewModel,
                true
            )
        )
    }
}

fun MainActivity.setDataSyncObserver(alreadyRefreshedTable: String?) {
    entityViewModelIsToSyncList.map { it.isToSync = true }
    alreadyRefreshedTable?.let {
        entityViewModelIsToSyncList.filter { it.vm.getAssociatedTableName() == alreadyRefreshedTable }[0].isToSync =
            false
    }
    dataSync.setObserver(entityViewModelIsToSyncList, alreadyRefreshedTable)
}
