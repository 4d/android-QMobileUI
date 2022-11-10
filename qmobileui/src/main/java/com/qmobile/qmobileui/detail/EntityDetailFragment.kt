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
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.EntityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityViewModel
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionNavigable
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.utils.ActionHelper
import com.qmobile.qmobileui.ui.setFadeThroughExitTransition
import com.qmobile.qmobileui.ui.setSharedAxisZEnterTransition
import com.qmobile.qmobileui.ui.setupToolbarTitle
import com.qmobile.qmobileui.utils.ResourcesHelper
import com.qmobile.qmobileui.webview.WebViewHelper.adjustSize
import com.qmobile.qmobileui.webview.WebViewHelper.checkIfChildIsWebView

open class EntityDetailFragment : BaseFragment(), ActionNavigable, MenuProvider {

    // views
    private var _binding: ViewDataBinding? = null
    val binding get() = _binding!!
    private lateinit var webView: WebView
    private lateinit var entityViewModel: EntityViewModel<EntityModel>

    // fragment parameters
    override var tableName = ""
    private var itemId = ""

    override lateinit var actionActivity: ActionActivity
    private var currentRecordActionsJsonObject = BaseApp.runtimeDataHolder.currentRecordActions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("navbarTitle")?.let { navbarTitle = it }
        arguments?.getString("itemId")?.let { itemId = it }
        arguments?.getString("tableName")?.let { tableName = it }

        setSharedAxisZEnterTransition()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.setupToolbarTitle(navbarTitle)
        // Do not give activity as viewModelStoreOwner as it will always give the same detail form fragment
        entityViewModel = getEntityViewModel(this, tableName, itemId, delegate.apiService)

        _binding = DataBindingUtil.inflate<ViewDataBinding>(
            inflater,
            ResourcesHelper.detailLayoutFromTable(inflater.context, tableName),
            container,
            false
        ).apply {
            lifecycleOwner = viewLifecycleOwner
        }

        binding.root.checkIfChildIsWebView()?.let { foundWebView ->
            webView = foundWebView
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }
            }
            webView.adjustSize()
        }
        initMenuProvider()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EntityDetailFragmentObserver(this, entityViewModel).initObservers()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_webview, menu)
        setupActionsMenu(menu)
        setupWebViewMenuIfNeeded(menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    private fun setupActionsMenu(menu: Menu) {
        val currentRecordActions = mutableListOf<Action>()
        ActionHelper.fillActionList(currentRecordActionsJsonObject, tableName, currentRecordActions)
        // actionActivity.setCurrentEntityModel() is called in EntityViewPagerFragment#onPageSelected()
        actionActivity.setupActionsMenu(menu, currentRecordActions, this) {
            // Nothing to do
        }
    }

    private fun setupWebViewMenuIfNeeded(menu: Menu) {
        menu.findItem(R.id.action_web_view_share).apply {
            isVisible = ::webView.isInitialized
            if (::webView.isInitialized) {
                setOnMenuItemClickListener {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "")
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
                    webView.adjustSize()
                    webView.reload()
                    true
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ActionActivity) {
            actionActivity = context
        }
        // Access resources elements
    }

    override fun navigateToPendingTasks() {
        activity?.let {
            setFadeThroughExitTransition()
            BaseApp.genericNavigationResolver.navigateToPendingTasks(
                fragmentActivity = it,
                tableName = tableName,
                currentItemId = itemId
            )
        }
    }

    override fun getActionContent(actionUUID: String, itemId: String?): MutableMap<String, Any> {
        // Event if we are in a N-1 relation, we don't need to provide parent information in the request
        return ActionHelper.getActionContent(
            tableName = tableName,
            actionUUID = actionUUID,
            itemId = itemId ?: ""
        )
    }

    override fun navigateToActionForm(action: Action, itemId: String?) {
        setFadeThroughExitTransition()
        // Even if we are in a N-1 relation, we don't need to provide parent information in the request
        BaseApp.genericNavigationResolver.navigateToActionForm(
            viewDataBinding = binding,
            tableName = tableName,
            itemId = itemId ?: this.itemId,
            relationName = "",
            parentItemId = "",
            pendingTaskId = "",
            actionUUID = action.uuid,
            navbarTitle = action.getPreferredShortName()
        )
    }
}
