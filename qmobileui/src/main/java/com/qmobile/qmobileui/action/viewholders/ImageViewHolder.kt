/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileui.utils.createTempImageFile
import timber.log.Timber
import java.io.File
import java.io.IOException

class ImageViewHolder(itemView: View, hideKeyboardCallback: () -> Unit) :
    BaseViewHolder(itemView, hideKeyboardCallback) {

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntityJsonObject, onValueChanged)
        imageButton.setOnClickListener {
            // setup the alert builder
            val builder = MaterialAlertDialogBuilder(itemView.context)
            builder.setTitle("Choose a picture")
            val options = arrayOf("Camera", "Gallery")
            builder.setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        capturePhoto()
                    }
                    1 -> {
                        pickImageFromGallery()
                    }
                }
            }
            // create and show the alert dialog
            val dialog = builder.create()
            dialog.show()
        }
        displaySelectedImageIfNeed()
    }

    override fun validate(displayError: Boolean): Boolean {
        return true
    }

    override fun getInputType(format: String): Int {
        return -1
    }

    private fun pickImageFromGallery() {
        val context = itemView.context
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        (context as Activity).startActivityForResult(
            intent,
            bindingAdapterPosition // Send position as request code, so we can update image preview only for the selected item
        )
    }

    private fun capturePhoto() {
        val context = itemView.context

        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(context.packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createTempImageFile(itemView.context)
                } catch (ex: IOException) {
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        context,
                        "com.4D.android.fileprovider",
                        it
                    )
                    (context as Activity).startActivityForResult(
                        takePictureIntent,
                        bindingAdapterPosition // Send position as request code, so we can update image preview only for the selected item
                    )
                }
            }
        }
    }

    private fun displaySelectedImageIfNeed() {
        try {
            if (itemJsonObject.get("bitmap") != null) {
                imageButton.setImageBitmap(itemJsonObject.get("bitmap") as Bitmap)
                itemJsonObject.remove("bitmap")
            }
        } catch (e: Exception) {
            Timber.e("ActionParameterViewHolder: ",e.localizedMessage)
        }

        try {
            if (itemJsonObject.get("uri") != null) {
                val uri = itemJsonObject.get("uri") as Uri
                imageButton.setImageURI(uri)
                itemJsonObject.remove("uri")
            }
        } catch (e: Exception) {
            Timber.e("ActionParameterViewHolder: ",e.localizedMessage)
        }
    }
}
