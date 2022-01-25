/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.menu.MenuBuilder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiClient
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.utils.LoginRequiredCallback
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.network.NetworkStateEnum
import com.qmobile.qmobiledatasync.relation.ManyToOneRelation
import com.qmobile.qmobiledatasync.relation.OneToManyRelation
import com.qmobile.qmobiledatasync.sync.DataSyncStateEnum
import com.qmobile.qmobiledatasync.sync.resetIsToSync
import com.qmobile.qmobiledatasync.toast.Event
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.toast.ToastMessageHolder
import com.qmobile.qmobiledatasync.utils.ScheduleRefreshEnum
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.EntityListViewModelFactory
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.Action
import com.qmobile.qmobileui.action.ActionHelper
import com.qmobile.qmobileui.activity.BaseActivity
import com.qmobile.qmobileui.activity.loginactivity.LoginActivity
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.utils.ToastHelper
import com.qmobile.qmobileui.utils.setupWithNavController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : BaseActivity(), FragmentCommunication, LifecycleEventObserver {

    private var loginStatusText = ""
    private var onLaunch = true
    private var authenticationRequested = true
    private var shouldDelayOnForegroundEvent = AtomicBoolean(false)
    private var currentNavController: LiveData<NavController>? = null
    private lateinit var mainActivityDataSync: MainActivityDataSync
    private lateinit var mainActivityObserver: MainActivityObserver
    private var job: Job? = null

    // FragmentCommunication
    override lateinit var apiService: ApiService
    private lateinit var selectedAction: Action
    var entity: EntityModel? = null

    // ViewModels
    lateinit var entityListViewModelList: MutableList<EntityListViewModel<EntityModel>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init system services in onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (savedInstanceState == null) {
            // Retrieve bundled parameter to know if there was a successful login with statusText
            loginStatusText = intent.getStringExtra(LOGIN_STATUS_TEXT) ?: ""
            setupBottomNavigationBar()
        } // Else, need to wait for onRestoreInstanceState

        // Init ApiClients
        refreshAllApiClients()

        initViewModels()
        getEntityListViewModelList()
        mainActivityObserver = MainActivityObserver(this, entityListViewModelList).apply {
            initObservers()
        }

        // Init data sync class
        mainActivityDataSync = MainActivityDataSync(this)

        // Follow activity lifecycle and check when activity enters foreground for data sync
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
        setupBottomNavigationBar()
    }

    // Back button from appbar
    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    // Get EntityListViewModel list
    private fun getEntityListViewModelList() {
        entityListViewModelList = mutableListOf()
        BaseApp.genericTableHelper.tableNames().forEach { tableName ->
            val clazz = BaseApp.genericTableHelper.entityListViewModelClassFromTable(tableName)

            entityListViewModelList.add(
                ViewModelProvider(
                    this,
                    EntityListViewModelFactory(
                        tableName,
                        apiService
                    )
                )[clazz]
            )
        }
    }

    /**
     * If remoteUrl has been changed, we refresh ApiClient with new parameters (which are stored
     * in SharedPreferences)
     */
    override fun refreshAllApiClients() {
        super.refreshApiClients()

        // Interceptor notifies the MainActivity that we need to go to login page. First, stop syncing
        val loginRequiredCallbackForInterceptor: LoginRequiredCallback = {
            if (!BaseApp.runtimeDataHolder.guestLogin)
                mainActivityDataSync.dataSync.loginRequired.set(true)
        }
        apiService = ApiClient.getApiService(
            loginApiService = loginApiService,
            loginRequiredCallback = loginRequiredCallbackForInterceptor,
            sharedPreferencesHolder = BaseApp.sharedPreferencesHolder,
            logBody = BaseApp.runtimeDataHolder.logLevel <= Log.VERBOSE,
            mapper = BaseApp.mapper
        )
        if (this::entityListViewModelList.isInitialized) {
            entityListViewModelList.forEach { it.refreshRestRepository(apiService) }
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                Timber.d("[${Lifecycle.Event.ON_STOP}]")
                shouldDelayOnForegroundEvent.set(false)
            }
            Lifecycle.Event.ON_START -> {
                if (loginViewModel.authenticationState.value == AuthenticationStateEnum.AUTHENTICATED) {
                    Timber.d("[${Lifecycle.Event.ON_START}]")
                    applyOnForegroundEvent()
                } else {
                    Timber.d("[${Lifecycle.Event.ON_START} - Delayed event, waiting for authentication]")
                    shouldDelayOnForegroundEvent.set(true)
                }
            }
            else -> {}
        }
    }

    private fun applyOnForegroundEvent() {
        Timber.d("applyOnForegroundEvent")
        if (onLaunch) {
            Timber.d("applyOnForegroundEvent on Launch")
            onLaunch = false
            // Refreshing it again, as a remoteUrl change would bring issues with post requests
            // going on previous remoteUrl
            refreshAllApiClients()
            entityListViewModelList.resetIsToSync()
        }
        dataSync()
    }

    override fun requestAuthentication() {
        authenticationRequested = false
        tryAutoLogin()
    }

    private fun dataSync(alreadyRefreshedTable: String? = null) {
        mainActivityDataSync.dataSync(connectivityViewModel, alreadyRefreshedTable)
    }

    /**
     * Performs data sync, requested by a table request
     */
    override fun requestDataSync(currentTableName: String) {

        val entityListViewModel =
            entityListViewModelList.find { it.getAssociatedTableName() == currentTableName }

        checkNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                if (loginViewModel.authenticationState.value != AuthenticationStateEnum.AUTHENTICATED) {
                    // This is to schedule a notifyDataSetChanged() because of cached images (only on first dataSync)
                    entityListViewModel?.setScheduleRefreshState(ScheduleRefreshEnum.SCHEDULE)
                    requestAuthentication()
                } else {
                    // AUTHENTICATED
                    when (entityListViewModel?.dataSynchronized?.value) {
                        DataSyncStateEnum.UNSYNCHRONIZED -> dataSync()
                        DataSyncStateEnum.SYNCHRONIZED -> {
                            job?.cancel()
                            job = lifecycleScope.launch {
                                entityListViewModel.getEntities { shouldSyncData ->
                                    if (shouldSyncData) {
                                        Timber.d("GlobalStamp changed, synchronization is required")
                                        dataSync(currentTableName)
                                    } else {
                                        Timber.d("GlobalStamp unchanged, no synchronization is required")
                                    }
                                }
                            }
                        }
                        DataSyncStateEnum.SYNCHRONIZING -> Timber.d("Synchronization already in progress")
                        DataSyncStateEnum.RESYNC ->
                            Timber.d("Resynchronization table, because globalStamp changed while performing a dataSync")
                        else -> {}
                    }
                }
            }

            override fun onServerInaccessible() {
                connectivityViewModel.toastMessage.showMessage(
                    getString(R.string.action_send_server_not_accessible),
                    currentTableName,
                    MessageType.ERROR
                )
            }

            override fun onNoInternet() {
                connectivityViewModel.toastMessage.showMessage(
                    getString(R.string.action_send_no_internet),
                    currentTableName,
                    MessageType.ERROR
                )
            }
        })
    }

    override fun handleAuthenticationState(authenticationState: AuthenticationStateEnum) {
        when (authenticationState) {
            AuthenticationStateEnum.AUTHENTICATED -> {
                if (loginStatusText.isNotEmpty()) {
                    ToastHelper.show(this, loginStatusText, MessageType.SUCCESS)
                    loginStatusText = ""
                }
                if (shouldDelayOnForegroundEvent.getAndSet(false)) {
                    applyOnForegroundEvent()
                }
            }
            AuthenticationStateEnum.LOGOUT -> {
                // Logout performed
                if (!BaseApp.runtimeDataHolder.guestLogin)
                    startLoginActivity()
            }
            else -> {
            }
        }
    }

    @Suppress("RestrictedApi")
    override fun setupActionsMenu(
        menu: Menu,
        actions: List<Action>,
        onMenuItemClick: (Action) -> Unit
    ) {
        (menu as? MenuBuilder)?.setOptionalIconsVisible(true)

        val withIcons = actions.firstOrNull { it.getIconDrawablePath() != null } != null
        actions.forEach { action ->
            val drawable =
                if (withIcons) ActionHelper.getActionIconDrawable(this, action) else null

            menu.add(action.getPreferredName())
                .setOnMenuItemClickListener {
                    onMenuItemClick(action)
                    true
                }
                .setIcon(drawable)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
    }

    override fun setSelectAction(action: Action) {
        selectedAction = action
    }

    override fun getSelectAction(): Action {
        return selectedAction
    }

    override fun setSelectedEntity(entityModel: EntityModel?) {
        entity = entityModel
    }

    override fun getSelectedEntity(): EntityModel? {
        return entity
    }

    override fun handleNetworkState(networkState: NetworkStateEnum) {
        when (networkState) {
            NetworkStateEnum.CONNECTED -> {
                // Setting the authenticationState to its initial value
                if (BaseApp.sharedPreferencesHolder.sessionToken.isNotEmpty())
                    loginViewModel.setAuthenticationState(AuthenticationStateEnum.AUTHENTICATED)

                // If guest and not yet logged in, auto login
                if (BaseApp.sharedPreferencesHolder.sessionToken.isEmpty() &&
                    BaseApp.runtimeDataHolder.guestLogin &&
                    authenticationRequested
                ) {
                    authenticationRequested = false
                    tryAutoLogin()
                }
            }
            else -> {
            }
        }
    }

    // Observe any toast message from Entity Detail
    override fun observeEntityToastMessage(message: SharedFlow<Event<ToastMessageHolder>>) {
        mainActivityObserver.observeEntityToastMessage(message)
    }

    /**
     * Goes back to login page
     */
    fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(LOGGED_OUT, true)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK
        )
        startActivity(intent)
        finish()
    }

    /**
     * Tries to login while in guest mode. Might fail if no Internet connection
     */
    private fun tryAutoLogin() {
        if (connectivityViewModel.isConnected()) {
            loginViewModel.login { }
        } else {
            authenticationRequested = true
            Timber.d("No Internet connection, authenticationRequested")
            ToastHelper.show(
                this,
                resources.getString(R.string.no_internet_auto_login),
                MessageType.WARNING
            )
        }
    }

    /**
     * Called on first creation and when restoring state.
     */
    private fun setupBottomNavigationBar() {
        val bottomNav = this.findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.menu.clear() // clear old inflated items.
        BaseApp.bottomNavigationMenu?.let {
            bottomNav.inflateMenu(it)
        }

        val navGraphIds = BaseApp.navGraphIds

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = bottomNav.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_container,
            intent = intent
        )
        // Whenever the selected controller changes, setup the action bar.
        controller.observe(
            this,
            { navController ->
                setupActionBarWithNavController(navController)
            }
        )
        currentNavController = controller
    }

    /**
     * Commands the appropriate EntityListViewModel to add the related entity in its dao
     */
    fun dispatchNewRelatedEntity(manyToOneRelation: ManyToOneRelation) {
        val entityListViewModel =
            entityListViewModelList.find { it.getAssociatedTableName() == manyToOneRelation.className }
        entityListViewModel?.insertNewRelatedEntity(manyToOneRelation)
    }

    /**
     * Commands the appropriate EntityListViewModel to add the related entities in its dao
     */
    fun dispatchNewRelatedEntities(oneToManyRelation: OneToManyRelation) {
        val entityListViewModel =
            entityListViewModelList.find { it.getAssociatedTableName() == oneToManyRelation.className }
        entityListViewModel?.insertNewRelatedEntities(oneToManyRelation)
    }
}
