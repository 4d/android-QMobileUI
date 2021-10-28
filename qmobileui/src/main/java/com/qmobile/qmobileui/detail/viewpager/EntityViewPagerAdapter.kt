/*
 * Created by Quentin Marciset on 29/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail.viewpager

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.app.BaseApp

/**
 * Not used as we are not using ViewPager with PagedList.
 * Unfortunately could not find a way to use PagingData with ViewPager so far
 */
class EntityViewPagerAdapter(
    fragment: Fragment,
    private val tableName: String,
    val list: List<Any?> // we can have a list here because the pages were already loaded from the previous list form
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return list.size
    }

    override fun createFragment(position: Int): Fragment {
        val entity: EntityModel? = list[position] as EntityModel?
        val itemId = entity?.__KEY ?: "0"

        return BaseApp.genericTableFragmentHelper.getDetailFragment(tableName).apply {
            arguments = Bundle().apply {
                putString("itemId", itemId)
                putString("tableName", tableName)
            }
        }
    }
}
