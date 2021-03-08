/*
 * Created by Quentin Marciset on 11/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import androidx.sqlite.db.SimpleSQLiteQuery
import org.json.JSONArray
import org.json.JSONObject

class SqlQueryBuilderUtil(
    var tableName: String,
    private val searchField: JSONObject = QMobileUiUtil.appUtilities.searchField // has columns to Filter
) {

    val getAll = { SimpleSQLiteQuery("SELECT * FROM $tableName") }

    fun sortQuery(dataToSort: String): SimpleSQLiteQuery {
        val stringBuffer = StringBuffer("SELECT * FROM $tableName WHERE  ")
        if (searchField.has(tableName)) {
            conditionAdder(searchField.getJSONArray(tableName), stringBuffer, dataToSort)
        }
        return SimpleSQLiteQuery(stringBuffer.toString())
    }

    private fun conditionAdder(
        columnsToFilter: JSONArray,
        stringBuffer: StringBuffer,
        dataToSort: String
    ) {
        (0 until columnsToFilter.length()).forEach {
            when {
                (columnsToFilter.length() == 1) ->
                    stringBuffer.append("\"${columnsToFilter[it]}\" LIKE  \'%$dataToSort%\' ")
                (it == (columnsToFilter.length() - 1)) ->
                    stringBuffer.append("\"${columnsToFilter[it]}\" LIKE  \'%$dataToSort%\' ")
                else ->
                    stringBuffer.append("\"${columnsToFilter[it]}\" LIKE  \'%$dataToSort%\'  OR ")
            }
        }
    }
}
