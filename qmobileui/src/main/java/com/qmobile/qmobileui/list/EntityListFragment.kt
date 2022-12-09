/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.view.LayoutInflater
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
                val sectionField = BaseApp.genericTableHelper.getSectionFieldForTable(tableName)?.name
                if (!sectionField.isNullOrEmpty()) {
                    val sectionItemDecoration = RecyclerSectionItemDecoration(
                        resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                        true,
                        adapter.getSectionCallback(sectionField)
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
}
