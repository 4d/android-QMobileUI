/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.settings

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.qmobile.qmobileapi.auth.AuthenticationState
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.utils.displaySnackBar
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("TooManyFunctions")
class SettingsFragment :
    PreferenceFragmentCompat(),
    BaseFragment,
    Preference.OnPreferenceClickListener,
    Preference.OnPreferenceChangeListener {

    var firstTime = true
    private var remoteUrlPref: Preference? = null
    private var serverAccessibleDrawable: Drawable? = null
    private var serverNotAccessibleDrawable: Drawable? = null
    private lateinit var accountCategoryKey: String
    private lateinit var remoteUrlPrefKey: String
    private lateinit var logoutPrefKey: String
    private lateinit var remoteUrl: String
    private lateinit var checkNetworkRequested: AtomicBoolean

    // BaseFragment
    override lateinit var delegate: FragmentCommunication

    // UI strings
    private lateinit var noInternetString: String
    private lateinit var serverAccessibleString: String
    private lateinit var serverNotAccessibleString: String

    // ViewModels
    lateinit var loginViewModel: LoginViewModel
    lateinit var connectivityViewModel: ConnectivityViewModel

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
        accountCategoryKey = resources.getString(R.string.cat_account_key)
        logoutPrefKey = resources.getString(R.string.pref_logout_key)
        serverAccessibleDrawable = ContextCompat.getDrawable(context, R.drawable.network_ok_circle)
        serverNotAccessibleDrawable =
            ContextCompat.getDrawable(context, R.drawable.network_nok_circle)
        noInternetString = resources.getString(R.string.no_internet)
        serverAccessibleString = resources.getString(R.string.server_accessible)
        serverNotAccessibleString = resources.getString(R.string.server_not_accessible)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        getViewModel()
        initLayout()
        setupObservers()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Every time we land on the fragment, we want refreshed data
        checkNetworkRequested = AtomicBoolean(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    /**
     * Initializes layout components
     */
    private fun initLayout() {
        this.remoteUrl = loginViewModel.authInfoHelper.remoteUrl

        if (!loginViewModel.authInfoHelper.guestLogin)
            findPreference<PreferenceCategory>(accountCategoryKey)?.isVisible = true
        findPreference<Preference>(logoutPrefKey)?.onPreferenceClickListener = this

        remoteUrlPref =
            findPreference(resources.getString(R.string.pref_remote_url_key))
        remoteUrlPref?.setDefaultValue(this.remoteUrl)
        remoteUrlPref?.onPreferenceChangeListener = this
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        preference?.let {
            return when (preference.key) {
                logoutPrefKey -> {
                    // When Logout button is clicked, pop up a confirmation dialog
                    showLogoutDialog()
                    true
                }
                else -> {
                    false
                }
            }
        }
        return false
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        preference?.let {
            when (preference.key) {
                remoteUrlPrefKey -> {
                    // When remoteUrl is changed, notify activity to refresh ApiClients
                    val newRemoteUrl = newValue as String
                    loginViewModel.authInfoHelper.remoteUrl = newRemoteUrl
                    this.remoteUrl = newRemoteUrl
                    delegate.refreshApiClients()
                    remoteUrlPref?.setDefaultValue(newRemoteUrl)
                    checkNetworkRequested.set(true)
                    checkNetwork()
                }
            }
        }
        return true
    }

    /**
     * Displays a dialog to confirm logout
     */
    private fun showLogoutDialog() {
        activity?.let {
            AlertDialog.Builder(it)
                .setTitle(resources.getString(R.string.logout_dialog_title))
                .setMessage(resources.getString(R.string.logout_dialog_message))
                .setPositiveButton(resources.getString(R.string.logout_dialog_positive)) { _, _ ->
                    logout()
                }
                .setNegativeButton(resources.getString(R.string.logout_dialog_negative), null)
                .show()
        }
    }

    /**
     * Tries to logout if user is authenticated and connected to Internet
     */
    private fun logout() {
        if (isReady()) {
            loginViewModel.disconnectUser {}
        } else {
            if (!delegate.isConnected()) {
                activity?.let {
                    displaySnackBar(it, it.resources.getString(R.string.no_internet))
                }
                Timber.d("No Internet connection")
            } else if (loginViewModel.authenticationState.value != AuthenticationState.AUTHENTICATED) {
                activity?.let {
                    displaySnackBar(
                        it,
                        it.resources.getString(R.string.error_occurred_try_again)
                    )
                }
                Timber.d("Not authenticated yet")
            }
        }
    }

    /**
     * Checks network state, and adjust the indicator icon color
     */
    fun checkNetwork() {
        if (checkNetworkRequested.compareAndSet(true, false)) {
            if (delegate.isConnected()) {
                connectivityViewModel.checkAccessibility(this.remoteUrl)
            } else {
                setLayoutNoInternet()
            }
        }
    }

    /**
     * Sets the indicator icon color and text to no Internet status
     */
    fun setLayoutNoInternet() {
        remoteUrlPref?.summary = "$remoteUrl - $noInternetString"
        remoteUrlPref?.icon = serverNotAccessibleDrawable
    }

    /**
     * Sets the indicator icon color and text to server not accessible
     */
    fun setLayoutServerNotAccessible() {
        remoteUrlPref?.summary = "$remoteUrl - $serverNotAccessibleString"
        remoteUrlPref?.icon = serverNotAccessibleDrawable
    }

    /**
     * Sets the indicator icon color and text to server accessible
     */
    fun setLayoutServerAccessible() {
        remoteUrlPref?.summary = "$remoteUrl - $serverAccessibleString"
        remoteUrlPref?.icon = serverAccessibleDrawable
    }

    /**
     * Checks if environment is ready to perform an action
     */
    private fun isReady(): Boolean {
        if (loginViewModel.authenticationState.value == AuthenticationState.INVALID_AUTHENTICATION) {
            // For example server was not responding when trying to auto-login
            delegate.requestAuthentication()
            return false
        }
        return loginViewModel.authenticationState.value == AuthenticationState.AUTHENTICATED &&
            delegate.isConnected()
    }
}
