package com.qmarciset.androidmobileui.utils

import android.app.Application
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobiledatastore.AppDatabaseInterface
import com.qmarciset.androidmobileui.viewmodel.EntityListViewModel
import kotlin.reflect.KClass

interface FromTableInterface {

    fun entityListViewModelClassFromTable(tableName: String): KClass<EntityListViewModel<*>>

    fun parseEntityFromTable(tableName: String, jsonString: String): EntityModel

    fun <T> entityListViewModelFromTable(
        tableName: String,
        application: Application,
        appDatabase: AppDatabaseInterface,
        apiService: ApiService,
        fromTableInterface: FromTableInterface
    ): EntityListViewModel<T>

    fun listLayoutFromTable(tableName: String): Int

    fun itemLayoutFromTable(tableName: String): Int

    fun detailLayoutFromTable(tableName: String): Int
}
