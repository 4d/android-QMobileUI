/*
 * Created by Quentin Marciset on 29/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail.viewpager

import androidx.lifecycle.Observer
import androidx.sqlite.db.SupportSQLiteQuery
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel

fun EntityViewPagerFragment.getViewModel() {
    entityListViewModel = getEntityListViewModel(activity, tableName, delegate.apiService)
}

// fun EntityViewPagerFragment.setupObservers() {
//    observeEntityList()
// }

// Observe entity list
fun EntityViewPagerFragment.observeEntityList(sqLiteQuery: SupportSQLiteQuery) {
    entityListViewModel.getAllDynamicQuery(sqLiteQuery).observe(
        viewLifecycleOwner,
        Observer { entities ->
            entities?.let {
                // When entity list data changed, refresh the displayed list
                viewPager?.adapter =
                    EntityViewPagerAdapter(
                        this,
                        tableName,
                        it
                    )
                viewPager?.addOnPageChangeListener(this)
                viewPager?.currentItem = position
            }
        }
    )
}
