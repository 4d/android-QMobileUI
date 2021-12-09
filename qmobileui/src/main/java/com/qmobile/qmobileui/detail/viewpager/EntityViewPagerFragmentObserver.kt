/*
 * Created by Quentin Marciset on 29/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail.viewpager

import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.utils.collectWhenStarted
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobileui.activity.BaseObserver

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
        fragment.viewLifecycleOwner.collectWhenStarted(entityListViewModel.entityListPagedListSharedFlow) {
            fragment.adapter.submitList(it)
            val index = it.indexOfFirst { entityModel -> entityModel.__KEY == key }
            if (index > -1) {
                fragment.viewPager?.setCurrentItem(index, false)
            }
        }
    }
}
