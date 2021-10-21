/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.qmobile.qmobileapi.model.action.ActionContent
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.EntityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityViewModel
import com.qmobile.qmobileui.Action
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.utils.ResourcesHelper

open class EntityDetailFragment : Fragment(), BaseFragment {

    private var itemId: String = "0"
    private lateinit var entityViewModel: EntityViewModel<EntityModel>
    private var _binding: ViewDataBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    val binding get() = _binding!!
    var tableName: String = ""
    private var actionsJsonObject = BaseApp.runtimeDataHolder.currentRecordActions

    // BaseFragment
    override lateinit var delegate: FragmentCommunication

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.getString("itemId")?.let { itemId = it }
        arguments?.getString("tableName")?.let { tableName = it }

        // Do not give activity as viewModelStoreOwner as it will always give the same detail form fragment
        entityViewModel = getEntityViewModel(this, tableName, itemId, delegate.apiService)

        _binding = DataBindingUtil.inflate<ViewDataBinding>(
            inflater,
            ResourcesHelper.layoutFromTable(
                inflater.context,
                "${ResourcesHelper.FRAGMENT_DETAIL_PREFIX}_$tableName".lowercase()
            ),
            container,
            false
        ).apply {
            BaseApp.genericTableFragmentHelper.setEntityViewModel(this, entityViewModel)
            lifecycleOwner = viewLifecycleOwner
        }
        setHasOptionsMenu(hasActions())
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        setupActionsMenuIfNeeded(menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun hasActions() = actionsJsonObject.has(tableName)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
        // Access resources elements
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        EntityDetailFragmentObserver(this, entityViewModel).initObservers()
    }


    private fun setupActionsMenuIfNeeded(menu: Menu) {
        if (hasActions()) {
            val actions = mutableListOf<Action>()
            val length = actionsJsonObject.getJSONArray(tableName).length()
            for (i in 0 until length) {
                val jsonObject = actionsJsonObject.getSafeArray(tableName)?.getSafeObject(i)
                val action = Gson().fromJson(jsonObject.toString(), Action::class.java)
                action?.let {
                    actions.add(it)
                }
            }

            delegate.setupActionsMenu(menu, actions) { name ->
                entityViewModel.sendAction(
                    name,
                    ActionContent(
                        getActionContext()
                    )
                ) {
                    it?.dataSynchro?.let { shouldSyncData ->
                        if (shouldSyncData) {
                            forceSyncData()
                        }
                    }
                }
            }
        }
    }

    private fun getActionContext(): Map<String, Any> {
        return mapOf(
            "dataClass" to BaseApp.genericTableHelper.originalTableName(tableName),
            "entity" to
                    mapOf(
                        "primaryKey" to
                                entityViewModel.entity.value?.__KEY
                    )
        )
    }

    private fun forceSyncData() {
        delegate.requestDataSync(tableName)
    }

}