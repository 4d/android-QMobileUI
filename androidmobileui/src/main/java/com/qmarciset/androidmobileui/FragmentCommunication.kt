package com.qmarciset.androidmobileui

import android.app.Application
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobileapi.network.LoginApiService
import com.qmarciset.androidmobiledatastore.db.AppDatabaseInterface
import com.qmarciset.androidmobileui.utils.FromTableInterface
import com.qmarciset.androidmobileui.utils.NavigationInterface
import com.qmarciset.androidmobileui.utils.ViewDataBindingInterface

interface FragmentCommunication {

    val appInstance: Application

    val apiService: ApiService

    val loginApiService: LoginApiService

    val fromTableInterface: FromTableInterface

    val viewDataBindingInterface: ViewDataBindingInterface

    val navigationInterface: NavigationInterface

    val appDatabaseInterface: AppDatabaseInterface

    fun toast(message: String)
}
