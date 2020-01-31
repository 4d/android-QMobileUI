package com.qmarciset.androidmobileui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class LifecycleViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * LiveData
     */

    val entersForeground = MutableLiveData<Boolean>().apply { value = false }

    class LifecycleViewModelFactory(
        private val application: Application
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return LifecycleViewModel(application) as T
        }
    }
}
