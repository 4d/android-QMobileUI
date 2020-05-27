/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui

import android.app.Application
import android.net.ConnectivityManager
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobileapi.network.LoginApiService
import com.qmarciset.androidmobiledatastore.db.AppDatabaseInterface
import com.qmarciset.androidmobiledatasync.utils.FromTableForViewModel
import com.qmarciset.androidmobileui.utils.FromTableInterface
import com.qmarciset.androidmobileui.utils.NavigationInterface
import com.qmarciset.androidmobileui.utils.ViewDataBindingInterface

/**
 * Interface implemented by MainActivity to provide elements that depend on generated type
 */
interface FragmentCommunication {

    val appInstance: Application

    val apiService: ApiService

    val loginApiService: LoginApiService

    val fromTableInterface: FromTableInterface

    val fromTableForViewModel: FromTableForViewModel

    val viewDataBindingInterface: ViewDataBindingInterface

    val navigationInterface: NavigationInterface

    val appDatabaseInterface: AppDatabaseInterface

    val connectivityManager: ConnectivityManager

    fun refreshApiClients()

    fun isConnected(): Boolean

    fun requestDataSync(alreadyRefreshedTable: String)

    fun requestAuthentication()
}
