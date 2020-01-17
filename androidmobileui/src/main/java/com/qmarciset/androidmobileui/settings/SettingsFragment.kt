package com.qmarciset.androidmobileui.settings

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.qmarciset.androidmobileapi.auth.AuthInfoHelper
import com.qmarciset.androidmobileapi.auth.AuthenticationState
import com.qmarciset.androidmobileui.FragmentCommunication
import com.qmarciset.androidmobileui.R
import com.qmarciset.androidmobileui.viewmodel.LoginViewModel

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private lateinit var delegate: FragmentCommunication
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView()
    }

    private fun initView() {
        getViewModel()

        if (!loginViewModel.authInfoHelper.guestLogin)
            findPreference<PreferenceCategory>("account_category")?.isVisible = true
            findPreference<Preference>("logout")?.onPreferenceClickListener = this
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
        // access resources elements
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        preference?.let {
            return when (preference.key) {
                "logout" -> {
                    loginViewModel.authInfoHelper.sessionToken = ""
                    loginViewModel.authenticationState.postValue(AuthenticationState.UNAUTHENTICATED)
                    true
                }
                else -> false
            }
        }
        return false
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
//        preference?.let {
//            when(preference.key) {
//                "remote_url" -> logout()
//            }
//        }
        return true
    }

    private fun getViewModel() {
        loginViewModel = activity?.run {
            ViewModelProvider(
                this,
                LoginViewModel.LoginViewModelFactory(delegate.appInstance, delegate.apiService)
            )[LoginViewModel::class.java]
        } ?: throw IllegalStateException("Invalid Activity")
    }
}
