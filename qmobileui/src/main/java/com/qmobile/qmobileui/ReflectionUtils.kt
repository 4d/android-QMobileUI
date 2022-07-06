/*
 * Created by qmarciset on 28/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui

import timber.log.Timber
import kotlin.reflect.KProperty1

object ReflectionUtils {
    @Suppress("UNCHECKED_CAST")
    fun <R> readInstanceProperty(instance: Any?, propertyName: String): R? {
        if (instance == null) {
            return null
        }
        val property = instance::class.members.first { it.name == propertyName } as KProperty1<Any, *>
        return try {
            property.get(instance) as R
        } catch (e: ClassCastException) {
            Timber.e(e.message.orEmpty())
            null
        }
    }
}
