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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.auth.LoginRequiredCallback
import com.qmobile.qmobileapi.connectivity.NetworkStateEnum
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiClient
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.ManyToOneRelation
import com.qmobile.qmobiledatasync.relation.OneToManyRelation
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.EntityListViewModelFactory
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.BaseActivity
import com.qmobile.qmobileui.activity.loginactivity.LoginActivity
import com.qmobile.qmobileui.utils.ToastHelper
import com.qmobile.qmobileui.utils.setupWithNavController
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("TooManyFunctions")
class MainActivity : BaseActivity(), FragmentCommunication, LifecycleObserver {

    var loginStatusText = ""
    private var onLaunch = true
    var authenticationRequested = true
    var shouldDelayOnForegroundEvent = AtomicBoolean(false)
    var currentNavController: LiveData<NavController>? = null
    lateinit var mainActivityDataSync: MainActivityDataSync

    // FragmentCommunication
    override lateinit var apiService: ApiService

    // ViewModels
    lateinit var entityListViewModelList: MutableList<EntityListViewModel<EntityModel>>

    // Interceptor notifies the MainActivity that we need to go to login page. First, stop syncing
    private val loginRequiredCallbackForInterceptor: LoginRequiredCallback =
        object : LoginRequiredCallback {
            override fun loginRequired() {
                if (!BaseApp.runtimeDataHolder.guestLogin)
                    mainActivityDataSync.dataSync.loginRequired.set(true)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init data sync class
        mainActivityDataSync = MainActivityDataSync(this)

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
        MainActivityObserver(this, entityListViewModelList).initObservers()

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
        apiService = ApiClient.getApiService(
            context = this,
            loginApiService = loginApiService,
            loginRequiredCallback = loginRequiredCallbackForInterceptor,
            logBody = BaseApp.runtimeDataHolder.logLevel <= Log.VERBOSE
        )
        if (this::entityListViewModelList.isInitialized) {
            entityListViewModelList.forEach { it.refreshRestRepository(apiService) }
        }
    }

    /**
     * Listens to event ON_STOP - app going in background
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onBackground() {
        Timber.d("[${Lifecycle.Event.ON_STOP}]")
        shouldDelayOnForegroundEvent.set(false)
    }

    /**
     * Listens to event ON_START - app going in foreground
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onForeground() {
        if (loginViewModel.authenticationState.value == AuthenticationStateEnum.AUTHENTICATED) {
            Timber.d("[${Lifecycle.Event.ON_START}]")
            applyOnForegroundEvent()
        } else {
            Timber.d("[${Lifecycle.Event.ON_START} - Delayed event, waiting for authentication]")
            shouldDelayOnForegroundEvent.set(true)
        }
    }

    fun applyOnForegroundEvent() {
        Timber.d("applyOnForegroundEvent")
        if (onLaunch) {
            Timber.d("applyOnForegroundEvent on Launch")
            onLaunch = false
            // Refreshing it again, as a remoteUrl change would bring issues with post requests
            // going on previous remoteUrl
            refreshAllApiClients()
            mainActivityDataSync.getEntityListViewModelsForSync(entityListViewModelList)
        }
        mainActivityDataSync.prepareDataSync(connectivityViewModel, null)
    }

    override fun requestAuthentication() {
        authenticationRequested = false
        tryAutoLogin()
    }

    /**
     * Performs data sync, requested by a table request
     */
    override fun requestDataSync(alreadyRefreshedTable: String?) {
        mainActivityDataSync.prepareDataSync(connectivityViewModel, alreadyRefreshedTable)
    }

    override fun handleAuthenticationState(authenticationState: AuthenticationStateEnum) {
        when (authenticationState) {
            AuthenticationStateEnum.AUTHENTICATED -> {
                if (loginStatusText.isNotEmpty()) {
                    ToastHelper.show(this, loginStatusText, MessageType.SUCCESS)
                    loginStatusText = ""
                }
                if (shouldDelayOnForegroundEvent.compareAndSet(true, false)) {
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

    override fun handleNetworkState(networkState: NetworkStateEnum) {
        when (networkState) {
            NetworkStateEnum.CONNECTED -> {
                // Setting the authenticationState to its initial value
                if (BaseApp.sharedPreferencesHolder.sessionToken.isNotEmpty())
                    loginViewModel.authenticationState.postValue(AuthenticationStateEnum.AUTHENTICATED)

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
            ToastHelper.show(this, resources.getString(R.string.no_internet_auto_login), MessageType.WARNING)
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
