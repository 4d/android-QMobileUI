/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.model.action.ActionContent
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomRelation
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.sync.DataSyncStateEnum
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getLoginViewModel
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

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
    private var job: Job? = null
    internal lateinit var loginViewModel: LoginViewModel
    internal lateinit var entityListViewModel: EntityListViewModel<EntityModel>
    private var _binding: FragmentListBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    val binding get() = _binding!!
    var tableName: String = ""
    lateinit var adapter: EntityListAdapter

    private var tableActions = mutableListOf<Action>()
    private var currentRecordActions = mutableListOf<Action>()
    private var showMoreActions = mutableListOf<Action>()

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

        if (hasSearch())
            this.setHasOptionsMenu(true)

        entityListViewModel = getEntityListViewModel(activity, tableName, delegate.apiService)
        // We need this ViewModel to know when MainActivity has performed its $authenticate so that
        // we don't trigger the initial sync if we are not authenticated yet
        loginViewModel = getLoginViewModel(activity, delegate.loginApiService)

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
                val jsonObject = tableActionsJsonObject.getJSONArray(tableName).getJSONObject(i)
                tableActions.add(Gson().fromJson(jsonObject.toString(), Action::class.java))
            }
        }
        if (hasCurrentRecordActions()) {
            val length = currentRecorodActionsJsonObject.getJSONArray(tableName).length()
            for (i in 0 until (length)) {
                val jsonObject =
                    currentRecorodActionsJsonObject.getJSONArray(tableName).getJSONObject(i)
                var action = Gson().fromJson(jsonObject.toString(), Action::class.java)
                currentRecordActions.add(action)
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
                                showMoreActions.clear()
                                showMoreActions.addAll(
                                    currentRecordActions.subList(
                                        i,
                                        currentRecordActions.size
                                    )
                                )
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
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        val items = actions.map { it.getPreferredShortName() }
        builder.setItems(
            items.toTypedArray(),
            DialogInterface.OnClickListener { dialog, position ->
                sendAction(actions.get(position).name, selectedActionId)
            }
        )
        builder.show()
    }

    private fun sendAction(actionName: String, selectedActionId: String?) {
        entityListViewModel.sendAction(
            actionName,
            ActionContent(
                mapOf(
                    Pair("dataClass", tableName),
                    Pair("entity", mapOf(Pair("primaryKey", selectedActionId)))
                )
            )
        )
    }

    private fun createButton(
        position: Int,
        action: Action?,
        verticalIndex: Int
    ): SwipeHelper.ItemActionButton {
        val color =
            if (verticalIndex % 2 == 0) android.R.color.holo_blue_dark else android.R.color.holo_blue_light
        return SwipeHelper.ItemActionButton(
            requireContext(),
            action,
            14.0f,
            color,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    // the case of "..." button
                    if (action == null) {
                        showDialog(adapter.getSelectedItem(position)?.__KEY, showMoreActions)
                    } else {
                        sendAction(action.name, adapter.getSelectedItem(position)?.__KEY)
                    }
                }
            }
        )
    }

    /**
     * Forces data sync, when user pulls to refresh
     */
    private fun forceSyncData() {
        syncDataRequested.set(false)

        delegate.checkNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                if (loginViewModel.authenticationState.value != AuthenticationStateEnum.AUTHENTICATED) {
                    Timber.d("Not authenticated yet, syncDataRequested = $syncDataRequested")
                    delegate.requestAuthentication()
                } else {
                    // AUTHENTICATED
                    when (entityListViewModel.dataSynchronized.value) {
                        DataSyncStateEnum.UNSYNCHRONIZED -> delegate.requestDataSync(null)
                        DataSyncStateEnum.SYNCHRONIZED -> {
                            job?.cancel()
                            job = activity?.lifecycleScope?.launch {
                                entityListViewModel.getEntities { shouldSyncData ->
                                    if (shouldSyncData) {
                                        Timber.d("GlobalStamp changed, synchronization is required")
                                        delegate.requestDataSync(tableName)
                                    } else {
                                        Timber.d("GlobalStamp unchanged, no synchronization is required")
                                    }
                                }
                            }
                        }
                        DataSyncStateEnum.SYNCHRONIZING -> Timber.d("Synchronization already in progress")
                    }
                }
            }

            override fun onServiceInaccessible() {
                // Nothing to do
            }

            override fun onNoInternet() {
                // Nothing to do
            }
        })
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
        searchView.setOnQueryTextListener(searchListener)

        if (currentQuery.isEmpty()) {
            searchView.onActionViewCollapsed()
        } else {
            searchView.setQuery(currentQuery, true)
            searchView.isIconified = false
            searchPlate.clearFocus()
        }
        super.onPrepareOptionsMenu(menu)
    }

    data class TestModel(
        val id: Int,
        val description: String
    )

    @SuppressLint("RestrictedApi")
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
        val context = requireParentFragment().requireContext()
        tableActions.forEach { action ->
            val menuBuilder = menu as MenuBuilder
            menuBuilder.setOptionalIconsVisible(true)
            var menuItem = menu.add(
                action.getPreferredName()
            )
            val resId = if (action.icon != null) {
                context.resources.getIdentifier(
                    action.icon,
                    "drawable",
                    context.packageName
                )
            } else {
                0
            }
            menuItem.run {
                setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                setIcon(resId)
                setOnMenuItemClickListener {
                    entityListViewModel.sendAction(
                        action.name,
                        ActionContent(mapOf(Pair("dataClass", tableName)))
                    )
                    entityListViewModel.toastMessage.showMessage(
                        action.getPreferredName(),
                        "",
                        MessageType.NEUTRAL
                    )
                    true
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
