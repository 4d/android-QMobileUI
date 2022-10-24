/*
 * Created by qmarciset on 28/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.inputcontrols

import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.parseToString
import com.qmobile.qmobileapi.utils.parseToType
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.utils.FieldMapping
import com.qmobile.qmobiledatasync.utils.fieldAdjustment
import com.qmobile.qmobileui.action.sort.Sort
import com.qmobile.qmobileui.action.sort.Sort.sortMatchingKeywords
import com.qmobile.qmobileui.utils.ReflectionUtils
import com.qmobile.qmobileui.utils.ResourcesHelper
import org.json.JSONObject
import kotlin.collections.LinkedHashMap

object InputControl {

    private const val INPUT_CONTROL_FORMAT_HOLDER_KEY = "__temporary_input_control_format_holder_"

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

    private fun isValidDataSource(choiceList: Map<*, *>): Boolean {
        return ((choiceList["dataSource"] as? Map<*, *>)?.get("dataClass") as? String)?.isNotEmpty() == true &&
            ((choiceList["dataSource"] as? Map<*, *>)?.get("field") as? String)?.isNotEmpty() == true
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
            is List<*> -> {
                (search as? List<String>)?.filter { it.isNotEmpty() }?.forEach { list.add(it) }
            }
        }
        return list
    }

    fun getSortFields(dataSource: Map<String, Any>): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        (dataSource["field"] as? String)?.let { // default value
            map[it] = sortMatchingKeywords(Sort.Order.ASCENDING.verbose)
        }
        when (val order = dataSource["order"]) {
            is String -> { // ascending / descending to apply to "field"
                (dataSource["field"] as? String)?.let {
                    map[it] = sortMatchingKeywords(order)
                }
            }
        }
        when (val sort = dataSource["sort"]) {
            is List<*> -> { // list of fields with default ascending
                sort.forEach {
                    if (it is String) {
                        map[it] = sortMatchingKeywords(Sort.Order.ASCENDING.verbose)
                    }
                }
            }
            is Map<*, *> -> { // map of <field, order>
                for ((key, value) in sort) {
                    if (key is String && value is String) {
                        map[key] = sortMatchingKeywords(value)
                    }
                }
            }
        }
        return map
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

        fun ActionTask.cleanActionContent(): MutableMap<String, Any> {
            val newActionContent = this.actionContent?.toMutableMap() ?: return mutableMapOf()
            for (i in 0 until this.getNbParameters()) {
                newActionContent.remove(INPUT_CONTROL_FORMAT_HOLDER_KEY + "_$i")
            }
            return newActionContent
        }
    }
}
