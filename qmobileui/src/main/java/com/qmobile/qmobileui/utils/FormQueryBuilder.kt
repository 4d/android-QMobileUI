/*
 * Created by Quentin Marciset on 11/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import androidx.sqlite.db.SimpleSQLiteQuery
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobileui.action.sort.Sort
import com.qmobile.qmobileui.action.utils.SectionHelper
import timber.log.Timber

class FormQueryBuilder(
    var tableName: String,
    private val searchFields: List<String>? = BaseApp.runtimeDataHolder.tableInfo[tableName]?.searchFields,
    private val customSortFields: LinkedHashMap<String, String>? = null // used for Input control datasource
) {

    private val baseQuery = "SELECT * FROM $tableName"

    fun getQuery(pattern: String = ""): SimpleSQLiteQuery {
        val sortFieldsWithSection = SectionHelper.addSectionSortIfNeeded(tableName, customSortFields)
        val sortQuery = getSortQuery(sortFieldsWithSection)

        val sectionRelationQuery = SectionHelper.getSectionRelationQuery(tableName)

        if (pattern.isEmpty()) {
            return SimpleSQLiteQuery(baseQuery + sectionRelationQuery + sortQuery)
        }

        val stringBuilder = if (sectionRelationQuery.isNotEmpty()) {
            StringBuilder("SELECT * FROM $tableName $sectionRelationQuery AND ")
        } else {
            StringBuilder("SELECT * FROM $tableName AS T1 WHERE ")
        }
        searchFields?.let { columnsToFilter ->
            SearchQueryBuilder
                .appendPredicate(tableName, stringBuilder, columnsToFilter, pattern, sortQuery)
        }
        return SimpleSQLiteQuery(stringBuilder.toString().removeSuffix(" OR "))
    }

    fun getInputControlQuery(pattern: String = ""): SimpleSQLiteQuery {
        val sortQuery = getSortQuery()

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
        path: String
    ): SimpleSQLiteQuery {
        val sortQuery = getSortQuery()

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

    private fun getSortQuery(fields: LinkedHashMap<String, String>? = null): String {
        val sortFields = fields ?: customSortFields ?: Sort.getSortFieldsFromSharedPrefs(tableName) ?: return ""
        val sortStringBuffer = StringBuffer()
        sortFields.entries.forEach { entry ->
            if (sortStringBuffer.isEmpty()) {
                sortStringBuffer.append(" ORDER BY ${entry.key} ${entry.value}")
            } else {
                sortStringBuffer.append(", ${entry.key} ${entry.value}")
            }
        }
        return sortStringBuffer.toString()
    }
}
