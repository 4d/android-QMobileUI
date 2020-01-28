package com.qmarciset.androidmobileui.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.qmarciset.androidmobileapi.model.entity.Entities
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobileapi.utils.RequestErrorHandler
import com.qmarciset.androidmobileapi.utils.parseJsonToType
import com.qmarciset.androidmobiledatastore.db.AppDatabaseInterface
import com.qmarciset.androidmobileui.utils.FromTableInterface
import okhttp3.ResponseBody
import timber.log.Timber

@Suppress("UNCHECKED_CAST")
abstract class EntityListViewModel<T>(
    application: Application,
    appDatabase: AppDatabaseInterface,
    apiService: ApiService,
    tableName: String,
    private val fromTableInterface: FromTableInterface
) :
    BaseViewModel<T>(application, appDatabase, apiService, tableName) {

    open var entityList: LiveData<List<T>> = roomRepository.getAll()

    open val dataLoading = MutableLiveData<Boolean>().apply { value = false }

    init {
        Timber.d("EntityListViewModel initializing...")
    }

    fun delete(item: EntityModel) {
        roomRepository.delete(item as T)
    }

    fun deleteAll() {
        roomRepository.deleteAll()
    }

    fun insert(item: EntityModel) {
        roomRepository.insert(item as T)
    }

    fun insertAll(items: List<EntityModel>) {
        roomRepository.insertAll(items as List<T>)
    }

    // Gets all entities
    fun getAllFromApi() {
        dataLoading.value = true
        restRepository.getAllFromApi { isSuccess, response, error ->
            dataLoading.value = false
            if (isSuccess) {
                response?.body()?.let {
                    handleData(it)
                }
            } else {
                toastMessage.postValue("try_refresh_data")
                RequestErrorHandler.handleError(error)
            }
        }
    }

    // Retrieves data from response and insert it in database
    private fun handleData(responseBody: ResponseBody): Boolean {
        val json = responseBody.string()
        val gson = Gson()
        val entities = gson.parseJsonToType<Entities>(json)

        val entityList: List<T>? = gson.parseJsonToType<List<T>>(entities?.__ENTITIES)
        var isInserted = false
        entityList?.let {
            for (item in entityList) {
                val itemJson = gson.toJson(item)
                val entity: EntityModel? =
                    fromTableInterface.parseEntityFromTable(dao.tableName, itemJson.toString())
                entity.let {
                    this.insert(it as EntityModel)
                    isInserted = true
                }
            }
        }
        return isInserted
    }

    class EntityListViewModelFactory(
        private val application: Application,
        private val appDatabase: AppDatabaseInterface,
        private val apiService: ApiService,
        private val tableName: String,
        private val fromTableInterface: FromTableInterface
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return fromTableInterface.entityListViewModelFromTable<T>(
                tableName,
                application,
                appDatabase,
                apiService,
                fromTableInterface
            ) as T
        }
    }
}
