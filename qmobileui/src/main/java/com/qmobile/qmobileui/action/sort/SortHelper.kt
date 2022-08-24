package com.qmobile.qmobileui.action.sort

import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatasync.app.BaseApp
import org.json.JSONObject

object SortHelper {
    @Suppress("NestedBlockDepth")
    fun getSortFieldsForTable(tableName: String): LinkedHashMap<String, String>? {
        val parametersToSortWith = BaseApp.sharedPreferencesHolder.parametersToSortWith
        if (parametersToSortWith.isNotEmpty()) {
            // Json object containing all sort fields : Map<tableName, MapOf<fieldName, order (asc/desc))>>
            val jsonObject = JSONObject(parametersToSortWith)
            jsonObject.getSafeString(tableName)?.let { fieldsToSortCurrentTableJsonString ->
                val fieldsToSortCurrentTable: LinkedHashMap<String, String> = LinkedHashMap()
                // Json object only current table sort fields :  MapOf<fieldName, order (asc/desc)>
                val currentTableFieldsJsonObject = JSONObject(fieldsToSortCurrentTableJsonString)
                // Extracting the json content to a hashmap
                val keysItr = currentTableFieldsJsonObject.keys()
                while (keysItr.hasNext()) {
                    val key = keysItr.next()
                    currentTableFieldsJsonObject.getSafeString(key)?.let { value ->
                        fieldsToSortCurrentTable[key] = value
                    }
                }
                return fieldsToSortCurrentTable
            }
        }
        return null
    }
}
