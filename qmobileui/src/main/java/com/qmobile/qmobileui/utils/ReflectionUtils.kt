/*
 * Created by qmarciset on 11/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.qmobile.qmobileapi.utils.getSafeAny
import com.qmobile.qmobileapi.utils.parseToString
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import org.json.JSONObject

object ReflectionUtils {

    private val noJsonPropertyMapper = JsonMapper.builder().configure(MapperFeature.USE_ANNOTATIONS, false).build()

    /**
     * Handles alias resolving
     */
    fun getInstanceProperty(instance: RoomEntity, propertyName: String): Any? {
        return if (propertyName.contains(".")) {
            var tmpInstance: Any? = instance
            propertyName.split("?.").forEach { part ->
                val subTmpInstance = tmpInstance
                tmpInstance = when {
                    subTmpInstance is JSONObject -> subTmpInstance.getSafeAny(part)
                    subTmpInstance != null && subTmpInstance.toString() != "null" -> JSONObject(
                        noJsonPropertyMapper.parseToString(
                            subTmpInstance
                        )
                    ).getSafeAny(part)
                    else -> null
                }
            }
            tmpInstance
        } else {
            JSONObject(noJsonPropertyMapper.parseToString(instance.__entity)).getSafeAny(propertyName)
        }
    }

    fun getInstancePropertyForInputControl(instance: RoomEntity, propertyName: String): Any? {
        return if (propertyName.contains(".")) {
            var tmpInstance: Any? = instance
            propertyName.split(".").forEach { part ->
                val subTmpInstance = tmpInstance
                tmpInstance = when {
                    subTmpInstance is JSONObject -> subTmpInstance.getSafeAny(part)
                    subTmpInstance != null && subTmpInstance.toString() != "null" -> JSONObject(
                        BaseApp.mapper.parseToString(
                            subTmpInstance
                        )
                    ).getSafeAny(part)
                    else -> null
                }
            }
            tmpInstance
        } else {
            JSONObject(noJsonPropertyMapper.parseToString(instance.__entity)).getSafeAny(propertyName)
        }
    }
}
