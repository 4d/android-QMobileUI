/*
 * Created by qmarciset on 9/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui

import android.net.ConnectivityManager
import com.qmobile.qmobileapi.network.AccessibilityApiService
import com.qmobile.qmobileapi.network.LoginApiService
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.network.RemoteUrlChanger

interface ActivitySettingsInterface {

    val loginApiService: LoginApiService

    val accessibilityApiService: AccessibilityApiService

    val connectivityManager: ConnectivityManager

    fun refreshAllApiClients()

    fun requestAuthentication()

    fun showRemoteUrlEditDialog(remoteUrl: String, remoteUrlChanger: RemoteUrlChanger)

    fun checkNetwork(networkChecker: NetworkChecker)

    fun logout()
}
