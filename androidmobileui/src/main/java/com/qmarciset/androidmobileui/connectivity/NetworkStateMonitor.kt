package com.qmarciset.androidmobileui.connectivity

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class NetworkStateMonitor(private val connectivityManager: ConnectivityManager) :
    LiveData<NetworkState>() {

    private var hasNetworkChanged: Boolean = false

    private val networkStateObject = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            super.onLost(network)
            hasNetworkChanged = true
            postValue(NetworkState.CONNECTION_LOST)
        }

        override fun onUnavailable() {
            super.onUnavailable()
            hasNetworkChanged = true
            postValue(NetworkState.DISCONNECTED)
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            if (hasNetworkChanged) {
                postValue(NetworkState.CONNECTED)
            }
        }
    }

    override fun onActive() {
        super.onActive()
        connectivityManager.registerNetworkCallback(networkRequestBuilder(), networkStateObject)
    }

    override fun onInactive() {
        super.onInactive()
        try {
            connectivityManager.unregisterNetworkCallback(networkStateObject)
        } catch (e: Exception) {
            Timber.d("NetworkCallback for Wi-fi was not registered or already unregistered")
        }
    }

    private fun networkRequestBuilder(): NetworkRequest {
        return NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
    }
}
