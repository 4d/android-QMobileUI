/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.model.entity.Photo
import com.qmobile.qmobileapi.utils.getSafeAny
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.binding.ImageHelper
import com.qmobile.qmobileui.binding.Transformations
import com.qmobile.qmobileui.binding.getColorFromAttr
import com.qmobile.qmobileui.utils.ToastHelper
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageViewHolder(
    itemView: View,
    hideKeyboardCallback: () -> Unit,
    private val startActivityCallback: (intent: Intent, position: Int, photoFilePath: String?) -> Unit
) :
    BaseViewHolder(itemView, hideKeyboardCallback) {

    private val label: TextView = itemView.findViewById(R.id.label)
    private val error: TextView = itemView.findViewById(R.id.error)
    private var imageView: ImageView = itemView.findViewById(R.id.image_view)
    private var removeItem: ImageView = itemView.findViewById(R.id.item_remove)

    private var currentUri: String? = null

    companion object {
        const val DEFAULT_PLACEHOLDER_ICON_ALPHA = 0.6F
    }

    override fun bind(
        item: Any,
        currentEntity: EntityModel?,
        preset: String?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, preset, onValueChanged)

        itemJsonObject.getSafeString("label")?.let { parameterLabel ->
            label.text = if (isMandatory()) {
                "$parameterLabel *"
            } else {
                parameterLabel
            }
        }

        imageView.setOnClickListener {
            MaterialAlertDialogBuilder(itemView.context)
                .setTitle(itemView.context.getString(R.string.action_parameter_image_dialog_title))
                .setItems(
                    arrayOf(
                        itemView.context.getString(R.string.action_parameter_image_dialog_photo),
                        itemView.context.getString(R.string.action_parameter_image_dialog_gallery)
                    )
                ) { _, which ->
                    when (which) {
                        0 -> intentChooser(MediaStore.ACTION_IMAGE_CAPTURE)
                        1 -> intentChooser(Intent.ACTION_PICK)
                    }
                }
                .show()
        }

        removeItem.setOnClickListener {
            ContextCompat.getDrawable(imageView.context, R.drawable.image_plus)?.let {
                imageView.setImageDrawable(it)
                removeItem.visibility = View.INVISIBLE
                imageView.alpha = DEFAULT_PLACEHOLDER_ICON_ALPHA
                currentUri = null
                onValueChanged(parameterName, null, null, validate(false))
            }
        }

        newImageChosen()?.let { uri ->
            displayImage(uri)
        } ?: kotlin.run {
            getDefaultFieldValue(currentEntity, itemJsonObject) { defaultPhoto ->
                (defaultPhoto as Photo?)?.__deferred?.uri?.let {
                    displayImage(it)
                }
            }
        }

        onValueChanged(parameterName, currentUri, null, validate(false))
    }

    override fun validate(displayError: Boolean): Boolean {

        if (isMandatory() && currentUri != null) {
            if (displayError)
                showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }
        return true
    }

    override fun getInputType(format: String): Int {
        return -1
    }

    private fun newImageChosen(): Uri? =
        itemJsonObject.getSafeAny("image_uri") as Uri?

    private fun displayImage(data: Any) {
        currentUri = data.toString()
        error.visibility = View.INVISIBLE

        val glideRequest = ImageHelper.getGlideRequest(imageView, data)

        Transformations.getTransformation(
            "CropCircleWithBorder",
            imageView.context.getColorFromAttr(android.R.attr.colorPrimary)
        )?.let { transformation ->
            glideRequest.transform(transformation)
        }

        glideRequest.into(imageView)
        imageView.alpha = 1F
        removeItem.visibility = View.VISIBLE
    }

    private fun showError(text: String) {
        error.text = text
        error.visibility = View.VISIBLE
    }

    private fun intentChooser(actionType: String) {
        imageView.context.also { context ->
            when (actionType) {
                Intent.ACTION_PICK -> {
                    val pickPhoto =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    pickPhoto.type = "image/*"
                    startActivityCallback(pickPhoto, bindingAdapterPosition, null)
                }
                MediaStore.ACTION_IMAGE_CAPTURE -> {
                    val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    takePicture.also { takePictureIntent ->
                        // Ensure that there's a camera activity to handle the intent
                        takePictureIntent.resolveActivity(context.packageManager)?.also {
                            // Create the File where the photo should go
                            val photoFile: File? = try {
                                createTempImageFile(context)
                            } catch (ex: IOException) {
                                Timber.e(ex.localizedMessage)
                                ToastHelper.show(
                                    context,
                                    "Could not create temporary file",
                                    MessageType.ERROR
                                )
                                null
                            }
                            // Continue only if the File was successfully created
                            photoFile?.also {
                                try {
                                    val photoURI: Uri = FileProvider.getUriForFile(
                                        context,
                                        context.packageName + ".provider",
                                        it
                                    )
                                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                                    startActivityCallback(
                                        takePictureIntent,
                                        bindingAdapterPosition,
                                        it.absolutePath
                                    )
                                } catch (e: IllegalArgumentException) {
                                    Timber.e(e.localizedMessage)
                                    ToastHelper.show(
                                        context,
                                        "Could not create temporary file",
                                        MessageType.ERROR
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createTempImageFile(context: Context): File {
        // Create an image file
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "qmobile_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }
}
