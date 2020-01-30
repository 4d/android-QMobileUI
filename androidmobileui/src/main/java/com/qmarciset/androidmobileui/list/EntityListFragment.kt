package com.qmarciset.androidmobileui.list

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qmarciset.androidmobileapi.auth.AuthenticationState
import com.qmarciset.androidmobileui.BaseFragment
import com.qmarciset.androidmobileui.FragmentCommunication
import com.qmarciset.androidmobileui.R
import com.qmarciset.androidmobileui.connectivity.NetworkState
import com.qmarciset.androidmobileui.utils.buildSnackBar
import com.qmarciset.androidmobileui.utils.displaySnackBar
import com.qmarciset.androidmobileui.utils.fetchResourceString
import com.qmarciset.androidmobileui.viewmodel.ConnectivityViewModel
import com.qmarciset.androidmobileui.viewmodel.EntityListViewModel
import com.qmarciset.androidmobileui.viewmodel.LifecycleViewModel
import com.qmarciset.androidmobileui.viewmodel.LoginViewModel
import kotlinx.android.synthetic.main.fragment_list_stub.*
import timber.log.Timber

class EntityListFragment : Fragment(), BaseFragment {

    private var tableName: String = ""
    private var syncDataRequested = false
    private lateinit var adapter: EntityListAdapter
    private lateinit var delegate: FragmentCommunication
    private lateinit var entityListViewModel: EntityListViewModel<*>
    private lateinit var lifecycleViewModel: LifecycleViewModel
    private lateinit var connectivityViewModel: ConnectivityViewModel
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.getString("tableName")?.let { tableName = it }

        getViewModel()

        val dataBinding: ViewDataBinding = DataBindingUtil.inflate<ViewDataBinding>(
            inflater,
            delegate.fromTableInterface.listLayoutFromTable(tableName),
            container,
            false
        ).apply {
            delegate.viewDataBindingInterface.setEntityListViewModel(this, entityListViewModel)
            lifecycleOwner = viewLifecycleOwner
        }

        return dataBinding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
        // access resources elements
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView()
    }

    override fun onDestroyView() {
        fragment_list_recycler_view.adapter = null
        super.onDestroyView()
    }

    override fun getViewModel() {
        val kClazz = delegate.fromTableInterface.entityListViewModelClassFromTable(tableName)
        entityListViewModel = activity?.run {
            ViewModelProvider(
                this,
                EntityListViewModel.EntityListViewModelFactory(
                    delegate.appInstance,
                    delegate.appDatabaseInterface,
                    delegate.apiService,
                    tableName,
                    delegate.fromTableInterface
                )
            )[kClazz.java]
        } ?: throw IllegalStateException("Invalid Activity")

        lifecycleViewModel = activity?.run {
            ViewModelProvider(
                this,
                LifecycleViewModel.LifecycleViewModelFactory(delegate.appInstance)
            )[LifecycleViewModel::class.java]
        } ?: throw IllegalStateException("Invalid Activity")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectivityViewModel = activity?.run {
                ViewModelProvider(
                    this,
                    ConnectivityViewModel.ConnectivityViewModelFactory(
                        delegate.appInstance,
                        delegate.connectivityManager
                    )
                )[ConnectivityViewModel::class.java]
            } ?: throw IllegalStateException("Invalid Activity")
        }

        // We need this ViewModel to know when MainActivity has performed its $authenticate so that
        // we don't trigger the initial sync if we are not authenticated yet
        loginViewModel = activity?.run {
            ViewModelProvider(
                this,
                LoginViewModel.LoginViewModelFactory(delegate.appInstance, delegate.loginApiService)
            )[LoginViewModel::class.java]
        } ?: throw IllegalStateException("Invalid Activity")
    }

    override fun setupObservers() {
        entityListViewModel.entityList.observe(viewLifecycleOwner, Observer { entities ->
            entities?.let {
                adapter.setEntities(it)
            }
        })

        entityListViewModel.toastMessage.observe(viewLifecycleOwner, Observer { message ->
            val toastMessage = context?.fetchResourceString(message) ?: ""
            if (toastMessage.isNotEmpty()) {
                activity?.let {
                    Toast.makeText(it, message, Toast.LENGTH_LONG).show()
                }
                // To avoid the error toast to be displayed without performing a refresh again
                entityListViewModel.toastMessage.postValue("")
            }
        })

        lifecycleViewModel.entersForeground.observe(
            viewLifecycleOwner,
            Observer { entersForeground ->
                if (entersForeground) {
                    if (delegate.isConnected()) {
                        if (loginViewModel.authenticationState.value == AuthenticationState.AUTHENTICATED) {
                            entityListViewModel.getData()
                        } else {
                            syncDataRequested = true
                            displaySnackBar(activity, "An error occurred, please try again later")
                            Timber.d("Not authenticated yet, refreshDataRequested = $syncDataRequested")
                        }
                    } else {
                        syncDataRequested = true
                        displaySnackBar(activity, "No internet connection")
                        Timber.d("No internet connection, refreshDataRequested = $syncDataRequested")
                    }
                }
            })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectivityViewModel.networkStateMonitor.observe(
                viewLifecycleOwner,
                Observer { networkState ->
                    Timber.d("<NetworkState changed -> $networkState>")
                    when (networkState) {
                        NetworkState.CONNECTED -> {
                            if (syncDataRequested &&
                                loginViewModel.authenticationState.value == AuthenticationState.AUTHENTICATED
                            ) {
                                syncDataRequested = false
                                entityListViewModel.getData()
                            }
                        }
                        NetworkState.CONNECTION_LOST -> {
                        }
                        NetworkState.DISCONNECTED -> {
                        }
                        NetworkState.CONNECTING -> {
                        }
                        else -> {
                        }
                    }
                })
        }

        loginViewModel.authenticationState.observe(
            this,
            Observer { authenticationState ->
                Timber.d("<AuthenticationState changed -> $authenticationState>")
                when (authenticationState) {
                    AuthenticationState.AUTHENTICATED -> {
                        if (syncDataRequested && delegate.isConnected()) {
                            syncDataRequested = false
                            entityListViewModel.getData()
                        }
                    }
                    AuthenticationState.INVALID_AUTHENTICATION -> {
                    }
                    else -> {
                    }
                }
            })
    }

    private fun initView() {
        setupObservers()
        initRecyclerView()
        initOnRefreshListener()
        initSwipeToDeleteAndUndo()
    }

    private fun initRecyclerView() {
        adapter = EntityListAdapter(
            tableName,
            delegate.fromTableInterface,
            delegate.navigationInterface
        )

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

    private fun initOnRefreshListener() {
        fragment_list_swipe_to_refresh.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(
                requireContext(), R.color.list_swipe_to_refresh
            )
        )
        fragment_list_swipe_to_refresh.setColorSchemeColors(Color.WHITE)
        fragment_list_swipe_to_refresh.setOnRefreshListener {
            if (delegate.isConnected()) {
                if (loginViewModel.authenticationState.value == AuthenticationState.AUTHENTICATED) {
                    entityListViewModel.getData()
                } else {
                    syncDataRequested = false
                    displaySnackBar(activity, "An error occurred, please try again later")
                    Timber.d("Not authenticated yet, refreshDataRequested = $syncDataRequested")
                }
            } else {
                syncDataRequested = false
                displaySnackBar(activity, "No internet connection")
                Timber.d("No internet connection, refreshDataRequested = $syncDataRequested")
            }
            fragment_list_recycler_view.adapter = adapter
            fragment_list_swipe_to_refresh.isRefreshing = false
        }
    }

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
                        snackBar.setActionTextColor(Color.YELLOW)
                        snackBar.show()
                    }
                }
            }

        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(fragment_list_recycler_view)
    }
}
