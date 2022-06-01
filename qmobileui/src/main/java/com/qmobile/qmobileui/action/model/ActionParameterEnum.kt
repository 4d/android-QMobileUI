package com.qmobile.qmobileui.action.model

enum class ActionParameterEnum(val type: String, val format: String = "default") {
    // Text
    TEXT_DEFAULT("string", "default"),
    TEXT_EMAIL("string", "email"),
    TEXT_PASSWORD("string", "password"),
    TEXT_URL("string", "url"),
    TEXT_ZIP("string", "zipCode"),
    TEXT_PHONE("string", "phone"),
    TEXT_ACCOUNT("string", "account"),
    TEXT_AREA("string", "textArea"),

    // Boolean
    BOOLEAN_DEFAULT("bool", "default"),
    BOOLEAN_CHECK("bool", "check"),

    // Number
    NUMBER_DEFAULT1("number", "default"),
    NUMBER_DEFAULT2("number", "number"),
    NUMBER_SCIENTIFIC("number", "scientific"),
    NUMBER_PERCENTAGE("number", "percent"),
    NUMBER_INTEGER("number", "integer"),
    NUMBER_SPELL_OUT("number", "spellOut"),

    // Date
    DATE_DEFAULT1("date", "default"),
    DATE_DEFAULT2("date", "date"),
    DATE_SHORT("date", "shortDate"),
    DATE_LONG("date", "longDate"),
    DATE_FULL("date", "fullDate"),

    // Time
    TIME_DEFAULT("time", "default"),
    TIME_DURATION("time", "duration"),

    // Image
    IMAGE("image"),
    SIGNATURE("image", "signature"),

    // Barcode
    BARCODE("string", "barcode")
}
