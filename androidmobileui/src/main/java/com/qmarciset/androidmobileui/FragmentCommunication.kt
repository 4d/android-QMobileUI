/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui

import android.net.ConnectivityManager
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobileapi.network.LoginApiService

/**
 * Interface implemented by MainActivity to provide elements that depend on generated type
 */
interface FragmentCommunication {

    val apiService: ApiService

    val loginApiService: LoginApiService

    val connectivityManager: ConnectivityManager

    fun refreshApiClients()

    fun isConnected(): Boolean

    fun requestDataSync(alreadyRefreshedTable: String)

    fun requestAuthentication()
}
