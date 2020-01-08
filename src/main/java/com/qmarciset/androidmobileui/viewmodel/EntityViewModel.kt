package com.qmarciset.androidmobileui.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobiledatastore.db.AppDatabaseInterface
import timber.log.Timber

open class EntityViewModel<T>(
    application: Application,
    appDatabase: AppDatabaseInterface,
    apiService: ApiService,
    id: String,
    tableName: String
) :
    BaseViewModel<T>(application, appDatabase, apiService, tableName) {

    open val entity: LiveData<T> = roomRepository.getOne(id)

    init {
        Timber.d("EntityViewModel initializing...")
    }

    class EntityViewModelFactory(
        private val application: Application,
        private val appDatabase: AppDatabaseInterface,
        private val apiService: ApiService,
        private val id: String,
        private val tableName: String
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return EntityViewModel<T>(
                application,
                appDatabase,
                apiService,
                id,
                tableName
            ) as T
        }
    }
}
