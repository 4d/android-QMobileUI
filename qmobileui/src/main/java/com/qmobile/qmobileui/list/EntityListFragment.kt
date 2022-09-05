/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ListAdapter
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.utils.LayoutType
import com.qmobile.qmobiledatasync.utils.fieldAdjustment
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionNavigable
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.sort.SortFormat
import com.qmobile.qmobileui.action.sort.SortHelper
import com.qmobile.qmobileui.action.utils.ActionHelper
import com.qmobile.qmobileui.databinding.FragmentListBinding
import com.qmobile.qmobileui.list.swipe.ItemActionButton
import com.qmobile.qmobileui.list.swipe.SwipeToActionCallback
import com.qmobile.qmobileui.ui.BounceEdgeEffectFactory
import com.qmobile.qmobileui.ui.GridDividerDecoration
import com.qmobile.qmobileui.ui.setupToolbarTitle
import com.qmobile.qmobileui.utils.FormQueryBuilder
import com.qmobile.qmobileui.utils.hideKeyboard
import org.json.JSONObject

open class EntityListFragment : BaseFragment(), ActionNavigable {

    companion object {
        private const val CURRENT_SEARCH_QUERY_KEY = "currentSearchQuery_key"
        private const val MAX_ACTIONS_THRESHOLD = 2
        private const val MAX_ACTIONS_VISIBLE = 3
    }

    // views
    private var _binding: FragmentListBinding? = null
    val binding get() = _binding!!
    private lateinit var searchView: SearchView
    private lateinit var searchPlate: EditText
    internal lateinit var adapter: EntityListAdapter
    private lateinit var currentRecordActionsListAdapter: ListAdapter
    private lateinit var entityListViewModel: EntityListViewModel<EntityModel>

    override lateinit var actionActivity: ActionActivity
    private var tableActionsJsonObject = BaseApp.runtimeDataHolder.tableActions
    private var currentRecordActionsJsonObject = BaseApp.runtimeDataHolder.currentRecordActions
    private lateinit var formQueryBuilder: FormQueryBuilder
    private var sortFields: LinkedHashMap<String, String>? = null

    // fragment parameters
    override var tableName = ""
    private var parentTableName = ""
    private var path = ""
    private var parentItemId = ""
    private var fromRelation = false

    private val tableActions = mutableListOf<Action>()
    private var currentRecordActions = mutableListOf<Action>()
    private var hasSearch = false
    private var hasTableActions = false
    private var hasCurrentRecordActions = false
    private var isSwipable = false
    private var searchPattern = "" // search area
    private var relation: Relation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(CURRENT_SEARCH_QUERY_KEY, "")?.let { searchPattern = it }

        // Base entity list fragment
        arguments?.getString("tableName")?.let {
            tableName = it
            navbarTitle = it
        }
        // Entity list fragment from relation
        arguments?.getString("destinationTable")?.let { dest ->
            if (dest.isNotEmpty()) {
                tableName = dest
                fromRelation = true
                arguments?.getString("navbarTitle")?.let { navbarTitle = it }
            }
        }

        arguments?.getString("parentItemId")?.let { parentItemId = it }
        arguments?.getString("parentTableName")?.let { parentTableName = it }
        arguments?.getString("path")?.let { path = it }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity?.setupToolbarTitle(navbarTitle)
        formQueryBuilder = FormQueryBuilder(tableName)

        hasSearch = BaseApp.runtimeDataHolder.tableInfo[tableName]?.searchFields?.isNotEmpty() == true
        hasTableActions = tableActionsJsonObject.has(tableName)
        hasCurrentRecordActions = currentRecordActionsJsonObject.has(tableName)
        isSwipable = BaseApp.genericTableFragmentHelper.isSwipeAllowed(tableName)

        entityListViewModel = getEntityListViewModel(activity, tableName, delegate.apiService)
        if (hasSearch || hasTableActions) {
            this.setHasOptionsMenu(true)
        } else {
            setSearchQuery()
        }

        _binding = FragmentListBinding.inflate(inflater, container, false).apply {
            viewModel = entityListViewModel
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ActionActivity) {
            actionActivity = context
        }
        // Access resources elements
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initActions()
        initCellSwipe()
        initRecyclerView()
        initOnRefreshListener()
        EntityListFragmentObserver(this, entityListViewModel).initObservers()
        hideKeyboard(activity)
        BaseApp.genericTableFragmentHelper.getCustomEntityListFragment(tableName, binding)
            ?.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        binding.fragmentListRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    /**
     * Initialize recyclerView
     */
    private fun initRecyclerView() {
        adapter = EntityListAdapter(
            tableName = tableName,
            lifecycleOwner = viewLifecycleOwner,
            onItemClick = { dataBinding, key ->
                BaseApp.genericNavigationResolver.navigateFromListToViewPager(
                    viewDataBinding = dataBinding,
                    key = key,
                    query = searchPattern,
                    sourceTable = if (fromRelation) parentTableName else tableName,
                    destinationTable = if (fromRelation) tableName else "",
                    parentItemId = parentItemId,
                    parentTableName = parentTableName,
                    path = path
                )
            },
            onItemLongClick = { currentEntity ->
                if (hasCurrentRecordActions && !isSwipable) {
                    showDialog { action ->
                        actionActivity.setCurrentEntityModel(currentEntity)
                        actionActivity.onActionClick(action, this@EntityListFragment)
                    }
                }
            }
        )

        when (BaseApp.genericTableFragmentHelper.layoutType(tableName)) {
            LayoutType.GRID -> {
                val gridSpanCount = resources.getInteger(R.integer.grid_span_count)
                binding.fragmentListRecyclerView.layoutManager =
                    GridLayoutManager(activity, gridSpanCount, GridLayoutManager.VERTICAL, false)
                val divider = GridDividerDecoration(
                    resources.getDimensionPixelSize(R.dimen.grid_divider_size),
                    ContextCompat.getColor(requireContext(), R.color.divider_color),
                    gridSpanCount
                )
                binding.fragmentListRecyclerView.addItemDecoration(divider)
            }
            else -> {
                binding.fragmentListRecyclerView.layoutManager =
                    LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
                val divider = DividerItemDecoration(activity, LinearLayoutManager.VERTICAL)
                binding.fragmentListRecyclerView.addItemDecoration(divider)
            }
        }

        binding.fragmentListRecyclerView.adapter = adapter
        binding.fragmentListRecyclerView.edgeEffectFactory = BounceEdgeEffectFactory()
    }

    /**
     * Initialize Pull to refresh
     */
    private fun initOnRefreshListener() {
        binding.fragmentListSwipeToRefresh.setOnRefreshListener {
            delegate.requestDataSync(tableName)
            binding.fragmentListRecyclerView.adapter = adapter
            binding.fragmentListSwipeToRefresh.isRefreshing = false
        }
    }

    private fun initActions() {
        tableActions.clear()
        currentRecordActions.clear()
        if (hasTableActions) {
            ActionHelper.fillActionList(tableActionsJsonObject, tableName, tableActions)
        }
        if (hasCurrentRecordActions) {
            ActionHelper.fillActionList(currentRecordActionsJsonObject, tableName, currentRecordActions)
            currentRecordActionsListAdapter = ActionHelper.getActionArrayAdapter(requireContext(), currentRecordActions)
        }
    }

    /**
     * Initialize Swipe to actions
     */
    private fun initCellSwipe() {
        if (hasCurrentRecordActions && isSwipable) {
            val itemTouchHelper =
                ItemTouchHelper(object : SwipeToActionCallback(binding.fragmentListRecyclerView) {
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
                            val swipeButton = createSwipeButton(position, action, i) { clickedAction, entity ->
                                actionActivity.setCurrentEntityModel(entity)
                                actionActivity.onActionClick(clickedAction, this@EntityListFragment)
                            }
                            swipeButtons.add(swipeButton)
                            if (action == null) break
                        }
                        return swipeButtons
                    }
                })
            itemTouchHelper.attachToRecyclerView(binding.fragmentListRecyclerView)
        }
    }

    private fun showDialog(onClick: (action: Action) -> Unit) {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(requireContext().getString(R.string.action_list_title))
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
        return ItemActionButton(requireContext(), action, horizontalIndex) {
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

    override fun onPrepareOptionsMenu(menu: Menu) {
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
        super.onPrepareOptionsMenu(menu)
    }

    // Searchable implementation
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        setupActionsMenuIfNeeded(menu)
        setupSearchMenuIfNeeded(menu, inflater)
        sortListIfNeeded()
        setSearchQuery()

        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun setupActionsMenuIfNeeded(menu: Menu) {
        val parametersToSortWith = BaseApp.sharedPreferencesHolder.parametersToSortWith
        // If user already applied a sort, we need no more to apply default sort
        if (parametersToSortWith.isEmpty() || !JSONObject(parametersToSortWith).has(tableName)) {
            val sortActions = tableActions.filter { it.isSortAction() }
            when (sortActions.size) {
                0 -> {
                    val defaultFieldToSortWith = BaseApp.runtimeDataHolder.tableInfo[tableName]?.defaultSortField
                    if (defaultFieldToSortWith != null) {
                        saveSortChoice(mapOf(defaultFieldToSortWith.fieldAdjustment() to SortFormat.ASCENDING.value))
                    }
                }

                1 -> {
                    // no call for sort item here, just save it in shared prefs to be used in sortItems() (triggered later)
                    sortActions.firstOrNull()?.sortFields?.let { saveSortChoice(it) }
                }
                else -> {
                    // if more than one action we apply by default the first one
                    val defaultSort = sortActions.firstOrNull()?.sortFields
                    defaultSort?.let { saveSortChoice(it) }
                }
            }
        }

        // if the only action is sort action it should not be displayed
        if (tableActions.size == 1 && tableActions[0].isSortAction()) {
            tableActions.clear()
        }

        if (hasTableActions) {
            actionActivity.setupActionsMenu(menu, tableActions, this) {
                sortItems(it)
            }
        }
    }

    private fun setupSearchMenuIfNeeded(menu: Menu, inflater: MenuInflater) {
        if (hasSearch) {
            inflater.inflate(R.menu.menu_search, menu)
            searchView = menu.findItem(R.id.search).actionView as SearchView
            searchPlate =
                searchView.findViewById(androidx.appcompat.R.id.search_src_text) as EditText
            searchPlate.hint = ""

            searchPlate.setOnEditorActionListener { textView, actionId, keyEvent ->
                if ((keyEvent != null && (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) ||
                    (actionId == EditorInfo.IME_ACTION_DONE)
                ) {
                    hideKeyboard(activity)
                    textView.clearFocus()
                    if (textView.text.isEmpty()) {
                        searchView.onActionViewCollapsed()
                    }
                }
                true
            }

            searchView.setOnCloseListener {
                searchView.onActionViewCollapsed()
                true
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CURRENT_SEARCH_QUERY_KEY, searchPattern)
    }

    private fun setSearchQuery() {
        val formQuery = if (fromRelation) {
            formQueryBuilder.getRelationQuery(
                parentItemId = parentItemId,
                pattern = searchPattern,
                parentTableName = parentTableName,
                path = path,
                sortFields
            )
        } else {
            formQueryBuilder.getQuery(searchPattern, sortFields)
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

    override fun navigateToActionForm(action: Action, itemId: String?) {
        BaseApp.genericNavigationResolver.navigateToActionForm(
            viewDataBinding = binding,
            tableName = relation?.source ?: tableName,
            itemId = itemId ?: "",
            relationName = relation?.name ?: "",
            parentItemId = parentItemId,
            pendingTaskId = "",
            actionUUID = action.uuid,
            navbarTitle = action.getPreferredShortName()
        )
    }

    override fun navigateToPendingTasks() {
        activity?.let {
            BaseApp.genericNavigationResolver.navigateToPendingTasks(
                fragmentActivity = it,
                tableName = relation?.source ?: tableName,
                currentItemId = ""
            )
        }
    }

    override fun navigateToActionWebView(
        path: String,
        actionName: String,
        actionLabel: String?,
        actionShortLabel: String?
    ) {
            BaseApp.genericNavigationResolver.navigateToActionWebView(
                viewDataBinding = binding,
                path = path,
                actionName = actionName,
                actionLabel = actionLabel,
                actionShortLabel = actionShortLabel
            )
    }

    private fun sortItems(action: Action) {
        sortFields = action.sortFields
        setSearchQuery()
        sortFields?.let {
            saveSortChoice(it)
        }
    }

    private fun saveSortChoice(fieldsToSortBy: Map<String, String>) {
        val parametersToSortWith = BaseApp.sharedPreferencesHolder.parametersToSortWith
        BaseApp.sharedPreferencesHolder.parametersToSortWith =
            if (parametersToSortWith.isNotEmpty()) {
                val allTablesJsonObject = JSONObject(parametersToSortWith)
                allTablesJsonObject.put(tableName, JSONObject(fieldsToSortBy).toString())
                allTablesJsonObject.toString()
            } else {
                JSONObject(mapOf(tableName to JSONObject(fieldsToSortBy).toString())).toString()
            }
    }

    // Used to sort items of current table if a sort action is already applied (and persisted in shared prefs)
    private fun sortListIfNeeded() {
        SortHelper.getSortFieldsForTable(tableName)?.let {
            if (it.isNotEmpty()) {
                sortFields = it
            }
        }
    }
}
