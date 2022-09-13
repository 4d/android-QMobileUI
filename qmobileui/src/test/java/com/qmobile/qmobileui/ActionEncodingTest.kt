/*
 * Created by htemanni on 8/9/2022.
 * 4D SAS
 * Copyright (c) 2022 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui

import android.util.Base64
import com.qmobile.qmobileui.action.utils.ActionHelper
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ActionEncodingTest {

    @Before
    fun `Bypass android_util_Base64 to java_util_Base64`() {
        mockkStatic(Base64::class)
        val arraySlot = slot<ByteArray>()

        every {
            Base64.encodeToString(capture(arraySlot), Base64.NO_WRAP)
        } answers {
            java.util.Base64.getEncoder().encodeToString(arraySlot.captured)
        }

        val stringSlot = slot<String>()
        every {
            Base64.decode(capture(stringSlot), Base64.NO_WRAP)
        } answers {
            java.util.Base64.getDecoder().decode(stringSlot.captured)
        }
    }

    @Test
    fun `get base64 encoded context  from action content for current entity`() {
        val content: HashMap<String, Any> = HashMap()
        val context: HashMap<String, Any> = HashMap()
        val entity = mapOf("primaryKey" to 2)
        context["dataClass"] = "Employee"
        context["entity"] = entity
        content["context"] = context
        assert(ActionHelper.getBase64EncodedContext(content) == "eyJkYXRhQ2xhc3MiOiJFbXBsb3llZSIsImVudGl0eSI6eyJwcmltYXJ5S2V5IjoyfX0=")
    }

    @Test
    fun `get base64 encoded context  from action content for table`() {
        val content: HashMap<String, Any> = HashMap()
        val context: HashMap<String, Any> = HashMap()
        context["dataClass"] = "Employee"
        content["context"] = context
        println(content)
        assert(ActionHelper.getBase64EncodedContext(content) == "eyJkYXRhQ2xhc3MiOiJFbXBsb3llZSJ9")
    }
}
