/*
 * Created by qmarciset on 6/5/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.utils.containsIgnoreCase
import com.qmobile.qmobiledatasync.utils.fieldAdjustment
import com.qmobile.qmobiledatasync.utils.tableNameAdjustment
import com.qmobile.qmobileui.utils.SearchQueryBuilder.removeSuffix
import org.json.JSONArray
import timber.log.Timber

object SearchQueryBuilder {

    fun appendPredicate(
        tableName: String,
        stringBuilder: StringBuilder,
        columnsToFilter: JSONArray,
        pattern: String,
        sortQuery: String? = null
    ) {
        (0 until columnsToFilter.length()).forEach eachColumn@{ index ->
            val fieldName = columnsToFilter.getSafeString(index)
            if (fieldName !is String) return@eachColumn

            if (fieldName.contains(".")) { // manager.FirstName

                val pathWithoutFieldName = fieldName.substringBeforeLast(".")

                val relation = if (pathWithoutFieldName.contains(".")) { // alias
                    RelationHelper.getRelations(tableName).find { it.path == pathWithoutFieldName }
                } else { // basic relation
                    RelationHelper.getRelations(tableName).find { it.name == pathWithoutFieldName }
                }

                relation?.let { rel ->
                    appendRelationQuery(tableName, stringBuilder, fieldName, pattern, rel, sortQuery)
                }
            } else {
                stringBuilder.append("T1.$fieldName LIKE \'%$pattern%\' OR ")
                stringBuilder.append(appendFromFormat(tableName, fieldName, pattern, "T1.$fieldName"))
            }
        }

        sortQuery?.let { query ->
            stringBuilder.removeSuffix(" OR ")
            stringBuilder.append(query)
        }
    }

    private fun appendRelationQuery(
        tableName: String,
        stringBuilder: StringBuilder,
        fieldName: String,
        pattern: String,
        relation: Relation,
        sortQuery: String? = null
    ) {
        val baseQuery = getBaseQuery(relation)
        stringBuilder.append(baseQuery)
        val depth = fieldName.count { it == '.' }
        val endFieldName = fieldName.substringAfterLast(".")
        val appendFromFormat = appendFromFormat(tableName, fieldName, pattern, "T${depth + 1}.$endFieldName")
        if (appendFromFormat.isNotEmpty())
            stringBuilder.append("( ")
        stringBuilder.append("T${depth + 1}.$endFieldName LIKE \'%$pattern%\' OR ")
        stringBuilder.append(appendFromFormat)
        stringBuilder.removeSuffix(" OR ")
        if (appendFromFormat.isNotEmpty())
            stringBuilder.append(" )")
        repeat(baseQuery.count { it == '(' }) {
            stringBuilder.append(" )")
        }
        stringBuilder.append(" OR ")

        sortQuery?.let { query ->
            stringBuilder.removeSuffix(" OR ")
            stringBuilder.append(query)
        }
    }

    private fun appendFromFormat(
        tableName: String,
        fieldName: String,
        pattern: String,
        fieldForQuery: String
    ): String {
        var appendice = ""
        BaseApp.runtimeDataHolder.customFormatters[tableName.tableNameAdjustment()]?.get(fieldName.fieldAdjustment())
            ?.let { fieldMapping ->
                if (fieldMapping.binding == "localizedText") {

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
            this.replace(this.length - (suffix.length), this.length, "")
    }

    private fun getBaseQuery(relation: Relation): String {
        val path = relation.path.ifEmpty { relation.name }
        val newPath = RelationHelper.unAliasPath(path, relation.source)
        Timber.d("newPath: $newPath")

        val pathList = newPath.split(".")
        return depthRelation(relation, pathList, 0)
    }

    private fun depthRelation(parent: Relation, path: List<String>, depth: Int): String {

        val source = if (depth == 0) parent.source else parent.dest
        val relation = RelationHelper.getRelation(source, path[depth])

        val query = StringBuilder()

        when (depth) {
            0 -> { // first
                query.append("EXISTS ( ")
                query.append(partQuery(relation, depth + 1))
                if (path.size > 1) {
                    query.append(" AND EXISTS ( ")
                    query.append(depthRelation(relation, path, depth + 1))
                }
                query.append(" AND ")
            }
            path.size - 1 -> { // last
                query.append(partQuery(relation, depth + 1))
            }
            else -> {
                query.append(partQuery(relation, depth + 1))
                query.append(" AND EXISTS ( ")
                query.append(depthRelation(relation, path, depth + 1))
            }
        }

        return query.toString()
    }

    private fun partQuery(relation: Relation, depth: Int): String {
        return if (relation.type == Relation.Type.MANY_TO_ONE)
            "SELECT * FROM ${relation.dest} AS T${depth + 1} WHERE T${depth + 1}.__KEY = T$depth.__${relation.name}Key"
        else
            "SELECT * FROM ${relation.source} AS T$depth WHERE T${depth + 1}.__${relation.inverse}Key = T$depth.__KEY"
    }
}
