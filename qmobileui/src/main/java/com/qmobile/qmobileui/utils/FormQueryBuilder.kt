/*
 * Created by Quentin Marciset on 11/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import androidx.sqlite.db.SimpleSQLiteQuery
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.RelationHelper
import timber.log.Timber

class FormQueryBuilder(
    var tableName: String,
    private val searchFields: List<String>? = BaseApp.runtimeDataHolder.tableInfo[tableName]?.searchFields
) {

    private val baseQuery = "SELECT * FROM $tableName"

    fun getQuery(pattern: String = "", sortFields: LinkedHashMap<String, String>? = null): SimpleSQLiteQuery {
        val sortQuery = sortFields?.let { getSortQueryFromFieldList(it) } ?: ""

        if (pattern.isEmpty()) {
            return SimpleSQLiteQuery(baseQuery + sortQuery)
        }

        val stringBuilder = StringBuilder("SELECT * FROM $tableName AS T1 WHERE ")
        searchFields?.let { columnsToFilter ->
            SearchQueryBuilder
                .appendPredicate(tableName, stringBuilder, columnsToFilter, pattern, sortQuery)
        }
        return SimpleSQLiteQuery(stringBuilder.toString().removeSuffix(" OR "))
    }

    fun getRelationQuery(
        parentItemId: String,
        pattern: String = "",
        parentTableName: String,
        path: String,
        sortFields: LinkedHashMap<String, String>?
    ): SimpleSQLiteQuery {
        val sortQuery = sortFields?.let { getSortQueryFromFieldList(it) } ?: ""

        val relation = if (path.contains(".")) {
            RelationHelper.getRelations(parentTableName).find { it.path == path }
        } else {
            RelationHelper.getRelations(parentTableName).find { it.name == path }
        }

        relation?.let {
            val query = DeepQueryBuilder.createQuery(relation, parentItemId)
            return if (pattern.isEmpty()) {
                SimpleSQLiteQuery(query + sortQuery)
            } else {
                val stringBuilder = StringBuilder("$query AND ( ")
                searchFields?.let { columnsToFilter ->
                    SearchQueryBuilder.appendPredicate(tableName, stringBuilder, columnsToFilter, pattern)
                }
                SimpleSQLiteQuery(stringBuilder.toString().removeSuffix(" OR ").plus(" )"))
            }
        } ?: kotlin.run {
            Timber.e("Missing relation with path [$path] from table [$tableName]")
            return SimpleSQLiteQuery("$baseQuery WHERE __KEY = -1")
        }
    }

    private fun getSortQueryFromFieldList(fields: HashMap<String, String>): String? {
        val sortStringBuffer = StringBuffer()

        if (fields.isEmpty()) {
            return null
        } else {
            fields.entries.forEach {
                if (sortStringBuffer.isEmpty()) {
                    sortStringBuffer.append(" ORDER BY ${it.key} ${it.value}")
                } else {
                    sortStringBuffer.append(", ${it.key} ${it.value}")
                }
            }
        }
        return sortStringBuffer.toString()
    }
}
