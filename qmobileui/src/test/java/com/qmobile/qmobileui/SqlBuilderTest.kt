/*
 * Created by Maturi Karthik on 2/4/2021.
 * 4D SAS
 * Copyright (c) 2021 Maturi Karthik. All rights reserved.
 */

package com.qmobile.qmobileui

import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.utils.FieldMapping
import com.qmobile.qmobiledatasync.utils.GenericTableHelper
import com.qmobile.qmobiledatasync.utils.RuntimeDataHolder
import com.qmobile.qmobileui.utils.SqlQueryBuilderUtil
import io.mockk.every
import io.mockk.mockkObject
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SqlBuilderTest {

    private val searchFieldsJson: JSONObject = JSONObject().apply {
        put("Employee", listOf("LastName", "FirstName"))
        put("Service", listOf("name", "employeeNumber"))
    }

    @Before
    fun setup() {
        mockkObject(BaseApp)
    }

    @Test
    fun testSqlGetAllQuery() {
        val sqlQueryBuilder =
            SqlQueryBuilderUtil(tableName = "Service", searchField = searchFieldsJson)
        val actualQueryResult = sqlQueryBuilder.getAll().sql
        val expectedQueryResult = SimpleSQLiteQuery("SELECT * FROM Service").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSqlSortQuery() {
        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)
        mockRuntimeDataHolder.customFormatters = mapOf()
        BaseApp.runtimeDataHolder = mockRuntimeDataHolder

        val sqlQueryBuilder =
            SqlQueryBuilderUtil(tableName = "Service", searchField = searchFieldsJson)
        val actualQueryResult = sqlQueryBuilder.sortQuery("abc").sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Service AS T1 WHERE `name` LIKE '%abc%' OR `employeeNumber` LIKE '%abc%' ").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSqlSortQueryWithRelation() {
        val searchFields: JSONObject = JSONObject().apply {
            put("Table_3", listOf("LastName", "relation4.field_x"))
            put("Service", listOf("name", "employeeNumber"))
        }

        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)
        val mockGenericTableHelper = Mockito.mock(GenericTableHelper::class.java)
        mockRuntimeDataHolder.customFormatters = mapOf()
        BaseApp.runtimeDataHolder = mockRuntimeDataHolder
        BaseApp.genericTableHelper = mockGenericTableHelper
        Mockito.`when`(mockGenericTableHelper.getRelatedTableName("Table_3", "relation4"))
            .thenReturn("RELATED_TABLE")

        val sqlQueryBuilder = SqlQueryBuilderUtil(tableName = "Table_3", searchField = searchFields)
        val actualQueryResult = sqlQueryBuilder.sortQuery("abc").sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Table_3 AS T1 WHERE `LastName` LIKE '%abc%' OR EXISTS ( SELECT * FROM RELATED_TABLE as T2 WHERE T1.__relation4Key = T2.__KEY AND T2.field_x LIKE '%abc%' ) ").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSqlSortQueryWithCustomFormatNotFound() {
        val searchFields: JSONObject = JSONObject().apply {
            put("Table_3", listOf("LastName", "field_x"))
            put("Service", listOf("name", "employeeNumber"))
        }

        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)
        val customFormattersJsonObj = JSONObject(customFormattersJson)
        mockRuntimeDataHolder.customFormatters =
            FieldMapping.buildCustomFormatterBinding(customFormattersJsonObj)
        BaseApp.runtimeDataHolder = mockRuntimeDataHolder

        val sqlQueryBuilder = SqlQueryBuilderUtil(tableName = "Table_3", searchField = searchFields)
        val actualQueryResult = sqlQueryBuilder.sortQuery("abc").sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Table_3 AS T1 WHERE `LastName` LIKE '%abc%' OR `field_x` LIKE '%abc%' ").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSqlSortQueryWithCustomFormatFound() {
        val searchFields: JSONObject = JSONObject().apply {
            put("Table_3", listOf("LastName", "field_x"))
            put("Service", listOf("name", "employeeNumber"))
        }

        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)
        val customFormattersJsonObj = JSONObject(customFormattersJson)
        mockRuntimeDataHolder.customFormatters =
            FieldMapping.buildCustomFormatterBinding(customFormattersJsonObj)
        BaseApp.runtimeDataHolder = mockRuntimeDataHolder

        val sqlQueryBuilder = SqlQueryBuilderUtil(tableName = "Table_3", searchField = searchFields)
        val actualQueryResult = sqlQueryBuilder.sortQuery("UX").sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Table_3 AS T1 WHERE `LastName` LIKE '%UX%' OR `field_x` LIKE '%UX%' OR field_x == '0' ").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSqlSortQueryWithRelationWithCustomFormatFound() {
        val searchFields: JSONObject = JSONObject().apply {
            put("Table_4", listOf("LastName", "relationField.field_1"))
            put("Service", listOf("name", "employeeNumber"))
        }

        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)
        val mockGenericTableHelper = Mockito.mock(GenericTableHelper::class.java)
        val customFormattersJsonObj = JSONObject(customFormattersJson)
        mockRuntimeDataHolder.customFormatters =
            FieldMapping.buildCustomFormatterBinding(customFormattersJsonObj)
        BaseApp.runtimeDataHolder = mockRuntimeDataHolder
        BaseApp.genericTableHelper = mockGenericTableHelper
        Mockito.`when`(mockGenericTableHelper.getRelatedTableName("Table_4", "relationField"))
            .thenReturn("RELATED_TABLE")

        val sqlQueryBuilder = SqlQueryBuilderUtil(tableName = "Table_4", searchField = searchFields)
        val actualQueryResult = sqlQueryBuilder.sortQuery("UX").sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Table_4 AS T1 WHERE `LastName` LIKE '%UX%' OR EXISTS ( SELECT * FROM RELATED_TABLE as T2 WHERE T1.__relationFieldKey = T2.__KEY AND ( T2.field_1 LIKE '%UX%' OR T2.field_1 == '0' ) ) ").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSearchableField() {
        every { BaseApp.runtimeDataHolder } returns RuntimeDataHolder(
            initialGlobalStamp = 0,
            guestLogin = true,
            remoteUrl = "",
            searchField = searchFieldsJson,
            sdkVersion = "12.5",
            logLevel = Log.DEBUG,
            dumpedTables = "",
            queries = mutableMapOf(),
            tableProperties = mutableMapOf(),
            customFormatters = mapOf(),
            embeddedFiles = mutableListOf()
        )
        Assert.assertEquals(searchFieldsJson, BaseApp.runtimeDataHolder.searchField)
    }
}
