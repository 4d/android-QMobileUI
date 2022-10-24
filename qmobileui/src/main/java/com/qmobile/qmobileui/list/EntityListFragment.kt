/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ListAdapter
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
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
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.utils.LayoutType
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionNavigable
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.sort.Sort
import com.qmobile.qmobileui.action.utils.ActionHelper
import com.qmobile.qmobileui.databinding.FragmentListBinding
import com.qmobile.qmobileui.list.swipe.ItemActionButton
import com.qmobile.qmobileui.list.swipe.SwipeToActionCallback
import com.qmobile.qmobileui.ui.BounceEdgeEffectFactory
import com.qmobile.qmobileui.ui.GridDividerDecoration
import com.qmobile.qmobileui.ui.setFadeThroughExitTransition
import com.qmobile.qmobileui.ui.setSharedAxisXEnterTransition
import com.qmobile.qmobileui.ui.setSharedAxisXExitTransition
import com.qmobile.qmobileui.ui.setSharedAxisZEnterTransition
import com.qmobile.qmobileui.ui.setupToolbarTitle
import com.qmobile.qmobileui.utils.FormQueryBuilder
import com.qmobile.qmobileui.utils.hideKeyboard

open class EntityListFragment : BaseFragment(), ActionNavigable, MenuProvider {

    companion object {
        private const val CURRENT_SEARCH_QUERY_KEY = "currentSearchQuery_key"
        private const val MAX_ACTIONS_THRESHOLD = 2
        private const val MAX_ACTIONS_VISIBLE = 3
    }

    // views
    private var _binding: FragmentListBinding? = null
    val binding get() = _binding!!
    internal lateinit var adapter: EntityListAdapter
    private lateinit var currentRecordActionsListAdapter: ListAdapter
    private lateinit var entityListViewModel: EntityListViewModel<EntityModel>

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
    private var hasTableActions = false
    private var hasCurrentRecordActions = false
    private var isSwipable = false
    private var searchPattern = "" // search area
    private var relation: Relation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(CURRENT_SEARCH_QUERY_KEY, "")?.let { searchPattern = it }

        setSharedAxisXEnterTransition()

        // Base entity list fragment
        arguments?.getString("tableName")?.let {
            tableName = it
            navbarTitle = it
        }
        // Entity list fragment from relation
        arguments?.getString("relationName")?.let { relationName ->
            if (relationName.isNotEmpty()) {
                var parentTableName = ""
                arguments?.getString("parentTableName")?.let { parentTableName = it }
                relation = RelationHelper.getRelation(parentTableName, relationName)
                tableName = relation?.dest ?: tableName
                arguments?.getString("parentItemId")?.let { parentItemId = it }
                arguments?.getString("path")?.let { path = it }
                arguments?.getString("navbarTitle")?.let { navbarTitle = it }
                setSharedAxisZEnterTransition()
            }
        }
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
            initMenuProvider()
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
                setSharedAxisXExitTransition()
                BaseApp.genericNavigationResolver.navigateFromListToViewPager(
                    viewDataBinding = dataBinding,
                    key = key,
                    query = searchPattern,
                    sourceTable = relation?.source ?: tableName,
                    relationName = relation?.name ?: "",
                    parentItemId = parentItemId,
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

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        setupActionsMenuIfNeeded(menu)
        setupSearchMenuIfNeeded(menu, menuInflater)
        setSearchQuery()
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
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

    private fun setupActionsMenuIfNeeded(menu: Menu) {
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

        if (hasTableActions) {
            actionActivity.setupActionsMenu(menu, actionsForMenu, this) { onSortAction ->
                Sort.saveSortChoice(tableName, onSortAction.sortFields)
                setSearchQuery()
            }
        }
    }

    private fun setupSearchMenuIfNeeded(menu: Menu, inflater: MenuInflater) {
        if (hasSearch) {
            setupSearchView(menu, inflater)
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

    override fun navigateToActionForm(action: Action, itemId: String?) {
        setFadeThroughExitTransition()
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
            setFadeThroughExitTransition()
            BaseApp.genericNavigationResolver.navigateToPendingTasks(
                fragmentActivity = it,
                tableName = relation?.source ?: tableName,
                currentItemId = ""
            )
        }
    }
}
