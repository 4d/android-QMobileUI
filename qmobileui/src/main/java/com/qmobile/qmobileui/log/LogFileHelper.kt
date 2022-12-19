/*
 * Created by qmarciset on 15/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.log

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPOutputStream

@SuppressLint("LogNotTimber")
object LogFileHelper {

    private const val logFilePrefix = "crash_log_"
    private const val logsFolder = "logs"
    private const val zipFileName = "data.zip"
    private const val gZIPBufferSize = 1024
    private val logFileTimeFormat = SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", Locale.US)

    fun createLogFile(context: Context, content: String) {
        try {
            val fileName = "${logFilePrefix}${getCurrentDateTimeLogFormat()}.txt"
            val newFile = File(context.filesDir.absolutePath + File.separator + logsFolder + File.separator + fileName)
            newFile.apply {
                parentFile?.mkdirs()
                createNewFile()
                writeText(content)
            }
        } catch (ex: FileNotFoundException) {
            Log.e("LogFileHelper", ex.message.orEmpty())
            Log.e("LogFileHelper", "Could not create crash log file")
        } catch (ioe: IOException) {
            Log.e("LogFileHelper", ioe.message.orEmpty())
            Log.e("LogFileHelper", "Could not write to crash log file to log UncaughtException")
        }
    }

    fun getCurrentDateTimeLogFormat(): String {
        return logFileTimeFormat.format(Date())
    }

    fun findCrashLogFile(context: Context): File? {
        return try {
            context.filesDir.walkTopDown().firstOrNull { it.name.startsWith(logFilePrefix) }
        } catch (ex: FileNotFoundException) {
            Log.e("LogFileHelper", ex.message.orEmpty())
            Log.e("LogFileHelper", "Could not find crash log file")
            null
        } catch (ioe: IOException) {
            Log.e("LogFileHelper", ioe.message.orEmpty())
            Log.e("LogFileHelper", "Could not get crash log file")
            null
        }
    }

    fun cleanOlderCrashLogs(context: Context) {
        context.filesDir.walkTopDown().filter { it.name.startsWith(logFilePrefix) || it.name == zipFileName }
            .forEach { file ->
                file.delete()
            }
    }

    fun compress(file: File): File? {
        return try {
            // Create a new file for the compressed logs.
            val compressed = File(file.parentFile?.absolutePath, zipFileName)
            write(file, compressed)
            compressed
        } catch (e: IOException) {
            Log.e("LogFileHelper", e.message.orEmpty())
            Log.e("LogFileHelper", "An error occurred while compressing log file into gzip format")
            null
        }
    }

    @Suppress("NestedBlockDepth")
    private fun write(file: File, compressed: File) {
        FileInputStream(file).use { fis ->
            FileOutputStream(compressed).use { fos ->
                GZIPOutputStream(fos).use { gzos ->

                    val buffer = ByteArray(gZIPBufferSize)
                    var length = fis.read(buffer)

                    while (length > 0) {
                        gzos.write(buffer, 0, length)
                        length = fis.read(buffer)
                    }

                    // Finish file compressing and close all streams.
                    gzos.finish()
                }
            }
        }
    }
}
