/*
 * Created by Quentin Marciset on 29/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail.viewpager

import androidx.lifecycle.lifecycleScope
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobileui.activity.BaseObserver
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class EntityViewPagerFragmentObserver(
    private val fragment: EntityViewPagerFragment,
    private val entityListViewModel: EntityListViewModel<EntityModel>,
    private val key: String
) : BaseObserver {

    override fun initObservers() {
        observeEntityList()
    }

    // Observe entity list
    private fun observeEntityList() {
        entityListViewModel.entityListLiveData.observe(
            fragment.viewLifecycleOwner,
            {
                fragment.adapter.submitList(it)
                val index = it.indexOfFirst { entityModel -> entityModel.__KEY == key }
                if (index > -1) {
                    fragment.viewPager?.setCurrentItem(index, false)
                }
            }
        )

//        fragment.lifecycleScope.launch {
//            entityListViewModel.entityListFlow.distinctUntilChanged().collectLatest {
//                fragment.adapter.submitData(it)
////                val index = it.indexOfFirst { entityModel -> entityModel.__KEY == key }
////                if (index > -1) {
////                    fragment.viewPager?.setCurrentItem(index, false)
////                }
//            }
//        }
    }
}
