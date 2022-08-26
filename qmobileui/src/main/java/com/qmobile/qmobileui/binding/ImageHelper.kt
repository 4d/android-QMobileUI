/*
 * Created by Quentin Marciset on 4/6/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.binding

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.glide.CustomRequestListener
import com.qmobile.qmobileui.ui.SnackbarHelper
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException
import java.util.Calendar

object ImageHelper {

    const val drawableStartWidth = 24
    const val drawableStartHeight = 24
    const val drawableSpace = 8
    const val luminanceThreshold = 0.5
    const val ICON_MARGIN = 8
    const val NO_ICON_PADDING = 24
    const val DEFAULT_BITMAP_QUALITY = 85

    private val factory =
        DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()

    fun getGlideRequest(view: View, data: Any): RequestBuilder<Drawable> =
        Glide.with(view.context.applicationContext)
            .load(data)
            .transition(DrawableTransitionOptions.withCrossFade(factory))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .listener(CustomRequestListener())
            .error(R.drawable.image_not_supported)

    fun getImage(imageUrl: String?, fieldName: String?, key: String?, tableName: String?): Any =
        tryImageFromAssets(tableName, key, fieldName)
            ?: if (!imageUrl.isNullOrEmpty()) imageUrl else R.drawable.image_not_supported

    private fun tryImageFromAssets(tableName: String?, key: String?, fieldName: String?): Uri? {
        BaseApp.runtimeDataHolder.embeddedFiles.find {
            it.contains(tableName + File.separator + "$tableName($key)_${fieldName}_")
        }?.let { path ->
            Timber.d("file = $path")
            return Uri.parse("file:///android_asset/$path")
        }
        return null
    }

    fun Drawable?.adjustActionDrawableMargins(context: Context): Drawable {
        val iconMarginPx =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                ICON_MARGIN.toFloat(),
                context.resources.displayMetrics
            )
                .toInt()

        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            InsetDrawable(this, iconMarginPx, 0, iconMarginPx, 0)
        } else {
            object : InsetDrawable(this, iconMarginPx, 0, iconMarginPx, 0) {
                override fun getIntrinsicWidth(): Int {
                    return intrinsicHeight + iconMarginPx + iconMarginPx
                }
            }
        }
    }

    fun getTempImageFile(
        activity: FragmentActivity,
        format: Bitmap.CompressFormat,
        callback: (uri: Uri, photoFilePath: String) -> Unit
    ) {
        val photoFile: File? = try {
            createTempImageFile(activity, format)
        } catch (ex: IOException) {
            Timber.e(ex.message.orEmpty())
            SnackbarHelper.show(activity, "Could not create temporary file", ToastMessage.Type.ERROR)
            null
        }
        photoFile?.let {
            try {
                val photoURI: Uri = FileProvider.getUriForFile(activity, activity.packageName + ".provider", it)
                callback(photoURI, it.absolutePath)
            } catch (e: IllegalArgumentException) {
                Timber.e(e.message.orEmpty())
                SnackbarHelper.show(activity, "Could not create temporary file", ToastMessage.Type.ERROR)
            }
        }
    }

    private fun createTempImageFile(activity: FragmentActivity, format: Bitmap.CompressFormat): File {
        val timeStamp = Calendar.getInstance().timeInMillis
        val storageDir: File? = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return when (format) {
            Bitmap.CompressFormat.PNG -> File.createTempFile("PNG_${timeStamp}_", ".png", storageDir)
            else -> File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        }
    }
}

@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

fun isDarkColor(@ColorInt color: Int): Boolean =
    ColorUtils.calculateLuminance(color) < ImageHelper.luminanceThreshold

val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun File.writeBitmap(
    bitmap: Bitmap,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    quality: Int = ImageHelper.DEFAULT_BITMAP_QUALITY
) {
    outputStream().use { out ->
        bitmap.compress(format, quality, out)
        out.flush()
    }
}
