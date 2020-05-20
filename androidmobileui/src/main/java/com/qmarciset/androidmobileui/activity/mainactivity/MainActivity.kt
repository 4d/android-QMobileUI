/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.activity.mainactivity

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavController
import com.qmarciset.androidmobileapi.auth.AuthInfoHelper
import com.qmarciset.androidmobileapi.auth.AuthenticationState
import com.qmarciset.androidmobileapi.auth.LoginRequiredCallback
import com.qmarciset.androidmobileapi.connectivity.NetworkState
import com.qmarciset.androidmobileapi.connectivity.NetworkUtils
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobileapi.network.ApiClient
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobileapi.network.LoginApiService
import com.qmarciset.androidmobiledatastore.db.AppDatabaseInterface
import com.qmarciset.androidmobiledatasync.sync.DataSync
import com.qmarciset.androidmobiledatasync.sync.EntityViewModelIsToSync
import com.qmarciset.androidmobiledatasync.sync.unsuccessfulSynchronizationNeedsLogin
import com.qmarciset.androidmobiledatasync.utils.FromTableForViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.EntityListViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.LoginViewModel
import com.qmarciset.androidmobileui.FragmentCommunication
import com.qmarciset.androidmobileui.R
import com.qmarciset.androidmobileui.activity.BaseActivity
import com.qmarciset.androidmobileui.activity.loginactivity.LoginActivity
import com.qmarciset.androidmobileui.app.BaseApp
import com.qmarciset.androidmobileui.utils.FromTableInterface
import com.qmarciset.androidmobileui.utils.NavigationInterface
import com.qmarciset.androidmobileui.utils.ViewDataBindingInterface
import com.qmarciset.androidmobileui.utils.displaySnackBar
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

@SuppressLint("BinaryOperationInTimber")
class MainActivity : BaseActivity(), FragmentCommunication, LifecycleObserver {

    private var onLaunch = true
    private var authenticationRequested = true
    private var shouldDelayOnForegroundEvent = AtomicBoolean(false)
    private val authInfoHelper = AuthInfoHelper.getInstance(this)
    var currentNavController: LiveData<NavController>? = null
    lateinit var dataSync: DataSync

    // FragmentCommunication
    override val appInstance: Application = BaseApp.instance
    override val appDatabaseInterface: AppDatabaseInterface = BaseApp.appDatabaseInterface
    override val fromTableInterface: FromTableInterface = BaseApp.fromTableInterface
    override val fromTableForViewModel: FromTableForViewModel = BaseApp.fromTableForViewModel
    override val navigationInterface: NavigationInterface = BaseApp.navigationInterface
    override val viewDataBindingInterface: ViewDataBindingInterface =
        BaseApp.viewDataBindingInterface
    override lateinit var apiService: ApiService
    override lateinit var loginApiService: LoginApiService
    override lateinit var connectivityManager: ConnectivityManager

    // ViewModels
    lateinit var loginViewModel: LoginViewModel
    lateinit var connectivityViewModel: ConnectivityViewModel
    lateinit var entityViewModelIsToSyncList: MutableList<EntityViewModelIsToSync>
    lateinit var entityListViewModelList: MutableList<EntityListViewModel<EntityModel>>

    // Interceptor notifies the MainActivity that we need to go to login page. First, stop syncing
    private val loginRequiredCallbackForInterceptor: LoginRequiredCallback = object : LoginRequiredCallback {
        override fun loginRequired() {
            if (!authInfoHelper.guestLogin)
                dataSync.loginRequired.set(true)
        }
    }

    // DataSync notifies MainActivity to go to login page
    private val loginRequiredCallbackForDataSync: LoginRequiredCallback = object : LoginRequiredCallback {
        override fun loginRequired() {
            if (!authInfoHelper.guestLogin) {
                dataSync.unsuccessfulSynchronizationNeedsLogin(entityViewModelIsToSyncList)
                startLoginActivity()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init data sync class
        dataSync = DataSync(this, authInfoHelper, loginRequiredCallbackForDataSync)

        // Init system services in onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // For test purposes only
//        appDatabaseInterface.populateDatabase()
//        authInfoHelper.globalStamp = 240

        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        } // Else, need to wait for onRestoreInstanceState

        // Init ApiClients
        refreshApiClients()

        getViewModel()
        setupObservers()

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

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    /**
     * If remoteUrl has been changed, we refresh ApiClient with new parameters (which are stored
     * in SharedPreferences)
     */
    override fun refreshApiClients() {
        ApiClient.clearApiClients()
        loginApiService = ApiClient.getLoginApiService(this)
        apiService = ApiClient.getApiService(
            context = this,
            loginApiService = loginApiService,
            loginRequiredCallback = loginRequiredCallbackForInterceptor
        )
    }

    override fun getViewModel() {
        getMainActivityViewModel()
    }

    override fun setupObservers() {

        // Observe authentication state
        loginViewModel.authenticationState.observe(
            this,
            Observer { authenticationState ->
                Timber.i("[AuthenticationState : $authenticationState]")
                when (authenticationState) {
                    AuthenticationState.AUTHENTICATED -> {
                        if (shouldDelayOnForegroundEvent.compareAndSet(true, false)) {
                            applyOnForegroundEvent()
                        }
                    }
                    AuthenticationState.LOGOUT -> {
                        // Logout performed
                        if (!authInfoHelper.guestLogin)
                            startLoginActivity()
                    }
                    else -> {
                    }
                }
            })

        // Observe network status
        if (NetworkUtils.sdkNewerThanKitKat) {
            connectivityViewModel.networkStateMonitor.observe(this, Observer { networkState ->
                Timber.i("[NetworkState : $networkState]")
                when (networkState) {
                    NetworkState.CONNECTED -> {
                        // Setting the authenticationState to its initial value
                        if (authInfoHelper.sessionToken.isNotEmpty())
                            loginViewModel.authenticationState.postValue(AuthenticationState.AUTHENTICATED)

                        // If guest and not yet logged in, auto login
                        if (authInfoHelper.sessionToken.isEmpty() &&
                            authInfoHelper.guestLogin &&
                            authenticationRequested
                        ) {
                            authenticationRequested = false
                            tryAutoLogin()
                        }
                    }
                    else -> {
                    }
                }
            })
        }

        // Observe when data are synchronized
        for (entityListViewModel in entityListViewModelList) {
            entityListViewModel.dataSynchronized.observe(this, Observer { dataSyncState ->
                Timber.i("[DataSyncState : $dataSyncState, " +
                        "Table : ${entityListViewModel.getAssociatedTableName()}, " +
                        "Instance : $entityListViewModel]")
            })
        }
    }

    /**
     * Goes back to login page
     */
    private fun startLoginActivity() {
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
     * Listens to event ON_STOP - app going in background
     */
    @Suppress("unused")
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onBackground() {
        Timber.i("[${Lifecycle.Event.ON_STOP}]")
        shouldDelayOnForegroundEvent.set(false)
    }

    /**
     * Listens to event ON_START - app going in foreground
     */
    @Suppress("unused")
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onForeground() {
        if (loginViewModel.authenticationState.value == AuthenticationState.AUTHENTICATED) {
            Timber.i("[${Lifecycle.Event.ON_START}]")
            applyOnForegroundEvent()
        } else {
            Timber.i("[${Lifecycle.Event.ON_START} - Delayed event, waiting for authentication]")
            shouldDelayOnForegroundEvent.set(true)
        }
    }

    private fun applyOnForegroundEvent() {
        if (onLaunch) {
            onLaunch = false
            getEntityListViewModelsForSync()
        }
        setDataSyncObserver(null)
    }

    override fun isConnected(): Boolean =
        NetworkUtils.isConnected(
            connectivityViewModel.networkStateMonitor.value,
            connectivityManager
        )

    override fun requestAuthentication() {
        authenticationRequested = false
        tryAutoLogin()
    }

    /**
     * Tries to login while in guest mode. Might fail if no Internet connection
     */
    private fun tryAutoLogin() {
        if (isConnected()) {
            loginViewModel.login { }
        } else {
            authenticationRequested = true
            Timber.d("No Internet connection, authenticationRequested")
            displaySnackBar(
                this,
                resources.getString(R.string.no_internet_auto_login)
            )
        }
    }

    /**
     * Performs data sync, requested by a table request
     */
    override fun requestDataSync(alreadyRefreshedTable: String) {
        setDataSyncObserver(alreadyRefreshedTable)
    }

    /**
     * Commands the appropriate EntityListViewModel to add the related entity in its dao
     */
    override fun dispatchNewRelatedEntity(tableName: String, entity: EntityModel) {
        val entityListViewModel = entityListViewModelList.first { it.getAssociatedTableName() == tableName }
        entityListViewModel.insert(entity)
    }
}
