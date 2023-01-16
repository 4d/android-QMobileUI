/*
 * Created by htemanni on 19/9/2022.
 * 4D SAS
 * Copyright (c) 2022 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui.action.utils

import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobileui.action.sort.Sort
import com.qmobile.qmobileui.action.sort.Sort.getTypeConstraints

object SectionHelper {

    private const val SOURCE_TABLE_SQL_NAME = "T1"
    private const val DESTINATION_TABLE_SQL_NAME = "T2"

    // To group items by a field section, we should add the field in the first position of sortFields list. This function
    // takes the sortList (used for sort actions) as parameter and add the section field (if exists) in the first
    // position. Returns all in LinkedHashMap (LinkedHashMap keeps order of item)
    fun addSectionSortIfNeeded(
        tableName: String,
        customSortFields: LinkedHashMap<String, String>? = null // here for unit tests
    ): LinkedHashMap<String, String>? {
        val sectionField = BaseApp.genericTableHelper.getSectionFieldForTable(tableName)
        var sectionFieldName = sectionField?.name

        if (sectionFieldName?.contains(".") == true) {
            sectionFieldName = "$DESTINATION_TABLE_SQL_NAME.${sectionFieldName.split(".").last()}"
        }

        val sortFields = customSortFields ?: Sort.getSortFieldsFromSharedPrefs(tableName)

        if (!sectionFieldName.isNullOrEmpty()) {
            val sectionFieldType = sectionField?.type
            val key = getTypeConstraints(sectionFieldName, sectionFieldType, Sort.Order.ASCENDING.value)
            val sortListWithSection = linkedMapOf(key to Sort.Order.ASCENDING.value)
            if (sortFields != null) {
                sortListWithSection.putAll(sortFields)
            }
            return sortListWithSection
        }
        return sortFields
    }

    fun getSectionRelationQuery(tableName: String): String {
        BaseApp.genericTableHelper.getSectionFieldForTable(tableName)?.path?.let { sectionFieldPath ->
            if (sectionFieldPath.contains(".")) {
                val relationName = sectionFieldPath.split(".").first()
                val sectionRelation = RelationHelper.getRelation(tableName, relationName)
                return getQuery(sectionRelation)
            }
        }
        return ""
    }

    private fun getQuery(relation: Relation): String {
        return " AS $SOURCE_TABLE_SQL_NAME LEFT JOIN ${relation.dest} " +
            "$DESTINATION_TABLE_SQL_NAME WHERE " +
            "$DESTINATION_TABLE_SQL_NAME.__KEY = $SOURCE_TABLE_SQL_NAME.__${relation.name}KEY"
    }
}
