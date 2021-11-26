package com.qmobile.qmobileui.action

import com.qmobile.qmobiledatasync.app.BaseApp

class ActionHelper {
    companion object {
        fun getActionContext(
            tableName: String,
            selectedActionId: String?,
            paramsToSubmit: HashMap<String, Any>? = null
        ): Map<String, Any> {
            val actionContext = mutableMapOf<String, Any>(
                "dataClass" to
                        BaseApp.genericTableHelper.originalTableName(tableName)
            )
            if (selectedActionId != null) {
                actionContext["entity"] = mapOf("primaryKey" to selectedActionId)
            }

            if (paramsToSubmit != null && paramsToSubmit.isNotEmpty()) {
                actionContext["parameters"] = paramsToSubmit
            }
            return actionContext
        }
    }
}