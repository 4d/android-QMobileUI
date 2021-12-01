/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.qmobile.qmobileapi.model.action.ActionContent
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.viewmodel.EntityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityViewModel
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.actions.Action
import com.qmobile.qmobileui.actions.addActions
import com.qmobile.qmobileui.ui.NetworkChecker
import com.qmobile.qmobileui.utils.ResourcesHelper

open class EntityDetailFragment : Fragment(), BaseFragment {

    private var itemId: String = "0"
    private lateinit var entityViewModel: EntityViewModel<EntityModel>
    private var _binding: ViewDataBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    val binding get() = _binding!!
    var tableName: String = ""
    private var actionsJsonObject = BaseApp.runtimeDataHolder.currentRecordActions

    private lateinit var webView: WebView

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
            ResourcesHelper.detailLayoutFromTable(inflater.context, tableName),
            container,
            false
        ).apply {
            BaseApp.genericTableFragmentHelper.setEntityViewModel(this, entityViewModel)
            lifecycleOwner = viewLifecycleOwner
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkIfChildIsWebView(view)?.let { foundWebView ->
            webView = foundWebView
        }
        setHasOptionsMenu(::webView.isInitialized || hasActions())
    }

    @Suppress("ReturnCount")
    private fun checkIfChildIsWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view as? ViewGroup != null) {
            (view as? ViewGroup)?.children?.forEach { child ->
                if (child as? ViewGroup != null) {
                    return checkIfChildIsWebView(child)
                }
                if (child is WebView) return child
            }
        }
        return null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        setupActionsMenuIfNeeded(menu)
        setupWebViewMenuIfNeeded(menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun setupWebViewMenuIfNeeded(menu: Menu) {
        menu.findItem(R.id.action_web_view_share).apply {
            isVisible = ::webView.isInitialized
            if (::webView.isInitialized) {
                setOnMenuItemClickListener {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "This is my text to send.")
                        type = "text/plain"
                    }

//                    val shareIntent = Intent.createChooser(sendIntent, "Share using")
                    startActivity(sendIntent)

                    true
                }
            }
        }
        menu.findItem(R.id.action_web_view_refresh).apply {
            isVisible = ::webView.isInitialized
            if (::webView.isInitialized) {
                setOnMenuItemClickListener {
                    webView.reload()
                    true
                }
            }
        }
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
            actions.addActions(actionsJsonObject, tableName)

            delegate.setupActionsMenu(menu, actions) { name ->
                delegate.checkNetwork(object : NetworkChecker {
                    override fun onServerAccessible() {
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

                    override fun onServerInaccessible() {
                        entityViewModel.toastMessage.showMessage(
                            context?.getString(R.string.action_send_server_not_accessible),
                            tableName,
                            MessageType.ERROR
                        )
                    }

                    override fun onNoInternet() {
                        entityViewModel.toastMessage.showMessage(
                            context?.getString(R.string.action_send_no_internet),
                            tableName,
                            MessageType.ERROR
                        )
                    }
                })
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
