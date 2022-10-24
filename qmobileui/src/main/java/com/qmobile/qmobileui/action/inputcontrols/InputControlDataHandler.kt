/*
 * Created by qmarciset on 22/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.inputcontrols

import androidx.fragment.app.Fragment
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.utils.FieldMapping
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols.BaseInputControlViewHolder.Companion.NO_VALUE_PLACEHOLDER_KEY
import com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols.CustomViewViewHolder
import com.qmobile.qmobileui.ui.SnackbarHelper
import java.util.LinkedList

interface InputControlDataHandler {

    var fieldMapping: FieldMapping?

    fun setupInputControlData(isMandatory: Boolean) {
        // Input control data can either be a static list / map or fetched data from database
        BaseApp.runtimeDataHolder.inputControls.find { it.name == fieldMapping?.name }?.let { fieldMapping ->

            if (InputControl.hasStaticData(fieldMapping.choiceList)) {
                fieldMapping.prepareStaticData(isMandatory)
            } else {
                fieldMapping.prepareDataSource(isMandatory)
            }
        }
    }

    fun FieldMapping.prepareStaticData(isMandatory: Boolean)

    fun FieldMapping.prepareDataSource(isMandatory: Boolean)

    fun handleMandatory(items: LinkedList<Any>, isMandatory: Boolean, callback: (items: LinkedList<Any>) -> Unit) {
        if (isMandatory || this is CustomViewViewHolder) {
            callback(items)
        } else {
            if (items.size > 0) {
                val newList = LinkedList<Any>()
                when (items[0]) {
                    is Pair<*, *> -> newList.add(Pair(NO_VALUE_PLACEHOLDER_KEY, null))
                    else -> newList.add(NO_VALUE_PLACEHOLDER_KEY)
                }
                newList.addAll(items)
                callback(newList)
            }
        }
    }

    fun handleDataSource(
        fragment: Fragment,
        fieldMapping: FieldMapping,
        callback: (entities: LinkedList<Any>, field: String, entityFormat: String?, hasSearch: Boolean) -> Unit
    ): InputControlDataSourceHandler {
        val dataSource = InputControl.getDataSource(fieldMapping.choiceList)
        val searchFields = InputControl.getSearchFields(dataSource)
        val sortFields = InputControl.getSortFields(dataSource)

        val dataSourceHandler = InputControlDataSourceHandler(fragment, dataSource)

        var errorGiven = false // only give the empty data error once

        dataSourceHandler.apply {
            prepare(fieldMapping.name, searchFields, sortFields) { field: String, entityFormat: String? ->
                setSearchQuery()
                observe { entities ->
                    if (entities.isEmpty()) {
                        if (!errorGiven) {
                            SnackbarHelper.show(
                                fragment.activity,
                                fragment.resources.getString(
                                    R.string.input_control_datasource_no_data,
                                    fieldMapping.name
                                )
                            )
                            errorGiven = true
                        }
                    }
                    val linkedList: LinkedList<Any> = LinkedList(entities)
                    callback(linkedList, field, entityFormat, searchFields.isNotEmpty())
                }
            }
        }
        return dataSourceHandler
    }
}
