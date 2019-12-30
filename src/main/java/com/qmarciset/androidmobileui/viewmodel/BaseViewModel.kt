package com.qmarciset.androidmobileui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.qmarciset.androidmobileapi.repository.RestRepository
import com.qmarciset.androidmobiledatastore.AppDatabaseInterface
import com.qmarciset.androidmobiledatastore.dao.BaseDao
import com.qmarciset.androidmobiledatastore.repository.RoomRepository

/**
 * If you need to use context inside your viewmodel you should use AndroidViewModel, because it
 * contains the application context (to retrieve the context call getApplication() ), otherwise use
 * regular ViewModel.
 */
abstract class BaseViewModel<T>(
    application: Application,
    appDatabase: AppDatabaseInterface,
    tableName: String
) :
    AndroidViewModel(application) {

    val dao: BaseDao<T> = appDatabase.getAppDatabase(application).getDao(tableName)


//    val clazz = Class.forName("com.qmarciset.sample4dapp.data.db.AppDatabase") as Class<AppDatabaseInterface>


//        val kk = clazz.constructors.first().newInstance() as AppDatabaseInterface



//    val dao: BaseDao<T> = qq.getAppDatabase(application, viewModelScope).getDao(tableName)


    val roomRepository: RoomRepository<T> =
        RoomRepository(dao)
    val restRepository: RestRepository =
        RestRepository(tableName, application.applicationContext)

    val toastMessage = MutableLiveData<String>()

    override fun onCleared() {
        super.onCleared()
        restRepository.disposable.dispose()
    }
}
