package com.qmobile.qmobileui.action

import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeString
import org.json.JSONArray
import org.json.JSONObject

class ActionFactory {
    companion object {
        fun createActionFromJsonObject(jsonObject: JSONObject): Action {
            jsonObject.apply {
                return Action(
                    name = getSafeString("name") ?: "",
                    icon = getSafeString("icon"),
                    shortLabel = getString("shortLabel"),
                    label = getString("label"),
                    parameters = getSafeArray("parameters") ?: JSONArray()
                )
            }
        }
    }
}