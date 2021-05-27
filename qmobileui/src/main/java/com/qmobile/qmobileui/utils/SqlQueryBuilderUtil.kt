/*
 * Created by Quentin Marciset on 11/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import androidx.sqlite.db.SimpleSQLiteQuery
import org.json.JSONArray
import org.json.JSONObject

class SqlQueryBuilderUtil(
    var tableName: String,
    private val searchField: JSONObject = QMobileUiUtil.appUtilities.searchField // has columns to Filter
) {

    fun getAll() = SimpleSQLiteQuery("SELECT * FROM $tableName")

    fun sortQuery(dataToSort: String): SimpleSQLiteQuery {
        val stringBuffer = StringBuffer("SELECT * FROM $tableName WHERE ")
        if (searchField.has(tableName)) {
            conditionAdder(searchField.getJSONArray(tableName), stringBuffer, dataToSort)
        }
        return SimpleSQLiteQuery(stringBuffer.toString().removeSuffix("OR "))
    }

    @Suppress("NestedBlockDepth")
    private fun conditionAdder(
        columnsToFilter: JSONArray,
        stringBuffer: StringBuffer,
        dataToSort: String
    ) {
        (0 until columnsToFilter.length()).forEach {
            val field = columnsToFilter[it]
            stringBuffer.append("`$field` LIKE \'%$dataToSort%\' OR ")
            QMobileUiUtil.appUtilities.customFormatters[tableName]?.get(field)
                ?.let { fieldMapping ->
                    if (fieldMapping.binding == "localizedText") {
                        when (fieldMapping.choiceList) {
                            is Map<*, *> -> {
                                for ((key, value) in fieldMapping.choiceList) {
                                    if ((value as? String?)?.containsIgnoreCase(dataToSort) == true) {
                                        stringBuffer.append("`$field` == \'$key\' OR ")
                                    }
                                }
                            }
                            is List<*> -> {
                                for ((i, value) in fieldMapping.choiceList.withIndex()) {
                                    if ((value as? String?)?.containsIgnoreCase(dataToSort) == true) {
                                        stringBuffer.append("`$field` == \'$i\' OR ")
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }
}
