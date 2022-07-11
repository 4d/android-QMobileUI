/*
 * Created by Maturi Karthik on 2/4/2021.
 * 4D SAS
 * Copyright (c) 2021 Maturi Karthik. All rights reserved.
 */

package com.qmobile.qmobileui

import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.utils.FieldMapping
import com.qmobile.qmobiledatasync.utils.RuntimeDataHolder
import com.qmobile.qmobileui.utils.FormQueryBuilder
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
        val formQueryBuilder =
            FormQueryBuilder(tableName = "Service", searchField = searchFieldsJson)
        val actualQueryResult = formQueryBuilder.getQuery().sql
        val expectedQueryResult = SimpleSQLiteQuery("SELECT * FROM Service").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSqlSortQuery() {
        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)
        mockRuntimeDataHolder.customFormatters = mapOf()
        BaseApp.runtimeDataHolder = mockRuntimeDataHolder

        val formQueryBuilder =
            FormQueryBuilder(tableName = "Service", searchField = searchFieldsJson)
        val actualQueryResult = formQueryBuilder.getQuery("abc").sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Service AS T1 WHERE T1.name LIKE '%abc%' OR T1.employeeNumber LIKE '%abc%'").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSqlSortActionQueryWithMultipleFields() {
        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)
        mockRuntimeDataHolder.customFormatters = mapOf()
        BaseApp.runtimeDataHolder = mockRuntimeDataHolder

        val formQueryBuilder =
            FormQueryBuilder(tableName = "Employee", searchField = searchFieldsJson)
        val actualQueryResult =
            formQueryBuilder.getQuery("abc", sortFields = linkedMapOf( "name" to "DESC", "id" to "DESC", "age" to "ASC")).sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Employee AS T1 WHERE T1.LastName LIKE '%abc%' OR T1.FirstName LIKE '%abc%' order by name DESC, id DESC, age ASC").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSqlSortActionQueryWithOneField() {
        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)
        mockRuntimeDataHolder.customFormatters = mapOf()
        BaseApp.runtimeDataHolder = mockRuntimeDataHolder
        val formQueryBuilder =
            FormQueryBuilder(tableName = "Service", searchField = searchFieldsJson)
        val actualQueryResult = formQueryBuilder.getQuery("abc", hashMapOf("name" to "DESC")).sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Service AS T1 WHERE T1.name LIKE '%abc%' OR T1.employeeNumber LIKE '%abc%' order by name DESC").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSqlSortQueryWithRelation() {
        val searchFields: JSONObject = JSONObject().apply {
            put("Table_3", listOf("LastName", "relation4.field_x"))
            put("Service", listOf("name", "employeeNumber"))
        }

        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)
        mockRuntimeDataHolder.customFormatters = mapOf()
        mockRuntimeDataHolder.relations = listOf(
            Relation("Table_3", "RELATED_TABLE", "relation4", "inverse", Relation.Type.MANY_TO_ONE)
        )
        BaseApp.runtimeDataHolder = mockRuntimeDataHolder

        val formQueryBuilder = FormQueryBuilder(tableName = "Table_3", searchField = searchFields)
        val actualQueryResult = formQueryBuilder.getQuery("abc").sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Table_3 AS T1 WHERE T1.LastName LIKE '%abc%' OR EXISTS ( SELECT * FROM RELATED_TABLE AS T2 WHERE T2.__KEY = T1.__relation4Key AND T2.field_x LIKE '%abc%' )").sql
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

        val formQueryBuilder = FormQueryBuilder(tableName = "Table_3", searchField = searchFields)
        val actualQueryResult = formQueryBuilder.getQuery("abc").sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Table_3 AS T1 WHERE T1.LastName LIKE '%abc%' OR T1.field_x LIKE '%abc%'").sql
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

        val formQueryBuilder = FormQueryBuilder(tableName = "Table_3", searchField = searchFields)
        val actualQueryResult = formQueryBuilder.getQuery("UX").sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Table_3 AS T1 WHERE T1.LastName LIKE '%UX%' OR T1.field_x LIKE '%UX%' OR T1.field_x == '0'").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSqlSortQueryWithRelationWithCustomFormatFound() {
        val searchFields: JSONObject = JSONObject().apply {
            put("Table_4", listOf("LastName", "relationField.field_1"))
            put("Service", listOf("name", "employeeNumber"))
        }

        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)
        val customFormattersJsonObj = JSONObject(customFormattersJson)
        mockRuntimeDataHolder.customFormatters =
            FieldMapping.buildCustomFormatterBinding(customFormattersJsonObj)
        mockRuntimeDataHolder.relations = listOf(
            Relation("Table_4", "RELATED_TABLE", "relationField", "inverse", Relation.Type.MANY_TO_ONE)
        )
        BaseApp.runtimeDataHolder = mockRuntimeDataHolder

        val formQueryBuilder = FormQueryBuilder(tableName = "Table_4", searchField = searchFields)
        val actualQueryResult = formQueryBuilder.getQuery("UX").sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Table_4 AS T1 WHERE T1.LastName LIKE '%UX%' OR EXISTS ( SELECT * FROM RELATED_TABLE AS T2 WHERE T2.__KEY = T1.__relationFieldKey AND ( T2.field_1 LIKE '%UX%' OR T2.field_1 == '0' ) )").sql
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
            dumpedTables = listOf(),
            relations = listOf(),
            tableInfo = mapOf(),
            customFormatters = mapOf(),
            embeddedFiles = mutableListOf(),
            tableActions = JSONObject(),
            currentRecordActions = JSONObject()
        )
        Assert.assertEquals(searchFieldsJson, BaseApp.runtimeDataHolder.searchField)
    }
}
