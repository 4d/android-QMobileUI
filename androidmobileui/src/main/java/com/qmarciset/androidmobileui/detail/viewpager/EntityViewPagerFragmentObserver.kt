/*
 * Created by Quentin Marciset on 29/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.detail.viewpager

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.qmarciset.androidmobiledatasync.app.BaseApp
import com.qmarciset.androidmobiledatasync.viewmodel.factory.EntityListViewModelFactory

fun EntityViewPagerFragment.getViewModel() {
    getEntityListViewModel()
}

fun EntityViewPagerFragment.setupObservers() {
    observeEntityList()
}

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
fun EntityViewPagerFragment.observeEntityList() {
    entityListViewModel.entityList.observe(
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
