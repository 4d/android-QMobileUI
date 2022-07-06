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
        entityListViewModel.entityListPagedListSharedFlow.launchAndCollectIn(
            fragment.viewLifecycleOwner,
            Lifecycle.State.STARTED
        ) {
            fragment.adapter.submitList(it)
            val index = it.indexOfFirst { roomEntity -> (roomEntity.__entity as EntityModel?)?.__KEY == fragment.key }
            if (index > -1) {
                fragment.viewPager?.setCurrentItem(index, false)
            }
        }
//        fragment.lifecycleScope.launch {
//            entityListViewModel.entityListPagingDataFlow.distinctUntilChanged().collectLatest {
//                fragment.adapter.submitData(it)
//                val index = it.indexOfFirst { roomEntity -> (roomEntity.__entity as EntityModel?)?.__KEY == fragment.key }
//                if (index > -1) {
//                    fragment.viewPager?.setCurrentItem(index, false)
//                }
//                fragment.viewPager?.adapter = fragment.adapter
//            }
//        }
    }
}
