/*
 * Created by qmarciset on 11/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders

import android.Manifest
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.utils.PermissionChecker

class ImageViewHolder(
    itemView: View,
    format: String,
    private val actionTypesCallback: (actionTypes: Action.Type, position: Int) -> Unit
) : BaseImageViewHolder(itemView, format) {

    override fun getPlaceholderRes(): Int = R.drawable.add_photo_alternate

    override fun onImageClick() {
        MaterialAlertDialogBuilder(itemView.context)
            .setTitle(itemView.context.getString(R.string.action_parameter_image_dialog_title))
            .setItems(
                arrayOf(
                    itemView.context.getString(R.string.action_parameter_image_dialog_photo),
                    itemView.context.getString(R.string.action_parameter_image_dialog_gallery)
                )
            ) { _, which ->
                when (which) {
                    0 -> askCameraPermissionAndProceed()
                    1 -> actionTypesCallback(Action.Type.PICK_PHOTO_GALLERY, bindingAdapterPosition)
                }
            }
            .show()
    }

    private fun askCameraPermissionAndProceed() {
        (itemView.context as PermissionChecker?)?.askPermission(
            context = itemView.context,
            permission = Manifest.permission.CAMERA,
            rationale = itemView.context.getString(R.string.permission_rationale_camera)
        ) { isGranted ->
            if (isGranted) {
                actionTypesCallback(Action.Type.TAKE_PICTURE_CAMERA, bindingAdapterPosition)
            }
        }
    }
}
