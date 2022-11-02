/*
 * Created by Quentin Marciset on 11/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import androidx.sqlite.db.SimpleSQLiteQuery
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.relation.RelationQueryBuilderSection
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
        var query = baseQuery
        val sortFields = customSortFields ?: Sort.getSortFieldsFromSharedPrefs(tableName)
        val sortFieldsWithSection = SectionHelper.addSectionSortIfNeeded(tableName, sortFields)
        val sectionPath = BaseApp.genericTableHelper.getSectionFieldForTable(tableName)?.path

        var relationPartQuery: String = ""

        if (!sectionPath.isNullOrEmpty()) {
            if (sectionPath.contains(".")) {
                val sectionRelation = RelationHelper.getRelation(
                    tableName,
                    sectionPath.split(".").first()
                )
                sectionRelation.let {
                    relationPartQuery = RelationQueryBuilderSection.createQuery(
                        it
                    ).sql
                }
            }
        }

        val sortQuery = getSortQuery(sortFieldsWithSection)
        if (pattern.isEmpty()) {
            return SimpleSQLiteQuery(query + relationPartQuery + sortQuery)
        }

        val stringBuilder = if (relationPartQuery.isNotEmpty()) {
            StringBuilder("SELECT * FROM $tableName $relationPartQuery AND ")
        } else {
            StringBuilder("SELECT * FROM $tableName AS T1 WHERE ")
        }
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
