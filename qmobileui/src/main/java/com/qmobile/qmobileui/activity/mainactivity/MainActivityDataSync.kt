/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import com.qmobile.qmobiledatasync.sync.EntityViewModelIsToSync
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.utils.customSnackBar

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

    if (connectivityViewModel.isConnected()) {
        connectivityViewModel.isServerConnectionOk { isAccessible ->
            if (isAccessible) {
                entityViewModelIsToSyncList.map { it.isToSync = true }
                alreadyRefreshedTable?.let {
                    entityViewModelIsToSyncList.find {
                        it.vm.getAssociatedTableName() == alreadyRefreshedTable
                    }?.isToSync = false
                }
                dataSync.setObserver(entityViewModelIsToSyncList, alreadyRefreshedTable)
            } else {
                // Nothing to do, errors already provided in isServerConnectionOk
            }
        }
    } else {
        customSnackBar(this, resources.getString(R.string.no_internet), null)
    }
}
