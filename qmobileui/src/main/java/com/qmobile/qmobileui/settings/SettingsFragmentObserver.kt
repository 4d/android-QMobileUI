/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.settings

import com.qmobile.qmobileapi.connectivity.NetworkStateEnum
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobileui.activity.BaseObserver

class SettingsFragmentObserver(private val fragment: SettingsFragment, private val connectivityViewModel: ConnectivityViewModel) : BaseObserver {

    override fun initObservers() {
        observeNetworkStatus()
    }

    // Observe network status
    private fun observeNetworkStatus() {
        connectivityViewModel.networkStateMonitor.observe(
            fragment.viewLifecycleOwner,
            { networkState ->
                if (fragment.firstTime || !fragment.firstTime && networkState == NetworkStateEnum.CONNECTED) {
                    fragment.firstTime = false
                    fragment.checkNetwork()
                }
            }
        )
    }
}
