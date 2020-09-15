/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.list

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qmarciset.androidmobileapi.auth.AuthenticationState
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobiledatasync.app.BaseApp
import com.qmarciset.androidmobiledatasync.sync.DataSyncState
import com.qmarciset.androidmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.EntityListViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.LoginViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.delete
import com.qmarciset.androidmobiledatasync.viewmodel.insert
import com.qmarciset.androidmobileui.BaseFragment
import com.qmarciset.androidmobileui.FragmentCommunication
import com.qmarciset.androidmobileui.R
import com.qmarciset.androidmobileui.utils.buildSnackBar
import com.qmarciset.androidmobileui.utils.displaySnackBar
import kotlinx.android.synthetic.main.fragment_list.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class EntityListFragment : Fragment(), BaseFragment {

    var tableName: String = ""
    lateinit var syncDataRequested: AtomicBoolean
    lateinit var adapter: EntityListAdapter

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

        // Every time we land on the fragment, we want refreshed data
        syncDataRequested = AtomicBoolean(true)

        getViewModel()

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
        initSwipeToDeleteAndUndo()
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
        fragment_list_swipe_to_refresh.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.colorPrimary
            )
        )
        fragment_list_swipe_to_refresh.setColorSchemeColors(Color.WHITE)
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
            object : SwipeToDeleteCallback(requireContext()) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val item = adapter.getEntities()[position]
                    entityListViewModel.delete(item)
                    activity?.let {
                        val snackBar =
                            buildSnackBar(it, it.resources.getString(R.string.snackbar_remove))
                        snackBar.setAction(it.resources.getString(R.string.snackbar_undo)) {
                            entityListViewModel.insert(item)
//                            rv_main.scrollToPosition(position)
                        }
                        snackBar.setActionTextColor(
                            ContextCompat.getColor(
                                BaseApp.instance,
                                R.color.colorAccent
                            )
                        )
                        snackBar.show()
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
                    displaySnackBar(it, it.resources.getString(R.string.no_internet))
                }
                Timber.d("No Internet connection, syncDataRequested")
            } else if (loginViewModel.authenticationState.value != AuthenticationState.AUTHENTICATED) {
                activity?.let {
                    displaySnackBar(
                        it,
                        it.resources.getString(R.string.error_occurred_try_again)
                    )
                }
                Timber.d("Not authenticated yet, syncDataRequested = $syncDataRequested")
            }
        }
    }

    /**
     * Checks if environment is ready to perform an action
     */
    fun isReady(): Boolean {
        if (loginViewModel.authenticationState.value == AuthenticationState.INVALID_AUTHENTICATION) {
            // For example server was not responding when trying to auto-login
            delegate.requestAuthentication()
            return false
        }
        return loginViewModel.authenticationState.value == AuthenticationState.AUTHENTICATED &&
            entityListViewModel.dataSynchronized.value == DataSyncState.SYNCHRONIZED &&
            delegate.isConnected()
    }
}
