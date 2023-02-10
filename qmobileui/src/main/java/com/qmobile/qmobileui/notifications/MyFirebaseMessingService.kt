/*
 * Created by qmarciset on 19/1/2023.
 * 4D SAS
 * Copyright (c) 2023 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.BaseActivity
import com.qmobile.qmobileui.activity.loginactivity.LoginActivity
import com.qmobile.qmobileui.activity.mainactivity.MainActivity
import com.qmobile.qmobileui.utils.DeepLinkUtil.PN_DEEPLINK_DATACLASS
import com.qmobile.qmobileui.utils.DeepLinkUtil.PN_DEEPLINK_PRIMARY_KEY
import timber.log.Timber
import kotlin.random.Random

class MyFirebaseMessingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Timber.d("Refreshed token : $token")
        (baseContext as? MainActivity)?.getCurrentFCMToken()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Timber.i("Push notification received, message: ${message.data}")
        if (BaseApp.runtimeDataHolder.pushNotification) {
            showNotification(message)
        }
    }

    @Suppress("LongMethod")
    private fun showNotification(message: RemoteMessage) {
        applicationContext.apply {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val content: Map<String, String> = message.data

            var dataSynchro = false
            var title: String? = null
            var body: String? = null
            var color: String? = null
            var sound: String? = null
            var imageUrl: String? = null

            // open()
            var dataClass: String? = null
            var primaryKey: String? = null

            for ((key, value) in content) {
                when (key) {
                    "dataSynchro" -> dataSynchro = value.toBoolean()
                    "title" -> title = value
                    "body" -> body = value
                    "color" -> color = value
                    "sound" -> sound = value
                    "imageUrl" -> imageUrl = value
                    "dataClass" -> dataClass = value
                    "entity" ->
                        primaryKey =
                            retrieveJSONObject(value)?.getSafeString("primaryKey")
                }
            }

            val notificationId = Random.nextInt(0, Int.MAX_VALUE)
            BaseApp.sharedPreferencesHolder.addNotificationId(notificationId)

            val activityIntent = Intent(this, LoginActivity::class.java)

            dataClass?.let { activityIntent.putExtra(PN_DEEPLINK_DATACLASS, it) }
            primaryKey?.let { activityIntent.putExtra(PN_DEEPLINK_PRIMARY_KEY, it) }

            activityIntent.putExtra(BaseActivity.PUSH_DATA_SYNC, dataSynchro)
            activityIntent.putExtra(BaseActivity.CURRENT_NOTIFICATION_ID, notificationId)
            val activityPendingIntent = PendingIntent.getActivity(
                this,
                Random.nextInt(0, Int.MAX_VALUE),
                activityIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) PendingIntent.FLAG_IMMUTABLE else 0
            )

            val futureTarget = imageUrl?.let {
                Glide.with(this)
                    .asBitmap()
                    .load(it)
                    .submit()
            }

            val style = if (futureTarget != null) {
                NotificationCompat.BigPictureStyle()
                    .bigPicture(futureTarget.get())
                    .bigLargeIcon(null)
            } else {
                NotificationCompat.BigTextStyle()
                    .bigText(body)
            }

            val notification =
                NotificationCompat.Builder(this, resources.getString(R.string.push_channel_id))
                    .setSmallIcon(R.drawable.launch_screen)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setContentIntent(activityPendingIntent)
                    .setLargeIcon(futureTarget?.get())
                    .setStyle(style)
                    .setColor(getColor(color) ?: ContextCompat.getColor(this, R.color.seed))
                    .setSound(getSound(sound))
                    .build()

            Glide.with(this).clear(futureTarget)

            notificationManager.notify(notificationId, notification)
        }
    }

    private fun getColor(colorHex: String?): Int? {
        if (colorHex != null) {
            return try {
                Color.parseColor(colorHex)
            } catch (e: java.lang.Exception) {
                Timber.e(e.message.orEmpty())
                null
            }
        }
        return null
    }

    private fun getSound(sound: String?): Uri? {
        if (sound != null) {
            return Uri.parse(sound)
        }
        return null
    }
}
