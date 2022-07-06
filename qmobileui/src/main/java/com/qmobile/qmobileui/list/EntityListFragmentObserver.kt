/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.utils.ScheduleRefresh
import com.qmobile.qmobiledatasync.utils.launchAndCollectIn
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
    private fun observeDataSynchronized() {
        entityListViewModel.dataSynchronized.launchAndCollectIn(fragment, Lifecycle.State.STARTED) { dataSyncState ->
            Timber.d(
                "[DataSyncState : $dataSyncState, " +
                    "Table : ${entityListViewModel.getAssociatedTableName()}] "
            )
        }
    }

    private fun observeScheduleRefresh() {
        entityListViewModel.scheduleRefresh.launchAndCollectIn(fragment, Lifecycle.State.STARTED) { scheduleRefresh ->
            if (scheduleRefresh == ScheduleRefresh.PERFORM) {
                entityListViewModel.setScheduleRefreshState(ScheduleRefresh.NO)
                val layoutManager = fragment.binding.fragmentListRecyclerView.layoutManager as LinearLayoutManager
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                val childCount = fragment.binding.fragmentListRecyclerView.childCount
                fragment.adapter.notifyItemRangeChanged(firstVisible, childCount)
            }
        }
    }

    private fun observeEntityList() {
        fragment.lifecycleScope.launch {
            entityListViewModel.entityListPagingDataFlow.distinctUntilChanged().collectLatest {
                fragment.adapter.submitData(it)
            }
        }
    }
}
