/*
 * Created by Maturi Karthik on 2/4/2021.
 * 4D SAS
 * Copyright (c) 2021 Maturi Karthik. All rights reserved.
 */

package com.qmobile.qmobileui

import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.utils.RuntimeDataHolder
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
        mockkObject(BaseApp)
        every { BaseApp.runtimeDataHolder } returns RuntimeDataHolder(
            initialGlobalStamp = 0,
            guestLogin = true,
            remoteUrl = "",
            searchField = tableJson,
            sdkVersion = "12.5",
            logLevel = Log.DEBUG,
            dumpedTables = "",
            queries = mutableMapOf(),
            tableProperties = mutableMapOf(),
            customFormatters = mapOf(),
            embeddedFiles = mutableListOf()
        )
        Assert.assertEquals(tableJson, BaseApp.runtimeDataHolder.searchField)
    }
}
