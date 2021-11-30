package com.qmobile.qmobileui.action

import com.qmobile.qmobiledatasync.app.BaseApp

class ActionHelper {
    companion object {
        fun getActionContext(
            tableName: String,
            selectedActionId: String?,
            parameters: HashMap<String, Any>? = null,
            metaData: HashMap<String, String>? = null
        ): Map<String, Any> {
            val actionContext = mutableMapOf<String, Any>(
                "dataClass" to
                        BaseApp.genericTableHelper.originalTableName(tableName)
            )
            if (selectedActionId != null) {
                actionContext["entity"] = mapOf("primaryKey" to selectedActionId)
            }

            if (parameters != null && parameters.isNotEmpty()) {
                actionContext["parameters"] = parameters
            }
            if (metaData != null && metaData.isNotEmpty()) {
                actionContext["metadata"] = ActionMetaData(metaData)
            }
            return actionContext
        }
    }
}