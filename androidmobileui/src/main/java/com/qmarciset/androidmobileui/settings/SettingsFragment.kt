package com.qmarciset.androidmobileui.settings

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.qmarciset.androidmobileui.BaseFragment
import com.qmarciset.androidmobileui.FragmentCommunication
import com.qmarciset.androidmobileui.R
import com.qmarciset.androidmobileui.viewmodel.LoginViewModel

class SettingsFragment : PreferenceFragmentCompat(), BaseFragment, Preference.OnPreferenceClickListener,
    Preference.OnPreferenceChangeListener {

    private lateinit var delegate: FragmentCommunication
    private lateinit var loginViewModel: LoginViewModel

    private var remoteUrlPref: Preference? = null
    private lateinit var accountCategoryKey: String
    private lateinit var remoteUrlPrefKey: String
    private lateinit var logoutPrefKey: String

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
        // access resources elements
        remoteUrlPrefKey = resources.getString(R.string.pref_remote_url_key)
        accountCategoryKey = resources.getString(R.string.cat_account_key)
        logoutPrefKey = resources.getString(R.string.pref_logout_key)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView()
    }

    override fun getViewModel() {
        loginViewModel = activity?.run {
            ViewModelProvider(
                this,
                LoginViewModel.LoginViewModelFactory(delegate.appInstance, delegate.loginApiService)
            )[LoginViewModel::class.java]
        } ?: throw IllegalStateException("Invalid Activity")
    }

    override fun setupObservers() {
        // Nothing to observe yet
    }

    private fun initView() {

        getViewModel()

        if (!loginViewModel.authInfoHelper.guestLogin)
            findPreference<PreferenceCategory>(accountCategoryKey)?.isVisible = true
        findPreference<Preference>(logoutPrefKey)?.onPreferenceClickListener = this

        remoteUrlPref =
            findPreference<Preference>(resources.getString(R.string.pref_remote_url_key))
        remoteUrlPref?.setDefaultValue(loginViewModel.authInfoHelper.remoteUrl)
        remoteUrlPref?.summary = loginViewModel.authInfoHelper.remoteUrl
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
                    delegate.refreshApiClients()
                    remoteUrlPref?.setDefaultValue(newRemoteUrl)
                    remoteUrlPref?.summary = newRemoteUrl
                }
            }
        }
        return true
    }

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

    private fun logout() {
        loginViewModel.disconnectUser()
    }
}
