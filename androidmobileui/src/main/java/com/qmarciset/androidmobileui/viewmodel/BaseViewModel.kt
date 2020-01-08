package com.qmarciset.androidmobileui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobileapi.repository.RestRepository
import com.qmarciset.androidmobiledatastore.dao.BaseDao
import com.qmarciset.androidmobiledatastore.db.AppDatabaseInterface
import com.qmarciset.androidmobiledatastore.repository.RoomRepository

/**
 * If you need to use context inside your viewmodel you should use AndroidViewModel, because it
 * contains the application context (to retrieve the context call getApplication() ), otherwise use
 * regular ViewModel.
 */
abstract class BaseViewModel<T>(
    application: Application,
    appDatabase: AppDatabaseInterface,
    apiService: ApiService,
    tableName: String
) :
    AndroidViewModel(application) {

    val dao: BaseDao<T> = appDatabase.getDao(tableName)

    val roomRepository: RoomRepository<T> =
        RoomRepository(dao)
    val restRepository: RestRepository =
        RestRepository(tableName, apiService)

    val toastMessage = MutableLiveData<String>()

    override fun onCleared() {
        super.onCleared()
        restRepository.disposable.dispose()
    }
}
