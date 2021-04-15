/*
 * Created by Maturi Karthik on 2/4/2021.
 * 4D SAS
 * Copyright (c) 2021 Maturi Karthik. All rights reserved.
 */

package com.qmobile.qmobileui

import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.qmobile.qmobileui.model.AppUtilities
import com.qmobile.qmobileui.utils.QMobileUiUtil
import com.qmobile.qmobileui.utils.SqlQueryBuilderUtil
import io.mockk.every
import io.mockk.mockkObject
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SqlBuilderTest {

    private val tableJson: JSONObject = JSONObject().put(
        "searchableField",
        mutableListOf(
            createTable("Employee", listOf("LastName", "LastName")),
            createTable("Service", listOf("name", "employeeNumber"))
        )
    )

    private fun createTable(
        tableName: String,
        listOfColumn: List<String>
    ): HashMap<String, List<String>> {
        val table = HashMap<String, List<String>>()
        table[tableName] = listOfColumn
        return table
    }

    @Test
    fun testSqlGetAllQuery() {
        val sqlQueryBuilder = SqlQueryBuilderUtil(tableName = "Service", searchField = tableJson)
        val actualQueryResult = sqlQueryBuilder.getAll().sql
        val expectedQueryResult = SimpleSQLiteQuery("SELECT * FROM Service").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSearchableField() {
        mockkObject(QMobileUiUtil)
        every { QMobileUiUtil.appUtilities } returns AppUtilities(
            0,
            true,
            "",
            tableJson,
            tableJson,
            tableJson,
            "12.5",
            Log.DEBUG,
            ""
        )
        Assert.assertEquals(tableJson, QMobileUiUtil.appUtilities.searchField)
    }
}
