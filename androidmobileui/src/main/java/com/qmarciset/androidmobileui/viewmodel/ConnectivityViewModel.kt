package com.qmarciset.androidmobileui.viewmodel

import android.app.Application
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qmarciset.androidmobileapi.connectivity.NetworkState
import com.qmarciset.androidmobileapi.connectivity.NetworkStateMonitor
import com.qmarciset.androidmobileapi.connectivity.ServerAccessibility
import com.qmarciset.androidmobileapi.utils.PING_TIMEOUT
import java.net.URL
import timber.log.Timber

class ConnectivityViewModel(application: Application, connectivityManager: ConnectivityManager) :
    AndroidViewModel(application) {

    init {
        Timber.i("ConnectivityViewModel initializing...")
    }

    private val serverAccessibility = ServerAccessibility()

    /**
     * LiveData
     */

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    val networkStateMonitor: LiveData<NetworkState> =
        NetworkStateMonitor(
            connectivityManager
        )

    val serverAccessible = MutableLiveData<Boolean>().apply { value = false }

    /**
     * Pings server to check if it is accessible or not
     */
    fun checkAccessibility(remoteUrl: String) {
        val url = URL(remoteUrl)
        serverAccessibility.pingServer(url.host, url.port, PING_TIMEOUT) { isAccessible ->
            serverAccessible.postValue(isAccessible)
        }
    }

    override fun onCleared() {
        super.onCleared()
        serverAccessibility.disposable.dispose()
    }

    class ConnectivityViewModelFactory(
        private val application: Application,
        private val connectivityManager: ConnectivityManager
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ConnectivityViewModel(
                application,
                connectivityManager
            ) as T
        }
    }
}
