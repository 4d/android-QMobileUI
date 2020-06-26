/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.detail

import androidx.lifecycle.ViewModelProvider
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobiledatasync.viewmodel.EntityViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.factory.EntityViewModelFactory
import kotlin.reflect.KClass

fun EntityDetailFragment.getViewModel() {
    getEntityViewModel()
}

// Get EntityViewModel
@Suppress("UNCHECKED_CAST")
fun EntityDetailFragment.getEntityViewModel() {
    val kClazz: KClass<EntityViewModel<EntityModel>> =
        EntityViewModel::class as KClass<EntityViewModel<EntityModel>>
    entityViewModel = ViewModelProvider(
        this,
        EntityViewModelFactory(
            tableName,
            itemId,
            delegate.apiService
        )
    )[kClazz.java]
}
