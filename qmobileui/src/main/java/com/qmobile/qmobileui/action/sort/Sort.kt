package com.qmobile.qmobileui.action.sort

import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.utils.fieldAdjustment
import org.json.JSONObject

object Sort {

    fun getSortFieldsFromSharedPrefs(tableName: String): LinkedHashMap<String, String>? {
        val parametersToSortWith = BaseApp.sharedPreferencesHolder.parametersToSortWith
        // Json object containing all sort fields : Map<tableName, MapOf<fieldName, order (asc/desc))>>
        parametersToSortWith.getSafeString(tableName)?.let { fieldsToSortCurrentTableJsonString ->
            // Json object only current table sort fields :  MapOf<fieldName, order (asc/desc)>
            val currentTableFieldsJsonObject = JSONObject(fieldsToSortCurrentTableJsonString)
            return currentTableFieldsJsonObject.getSortByTable()
        }
        return null
    }

    // Extracting the json content to a hashmap
    private fun JSONObject.getSortByTable(): LinkedHashMap<String, String> {
        val fieldsToSortCurrentTable: LinkedHashMap<String, String> = LinkedHashMap()
        val keysItr = this.keys()
        while (keysItr.hasNext()) {
            val key = keysItr.next()
            this.getSafeString(key)?.let { value ->
                fieldsToSortCurrentTable[key] = value
            }
        }
        return fieldsToSortCurrentTable
    }

    fun saveSortChoice(tableName: String, fieldsToSortBy: Map<String, String>?) {
        fieldsToSortBy?.let {
            BaseApp.sharedPreferencesHolder.parametersToSortWith =
                BaseApp.sharedPreferencesHolder.parametersToSortWith.put(tableName, JSONObject(it).toString())
        }
    }

    fun getDefaultSortField(tableName: String): Map<String, String>? {
        val defaultFieldToSortWith = BaseApp.runtimeDataHolder.tableInfo[tableName]?.defaultSortField
        return if (defaultFieldToSortWith != null) {
            mapOf(defaultFieldToSortWith.fieldAdjustment() to Order.ASCENDING.value)
        } else {
            null
        }
    }

    fun sortMatchingKeywords(format: String?): String {
        return when (format) {
            "ascending" -> Order.ASCENDING.value
            "descending" -> Order.DESCENDING.value
            else -> ""
        }
    }

    fun getTypeConstraints(field: String, type: String?, order: String? = null): String {
        // if the field is a time we have to convert it from string to int, otherwise the AM/PM sort will not work
        // if type is string we make the sort case insensitive
        return when (type) {
            "time" -> "CAST ($field AS INT)"
            "string" -> "$field COLLATE NOCASE"
            "date" -> { // order by year then month and finally day
                "replace($field, rtrim($field," +
                        " replace($field, '!', '')), '') ${order?: Order.ASCENDING.value} ," +   //year
                        "  substr(replace ($field, substr($field, 0, 1+instr($field, '!')),\"\"), 0, " + // Month
                        "instr(   replace ($field, substr($field, 0, 1+instr($field, '!')),\"\"), " +
                        "'!')) ${order?: Order.ASCENDING.value} ," +
                        "   substr($field, 0, instr($field, '!'))" // Day
            }
            else -> field
        }
    }
    enum class Order(val value: String, val verbose: String) {
        ASCENDING("ASC", "ascending"),
        DESCENDING("DESC", "descending")
    }
}
