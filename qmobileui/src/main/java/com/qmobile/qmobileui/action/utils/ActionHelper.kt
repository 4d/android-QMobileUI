/*
 * Created by htemanni on 1/6/2022.
 * 4D SAS
 * Copyright (c) 2022 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui.action.utils

import android.util.Base64
import com.qmobile.qmobileapi.utils.getJSONObjectList
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.parseToString
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiClient
import com.qmobile.qmobileapi.utils.getSafeBoolean
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.utils.fieldAdjustment
import com.qmobile.qmobileui.action.inputcontrols.InputControl
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.model.ActionMetaData
import com.qmobile.qmobileui.action.sort.Sort
import com.qmobile.qmobileui.action.sort.Sort.getTypeConstraints
import com.qmobile.qmobileui.formatters.TimeFormat
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

object ActionHelper {

    fun getActionContent(
        tableName: String,
        actionUUID: String,
        itemId: String = "",
        parameters: HashMap<String, Any>? = null,
        metaData: HashMap<String, String>? = null,
        parentItemId: String = "",
        relation: Relation? = null
    ): MutableMap<String, Any> {
        val actionContext = mutableMapOf<String, Any>(
            "dataClass" to
                (BaseApp.runtimeDataHolder.tableInfo[tableName]?.originalName ?: "")
        )
        // entity
        val entity = HashMap<String, Any>()

        if (itemId.isNotEmpty()) {
            entity["primaryKey"] = itemId
        }

//        if (relation != null) {
//            entity["relationName"] = relation.inverseAliasPath()
//        }

        if (entity.isNotEmpty()) {
            actionContext["entity"] = entity
        }

        // parent
        if (relation != null) {
            val parent = HashMap<String, Any>()

            parent["primaryKey"] = parentItemId

            // 4D relation name
            val originalRelationName =
                BaseApp.runtimeDataHolder.tableInfo[relation.source]
                    ?.fields?.get(relation.name)
                    ?.removeSuffix(Relation.SUFFIX) ?: ""
            parent["relationName"] = originalRelationName
            parent["dataClass"] = relation.source

            if (parent.isNotEmpty()) {
                actionContext["parent"] = parent
            }
        }

        val map: MutableMap<String, Any> = mutableMapOf()
        map["context"] = actionContext
        map["id"] = actionUUID
        parameters?.let { map.put("parameters", parameters) }
        metaData?.let { map.put("metadata", ActionMetaData(metaData)) }
        return map
    }

    fun updateActionContent(actionContent:  MutableMap<String, Any>?,
                            parameters: HashMap<String, Any>? = null,
                            metaData: HashMap<String, String>? = null) {

        parameters?.let { actionContent?.put("parameters", parameters) }
        metaData?.let { actionContent?.put("metadata", ActionMetaData(metaData)) }
    }

    fun updateActionContentId(actionContent: MutableMap<String, Any>?) {
        actionContent?.put("id", UUID.randomUUID().toString())
    }

    fun createActionFromJsonObject(jsonObject: JSONObject): Action {
        jsonObject.apply {
            val parameters = getSafeArray("parameters") ?: JSONArray()
            return Action(
                name = getSafeString("name") ?: "",
                shortLabel = getSafeString("shortLabel"),
                label = getSafeString("label"),
                icon = getSafeString("icon"),
                preset = getSafeString("preset"),
                scope = if (getSafeString("scope") == "table") Action.Scope.TABLE else Action.Scope.CURRENT_RECORD,
                parameters = parameters,
                uuid = getSafeString("uuid") ?: "",
                description = getSafeString("description") ?: "",
                sortFields = getSortFields(parameters),
                hasUniqueTask = getSafeBoolean("hasUniqueTask") ?: false
            )
        }
    }

    private fun getSortFields(parameters: JSONArray): LinkedHashMap<String, String> {
        val fieldsToSortBy: LinkedHashMap<String, String> = LinkedHashMap()
        parameters.getJSONObjectList().forEach { parameter ->
            val format = Sort.sortMatchingKeywords(parameter.getSafeString("format"))

            val type = parameter.getSafeString("type")
            parameter.getSafeString("name")?.let { name ->
                val fieldName = name.fieldAdjustment()
                val key = getTypeConstraints(fieldName, type, format)

                if (format.isNotEmpty()) {
                    fieldsToSortBy[key] = format
                }
            }
        }
        return fieldsToSortBy
    }

    fun fillActionList(json: JSONObject, tableName: String, actionList: MutableList<Action>) {
        getActionObjectList(json, tableName).forEach {
            val action = createActionFromJsonObject(it)
            // Action having preset "sort" without any parameter should be discarded
            if (!(action.isSortAction() && action.parameters.length() == 0)) {
                actionList.add(action)
            }
        }
    }

    fun getActionObjectList(json: JSONObject, tableName: String): List<JSONObject> {
        val objectList = mutableListOf<JSONObject>()
        json.getSafeArray(tableName)?.let { actionsArray ->
            for (i in 0 until actionsArray.length()) {
                actionsArray.getJSONObject(i)?.let {
                    objectList.add(it)
                }
            }
        }
        return objectList
    }

    // Get base 64 encoded context from actionContent map
    fun getBase64EncodedContext(actionContent: Map<String, Any>): String {
        val context = BaseApp.mapper.parseToString(actionContent["context"])
        return Base64.encodeToString(context.toByteArray(), Base64.NO_WRAP)
    }

    @Suppress("UNCHECKED_CAST")
    fun ActionTask.cleanActionContent(): MutableMap<String, Any> {
        val newActionContent = this.actionContent?.toMutableMap() ?: return mutableMapOf()
        cleanInputControlFormatHolders(this, newActionContent)
        (newActionContent["parameters"] as? MutableMap<String, Any?>)?.let { parameters ->
            this.getParametersAsList().forEach { actionParameter ->
                convertTimeToSeconds(actionParameter, parameters)
                removeImageURL(actionParameter, parameters)
            }
        }
        return newActionContent
    }

    private fun cleanInputControlFormatHolders(actionTask: ActionTask, content: MutableMap<String, Any>) {
        for (i in 0 until actionTask.getNbParameters()) {
            content.remove(InputControl.INPUT_CONTROL_FORMAT_HOLDER_KEY + "_$i")
        }
    }

    private fun convertTimeToSeconds(actionParameter: JSONObject, parameters: MutableMap<String, Any?>) {
        if (actionParameter.getSafeString("type") != "time") return
        val parameterName = actionParameter.getSafeString("name") ?: return
        val millisValue = (parameters[parameterName] as? Long) ?: return
        parameters[parameterName] = millisValue / TimeFormat.INT_1000
    }

    // picture URL means already on server, so do not send URL (maybe replace by a new upload)
    private fun removeImageURL(actionParameter: JSONObject, parameters: MutableMap<String, Any?>) {
        if (actionParameter.getSafeString("type") != "image") return
        val parameterName = actionParameter.getSafeString("name") ?: return
        val stringValue = (parameters[parameterName] as? String) ?: return
        if (stringValue.startsWith(ApiClient.SERVER_ENDPOINT)) { // must be an upload ID
            parameters.remove(parameterName)
        } else if (stringValue.isEmpty()) {
            parameters[parameterName] = null // no upload, must set image to null (or maybe set metadata upload...)
        }
    }
}
