/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileapi.auth.AuthenticationState
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getLoginViewModel
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.SettingsActivity
import com.qmobile.qmobileui.action.utils.ActionHelper
import com.qmobile.qmobileui.activity.BaseActivity
import com.qmobile.qmobileui.feedback.FeedbackHandler
import com.qmobile.qmobileui.network.RemoteUrlChanger
import com.qmobile.qmobileui.ui.SnackbarHelper
import com.qmobile.qmobileui.ui.setSharedAxisXExitTransition
import com.qmobile.qmobileui.ui.setupToolbarTitle
import timber.log.Timber

class SettingsFragment :
    PreferenceFragmentCompat(),
    Preference.OnPreferenceClickListener,
    RemoteUrlChanger {

    internal var firstTime = true
    private val remoteUrlPrefKey by lazy { getString(R.string.pref_remote_url_key) }
    private val feedbackPrefKey by lazy { getString(R.string.pref_feedback_key) }
    private val accountCategoryKey by lazy { getString(R.string.cat_account_key) }
    private val pendingTaskPrefKey by lazy { getString(R.string.pref_pending_tasks_key) }
    private val logoutPrefKey by lazy { getString(R.string.pref_logout_key) }

    private val remoteUrlPref by lazy { findPreference<Preference>(remoteUrlPrefKey) }
    private val feedbackPref by lazy { findPreference<Preference>(feedbackPrefKey) }
    internal val pendingTaskPref by lazy { findPreference<Preference>(pendingTaskPrefKey) }
    private lateinit var remoteUrl: String

    private lateinit var settingsActivity: SettingsActivity
    internal lateinit var actionActivity: ActionActivity
    internal lateinit var delegate: FragmentCommunication

    // ViewModels
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var connectivityViewModel: ConnectivityViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SettingsActivity) {
            settingsActivity = context
        }
        if (context is FragmentCommunication) {
            delegate = context
        }
        if (context is ActionActivity) {
            actionActivity = context
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setupToolbarTitle(getString(R.string.settings_navbar_title))
        loginViewModel = getLoginViewModel(activity, settingsActivity.loginApiService)

        connectivityViewModel = getConnectivityViewModel(
            activity,
            settingsActivity.connectivityManager,
            settingsActivity.accessibilityApiService
        )

        initLayout()
        SettingsFragmentObserver(this, connectivityViewModel).initObservers()
    }

    /**
     * Initializes layout components
     */
    private fun initLayout() {
        this.remoteUrl = BaseApp.sharedPreferencesHolder.remoteUrl

        if (!BaseApp.runtimeDataHolder.guestLogin) {
            findPreference<PreferenceCategory>(accountCategoryKey)?.isVisible = true
        }
        findPreference<Preference>(logoutPrefKey)?.onPreferenceClickListener = this
        remoteUrlPref?.onPreferenceClickListener = this
        pendingTaskPref?.onPreferenceClickListener = this
        feedbackPref?.onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        return when (preference.key) {
            remoteUrlPrefKey -> {
                settingsActivity.showRemoteUrlEditDialog(remoteUrl, this)
                true
            }
            logoutPrefKey -> {
                // When Logout button is clicked, pop up a confirmation dialog
                showLogoutDialog()
                true
            }
            pendingTaskPrefKey -> {
                activity?.let {
                    setSharedAxisXExitTransition()
                    BaseApp.genericNavigationResolver.navigateToPendingTasks(
                        fragmentActivity = it,
                        tableName = "",
                        currentItemId = ""
                    )
                }
                true
            }
            feedbackPrefKey -> {
                (activity as? BaseActivity)?.apply {
                    FeedbackHandler(this)
                }
                true
            }
            else -> {
                false
            }
        }
    }

    /**
     * Displays a dialog to confirm logout
     */
    private fun showLogoutDialog() {
        val nbPendingTask = actionActivity.getTaskVM().pendingTasks.value?.size ?: 0
        val title = if (nbPendingTask > 0) {
            getString(R.string.logout_dialog_message_if_pending_task)
        } else {
            getString(R.string.logout_dialog_title)
        }
        // actionActivity.getTaskVM().pendingTasks.value?.filter { it.isPending() && ActionHelper.match(it, entity = )}


        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_QMobile_MaterialAlertDialog_FullWidthButtons)
            .setTitle(title)
            .setMessage(getString(R.string.logout_dialog_message))
            .setNegativeButton(getString(R.string.logout_dialog_negative), null)
            .setPositiveButton(getString(R.string.logout_dialog_positive)) { _, _ ->
                logout()
            }
            .show()
    }

    /**
     * Tries to logout if user is authenticated and connected to Internet
     */
    private fun logout() {
        when {
            isReady() -> {
                settingsActivity.logout(false)
            }
            !connectivityViewModel.isConnected() -> {
                SnackbarHelper.show(activity, getString(R.string.no_internet), ToastMessage.Type.WARNING)
                Timber.d("No Internet connection")
            }
            loginViewModel.authenticationState.value != AuthenticationState.AUTHENTICATED -> {
                Timber.d("Not authenticated yet")
            }
        }
    }

    /**
     * Checks if environment is ready to perform an action
     */
    private fun isReady(): Boolean {
        if (loginViewModel.authenticationState.value == AuthenticationState.INVALID_AUTHENTICATION) {
            // For example server was not responding when trying to auto-login
            settingsActivity.requestAuthentication()
            return false
        }
        return loginViewModel.authenticationState.value == AuthenticationState.AUTHENTICATED &&
            connectivityViewModel.isConnected()
    }

    override fun onServerAccessible() {
        activity?.apply {
            remoteUrlPref?.summary =
                this.getString(
                    R.string.remote_url_placeholder,
                    remoteUrl,
                    getString(R.string.server_accessible)
                )
        }
        remoteUrlPref?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.network_ok_circle)
    }

    override fun onServerInaccessible() {
        activity?.apply {
            remoteUrlPref?.summary =
                this.getString(
                    R.string.remote_url_placeholder,
                    remoteUrl,
                    getString(R.string.server_not_accessible)
                )
        }
        remoteUrlPref?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.network_nok_circle)
    }

    override fun onNoInternet() {
        activity?.apply {
            remoteUrlPref?.summary =
                this.getString(R.string.remote_url_placeholder, remoteUrl, getString(R.string.no_internet))
        }
        remoteUrlPref?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.network_nok_circle)
    }

    override fun onValidRemoteUrlChange(newRemoteUrl: String) {
        BaseApp.sharedPreferencesHolder.remoteUrl = newRemoteUrl
        remoteUrl = newRemoteUrl
        remoteUrlPref?.summary = getString(R.string.remote_url_checking)
        this@SettingsFragment.settingsActivity.refreshAllApiClients()
    }
}
