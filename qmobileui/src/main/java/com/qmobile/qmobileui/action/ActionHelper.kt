package com.qmobile.qmobileui.action

import com.qmobile.qmobiledatasync.app.BaseApp

class ActionHelper {
    companion object {
        fun getActionContent(
            tableName: String,
            selectedActionId: String?,
            parameters: HashMap<String, Any>? = null,
            metaData: HashMap<String, String>? = null
        ): MutableMap<String, Any> {
            val map: MutableMap<String, Any> = mutableMapOf()
            val actionContext = mutableMapOf<String, Any>(
                "dataClass" to
                        BaseApp.genericTableHelper.originalTableName(tableName)
            )
            if (selectedActionId != null) {
                actionContext["entity"] = mapOf("primaryKey" to selectedActionId)
            }
            map["context"] = actionContext
            parameters?.let { map.put("parameters", parameters) }
            metaData?.let { map.put("metadata", ActionMetaData(metaData)) }
            return map
        }
    }

}