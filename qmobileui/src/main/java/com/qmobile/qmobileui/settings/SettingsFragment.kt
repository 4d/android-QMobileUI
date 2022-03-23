/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.settings

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobiledatastore.dao.ActionTaskDao
import com.qmobile.qmobiledatastore.dao.STATUS
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getLoginViewModel
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.network.RemoteUrlChange
import com.qmobile.qmobileui.utils.ToastHelper
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsFragment :
    PreferenceFragmentCompat(),
    BaseFragment,
    Preference.OnPreferenceClickListener,
    RemoteUrlChange {

    var firstTime = true
    private var logoutDialogTitle = ""
    private var logoutDialogMessage = ""
    private var logoutDialogMessageIfPendingTask = ""
    private var logoutDialogPositive = ""
    private var logoutDialogNegative = ""
    private var remoteUrlPref: Preference? = null
    private var pendingTaskPref: Preference? = null
    private var serverAccessibleDrawable: Drawable? = null
    private var serverNotAccessibleDrawable: Drawable? = null
    private lateinit var accountCategoryKey: String
    private lateinit var remoteUrlPrefKey: String
    private lateinit var pendingTaskPrefKey: String
    private lateinit var logoutPrefKey: String
    private lateinit var remoteUrl: String
    private lateinit var logoutDialogBuilder: MaterialAlertDialogBuilder

    // BaseFragment
    override lateinit var delegate: FragmentCommunication

    // UI strings
    private lateinit var noInternetString: String
    private lateinit var serverAccessibleString: String
    private lateinit var serverNotAccessibleString: String

    // ViewModels
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var connectivityViewModel: ConnectivityViewModel
    private lateinit var actionTaskDao: ActionTaskDao

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
        // Access resources elements
        remoteUrlPrefKey = resources.getString(R.string.pref_remote_url_key)
        pendingTaskPrefKey = resources.getString(R.string.pref_pending_tasks_key)
        accountCategoryKey = resources.getString(R.string.cat_account_key)
        logoutPrefKey = resources.getString(R.string.pref_logout_key)
        serverAccessibleDrawable = ContextCompat.getDrawable(context, R.drawable.network_ok_circle)
        serverNotAccessibleDrawable =
            ContextCompat.getDrawable(context, R.drawable.network_nok_circle)
        noInternetString = resources.getString(R.string.no_internet)
        serverAccessibleString = resources.getString(R.string.server_accessible)
        serverNotAccessibleString = resources.getString(R.string.server_not_accessible)

        logoutDialogBuilder = MaterialAlertDialogBuilder(
            context,
            R.style.TitleThemeOverlay_MaterialComponents_MaterialAlertDialog
        )

        logoutDialogTitle = resources.getString(R.string.logout_dialog_title)
        logoutDialogMessage = resources.getString(R.string.logout_dialog_message)
        logoutDialogMessageIfPendingTask =
            resources.getString(R.string.logout_dialog_message_if_pending_task)
        logoutDialogPositive = resources.getString(R.string.logout_dialog_positive)
        logoutDialogNegative = resources.getString(R.string.logout_dialog_negative)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        actionTaskDao = BaseApp.daoProvider.getActionTaskDao()
        loginViewModel = getLoginViewModel(activity, delegate.loginApiService)
        connectivityViewModel = getConnectivityViewModel(
            activity,
            delegate.connectivityManager,
            delegate.accessibilityApiService
        )
        initLayout()
        SettingsFragmentObserver(this, connectivityViewModel).initObservers()
    }

    /**
     * Initializes layout components
     */
    private fun initLayout() {
        this.remoteUrl = BaseApp.sharedPreferencesHolder.remoteUrl

        if (!BaseApp.runtimeDataHolder.guestLogin)
            findPreference<PreferenceCategory>(accountCategoryKey)?.isVisible = true
        findPreference<Preference>(logoutPrefKey)?.onPreferenceClickListener = this
        remoteUrlPref = findPreference(remoteUrlPrefKey)
        remoteUrlPref?.onPreferenceClickListener = this

        pendingTaskPref = findPreference(pendingTaskPrefKey)
        pendingTaskPref?.onPreferenceClickListener = this

        actionTaskDao.getAll().observe(viewLifecycleOwner, { allTasks ->
            val pendingTasks = allTasks.filter { actionTask -> actionTask.status == STATUS.PENDING }
            pendingTaskPref?.summary =
                getString(R.string.pref_pending_tasks_count, pendingTasks.size)
        })
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        preference?.let {
            return when (preference.key) {

                remoteUrlPrefKey -> {
                    delegate.showRemoteUrlEditDialog(remoteUrl, this)
                    true
                }
                logoutPrefKey -> {
                    // When Logout button is clicked, pop up a confirmation dialog
                    showLogoutDialog()
                    true
                }
                pendingTaskPrefKey -> {
                    BaseApp.genericNavigationResolver.navigateFromSettingsToTasks(requireActivity())
                    true
                }

                else -> {
                    false
                }
            }
        }
        return false
    }

    /**
     * Displays a dialog to confirm logout
     */
    private fun showLogoutDialog() {
        actionTaskDao.getAll().observe(
            viewLifecycleOwner,
            Observer {
                val nbPendingTask =
                    it.filter { actionTask -> actionTask.status == STATUS.PENDING }.count()
                logoutDialogBuilder
                    .setTitle(
                        if (nbPendingTask > 0) {
                            logoutDialogMessageIfPendingTask
                        } else {
                            logoutDialogTitle
                        }
                    )
                    .setMessage(logoutDialogMessage)
                    .setNegativeButton(logoutDialogNegative, null)
                    .setPositiveButton(logoutDialogPositive) { _, _ ->
                        logout()
                    }
                    .show()
            }
        )
    }

    /**
     * Tries to logout if user is authenticated and connected to Internet
     */
    private fun logout() {
        if (isReady()) {
            loginViewModel.disconnectUser {}
            lifecycleScope.launch {
                actionTaskDao.deleteAll()
            }
        } else {
            if (!connectivityViewModel.isConnected()) {
                activity?.let {
                    ToastHelper.show(
                        it,
                        it.resources.getString(R.string.no_internet),
                        MessageType.WARNING
                    )
                }
                Timber.d("No Internet connection")
            } else if (loginViewModel.authenticationState.value != AuthenticationStateEnum.AUTHENTICATED) {
                Timber.d("Not authenticated yet")
            }
        }
    }

    /**
     * Checks if environment is ready to perform an action
     */
    private fun isReady(): Boolean {
        if (loginViewModel.authenticationState.value == AuthenticationStateEnum.INVALID_AUTHENTICATION) {
            // For example server was not responding when trying to auto-login
            delegate.requestAuthentication()
            return false
        }
        return loginViewModel.authenticationState.value == AuthenticationStateEnum.AUTHENTICATED &&
            connectivityViewModel.isConnected()
    }

    override fun onServerAccessible() {
        remoteUrlPref?.summary =
            getString(R.string.remote_url_placeholder, remoteUrl, serverAccessibleString)
        remoteUrlPref?.icon = serverAccessibleDrawable
    }

    override fun onServerInaccessible() {
        remoteUrlPref?.summary =
            getString(R.string.remote_url_placeholder, remoteUrl, serverNotAccessibleString)
        remoteUrlPref?.icon = serverNotAccessibleDrawable
    }

    override fun onNoInternet() {
        remoteUrlPref?.summary =
            getString(R.string.remote_url_placeholder, remoteUrl, noInternetString)
        remoteUrlPref?.icon = serverNotAccessibleDrawable
    }

    override fun onValidRemoteUrlChange(newRemoteUrl: String) {
        BaseApp.sharedPreferencesHolder.remoteUrl = newRemoteUrl
        remoteUrl = newRemoteUrl
        this@SettingsFragment.delegate.refreshAllApiClients()
    }
}
