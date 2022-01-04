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
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.viewmodel.EntityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityViewModel
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.Action
import com.qmobile.qmobileui.action.ActionHelper
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.ui.checkIfChildIsWebView
import com.qmobile.qmobileui.utils.ResourcesHelper
import com.qmobile.qmobileui.webview.MyWebViewClient

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
        view.checkIfChildIsWebView()?.let { foundWebView ->
            webView = foundWebView
            webView.webViewClient = MyWebViewClient()
        }
        setHasOptionsMenu(::webView.isInitialized || hasActions())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_webview, menu)
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
            val length = actionsJsonObject.getJSONArray(tableName).length()
            for (i in 0 until length) {
                val jsonObject = actionsJsonObject.getSafeArray(tableName)?.getSafeObject(i)
                jsonObject?.let {
                    actions.add(ActionHelper.createActionFromJsonObject(it))
                }
            }

            delegate.setupActionsMenu(menu, actions) { action ->
                if (action.parameters.length() > 0) {
                    BaseApp.genericNavigationResolver.navigateToActionForm(
                        binding,
                        destinationTable = tableName
                    )
                    delegate.setSelectAction(action)
                    delegate.setSelectedEntity(entityViewModel.entity.value)
                } else {
                    delegate.checkNetwork(object : NetworkChecker {
                        override fun onServerAccessible() {
                            entityViewModel.sendAction(
                                action.name,
                                ActionHelper.getActionContent(tableName, itemId)
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
    }

    private fun forceSyncData() {
        delegate.requestDataSync(tableName)
    }
}
