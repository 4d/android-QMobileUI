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

object SectionHelper {

    private const val SOURCE_TABLE_SQL_NAME = "T1"
    private const val DESTINATION_TABLE_SQL_NAME = "T2"

    // To group items by a field section, we should add the field in the first position of sortFields list. This function
    // takes the sortList (used for sort actions) as parameter and add the section field (if exists) in the first
    // position. Returns all in LinkedHashMap (LinkedHashMap keeps order of item)
    fun addSectionSortIfNeeded(tableName: String): LinkedHashMap<String, String>? {
        var sectionField = BaseApp.genericTableHelper.getSectionFieldForTable(tableName)?.name

        if (!sectionField.isNullOrEmpty()) {
            if (sectionField.contains(".")) {
                sectionField = "$DESTINATION_TABLE_SQL_NAME.${sectionField.split(".").last()}"
            }
        }

        val sortFields = Sort.getSortFieldsFromSharedPrefs(tableName)

        val sectionFieldType = BaseApp.genericTableHelper.getSectionFieldForTable(tableName)?.type
        if (!sectionField.isNullOrEmpty()) {
            val key = ActionHelper.getSortFieldKeyForType(sectionFieldType, sectionField)
            val sortListWithSection = linkedMapOf(key to Sort.Order.ASCENDING.value)
            if (sortFields != null) {
                sortListWithSection.putAll(sortFields.filter { !it.key.contains(sectionField) })
            }
            return sortListWithSection
        }
        return sortFields
    }

    fun getSectionRelationQuery(tableName: String): String {
        BaseApp.genericTableHelper.getSectionFieldForTable(tableName)?.path?.let { sectionPath ->
            if (sectionPath.contains(".")) {
                val relationName = sectionPath.split(".").first()
                val sectionRelation = RelationHelper.getRelation(tableName, relationName)
                return getQuery(sectionRelation)
            }
        }
        return ""
    }

    private fun getQuery(relation: Relation): String {
        return " AS $SOURCE_TABLE_SQL_NAME LEFT  JOIN ${relation.dest} " +
            " $DESTINATION_TABLE_SQL_NAME WHERE " +
            "$DESTINATION_TABLE_SQL_NAME.__KEY = $SOURCE_TABLE_SQL_NAME.__${relation.name}KEY"
    }
}
