/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.app.AlertDialog
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
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.qmobile.qmobileapi.model.action.ActionContent
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobiledatastore.data.RoomRelation
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobileui.Action
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.binding.getColorFromAttr
import com.qmobile.qmobileui.binding.isDarkColor
import com.qmobile.qmobileui.databinding.FragmentListBinding
import com.qmobile.qmobileui.list.viewholder.SwipeHelper
import com.qmobile.qmobileui.ui.ItemDecorationSimpleCollection
import com.qmobile.qmobileui.ui.NetworkChecker
import com.qmobile.qmobileui.utils.SqlQueryBuilderUtil
import com.qmobile.qmobileui.utils.hideKeyboard
import java.util.concurrent.atomic.AtomicBoolean
import android.widget.TextView


@Suppress("TooManyFunctions")
open class EntityListFragment : Fragment(), BaseFragment {

    companion object {
        private const val CURRENT_QUERY_KEY = "currentQuery_key"
        private const val MAX_ACTIONS_VISIBLE = 3
    }

    private lateinit var syncDataRequested: AtomicBoolean
    private lateinit var searchView: SearchView
    private lateinit var searchPlate: EditText
    private var searchableFields = BaseApp.runtimeDataHolder.searchField
    private var tableActionsJsonObject = BaseApp.runtimeDataHolder.listActions
    private var currentRecorodActionsJsonObject = BaseApp.runtimeDataHolder.currentRecordActions
    private lateinit var sqlQueryBuilderUtil: SqlQueryBuilderUtil
    private var currentQuery = ""
    internal lateinit var entityListViewModel: EntityListViewModel<EntityModel>
    private var _binding: FragmentListBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    val binding get() = _binding!!
    var tableName: String = ""
    lateinit var adapter: EntityListAdapter

    private val tableActions = mutableListOf<Action>()
    private var currentRecordActions = mutableListOf<Action>()

    // BaseFragment
    override lateinit var delegate: FragmentCommunication

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.getString("tableName")?.let { tableName = it }
        sqlQueryBuilderUtil = SqlQueryBuilderUtil(tableName)

        // Every time we land on the fragment, we want refreshed data // not anymore
        syncDataRequested = AtomicBoolean(true) // unused

        if (hasSearch() || hasTableActions())
            this.setHasOptionsMenu(true)

        entityListViewModel = getEntityListViewModel(activity, tableName, delegate.apiService)

        _binding = FragmentListBinding.inflate(inflater, container, false).apply {
            viewModel = entityListViewModel
            lifecycleOwner = viewLifecycleOwner
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initActions()
        initCellSwipe()
    }

    private fun initActions() {
        tableActions.clear()
        currentRecordActions.clear()
        if (hasTableActions()) {
            val length = tableActionsJsonObject.getJSONArray(tableName).length()
            for (i in 0 until length) {
                val jsonObject = tableActionsJsonObject.getSafeArray(tableName)?.getJSONObject(i)
                tableActions.add(Gson().fromJson(jsonObject.toString(), Action::class.java))
            }
        }
        if (hasCurrentRecordActions()) {
            val length = currentRecorodActionsJsonObject.getSafeArray(tableName)?.length()
            if (length != null) {
                for (i in 0 until (length)) {
                    val jsonObject =
                        currentRecorodActionsJsonObject.getSafeArray(tableName)?.getSafeObject(i)
                    jsonObject?.let {
                        var action = Gson().fromJson(it.toString(), Action::class.java)
                        currentRecordActions.add(action)
                    }
                }
            }
        }
    }

    private fun hasSearch() = searchableFields.has(tableName)
    private fun hasTableActions() = tableActionsJsonObject.has(tableName)
    private fun hasCurrentRecordActions() = currentRecorodActionsJsonObject.has(tableName)

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
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initRecyclerView()
        initOnRefreshListener()
        EntityListFragmentObserver(this, entityListViewModel).initObservers()
        hideKeyboard(activity)
        setSearchQuery()
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
            object : RelationCallback {
                override fun getRelations(entity: EntityModel): Map<String, LiveData<RoomRelation>> =
                    BaseApp.genericTableHelper.getRelationsInfo(tableName, entity)
//                    entityListViewModel.getRelationsInfo(entity)
            },
            { selectedActionId ->
                if (hasCurrentRecordActions()) {
                    showDialog(selectedActionId, currentRecordActions)
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

    /**
     * Initialize Swipe to delete
     */
    private fun initCellSwipe() {
        if (hasCurrentRecordActions()) {
            val itemTouchHelper =
                ItemTouchHelper(object : SwipeHelper(binding.fragmentListRecyclerView) {
                    override fun instantiateUnderlayButton(position: Int): List<ItemActionButton> {
                        var buttons = mutableListOf<ItemActionButton>()
                        for (i in 0 until (currentRecordActions.size)) {
                            if ((i + 1) > MAX_ACTIONS_VISIBLE) {
                                buttons.add(createButton(position, null, i))
                                break
                            }
                            var action = currentRecordActions.get(i)
                            buttons.add(createButton(position, action, i))
                        }
                        return buttons
                    }
                })
            itemTouchHelper.attachToRecyclerView(binding.fragmentListRecyclerView)
        }
    }

    private fun showDialog(selectedActionId: String?, actions: MutableList<Action>) {
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
                //Put the image on the TextView
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
                textView.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0)
                //Add margin between image and text (support various screen densities)
                val paddingDrawable = (5 * resources.displayMetrics.density + 0.5f).toInt()
                textView.compoundDrawablePadding = paddingDrawable
                return itemView
            }
        }
        AlertDialog.Builder(requireContext())
            .setAdapter(adapter) { dialog, position ->
                sendCurrentRecordAction(actions.get(position).name, selectedActionId)
            }.show()
    }

    private fun sendAction(onResult: () -> Unit) {
        delegate.checkNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                onResult()
            }

            override fun onServiceInaccessible() {
                entityListViewModel.toastMessage.showMessage(
                    context?.getString(R.string.action_send_server_not_accessible),
                    tableName,
                    MessageType.ERROR
                )
            }

            override fun onNoInternet() {
                entityListViewModel.toastMessage.showMessage(
                    context?.getString(R.string.action_send_no_internet),
                    tableName,
                    MessageType.ERROR
                )
            }
        })
    }
    private fun sendCurrentRecordAction(actionName: String, selectedActionId: String?) {
        sendAction {
            entityListViewModel.sendAction(
                actionName,
                ActionContent(
                    getActionContext(selectedActionId)
                )
            ) {
                if (it != null) {
                    it.dataSynchro?.let { it1 -> syncDataIfNeeded(it1) }
                }
            }
        }
    }


    private fun getActionContext(selectedActionId: String?): Map<String, Any> {
        val actionContext = mutableMapOf<String, Any>(
            "dataClass" to
                    BaseApp.genericTableHelper.originalTableName(tableName)
        )
        if (selectedActionId != null) {
            actionContext["entity"] = mapOf("primaryKey" to selectedActionId)
        }
        return actionContext
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
                        showDialog(adapter.getSelectedItem(position)?.__KEY, currentRecordActions)
                    } else {
                        sendCurrentRecordAction(
                            action.name,
                            adapter.getSelectedItem(position)?.__KEY
                        )
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        setupActionsMenuIfNeeded(menu)
        setupSearchMenuIfNeeded(menu, inflater)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CURRENT_QUERY_KEY, currentQuery)
    }

    private fun setSearchQuery() {
        if (currentQuery.isEmpty())
            entityListViewModel.setSearchQuery(sqlQueryBuilderUtil.getAll())
        else
            entityListViewModel.setSearchQuery(sqlQueryBuilderUtil.sortQuery(currentQuery))
    }

    private fun setupActionsMenuIfNeeded(menu: Menu) {
        if (hasTableActions()) {
            delegate.setupActionsMenu(menu, tableActions) { name ->
                sendAction {
                    entityListViewModel.sendAction(
                        name,
                        ActionContent(getActionContext(null))
                    ) {
                        if (it != null) {
                            it.dataSynchro?.let { it1 -> syncDataIfNeeded(it1) }
                        }
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
}
