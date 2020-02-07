/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.utils

import com.qmarciset.androidmobiledatasync.viewmodel.EntityListViewModel
import kotlin.reflect.KClass

/**
 * Interface implemented by MainActivity to provide different elements depending of the generated type
 */
interface FromTableInterface {

    /**
     * Provides the list of table names
     */
    val tableNames: List<String>

    /**
     * Provides the appropriate EntityListViewModel KClass
     */
    fun entityListViewModelClassFromTable(tableName: String): KClass<EntityListViewModel<*>>

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
