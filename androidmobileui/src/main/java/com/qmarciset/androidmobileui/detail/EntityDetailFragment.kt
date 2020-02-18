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
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobiledatasync.viewmodel.EntityViewModel
import com.qmarciset.androidmobileui.BaseFragment
import com.qmarciset.androidmobileui.FragmentCommunication
import kotlin.reflect.KClass

class EntityDetailFragment : Fragment(), BaseFragment {

    private var itemId: String = "0"
    private var tableName: String = ""

    // BaseFragment
    override lateinit var delegate: FragmentCommunication

    // ViewModels
    private lateinit var entityViewModel: EntityViewModel<EntityModel>

    companion object {
        fun newInstance(itemId: String, tableName: String) = EntityDetailFragment().apply {
            arguments = Bundle().apply {
                putString("itemId", itemId)
                putString("tableName", tableName)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.getString("itemId")?.let { itemId = it }
        arguments?.getString("tableName")?.let { tableName = it }

        getViewModel()

        val dataBinding: ViewDataBinding = DataBindingUtil.inflate<ViewDataBinding>(
            inflater,
            delegate.fromTableInterface.detailLayoutFromTable(tableName),
            container,
            false
        ).apply {
            delegate.viewDataBindingInterface.setEntityViewModel(this, entityViewModel)
            lifecycleOwner = viewLifecycleOwner
        }
        return dataBinding.root
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

        setupObservers()
    }

    override fun getViewModel() {

        // Get EntityViewModel
        @Suppress("UNCHECKED_CAST")
        val kClazz = EntityViewModel::class as KClass<EntityViewModel<EntityModel>>
        entityViewModel = ViewModelProvider(
            this,
            EntityViewModel.EntityViewModelFactory(
                delegate.appInstance,
                tableName,
                itemId,
                delegate.appDatabaseInterface,
                delegate.apiService,
                delegate.fromTableForViewModel
            )
        )[kClazz.java]
    }

    override fun setupObservers() {
        return
    }
}
