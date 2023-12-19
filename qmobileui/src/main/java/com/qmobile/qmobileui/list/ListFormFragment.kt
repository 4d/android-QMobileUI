/*
 * Created by qmarciset on 7/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ListAdapter
import androidx.appcompat.widget.SearchView
import androidx.camera.core.ExperimentalGetImage
import androidx.core.view.MenuProvider
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.utils.CustomEntityListFragment
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionNavigable
import com.qmobile.qmobileui.action.barcode.BarcodeScannerFragment.Companion.BARCODE_VALUE_KEY
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.sort.Sort
import com.qmobile.qmobileui.action.utils.ActionHelper
import com.qmobile.qmobileui.action.utils.ActionUIHelper
import com.qmobile.qmobileui.list.swipe.ItemActionButton
import com.qmobile.qmobileui.list.swipe.SwipeToActionCallback
import com.qmobile.qmobileui.ui.SnackbarHelper
import com.qmobile.qmobileui.ui.setFadeThroughExitTransition
import com.qmobile.qmobileui.ui.setSharedAxisXEnterTransition
import com.qmobile.qmobileui.ui.setSharedAxisXExitTransition
import com.qmobile.qmobileui.ui.setSharedAxisZEnterTransition
import com.qmobile.qmobileui.ui.setupToolbarTitle
import com.qmobile.qmobileui.utils.DeepLinkUtil
import com.qmobile.qmobileui.utils.FormQueryBuilder
import com.qmobile.qmobileui.utils.PermissionChecker
import com.qmobile.qmobileui.utils.hideKeyboard
import java.util.concurrent.atomic.AtomicBoolean

abstract class ListFormFragment : BaseFragment(), ActionNavigable, MenuProvider {

    companion object {
        private const val CURRENT_SEARCH_QUERY_KEY = "currentSearchQuery_key"
        private const val MAX_ACTIONS_THRESHOLD = 2
        private const val MAX_ACTIONS_VISIBLE = 3
        private const val BARCODE_FRAGMENT_REQUEST_KEY = "entity_list_fragment_scan_request"
    }

    // views
    private var _binding: ViewDataBinding? = null
    internal val binding get() = _binding!!
    internal lateinit var recyclerView: RecyclerView
    internal lateinit var adapter: EntityListAdapter
    private lateinit var currentRecordActionsListAdapter: ListAdapter
    protected lateinit var entityListViewModel: EntityListViewModel<EntityModel>
    protected var customFragment: CustomEntityListFragment? = null

    override lateinit var actionActivity: ActionActivity
    private var tableActionsJsonObject = BaseApp.runtimeDataHolder.tableActions
    private var currentRecordActionsJsonObject = BaseApp.runtimeDataHolder.currentRecordActions
    private lateinit var formQueryBuilder: FormQueryBuilder

    // fragment parameters
    override var tableName = ""
    private var path = ""
    private var parentItemId = ""

    private val tableActions = mutableListOf<Action>()
    private val currentRecordActions = mutableListOf<Action>()
    private var hasSearch = false
    private var searchableWithBarcode = false
    private var hasCurrentRecordActions = false
    private var isSwipable = false
    private var searchPattern = "" // search area
    private var relation: Relation? = null
    internal var searchingFromBarCode = AtomicBoolean(false)
    internal var allowToOpenFirstRecord = AtomicBoolean(false)

    private val searchListener: SearchView.OnQueryTextListener =
        object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    if (searchPattern != it) {
                        searchPattern = it
                        setSearchQuery()
                    }
                }
                return true
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ActionActivity) {
            actionActivity = context
        }
        // Access resources elements
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(CURRENT_SEARCH_QUERY_KEY, "")?.let { searchPattern = it }

        setSharedAxisXEnterTransition()

        // Base entity list fragment
        arguments?.getString("tableName")?.let { tableName = it }
        arguments?.getString("navbarTitle")?.let { navbarTitle = it }
        // Entity list fragment from relation
        arguments?.getString("relationName")?.let { relationName ->
            if (relationName.isNotEmpty()) {
                var parentTableName = ""
                arguments?.getString("parentTableName")?.let { parentTableName = it }
                relation = RelationHelper.getRelation(parentTableName, relationName)
                tableName = relation?.dest ?: tableName
                arguments?.getString("parentItemId")?.let { parentItemId = it }
                arguments?.getString("path")?.let { path = it }
                setSharedAxisZEnterTransition()
            }
        }
    }

    @ExperimentalGetImage
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        navbarTitle?.let { activity?.setupToolbarTitle(it) }

        setFragmentResultListener(BARCODE_FRAGMENT_REQUEST_KEY) { _, bundle ->
            bundle.getString(BARCODE_VALUE_KEY)?.let { barcodeValue ->
                when {
                    DeepLinkUtil.isAppUrlScheme(requireContext(), barcodeValue) ->
                        handleDeepLinkUrlFromScan(barcodeValue)
                    DeepLinkUtil.isUniversalLink(requireContext(), barcodeValue) ->
                        handleDeepLinkUrlFromScan(barcodeValue)
                    else -> {
                        allowToOpenFirstRecord.set(true)
                        searchingFromBarCode.set(true)
                        searchPattern = barcodeValue
                    }
                }
            }
        }

        entityListViewModel = getEntityListViewModel(activity, tableName, delegate.apiService)

        _binding = inflateBinding(inflater, container)
        recyclerView = binding.root.findViewById(R.id.fragment_list_recycler_view)
        return binding.root
    }

    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        formQueryBuilder = FormQueryBuilder(tableName)

        hasSearch =
            BaseApp.runtimeDataHolder.tableInfo[tableName]?.searchFields?.isNotEmpty() == true
        searchableWithBarcode =
            BaseApp.runtimeDataHolder.tableInfo[tableName]?.searchableWithBarcode ?: false
        hasCurrentRecordActions = currentRecordActionsJsonObject.has(tableName)
        isSwipable = BaseApp.genericTableFragmentHelper.isSwipeAllowed(tableName)

        initMenuProvider()
        initActions()
        initCellSwipe()
        initAdapter()
        initRecyclerView()
        initOnRefreshListener()
        ListFragmentObserver(this, entityListViewModel).initObservers()
        hideKeyboard(activity)
        customFragment =
            BaseApp.genericTableFragmentHelper.getCustomEntityListFragment(tableName, binding)
        customFragment?.onViewCreated(view, savedInstanceState)

        handleDeepLinkIfNeeded()
    }

    override fun onDestroyView() {
        recyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        setupActionsMenu(menu)
        setupSearchMenuIfNeeded(menu, menuInflater)
        setSearchQuery()
    }

    override fun onPrepareMenu(menu: Menu) {
        if (hasSearch) {
            searchView.setOnQueryTextListener(searchListener)

            if (searchPattern.isEmpty()) {
                searchView.onActionViewCollapsed()
            } else {
                searchView.setQuery(searchPattern, true)
                searchView.isIconified = false
                searchPlate.clearFocus()
            }
        }
        super.onPrepareMenu(menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    private fun initAdapter() {
        adapter = EntityListAdapter(
            tableName = tableName,
            lifecycleOwner = viewLifecycleOwner,
            onItemClick = { dataBinding, position ->
                onItemClick(dataBinding, position)
            },
            onItemLongClick = { currentEntity ->
                if (hasCurrentRecordActions && !isSwipable) {
                    showDialog { action ->
                        actionActivity.setCurrentEntityModel(currentEntity)
                        actionActivity.onActionClick(action, this@ListFormFragment)
                    }
                }
            }
        )
    }

    internal fun onItemClick(dataBinding: ViewDataBinding, position: Int) {
        setSharedAxisXExitTransition()
        BaseApp.genericNavigationResolver.navigateFromListToViewPager(
            viewDataBinding = dataBinding,
            sourceTable = relation?.source ?: tableName,
            position = position,
            query = searchPattern,
            relationName = relation?.name ?: "",
            parentItemId = parentItemId,
            path = path,
            navbarTitle = navbarTitle ?: ""
        )
    }

    /**
     * Initialize recyclerView
     */
    abstract fun initRecyclerView()

    /**
     * Initialize Pull to refresh
     */
    abstract fun initOnRefreshListener()

    private fun initActions() {
        tableActions.clear()
        currentRecordActions.clear()
        if (tableActionsJsonObject.has(tableName)) {
            ActionHelper.fillActionList(tableActionsJsonObject, tableName, tableActions)
        }
        if (hasCurrentRecordActions) {
            ActionHelper.fillActionList(
                currentRecordActionsJsonObject,
                tableName,
                currentRecordActions
            )
            currentRecordActionsListAdapter =
                ActionUIHelper.getActionArrayAdapter(
                    requireContext(),
                    currentRecordActions,
                    delegate.isConnected()
                )
        }
    }

    /**
     * Initialize Swipe to actions
     */
    private fun initCellSwipe() {
        if (hasCurrentRecordActions && isSwipable) {
            val itemTouchHelper =
                ItemTouchHelper(object : SwipeToActionCallback(recyclerView) {
                    override fun instantiateUnderlayButton(position: Int): List<ItemActionButton> {
                        val swipeButtons = mutableListOf<ItemActionButton>()
                        for (i in 0 until (currentRecordActions.size)) {
                            val hasMoreButton =
                                (i + 1) > MAX_ACTIONS_THRESHOLD && currentRecordActions.size > MAX_ACTIONS_VISIBLE
                            val action = if (hasMoreButton) {
                                null
                            } else {
                                currentRecordActions[i]
                            }
                            val swipeButton =
                                createSwipeButton(position, action, i) { clickedAction, entity ->
                                    actionActivity.setCurrentEntityModel(entity)
                                    actionActivity.onActionClick(
                                        clickedAction,
                                        this@ListFormFragment
                                    )
                                }
                            swipeButtons.add(swipeButton)
                            if (action == null) break
                        }
                        return swipeButtons
                    }
                })
            itemTouchHelper.attachToRecyclerView(recyclerView)
        }
    }

    private fun showDialog(onClick: (action: Action) -> Unit) {
        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Material3_MaterialAlertDialog
        )
            .setTitle(getString(R.string.action_list_title))
            .setAdapter(currentRecordActionsListAdapter) { _, position ->
                onClick(currentRecordActions[position])
            }
            .show()
    }

    private fun createSwipeButton(
        position: Int,
        action: Action?,
        horizontalIndex: Int,
        onActionClick: (action: Action, roomEntity: RoomEntity) -> Unit
    ): ItemActionButton {
        val isEnabled = action?.let {
            it.isOfflineCompatible() || delegate.isConnected()
        } ?: false
        return ItemActionButton(requireContext(), action, horizontalIndex, isEnabled) {
            adapter.getSelectedItem(position)?.let { entity ->
                if (action == null) { // the case of "..." button
                    showDialog { clickedAction ->
                        onActionClick(clickedAction, entity)
                    }
                } else {
                    onActionClick(action, entity)
                }
            }
        }
    }

    private fun setupActionsMenu(menu: Menu) {
        val parametersToSortWith = BaseApp.sharedPreferencesHolder.parametersToSortWith
        val sortActions = tableActions.filter { it.isSortAction() }
        // If user already applied a sort, we need no more to apply default sort
        if (!parametersToSortWith.has(tableName)) {
            // if 1, no call for sort item here, just save it in shared prefs to be used in sortItems()
            // if more than one, we apply by default the first one
            val fieldsToSortBy: Map<String, String>? = if (sortActions.isEmpty()) {
                Sort.getDefaultSortField(tableName)
            } else {
                sortActions.firstOrNull()?.sortFields
            }
            Sort.saveSortChoice(tableName, fieldsToSortBy)
        }

        val actionsForMenu = if (sortActions.size == 1) {
            tableActions.filter { !it.isSortAction() }
        } else {
            tableActions
        }

        actionActivity.setupActionsMenu(menu, actionsForMenu, this) { onSortAction ->
            Sort.saveSortChoice(tableName, onSortAction.sortFields)
            setSearchQuery()
        }
    }

    private fun setupSearchMenuIfNeeded(menu: Menu, inflater: MenuInflater) {
        if (hasSearch) {
            setupSearchView(menu, inflater, searchableWithBarcode) {
                activity?.apply {
                    (this as? PermissionChecker)?.askPermission(
                        context = this,
                        permission = Manifest.permission.CAMERA,
                        rationale = getString(R.string.permission_rationale_barcode)
                    ) { isGranted ->
                        if (isGranted) {
                            BaseApp.genericNavigationResolver.navigateToActionScanner(
                                this,
                                BARCODE_FRAGMENT_REQUEST_KEY
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CURRENT_SEARCH_QUERY_KEY, searchPattern)
    }

    private fun setSearchQuery() {
        val formQuery = if (relation != null) {
            formQueryBuilder.getRelationQuery(
                parentItemId = parentItemId,
                pattern = searchPattern,
                parentTableName = relation?.source ?: "",
                path = path
            )
        } else {
            formQueryBuilder.getQuery(searchPattern)
        }
        entityListViewModel.setSearchQuery(formQuery)
    }

    override fun getActionContent(actionUUID: String, itemId: String?): MutableMap<String, Any> {
        return ActionHelper.getActionContent(
            tableName = tableName,
            actionUUID = actionUUID,
            itemId = itemId ?: "",
            parentItemId = parentItemId,
            relation = relation
        )
    }

    override fun navigateToActionForm(action: Action, itemId: String?, taskId: String?) {
        setFadeThroughExitTransition()
        BaseApp.genericNavigationResolver.navigateToActionForm(
            viewDataBinding = binding,
            tableName = relation?.source ?: tableName,
            itemId = itemId ?: "",
            relationName = relation?.name ?: "",
            parentItemId = parentItemId,
            pendingTaskId = taskId ?: "",
            actionUUID = action.uuid,
            navbarTitle = action.getPreferredShortName()
        )
    }

    override fun navigateToPendingTasks() {
        activity?.let {
            setFadeThroughExitTransition()
            BaseApp.genericNavigationResolver.navigateToPendingTasks(
                fragmentActivity = it,
                tableName = relation?.source ?: tableName,
                currentItemId = ""
            )
        }
    }

    private fun handleDeepLinkIfNeeded() {
        val newIntent = activity?.intent
        val data: Uri? = newIntent?.data
        if (data != null && data.isHierarchical) {
            val uri = Uri.parse(newIntent.dataString)
            val dataClass = uri.getQueryParameter("dataClass")
            val primaryKey = uri.getQueryParameter("entity.primaryKey")
            val relationName = uri.getQueryParameter("relationName")
            moveForDeepLink(dataClass, primaryKey, relationName)
        } else {
            val dataClass = newIntent?.getStringExtra(DeepLinkUtil.PN_DEEPLINK_DATACLASS)
            val primaryKey = newIntent?.getStringExtra(DeepLinkUtil.PN_DEEPLINK_PRIMARY_KEY)
            moveForDeepLink(dataClass, primaryKey)
        }
    }

    private fun moveForDeepLink(dataClass: String?, primaryKey: String?, relationName: String? = null) {
        if (dataClass == tableName && !primaryKey.isNullOrEmpty()) {
            activity?.apply {
                BaseApp.genericNavigationResolver.navigateToDetailFromDeepLink(
                    fragmentActivity = this,
                    tableName = dataClass,
                    navbarTitle = dataClass,
                    itemId = primaryKey
                )

                if (relationName.isNullOrEmpty()) {
                    activity?.intent = null
                }
            }
        }
    }

    private fun handleDeepLinkUrlFromScan(barcodeValue: String) {
        val uri = Uri.parse(barcodeValue)
        val dataClass = uri.getQueryParameter("dataClass") ?: ""
        when {
            dataClass == "" -> SnackbarHelper.show(activity, getString(R.string.incorrect_deeplink))
            BaseApp.runtimeDataHolder.tableInfo.keys.contains(dataClass) ->
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            else -> SnackbarHelper.show(activity, getString(R.string.table_not_found, dataClass))
        }
    }
}
