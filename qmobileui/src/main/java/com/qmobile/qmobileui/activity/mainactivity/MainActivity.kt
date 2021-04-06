/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavController
import com.qmobile.qmobileapi.auth.AuthInfoHelper
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.auth.LoginRequiredCallback
import com.qmobile.qmobileapi.connectivity.isConnected
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiClient
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.network.LoginApiService
import com.qmobile.qmobiledatasync.sync.DataSync
import com.qmobile.qmobiledatasync.sync.EntityViewModelIsToSync
import com.qmobile.qmobiledatasync.sync.unsuccessfulSynchronizationNeedsLogin
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.BaseActivity
import com.qmobile.qmobileui.utils.QMobileUiUtil
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("TooManyFunctions")
class MainActivity : BaseActivity(), FragmentCommunication, LifecycleObserver {

    private var onLaunch = true
    var authenticationRequested = true
    var shouldDelayOnForegroundEvent = AtomicBoolean(false)
    val authInfoHelper = AuthInfoHelper.getInstance(this)
    var currentNavController: LiveData<NavController>? = null
    lateinit var dataSync: DataSync

    // FragmentCommunication
    override lateinit var apiService: ApiService
    override lateinit var loginApiService: LoginApiService
    override lateinit var connectivityManager: ConnectivityManager

    // ViewModels
    lateinit var loginViewModel: LoginViewModel
    lateinit var connectivityViewModel: ConnectivityViewModel
    lateinit var entityViewModelIsToSyncList: MutableList<EntityViewModelIsToSync>
    lateinit var entityListViewModelList: MutableList<EntityListViewModel<EntityModel>>

    // Interceptor notifies the MainActivity that we need to go to login page. First, stop syncing
    private val loginRequiredCallbackForInterceptor: LoginRequiredCallback =
        object : LoginRequiredCallback {
            override fun loginRequired() {
                if (!authInfoHelper.guestLogin)
                    dataSync.loginRequired.set(true)
            }
        }

    // DataSync notifies MainActivity to go to login page
    private val loginRequiredCallbackForDataSync: LoginRequiredCallback =
        object : LoginRequiredCallback {
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

    // Back button from appbar
    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    /**
     * If remoteUrl has been changed, we refresh ApiClient with new parameters (which are stored
     * in SharedPreferences)
     */
    override fun refreshApiClients() {
        ApiClient.clearApiClients()
        loginApiService = ApiClient.getLoginApiService(
            context = this,
            logBody = QMobileUiUtil.appUtilities.logLevel <= Log.VERBOSE
        )
        apiService = ApiClient.getApiService(
            context = this,
            loginApiService = loginApiService,
            loginRequiredCallback = loginRequiredCallbackForInterceptor,
            logBody = QMobileUiUtil.appUtilities.logLevel <= Log.VERBOSE
        )
        if (this::loginViewModel.isInitialized) {
            loginViewModel.refreshAuthRepository(loginApiService)
        }
        if (this::entityListViewModelList.isInitialized) {
            entityListViewModelList.forEach { it.refreshRestRepository(apiService) }
        }
    }

    /**
     * Listens to event ON_STOP - app going in background
     */
    @Suppress("unused")
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onBackground() {
        Timber.d("[${Lifecycle.Event.ON_STOP}]")
        shouldDelayOnForegroundEvent.set(false)
    }

    /**
     * Listens to event ON_START - app going in foreground
     */
    @Suppress("unused")
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
        if (onLaunch) {
            onLaunch = false
            getEntityListViewModelsForSync()
        }
        setDataSyncObserver(null)
    }

    override fun isConnected(): Boolean =
        connectivityManager.isConnected(connectivityViewModel.networkStateMonitor.value)

    override fun requestAuthentication() {
        authenticationRequested = false
        tryAutoLogin()
    }

    /**
     * Performs data sync, requested by a table request
     */
    override fun requestDataSync(alreadyRefreshedTable: String) {
        setDataSyncObserver(alreadyRefreshedTable)
    }

    override fun darkModeEnabled(): Boolean {
        return when (resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }
    }
}
