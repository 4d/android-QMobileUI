/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileapi.auth.AuthenticationState
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiClient
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.utils.LoginRequiredCallback
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.network.NetworkState
import com.qmobile.qmobiledatasync.sync.DataSync
import com.qmobile.qmobiledatasync.sync.resetIsToSync
import com.qmobile.qmobiledatasync.toast.Event
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobiledatasync.utils.ScheduleRefresh
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.EntityListViewModelFactory
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.ActivitySettingsInterface
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.Action
import com.qmobile.qmobileui.action.ActionHelper
import com.qmobile.qmobileui.action.ActionNavigable
import com.qmobile.qmobileui.activity.BaseActivity
import com.qmobile.qmobileui.activity.loginactivity.LoginActivity
import com.qmobile.qmobileui.binding.getColorFromAttr
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.utils.PermissionChecker
import com.qmobile.qmobileui.utils.ToastHelper
import com.qmobile.qmobileui.utils.setupWithNavController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

const val BASE_PERMISSION_REQUEST_CODE = 1000

class MainActivity :
    BaseActivity(),
    FragmentCommunication,
    ActivitySettingsInterface,
    LifecycleEventObserver,
    PermissionChecker,
    ActionActivity {

    private var loginStatusText = ""
    private var onLaunch = true
    private var authenticationRequested = true
    private var shouldDelayOnForegroundEvent = AtomicBoolean(false)
    private var currentNavController: LiveData<NavController>? = null
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var mainActivityDataSync: MainActivityDataSync
    private lateinit var mainActivityObserver: MainActivityObserver
    private var job: Job? = null

    // FragmentCommunication
    override lateinit var apiService: ApiService
    private lateinit var selectedAction: Action
    var currentEntity: EntityModel? = null

    private var serverNotAccessibleString = ""
    private var noInternetString = ""

    // ViewModels
    lateinit var entityListViewModelList: MutableList<EntityListViewModel<EntityModel>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serverNotAccessibleString = getString(R.string.server_not_accessible)
        noInternetString = getString(R.string.no_internet)

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
                if (loginViewModel.authenticationState.value == AuthenticationState.AUTHENTICATED) {
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
            dataSync()
        }
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
                if (loginViewModel.authenticationState.value != AuthenticationState.AUTHENTICATED) {
                    // This is to schedule a notifyDataSetChanged() because of cached images (only on first dataSync)
                    entityListViewModel?.setScheduleRefreshState(ScheduleRefresh.SCHEDULE)
                    requestAuthentication()
                } else {
                    // AUTHENTICATED
                    when (entityListViewModel?.dataSynchronized?.value) {
                        DataSync.State.UNSYNCHRONIZED -> dataSync()
                        DataSync.State.SYNCHRONIZED -> {
                            job?.cancel()
                            job = lifecycleScope.launch {
                                entityListViewModel.getEntities { shouldSyncData ->
                                    if (shouldSyncData) {
                                        Timber.d("GlobalStamp changed, synchronization is required")
                                        Timber.i("Starting a dataSync procedure")
                                        dataSync(currentTableName)
                                    } else {
                                        Timber.d("GlobalStamp unchanged, no synchronization is required")
                                    }
                                }
                            }
                        }
                        DataSync.State.SYNCHRONIZING -> Timber.d("Synchronization already in progress")
                        DataSync.State.RESYNC ->
                            Timber.d("Resynchronization table, because globalStamp changed while performing a dataSync")
                        else -> {}
                    }
                }
            }

            override fun onServerInaccessible() {
                onServerInaccessible(currentTableName)
            }

            override fun onNoInternet() {
                onNoInternet(currentTableName)
            }
        })
    }

    private fun onServerInaccessible(tableName: String) {
        connectivityViewModel.toastMessage.showMessage(serverNotAccessibleString, tableName, ToastMessage.Type.ERROR)
    }

    private fun onNoInternet(tableName: String) {
        connectivityViewModel.toastMessage.showMessage(noInternetString, tableName, ToastMessage.Type.ERROR)
    }

    override fun handleAuthenticationState(authenticationState: AuthenticationState) {
        when (authenticationState) {
            AuthenticationState.AUTHENTICATED -> {
                if (loginStatusText.isNotEmpty()) {
                    ToastHelper.show(this, loginStatusText, ToastMessage.Type.SUCCESS)
                    loginStatusText = ""
                }
                if (shouldDelayOnForegroundEvent.getAndSet(false)) {
                    applyOnForegroundEvent()
                }
            }
            AuthenticationState.LOGOUT -> {
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
        actionNavigable: ActionNavigable,
        isEntityAction: Boolean
    ) {
        (menu as? MenuBuilder)?.setOptionalIconsVisible(true)

        val withIcons = actions.firstOrNull { it.getIconDrawablePath() != null } != null
        actions.forEach { action ->
            val drawable =
                if (withIcons) ActionHelper.getActionIconDrawable(this, action) else null

            drawable?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                getColorFromAttr(R.attr.colorOnSurface),
                BlendModeCompat.SRC_ATOP
            )

            menu.add(action.getPreferredName())
                .setOnMenuItemClickListener {
                    onActionClick(action, actionNavigable, isEntityAction)
                    true
                }
                .setIcon(drawable)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
    }

    override fun onActionClick(action: Action, actionNavigable: ActionNavigable, isEntityAction: Boolean) {
        if (action.parameters.length() > 0) {
            selectedAction = action
            if (!isEntityAction)
                currentEntity = null
            actionNavigable.navigationToActionForm(action, currentEntity?.__KEY)
        } else {
            sendAction(
                actionName = action.name,
                actionContent = actionNavigable.getActionContent(currentEntity?.__KEY),
                tableName = actionNavigable.tableName
            ) {
                // Nothing to do
            }
        }
    }

    override fun getSelectedAction(): Action {
        return selectedAction
    }

    override fun getSelectedEntity(): EntityModel? {
        return currentEntity
    }

    override fun setCurrentEntityModel(entityModel: EntityModel?) {
        currentEntity = entityModel
    }

    override fun sendAction(
        actionName: String,
        actionContent: MutableMap<String, Any>,
        tableName: String,
        onActionSent: () -> Unit
    ) {
        checkNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                entityListViewModelList[0].sendAction(actionName, actionContent) { actionResponse ->
                    actionResponse?.let {
                        actionResponse.dataSynchro?.let { dataSynchro ->
                            if (dataSynchro) {
                                requestDataSync(tableName)
                            }
                        }
                        onActionSent()
                    }
                }
            }

            override fun onServerInaccessible() {
                onServerInaccessible(tableName)
            }

            override fun onNoInternet() {
                onNoInternet(tableName)
            }
        })
    }

    override fun uploadImage(
        bodies: Map<String, RequestBody?>,
        tableName: String,
        onImageUploaded: (parameterName: String, receivedId: String) -> Unit,
        onAllUploadFinished: () -> Unit
    ) {
        checkNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                entityListViewModelList[0].uploadImage(
                    imagesToUpload = bodies,
                    onImageUploaded = { parameterName, receivedId ->
                        onImageUploaded(parameterName, receivedId)
                    }
                ) {
                    onAllUploadFinished()
                }
            }

            override fun onServerInaccessible() {
                onServerInaccessible(tableName)
            }

            override fun onNoInternet() {
                onNoInternet(tableName)
            }
        })
    }

    override fun handleNetworkState(networkState: NetworkState) {
        when (networkState) {
            NetworkState.CONNECTED -> {
                // Setting the authenticationState to its initial value
                if (BaseApp.sharedPreferencesHolder.sessionToken.isNotEmpty())
                    loginViewModel.setAuthenticationState(AuthenticationState.AUTHENTICATED)

                // If guest and not yet logged in, auto login
                if (BaseApp.sharedPreferencesHolder.sessionToken.isEmpty() &&
                    BaseApp.runtimeDataHolder.guestLogin &&
                    authenticationRequested
                ) {
                    authenticationRequested = false
                    tryAutoLogin()
                }
            }
            else -> {}
        }
    }

    // Observe any toast message from Entity Detail
    override fun observeEntityToastMessage(message: SharedFlow<Event<ToastMessage.Holder>>) {
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
            ToastHelper.show(this, getString(R.string.no_internet_auto_login), ToastMessage.Type.WARNING)
        }
    }

    /**
     * Called on first creation and when restoring state.
     */
    private fun setupBottomNavigationBar() {
        bottomNav = this.findViewById(R.id.bottom_nav)
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
            this
        ) { navController ->
            setupActionBarWithNavController(navController)
        }
        currentNavController = controller
    }

    private val requestPermissionMap: MutableMap<Int, (isGranted: Boolean) -> Unit> = mutableMapOf()

    /**
     * This method is accessible from BindingAdapters for Custom formatters
     */
    fun askPermission(permission: String, rationale: String, callback: (isGranted: Boolean) -> Unit) {
        val requestPermissionCode = BASE_PERMISSION_REQUEST_CODE + requestPermissionMap.size
        requestPermissionMap[requestPermissionCode] = callback

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                MaterialAlertDialogBuilder(this, R.style.TitleThemeOverlay_MaterialComponents_MaterialAlertDialog)
                    .setTitle(getString(R.string.permission_dialog_title))
                    .setMessage(rationale)
                    .setPositiveButton(getString(R.string.permission_dialog_positive)) { _, _ ->
                        ActivityCompat.requestPermissions(this, arrayOf(permission), requestPermissionCode)
                    }
                    .setNegativeButton(getString(R.string.permission_dialog_negative)) { dialog, _ -> dialog.cancel() }
                    .show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(permission), requestPermissionCode)
            }
        } else {
            callback(true)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when {
            requestPermissionMap.containsKey(requestCode) -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    requestPermissionMap[requestCode]?.invoke(true)
                } else {
                    requestPermissionMap[requestCode]?.invoke(false)
                }
                return
            }
            else -> {}
        }
    }

    override fun setFullScreenMode(isFullScreen: Boolean) {
        bottomNav.visibility = if (isFullScreen) {
            supportActionBar?.hide()
            View.GONE
        } else {
            supportActionBar?.show()
            View.VISIBLE
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        setFullScreenMode(false)
    }
}
