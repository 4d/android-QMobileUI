/*
 * Created by Quentin Marciset on 10/3/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.model

import androidx.sqlite.db.SimpleSQLiteQuery

data class QueryHolder(var query: SimpleSQLiteQuery?, var isSearchActive: Boolean = false)
