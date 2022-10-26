/*
 * Created by Maturi Karthik on 2/4/2021.
 * 4D SAS
 * Copyright (c) 2021 Maturi Karthik. All rights reserved.
 */

package com.qmobile.qmobileui

import androidx.sqlite.db.SimpleSQLiteQuery
import com.qmobile.qmobileapi.model.entity.TableInfo
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.utils.FieldMapping
import com.qmobile.qmobiledatasync.utils.GenericTableHelper
import com.qmobile.qmobiledatasync.utils.RuntimeDataHolder
import com.qmobile.qmobiledatasync.utils.TableInfoHelper.buildTableInfo
import com.qmobile.qmobileui.utils.FormQueryBuilder
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

    private val searchFieldsEmployee = listOf("LastName", "FirstName")
    private val searchFieldsService = listOf("name", "employeeNumber")

    @Before
    fun setup() {
        mockkObject(BaseApp)
    }

    @Test
    fun testSqlGetAllQuery() {
        val mockGenericTableHelper = Mockito.mock(GenericTableHelper::class.java)
        BaseApp.genericTableHelper = mockGenericTableHelper
        val formQueryBuilder =
            FormQueryBuilder(tableName = "Service", searchFields = searchFieldsService, customSortFields = linkedMapOf())
        val actualQueryResult = formQueryBuilder.getQuery().sql
        val expectedQueryResult = SimpleSQLiteQuery("SELECT * FROM Service").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSqlSortQuery() {
        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)
        mockRuntimeDataHolder.customFormatters = mapOf()
        val mockGenericTableHelper = Mockito.mock(GenericTableHelper::class.java)
        BaseApp.genericTableHelper = mockGenericTableHelper

        val formQueryBuilder =
            FormQueryBuilder(tableName = "Service", searchFields = searchFieldsService, customSortFields = linkedMapOf())
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
            FormQueryBuilder(tableName = "Employee", searchFields = searchFieldsEmployee, customSortFields = linkedMapOf("name COLLATE NOCASE" to "DESC", "id" to "DESC", "age" to "ASC"))
        val actualQueryResult =
            formQueryBuilder.getQuery("abc").sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Employee AS T1 WHERE T1.lastName LIKE '%abc%' OR T1.firstName LIKE '%abc%' ORDER BY name COLLATE NOCASE DESC, id DESC, age ASC").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSqlSortActionQueryWithOneField() {
        val mockGenericTableHelper = Mockito.mock(GenericTableHelper::class.java)
        BaseApp.genericTableHelper = mockGenericTableHelper
        val formQueryBuilder =
            FormQueryBuilder(tableName = "Service", searchFields = searchFieldsService, customSortFields = linkedMapOf("name COLLATE NOCASE" to "DESC"))
        val actualQueryResult = formQueryBuilder.getQuery("abc").sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Service AS T1 WHERE T1.name LIKE '%abc%' OR T1.employeeNumber LIKE '%abc%' ORDER BY name COLLATE NOCASE DESC").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSqlSortQueryWithRelation() {
        val searchFields = listOf("LastName", "relation4.field_x")
        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)

        val mockGenericTableHelper = Mockito.mock(GenericTableHelper::class.java)
        BaseApp.genericTableHelper = mockGenericTableHelper

        mockRuntimeDataHolder.customFormatters = mapOf()
        mockRuntimeDataHolder.relations = listOf(
            Relation("Table_3", "RELATED_TABLE", "relation4", "inverse", Relation.Type.MANY_TO_ONE)
        )
        BaseApp.runtimeDataHolder = mockRuntimeDataHolder

        val formQueryBuilder = FormQueryBuilder(tableName = "Table_3", searchFields = searchFields, customSortFields = linkedMapOf())
        val actualQueryResult = formQueryBuilder.getQuery("abc").sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Table_3 AS T1 WHERE T1.lastName LIKE '%abc%' OR EXISTS ( SELECT * FROM RELATED_TABLE AS T2 WHERE T2.__KEY = T1.__relation4Key AND T2.field_x LIKE '%abc%' )").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSqlSortQueryWithCustomFormatNotFound() {
        val searchFields = listOf("LastName", "field_x")
        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)
        val mockGenericTableHelper = Mockito.mock(GenericTableHelper::class.java)
        BaseApp.genericTableHelper = mockGenericTableHelper

        val customFormattersJsonObj = JSONObject(customFormattersJson)
        mockRuntimeDataHolder.customFormatters =
            FieldMapping.buildCustomFormatterBinding(customFormattersJsonObj)
        BaseApp.runtimeDataHolder = mockRuntimeDataHolder

        val formQueryBuilder = FormQueryBuilder(tableName = "Table_3", searchFields = searchFields, customSortFields = linkedMapOf())
        val actualQueryResult = formQueryBuilder.getQuery("abc").sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Table_3 AS T1 WHERE T1.lastName LIKE '%abc%' OR T1.field_x LIKE '%abc%'").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSqlSortQueryWithCustomFormatFound() {
        val searchFields = listOf("LastName", "field_x")
        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)
        val mockGenericTableHelper = Mockito.mock(GenericTableHelper::class.java)
        BaseApp.genericTableHelper = mockGenericTableHelper

        val customFormattersJsonObj = JSONObject(customFormattersJson)
        mockRuntimeDataHolder.customFormatters =
            FieldMapping.buildCustomFormatterBinding(customFormattersJsonObj)
        BaseApp.runtimeDataHolder = mockRuntimeDataHolder

        val formQueryBuilder = FormQueryBuilder(tableName = "Table_3", searchFields = searchFields, customSortFields = linkedMapOf())
        val actualQueryResult = formQueryBuilder.getQuery("UX").sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Table_3 AS T1 WHERE T1.lastName LIKE '%UX%' OR T1.field_x LIKE '%UX%' OR T1.field_x == '0'").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }

    @Test
    fun testSqlSortQueryWithRelationWithCustomFormatFound() {
        val searchFields = listOf("LastName", "relationField.field_1")
        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)
        val mockGenericTableHelper = Mockito.mock(GenericTableHelper::class.java)
        BaseApp.genericTableHelper = mockGenericTableHelper
        val customFormattersJsonObj = JSONObject(customFormattersJson)
        mockRuntimeDataHolder.customFormatters =
            FieldMapping.buildCustomFormatterBinding(customFormattersJsonObj)
        mockRuntimeDataHolder.relations = listOf(
            Relation("Table_4", "RELATED_TABLE", "relationField", "inverse", Relation.Type.MANY_TO_ONE)
        )
        BaseApp.runtimeDataHolder = mockRuntimeDataHolder

        val formQueryBuilder = FormQueryBuilder(tableName = "Table_4", searchFields = searchFields, customSortFields = linkedMapOf())
        val actualQueryResult = formQueryBuilder.getQuery("UX").sql
        val expectedQueryResult =
            SimpleSQLiteQuery("SELECT * FROM Table_4 AS T1 WHERE T1.lastName LIKE '%UX%' OR EXISTS ( SELECT * FROM RELATED_TABLE AS T2 WHERE T2.__KEY = T1.__relationFieldKey AND ( T2.field_1 LIKE '%UX%' OR T2.field_1 == '0' ) )").sql
        Assert.assertEquals(expectedQueryResult, actualQueryResult)
    }
}
