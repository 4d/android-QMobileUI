package com.qmarciset.androidmobileui.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.qmarciset.androidmobileapi.auth.AuthInfoHelper
import com.qmarciset.androidmobileapi.model.entity.Entities
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobileapi.utils.RequestErrorHelper
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
    private val tableName: String,
    private val fromTableInterface: FromTableInterface
) :
    BaseViewModel<T>(application, appDatabase, apiService, tableName) {

    init {
        Timber.i("EntityListViewModel initializing...")
    }

    private val authInfoHelper = AuthInfoHelper(application.applicationContext)
    private var hasGlobalStamp = fromTableInterface.hasGlobalStampPropertyFromTable(tableName)

    /**
     * LiveData
     */

    open var entityList: LiveData<List<T>> = roomRepository.getAll()

    open val dataLoading = MutableLiveData<Boolean>().apply { value = false }

    @Volatile
    private var globalStamp = authInfoHelper.globalStamp
        @Synchronized set(value) {
            Timber.d("GlobalStamp updated, old value was $globalStamp, new value is $value")
            field = value
            authInfoHelper.globalStamp = value
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

    fun getData() {
        if (hasGlobalStamp)
            if (tableName == "Employee") // test purpose, Service table has no __GlobalStamp
                getMoreRecentEntities()
            else
                getAll()
        else
            getAll()
    }

    /**
     * Gets all entities more recent than current globalStamp
     */
    private fun getMoreRecentEntities() {
        dataLoading.value = true
        val predicate = buildGlobalStampPredicate()
        restRepository.getMoreRecentEntities(predicate) { isSuccess, response, error ->
            dataLoading.value = false
            if (isSuccess) {
                response?.body()?.let {
                    handleData(it)
                }
            } else {
                toastMessage.postValue("try_refresh_data")
                RequestErrorHelper.handleError(error)
            }
        }
    }

    /**
     * Gets all entities
     */
    fun getAll() {
        dataLoading.value = true
        restRepository.getAll { isSuccess, response, error ->
            dataLoading.value = false
            if (isSuccess) {
                response?.body()?.let {
                    handleData(it)
                }
            } else {
                toastMessage.postValue("try_refresh_data")
                RequestErrorHelper.handleError(error)
            }
        }
    }

    /**
     * Retrieves data from response and insert it in database
     */
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
                    if (hasGlobalStamp) {
                        Timber.i("[ Inserting entity with __GlobalStamp = ${it.__GlobalStamp} ]")
                        it.__GlobalStamp?.let { stamp ->
                            if (globalStamp < stamp)
                                globalStamp = stamp
                        }
                    } else {
                        Timber.i("[ Inserting entity with no __GlobalStamp ]")
                    }
                }
            }
        }
        return isInserted
    }

    /**
     * Returns predicate for requests with __GlobalStamp
     */
    private fun buildGlobalStampPredicate(): String {
        return "\"__GlobalStamp > $globalStamp AND __GlobalStamp <= ${globalStamp + 2}\""
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
