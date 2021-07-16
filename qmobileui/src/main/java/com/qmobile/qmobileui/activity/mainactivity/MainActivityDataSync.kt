/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import com.qmobile.qmobileapi.auth.LoginRequiredCallback
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.sync.DataSync
import com.qmobile.qmobiledatasync.sync.EntityViewModelIsToSync
import com.qmobile.qmobiledatasync.sync.unsuccessfulSynchronizationNeedsLogin
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.utils.ToastHelper

class MainActivityDataSync(private val activity: MainActivity) {

    lateinit var entityViewModelIsToSyncList: MutableList<EntityViewModelIsToSync>

    // DataSync notifies MainActivity to go to login page
    private val loginRequiredCallbackForDataSync: LoginRequiredCallback =
        object : LoginRequiredCallback {
            override fun loginRequired() {
                if (!BaseApp.runtimeDataHolder.guestLogin) {
                    dataSync.unsuccessfulSynchronizationNeedsLogin(entityViewModelIsToSyncList)
                    activity.startLoginActivity()
                }
            }
        }

    val dataSync = DataSync(activity, BaseApp.sharedPreferencesHolder, loginRequiredCallbackForDataSync)

    fun getEntityListViewModelsForSync(entityListViewModelList: MutableList<EntityListViewModel<EntityModel>>) {
        entityViewModelIsToSyncList = mutableListOf()

        entityListViewModelList.forEach { entityListViewModel ->
            entityViewModelIsToSyncList.add(
                EntityViewModelIsToSync(
                    entityListViewModel,
                    true
                )
            )
        }
    }

    fun prepareDataSync(
        connectivityViewModel: ConnectivityViewModel,
        alreadyRefreshedTable: String?
    ) {
        if (connectivityViewModel.isConnected()) {
            connectivityViewModel.isServerConnectionOk { isAccessible ->
                if (isAccessible) {
                    setDataSyncObserver(alreadyRefreshedTable)
                } else {
                    // Nothing to do, errors already provided in isServerConnectionOk
                }
            }
        } else {
            ToastHelper.show(
                activity,
                activity.resources.getString(R.string.no_internet),
                MessageType.WARNING
            )
        }
    }

    fun setDataSyncObserver(alreadyRefreshedTable: String?) {
        entityViewModelIsToSyncList.map { it.isToSync = true }
        alreadyRefreshedTable?.let {
            entityViewModelIsToSyncList.find {
                it.vm.getAssociatedTableName() == alreadyRefreshedTable
            }?.isToSync = false
        }
        dataSync.setObserver(entityViewModelIsToSyncList, alreadyRefreshedTable)
    }
}
