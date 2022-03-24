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
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListAdapter
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobiledatastore.dao.ActionInfo
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.dao.ActionTaskDao
import com.qmobile.qmobiledatastore.dao.STATUS
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.Action
import com.qmobile.qmobileui.action.ActionHelper
import com.qmobile.qmobileui.binding.getColorFromAttr
import com.qmobile.qmobileui.binding.isDarkColor
import com.qmobile.qmobileui.databinding.FragmentListBinding
import com.qmobile.qmobileui.list.viewholder.SwipeHelper
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.ui.BounceEdgeEffectFactory
import com.qmobile.qmobileui.ui.ItemDecorationSimpleCollection
import com.qmobile.qmobileui.utils.FormQueryBuilder
import com.qmobile.qmobileui.utils.hideKeyboard
import kotlinx.coroutines.launch
import java.util.Date

open class EntityListFragment : Fragment(), BaseFragment {

    companion object {
        private const val CURRENT_QUERY_KEY = "currentQuery_key"
        private const val MAX_ACTIONS_VISIBLE = 2
        private const val DIALOG_ICON_PADDING = 5
    }

    private lateinit var searchView: SearchView
    private lateinit var searchPlate: EditText
    private var searchableFields = BaseApp.runtimeDataHolder.searchField
    private var tableActionsJsonObject = BaseApp.runtimeDataHolder.listActions
    private var currentRecordActionsJsonObject = BaseApp.runtimeDataHolder.currentRecordActions
    private lateinit var formQueryBuilder: FormQueryBuilder
    private var currentQuery = ""
    private lateinit var entityListViewModel: EntityListViewModel<EntityModel>
    lateinit var adapter: EntityListAdapter
    private var _binding: FragmentListBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    val binding get() = _binding!!
    private var tableName: String = ""
    private var inverseName: String = ""
    private var parentItemId: String = "0"
    private var fromRelation = false
    private var parentRelationName: String = ""
    private var parentTableName: String? = null

    private val tableActions = mutableListOf<Action>()
    private var currentRecordActions = mutableListOf<Action>()
    private lateinit var actionTaskDao: ActionTaskDao

    // BaseFragment
    override lateinit var delegate: FragmentCommunication

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
        if (fromRelation) {
            parentTableName =
                BaseApp.genericRelationHelper.getRelatedTableName(tableName, inverseName)
            parentRelationName =
                BaseApp.genericRelationHelper.getInverseRelationName(tableName, inverseName)
        }

        formQueryBuilder = FormQueryBuilder(tableName)

        if (hasSearch() || hasTableActions())
            this.setHasOptionsMenu(true)

        entityListViewModel = getEntityListViewModel(activity, tableName, delegate.apiService)

        _binding = FragmentListBinding.inflate(inflater, container, false).apply {
            viewModel = entityListViewModel
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    private fun hasSearch() = searchableFields.has(tableName)
    private fun hasTableActions() = tableActionsJsonObject.has(tableName)
    private fun hasCurrentRecordActions() = currentRecordActionsJsonObject.has(tableName)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
        // Access resources elements
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(CURRENT_QUERY_KEY, "")?.let { currentQuery = it }
        actionTaskDao = BaseApp.daoProvider.getActionTaskDao()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
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
            tableName, viewLifecycleOwner,
            { dataBinding, key ->
                BaseApp.genericNavigationResolver.navigateFromListToViewPager(
                    viewDataBinding = dataBinding,
                    key = key,
                    query = currentQuery,
                    destinationTable = if (fromRelation) tableName else "",
                    currentItemId = parentItemId,
                    inverseName = inverseName
                )
            },
            { currentEntity ->

                if ((hasCurrentRecordActions()) &&
                    !BaseApp.genericTableFragmentHelper.isSwipeAllowed(
                            tableName
                        )
                ) {
                    showDialog(currentEntity, currentRecordActions)
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
            forceSyncData()
            binding.fragmentListRecyclerView.adapter = adapter
            binding.fragmentListSwipeToRefresh.isRefreshing = false
        }
    }

    private fun initActions() {
        tableActions.clear()
        currentRecordActions.clear()
        if (hasTableActions()) {
            val length = tableActionsJsonObject.getJSONArray(tableName).length()
            for (i in 0 until length) {
                val jsonObject = tableActionsJsonObject.getSafeArray(tableName)?.getJSONObject(i)
                jsonObject?.let {
                    tableActions.add(ActionHelper.createActionFromJsonObject(it))
                }
            }
        }
        if (hasCurrentRecordActions()) {
            val length = currentRecordActionsJsonObject.getJSONArray(tableName).length()
            for (i in 0 until (length)) {
                val jsonObject =
                    currentRecordActionsJsonObject.getJSONArray(tableName).getJSONObject(i)

                jsonObject?.let {
                    currentRecordActions.add(ActionHelper.createActionFromJsonObject(it))
                }
            }
        }
    }

    /**
     * Initialize Swipe to delete
     */
    private fun initCellSwipe() {

        if (hasCurrentRecordActions() && BaseApp.genericTableFragmentHelper.isSwipeAllowed(
                tableName
            )
        ) {
            val itemTouchHelper =
                ItemTouchHelper(object : SwipeHelper(binding.fragmentListRecyclerView) {
                    override fun instantiateUnderlayButton(position: Int): List<ItemActionButton> {
                        val buttons = mutableListOf<ItemActionButton>()
                        for (i in 0 until (currentRecordActions.size)) {
                            if ((i + 1) > MAX_ACTIONS_VISIBLE) {
                                buttons.add(createButton(position, null, i))
                                break
                            }
                            val action = currentRecordActions[i]
                            buttons.add(createButton(position, action, i))
                        }
                        return buttons
                    }
                })
            itemTouchHelper.attachToRecyclerView(binding.fragmentListRecyclerView)
        }
    }

    private fun showDialog(currentEntity: EntityModel?, actions: MutableList<Action>) {
        val items = actions.map {
            DialogItem(
                it.getPreferredShortName(),
                it.getIconDrawablePath()
            )
        }.toTypedArray()

        val adapter: ListAdapter = object : ArrayAdapter<DialogItem?>(
            requireContext(),
            android.R.layout.select_dialog_item,
            android.R.id.text1,
            items
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val itemView = super.getView(position, convertView, parent)
                val textView = itemView.findViewById<View>(android.R.id.text1) as TextView
                val item = items[position]
                val resId = if (item.icon != null) {
                    resources.getIdentifier(
                        item.icon,
                        "drawable",
                        context.packageName
                    )
                } else {
                    0
                }
                textView.text = item.text
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, resId, 0)
                // Add margin between image and text (support various screen densities)
                val paddingDrawable =
                    (DIALOG_ICON_PADDING * resources.displayMetrics.density).toInt()
                textView.compoundDrawablePadding = paddingDrawable
                return itemView
            }
        }
        val dialogBuilder = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.TitleThemeOverlay_MaterialComponents_MaterialAlertDialog
        )
        dialogBuilder.setAdapter(adapter) { _, position ->
            onCurrentActionClicked(actions[position], currentEntity)
        }

        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private fun onCurrentActionClicked(action: Action, currentEntityModel: EntityModel?) {
        if (action.parameters.length() > 0) {
            BaseApp.genericNavigationResolver.navigateToActionForm(
                binding,
                destinationTable = tableName,
                navBarTitle = action.getPreferredShortName(),
                inverseName = inverseName,
                parentItemId = parentItemId,
                fromRelation = fromRelation
            )
            delegate.setSelectAction(action)
            delegate.setSelectedEntity(currentEntityModel)
        } else {
            sendCurrentRecordAction(action, currentEntityModel?.__KEY, action.id)
        }
    }

    private fun sendAction(action: Action, selectedItemId: String?, actionUUID: String) {
        delegate.checkNetwork(object : NetworkChecker {
            val task = ActionTask(
                status = STATUS.PENDING,
                date = Date(),
                relatedItemId = selectedItemId,
                label = action.getPreferredName(),
                actionInfo = ActionInfo(
                    paramsToSubmit = null,
                    metaDataToSubmit = null,
                    imagesToUpload = null,
                    validationMap = null,
                    allParameters = null,
                    actionName = action.name,
                    tableName = tableName,
                    currentRecordId = delegate.getSelectedEntity()?.__KEY,
                    actionUUID = actionUUID
                )
            )

            override fun onServerAccessible() {
                lifecycleScope.launch {
                    task.id = actionTaskDao.insert(
                        task
                    )
                }
                entityListViewModel.sendAction(
                    action.name,
                    ActionHelper.getActionContent(
                        tableName = tableName,
                        selectedItemId = selectedItemId,
                        relationName = inverseName,
                        parentPrimaryKey = parentItemId,
                        parentTableName = parentTableName,
                        parentRelationName = parentRelationName,
                        actionUUID = action.name
                    )
                ) { actionResponse ->
                    actionResponse?.let {
                        lifecycleScope.launch {
                            val status = if (actionResponse.success) {
                                STATUS.SUCCESS
                            } else {
                                STATUS.ERROR_SERVER
                            }

                            task.status = status
                            task.message = actionResponse.statusText
                            actionTaskDao.insert(
                                task
                            )
                        }
                        actionResponse.dataSynchro?.let { dataSynchro ->
                            syncDataIfNeeded(dataSynchro)
                        }
                    }
                }
            }

            override fun onServerInaccessible() {
                lifecycleScope.launch {

                    actionTaskDao.insert(
                        task
                    )
                }

                if (BaseApp.shouldShowActionError()) {
                    entityListViewModel.toastMessage.showMessage(
                        context?.getString(R.string.action_send_server_not_accessible),
                        tableName,
                        MessageType.NEUTRAL
                    )
                }
            }

            override fun onNoInternet() {
                lifecycleScope.launch {
                    actionTaskDao.insert(
                        task
                    )
                }

                if (BaseApp.shouldShowActionError()) {
                    entityListViewModel.toastMessage.showMessage(
                        context?.getString(R.string.action_send_no_internet),
                        tableName,
                        MessageType.NEUTRAL
                    )
                }
            }
        })
    }

    private fun sendCurrentRecordAction(
        action: Action,
        selectedItemId: String?,
        actionUUID: String
    ) {
        sendAction(action, selectedItemId, actionUUID)
    }

    private fun syncDataIfNeeded(shouldSyncData: Boolean) {
        if (shouldSyncData) {
            forceSyncData()
        }
    }

    private fun createButton(
        position: Int,
        action: Action?,
        horizontalIndex: Int
    ): SwipeHelper.ItemActionButton {

        return SwipeHelper.ItemActionButton(
            requireContext(),
            action,
            horizontalIndex,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    // the case of "..." button
                    if (action == null) {
                        showDialog(adapter.getSelectedItem(position), currentRecordActions)
                    } else {
                        onCurrentActionClicked(action, adapter.getSelectedItem(position))
                    }
                }
            }
        )
    }

    /**
     * Forces data sync, when user pulls to refresh
     */
    private fun forceSyncData() {
        delegate.requestDataSync(tableName)
    }

    private val searchListener: SearchView.OnQueryTextListener =
        object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    currentQuery = it
                    setSearchQuery()
                }
                return true
            }
        }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (hasSearch()) {
            searchView.setOnQueryTextListener(searchListener)

            if (currentQuery.isEmpty()) {
                searchView.onActionViewCollapsed()
            } else {
                searchView.setQuery(currentQuery, true)
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
        if (hasTableActions()) {

            delegate.setupActionsMenu(menu, tableActions) { action, isPendingActionButton ->

                when {
                    isPendingActionButton -> {
                        BaseApp.genericNavigationResolver.navigateToActionTasks(
                            requireActivity(),
                            binding,
                            tableName,
                            null
                        )
                    }
                    action.parameters.length() > 0 -> {

                        BaseApp.genericNavigationResolver.navigateToActionForm(
                            binding,
                            destinationTable = tableName,
                            navBarTitle = action.getPreferredShortName(),
                            inverseName = inverseName,
                            parentItemId = parentItemId,
                            fromRelation = fromRelation
                        )

                        delegate.setSelectAction(action)
                    }
                    else -> {
                        sendAction(action, null, action.id)
                    }
                }
            }
        }
    }

    private fun setupSearchMenuIfNeeded(menu: Menu, inflater: MenuInflater) {
        if (hasSearch()) {
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
        outState.putString(CURRENT_QUERY_KEY, currentQuery)
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
}
