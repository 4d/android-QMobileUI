/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Context.SEARCH_SERVICE
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.sqlite.db.SupportSQLiteQuery
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.connectivity.NetworkStateEnum
import com.qmobile.qmobileapi.connectivity.sdkNewerThanKitKat
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.sync.DataSyncStateEnum
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.delete
import com.qmobile.qmobiledatasync.viewmodel.factory.getConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getLoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.insert
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.ui.CustomSearchView
import com.qmobile.qmobileui.ui.SearchListener
import com.qmobile.qmobileui.utils.QMobileUiUtil
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
        getViewModels()
        observe()

        val dataBinding: ViewDataBinding = DataBindingUtil.inflate<ViewDataBinding>(
            inflater,
            R.layout.fragment_list,
            container,
            false
        ).apply {
            BaseApp.viewDataBindingInterface.setEntityListViewModel(this, entityListViewModel)
            lifecycleOwner = viewLifecycleOwner
        }

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

    override fun getViewModels() {
        entityListViewModel = getEntityListViewModel(activity, delegate.apiService, tableName)
        getConnectivityViewModel(activity, delegate.connectivityManager)?.let { connectivityViewModel = it }
        loginViewModel = getLoginViewModel(activity, delegate.loginApiService)
    }

    override fun observe() {
        observeEntityListDynamicSearch(sqlQueryBuilderUtil.getAll())
        observeDataSynchronized()
        observeAuthenticationState()
        observeNetworkStatus()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initRecyclerView()
        initOnRefreshListener()
        initSwipeToDeleteAndUndo()
        observe()
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
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        fragment_list_recycler_view.addItemDecoration(
            DividerItemDecoration(
                activity,
                RecyclerView.VERTICAL
            )
        )
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
        if (data.isEmpty()) observeEntityListDynamicSearch(sqlQueryBuilderUtil.getAll())
        else observeEntityListDynamicSearch(
            sqlQueryBuilderUtil.sortQuery(data)
        )
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
        super.onCreateOptionsMenu(menu, inflater)
    }

    // Sql Dynamic Query Support
    fun observeEntityListDynamicSearch(sqLiteQuery: SupportSQLiteQuery) {
        entityListViewModel.getAllDynamicQuery(sqLiteQuery).observe(
            viewLifecycleOwner,
            {
                it.let {
                    adapter.setEntities(it)
                    if (it.isEmpty()) Toast.makeText(this.context, "No data Found", Toast.LENGTH_SHORT).apply {
                        setGravity(Gravity.CENTER, 0, 0)
                        show()
                    }
                }
            }
        )
    }

    // Observe when data are synchronized
    @SuppressLint("BinaryOperationInTimber")
    fun observeDataSynchronized() {
        entityListViewModel.dataSynchronized.observe(
            viewLifecycleOwner,
            Observer { dataSyncState ->
                Timber.i(
                    "[DataSyncState : $dataSyncState, " +
                        "Table : ${entityListViewModel.getAssociatedTableName()}, " +
                        "Instance : $entityListViewModel]"
                )
            }
        )
    }

    // Observe authentication state
    fun observeAuthenticationState() {
        loginViewModel.authenticationState.observe(
            viewLifecycleOwner,
            Observer { authenticationState ->
                when (authenticationState) {
                    AuthenticationStateEnum.AUTHENTICATED -> {
                        if (isReady()) {
                            syncData()
                        } else {
                            syncDataRequested.set(true)
                        }
                    }
                    else -> {
                    }
                }
            }
        )
    }

    // Observe network status
    fun observeNetworkStatus() {
        if (sdkNewerThanKitKat) {
            connectivityViewModel.networkStateMonitor.observe(
                viewLifecycleOwner,
                Observer { networkState ->
                    when (networkState) {
                        NetworkStateEnum.CONNECTED -> {
                            if (isReady()) {
                                syncData()
                            } else {
                                syncDataRequested.set(true)
                            }
                        }
                        else -> {
                        }
                    }
                }
            )
        }
    }
}
