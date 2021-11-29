/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail

import android.content.Context
import android.os.Bundle

import android.view.MenuItem
import android.view.MenuInflater
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.databinding.FragmentActionParametersBinding
import com.qmobile.qmobileui.action.ActionsParametersListAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.qmobile.qmobileapi.model.action.ActionContent
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobileui.action.ActionHelper
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.ui.NetworkChecker

open class ActionParametersFragment : Fragment(), BaseFragment {

    private var _binding: ViewDataBinding? = null
    internal lateinit var entityListViewModel: EntityListViewModel<EntityModel>
    val binding get() = _binding!!
    lateinit var tableName: String
    override lateinit var delegate: FragmentCommunication
    private val paramsToSubmit = HashMap<String, Any>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        arguments?.getString("tableName")?.let { tableName = it }
        entityListViewModel = getEntityListViewModel(activity, tableName, delegate.apiService)
        _binding = FragmentActionParametersBinding.inflate(
            inflater,
            container,
            false
        ).apply {
            lifecycleOwner = viewLifecycleOwner
            val adapter = ActionsParametersListAdapter(
                requireContext(),
                delegate.getSelectAction().parameters
            ) { name: String, value: Any ->
                paramsToSubmit[name] = value
            }
            val layoutManager =LinearLayoutManager(requireContext())
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter

            val dividerItemDecoration = DividerItemDecoration(
                recyclerView.context,
                layoutManager.orientation
            )
            recyclerView.addItemDecoration(dividerItemDecoration)
        }

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_actions_parameters, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.validate) {
            sendAction(delegate.getSelectAction().name, null)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
    }

    private fun sendAction(actionName: String, selectedActionId: String?) {
        delegate.checkNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                entityListViewModel.sendAction(
                    actionName,
                    ActionContent(
                        ActionHelper.getActionContext(tableName, selectedActionId, paramsToSubmit)
                    )
                ) { actionResponse ->
                    actionResponse?.let {
                        actionResponse.dataSynchro?.let { dataSynchro ->
                            if (dataSynchro) {
                                delegate.requestDataSync(tableName)
                            }
                        }
                    }
                }
            }

            override fun onServerInaccessible() {
                entityListViewModel.toastMessage.showMessage(
                    context?.getString(R.string.action_send_server_not_accessible),
                    tableName,
                    MessageType.ERROR
                )
            }

            override fun onNoInternet() {
                entityListViewModel.toastMessage.showMessage(
                    context?.getString(R.string.action_send_no_internet),
                    tableName,
                    MessageType.ERROR
                )
            }
        })
    }

}