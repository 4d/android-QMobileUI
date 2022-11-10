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
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.relation.RelationHelper.inverseAliasPath
import com.qmobile.qmobiledatasync.utils.fieldAdjustment
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.model.ActionMetaData
import com.qmobile.qmobileui.action.sort.Sort
import com.qmobile.qmobileui.action.sort.Sort.getTypeConstraints
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

        if (relation != null) {
            entity["relationName"] = relation.name
        }

        if (entity.isNotEmpty()) {
            actionContext["entity"] = entity
        }

        // parent
        if (relation != null) {
            val parent = HashMap<String, Any>()

            parent["primaryKey"] = parentItemId

            parent["relationName"] = relation.inverseAliasPath()
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
                sortFields = getSortFields(parameters)
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
}
