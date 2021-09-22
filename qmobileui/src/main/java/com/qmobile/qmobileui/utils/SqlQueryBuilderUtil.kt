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
    private val searchField: JSONObject = BaseApp.runtimeDataHolder.searchField // has columns to Filter
) {

    fun getAll() = SimpleSQLiteQuery("SELECT * FROM $tableName")

    fun setRelationQuery(parentTableName: String? = null, parentItemId: String? = null, relatedEntities: Array<String>): SimpleSQLiteQuery {
//        val stringBuffer = StringBuffer("SELECT * FROM $tableName AS T1 WHERE EXISTS (SELECT * FROM $parentTableName AS T2 WHERE T2.__KEY = $parentItemId AND  )")
        val stringBuffer = StringBuffer("SELECT * FROM $tableName AS T1 WHERE T1.__KEY in (${
            relatedEntities.joinToString(",") { "'$it'" }
        })")
        return SimpleSQLiteQuery(stringBuffer.toString())
    }

    fun sortQuery(dataToSort: String): SimpleSQLiteQuery {
        val stringBuffer = StringBuffer("SELECT * FROM $tableName AS T1 WHERE ")
        searchField.getSafeArray(tableName)?.let {
            conditionAdder(it, stringBuffer, dataToSort)
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
                        "T1.__${relation}Key = T2.__KEY AND "
                )
                val appendFromFormat = appendFromFormat(field, dataToSort, "T2.$relatedField")
                if (appendFromFormat.isEmpty()) {
                    stringBuffer.append("T2.$relatedField LIKE '%$dataToSort%' OR ")
                } else {
                    stringBuffer.append("( T2.$relatedField LIKE '%$dataToSort%' OR $appendFromFormat")
                    stringBuffer.removeSuffix("OR ")
                    stringBuffer.append(") ")
                }
                stringBuffer.removeSuffix("OR ")
                stringBuffer.append(") OR ")
            } else {
                stringBuffer.append("`$field` LIKE \'%$dataToSort%\' OR ")
                stringBuffer.append(appendFromFormat(field, dataToSort))
            }
        }
    }

    private fun appendFromFormat(
        field: String,
        dataToSort: String,
        relatedField: String? = null
    ): String {
        var appendice = ""
        BaseApp.runtimeDataHolder.customFormatters[tableName.tableNameAdjustment()]?.get(field.fieldAdjustment())
            ?.let { fieldMapping ->
                if (fieldMapping.binding == "localizedText") {

                    val fieldForQuery: String = relatedField ?: field
                    val choiceList = fieldMapping.choiceList
                    appendice = when (choiceList) {
                        is Map<*, *> -> {
                            appendFromFormatMap(
                                choiceList,
                                fieldForQuery,
                                dataToSort
                            )
                        }
                        is List<*> -> {
                            appendFromFormatList(
                                choiceList,
                                fieldForQuery,
                                dataToSort
                            )
                        }
                        else -> ""
                    }
                }
            }
        return appendice
    }

    private fun appendFromFormatMap(
        choiceList: Map<*, *>,
        field: String,
        dataToSort: String
    ): String {
        var appendice = ""
        for ((key, value) in choiceList) {
            if ((value as? String?)?.containsIgnoreCase(dataToSort) == true) {
                appendice += "$field == \'$key\' OR "
            }
        }
        return appendice
    }

    private fun appendFromFormatList(
        choiceList: List<*>,
        field: String,
        dataToSort: String
    ): String {
        var appendice = ""
        for ((i, value) in choiceList.withIndex()) {
            if ((value as? String?)?.containsIgnoreCase(dataToSort) == true) {
                appendice += "$field == \'$i\' OR "
            }
        }
        return appendice
    }

    private fun StringBuffer.removeSuffix(suffix: String) {
        if (this.toString().endsWith(suffix))
            this.replace(
                this.toString().length - (suffix.length + 1),
                this.toString().length - 1,
                ""
            )
    }
}
