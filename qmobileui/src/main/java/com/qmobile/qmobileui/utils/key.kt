/*
 * Created by Quentin Marciset on 23/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

@file:JvmName("Key")

package com.qmobile.qmobileui.utils

enum class TypeChoice(var key: String) {
    EuroCurrency("currencyEuro"),
    JapanCurrency("currencyYen"),
    USCurrency("currencyDollar"),
    Percentage("percent"),
    Ordinal("ordinal"),
    SpellOut("spellOut"),
    Number("integer"),
    Real("real"),
    Decimal("decimal"),
    YesNo("localizedText,noOrYes"),
    TrueFalse("localizedText,falseOrTrue"),
    ShortTime("shortTime"),
    Time("mediumTime"),
    Duration("duration"),
    FullDate("fullDate"),
    LongDate("longDate"),
    MediumDate("mediumDate"),
    ShortDate("shortDate")
}
