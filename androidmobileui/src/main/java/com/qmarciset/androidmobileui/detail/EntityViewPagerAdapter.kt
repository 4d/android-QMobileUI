/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.detail

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import com.qmarciset.androidmobileapi.model.entity.EntityModel

class EntityViewPagerAdapter(
    fragment: Fragment,
    private val tableName: String,
    val list: List<Any?>
) : FragmentStatePagerAdapter(
    fragment.childFragmentManager,
    BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) {

    override fun getItem(position: Int): Fragment {
        val entity: EntityModel? = list[position] as EntityModel?
        val itemId = entity?.__KEY ?: "0"
        return EntityDetailFragment.newInstance(itemId, tableName)
    }

    override fun getCount(): Int {
        return list.size
    }
}
