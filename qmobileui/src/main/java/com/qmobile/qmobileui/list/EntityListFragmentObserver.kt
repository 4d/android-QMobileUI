/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.annotation.SuppressLint
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.utils.ScheduleRefreshEnum
import com.qmobile.qmobiledatasync.utils.collectWhenStarted
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobileui.activity.BaseObserver
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

class EntityListFragmentObserver(
    private val fragment: EntityListFragment,
    private val entityListViewModel: EntityListViewModel<EntityModel>
) : BaseObserver {

    override fun initObservers() {
        observeDataSynchronized()
        observeScheduleRefresh()
        observeEntityList()
    }

    // Observe when data are synchronized
    @SuppressLint("BinaryOperationInTimber")
    private fun observeDataSynchronized() {
        fragment.collectWhenStarted(entityListViewModel.dataSynchronized) { dataSyncState ->
            Timber.d(
                "[DataSyncState : $dataSyncState, " +
                    "Table : ${entityListViewModel.getAssociatedTableName()}, " +
                    "Instance : $entityListViewModel]"
            )
        }
    }

    private fun observeScheduleRefresh() {
        fragment.collectWhenStarted(entityListViewModel.scheduleRefresh) { scheduleRefresh ->
            if (scheduleRefresh == ScheduleRefreshEnum.PERFORM) {
                entityListViewModel.setScheduleRefreshState(ScheduleRefreshEnum.NO)
                val layoutManager = fragment.binding.fragmentListRecyclerView.layoutManager as LinearLayoutManager
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                val childCount = fragment.binding.fragmentListRecyclerView.childCount
                fragment.adapter.notifyItemRangeChanged(firstVisible, childCount)
            }
        }
    }

    private fun observeEntityList() {
        fragment.lifecycleScope.launch {
            entityListViewModel.entityListFlow.distinctUntilChanged().collectLatest {
                fragment.adapter.submitData(it)
            }
        }
    }
}
