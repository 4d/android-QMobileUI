/*
 * Created by Quentin Marciset on 26/5/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeBoolean
import com.qmobile.qmobileapi.utils.getSafeInt
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.getStringList
import com.qmobile.qmobileapi.utils.toStringMap
import org.json.JSONObject

data class FieldMapping(
    val binding: String?,
    val choiceList: Any?, // choiceList can be a JSONObject or a JSONArray
    val isSearchable: Boolean?,
    val name: String?,
    val imageWidth: Int?,
    val imageHeight: Int?
)

@Suppress("NestedBlockDepth")
fun buildCustomFormatterBinding(customFormatters: JSONObject?): Map<String, Map<String, FieldMapping>> {
    val tableMap: MutableMap<String, Map<String, FieldMapping>> = mutableMapOf()

    customFormatters?.names()?.let { tableKeysIndex ->
        for (i in 0 until tableKeysIndex.length()) {
            val tableKey = tableKeysIndex.getString(i)
            customFormatters.getSafeObject(tableKey)?.let { fieldsJsonObject ->

                val fieldMap: MutableMap<String, FieldMapping> = mutableMapOf()

                fieldsJsonObject.names()?.let { fieldKeysIndex ->
                    for (j in 0 until fieldKeysIndex.length()) {
                        val fieldKey = fieldKeysIndex.getString(j)
                        fieldsJsonObject.getSafeObject(fieldKey)?.let { fieldMappingJsonObject ->
                            fieldMap[fieldKey] = getFieldMapping(fieldMappingJsonObject)
                        }
                    }
                }
                tableMap[tableKey] = fieldMap
            }
        }
    }

    return tableMap
}

fun getFieldMapping(fieldMappingJsonObject: JSONObject): FieldMapping = FieldMapping(
    binding = fieldMappingJsonObject.getSafeString("binding"),
    choiceList = fieldMappingJsonObject.getSafeObject("choiceList")
        ?.toStringMap()
        ?: fieldMappingJsonObject.getSafeArray("choiceList")
            .getStringList(), // choiceList can be a JSONObject or a JSONArray
    isSearchable = fieldMappingJsonObject.getSafeBoolean("isSearchable"),
    name = fieldMappingJsonObject.getSafeString("name"),
    imageWidth = fieldMappingJsonObject.getSafeInt("imageWidth"),
    imageHeight = fieldMappingJsonObject.getSafeInt("imageHeight")
)

fun getChoiceListString(fieldMapping: FieldMapping, text: String): String? {
    return when (fieldMapping.choiceList) {
        is Map<*, *> -> {
            fieldMapping.choiceList[text] as? String?
        }
        is List<*> -> {
            text.toIntOrNull()?.let { index ->
                if (index < fieldMapping.choiceList.size)
                    fieldMapping.choiceList[index] as? String?
                else
                    null
            }
        }
        else -> null
    }
}
