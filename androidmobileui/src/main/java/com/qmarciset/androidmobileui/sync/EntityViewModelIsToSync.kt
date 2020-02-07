/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.sync

import com.qmarciset.androidmobileui.viewmodel.EntityListViewModel

data class EntityViewModelIsToSync(val vm: EntityListViewModel<*>, var isToSync: Boolean)
