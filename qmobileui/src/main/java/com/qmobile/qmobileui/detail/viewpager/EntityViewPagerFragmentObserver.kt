/*
 * Created by Quentin Marciset on 29/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail.viewpager

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.sqlite.db.SupportSQLiteQuery
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.factory.EntityListViewModelFactory

fun EntityViewPagerFragment.getViewModel() {
    getEntityListViewModel()
}

// fun EntityViewPagerFragment.setupObservers() {
//    observeEntityList()
// }

// Get EntityListViewModel
fun EntityViewPagerFragment.getEntityListViewModel() {
    val clazz = BaseApp.fromTableForViewModel.entityListViewModelClassFromTable(tableName)
    entityListViewModel = activity?.run {
        ViewModelProvider(
            this,
            EntityListViewModelFactory(
                tableName,
                delegate.apiService
            )
        )[clazz]
    } ?: throw IllegalStateException("Invalid Activity")
}

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
