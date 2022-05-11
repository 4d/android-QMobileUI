/*
 * Created by qmarciset on 6/5/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.relation.RelationHelper
import timber.log.Timber
import java.lang.StringBuilder

object DeepQueryBuilder {

    fun createQuery(relation: Relation, parentItemId: String): String {

        val path = relation.path.ifEmpty { relation.name }
        val newPath = RelationHelper.unAliasPath(path, relation.source)
        Timber.d("newPath: $newPath")

        val pathList = newPath.split(".")

        val hasDepth = pathList.size > 1

        val builder = StringBuilder("SELECT * FROM ${relation.dest} AS T1 WHERE ")

        if (hasDepth) {
            builder.append(depthRelation(relation, pathList, 0, parentItemId))
            repeat(builder.count { it == '(' }) {
                builder.append(" )")
            }
        } else {
            builder.append(endCondition(relation, parentItemId, 1))
        }

        return builder.toString()
    }

    private fun depthRelation(parent: Relation, path: List<String>, depth: Int, parentItemId: String): String {

        val source = if (depth == 0) parent.source else parent.dest
        val relation = RelationHelper.getRelation(source, path[depth])

        val query = StringBuilder()

        when (depth) {
            0 -> { // first
                query.append("EXISTS ( ")
                query.append(depthRelation(relation, path, depth + 1, parentItemId))
                query.append(" AND EXISTS ( ")
                query.append(partQuery(relation, path.size))
                query.append(" AND ")
                query.append(endCondition(relation, parentItemId, path.size))
            }
            path.size - 1 -> { // last
                query.append(partQuery(relation, 1))
            }
            else -> {
                query.append(depthRelation(relation, path, depth + 1, parentItemId))
                query.append(" AND EXISTS ( ")
                query.append(partQuery(relation, path.size - depth))
            }
        }

        return query.toString()
    }

    private fun partQuery(relation: Relation, depth: Int): String {
        return if (relation.type == Relation.Type.MANY_TO_ONE)
            "SELECT * FROM ${relation.source} AS T${depth + 1} WHERE T$depth.__KEY = T${depth + 1}.__${relation.name}Key"
        else
            "SELECT * FROM ${relation.source} AS T${depth + 1} WHERE T$depth.__${relation.inverse}Key = T${depth + 1}.__KEY"
    }

    private fun endCondition(relation: Relation, parentItemId: String, depth: Int): String {
        return if (relation.type == Relation.Type.ONE_TO_MANY)
            "T$depth.__${relation.inverse}Key = $parentItemId"
        else
            "T${depth + 1}.__KEY = $parentItemId"
    }
}
