/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.gson.Gson
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.EntityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityViewModel
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.model.QMobileUiConstants
import com.qmobile.qmobileui.utils.layoutFromTable
import timber.log.Timber
import java.util.Locale

class EntityDetailFragment : Fragment(), BaseFragment {

    var itemId: String = "0"
    var tableName: String = ""

    // BaseFragment
    override lateinit var delegate: FragmentCommunication

    // ViewModels
    lateinit var entityViewModel: EntityViewModel<EntityModel>

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

        getViewModels()
        observe()

        val dataBinding: ViewDataBinding = DataBindingUtil.inflate<ViewDataBinding>(
            inflater,
            layoutFromTable(
                inflater.context,
                "${QMobileUiConstants.Prefix.FRAGMENT_DETAIL_PREFIX}$tableName".toLowerCase(Locale.getDefault())
            ),
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

    override fun getViewModels() {
        entityViewModel = getEntityViewModel(activity, delegate.apiService, tableName, itemId)
    }

    // Observe entity list
    override fun observe() {
        entityViewModel.entity.observe(
            viewLifecycleOwner,
            Observer { entity ->
                Timber.d("Observed entity from Room, json = ${Gson().toJson(entity)}")

                val relationKeysMap = entityViewModel.getManyToOneRelationKeysFromEntity(entity)
                for ((relationName, liveDateListRoomRelation) in relationKeysMap) {
                    liveDateListRoomRelation.observe(
                        viewLifecycleOwner,
                        Observer { roomRelation ->
                            roomRelation?.let {
                                entityViewModel.setRelationToLayout(relationName, roomRelation)
                            }
                        }
                    )
                }
            }
        )
    }
}
