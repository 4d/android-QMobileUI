package com.qmobile.qmobileui.action

import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatasync.app.BaseApp
import org.json.JSONArray
import org.json.JSONObject

class ActionHelper private constructor() {

    companion object {
        fun getActionContent(
            tableName: String,
            selectedActionId: String?,
            parameters: HashMap<String, Any>? = null,
            metaData: HashMap<String, String>? = null,
            relationName: String? = null,
            parentPrimaryKey: String? = null,
            parentTableName: String? = null,
            parentRelationName: String? = null
        ): MutableMap<String, Any> {
            val map: MutableMap<String, Any> = mutableMapOf()
            val actionContext = mutableMapOf<String, Any>(
                "dataClass" to
                    BaseApp.genericTableHelper.originalTableName(tableName)
            )

            // entity
            val entity = HashMap<String, Any>()
            if (selectedActionId != null) {
                entity["primaryKey"] = selectedActionId
            }
            if (!relationName.isNullOrEmpty()) {
                entity["relationName"] = relationName
            }
            if (entity.isNotEmpty()) {
                actionContext["entity"] = entity
            }

            // parent
            if ((parentPrimaryKey != null) && (parentPrimaryKey != "0")) {
                val parent = HashMap<String, Any>()

                parent["primaryKey"] = parentPrimaryKey
                if (!parentRelationName.isNullOrEmpty()) {
                    parent["relationName"] = parentRelationName
                }

                if (!parentTableName.isNullOrEmpty()) {
                    parent["dataClass"] = parentTableName
                }

                if (parent.isNotEmpty()) {
                    actionContext["parent"] = parent
                }
            }

            map["context"] = actionContext
            parameters?.let { map.put("parameters", parameters) }
            metaData?.let { map.put("metadata", ActionMetaData(metaData)) }
            return map
        }

        fun createActionFromJsonObject(jsonObject: JSONObject): Action {
            jsonObject.apply {
                return Action(
                    name = getSafeString("name") ?: "",
                    icon = getSafeString("icon"),
                    shortLabel = getSafeString("shortLabel"),
                    label = getSafeString("label"),
                    preset = getSafeString("preset"),
                    parameters = getSafeArray("parameters") ?: JSONArray()
                )
            }
        }
    }
}
