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
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.EntityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityViewModel
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.Action
import com.qmobile.qmobileui.action.ActionHelper
import com.qmobile.qmobileui.action.ActionNavigable
import com.qmobile.qmobileui.ui.checkIfChildIsWebView
import com.qmobile.qmobileui.utils.ResourcesHelper
import com.qmobile.qmobileui.webview.MyWebViewClient

open class EntityDetailFragment : Fragment(), BaseFragment, ActionNavigable {

    // views
    private var _binding: ViewDataBinding? = null
    val binding get() = _binding!!

    private lateinit var entityViewModel: EntityViewModel<EntityModel>
    override lateinit var delegate: FragmentCommunication
    override lateinit var actionActivity: ActionActivity
    private var currentRecordActionsJsonObject = BaseApp.runtimeDataHolder.currentRecordActions

    // fragment parameters
    override var tableName: String = ""
    private var itemId: String = "0"
    private var inverseName: String = ""
    private var parentItemId: String = "0"
    private var parentRelationName: String = ""
    private var parentTableName: String? = null
    private var fromRelation = false

    private lateinit var webView: WebView
    private var hasActions = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.getString("itemId")?.let { itemId = it }
        arguments?.getString("tableName")?.let { tableName = it }
        arguments?.getString("destinationTable")?.let {
            if (it.isNotEmpty()) {
                tableName = it
                fromRelation = true
            }
        }

        if (fromRelation) {
            parentTableName =
                BaseApp.genericRelationHelper.getRelatedTableName(tableName, inverseName)
            parentRelationName =
                BaseApp.genericRelationHelper.getInverseRelationName(tableName, inverseName)
        }

        hasActions = currentRecordActionsJsonObject.has(tableName)

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
        setHasOptionsMenu(::webView.isInitialized || hasActions)
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
        if (context is ActionActivity) {
            actionActivity = context
        }
        // Access resources elements
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        EntityDetailFragmentObserver(this, entityViewModel).initObservers()
    }

    private fun setupActionsMenuIfNeeded(menu: Menu) {
        if (hasActions) {
            val currentRecordActions = mutableListOf<Action>()
            ActionHelper.fillActionList(currentRecordActionsJsonObject, tableName, currentRecordActions)
            // actionActivity.setSelectedEntity() is called in observeEntity()
            actionActivity.setupActionsMenu(menu, currentRecordActions, this, true)
        }
    }

    override fun getActionContent(actionId: String?): MutableMap<String, Any> {
        return ActionHelper.getActionContent(
            tableName = tableName,
            selectedActionId = itemId,
            relationName = inverseName,
            parentPrimaryKey = parentItemId,
            parentTableName = parentTableName,
            parentRelationName = parentRelationName
        )
    }

    override fun navigationToActionForm(action: Action) {
        BaseApp.genericNavigationResolver.navigateToActionForm(
            binding,
            destinationTable = tableName,
            navBarTitle = action.getPreferredShortName(),
            inverseName = inverseName,
            parentItemId = parentItemId,
            fromRelation = fromRelation
        )
    }
}
