/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.android.material.appbar.AppBarLayout
import com.qmobile.qmobileui.action.actionparameters.ActionParametersFragment
import com.qmobile.qmobileui.action.pendingtasks.TasksFragment
import com.qmobile.qmobileui.detail.EntityDetailFragment
import com.qmobile.qmobileui.list.EntityListFragment

abstract class BaseFragment : Fragment() {

    internal lateinit var delegate: FragmentCommunication

    internal var navbarTitle = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
    }

    protected fun MenuProvider.initMenuProvider() {
        val menuHost: MenuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAppBar()
    }

    private fun setupAppBar() {
        // Mandatory step as the SwipeRefreshLayout will cause issues with AppBarLayout coloring while scrolling
        activity?.findViewById<AppBarLayout>(R.id.appbar)?.let { appBarLayout ->
            when (this) {
                is EntityListFragment -> appBarLayout.liftOnScrollTargetViewId = R.id.fragment_list_recycler_view
                is EntityDetailFragment -> appBarLayout.liftOnScrollTargetViewId = R.id.base_detail_scroll_view
                is ActionParametersFragment -> appBarLayout.liftOnScrollTargetViewId = R.id.parameters_recycler_view
                is TasksFragment -> appBarLayout.liftOnScrollTargetViewId = R.id.task_nested_scroll_view
            }
        }
    }
}
