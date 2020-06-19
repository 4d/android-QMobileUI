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
import com.qmarciset.androidmobiledatasync.app.BaseApp
import com.qmarciset.androidmobiledatasync.viewmodel.EntityViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.factory.EntityViewModelFactory
import com.qmarciset.androidmobileui.BaseFragment
import com.qmarciset.androidmobileui.FragmentCommunication
import com.qmarciset.androidmobileui.utils.detailLayoutFromTable
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
            detailLayoutFromTable(inflater.context, tableName),
            container,
            false
        ).apply {
            BaseApp.viewDataBindingInterface.setEntityViewModel(this, entityViewModel)
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

    @Suppress("UNCHECKED_CAST")
    override fun getViewModel() {

        // Get EntityViewModel
        val kClazz: KClass<EntityViewModel<EntityModel>> = EntityViewModel::class as KClass<EntityViewModel<EntityModel>>
        entityViewModel = ViewModelProvider(
            this,
            EntityViewModelFactory(
                tableName,
                itemId,
                delegate.apiService
            )
        )[kClazz.java]
    }

    override fun setupObservers() {
        return
    }
}
