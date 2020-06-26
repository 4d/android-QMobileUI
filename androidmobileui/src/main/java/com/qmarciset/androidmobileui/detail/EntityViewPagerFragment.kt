/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.detail

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobiledatasync.viewmodel.EntityListViewModel
import com.qmarciset.androidmobileui.BaseFragment
import com.qmarciset.androidmobileui.FragmentCommunication
import com.qmarciset.androidmobileui.R

class EntityViewPagerFragment : Fragment(), BaseFragment {

    var position: Int = 0
    var tableName: String = ""
    var viewPager: ViewPager? = null

    // BaseFragment
    override lateinit var delegate: FragmentCommunication

    // ViewModel
    lateinit var entityListViewModel: EntityListViewModel<EntityModel>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewPager = inflater.inflate(R.layout.fragment_pager, container, false) as ViewPager
        arguments?.getInt("position")?.let { position = it }
        arguments?.getString("tableName")?.let { tableName = it }
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

        getViewModel()
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager = null
    }
}
