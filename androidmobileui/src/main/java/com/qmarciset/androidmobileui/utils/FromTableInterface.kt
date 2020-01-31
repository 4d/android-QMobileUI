package com.qmarciset.androidmobileui.utils

import android.app.Application
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobiledatastore.db.AppDatabaseInterface
import com.qmarciset.androidmobileui.viewmodel.EntityListViewModel
import kotlin.reflect.KClass

/**
 * Interface implemented by MainActivity to provide different elements depending of the generated type
 */
interface FromTableInterface {

    /**
     * Provides the appropriate EntityListViewModel KClass
     */
    fun entityListViewModelClassFromTable(tableName: String): KClass<EntityListViewModel<*>>

    /**
     * Provides the appropriate Entity
     */
    fun parseEntityFromTable(tableName: String, jsonString: String): EntityModel

    /**
     * Checks if entity has a __GlobalStamp property
     */
    fun hasGlobalStampPropertyFromTable(tableName: String): Boolean

    /**
     * Provides the appropriate EntityListViewModel
     */
    fun <T> entityListViewModelFromTable(
        tableName: String,
        application: Application,
        appDatabase: AppDatabaseInterface,
        apiService: ApiService,
        fromTableInterface: FromTableInterface
    ): EntityListViewModel<T>

    /**
     * Provides the appropriate list layout
     */
    fun listLayoutFromTable(tableName: String): Int

    /**
     * Provides the appropriate RecyclerView item layout
     */
    fun itemLayoutFromTable(tableName: String): Int

    /**
     * Provides the appropriate detail layout
     */
    fun detailLayoutFromTable(tableName: String): Int
}
