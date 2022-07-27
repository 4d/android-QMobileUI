/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui

import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobiledatasync.toast.Event
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobileui.network.NetworkChecker
import kotlinx.coroutines.flow.SharedFlow

/**
 * Interface implemented by MainActivity to provide elements that depend on generated type
 */
interface FragmentCommunication {

    val apiService: ApiService // list, viewpager, detail

    fun checkNetwork(networkChecker: NetworkChecker)

    fun requestDataSync(currentTableName: String) // list

    fun observeEntityToastMessage(message: SharedFlow<Event<ToastMessage.Holder>>) // detail

    fun setFullScreenMode(isFullScreen: Boolean) // actions
}
