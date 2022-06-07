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
import com.qmobile.qmobileui.ui.checkIfChildIsWebView
import com.qmobile.qmobileui.utils.ResourcesHelper
import com.qmobile.qmobileui.webview.MyWebViewClient

open class EntityDetailFragment : BaseFragment(), ActionNavigable {

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
    private var hasActions = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.getString("itemId")?.let { itemId = it }
        arguments?.getString("tableName")?.let { tableName = it }

        // Do not give activity as viewModelStoreOwner as it will always give the same detail form fragment
        entityViewModel = getEntityViewModel(this, tableName, itemId, delegate.apiService)

        hasActions = currentRecordActionsJsonObject.has(tableName)

        _binding = DataBindingUtil.inflate<ViewDataBinding>(
            inflater,
            ResourcesHelper.detailLayoutFromTable(inflater.context, tableName),
            container,
            false
        ).apply {
//            BaseApp.genericTableFragmentHelper.setEntityViewModel(this, entityViewModel)
            lifecycleOwner = viewLifecycleOwner
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EntityDetailFragmentObserver(this, entityViewModel).initObservers()
        view.checkIfChildIsWebView()?.let { foundWebView ->
            webView = foundWebView
            webView.webViewClient = MyWebViewClient()
        }

        setHasOptionsMenu(::webView.isInitialized || hasActions)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_webview, menu)
        setupActionsMenuIfNeeded(menu)
        setupWebViewMenuIfNeeded(menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun setupActionsMenuIfNeeded(menu: Menu) {
        if (hasActions) {
            val currentRecordActions = mutableListOf<Action>()
            ActionHelper.fillActionList(currentRecordActionsJsonObject, tableName, currentRecordActions)
            // actionActivity.setCurrentEntityModel() is called in EntityViewPagerFragment#onPageSelected()
            actionActivity.setupActionsMenu(menu, currentRecordActions, this)
        }
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ActionActivity) {
            actionActivity = context
        }
        // Access resources elements
    }

    override fun navigateToPendingTasks() {
        activity?.let {
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
        // Event if we are in a N-1 relation, we don't need to provide parent information in the request
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
