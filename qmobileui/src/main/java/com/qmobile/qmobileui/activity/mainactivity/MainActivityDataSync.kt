/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import com.qmobile.qmobileapi.utils.LoginRequiredCallback
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.sync.DataSync
import com.qmobile.qmobiledatasync.sync.unsuccessfulSynchronizationNeedsLogin
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.utils.ToastHelper

class MainActivityDataSync(private val activity: MainActivity) {

    // DataSync notifies MainActivity to go to login page
    private val loginRequiredCallbackForDataSync: LoginRequiredCallback = {
        if (!BaseApp.runtimeDataHolder.guestLogin) {
            dataSync.unsuccessfulSynchronizationNeedsLogin(activity.entityListViewModelList)
            activity.startLoginActivity()
        }
    }

    val dataSync = DataSync(activity, BaseApp.sharedPreferencesHolder, loginRequiredCallbackForDataSync)

    fun prepareDataSync(
        connectivityViewModel: ConnectivityViewModel,
        alreadyRefreshedTable: String?
    ) {
        if (connectivityViewModel.isConnected()) {
            connectivityViewModel.isServerConnectionOk { isAccessible ->
                if (isAccessible) {
                    setDataSyncObserver(alreadyRefreshedTable)
                } // else : Nothing to do, errors already provided in isServerConnectionOk
            }
        } else {
            ToastHelper.show(
                activity,
                activity.resources.getString(R.string.no_internet),
                MessageType.WARNING
            )
        }
    }

    private fun setDataSyncObserver(alreadyRefreshedTable: String?) {
        activity.entityListViewModelList.map { it.isToSync.set(true) }
        alreadyRefreshedTable?.let {
            activity.entityListViewModelList.find {
                it.getAssociatedTableName() == alreadyRefreshedTable
            }?.isToSync?.set(false)
        }
        dataSync.setObserver(activity.entityListViewModelList, alreadyRefreshedTable)
    }
}
