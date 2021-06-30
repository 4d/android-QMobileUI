/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.settings

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getLoginViewModel
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.utils.ToastHelper
import com.qmobile.qmobileui.utils.hideKeyboard
import timber.log.Timber

@Suppress("TooManyFunctions")
class SettingsFragment :
    PreferenceFragmentCompat(),
    BaseFragment,
    Preference.OnPreferenceClickListener,
    Preference.OnPreferenceChangeListener {

    var firstTime = true
    private var remoteUrlPref: EditTextPreference? = null
    private var serverAccessibleDrawable: Drawable? = null
    private var serverNotAccessibleDrawable: Drawable? = null
    private lateinit var accountCategoryKey: String
    private lateinit var remoteUrlPrefKey: String
    private lateinit var logoutPrefKey: String
    private lateinit var remoteUrl: String

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
        this.remoteUrl = loginViewModel.authInfoHelper.remoteUrl

        if (!loginViewModel.authInfoHelper.guestLogin)
            findPreference<PreferenceCategory>(accountCategoryKey)?.isVisible = true
        findPreference<Preference>(logoutPrefKey)?.onPreferenceClickListener = this

        remoteUrlPref = findPreference(remoteUrlPrefKey)
        remoteUrlPref?.setDefaultValue(this.remoteUrl)
        remoteUrlPref?.onPreferenceChangeListener = this

        remoteUrlPref?.setOnBindEditTextListener { editText ->
            editText.setSingleLine()
            editText.setSelection(editText.text.length)
            editText.imeOptions = EditorInfo.IME_ACTION_DONE
            editText.setOnEditorActionListener { textView, actionId, keyEvent ->
                if ((keyEvent != null && (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) ||
                    (actionId == EditorInfo.IME_ACTION_DONE)
                ) {
                    remoteUrlPref?.setDefaultValue(textView.text.toString())
                    hideKeyboard(activity)
                    dismissDialog()
                }
                false
            }
        }
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
                    delegate.refreshAllApiClients()
                    remoteUrlPref?.setDefaultValue(newRemoteUrl)
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
            MaterialAlertDialogBuilder(it)
                .setTitle(resources.getString(R.string.logout_dialog_title))
                .setMessage(resources.getString(R.string.logout_dialog_message))
                .setNegativeButton(resources.getString(R.string.logout_dialog_negative), null)
                .setPositiveButton(resources.getString(R.string.logout_dialog_positive)) { _, _ ->
                    logout()
                }
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
     * Checks network state, and adjust the indicator icon color
     */
    fun checkNetwork() {
        if (connectivityViewModel.isConnected()) {
            connectivityViewModel.isServerConnectionOk { isAccessible ->
                if (isAccessible) {
                    setLayoutServerAccessible()
                } else {
                    setLayoutServerNotAccessible()
                }
            }
        } else {
            setLayoutNoInternet()
        }
    }

    /**
     * Sets the indicator icon color and text to no Internet status
     */
    private fun setLayoutNoInternet() {
        remoteUrlPref?.summary = getString(R.string.remote_url_placeholder, remoteUrl, noInternetString)
        remoteUrlPref?.icon = serverNotAccessibleDrawable
    }

    /**
     * Sets the indicator icon color and text to server not accessible
     */
    private fun setLayoutServerNotAccessible() {
        remoteUrlPref?.summary = getString(R.string.remote_url_placeholder, remoteUrl, serverNotAccessibleString)
        remoteUrlPref?.icon = serverNotAccessibleDrawable
    }

    /**
     * Sets the indicator icon color and text to server accessible
     */
    private fun setLayoutServerAccessible() {
        remoteUrlPref?.summary = getString(R.string.remote_url_placeholder, remoteUrl, serverAccessibleString)
        remoteUrlPref?.icon = serverAccessibleDrawable
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

    private fun dismissDialog() {
        activity?.supportFragmentManager?.fragments?.firstOrNull()?.childFragmentManager
            ?.fragments?.forEach { fragment ->
                if (fragment is EditTextPreferenceDialogFragmentCompat) {
                    fragment.onDialogClosed(true)
                    fragment.dismiss()
                }
            }
    }
}
