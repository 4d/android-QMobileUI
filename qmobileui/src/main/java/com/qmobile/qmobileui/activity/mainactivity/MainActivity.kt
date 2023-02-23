/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import com.qmobile.qmobileapi.auth.AuthenticationState
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.model.error.AuthorizedStatus
import com.qmobile.qmobileapi.utils.DeviceInfo
import com.qmobile.qmobileapi.utils.LoginRequiredCallback
import com.qmobile.qmobileapi.utils.UploadHelper.UPLOADED_METADATA_STRING
import com.qmobile.qmobileapi.utils.UploadHelper.getBodies
import com.qmobile.qmobileapi.utils.getSafeBoolean
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
import com.qmobile.qmobiledatasync.viewmodel.ActionViewModel
import com.qmobile.qmobiledatasync.viewmodel.DeletedRecordsViewModel
import com.qmobile.qmobiledatasync.viewmodel.PushViewModel
import com.qmobile.qmobiledatasync.viewmodel.TaskViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getActionViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getDeletedRecordsViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getPushViewModel
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.FeedbackActivity
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.SettingsActivity
import com.qmobile.qmobileui.action.ActionNavigable
import com.qmobile.qmobileui.action.actionparameters.ActionParametersFragment
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.utils.ActionHelper
import com.qmobile.qmobileui.action.utils.ActionHelper.cleanActionContent
import com.qmobile.qmobileui.action.utils.ActionUIHelper
import com.qmobile.qmobileui.action.utils.ActionUIHelper.getMenuDrawable
import com.qmobile.qmobileui.action.utils.ActionUIHelper.setMenuItemColorFilter
import com.qmobile.qmobileui.activity.BaseActivity
import com.qmobile.qmobileui.activity.loginactivity.LoginActivity
import com.qmobile.qmobileui.binding.ImageHelper
import com.qmobile.qmobileui.binding.px
import com.qmobile.qmobileui.crash.SignalHandler
import com.qmobile.qmobileui.databinding.ActivityMainBinding
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.ui.NoSwipeBehavior
import com.qmobile.qmobileui.ui.SnackbarHelper
import com.qmobile.qmobileui.ui.noTabLayoutUI
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

@Suppress("LargeClass")
class MainActivity :
    BaseActivity(),
    FragmentCommunication,
    SettingsActivity,
    LifecycleEventObserver,
    ActionActivity,
    FeedbackActivity {

    private var loginStatusText = ""
    private var onLaunch = true
    private var authenticationRequested = true
    private var shouldDelayOnForegroundEvent = AtomicBoolean(false)
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainActivityDataSync: MainActivityDataSync
    private lateinit var mainActivityObserver: MainActivityObserver
    private lateinit var actionViewModel: ActionViewModel
    private lateinit var pushViewModel: PushViewModel
    internal lateinit var deletedRecordsViewModel: DeletedRecordsViewModel
    private lateinit var signalHandler: SignalHandler

    private var isFullScreen = false
    private var snackbar: Snackbar? = null
    private var snackBarRequired = false
    private var licenseCheckRequired = AtomicBoolean(BaseApp.sharedPreferencesHolder.guestLogin)

    private var currentEntity: RoomEntity? = null
    private var authorizedStatus = AuthorizedStatus.AUTHORIZED
    private val logoutRequested = AtomicBoolean(false)
    private val pushTokenToBeSent = AtomicBoolean(false)
    private var pushDataSync = false
    private val tabLayoutSetup = AtomicBoolean(false)

    override val activityResultControllerImpl = ActivityResultControllerImpl(this)
    override val permissionCheckerImpl = PermissionCheckerImpl(this)

    val navController: NavController
        get() = (supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment).navController

    val currentNavigationFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.nav_host_container)
            ?.childFragmentManager
            ?.fragments
            ?.first()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        signalHandler = SignalHandler(this).apply {
            initSignalHandler()
        }

        setupUI()

        // Init system services in onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        pushDataSync = intent.getBooleanExtra(PUSH_DATA_SYNC, false)
        Timber.i("pushDataSync: $pushDataSync")

        // Init ApiClients
        refreshAllApiClients()

        initViewModels()

        if (savedInstanceState == null) {
            // Retrieve bundled parameter to know if there was a successful login with statusText
            loginStatusText = intent.getStringExtra(LOGIN_STATUS_TEXT) ?: ""

            if (!pushDataSync || !isConnected()) {
                setupTabLayout()
            }
        } // Else, need to wait for onRestoreInstanceState

        mainActivityObserver =
            MainActivityObserver(
                this,
                entityListViewModelList,
                actionViewModel,
                pushViewModel,
                taskViewModel
            ).apply {
                initObservers()
            }

        mainActivityDataSync = MainActivityDataSync(this)

        // Follow activity lifecycle and check when activity enters foreground for data sync
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        if (licenseCheckRequired.get()) {
            performLicenseCheck()
        }

        if (BaseApp.runtimeDataHolder.pushNotification) {
            askNotificationsPermission()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        signalHandler.clearSignalHandler()
    }

    override fun initViewModels() {
        super.initViewModels()
        actionViewModel = getActionViewModel(this, apiService)
        pushViewModel = getPushViewModel(this, apiService)
        deletedRecordsViewModel = getDeletedRecordsViewModel(this, apiService)
    }

    private fun setupUI() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (noTabLayoutUI) {
            binding.scrollableTabLayout.visibility = View.GONE
        }

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

        if (!noTabLayoutUI) {
            binding.scrollableTabLayout.applyInsetter {
                type(navigationBars = true) {
                    padding(animated = true)
                }
            }

            binding.scrollableTabLayout.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this))

            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
                val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
                when {
                    imeVisible -> binding.scrollableTabLayout.visibility = View.GONE
                    isFullScreen -> binding.scrollableTabLayout.visibility = View.GONE
                    else -> binding.scrollableTabLayout.visibility = View.VISIBLE
                }
                insets
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        snackbar = SnackbarHelper.build(
            this@MainActivity,
            getString(R.string.max_device_connection_reached),
            ToastMessage.Type.ERROR,
            Snackbar.LENGTH_INDEFINITE,
            NoSwipeBehavior()
        )

        snackbar?.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                if (snackBarRequired) {
                    snackbar?.show()
                }
            }
        })
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Now that TabLayout has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // TabLayout with Navigation
        setupTabLayout()
    }

    // Back button from appbar
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    /**
     * If remoteUrl has been changed, we refresh ApiClient with new parameters (which are stored
     * in SharedPreferences)
     */
    override fun refreshAllApiClients() {
        // Interceptor notifies the MainActivity that we need to go to login page. First, stop syncing
        val loginRequiredCallbackForInterceptor: LoginRequiredCallback = {
            if (BaseApp.runtimeDataHolder.guestLogin) {
                dismissSnackbar()
            } else {
                mainActivityDataSync.dataSync.loginRequired.set(true)
            }
        }

        super.refreshApiClients(loginRequiredCallbackForInterceptor)
        if (::deletedRecordsViewModel.isInitialized) {
            deletedRecordsViewModel.refreshRestRepository(apiService)
        }
        if (::actionViewModel.isInitialized) {
            actionViewModel.refreshActionRepository(apiService)
        }
        if (::pushViewModel.isInitialized) {
            pushViewModel.refreshPushRepository(apiService)
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
                    onStartCallback()
                } else {
                    Timber.d("[${Lifecycle.Event.ON_START} - Delayed event, waiting for authentication]")
                    shouldDelayOnForegroundEvent.set(true)
                }
            }
            else -> {
            }
        }
    }

    private fun onStartCallback() {
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
    override fun requestDataSync(currentTableName: String?) {
        val entityListViewModel = if (currentTableName != null) {
            entityListViewModelList.find { it.getAssociatedTableName() == currentTableName }
        } else {
            entityListViewModelList.firstOrNull()
        }
        val tableName = entityListViewModel?.getAssociatedTableName() ?: ""

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
                            setCheckInProgress(true)
                            entityListViewModel.getEntities(false) { _, shouldSyncData ->
                                setCheckInProgress(false)
                                if (shouldSyncData) {
                                    mainActivityDataSync.shouldDataSync(tableName)
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
                cancelPushDataSync()
                onServerInaccessible(tableName)
            }

            override fun onNoInternet() {
                cancelPushDataSync()
                onNoInternet(tableName)
            }
        })
    }

    internal fun onServerInaccessible(tableName: String, isFromAction: Boolean = false) {
        if (isFromAction) {
            connectivityViewModel.toastMessage.showMessage(
                getString(R.string.action_send_server_not_accessible),
                tableName,
                ToastMessage.Type.NEUTRAL
            )
            navController.navigateUp()
        } else {
            connectivityViewModel.toastMessage
                .showMessage(
                    getString(R.string.server_not_accessible),
                    tableName,
                    ToastMessage.Type.ERROR
                )
        }
    }

    internal fun onNoInternet(tableName: String, isFromAction: Boolean = false) {
        if (isFromAction) {
            connectivityViewModel.toastMessage.showMessage(
                getString(R.string.action_send_no_internet),
                tableName,
                ToastMessage.Type.NEUTRAL
            )
            navController.navigateUp()
        } else {
            connectivityViewModel.toastMessage.showMessage(
                getString(R.string.no_internet),
                tableName,
                ToastMessage.Type.ERROR
            )
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
                    onStartCallback()
                }

                getCurrentFCMToken()

                if (pushDataSync) {
                    requestDataSync(null)
                }
            }
            AuthenticationState.LOGOUT -> {
                if (!isAlreadyLoggedIn()) {
                    if (BaseApp.runtimeDataHolder.guestLogin) {
                        tryAutoLogin()
                    } else {
                        startLoginActivity(authorizedStatus)
                    }
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

    @SuppressLint("RestrictedApi")
    override fun setupActionsMenu(
        menu: Menu,
        actions: List<Action>,
        actionNavigable: ActionNavigable,
        onSort: (action: Action) -> Unit
    ) {
        (menu as? MenuBuilder)?.setOptionalIconsVisible(true)

        val withIcons =
            actions.firstOrNull { it.getIconDrawablePath() != null } != null || actions.isEmpty()
        var order = 0
        actions.forEach { action ->
            val drawable =
                if (withIcons) {
                    ActionUIHelper.getActionIconDrawable(
                        this,
                        action,
                        ImageHelper.DRAWABLE_24.px
                    )
                } else {
                    null
                }
            drawable?.setMenuItemColorFilter(this)

            val isEnabled = action.isOfflineCompatible() || connectivityViewModel.isConnected()
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
                .setEnabled(isEnabled)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

            order++
        }

        // If we have only one action and having (preset = sort) it should be applied by default and already deleted from actions list)
        // Check if actions is not empty and contains at least one regular (non sort or openUrl) action
        if (actions.isNotEmpty() && actions.firstOrNull { !(it.isSortAction() || it.isOpenUrlAction()) } != null) {
            // Add pendingTasks menu item at the end
            val drawable =
                if (withIcons) getMenuDrawable(this, R.drawable.pending_actions) else null

            // not giving a simple string because we want a divider before pending tasks
            menu.add(1, Random().nextInt(), order, getString(R.string.pending_task_menu_item))
                .setOnMenuItemClickListener {
                    actionNavigable.navigateToPendingTasks()
                    true
                }
                .setIcon(drawable)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }

        // Add Settings
        val drawable =
            if (withIcons) getMenuDrawable(this, R.drawable.settings) else null

        menu.add(1, Random().nextInt(), order, getString(R.string.settings_navbar_title))
            .setOnMenuItemClickListener {
                BaseApp.genericNavigationResolver.navigateToSettings(this)
                true
            }
            .setIcon(drawable)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
            menu.setGroupDividerEnabled(true)
        }
    }

    override fun onActionClick(action: Action, actionNavigable: ActionNavigable) {
        // Avoid sending primary key on table scope request
        if (action.scope == Action.Scope.TABLE) {
            currentEntity = null
        }

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
                actionNavigable.navigateToActionForm(action, (currentEntity?.__entity as? EntityModel)?.__KEY)
            }
            action.isOfflineCompatible() || connectivityViewModel.isConnected() -> {
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
                    actionNavigable.getActionContent(
                        task.id,
                        (currentEntity?.__entity as? EntityModel)?.__KEY
                    )

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

                actionViewModel.sendAction(
                    actionTask.actionInfo.actionName,
                    cleanedActionContent
                ) { _, actionResponse ->
                    if (actionResponse != null) {
                        if (actionResponse.success) {
                            actionTask.status = ActionTask.Status.SUCCESS
                            actionResponse.share?.firstOrNull()?.value?.let { value ->
                                shareInfo(value)
                            }
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

    fun shareInfo(value: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, value)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
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
                actionViewModel.uploadImage(
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
                if (licenseCheckRequired.get()) {
                    performLicenseCheck()
                }

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

                if (pushTokenToBeSent.get()) {
                    sendPushTokenToServer()
                }
            }
            else -> {}
        }
    }

    // Observe any toast message from Entity Detail
    override fun observeEntityToastMessage(message: SharedFlow<Event<ToastMessage.Holder>>) {
        mainActivityObserver.observeEntityToastMessage(message)
    }

    override fun getTaskVM(): TaskViewModel {
        return taskViewModel
    }

    /**
     * Goes back to login page
     */
    private fun startLoginActivity(authorizedStatus: AuthorizedStatus) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(LOGGED_OUT, true)
        intent.putExtra(AUTHORIZED_STATUS, authorizedStatus)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun performLicenseCheck() {
        if (BaseApp.sharedPreferencesHolder.device.getSafeBoolean(DeviceInfo.SIMULATOR) != true) {
            queryNetwork(object : NetworkChecker {
                override fun onServerAccessible() {
                    if (licenseCheckRequired.getAndSet(false)) {
                        loginViewModel.checkLicenses { isOk ->
                            if (isOk) {
                                dismissSnackbar()
                            } else {
                                showSnackbar()
                            }
                        }
                    }
                }

                override fun onServerInaccessible() {
                    Timber.d(getString(R.string.server_not_accessible))
                }

                override fun onNoInternet() {
                    Timber.d(getString(R.string.no_internet))
                }
            })
        }
    }

    private fun showSnackbar() {
        if (snackbar?.isShown == false) {
            snackBarRequired = true
            snackbar?.show()
        }
    }

    private fun dismissSnackbar() {
        snackBarRequired = false
        snackbar?.dismiss()
        resetViewModelIsUnauthorized()
    }

    /**
     * Tries to login while in guest mode. Might fail if no Internet connection
     */
    private fun tryAutoLogin() {
        queryNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                loginViewModel.login { success, isMaxLicenseReached ->
                    when {
                        success -> {
                            logoutRequested.set(false)
                            resetViewModelIsUnauthorized()
                        }
                        isMaxLicenseReached -> {
                            showSnackbar()
                        }
                    }
                }
            }

            override fun onServerInaccessible() {
                cancelPushDataSync()
                authenticationRequested = true
                Timber.d("No Internet connection, authenticationRequested")
                SnackbarHelper.show(
                    this@MainActivity,
                    getString(R.string.server_not_accessible_auto_login),
                    ToastMessage.Type.WARNING
                )
            }

            override fun onNoInternet() {
                cancelPushDataSync()
                authenticationRequested = true
                Timber.d("No Internet connection, authenticationRequested")
                SnackbarHelper.show(
                    this@MainActivity,
                    getString(R.string.no_internet_auto_login),
                    ToastMessage.Type.WARNING
                )
            }
        })
    }

    private fun resetViewModelIsUnauthorized() {
        entityListViewModelList.forEach { it.setIsUnauthorizedState(false) }
    }

    /**
     * Called on first creation and when restoring state.
     */
    private fun setupTabLayout() {
        if (!tabLayoutSetup.getAndSet(true)) {
            // Setup the TabLayout with a list of navigation graphs
            val controller = binding.scrollableTabLayout.setupWithNavController(
                fragmentManager = supportFragmentManager,
                intent = intent
            ) {
                // If we are on a fullscreen activity and we change nav item, we need to cancel fullscreen mode
                setFullScreenMode(false)
            }
            // Whenever the selected controller changes, setup the action bar.
            controller.observe(
                this
            ) { navController ->
                setupActionBarWithNavController(navController)
            }
        }
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

    override fun isConnected(): Boolean {
        return connectivityViewModel.isConnected()
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
                    pendingTask.actionInfo.metaDataToSubmit?.set(
                        parameterName,
                        UPLOADED_METADATA_STRING
                    )
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
        this.isFullScreen = isFullScreen
        setMaterialFadeTransition(binding.mainContainer, true)
        if (isFullScreen) {
            binding.collapsingToolbar.visibility = View.GONE
            if (!noTabLayoutUI) {
                binding.scrollableTabLayout.visibility = View.GONE
            }
        } else {
            binding.collapsingToolbar.visibility = View.VISIBLE
            if (!noTabLayoutUI) {
                binding.scrollableTabLayout.visibility = View.VISIBLE
            }
        }
    }

    override fun logout(isUnauthorized: Boolean) {
        if (!logoutRequested.getAndSet(true) && BaseApp.sharedPreferencesHolder.sessionToken.isNotEmpty()) {
            when {
                !isUnauthorized -> {
                    authorizedStatus = AuthorizedStatus.AUTHORIZED
                    clearSpecificData()
                }
                taskViewModel.pendingTasks.value.isNullOrEmpty() -> {
                    authorizedStatus = AuthorizedStatus.UNAUTHORIZED
                }
                else -> {
                    authorizedStatus = AuthorizedStatus.UNAUTHORIZED_WITH_TASKS
                }
            }

            loginViewModel.disconnectUser(voluntaryLogout = !isUnauthorized) {}
        }
    }

    override fun checkNetwork(networkChecker: NetworkChecker, feedbackServer: Boolean) {
        queryNetwork(networkChecker, toastError = false, feedbackServer = feedbackServer)
    }

    internal fun setCheckInProgress(inProgress: Boolean) {
        binding.linearProgressIndicator.visibility = if (inProgress) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun sendPushTokenToServer(newToken: String? = null) {
        val token = newToken ?: BaseApp.sharedPreferencesHolder.fcmToken
        queryNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                pushViewModel.sendToken(token) { isSuccess ->
                    if (isSuccess) {
                        pushTokenToBeSent.set(false)
                        Timber.i("Push token successfully refreshed server side")
                    } else {
                        SnackbarHelper.show(
                            this@MainActivity,
                            getString(R.string.push_cannot_send),
                            ToastMessage.Type.WARNING
                        )
                    }
                }
            }

            override fun onServerInaccessible() {
                Timber.d(getString(R.string.server_not_accessible))
                pushTokenToBeSent.set(true)
            }

            override fun onNoInternet() {
                Timber.d(getString(R.string.no_internet))
                pushTokenToBeSent.set(true)
            }
        })
    }

    private fun askNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (this as? PermissionChecker)?.askPermission(
                context = this,
                permission = Manifest.permission.POST_NOTIFICATIONS,
                rationale = getString(R.string.permission_rationale_notifications)
            ) { isGranted ->
                Timber.i("Push notification permission : $isGranted")
                getCurrentFCMToken()
            }
        } else {
            Timber.i(
                "Push notification permission : true " +
                    "(SDK version (${Build.VERSION.SDK_INT}) <  Build.VERSION_CODES.TIRAMISU (33)"
            )
        }
    }

    internal fun getCurrentFCMToken() {
        if (BaseApp.runtimeDataHolder.pushNotification) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(
                OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Timber.w("Fetching FCM registration token failed", task.exception)
                        return@OnCompleteListener
                    }

                    // Get new FCM registration token
                    val token = task.result
                    Timber.d("New FCM Token : $token")
                    val oldToken = BaseApp.sharedPreferencesHolder.fcmToken
                    if (oldToken != token || pushTokenToBeSent.get()) {
                        sendPushTokenToServer(token)
                        BaseApp.sharedPreferencesHolder.fcmToken = token
                    }
                }
            )
        }
    }

    internal fun cancelPushDataSync() {
        pushDataSync = false
        setupTabLayout()
    }
}
