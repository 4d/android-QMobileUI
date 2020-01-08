package com.qmarciset.androidmobileui.utils

import androidx.databinding.ViewDataBinding
import com.qmarciset.androidmobileui.viewmodel.EntityListViewModel
import com.qmarciset.androidmobileui.viewmodel.EntityViewModel

interface ViewDataBindingInterface {

    fun setEntityListViewModel(
        viewDataBinding: ViewDataBinding,
        entityListViewModel: EntityListViewModel<*>
    )

    fun setEntityViewModel(viewDataBinding: ViewDataBinding, entityViewModel: EntityViewModel<*>)
}
