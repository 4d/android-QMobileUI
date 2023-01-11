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
import androidx.activity.addCallback
import androidx.core.view.MenuProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.mainactivity.MainActivity
import com.qmobile.qmobileui.binding.ImageHelper
import com.qmobile.qmobileui.ui.DepthPageTransformer
import com.qmobile.qmobileui.ui.setSharedAxisXEnterTransition
import com.qmobile.qmobileui.ui.setSharedAxisZEnterTransition
import com.qmobile.qmobileui.ui.setupToolbarTitle
import com.qmobile.qmobileui.utils.FormQueryBuilder

class EntityViewPagerFragment : BaseFragment(), MenuProvider {

    // views
    internal var viewPager: ViewPager2? = null
    lateinit var adapter: ViewPagerAdapter
    private lateinit var circularProgressIndicator: CircularProgressIndicator

    private lateinit var actionPrevious: MenuItem
    private lateinit var actionNext: MenuItem
    private lateinit var entityListViewModel: EntityListViewModel<EntityModel>

    // fragment parameters
    private var tableName = ""
    private var searchQueryPattern = ""
    private var parentItemId = ""
    private var path = ""
    private var relation: Relation? = null
    internal var position = 0

    private lateinit var formQueryBuilder: FormQueryBuilder
    private lateinit var actionActivity: ActionActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSharedAxisXEnterTransition()

        arguments?.getString("tableName")?.let { tableName = it }
        arguments?.getString("searchQueryPattern")?.let { searchQueryPattern = it }
        arguments?.getInt("position")?.let { position = it }
        arguments?.getString("navbarTitle")?.let { navbarTitle = it }

        arguments?.getString("relationName")?.let { relationName ->
            if (relationName.isNotEmpty()) {
                relation = RelationHelper.getRelation(tableName, relationName)
                tableName = relation?.dest ?: tableName
                arguments?.getString("parentItemId")?.let { parentItemId = it }
                arguments?.getString("path")?.let { path = it }
                setSharedAxisZEnterTransition()
            }
        }

        activity?.onBackPressedDispatcher?.addCallback {
            (activity as? MainActivity?)?.navController?.navigateUp()
            this.isEnabled = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        navbarTitle?.let { activity?.setupToolbarTitle(it) }
        return inflater.inflate(R.layout.fragment_pager, container, false)
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

        viewPager = view.findViewById(R.id.view_pager) as ViewPager2
        circularProgressIndicator = view.findViewById(R.id.circular_progress)

        formQueryBuilder = FormQueryBuilder(tableName)

        initMenuProvider()

        entityListViewModel = getEntityListViewModel(activity, tableName, delegate.apiService)

        viewPager?.setPageTransformer(DepthPageTransformer())
        adapter = ViewPagerAdapter(this, tableName)
        viewPager?.adapter = adapter
        viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                adapter.getValue(position)?.let { roomEntity ->
                    circularProgressIndicator.visibility = View.GONE
                    viewPager?.visibility = View.VISIBLE
                    actionActivity.setCurrentEntityModel(roomEntity)
                    this@EntityViewPagerFragment.position = position
                }
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

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_viewpager, menu)
        actionPrevious = menu.findItem(R.id.action_previous)
        actionNext = menu.findItem(R.id.action_next)

        viewPager?.currentItem?.let { pos ->
            handleActionPreviousEnability(pos)
            handleActionNextEnability(pos)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        val currPosition = viewPager?.currentItem
        currPosition?.let {
            when (menuItem.itemId) {
                R.id.action_previous -> {
                    viewPager?.setCurrentItem(currPosition - 1, true)
                }
                R.id.action_next -> {
                    viewPager?.setCurrentItem(currPosition + 1, true)
                }
                else -> {}
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
        menuItem.icon?.alpha = if (!menuItem.isEnabled) {
            ImageHelper.ARGB_HALF_VALUE
        } else {
            ImageHelper.ARGB_MAX_VALUE
        }
    }

    private fun setSearchQuery() {
        val formQuery = if (relation != null) {
            formQueryBuilder.getRelationQuery(
                parentItemId = parentItemId,
                pattern = searchQueryPattern,
                parentTableName = relation?.source ?: "",
                path = path
            )
        } else {
            formQueryBuilder.getQuery(searchQueryPattern)
        }
        entityListViewModel.setSearchQuery(formQuery)
    }
}
