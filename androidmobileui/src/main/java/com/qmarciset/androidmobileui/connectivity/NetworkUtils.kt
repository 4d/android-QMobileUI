package com.qmarciset.androidmobileui.connectivity

import android.net.ConnectivityManager
import android.os.Build
import com.qmarciset.androidmobileui.viewmodel.ConnectivityViewModel

object NetworkUtils {

    fun isConnected(
        connectivityViewModel: ConnectivityViewModel,
        connectivityManager: ConnectivityManager?
    ): Boolean = if (sdkOlderThanLollipop) {
        connectivityViewModel.networkStateMonitor.value == NetworkState.CONNECTED
    } else {
        isKitKatConnectedOrConnecting(connectivityManager)
    }

    @Suppress("DEPRECATION")
    private fun isKitKatConnectedOrConnecting(connectivityManager: ConnectivityManager?): Boolean {
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    val sdkOlderThanLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
}
