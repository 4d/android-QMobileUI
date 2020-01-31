package com.qmarciset.androidmobileui.settings

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.qmarciset.androidmobileapi.auth.AuthenticationState
import com.qmarciset.androidmobileapi.connectivity.NetworkState
import com.qmarciset.androidmobileapi.connectivity.NetworkUtils
import com.qmarciset.androidmobileui.BaseFragment
import com.qmarciset.androidmobileui.FragmentCommunication
import com.qmarciset.androidmobileui.R
import com.qmarciset.androidmobileui.utils.displaySnackBar
import com.qmarciset.androidmobileui.viewmodel.ConnectivityViewModel
import com.qmarciset.androidmobileui.viewmodel.LoginViewModel
import timber.log.Timber

class SettingsFragment : PreferenceFragmentCompat(), BaseFragment,
    Preference.OnPreferenceClickListener,
    Preference.OnPreferenceChangeListener {

    private var logoutRequested = false
    private var remoteUrlPref: Preference? = null
    private var networkOkDrawable: Drawable? = null
    private var networkNokDrawable: Drawable? = null
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
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var connectivityViewModel: ConnectivityViewModel

    companion object {
        const val SERVER_PING_TIMEOUT = 20000
    }

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
        networkOkDrawable = ContextCompat.getDrawable(context, R.drawable.network_ok_circle)
        networkNokDrawable = ContextCompat.getDrawable(context, R.drawable.network_nok_circle)
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

    private fun initLayout() {
        this.remoteUrl = loginViewModel.authInfoHelper.remoteUrl

        if (!loginViewModel.authInfoHelper.guestLogin)
            findPreference<PreferenceCategory>(accountCategoryKey)?.isVisible = true
        findPreference<Preference>(logoutPrefKey)?.onPreferenceClickListener = this

        remoteUrlPref =
            findPreference<Preference>(resources.getString(R.string.pref_remote_url_key))
        remoteUrlPref?.setDefaultValue(this.remoteUrl)
        remoteUrlPref?.onPreferenceChangeListener = this
        checkNetwork()
    }

    override fun getViewModel() {
        loginViewModel = activity?.run {
            ViewModelProvider(
                this,
                LoginViewModel.LoginViewModelFactory(delegate.appInstance, delegate.loginApiService)
            )[LoginViewModel::class.java]
        } ?: throw IllegalStateException("Invalid Activity")

        if (NetworkUtils.sdkNewerThanKitKat) {
            connectivityViewModel = activity?.run {
                ViewModelProvider(
                    this,
                    ConnectivityViewModel.ConnectivityViewModelFactory(
                        delegate.appInstance,
                        delegate.connectivityManager
                    )
                )[ConnectivityViewModel::class.java]
            } ?: throw IllegalStateException("Invalid Activity")
        }
    }

    override fun setupObservers() {

        if (NetworkUtils.sdkNewerThanKitKat) {
            connectivityViewModel.networkStateMonitor.observe(
                viewLifecycleOwner,
                Observer { networkState ->
                    Timber.d("<NetworkState changed -> $networkState>")
                    checkNetwork()
                    when (networkState) {
                        NetworkState.CONNECTED -> {
                            if (logoutRequested &&
                                loginViewModel.authenticationState.value == AuthenticationState.AUTHENTICATED
                            ) {
                                logoutRequested = false
                                logout()
                            }
                        }
                        else -> {
                        }
                    }
                })
        }

        loginViewModel.authenticationState.observe(
            this,
            Observer { authenticationState ->
                Timber.d("<AuthenticationState changed -> $authenticationState>")
                when (authenticationState) {
                    AuthenticationState.AUTHENTICATED -> {
                        if (logoutRequested && delegate.isConnected()) {
                            logoutRequested = false
                            logout()
                        }
                    }
                    AuthenticationState.INVALID_AUTHENTICATION -> {
                    }
                    else -> {
                    }
                }
            })
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
        if (delegate.isConnected()) {
            if (loginViewModel.authenticationState.value == AuthenticationState.AUTHENTICATED) {
                loginViewModel.disconnectUser()
            } else {
                logoutRequested = true
                activity?.let {
                    displaySnackBar(it, it.resources.getString(R.string.error_occurred_try_again))
                }
                Timber.d("Not authenticated yet, logoutRequested = $logoutRequested")
            }
        } else {
            logoutRequested = true
            activity?.let {
                displaySnackBar(it, it.resources.getString(R.string.no_internet))
            }
            Timber.d("No Internet connection, logoutRequested = $logoutRequested")
        }
    }

    /**
     * Checks network state, and adjust the indicator icon color
     */
    private fun checkNetwork() {
        if (delegate.isConnected()) {
            if (NetworkUtils.serverReachable(remoteUrl, SERVER_PING_TIMEOUT)) {
                remoteUrlPref?.summary = "$remoteUrl - $serverAccessibleString"
                remoteUrlPref?.icon = networkOkDrawable
            } else {
                remoteUrlPref?.summary = "$remoteUrl - $serverNotAccessibleString"
                remoteUrlPref?.icon = networkNokDrawable
            }
        } else {
            remoteUrlPref?.summary = "$remoteUrl - $noInternetString"
            remoteUrlPref?.icon = networkNokDrawable
        }
    }
}
