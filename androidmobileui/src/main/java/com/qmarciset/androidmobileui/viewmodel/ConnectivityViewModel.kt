package com.qmarciset.androidmobileui.viewmodel

import android.app.Application
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qmarciset.androidmobileui.connectivity.NetworkState
import com.qmarciset.androidmobileui.connectivity.NetworkStateMonitor

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ConnectivityViewModel(application: Application, connectivityManager: ConnectivityManager) :
    AndroidViewModel(application) {

    val networkStateMonitor: LiveData<NetworkState> =
        NetworkStateMonitor(
            connectivityManager
        )

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
