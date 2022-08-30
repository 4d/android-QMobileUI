/*
 * Created by qmarciset on 29/10/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.formatters

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.qmobile.qmobileapi.utils.parseToString
import com.qmobile.qmobiledatasync.app.BaseApp
import org.json.JSONArray

object JsonYamlFormat {

    private val prettyPrinter =
        DefaultPrettyPrinter().apply { indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE) }

    private val yamlMapper = ObjectMapper(
        YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR)
    )

    fun applyFormat(format: String, baseText: Any): String {
        return when (format) {
            "yaml" -> {
                if (baseText.toString().isEmpty()) {
                    ""
                } else {
                    yamlMapper.writeValueAsString(baseText)
                }
            }
            "jsonPrettyPrinted" -> {
                BaseApp.mapper.enable(SerializationFeature.INDENT_OUTPUT)
                    .setDefaultPrettyPrinter(prettyPrinter)
                    .parseToString(baseText)
            }
            "json" -> {
                BaseApp.mapper.disable(SerializationFeature.INDENT_OUTPUT).parseToString(baseText)
            }
            "jsonValues" -> {
                (baseText as? Map<*, *>)?.values?.let {
                    JSONArray(it).toString()
                } ?: ""
            }
            else -> {
                baseText.toString()
            }
        }
    }
}
