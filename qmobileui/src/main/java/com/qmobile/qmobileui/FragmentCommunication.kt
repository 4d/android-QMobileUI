/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui

import android.net.ConnectivityManager
import com.qmobile.qmobileapi.network.AccessibilityApiService
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.network.LoginApiService

/**
 * Interface implemented by MainActivity to provide elements that depend on generated type
 */
interface FragmentCommunication {

    val apiService: ApiService

    val loginApiService: LoginApiService

    val accessibilityApiService: AccessibilityApiService

    val connectivityManager: ConnectivityManager

    fun refreshApiClients()

    fun isConnected(): Boolean

    fun requestDataSync(alreadyRefreshedTable: String)

    fun requestAuthentication()

    fun darkModeEnabled(): Boolean
}
