/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.utils.LayoutType
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.databinding.FragmentListBinding
import com.qmobile.qmobileui.ui.BounceEdgeEffectFactory
import com.qmobile.qmobileui.ui.GridDividerDecoration
import com.qmobile.qmobileui.ui.noTabLayoutUI

open class EntityListFragment : ListFormFragment() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return FragmentListBinding.inflate(inflater, container, false).apply {
            viewModel = entityListViewModel
            lifecycleOwner = viewLifecycleOwner
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleDeepLinkIfNeeded()
    }
    override fun initRecyclerView() {
        when (BaseApp.genericTableFragmentHelper.layoutType(tableName)) {
            LayoutType.GRID -> {
                val gridSpanCount = resources.getInteger(R.integer.grid_span_count)
                recyclerView.layoutManager =
                    GridLayoutManager(activity, gridSpanCount, GridLayoutManager.VERTICAL, false)
                val divider = GridDividerDecoration(
                    resources.getDimensionPixelSize(R.dimen.grid_divider_size),
                    ContextCompat.getColor(requireContext(), R.color.divider_color),
                    gridSpanCount
                )
                recyclerView.addItemDecoration(divider)
            }
            else -> {
                recyclerView.layoutManager =
                    LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
                val divider = DividerItemDecoration(activity, LinearLayoutManager.VERTICAL)
                recyclerView.addItemDecoration(divider)

                // Add section itemDecoration if defined for this table
                val sectionFieldName = BaseApp.genericTableHelper.getSectionFieldForTable(tableName)?.name
                if (!sectionFieldName.isNullOrEmpty()) {
                    val sectionItemDecoration = RecyclerSectionItemDecoration(
                        resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                        true,
                        adapter.getSectionCallback(sectionFieldName)
                    )
                    recyclerView.addItemDecoration(sectionItemDecoration)
                    recyclerView.isVerticalScrollBarEnabled = false
                }
            }
        }

        if (!noTabLayoutUI) {
            recyclerView.setPadding(0, 0, 0, getPaddingBottom())
        }
        recyclerView.adapter = adapter
        recyclerView.edgeEffectFactory = BounceEdgeEffectFactory()
    }

    override fun initOnRefreshListener() {
        binding.root.findViewById<SwipeRefreshLayout>(R.id.fragment_list_swipe_to_refresh)?.apply {
            setOnRefreshListener {
                delegate.requestDataSync(tableName)
                recyclerView.adapter = adapter
                this.isRefreshing = false
            }
        }
    }

    private fun handleDeepLinkIfNeeded() {
        val intent = activity?.intent
        val data: Uri? = intent?.data
        if (data != null && data.isHierarchical) {
            val uri = Uri.parse(intent.dataString)

            val dataClass = uri.getQueryParameter("dataClass")
            val primaryKey = uri.getQueryParameter("entity.primaryKey")
            val relationName = uri.getQueryParameter("relationName")

            if ((dataClass == tableName) && (dataClass.isNotEmpty()) && (!primaryKey.isNullOrEmpty())) {
                BaseApp.genericNavigationResolver.navigateToDetailFromDeepLink(
                    fragmentActivity = requireActivity(),
                    tableName = dataClass,
                    navbarTitle = dataClass,
                    itemId = primaryKey
                )

                if (relationName.isNullOrEmpty()) {
                    activity?.intent = null
                }
            }
        }
    }
}
