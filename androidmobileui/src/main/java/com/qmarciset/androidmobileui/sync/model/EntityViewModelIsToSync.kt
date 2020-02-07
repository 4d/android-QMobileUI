package com.qmarciset.androidmobileui.sync.model

import com.qmarciset.androidmobileui.viewmodel.EntityListViewModel

data class EntityViewModelIsToSync(val vm: EntityListViewModel<*>, var isToSync: Boolean)
