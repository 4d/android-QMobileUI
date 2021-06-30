/*
 * Created by Quentin Marciset on 29/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail.viewpager

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.utils.SqlQueryBuilderUtil

@Suppress("TooManyFunctions")
class EntityViewPagerFragment : Fragment(), BaseFragment, ViewPager.OnPageChangeListener {

    var position: Int = 0
    var tableName: String = ""
    var viewPager: ViewPager? = null
    private var onFragmentCreation = true
    private lateinit var sqlQueryBuilderUtil: SqlQueryBuilderUtil

    private lateinit var actionPrevious: MenuItem
    private lateinit var actionNext: MenuItem

    // BaseFragment
    override lateinit var delegate: FragmentCommunication

    // ViewModel
    lateinit var entityListViewModel: EntityListViewModel<EntityModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onFragmentCreation = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewPager = inflater.inflate(R.layout.fragment_pager, container, false) as ViewPager
        if (onFragmentCreation)
            arguments?.getInt("position")?.let { position = it }
        arguments?.getString("tableName")?.let { tableName = it }

        sqlQueryBuilderUtil = SqlQueryBuilderUtil(tableName)

        this.setHasOptionsMenu(true)

        onFragmentCreation = false
        return viewPager
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
        // Access resources elements
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        entityListViewModel = getEntityListViewModel(activity, tableName, delegate.apiService)
        EntityViewPagerFragmentObserver(this, entityListViewModel).initObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager = null
    }

    override fun onPageScrollStateChanged(state: Int) {
        // Nothing to do
    }

    override fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
    ) {
        // Nothing to do
    }

    override fun onPageSelected(position: Int) {
        this@EntityViewPagerFragment.position = position

        handleActionPreviousEnability(position)
        handleActionNextEnability(position)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_viewpager, menu)
        actionPrevious = menu.findItem(R.id.action_previous)
        actionNext = menu.findItem(R.id.action_next)

        handleActionPreviousEnability(position)
        handleActionNextEnability(position)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val currPosition = viewPager?.currentItem
        currPosition?.let {
            when (item.itemId) {
                R.id.action_previous -> {
                    viewPager?.setCurrentItem(currPosition - 1, true)
                }
                R.id.action_next -> {
                    viewPager?.setCurrentItem(currPosition + 1, true)
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
        return false
    }

    private fun handleActionPreviousEnability(newPos: Int) {
        if (this::actionPrevious.isInitialized) {
            actionPrevious.isEnabled = newPos > 0
        }
    }

    private fun handleActionNextEnability(newPos: Int) {
        if (this::actionNext.isInitialized) {
            viewPager?.adapter?.count?.let { count ->
                actionNext.isEnabled = newPos < count - 1
            }
        }
    }
}
