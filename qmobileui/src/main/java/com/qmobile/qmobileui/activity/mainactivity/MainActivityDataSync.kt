/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import com.qmobile.qmobiledatasync.sync.EntityViewModelIsToSync

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
        entityViewModelIsToSyncList.first { it.vm.getAssociatedTableName() == alreadyRefreshedTable }.isToSync =
            false
    }
    dataSync.setObserver(entityViewModelIsToSyncList, alreadyRefreshedTable)
}
