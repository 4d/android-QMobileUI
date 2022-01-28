/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui

import android.net.ConnectivityManager
import android.view.Menu
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.AccessibilityApiService
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.network.LoginApiService
import com.qmobile.qmobiledatasync.toast.Event
import com.qmobile.qmobiledatasync.toast.ToastMessageHolder
import com.qmobile.qmobileui.action.Action
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.network.RemoteUrlChange
import kotlinx.coroutines.flow.SharedFlow

/**
 * Interface implemented by MainActivity to provide elements that depend on generated type
 */
@Suppress("TooManyFunctions")
interface FragmentCommunication {

    val apiService: ApiService

    val loginApiService: LoginApiService

    val accessibilityApiService: AccessibilityApiService

    val connectivityManager: ConnectivityManager

    fun refreshAllApiClients()

    fun requestDataSync(currentTableName: String)

    fun requestAuthentication()

    fun showRemoteUrlEditDialog(remoteUrl: String, remoteUrlChange: RemoteUrlChange)

    fun checkNetwork(networkChecker: NetworkChecker)

    fun observeEntityToastMessage(message: SharedFlow<Event<ToastMessageHolder>>)

    fun setupActionsMenu(menu: Menu, actions: List<Action>, onMenuItemClick: (Action) -> Unit)

    fun setSelectAction(action: Action)

    fun getSelectAction(): Action

    fun setSelectedEntity(entityModel: EntityModel?)

    fun getSelectedEntity(): EntityModel?

    fun setFullScreenMode(isFullScreen: Boolean)
}
