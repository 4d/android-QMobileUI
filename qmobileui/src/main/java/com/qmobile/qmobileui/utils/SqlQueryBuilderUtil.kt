/*
 * Created by Quentin Marciset on 11/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import androidx.sqlite.db.SimpleSQLiteQuery
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatasync.app.BaseApp
import org.json.JSONArray
import org.json.JSONObject

class SqlQueryBuilderUtil(
    var tableName: String,
    private val searchField: JSONObject = QMobileUiUtil.appUtilities.searchField // has columns to Filter
) {

    fun getAll() = SimpleSQLiteQuery("SELECT * FROM $tableName")

    fun sortQuery(dataToSort: String): SimpleSQLiteQuery {
        val stringBuffer = StringBuffer("SELECT * FROM $tableName AS T1 WHERE ")
        if (searchField.has(tableName)) {
            searchField.getSafeArray(tableName)
                ?.let { conditionAdder(it, stringBuffer, dataToSort) }
        }
        return SimpleSQLiteQuery(stringBuffer.toString().removeSuffix("OR "))
    }

    private fun conditionAdder(
        columnsToFilter: JSONArray,
        stringBuffer: StringBuffer,
        dataToSort: String
    ) {
        (0 until columnsToFilter.length()).forEach eachColumn@{
            val field = columnsToFilter.getSafeString(it)
            if (field !is String) return@eachColumn

            if (field.contains(".")) { // manager.FirstName

                val relation = field.split(".")[0] // manager
                val relatedField = field.split(".")[1] // FirstName
                val relatedTableName =
                    BaseApp.genericTableHelper.getRelatedTableName(tableName, relation)

                stringBuffer.append(
                    "EXISTS ( SELECT * FROM $relatedTableName as T2 WHERE " +
                        "T1.__${relation}Key = T2.__KEY AND T2.$relatedField " +
                        "LIKE '%$dataToSort%' ) OR "
                )
            } else {
                stringBuffer.append("`$field` LIKE \'%$dataToSort%\' OR ")
                appendFromFormat(field, stringBuffer, dataToSort)
            }
        }
    }

    private fun appendFromFormat(
        field: String,
        stringBuffer: StringBuffer,
        dataToSort: String
    ) {
        QMobileUiUtil.appUtilities.customFormatters[tableName]?.get(field)?.let { fieldMapping ->
            if (fieldMapping.binding == "localizedText") {
                when (fieldMapping.choiceList) {
                    is Map<*, *> -> {
                        appendFromFormatMap(
                            fieldMapping.choiceList,
                            field,
                            stringBuffer,
                            dataToSort
                        )
                    }
                    is List<*> -> {
                        appendFromFormatList(
                            fieldMapping.choiceList,
                            field,
                            stringBuffer,
                            dataToSort
                        )
                    }
                }
            }
        }
    }

    private fun appendFromFormatMap(
        choiceList: Map<*, *>,
        field: String,
        stringBuffer: StringBuffer,
        dataToSort: String
    ) {
        for ((key, value) in choiceList) {
            if ((value as? String?)?.containsIgnoreCase(dataToSort) == true) {
                stringBuffer.append("`$field` == \'$key\' OR ")
            }
        }
    }

    private fun appendFromFormatList(
        choiceList: List<*>,
        field: String,
        stringBuffer: StringBuffer,
        dataToSort: String
    ) {
        for ((i, value) in choiceList.withIndex()) {
            if ((value as? String?)?.containsIgnoreCase(dataToSort) == true) {
                stringBuffer.append("`$field` == \'$i\' OR ")
            }
        }
    }
}
