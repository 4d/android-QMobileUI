/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail

import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.parseToString
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.viewmodel.EntityViewModel
import com.qmobile.qmobileui.BR
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.BaseObserver
import com.qmobile.qmobileui.activity.mainactivity.MainActivity
import com.qmobile.qmobileui.ui.SnackbarHelper
import timber.log.Timber

class EntityDetailFragmentObserver(
    private val fragment: EntityDetailFragment,
    private val entityViewModel: EntityViewModel<EntityModel>
) : BaseObserver {

    override fun initObservers() {
        observeDoesEntityExist()
        fragment.delegate.observeEntityToastMessage(entityViewModel.toastMessage.message)
    }

    private fun observeDoesEntityExist() {
        entityViewModel.doesEntityExist.observe(
            fragment.viewLifecycleOwner
        ) { doesEntityExist ->
            Timber.d("Observed does entity exist : $doesEntityExist")
            if (doesEntityExist) {
                observeEntity()
            } else {
                SnackbarHelper.show(fragment.activity, fragment.getString(R.string.record_not_found))
                (fragment.activity as? MainActivity?)?.navController?.navigateUp()
            }
        }
    }

    // Observe entity
    private fun observeEntity() {
        entityViewModel.entity.observe(
            fragment.viewLifecycleOwner
        ) { entity ->
            Timber.d("Observed entity from Room, json = ${BaseApp.mapper.parseToString(entity)}")
            entity?.let {
                fragment.binding.setVariable(BR.entityData, entity)
                fragment.binding.executePendingBindings()
                RelationHelper.setupRelationNavigation(fragment.tableName, fragment.binding, entity)
            }
        }
    }
}
