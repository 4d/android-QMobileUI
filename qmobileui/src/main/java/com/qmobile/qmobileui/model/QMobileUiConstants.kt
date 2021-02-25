/*
 * Created by Quentin Marciset on 22/1/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.model

object QMobileUiConstants { // Compiled at stack-level
    // standard const
    const val EMBEDDED_PICTURES_PARENT = "Assets.xcassets"
    const val EMBEDDED_PICTURES = "Pictures"
    const val JSON_EXT = ".json"
    const val LAYOUT = "layout"
    const val INT_100: Int = 100
    const val INT_20: Int = 20
    const val INT_10: Int = 10
    const val INT_3: Int = 3
    const val INT_6: Int = 6
    const val INT_9: Int = 9
    const val INT_12: Int = 12
    const val INT_3600 = 3600
    const val INT_60: Int = 60
    const val INT_2: Int = 2
    const val INT_0: Int = 0

    object Language {
        const val LANGUAGE_ID = Device.ID
        const val LANGUAGE_CODE = "code"
        const val LANGUAGE_REGION = "region"
        const val LAYOUT = "layout"
    }

    object Prefix {
        const val RECYCLER_PREFIX = "recyclerview_item_"
        const val FRAGMENT_DETAIL_PREFIX = "fragment_detail_"
    }

    object Device {
        const val ID = "id"
        const val SIMULATOR = "simulator"
        const val DESCRIPTION = "description"
        const val VERSION = "version"
        const val OS = "os"
    }
}
