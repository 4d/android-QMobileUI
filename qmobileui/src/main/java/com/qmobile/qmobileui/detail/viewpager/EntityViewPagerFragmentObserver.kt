/*
 * Created by Quentin Marciset on 29/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail.viewpager

import com.qmobile.qmobileapi.model.entity.EntityModel
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
    // fun EntityViewPagerFragment.observeEntityList(sqLiteQuery: SupportSQLiteQuery) {
    private fun observeEntityList() {
        entityListViewModel.entityListLiveData.observe(
            fragment.viewLifecycleOwner,
            {
                fragment.viewPager?.adapter =
                    EntityViewPagerAdapter(
                        fragment,
                        fragment.tableName,
                        it
                    )
                fragment.viewPager?.addOnPageChangeListener(fragment)
                fragment.viewPager?.currentItem = fragment.position
            }
        )
        //    job?.cancel()
//    job = lifecycleScope.launch {
//        entityListViewModel.getAllDynamicQueryFlow(sqLiteQuery).collectLatest {
//            // When entity list data changed, refresh the displayed list
//            viewPager?.adapter =
//                EntityViewPagerAdapter(
//                    this@observeEntityList,
//                    tableName,
//                    it
//                )
//            viewPager?.addOnPageChangeListener(this@observeEntityList)
//            viewPager?.currentItem = position
//        }
//    }

//    entityListViewModel.getAllDynamicQuery(sqLiteQuery).observe(
//        viewLifecycleOwner,
//        { entities ->
//            entities?.let {
//                // When entity list data changed, refresh the displayed list
//                viewPager?.adapter =
//                    EntityViewPagerAdapter(
//                        this,
//                        tableName,
//                        it
//                    )
//                viewPager?.addOnPageChangeListener(this)
//                viewPager?.currentItem = position
//            }
//        }
//    )
    }
}
