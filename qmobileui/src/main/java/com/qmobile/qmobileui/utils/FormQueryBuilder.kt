/*
 * Created by Quentin Marciset on 11/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import androidx.sqlite.db.SimpleSQLiteQuery
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.RelationHelper
import org.json.JSONObject
import timber.log.Timber

class FormQueryBuilder(
    var tableName: String,
    private val searchField: JSONObject = BaseApp.runtimeDataHolder.searchField // has columns to filter
) {

    private val baseQuery = "SELECT * FROM $tableName"

    fun getQuery(pattern: String = ""): SimpleSQLiteQuery {
        if (pattern.isEmpty())
            return SimpleSQLiteQuery(baseQuery)

        val stringBuilder = StringBuilder("SELECT * FROM $tableName AS T1 WHERE ")
        searchField.getSafeArray(tableName)?.let { columnsToFilter ->
            SearchQueryBuilder.appendPredicate(tableName, stringBuilder, columnsToFilter, pattern)
        }
        return SimpleSQLiteQuery(stringBuilder.toString().removeSuffix(" OR "))
    }

    fun getRelationQuery(
        parentItemId: String,
        pattern: String = "",
        parentTableName: String,
        path: String
    ): SimpleSQLiteQuery {

        val relation = if (path.contains("."))
            RelationHelper.getRelations(parentTableName).find { it.path == path }
        else
            RelationHelper.getRelations(parentTableName).find { it.name == path }

        relation?.let {
            val query = DeepQueryBuilder.createQuery(relation, parentItemId)
            return if (pattern.isEmpty()) {
                SimpleSQLiteQuery(query)
            } else {

                val stringBuilder = StringBuilder("$query AND ( ")
                searchField.getSafeArray(tableName)?.let { columnsToFilter ->
                    SearchQueryBuilder.appendPredicate(tableName, stringBuilder, columnsToFilter, pattern)
                }
                SimpleSQLiteQuery(stringBuilder.toString().removeSuffix(" OR ").plus(" )"))
            }
        } ?: kotlin.run {
            Timber.e("Missing relation with path [$path] from table [$tableName]")
            return SimpleSQLiteQuery("$baseQuery WHERE __KEY = -1")
        }
    }
}
