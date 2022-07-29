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
import androidx.viewpager2.widget.ViewPager2
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.utils.ColorHelper
import com.qmobile.qmobileui.utils.FormQueryBuilder

class EntityViewPagerFragment : BaseFragment() {

    // views
    internal var viewPager: ViewPager2? = null
    lateinit var adapter: ViewPagerAdapter

//    lateinit var adapter: ViewPagerAdapter2
    private lateinit var actionPrevious: MenuItem
    private lateinit var actionNext: MenuItem
    private lateinit var entityListViewModel: EntityListViewModel<EntityModel>

    // fragment parameters
    internal var key = ""
    private var tableName = ""
    private var searchQueryPattern = ""
    private var parentItemId = ""
    private var parentTableName = ""
    private var path = ""
    private var fromRelation = false

    private lateinit var formQueryBuilder: FormQueryBuilder
    private lateinit var actionActivity: ActionActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("key")?.let { key = it }
        arguments?.getString("tableName")?.let { tableName = it }
        arguments?.getString("searchQueryPattern")?.let { searchQueryPattern = it }

        arguments?.getString("destinationTable")?.let {
            if (it.isNotEmpty()) {
                tableName = it
                fromRelation = true
            }
        }
        arguments?.getString("parentItemId")?.let { parentItemId = it }
        arguments?.getString("parentTableName")?.let { parentTableName = it }
        arguments?.getString("path")?.let { path = it }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewPager = inflater.inflate(R.layout.fragment_pager, container, false) as ViewPager2

        formQueryBuilder = FormQueryBuilder(tableName)

        this.setHasOptionsMenu(true)

        entityListViewModel = getEntityListViewModel(activity, tableName, delegate.apiService)
        return viewPager
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ActionActivity) {
            actionActivity = context
        }
        // Access resources elements
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ViewPagerAdapter(this, tableName)
//        adapter = ViewPagerAdapter2(tableName, this.viewLifecycleOwner)
        viewPager?.adapter = adapter
        viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                adapter.getValue(position)?.let { roomEntity ->
                    actionActivity.setCurrentEntityModel(roomEntity)
                    key = (roomEntity.__entity as EntityModel?)?.__KEY ?: ""
                    arguments?.putString("key", key)
                }
//                adapter.getSelectedItem(position)?.let { roomEntity ->
//                    actionActivity.setCurrentEntityModel(roomEntity)
//                    key = (roomEntity.__entity as EntityModel?)?.__KEY ?: ""
//                    arguments?.putString("key", key)
//                }
                handleActionPreviousEnability(position)
                handleActionNextEnability(position)
            }
        })

        EntityViewPagerFragmentObserver(this, entityListViewModel).initObservers()
        setSearchQuery()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_viewpager, menu)
        actionPrevious = menu.findItem(R.id.action_previous)
        actionNext = menu.findItem(R.id.action_next)

        viewPager?.currentItem?.let { pos ->
            handleActionPreviousEnability(pos)
            handleActionNextEnability(pos)
        }
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
            shadowDisabledButton(actionPrevious)
        }
    }

    private fun handleActionNextEnability(newPos: Int) {
        if (this::actionNext.isInitialized) {
            actionNext.isEnabled = newPos < adapter.itemCount - 1
            shadowDisabledButton(actionNext)
        }
    }

    private fun shadowDisabledButton(menuItem: MenuItem) {
        menuItem.icon.alpha = if (!menuItem.isEnabled) {
            ColorHelper.ARGB_HALF_VALUE
        } else {
            ColorHelper.ARGB_MAX_VALUE
        }
    }

    private fun setSearchQuery() {
        val formQuery = if (fromRelation) {
            formQueryBuilder.getRelationQuery(
                parentItemId = parentItemId,
                pattern = searchQueryPattern,
                parentTableName = parentTableName,
                path = path
            )
        } else {
            formQueryBuilder.getQuery(searchQueryPattern)
        }
        entityListViewModel.setSearchQuery(formQuery)
    }
}
