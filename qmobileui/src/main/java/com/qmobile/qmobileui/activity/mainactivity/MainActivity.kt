/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.qmobile.qmobileapi.auth.AuthenticationState
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiClient
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.utils.LoginRequiredCallback
import com.qmobile.qmobileapi.utils.UploadHelper.UPLOADED_METADATA_STRING
import com.qmobile.qmobileapi.utils.UploadHelper.getBodies
import com.qmobile.qmobiledatastore.dao.ActionInfo
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.network.NetworkState
import com.qmobile.qmobiledatasync.sync.DataSync
import com.qmobile.qmobiledatasync.sync.resetIsToSync
import com.qmobile.qmobiledatasync.toast.Event
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobiledatasync.utils.ScheduleRefresh
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.TaskViewModel
import com.qmobile.qmobiledatasync.viewmodel.deleteAll
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getTaskViewModel
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.ActivitySettingsInterface
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionNavigable
import com.qmobile.qmobileui.action.actionparameters.ActionParametersFragment
import com.qmobile.qmobileui.action.barcode.BarcodeScannerFragment
import com.qmobile.qmobileui.action.inputcontrols.InputControl.Format.cleanActionContent
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.utils.ActionHelper
import com.qmobile.qmobileui.action.utils.ActionUIHelper
import com.qmobile.qmobileui.action.utils.ActionUIHelper.paramMenuActionDrawable
import com.qmobile.qmobileui.action.webview.ActionWebViewFragment
import com.qmobile.qmobileui.activity.BaseActivity
import com.qmobile.qmobileui.activity.loginactivity.LoginActivity
import com.qmobile.qmobileui.binding.ImageHelper.adjustActionDrawableMargins
import com.qmobile.qmobileui.databinding.ActivityMainBinding
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.ui.SnackbarHelper
import com.qmobile.qmobileui.ui.setMaterialFadeTransition
import com.qmobile.qmobileui.ui.setSharedAxisZExitTransition
import com.qmobile.qmobileui.utils.PermissionChecker
import com.qmobile.qmobileui.utils.PermissionCheckerImpl
import com.qmobile.qmobileui.utils.setupWithNavController
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.RequestBody
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity :
    BaseActivity(),
    FragmentCommunication,
    ActivitySettingsInterface,
    LifecycleEventObserver,
    PermissionChecker,
    ActionActivity,
    ActivityResultController {

    private var loginStatusText = ""
    private var onLaunch = true
    private var authenticationRequested = true
    private var shouldDelayOnForegroundEvent = AtomicBoolean(false)
    private var currentNavController: LiveData<NavController>? = null
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainActivityDataSync: MainActivityDataSync
    private lateinit var mainActivityObserver: MainActivityObserver

    // FragmentCommunication
    override lateinit var apiService: ApiService
    private var currentEntity: RoomEntity? = null

    private var serverNotAccessibleString = ""
    private var serverNotAccessibleActionString = ""
    private var noInternetString = ""
    private var noInternetActionString = ""
    private var pendingTaskString = ""

    override val activityResultControllerImpl = ActivityResultControllerImpl(this)
    override val permissionCheckerImpl = PermissionCheckerImpl(this)

    val currentNavigationFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.nav_host_container)
            ?.childFragmentManager
            ?.fragments
            ?.first()

    // ViewModels
    lateinit var entityListViewModelList: MutableList<EntityListViewModel<EntityModel>>

    private lateinit var taskViewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.appbar.applyInsetter {
            type(statusBars = true) {
                padding(animated = true)
            }
        }

        binding.navHostContainer.applyInsetter {
            type(navigationBars = true) {
                padding(animated = true)
            }
            type(ime = true) {
                padding()
            }
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        serverNotAccessibleString = getString(R.string.server_not_accessible)
        serverNotAccessibleActionString = getString(R.string.action_send_server_not_accessible)
        noInternetString = getString(R.string.no_internet)
        noInternetActionString = getString(R.string.action_send_no_internet)
        pendingTaskString = getString(R.string.pending_task_menu_item)

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
        taskViewModel = getTaskViewModel(this)
        mainActivityObserver = MainActivityObserver(this, entityListViewModelList, taskViewModel).apply {
            initObservers()
        }

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
        BaseApp.runtimeDataHolder.tableInfo.keys.forEach { tableName ->
            val entityListViewModel = getEntityListViewModel(this, tableName, apiService)
            entityListViewModelList.add(entityListViewModel)
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
            if (!BaseApp.runtimeDataHolder.guestLogin) {
                mainActivityDataSync.dataSync.loginRequired.set(true)
            }
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
            else -> {
            }
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
            mainActivityDataSync.dataSync()
        }
    }

    override fun requestAuthentication() {
        authenticationRequested = false
        tryAutoLogin()
    }

    /**
     * Performs data sync, requested by a table request
     */
    override fun requestDataSync(currentTableName: String) {
        val entityListViewModel =
            entityListViewModelList.find { it.getAssociatedTableName() == currentTableName }

        queryNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                if (loginViewModel.authenticationState.value != AuthenticationState.AUTHENTICATED) {
                    // This is to schedule a notifyDataSetChanged() because of cached images (only on first dataSync)
                    entityListViewModel?.setScheduleRefreshState(ScheduleRefresh.SCHEDULE)
                    requestAuthentication()
                } else {
                    // AUTHENTICATED
                    when (entityListViewModel?.dataSynchronized?.value) {
                        DataSync.State.UNSYNCHRONIZED -> mainActivityDataSync.dataSync()
                        DataSync.State.SYNCHRONIZED -> {
                            entityListViewModel.getEntities { shouldSyncData ->
                                if (shouldSyncData) {
                                    mainActivityDataSync.shouldDataSync(currentTableName)
                                } else {
                                    Timber.d("GlobalStamp unchanged, no synchronization is required")
                                }
                            }
                        }
                        DataSync.State.SYNCHRONIZING -> Timber.d("Synchronization already in progress")
                        DataSync.State.RESYNC ->
                            Timber.d("Resynchronization table, because globalStamp changed while performing a dataSync")
                        else -> {
                        }
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

    private fun onServerInaccessible(tableName: String, isFromAction: Boolean = false) {
        if (isFromAction) {
            connectivityViewModel.toastMessage
                .showMessage(serverNotAccessibleActionString, tableName, ToastMessage.Type.NEUTRAL)
            onBackPressed()
        } else {
            connectivityViewModel.toastMessage
                .showMessage(serverNotAccessibleString, tableName, ToastMessage.Type.ERROR)
        }
    }

    private fun onNoInternet(tableName: String, isFromAction: Boolean = false) {
        if (isFromAction) {
            connectivityViewModel.toastMessage.showMessage(noInternetActionString, tableName, ToastMessage.Type.NEUTRAL)
            onBackPressed()
        } else {
            connectivityViewModel.toastMessage.showMessage(noInternetString, tableName, ToastMessage.Type.ERROR)
        }
    }

    override fun handleAuthenticationState(authenticationState: AuthenticationState) {
        when (authenticationState) {
            AuthenticationState.AUTHENTICATED -> {
                if (loginStatusText.isNotEmpty()) {
                    SnackbarHelper.show(this, loginStatusText, ToastMessage.Type.SUCCESS)
                    loginStatusText = ""
                }
                if (shouldDelayOnForegroundEvent.getAndSet(false)) {
                    applyOnForegroundEvent()
                }
            }
            AuthenticationState.LOGOUT -> {
                // Logout performed
                if (!BaseApp.runtimeDataHolder.guestLogin) {
                    startLoginActivity()
                }
            }
            else -> {
            }
        }
    }

    private fun checkPendingTasks() {
        // User is editing the action, don't try to send it now
        if (currentNavigationFragment !is ActionParametersFragment) {
            sendPendingTasks()
        }
    }

    override fun setupActionsMenu(
        menu: Menu,
        actions: List<Action>,
        actionNavigable: ActionNavigable,
        onSort: (action: Action) -> Unit
    ) {
        (menu as? MenuBuilder)?.setOptionalIconsVisible(true)

        val withIcons = actions.firstOrNull { it.getIconDrawablePath() != null } != null
        var order = 0
        actions.forEach { action ->
            val drawable = if (withIcons) ActionUIHelper.getActionIconDrawable(this, action) else null
            drawable?.paramMenuActionDrawable(this)

            // not giving a simple string because we want a divider before pending tasks
            menu.add(0, action.hashCode(), order, action.getPreferredName())
                .setOnMenuItemClickListener {
                    if (action.isSortAction()) {
                        onSort(action)
                    } else {
                        onActionClick(action, actionNavigable)
                    }
                    true
                }
                .setIcon(drawable)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

            order++
        }

        // Ps: if we have only one action and having (preset = sort) it should be applied by default and already deleted from actions list)
        // Check if actions is not empty and contains at least one regular (non sort or openUrl) action
        if (actions.isNotEmpty() && actions.firstOrNull { !(it.isSortAction() || it.isOpenUrlAction()) } != null) {
            // Add pendingTasks menu item at the end
            val drawable =
                if (withIcons) ContextCompat.getDrawable(this, R.drawable.pending_actions) else null
            drawable?.paramMenuActionDrawable(this)

            // not giving a simple string because we want a divider before pending tasks
            menu.add(1, Random().nextInt(), order, pendingTaskString)
                .setOnMenuItemClickListener {
                    actionNavigable.navigateToPendingTasks()
                    true
                }
                .setIcon(drawable?.adjustActionDrawableMargins())
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
            menu.setGroupDividerEnabled(true)
        }
    }

    override fun onActionClick(action: Action, actionNavigable: ActionNavigable) {
        when {
            action.isOpenUrlAction() -> {
                // action.description contains the url if openUrl action
                action.description?.let {
                    val base64EncodedContext = ActionHelper.getBase64EncodedContext(
                        actionNavigable.getActionContent(
                            actionUUID = action.uuid,
                            itemId = if (action.scope == Action.Scope.CURRENT_RECORD) {
                                (currentEntity?.__entity as EntityModel?)?.__KEY
                            } else {
                                ""
                            }
                        )
                    )
                    currentNavigationFragment?.setSharedAxisZExitTransition()
                    BaseApp.genericNavigationResolver.navigateToActionWebView(
                        fragmentActivity = this,
                        path = it,
                        actionName = action.name,
                        actionLabel = action.label,
                        actionShortLabel = action.shortLabel,
                        base64EncodedContext = base64EncodedContext
                    )
                }
            }
            action.parameters.length() > 0 -> {
                if (action.scope == Action.Scope.TABLE) {
                    currentEntity = null
                }
                actionNavigable.navigateToActionForm(action, (currentEntity?.__entity as? EntityModel)?.__KEY)
            }
            else -> {
                val task = ActionTask(
                    status = ActionTask.Status.PENDING,
                    date = Date(),
                    relatedItemId = (currentEntity?.__entity as? EntityModel)?.__KEY,
                    label = action.getPreferredName(),
                    actionInfo = ActionInfo(
                        actionName = action.name,
                        tableName = actionNavigable.tableName,
                        actionUUID = action.uuid,
                        isOfflineCompatible = action.isOfflineCompatible(),
                        preferredShortName = action.getPreferredShortName()
                    )
                )
                task.actionContent =
                    actionNavigable.getActionContent(task.id, (currentEntity?.__entity as? EntityModel)?.__KEY)

                sendAction(task, actionNavigable.tableName) {
                    // Nothing to do
                }
            }
        }
    }

    override fun setCurrentEntityModel(roomEntity: RoomEntity?) {
        currentEntity = roomEntity
    }

    override fun sendAction(
        actionTask: ActionTask,
        tableName: String,
        proceed: () -> Unit
    ) {
        if (actionTask.actionInfo.isOfflineCompatible) {
            taskViewModel.insert(actionTask)
        }

        queryNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                val savedActionContent = actionTask.actionContent
                val cleanedActionContent = actionTask.cleanActionContent()
                entityListViewModelList.firstOrNull()
                    ?.sendAction(actionTask.actionInfo.actionName, cleanedActionContent) { actionResponse ->
                        actionResponse?.let {
                            if (actionResponse.success) {
                                actionTask.status = ActionTask.Status.SUCCESS
                            } else {
                                actionTask.status = ActionTask.Status.ERROR_SERVER
                                actionTask.actionContent = savedActionContent
                            }

                            actionTask.message = actionResponse.statusText
                            actionTask.actionInfo.errors =
                                actionResponse.errors?.associateBy({ it.parameter }, { it.message })
                            taskViewModel.insert(actionTask)

                            if (actionResponse.dataSynchro == true) {
                                requestDataSync(tableName)
                            }
                        }
                        proceed()
                    }
            }

            override fun onServerInaccessible() {
                onServerInaccessible(tableName, isFromAction = true)
            }

            override fun onNoInternet() {
                onNoInternet(tableName, isFromAction = true)
            }
        })
    }

    override fun uploadImage(
        bodies: Map<String, RequestBody?>,
        tableName: String,
        isFromAction: Boolean,
        taskToSendIfOffline: ActionTask?,
        onImageUploaded: (parameterName: String, receivedId: String) -> Unit,
        onAllUploadFinished: () -> Unit
    ) {
        queryNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                entityListViewModelList.firstOrNull()?.uploadImage(
                    imagesToUpload = bodies,
                    onImageUploaded = { parameterName, receivedId ->
                        onImageUploaded(parameterName, receivedId)
                    },
                    onError = {
                        taskToSendIfOffline?.let { taskViewModel.insert(it) }
                    }
                ) {
                    onAllUploadFinished()
                }
            }

            override fun onServerInaccessible() {
                onServerInaccessible(tableName, isFromAction)
                taskToSendIfOffline?.let { taskViewModel.insert(it) }
            }

            override fun onNoInternet() {
                onNoInternet(tableName, isFromAction)
                taskToSendIfOffline?.let { taskViewModel.insert(it) }
            }
        })
    }

    override fun handleNetworkState(networkState: NetworkState) {
        when (networkState) {
            NetworkState.CONNECTED -> {
                // Setting the authenticationState to its initial value
                if (isAlreadyLoggedIn()) {
                    loginViewModel.setAuthenticationState(AuthenticationState.AUTHENTICATED)
                    checkPendingTasks()
                }

                // If guest and not yet logged in, auto login
                if (!isAlreadyLoggedIn() && BaseApp.runtimeDataHolder.guestLogin && authenticationRequested) {
                    authenticationRequested = false
                    tryAutoLogin()
                    checkPendingTasks()
                }
            }
            else -> {}
        }
    }

    // Observe any toast message from Entity Detail
    override fun observeEntityToastMessage(message: SharedFlow<Event<ToastMessage.Holder>>) {
        mainActivityObserver.observeEntityToastMessage(message)
    }

    override fun getTaskViewModel(): TaskViewModel {
        return taskViewModel
    }

    /**
     * Goes back to login page
     */
    fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(LOGGED_OUT, true)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
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
            SnackbarHelper.show(this, getString(R.string.no_internet_auto_login), ToastMessage.Type.WARNING)
        }
    }

    /**
     * Called on first creation and when restoring state.
     */
    private fun setupBottomNavigationBar() {
        binding.bottomNav.menu.clear() // clear old inflated items.
        BaseApp.bottomNavigationMenu?.let {
            binding.bottomNav.inflateMenu(it)
        }

        val navGraphIds = BaseApp.navGraphIds

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = binding.bottomNav.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_container,
            intent = intent
        ) {
            binding.appbar.setExpanded(true, true)
        }
        // Whenever the selected controller changes, setup the action bar.
        controller.observe(
            this
        ) { navController ->
            setupActionBarWithNavController(navController)
        }
        currentNavController = controller
    }

    override fun sendPendingTasks() {
        queryNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                taskViewModel.pendingTasks.value?.forEach { pendingTask ->
                    sendSinglePendingTask(pendingTask)
                }
            }

            override fun onServerInaccessible() {
                onServerInaccessible("")
            }

            override fun onNoInternet() {
                onNoInternet("")
            }
        })
    }

    private fun sendSinglePendingTask(pendingTask: ActionTask) {
        val images = pendingTask.actionInfo.imagesToUpload
        if (images.isNullOrEmpty()) {
            sendAction(pendingTask, pendingTask.actionInfo.tableName) {
                // Nothing to do
            }
        } else {
            val bodies = images.getBodies(this)

            uploadImage(
                bodies = bodies,
                tableName = "PendingTasks", // just for logs
                isFromAction = true,
                taskToSendIfOffline = null,
                onImageUploaded = { parameterName, receivedId ->
                    pendingTask.actionInfo.paramsToSubmit?.set(parameterName, receivedId)
                    pendingTask.actionInfo.metaDataToSubmit?.set(parameterName, UPLOADED_METADATA_STRING)
                },
                onAllUploadFinished = {
                    sendAction(pendingTask, pendingTask.actionInfo.tableName) {
                        // Nothing to do
                    }
                }
            )
        }
    }

    override fun setFullScreenMode(isFullScreen: Boolean) {
        setMaterialFadeTransition(binding.mainContainer, true)
        if (isFullScreen) {
            binding.collapsingToolbar.visibility = View.GONE
            binding.bottomNav.visibility = View.GONE
        } else {
            binding.collapsingToolbar.visibility = View.VISIBLE
            binding.bottomNav.visibility = View.VISIBLE
        }
    }

    override fun onBackPressed() {
        // If fullscreen fragment, set back ActionBar
        when (currentNavigationFragment) {
            is BarcodeScannerFragment, is ActionWebViewFragment -> setFullScreenMode(false)
        }
        super.onBackPressed()
    }

    override fun logout() {
        // delete data of table that has user specific queries
        entityListViewModelList.forEach { entityListViewModel ->
            val hasUserQuery =
                BaseApp.runtimeDataHolder.tableInfo[entityListViewModel.getAssociatedTableName()]?.hasUserQuery()
                    ?: false
            if (hasUserQuery) {
                entityListViewModel.deleteAll()
                entityListViewModel.resetGlobalStamp()
            }
        }
        taskViewModel.deleteAll()
        loginViewModel.disconnectUser {}
    }

    override fun checkNetwork(networkChecker: NetworkChecker) {
        queryNetwork(networkChecker)
    }

    internal fun setCheckInProgress(inProgress: Boolean) {
        binding.linearProgressIndicator.visibility = if (inProgress) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}
