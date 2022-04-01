/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.content.Context
import android.graphics.Color
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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.Action
import com.qmobile.qmobileui.action.ActionHelper
import com.qmobile.qmobileui.action.ActionNavigable
import com.qmobile.qmobileui.binding.getColorFromAttr
import com.qmobile.qmobileui.binding.isDarkColor
import com.qmobile.qmobileui.databinding.FragmentListBinding
import com.qmobile.qmobileui.list.viewholder.SwipeHelper
import com.qmobile.qmobileui.ui.BounceEdgeEffectFactory
import com.qmobile.qmobileui.ui.ItemDecorationSimpleCollection
import com.qmobile.qmobileui.utils.FormQueryBuilder
import com.qmobile.qmobileui.utils.hideKeyboard

open class EntityListFragment : BaseFragment(), ActionNavigable {

    companion object {
        private const val CURRENT_SEARCH_QUERY_KEY = "currentSearchQuery_key"
        private const val MAX_ACTIONS_VISIBLE = 2
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
    private var searchableFields = BaseApp.runtimeDataHolder.searchField
    private var tableActionsJsonObject = BaseApp.runtimeDataHolder.tableActions
    private var currentRecordActionsJsonObject = BaseApp.runtimeDataHolder.currentRecordActions
    private lateinit var formQueryBuilder: FormQueryBuilder

    // fragment parameters
    override var tableName = ""
    private var inverseName = ""
    private var parentItemId = ""
    private var parentRelationName = ""
    private var parentTableName = ""
    private var fromRelation = false

    private val tableActions = mutableListOf<Action>()
    private var currentRecordActions = mutableListOf<Action>()
    private var hasSearch = false
    private var hasTableActions = false
    private var hasCurrentRecordActions = false
    private var isSwipable = false
    private var searchQuery = "" // search area
    private var pathQuery = "" // path query from parent relation
    private var currentQuery = "" // global query
    private var relation: Relation? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Base entity list fragment
        arguments?.getString("tableName")?.let {
            tableName = it
        }
        // Entity list fragment from relation
        arguments?.getString("destinationTable")?.let {
            if (it.isNotEmpty()) {
                tableName = it
                fromRelation = true
            }
        }
        arguments?.getString("currentItemId")?.let { parentItemId = it }
        arguments?.getString("inverseName")?.let { inverseName = it }

        arguments?.getString("parentItemId")?.let { parentItemId = it }
        arguments?.getString("inverseName")?.let { inverseName = it }
        if (fromRelation) {
            RelationHelper.getRelation(tableName, inverseName).dest.let { parentTableName = it }
            RelationHelper.getRelation(tableName, inverseName).inverse.let { parentRelationName = it }
        }

//        arguments?.getString("inverseName")?.let { inverseName = it }

//        if (fromRelation) {
// //            parentTableName = RelationHelper.getRelation(tableName, inverseName).dest
// //            parentRelationName = RelationHelper.getRelation(tableName, inverseName).inverse
//            arguments?.getString("query")?.let { pathQuery = it }
//            arguments?.getString("parentItemId")?.let { parentItemId = it }
//
//        }

        formQueryBuilder = FormQueryBuilder(tableName)

        hasSearch = searchableFields.has(tableName)
        hasTableActions = tableActionsJsonObject.has(tableName)
        hasCurrentRecordActions = currentRecordActionsJsonObject.has(tableName)
        isSwipable = BaseApp.genericTableFragmentHelper.isSwipeAllowed(tableName)

        if (hasSearch || hasTableActions)
            this.setHasOptionsMenu(true)

        entityListViewModel = getEntityListViewModel(activity, tableName, delegate.apiService)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(CURRENT_SEARCH_QUERY_KEY, "")?.let { searchQuery = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initActions()
        initCellSwipe()
        initRecyclerView()
        initOnRefreshListener()
        EntityListFragmentObserver(this, entityListViewModel).initObservers()
        hideKeyboard(activity)
        setSearchQuery()
        BaseApp.genericTableFragmentHelper.getCustomEntityListFragment(tableName, binding)
            ?.onActivityCreated(savedInstanceState)
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
            tableName = tableName, lifecycleOwner = viewLifecycleOwner,
            onItemClick = { dataBinding, key ->
                BaseApp.genericNavigationResolver.navigateFromListToViewPager(
                    viewDataBinding = dataBinding,
                    key = key,
                    query = currentQuery,
                    destinationTable = if (fromRelation) tableName else "",
                    parentItemId = parentItemId,
                    inverseName = inverseName
                )
            },
            onItemLongClick = { currentEntity ->
                if (hasCurrentRecordActions && !isSwipable) {
                    showDialog { action ->
                        actionActivity.setCurrentEntityModel(currentEntity)
                        actionActivity.onActionClick(action, this@EntityListFragment, true)
                    }
                }
            }
        )

        binding.fragmentListRecyclerView.layoutManager =
            when (BaseApp.genericTableFragmentHelper.layoutType(tableName)) {
                "GRID" -> {
                    binding.fragmentListRecyclerView.addItemDecoration(
                        ItemDecorationSimpleCollection(
                            resources.getDimensionPixelSize(
                                R.dimen.simple_collection_spacing
                            ),
                            resources.getInteger(R.integer.simple_collection_columns)
                        )
                    )
                    GridLayoutManager(activity, 2, GridLayoutManager.VERTICAL, false)
                }
                else -> {
                    binding.fragmentListRecyclerView.addItemDecoration(
                        DividerItemDecoration(
                            activity,
                            RecyclerView.VERTICAL
                        )
                    )
                    LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
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
        }
    }

    /**
     * Initialize Swipe to delete
     */
    private fun initCellSwipe() {

        if (hasCurrentRecordActions && isSwipable) {
            currentRecordActionsListAdapter = ActionHelper.getActionArrayAdapter(requireContext(), currentRecordActions)

            val itemTouchHelper =
                ItemTouchHelper(object : SwipeHelper(binding.fragmentListRecyclerView) {
                    override fun instantiateUnderlayButton(position: Int): List<ItemActionButton> {
                        val buttons = mutableListOf<ItemActionButton>()
                        for (i in 0 until (currentRecordActions.size)) {
                            val action = if ((i + 1) > MAX_ACTIONS_VISIBLE) null else currentRecordActions[i]
                            val button = createButton(position, action, i) { clickedAction, entity ->
                                actionActivity.setCurrentEntityModel(entity)
                                actionActivity.onActionClick(clickedAction, this@EntityListFragment, true)
                            }
                            buttons.add(button)
                            if (action == null) break
                        }
                        return buttons
                    }
                })
            itemTouchHelper.attachToRecyclerView(binding.fragmentListRecyclerView)
        }
    }

    private fun showDialog(onClick: (action: Action) -> Unit) {
        MaterialAlertDialogBuilder(requireContext(), R.style.TitleThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle(requireContext().getString(R.string.action_list_title))
            .setAdapter(currentRecordActionsListAdapter) { _, position ->
                onClick(currentRecordActions[position])
            }
            .show()
    }

    private fun createButton(
        position: Int,
        action: Action?,
        horizontalIndex: Int,
        onActionClick: (action: Action, entity: RoomEntity) -> Unit
    ): SwipeHelper.ItemActionButton {
        return SwipeHelper.ItemActionButton(
            requireContext(),
            action,
            horizontalIndex,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
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
        )
    }

    private val searchListener: SearchView.OnQueryTextListener =
        object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    searchQuery = it
                    setSearchQuery()
                }
                return true
            }
        }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (hasSearch) {
            searchView.setOnQueryTextListener(searchListener)

            if (searchQuery.isEmpty()) {
                searchView.onActionViewCollapsed()
            } else {
                searchView.setQuery(searchQuery, true)
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
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun setupActionsMenuIfNeeded(menu: Menu) {
        if (hasTableActions) {
            actionActivity.setupActionsMenu(menu, tableActions, this, false)
        }
    }

    private fun setupSearchMenuIfNeeded(menu: Menu, inflater: MenuInflater) {
        if (hasSearch) {
            inflater.inflate(R.menu.menu_search, menu)
            searchView = menu.findItem(R.id.search).actionView as SearchView
            searchPlate =
                searchView.findViewById(androidx.appcompat.R.id.search_src_text) as EditText
            searchPlate.hint = ""
            searchPlate.setBackgroundResource(R.drawable.searchview_rounded)
            context?.getColorFromAttr(android.R.attr.colorPrimary)?.let {
                if (isDarkColor(it)) {
                    searchPlate.setTextColor(Color.BLACK)
                } else {
                    searchPlate.setTextColor(Color.WHITE)
                }
            }

            searchPlate.setOnEditorActionListener { textView, actionId, keyEvent ->
                if ((keyEvent != null && (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) ||
                    (actionId == EditorInfo.IME_ACTION_DONE)
                ) {
                    hideKeyboard(activity)
                    textView.clearFocus()
                    if (textView.text.isEmpty())
                        searchView.onActionViewCollapsed()
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
        outState.putString(CURRENT_SEARCH_QUERY_KEY, searchQuery)
    }

    private fun setSearchQuery() {
        val formQuery = if (fromRelation) {
            formQueryBuilder.getRelationQuery(
                parentItemId = parentItemId,
                inverseName = inverseName,
                pattern = currentQuery
            )
        } else {
            formQueryBuilder.getQuery(currentQuery)
        }
        entityListViewModel.setSearchQuery(formQuery)
    }


    override fun getActionContent(itemId: String?): MutableMap<String, Any> {
        return ActionHelper.getActionContent(
            tableName = tableName,
            itemId = itemId ?: "",
//            relationName = inverseName,
            parentItemId = parentItemId,
//            parentTableName = parentTableName,
//            parentRelationName = parentRelationName,
            relation = relation
        )
    }

    override fun navigationToActionForm(action: Action, itemId: String?) {
        BaseApp.genericNavigationResolver.navigateToActionForm(
            viewDataBinding = binding,
            tableName = relation?.source ?: tableName,
            itemId = itemId ?: "",
            relationName = relation?.name ?: "",
            parentItemId = parentItemId,
            navbarTitle = action.getPreferredShortName()
        )
    }
}
