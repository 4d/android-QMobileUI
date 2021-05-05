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
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomRelation
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.sync.DataSyncStateEnum
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.ui.ItemDecorationSimpleCollection
import com.qmobile.qmobileui.utils.QMobileUiUtil
import com.qmobile.qmobileui.utils.SqlQueryBuilderUtil
import com.qmobile.qmobileui.utils.hideKeyboard
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("TooManyFunctions")
class EntityListFragment : Fragment(), BaseFragment {

    var tableName: String = ""
    lateinit var syncDataRequested: AtomicBoolean
    lateinit var adapter: EntityListAdapter
    private lateinit var searchView: SearchView
    private lateinit var searchPlate: EditText
    private var searchableFields = QMobileUiUtil.appUtilities.searchField
    lateinit var sqlQueryBuilderUtil: SqlQueryBuilderUtil

    companion object {
        private const val CURRENT_QUERY_KEY = "currentQuery_key"
    }

    var currentQuery = ""

    var job: Job? = null

    // BaseFragment
    override lateinit var delegate: FragmentCommunication

    // ViewModels
    lateinit var loginViewModel: LoginViewModel
    lateinit var entityListViewModel: EntityListViewModel<EntityModel>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.getString("tableName")?.let { tableName = it }
        sqlQueryBuilderUtil = SqlQueryBuilderUtil(tableName)

        // Every time we land on the fragment, we want refreshed data // not anymore
        syncDataRequested = AtomicBoolean(true) // unused

        if (hasSearch()) this.setHasOptionsMenu(true)
        getViewModel()

        val dataBinding: ViewDataBinding = DataBindingUtil.inflate<ViewDataBinding>(
            inflater,
            R.layout.fragment_list,
            container,
            false
        ).apply {
            BaseApp.fragmentUtil.setEntityListViewModel(this, entityListViewModel)
            lifecycleOwner = viewLifecycleOwner
        }
        return dataBinding.root
    }

    private fun hasSearch() = searchableFields.has(tableName)

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
        setupObservers()
        observeEntityListDynamicSearch()
        hideKeyboard(activity)
        setSearchQuery()
    }

    override fun onDestroyView() {
        fragment_list_recycler_view.adapter = null
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
                    entityListViewModel.getRelationsInfo(entity)
            }
        )

        fragment_list_recycler_view.layoutManager =
            when (BaseApp.fragmentUtil.layoutType(tableName)) {
                "GRID" -> {
                    fragment_list_recycler_view.addItemDecoration(
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
                    fragment_list_recycler_view.addItemDecoration(
                        DividerItemDecoration(
                            activity,
                            RecyclerView.VERTICAL
                        )
                    )
                    LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
                }
            }

        fragment_list_recycler_view.adapter = adapter
    }

    /**
     * Initialize Pull to refresh
     */
    private fun initOnRefreshListener() {
        fragment_list_swipe_to_refresh.setOnRefreshListener {
            forceSyncData()
            fragment_list_recycler_view.adapter = adapter
            fragment_list_swipe_to_refresh.isRefreshing = false
        }
    }

    /**
     * Initialize Swipe to delete
     */
    /*private fun initSwipeToDeleteAndUndo() {
        val swipeToDeleteCallback: SwipeToDeleteCallback =
            object : SwipeToDeleteCallback(requireContext(), delegate.darkModeEnabled()) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val item = adapter.getEntities()[position]
                    entityListViewModel.delete(item)

                    activity?.let {
                        customSnackBar(
                            it,
                            it.resources.getString(R.string.snackbar_remove),
                            View.OnClickListener {
                                entityListViewModel.insert(item)
                                // rv_main.scrollToPosition(position)
                            }
                        )
                    }
                }
            }

        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(fragment_list_recycler_view)
    }*/

    /**
     * Forces data sync, when user pulls to refresh
     */
    private fun forceSyncData() {
        syncDataRequested.set(false)
        delegate.isConnected { isAccessible ->
            if (isAccessible) {

                if (loginViewModel.authenticationState.value != AuthenticationStateEnum.AUTHENTICATED) {
                    Timber.d("Not authenticated yet, syncDataRequested = $syncDataRequested")
                    delegate.requestAuthentication()
                } else {
                    // AUTHENTICATED
                    if (entityListViewModel.dataSynchronized.value == DataSyncStateEnum.UNSYNCHRONIZED) {
                        delegate.requestDataSync(null)
                    } else {
                        if (entityListViewModel.dataSynchronized.value == DataSyncStateEnum.SYNCHRONIZED) {

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
                    }
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

    // Searchable implementation
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (hasSearch()) {
            inflater.inflate(R.menu.menu_search, menu)
            searchView = menu.findItem(R.id.search).actionView as SearchView
            searchPlate = searchView.findViewById(androidx.appcompat.R.id.search_src_text) as EditText
            searchPlate.hint = ""
            searchPlate.setBackgroundResource(R.drawable.searchview_rounded)
            if (delegate.darkModeEnabled())
                searchPlate.setTextColor(Color.WHITE)
            else
                searchPlate.setTextColor(Color.BLACK)

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
}
