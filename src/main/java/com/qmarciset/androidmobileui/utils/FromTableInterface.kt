package com.qmarciset.androidmobileui.utils

import android.app.Application
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobiledatastore.AppDatabaseInterface
import com.qmarciset.androidmobileui.viewmodel.EntityListViewModel

interface FromTableInterface {

    fun parseEntityFromTable(tableName: String, jsonString: String): EntityModel

    fun <T> entityListViewModelFromTable(
        tableName: String,
        application: Application,
        appDatabase: AppDatabaseInterface,
        fromTableInterface: FromTableInterface
    ): EntityListViewModel<T>
}
