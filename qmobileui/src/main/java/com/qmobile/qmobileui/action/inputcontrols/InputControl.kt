/*
 * Created by qmarciset on 28/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.inputcontrols

import com.qmobile.qmobileapi.utils.getJSONObjectList
import com.qmobile.qmobileapi.utils.getSafeBoolean
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.getStringList
import com.qmobile.qmobileapi.utils.parseToString
import com.qmobile.qmobileapi.utils.parseToType
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.utils.FieldMapping
import com.qmobile.qmobiledatasync.utils.fieldAdjustment
import com.qmobile.qmobileui.action.sort.Sort
import com.qmobile.qmobileui.action.sort.Sort.getTypeConstraints
import com.qmobile.qmobileui.action.sort.Sort.sortMatchingKeywords
import com.qmobile.qmobileui.utils.ReflectionUtils
import com.qmobile.qmobileui.utils.ResourcesHelper
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.LinkedHashMap

object InputControl {

    const val INPUT_CONTROL_FORMAT_HOLDER_KEY = "__temporary_input_control_format_holder_"

    enum class Types(val format: String) {
        PUSH("/push"),
        SEGMENTED("/segmented"),
        POPOVER("/popover"),
        MENU("/sheet"),
        PICKER("/picker")
    }

    @Suppress("UNCHECKED_CAST")
    fun hasStaticData(choiceList: Any?): Boolean {
        return when (choiceList) {
            is List<*> -> true
            is Map<*, *> -> !isValidDataSource(choiceList)
            else -> false
        }
    }


    @Suppress("UNCHECKED_CAST")
    fun hasCurrentEntity(choiceList: Any?): Boolean {
        return when (choiceList) {
            is List<*> -> false
            is Map<*, *> -> isValidCurrentEntity(choiceList)
            else -> false
        }
    }

    fun getTypedValue(itemJsonObject: JSONObject, fieldValue: Any?): Any? {
        if (fieldValue == null) {
            return null
        }
        return when (itemJsonObject.getSafeString("type")) {
            "bool" -> getBooleanValue(fieldValue.toString())
            "number" -> fieldValue.toString().toIntOrNull()
            else -> fieldValue.toString()
        }
    }

    private fun getBooleanValue(fieldValue: String): Boolean {
        return when (fieldValue) {
            "0" -> false
            "1" -> true
            else -> fieldValue.toBoolean()
        }
    }

    fun FieldMapping.getImageName(text: String): String? {
        return ResourcesHelper.correctIconPath("${this.name}_$text")
    }

    private fun isValidDataSourceField(choiceList: Map<*, *>): Boolean {
        return ((choiceList["dataSource"] as? Map<*, *>)?.get("field") as? String)?.isNotEmpty() == true
    }
    private fun isValidDataSource(choiceList: Map<*, *>): Boolean {
        return ((choiceList["dataSource"] as? Map<*, *>)?.get("dataClass") as? String)?.isNotEmpty() == true &&
                isValidDataSourceField(choiceList)
    }

    private fun isValidCurrentEntity(choiceList: Map<*, *>): Boolean {
        return isValidDataSourceField(choiceList) &&
                (((choiceList["dataSource"] as? Map<*, *>)?.get("currentEntity") as? Boolean) == true)
    }

    @Suppress("UNCHECKED_CAST")
    fun getDataSource(choiceList: Any?): Map<String, Any> {
        return ((choiceList as? Map<*, *>)?.get("dataSource") as? Map<String, Any>) ?: mapOf()
    }

    fun getDataClass(dataSource: Map<String, Any>): String? {
        return dataSource["dataClass"] as? String
    }

    fun getField(dataSource: Map<String, Any>): String? {
        return dataSource["field"] as? String
    }

    fun getEntityFormat(dataSource: Map<String, Any>): String? {
        return dataSource["entityFormat"] as? String
    }

    @Suppress("UNCHECKED_CAST")
    fun getSearchFields(dataSource: Map<String, Any>): List<String> {
        val list = mutableListOf<String>()
        when (val search = dataSource["search"]) {
            is Boolean -> {
                (dataSource["field"] as? String)?.let { list.add(it) }
            }
            is String -> {
                if (search.isNotEmpty()) {
                    list.add(search)
                }
            }
            is JSONArray -> {
                (search as? JSONArray).getStringList().filter { it.isNotEmpty() }
                    .forEach { list.add(it) }
            }
        }
        return list
    }

    fun getSortFields(dataSource: Map<String, Any>, hasTextType: Boolean): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        val fieldType = if (hasTextType) {
            "string"
        } else {
            null
        }
        when (val order = dataSource["order"]) {
            is String -> { // ascending / descending to apply to "field"
                (dataSource["field"] as? String)?.let {
                    val key = getTypeConstraints(it.fieldAdjustment(), fieldType)
                    map[key] = sortMatchingKeywords(order)
                }
            }
        }
        map.getSort(dataSource, fieldType)
        (dataSource["field"] as? String)?.let { // default value
            val key = getTypeConstraints(it.fieldAdjustment(), fieldType)
            if (!map.contains(key)) {
                map[key] = sortMatchingKeywords(Sort.Order.ASCENDING.verbose)
            }
        }
        return map
    }

    private fun LinkedHashMap<String, String>.getSort(dataSource: Map<String, Any>, fieldType: String?) {
        when (val sort = dataSource["sort"]) {
            is String -> {
                val fields = sort.trim().split(",")
                fields.forEach {
                    val key = getTypeConstraints(it.fieldAdjustment(), fieldType)
                    this[key] = sortMatchingKeywords(Sort.Order.ASCENDING.verbose)
                }
            }
            is JSONArray -> { // list of fields with default ascending
                this.checkJSONArray(sort, fieldType)
            }
            is JSONObject -> { // map of <field, order>
                this.checkJSONObject(sort, fieldType)
            }
        }
    }

    private fun LinkedHashMap<String, String>.checkJSONArray(array: JSONArray, fieldType: String?) {
        if (array.length() > 0) {
            when (array.get(0)) {
                is JSONObject -> {
                    array.getJSONObjectList().forEach { json ->
                        this.checkJSONObject(json, fieldType)
                    }
                }
                is String -> {
                    array.getStringList().forEach {
                        val key = getTypeConstraints(it.fieldAdjustment(), fieldType)
                        this[key] = sortMatchingKeywords(Sort.Order.ASCENDING.verbose)
                    }
                }
            }
        }
    }

    private fun LinkedHashMap<String, String>.checkJSONObject(json: JSONObject, fieldType: String?) {
        json.getSafeString("field")?.let { field ->
            val key = getTypeConstraints(field.fieldAdjustment(), fieldType)
            this[key] = getOrder(json)
        }
    }

    private fun getOrder(jsonObject: JSONObject): String {
        var order: String = jsonObject.getSafeString("order")?.let { strOrder ->
            sortMatchingKeywords(strOrder)
        } ?: sortMatchingKeywords(Sort.Order.ASCENDING.verbose)

        jsonObject.getSafeBoolean("ascending")?.let { boolValue ->
            if (boolValue) {
                order = sortMatchingKeywords(Sort.Order.ASCENDING.verbose)
            }
        }
        jsonObject.getSafeBoolean("descending")?.let { boolValue ->
            if (boolValue) {
                order = sortMatchingKeywords(Sort.Order.DESCENDING.verbose)
            }
        }
        return order
    }

    fun applyEntityFormat(item: RoomEntity, entityFormat: String? = null): String {
        if (entityFormat.isNullOrEmpty()) {
            return ""
        }
        val regex = ("((?:%[\\w|\\s|\\.]+%)+)").toRegex()
        val newText = regex.replace(entityFormat) { matchResult ->
            val fieldName = matchResult.destructured.component1().removePrefix("%").removeSuffix("%")
            val fieldValue = ReflectionUtils.getInstancePropertyForInputControl(item, fieldName.fieldAdjustment())
            fieldValue?.toString() ?: ""
        }
        return newText
    }

    object Format {

        fun ActionTask.saveInputControlFormatHolders(
            inputControlFormatHolders: MutableMap<Int, InputControlFormatHolder>
        ) {
            for ((position, inputControlFormatHolder) in inputControlFormatHolders) {
                this.actionContent?.apply {
                    this[INPUT_CONTROL_FORMAT_HOLDER_KEY + "_$position"] =
                        BaseApp.mapper.parseToString(inputControlFormatHolder)
                }
            }
        }

        fun ActionTask.getInputControlFormatHolders(): Map<Int, InputControlFormatHolder> {
            val map = mutableMapOf<Int, InputControlFormatHolder>()
            this.actionContent?.let { actionContent ->
                for (i in 0 until this.getNbParameters()) {
                    getSingleInputControlFormatHolder(actionContent, i)?.let { holder ->
                        map[i] = holder
                    }
                }
            }
            return map
        }

        private fun getSingleInputControlFormatHolder(
            actionContent: MutableMap<String, Any>,
            i: Int
        ): InputControlFormatHolder? {
            actionContent[INPUT_CONTROL_FORMAT_HOLDER_KEY + "_$i"]?.let {
                return BaseApp.mapper.parseToType(it.toString())
            }
            return null
        }
    }
}
