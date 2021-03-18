/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.app.SearchManager
import android.content.Context
import android.content.Context.SEARCH_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.sync.DataSyncStateEnum
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.delete
import com.qmobile.qmobiledatasync.viewmodel.insert
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.ui.CustomSearchView
import com.qmobile.qmobileui.ui.ItemDecorationSimpleCollection
import com.qmobile.qmobileui.ui.SearchListener
import com.qmobile.qmobileui.utils.QMobileUiUtil
import com.qmobile.qmobileui.utils.SearchQueryStateHelper
import com.qmobile.qmobileui.utils.SqlQueryBuilderUtil
import com.qmobile.qmobileui.utils.customSnackBar
import kotlinx.android.synthetic.main.fragment_list.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("TooManyFunctions")
class EntityListFragment : Fragment(), BaseFragment, SearchListener {

    var tableName: String = ""
    lateinit var syncDataRequested: AtomicBoolean
    lateinit var adapter: EntityListAdapter
    private lateinit var searchView: SearchView
    private var searchableFields = QMobileUiUtil.appUtilities.searchField
    private lateinit var sqlQueryBuilderUtil: SqlQueryBuilderUtil

    // BaseFragment
    override lateinit var delegate: FragmentCommunication

    // ViewModels
    lateinit var loginViewModel: LoginViewModel
    lateinit var entityListViewModel: EntityListViewModel<EntityModel>
    lateinit var connectivityViewModel: ConnectivityViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.getString("tableName")?.let { tableName = it }
        sqlQueryBuilderUtil = SqlQueryBuilderUtil(tableName)
        // Every time we land on the fragment, we want refreshed data
        syncDataRequested = AtomicBoolean(true)

        displaySearchBarOnNavigationBar() // set has option Menu
        getViewModel()
        observeEntityListDynamicSearch(sqlQueryBuilderUtil.getAll())
        // observeEntityList()

        val dataBinding: ViewDataBinding = DataBindingUtil.inflate<ViewDataBinding>(
            inflater,
            R.layout.fragment_list,
            container,
            false
        ).apply {
            BaseApp.fragmentUtil.setEntityListViewModel(this, entityListViewModel)
            lifecycleOwner = viewLifecycleOwner
        }
        QMobileUiUtil.setQuery(sqlQueryBuilderUtil.getAll(), false)
        return dataBinding.root
    }

    private fun displaySearchBarOnNavigationBar() {
        if (searchableFields.has(tableName)) this.setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
        // Access resources elements
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initRecyclerView()
        initOnRefreshListener()
        setupObservers()
    }

    override fun onDestroyView() {
        fragment_list_recycler_view.adapter = null
        super.onDestroyView()
    }

    /**
     * Initialize recyclerView
     */
    private fun initRecyclerView() {
        adapter = EntityListAdapter(tableName)

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
    private fun initSwipeToDeleteAndUndo() {
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
    }

    /**
     * Requests data sync to MainActivity if requested
     */
    fun syncData() {
        if (syncDataRequested.compareAndSet(true, false)) {
            entityListViewModel.getEntities { shouldSyncData ->
                if (shouldSyncData) {
                    delegate.requestDataSync(tableName)
                }
            }
        }
    }

    /**
     * Forces data sync, when user pulls to refresh
     */
    private fun forceSyncData() {
        syncDataRequested.set(false)
        if (isReady()) {
            entityListViewModel.getEntities { shouldSyncData ->
                if (shouldSyncData) {
                    Timber.i("GlobalStamp changed, synchronization is required")
                    delegate.requestDataSync(tableName)
                } else {
                    Timber.i("GlobalStamp unchanged, no synchronization is required")
                }
            }
        } else {
            if (!delegate.isConnected()) {
                activity?.let {
                    customSnackBar(it, it.resources.getString(R.string.no_internet), null)
                }
                Timber.d("No Internet connection, syncDataRequested")
            } else if (loginViewModel.authenticationState.value != AuthenticationStateEnum.AUTHENTICATED) {
                /*activity?.let {
                    displaySnackBar(
                        it,
                        it.resources.getString(R.string.error_occurred_try_again)
                    )
                }*/
                Timber.d("Not authenticated yet, syncDataRequested = $syncDataRequested")
            }
        }
    }

    /**
     * Checks if environment is ready to perform an action
     */
    fun isReady(): Boolean {
        if (loginViewModel.authenticationState.value == AuthenticationStateEnum.INVALID_AUTHENTICATION) {
            // For example server was not responding when trying to auto-login
            delegate.requestAuthentication()
            return false
        }
        return loginViewModel.authenticationState.value == AuthenticationStateEnum.AUTHENTICATED &&
            entityListViewModel.dataSynchronized.value == DataSyncStateEnum.SYNCHRONIZED &&
            delegate.isConnected()
    }

    // Custom Search bar Listener
    override fun dataToSearch(data: String) {
        when {
            (data.isEmpty()) -> {
                QMobileUiUtil.setQuery(sqlQueryBuilderUtil.getAll(), false)
                observeEntityListDynamicSearch(sqlQueryBuilderUtil.getAll())
                SearchQueryStateHelper.setString(data)
            }
            else -> {
                observeEntityListDynamicSearch(
                    sqlQueryBuilderUtil.sortQuery(data)
                )
                QMobileUiUtil.setQuery(sqlQueryBuilderUtil.sortQuery(data), true)
                SearchQueryStateHelper.setString(data)
            }
        }
    }

    // Searchable implementation
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search, menu)
        searchView = CustomSearchView(
            context,
            menu.findItem(R.id.search), this
        ).addListener(
            (activity?.getSystemService(SEARCH_SERVICE) as SearchManager).getSearchableInfo(activity?.componentName)
        )
        if (SearchQueryStateHelper.getString() != "empty" && SearchQueryStateHelper.getString()
            .isNotEmpty()
        ) {
            searchView.isIconified = false
            searchView.setQuery(SearchQueryStateHelper.getString(), true)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }
}
