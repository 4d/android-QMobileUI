/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.sync

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.qmarciset.androidmobileapi.auth.AuthInfoHelper
import com.qmarciset.androidmobiledatasync.DataSyncState
import com.qmarciset.androidmobiledatasync.GlobalStampWithTable
import java.util.concurrent.atomic.AtomicInteger
import timber.log.Timber

class DataSync(
    private val activity: AppCompatActivity,
    private val authInfoHelper: AuthInfoHelper
) {

    companion object {
        var NUMBER_OF_REQUEST_MAX_LIMIT = 0
    }

    private var nbToReceive = 0

    fun setObserver(entityViewModelIsToSyncList: List<EntityViewModelIsToSync>) {

        var viewModelStillInitializing = true
        val received = AtomicInteger(0)
        var requestPerformed = 0
        nbToReceive = entityViewModelIsToSyncList.filter { it.isToSync }.size
        NUMBER_OF_REQUEST_MAX_LIMIT = nbToReceive * 3

        val receivedSyncedTableGS = mutableListOf<GlobalStampWithTable>()

        val liveDataMerger = MediatorLiveData<GlobalStampWithTable>()

        // merge LiveData
        for (dataSyncViewModelIsToSync in entityViewModelIsToSyncList) {
            liveDataMerger.addSource(dataSyncViewModelIsToSync.vm.globalStamp) {
                if (it != null) {
                    liveDataMerger.value =
                        GlobalStampWithTable(
                            dataSyncViewModelIsToSync.vm.dao.tableName,
                            it
                        )
                }
            }
        }

        val observer = Observer<GlobalStampWithTable> { globalStampWithTable ->
            if (!viewModelStillInitializing) {
                Timber.d("new globalStamps for table ${globalStampWithTable.tableName}, globalStamp value = ${globalStampWithTable.globalStamp}")

                receivedSyncedTableGS.add(globalStampWithTable)

                Timber.d("nbToReceive = $nbToReceive, received = ${received.get()}")
                if (received.incrementAndGet() == nbToReceive) {

                    // Get the max globalStamp between received ones, and stored one
                    val maxGlobalStamp =
                        maxOf(receivedSyncedTableGS.map { it.globalStamp }.maxBy { it } ?: 0,
                            authInfoHelper.globalStamp)

                    var isAtLeastOneToSync = false

                    for (entityViewModelIsToSync in entityViewModelIsToSyncList) {
                        val vmGs = entityViewModelIsToSync.vm.globalStamp.value ?: 0
                        if (vmGs < maxGlobalStamp) {
                            entityViewModelIsToSync.isToSync = true
                            isAtLeastOneToSync = true
                        }
                    }

                    if (!isAtLeastOneToSync) {
                        Timber.d("!isAtLeastOneToSync")
                        liveDataMerger.removeObservers(activity)
                        authInfoHelper.globalStamp = maxGlobalStamp
                        for (dataSyncViewModelIsToSync in entityViewModelIsToSyncList) {
                            // notify data are synced
                            dataSyncViewModelIsToSync.vm.dataSynchronized.postValue(
                                DataSyncState.SYNCHRONIZED)
                        }
                    } else {
                        Timber.d("isAtLeastOneToSync true")
                        received.set(0)
                        requestPerformed++
                        if (requestPerformed <= NUMBER_OF_REQUEST_MAX_LIMIT)
                            sync(entityViewModelIsToSyncList)
                    }
                }
            } else {
                Timber.d("nbToReceive = $nbToReceive, received = ${received.get()}")
                if (received.incrementAndGet() == nbToReceive) {
                    viewModelStillInitializing = false
                    received.set(0)
                    // first sync
                    sync(entityViewModelIsToSyncList)
                }
            }
        }

        // observe merged LiveData
        liveDataMerger.observe(activity, observer)
    }

    private fun sync(entityViewModelIsToSyncList: List<EntityViewModelIsToSync>) {

        nbToReceive = entityViewModelIsToSyncList.filter { it.isToSync }.size
        NUMBER_OF_REQUEST_MAX_LIMIT = nbToReceive * 3

        for (dataSyncViewModelIsToSync in entityViewModelIsToSyncList) {
            dataSyncViewModelIsToSync.vm.dataSynchronized.postValue(DataSyncState.SYNCHRONIZING)
            Timber.d("sync : tableName = ${dataSyncViewModelIsToSync.vm.dao.tableName}, istoSync : ${dataSyncViewModelIsToSync.isToSync}")
            if (dataSyncViewModelIsToSync.isToSync) {
                dataSyncViewModelIsToSync.isToSync = false
                dataSyncViewModelIsToSync.vm.getData {
                }
            }
        }
    }
}
