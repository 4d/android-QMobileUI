/*
 * Created by qmarciset on 21/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.inputcontrols

import androidx.fragment.app.Fragment
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.ui.SnackbarHelper
import com.qmobile.qmobileui.utils.FormQueryBuilder

class InputControlDataSourceHandler(private val fragment: Fragment, private val dataSource: Map<String, Any>) {

    private lateinit var entityListViewModel: EntityListViewModel<EntityModel>
    private lateinit var formQueryBuilder: FormQueryBuilder
    private var dataClass = ""

    fun prepare(
        inputControlName: String?,
        searchFields: List<String>,
        sortFields: LinkedHashMap<String, String>,
        onInputControlReady: (field: String, entityFormat: String?) -> Unit
    ) {
        InputControl.getDataClass(dataSource)?.let { dataClass ->
            this.dataClass = dataClass

            if (!BaseApp.runtimeDataHolder.tableInfo.keys.contains(dataClass)) {
                SnackbarHelper.show(
                    fragment.activity,
                    fragment.resources.getString(
                        R.string.input_control_datasource_no_table,
                        dataClass,
                        inputControlName
                    )
                )
                return
            }

            InputControl.getField(dataSource)?.let { field ->

                if (BaseApp.runtimeDataHolder.tableInfo[dataClass]?.fields?.contains(field) != true) {
                    SnackbarHelper.show(
                        fragment.activity,
                        fragment.resources.getString(
                            R.string.input_control_datasource_no_field,
                            field,
                            dataClass,
                            inputControlName
                        )
                    )
                    return
                }

                val entityFormat = InputControl.getEntityFormat(dataSource)

                formQueryBuilder = FormQueryBuilder(dataClass, searchFields, sortFields)
                (fragment.activity as? FragmentCommunication)?.let { delegate ->
                    // We want a new instance of the viewModel because searchView will impact EntityListFragment searchView
                    entityListViewModel = getEntityListViewModel(fragment, dataClass, delegate.apiService, true)
                    onInputControlReady(field, entityFormat)
                }
            }
        }
    }

    fun observe(dataRetrievedCallback: (entities: List<RoomEntity>) -> Unit) {
        InputControlDataSourceObserver(fragment, entityListViewModel, dataRetrievedCallback).initObservers()
    }

    fun setSearchQuery(searchPattern: String = "") {
        val formQuery = formQueryBuilder.getInputControlQuery(searchPattern)
        entityListViewModel.setSearchQuery(formQuery)
    }
}
