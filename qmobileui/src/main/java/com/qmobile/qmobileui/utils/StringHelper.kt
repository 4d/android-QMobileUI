/*
 * Created by Quentin Marciset on 27/5/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import java.text.Normalizer
import java.util.Locale

fun String.containsIgnoreCase(str: String): Boolean =
    this.toLowerCase(Locale.getDefault()).contains(str.toLowerCase(Locale.getDefault()))

fun String.tableNameAdjustment() =
    this.condense().capitalize(Locale.getDefault()).replaceSpecialChars().firstCharForTable()
        .validateWord()

fun String.fieldAdjustment() =
    this.condense().replaceSpecialChars().lowerCustomProperties().validateWordDecapitalized()

fun String.dataBindingAdjustment(): String =
    this.condense().replaceSpecialChars().firstCharForTable()
        .split("_")
        .joinToString("") { it.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault()) }

private fun String.condense() = this.replace("\\s".toRegex(), "")

private fun String.replaceSpecialChars(): String {
    return if (this.contains("Entities<")) {
        this.unaccent().replace("[^a-zA-Z0-9._<>]".toRegex(), "_")
    } else {
        this.unaccent().replace("[^a-zA-Z0-9._]".toRegex(), "_")
    }
}

private fun String.lowerCustomProperties() =
    if (this == "__KEY" || this == "__STAMP" || this == "__GlobalStamp" || this == "__TIMESTAMP")
        this
    else if (this.startsWith("__") && this.endsWith("Key"))
        this.removeSuffix("Key").toLowerCase(Locale.getDefault()) + "Key"
    else
        this.toLowerCase(Locale.getDefault())

private fun String.decapitalizeExceptID() =
    if (this == "ID") this.toLowerCase(Locale.getDefault()) else this.decapitalize(Locale.getDefault())

private fun String.firstCharForTable(): String =
    if (this.startsWith("_"))
        "Q$this"
    else
        this

private val REGEX_UNACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()

private fun CharSequence.unaccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return REGEX_UNACCENT.replace(temp, "")
}

private const val prefixReservedKeywords = "qmobile"

fun String.validateWord(): String {
    return this.split(".").joinToString(".") {
        if (reservedKeywords.contains(it)) "${prefixReservedKeywords}_$it" else it
    }
}

fun String.validateWordDecapitalized(): String {
    return this.decapitalizeExceptID().split(".").joinToString(".") {
        if (reservedKeywords.contains(it)) "${prefixReservedKeywords}_$it" else it
    }
}

val reservedKeywords = listOf(
    "as",
    "break",
    "class",
    "continue",
    "do",
    "else",
    "false",
    "for",
    "fun",
    "if",
    "in",
    "is",
    "null",
    "object",
    "package",
    "return",
    "super",
    "this",
    "throw",
    "true",
    "try",
    "typealias",
    "typeof",
    "val",
    "var",
    "when",
    "while",
    "by",
    "catch",
    "constructor",
    "delegate",
    "dynamic",
    "field",
    "file",
    "finally",
    "get",
    "import",
    "init",
    "param",
    "property",
    "receiver",
    "set",
    "setparam",
    "where",
    "actual",
    "abstract",
    "annotation",
    "companion",
    "const",
    "crossinline",
    "data",
    "enum",
    "expect",
    "external",
    "final",
    "infix",
    "inline",
    "inner",
    "internal",
    "lateinit",
    "noinline",
    "open",
    "operator",
    "out",
    "override",
    "private",
    "protected",
    "public",
    "reified",
    "sealed",
    "suspend",
    "tailrec",
    "vararg",
    "field",
    "it"
)
