/*
 * Created by Quentin Marciset on 29/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail.viewpager

import androidx.lifecycle.Lifecycle
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.utils.launchAndCollectIn
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobileui.activity.BaseObserver

class EntityViewPagerFragmentObserver(
    private val fragment: EntityViewPagerFragment,
    private val entityListViewModel: EntityListViewModel<EntityModel>
) : BaseObserver {

    override fun initObservers() {
        observeEntityList()
    }

    // Observe entity list
    private fun observeEntityList() {
        /*fragment.lifecycleScope.launchWhenCreated {
            fragment.adapter.loadStateFlow.map { it.refresh }
                .distinctUntilChanged()
                .collect {
                    if (it is LoadState.NotLoading) {
                        fragment.viewPager?.setCurrentItem(fragment.position, false)
                    }
                }
        }

        fragment.lifecycleScope.launch {
            entityListViewModel.entityListPagingDataFlow.distinctUntilChanged().collectLatest {
                fragment.adapter.submitData(it)
            }
        }*/

        entityListViewModel.entityListPagedListSharedFlow.launchAndCollectIn(
            fragment.viewLifecycleOwner,
            Lifecycle.State.STARTED
        ) { pagedList ->
            if (pagedList.isNotEmpty()) {
                fragment.adapter.submitList(pagedList)
                fragment.viewPager?.setCurrentItem(fragment.position, false)
            }
        }
    }
}
