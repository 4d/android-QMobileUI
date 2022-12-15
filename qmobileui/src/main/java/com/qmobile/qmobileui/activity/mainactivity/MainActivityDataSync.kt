/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import com.qmobile.qmobileapi.utils.LoginRequiredCallback
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.sync.DataSync
import com.qmobile.qmobiledatasync.sync.notifyDataUnSynced
import com.qmobile.qmobiledatasync.sync.syncDeletedRecords
import com.qmobile.qmobileui.network.NetworkChecker
import timber.log.Timber

class MainActivityDataSync(private val activity: MainActivity) {

    // DataSync notifies MainActivity to go to login page
    private val loginRequiredCallbackForDataSync: LoginRequiredCallback = {
        if (!BaseApp.runtimeDataHolder.guestLogin) {
            activity.entityListViewModelList.notifyDataUnSynced()
            activity.logout(true)
        }
    }

    val dataSync =
        DataSync(
            activity,
            activity.entityListViewModelList,
            activity.deletedRecordsViewModel,
            BaseApp.sharedPreferencesHolder,
            loginRequiredCallbackForDataSync
        )

    fun dataSync(alreadyRefreshedTable: String? = null) {
        activity.queryNetwork(
            object : NetworkChecker {
                override fun onServerAccessible() {
                    prepareViewModelsForDataSync(alreadyRefreshedTable)
                    dataSync.perform()
                }

                override fun onServerInaccessible() {
                    activity.onServerInaccessible("")
                }

                override fun onNoInternet() {
                    activity.onNoInternet("")
                }
            },
            toastError = true
        )
    }

    private fun prepareViewModelsForDataSync(alreadyRefreshedTable: String?) {
        activity.entityListViewModelList.map { it.isToSync.set(true) }
        alreadyRefreshedTable?.let {
            activity.entityListViewModelList.find {
                it.getAssociatedTableName() == alreadyRefreshedTable
            }?.isToSync?.set(false)
        }
    }

    fun shouldDataSync(currentTableName: String) {
        Timber.d("GlobalStamp changed, synchronization is required")
        if (activity.entityListViewModelList.size > 1) {
            Timber.i("Starting a dataSync procedure")
            dataSync(currentTableName)
        } else {
            Timber.i("The only table has already been synced. Only checking deletedRecords now")
            dataSync.syncDeletedRecords()
        }
    }
}
