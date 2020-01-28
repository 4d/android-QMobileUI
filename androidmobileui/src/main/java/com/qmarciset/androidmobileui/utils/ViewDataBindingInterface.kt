package com.qmarciset.androidmobileui.utils

import androidx.databinding.ViewDataBinding
import com.qmarciset.androidmobileui.viewmodel.EntityListViewModel
import com.qmarciset.androidmobileui.viewmodel.EntityViewModel

// Interface implemented by MainActivity to provide different elements depending of the generated type
interface ViewDataBindingInterface {

    // Sets the appropriate EntityListViewModel
    fun setEntityListViewModel(
        viewDataBinding: ViewDataBinding,
        entityListViewModel: EntityListViewModel<*>
    )

    // Sets the appropriate EntityViewModel
    fun setEntityViewModel(viewDataBinding: ViewDataBinding, entityViewModel: EntityViewModel<*>)
}
