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
import androidx.viewpager2.widget.ViewPager2
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.utils.FormQueryBuilder

@Suppress("TooManyFunctions")
class EntityViewPagerFragment : Fragment(), BaseFragment {

    var key: String = ""
    var tableName: String = ""
    private var currentQuery = ""
    var viewPager: ViewPager2? = null
    private lateinit var formQueryBuilder: FormQueryBuilder
    lateinit var adapter: ViewPagerAdapter
//    lateinit var adapter: ViewPagerAdapter2
    private var fromRelation = false
    private var inverseName: String = ""
    private var parentItemId: String = "0"

    private lateinit var actionPrevious: MenuItem
    private lateinit var actionNext: MenuItem

    // BaseFragment
    override lateinit var delegate: FragmentCommunication

    // ViewModel
    lateinit var entityListViewModel: EntityListViewModel<EntityModel>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewPager = inflater.inflate(R.layout.fragment_pager, container, false) as ViewPager2
        arguments?.getString("key")?.let { key = it }
        arguments?.getString("tableName")?.let { tableName = it }
        arguments?.getString("query")?.let { currentQuery = it }

        arguments?.getString("destinationTable")?.let {
            if (it.isNotEmpty()) {
                tableName = it
                fromRelation = true
            }
        }
        arguments?.getString("currentItemId")?.let { parentItemId = it }
        arguments?.getString("inverseName")?.let { inverseName = it }

        formQueryBuilder = FormQueryBuilder(tableName)

        this.setHasOptionsMenu(true)

        entityListViewModel = getEntityListViewModel(activity, tableName, delegate.apiService)
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
        adapter = ViewPagerAdapter(this, tableName)
//        adapter = ViewPagerAdapter2(tableName, this.viewLifecycleOwner)
        viewPager?.adapter = adapter
        viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                arguments?.putString("key", adapter.getValue(position)?.__KEY)
                handleActionPreviousEnability(position)
                handleActionNextEnability(position)
            }
        })

        EntityViewPagerFragmentObserver(this, entityListViewModel, key).initObservers()
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
        }
    }

    private fun handleActionNextEnability(newPos: Int) {
        if (this::actionNext.isInitialized) {
            actionNext.isEnabled = newPos < adapter.itemCount - 1
        }
    }

    private fun setSearchQuery() {
        val formQuery = if (fromRelation) {
            formQueryBuilder.getRelationQuery(
                parentItemId = parentItemId,
                inverseName = inverseName,
                pattern = currentQuery
            )
        } else {
            formQueryBuilder.getQuery(currentQuery)
        }
        entityListViewModel.setSearchQuery(formQuery)
    }
}
