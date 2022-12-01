/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.settings

import com.qmobile.qmobiledatasync.network.NetworkState
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.BaseObserver

class SettingsFragmentObserver(
    private val fragment: SettingsFragment,
    private val connectivityViewModel: ConnectivityViewModel
) : BaseObserver {

    override fun initObservers() {
        observeNetworkStatus()
        observePendingTasks()
    }

    // Observe network status
    private fun observeNetworkStatus() {
        connectivityViewModel.networkStateMonitor.observe(
            fragment.viewLifecycleOwner
        ) { networkState ->
            if (fragment.firstTime || (!fragment.firstTime && networkState == NetworkState.CONNECTED)) {
                fragment.firstTime = false
                fragment.delegate.checkNetwork(fragment)
            }
        }
    }

    private fun observePendingTasks() {
        fragment.actionActivity.getTaskVM().pendingTasks.observe(fragment.viewLifecycleOwner) { pendingTasks ->
            fragment.pendingTaskPref?.summary = fragment.getString(R.string.pref_pending_tasks_count, pendingTasks.size)
        }
    }
}
