/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.annotation.SuppressLint
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.sync.DataSyncStateEnum
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobileui.activity.BaseObserver
import timber.log.Timber

class EntityListFragmentRelationObserver(
    private val fragment: EntityListFragmentRelation,
    private val entityListViewModel: EntityListViewModel<EntityModel>
) : BaseObserver {

    override fun initObservers() {
        observeDataSynchronized()
        observeEntityListDynamicSearch()
    }

    // Observe when data are synchronized
    @SuppressLint("BinaryOperationInTimber")
    private fun observeDataSynchronized() {
        entityListViewModel.dataSynchronized.observe(
            fragment.viewLifecycleOwner,
            { dataSyncState ->
                Timber.d(
                    "[DataSyncState : $dataSyncState, " +
                        "Table : ${entityListViewModel.getAssociatedTableName()}, " +
                        "Instance : $entityListViewModel]"
                )
                // Refresh views as cached imaged would not be updated
                if (dataSyncState == DataSyncStateEnum.SYNCHRONIZED) {
                    fragment.adapter.notifyDataSetChanged()
                }
            }
        )
    }

    // Sql Dynamic Query Support
    private fun observeEntityListDynamicSearch() {
        entityListViewModel.entityListLiveData.observe(
            fragment.viewLifecycleOwner,
            {
                fragment.adapter.submitList(it)
            }
        )
//    entityListViewModel.getAllDynamicQuery(sqLiteQuery).observe(
//        viewLifecycleOwner,
//        { pagedList ->
//            adapter.submitList(pagedList)
//        }
//    )
    }
}
