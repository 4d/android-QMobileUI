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
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.auth.isRemoteUrlValid
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getConnectivityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getLoginViewModel
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.utils.ToastHelper
import timber.log.Timber

@Suppress("TooManyFunctions")
class SettingsFragment :
    PreferenceFragmentCompat(),
    BaseFragment,
    Preference.OnPreferenceClickListener {

    var firstTime = true
    private var logoutDialogTitle = ""
    private var logoutDialogMessage = ""
    private var logoutDialogPositive = ""
    private var logoutDialogNegative = ""
    private var prefRemoteUrlTitle = ""
    private var remoteUrlDialogPositive = ""
    private var remoteUrlDialogCancel = ""
    private var remoteUrlInvalid = ""
    private var remoteUrlPref: Preference? = null
    private var serverAccessibleDrawable: Drawable? = null
    private var serverNotAccessibleDrawable: Drawable? = null
    private lateinit var accountCategoryKey: String
    private lateinit var remoteUrlPrefKey: String
    private lateinit var logoutPrefKey: String
    private lateinit var remoteUrl: String
    private lateinit var remoteUrlEditDialogBuilder: MaterialAlertDialogBuilder
    private lateinit var logoutDialogBuilder: MaterialAlertDialogBuilder
    private lateinit var shakeAnimation: Animation
    private lateinit var remoteUrlEditLayout: TextInputLayout
    private lateinit var remoteUrlEditEditText: TextInputEditText
    private lateinit var remoteUrlEditDialog: View

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

        logoutDialogBuilder = MaterialAlertDialogBuilder(
            context,
            R.style.TitleThemeOverlay_MaterialComponents_MaterialAlertDialog
        )
        remoteUrlEditDialogBuilder = MaterialAlertDialogBuilder(
            context,
            R.style.TitleThemeOverlay_MaterialComponents_MaterialAlertDialog
        )
        shakeAnimation = AnimationUtils.loadAnimation(context, R.anim.shake)

        remoteUrlEditDialog = LayoutInflater.from(context)
            .inflate(
                R.layout.remote_url_edit_dialog,
                view?.findViewById(android.R.id.content),
                false
            )
        remoteUrlEditLayout = remoteUrlEditDialog.findViewById(R.id.remote_url_edit_layout)
        remoteUrlEditEditText = remoteUrlEditDialog.findViewById(R.id.remote_url_edit_edittext)
        prefRemoteUrlTitle = getString(R.string.pref_remote_url_title)
        remoteUrlDialogPositive = getString(R.string.remote_url_dialog_positive)
        remoteUrlDialogCancel = getString(R.string.remote_url_dialog_cancel)
        remoteUrlInvalid = resources.getString(R.string.remote_url_invalid)

        logoutDialogTitle = resources.getString(R.string.logout_dialog_title)
        logoutDialogMessage = resources.getString(R.string.logout_dialog_message)
        logoutDialogPositive = resources.getString(R.string.logout_dialog_positive)
        logoutDialogNegative = resources.getString(R.string.logout_dialog_negative)
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
        remoteUrlPref?.onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        preference?.let {
            return when (preference.key) {

                remoteUrlPrefKey -> {
                    showRemoteUrlEditDialog()
                    true
                }
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

    private fun showRemoteUrlEditDialog() {

        remoteUrlEditLayout.editText?.setText(remoteUrl)
        remoteUrlEditLayout.error = null

        remoteUrlEditDialogBuilder
            .setView(remoteUrlEditDialog)
            .setTitle(prefRemoteUrlTitle)
            .setPositiveButton(remoteUrlDialogPositive, null)
            .setNegativeButton(remoteUrlDialogCancel, null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newRemoteUrl = remoteUrlEditLayout.editText?.text.toString()
                        if (newRemoteUrl.isRemoteUrlValid()) {
                            // When remoteUrl is changed, notify activity to refresh ApiClients
                            loginViewModel.authInfoHelper.remoteUrl = newRemoteUrl
                            remoteUrl = newRemoteUrl
                            this@SettingsFragment.delegate.refreshAllApiClients()
                            checkNetwork()
                            dismiss()
                        } else {
                            remoteUrlEditEditText.startAnimation(shakeAnimation)
                            remoteUrlEditLayout.error = remoteUrlInvalid
                        }
                    }
                }
                if (remoteUrlEditEditText.requestFocus()) {
                    window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
            }.show()
    }

    /**
     * Displays a dialog to confirm logout
     */
    private fun showLogoutDialog() {
        logoutDialogBuilder
            .setTitle(logoutDialogTitle)
            .setMessage(logoutDialogMessage)
            .setNegativeButton(logoutDialogNegative, null)
            .setPositiveButton(logoutDialogPositive) { _, _ ->
                logout()
            }
            .show()
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
        remoteUrlPref?.summary =
            getString(R.string.remote_url_placeholder, remoteUrl, noInternetString)
        remoteUrlPref?.icon = serverNotAccessibleDrawable
    }

    /**
     * Sets the indicator icon color and text to server not accessible
     */
    private fun setLayoutServerNotAccessible() {
        remoteUrlPref?.summary =
            getString(R.string.remote_url_placeholder, remoteUrl, serverNotAccessibleString)
        remoteUrlPref?.icon = serverNotAccessibleDrawable
    }

    /**
     * Sets the indicator icon color and text to server accessible
     */
    private fun setLayoutServerAccessible() {
        remoteUrlPref?.summary =
            getString(R.string.remote_url_placeholder, remoteUrl, serverAccessibleString)
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
}
