/*
 * Created by qmarciset on 27/10/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail.viewpager

import androidx.fragment.app.Fragment
import androidx.paging.PagedList
import androidx.viewpager2.adapter.FragmentStateAdapter

abstract class PagedListPagerAdapter<T : Any>(fragment: Fragment) :
    FragmentStateAdapter(fragment) {

    private var pagedList: PagedList<T>? = null
    private var callback = PagerCallback()

    override fun getItemCount(): Int = pagedList?.size ?: 0

    abstract fun createItem(position: Int): Fragment

    abstract var isSmoothScroll: Boolean

    override fun createFragment(position: Int): Fragment {
        pagedList?.loadAround(position)
        return createItem(position)
    }

    fun getValue(position: Int): T? {
        return pagedList?.get(position)
    }

    fun submitList(pagedList: PagedList<T>?) {
        this.pagedList?.removeWeakCallback(callback)
        this.pagedList = pagedList
        this.pagedList?.addWeakCallback(null, callback)
        notifyDataSetChanged()
    }

    private inner class PagerCallback : PagedList.Callback() {
        override fun onChanged(position: Int, count: Int) = notifyDataSetChanged()
        override fun onInserted(position: Int, count: Int) = notifyDataSetChanged()
        override fun onRemoved(position: Int, count: Int) = notifyDataSetChanged()
    }
}
