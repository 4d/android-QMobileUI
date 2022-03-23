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
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.utils.containsIgnoreCase
import com.qmobile.qmobiledatasync.utils.fieldAdjustment
import com.qmobile.qmobiledatasync.utils.tableNameAdjustment
import org.json.JSONArray
import org.json.JSONObject

class FormQueryBuilder(
    var tableName: String,
    private val searchField: JSONObject = BaseApp.runtimeDataHolder.searchField // has columns to Filter
) {

    private val baseQuery = "SELECT * FROM $tableName"

    fun getQuery(pattern: String? = null): SimpleSQLiteQuery {
        if (pattern.isNullOrEmpty())
            return SimpleSQLiteQuery(baseQuery)
        val stringBuilder = StringBuilder("$baseQuery AS T_FINAL WHERE ")
        searchField.getSafeArray(tableName)?.let { columnsToFilter ->
            appendPredicate(stringBuilder, columnsToFilter, pattern)
        }
        return SimpleSQLiteQuery(stringBuilder.toString().removeSuffix("OR "))
    }

    fun getRelationQuery(
        pattern: String? = null,
        pathQuery: String
    ): SimpleSQLiteQuery {
//        val baseRelationQuery = "$baseQuery AS T1 WHERE T1.__${inverseName}Key = $parentItemId"
        if (pattern.isNullOrEmpty())
            return SimpleSQLiteQuery(pathQuery)
        val stringBuilder = StringBuilder("$pathQuery AND ( ")
        searchField.getSafeArray(tableName)?.let { columnsToFilter ->
            appendPredicate(stringBuilder, columnsToFilter, pattern)
        }
        return SimpleSQLiteQuery(stringBuilder.toString().removeSuffix("OR ").plus(")"))
    }

    private fun appendPredicate(
        stringBuilder: StringBuilder,
        columnsToFilter: JSONArray,
        pattern: String
    ) {
        (0 until columnsToFilter.length()).forEach eachColumn@{
            val field = columnsToFilter.getSafeString(it)
            if (field !is String) return@eachColumn

            if (field.contains(".")) { // manager.FirstName

                val relation = field.split(".")[0] // manager
                val relatedField = field.split(".")[1] // FirstName

                val relatedTableName = RelationHelper.getRelation(tableName, relation).dest

                stringBuilder.append(
                    "EXISTS ( SELECT * FROM $relatedTableName AS S2 WHERE " +
                        "T_FINAL.__${relation}Key = S2.__KEY AND "
                )
                val appendFromFormat = appendFromFormat(field, pattern, "S2.$relatedField")
                if (appendFromFormat.isEmpty()) {
                    stringBuilder.append("S2.$relatedField LIKE '%$pattern%' OR ")
                } else {
                    stringBuilder.append("( S2.$relatedField LIKE '%$pattern%' OR $appendFromFormat")
                    stringBuilder.removeSuffix("OR ")
                    stringBuilder.append(") ")
                }
                stringBuilder.removeSuffix("OR ")
                stringBuilder.append(") OR ")
            } else {
                stringBuilder.append("`$field` LIKE \'%$pattern%\' OR ")
                stringBuilder.append(appendFromFormat(field, pattern))
            }
        }
    }

    private fun appendFromFormat(
        field: String,
        pattern: String,
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
                                pattern
                            )
                        }
                        is List<*> -> {
                            appendFromFormatList(
                                choiceList,
                                fieldForQuery,
                                pattern
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
        pattern: String
    ): String {
        var appendice = ""
        for ((key, value) in choiceList) {
            if ((value as? String?)?.containsIgnoreCase(pattern) == true) {
                appendice += "$field == \'$key\' OR "
            }
        }
        return appendice
    }

    private fun appendFromFormatList(
        choiceList: List<*>,
        field: String,
        pattern: String
    ): String {
        var appendice = ""
        for ((i, value) in choiceList.withIndex()) {
            if ((value as? String?)?.containsIgnoreCase(pattern) == true) {
                appendice += "$field == \'$i\' OR "
            }
        }
        return appendice
    }

    private fun StringBuilder.removeSuffix(suffix: String) {
        if (this.endsWith(suffix))
            this.replace(this.length - (suffix.length + 1), this.length - 1, "")
    }
}
