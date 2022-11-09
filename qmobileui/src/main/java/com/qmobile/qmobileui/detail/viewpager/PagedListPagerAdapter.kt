/*
 * Created by qmarciset on 27/10/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail.viewpager

import androidx.fragment.app.Fragment
import androidx.paging.PagedList
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.qmobile.qmobiledatastore.data.RoomEntity
import timber.log.Timber
import java.lang.IndexOutOfBoundsException

abstract class PagedListPagerAdapter<T : RoomEntity>(fragment: Fragment) :
    FragmentStateAdapter(fragment) {

    private var pagedList: PagedList<T>? = null
    private var callback = PagerCallback(fragment)

    override fun getItemCount(): Int = pagedList?.size ?: 0

    abstract fun createItem(position: Int): Fragment

    abstract var isSmoothScroll: Boolean

    override fun createFragment(position: Int): Fragment {
        pagedList?.loadAround(position)
        return createItem(position)
    }

    @Suppress("TooGenericExceptionCaught")
    fun getValue(position: Int): T? {
        // Exception can occur when we go from an Intent (like phone call), pressing back to
        // activity will get an empty pagedList yet throwing an exception with get(0)
        return try {
            pagedList?.get(position)
        } catch (e: IndexOutOfBoundsException) {
            Timber.d(e.message.orEmpty())
            null
        }
    }

    fun submitList(pagedList: PagedList<T>?) {
        this.pagedList?.removeWeakCallback(callback)
        this.pagedList = pagedList
        this.pagedList?.addWeakCallback(null, callback)
        notifyDataSetChanged()
    }

    private inner class PagerCallback(private val fragment: Fragment) : PagedList.Callback() {
        override fun onChanged(position: Int, count: Int) {
            notifyDataSetChanged()
            (fragment as? EntityViewPagerFragment)?.apply {
                if (fragment.position < position + count && position < fragment.position) {
                    fragment.viewPager?.adapter = fragment.adapter
                    fragment.viewPager?.setCurrentItem(fragment.position, false)
                }
            }
        }
        override fun onInserted(position: Int, count: Int) {
            notifyDataSetChanged()
        }
        override fun onRemoved(position: Int, count: Int) {
            notifyDataSetChanged()
        }
    }
}
