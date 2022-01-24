/*
 * Created by Quentin Marciset on 11/8/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.annotation.SuppressLint
import android.content.Context
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream

// From Medium post : https://medium.com/@karnsaheb/logging-to-disk-reactively-on-android-68c4d0ec489

/**
 * The LogElement triple provides an easy wrapper for the Date
 * (as a string), the priority (log level), and the log message.
 */
typealias LogElement = Triple<String, Int, String?>

object LogController {

    /**
     * Flush sends a signal which allows the buffer to release its contents downstream.
     */
    private var flush = BehaviorSubject.create<Long>()

    /**
     * Signal that the flush has completed
     */
    private var flushCompleted = BehaviorSubject.create<Long>()

    private var LOG_LEVELS = arrayOf(
        "",
        "",
        "VERBOSE",
        "DEBUG",
        "INFO",
        "WARN",
        "ERROR",
        "ASSERT"
    )

    /**
     * ~1.66MB/~450kb gzipped.
     */

    private const val RETENTION_DAYS_DURATION: Long = 14
    private const val INTERVAL_SIGNAL_MINUTES: Long = 5
    private const val PROCESSED_THRESHOLD = 20
    private const val GZIP_BUFFER_SIZE = 1024
    private const val LOG_FILE_MAX_SIZE_THRESHOLD = 5 * 1024 * 1024
    private val LOG_FILE_TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    private val LOG_LINE_TIME_FORMAT =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private lateinit var filePath: String
    private const val LOG_FILE_NAME = "insights.log"

    fun initialize(context: Context) {
        filePath = try {
            getLogsDirectoryFromPath(
                context.filesDir.absolutePath
            )
        } catch (e: FileNotFoundException) {
            // Fallback to default path
            context.filesDir.absolutePath
        }
        println("LogsDirectory path = $filePath")
        Timber.plant(
            Timber.DebugTree(),
            FileTree()
        )
    }

    /**
     * The FileTree is the additional log handler which we plant.
     * It's role is to buffer logs and periodically write them to disk.
     */
    // @SuppressLint("CheckResult")
    class FileTree : Timber.Tree() {

        /**
         * The Observable which will receive the log messages which are
         * to be written to disk.
         */
        private val logBuffer = PublishSubject.create<LogElement>()

        init {

            // Maintain a count of the processed LogElements
            var processed = 0
            logBuffer.observeOn(Schedulers.computation())
                // Increment the counter after each item is processed
                // and perform a flush if the criteria is met.
                .doOnEach {
                    processed++

                    if (processed % PROCESSED_THRESHOLD == 0) {
                        flush()
                    }
                }
                // Merge the signal from flush and the signal from
                // the interval observer to create a dual signal.
                .buffer(
                    flush.mergeWith(
                        Observable.interval(
                            INTERVAL_SIGNAL_MINUTES,
                            TimeUnit.MINUTES
                        )
                    )
                )
                .subscribeOn(Schedulers.io())
                .subscribe { // it: LogElement
                    try {
                        // Open file
                        val f =
                            getLogFile()

                        // Write to log
                        FileWriter(f, true).use { fw ->
                            // Write log lines to the file
                            it.forEach { (date, priority, message) ->
                                fw.append("$date\t${LOG_LEVELS[priority]}\t$message\n")
                            }

                            // Write a line indicating the number of log lines proceed
                            fw.append(
                                "${LOG_LINE_TIME_FORMAT.format(Date())}\t${LOG_LEVELS[2] /* Verbose */}" +
                                    "\tFlushing logs -- total processed: $processed\n"
                            )

                            fw.flush()
                        }

                        // Validate file size
                        flushCompleted.onNext(f.length())
                    } catch (e: IOException) {
                        Timber.e("An error occurred while writing logs into log file")
                    }
                }

            flushCompleted
                .subscribeOn(Schedulers.io())
                .filter { size -> size > LOG_FILE_MAX_SIZE_THRESHOLD }
                .subscribe { rotateLogs() }
        }

        /**
         * Schedule this log to be written to disk.
         */
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // For the sake of simplicity we skip logging the exception,
            // but you can parse the exception and and emit it as needed.
            logBuffer.onNext(
                LogElement(
                    LOG_LINE_TIME_FORMAT.format(
                        Date()
                    ),
                    priority,
                    message
                )
            )
        }
    }

    fun flush(onComplete: (() -> Unit)? = null) {
        onComplete?.run {
            Timber.w("Subscribing to flush completion handler")

            flushCompleted
                .take(1)
                .timeout(2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .onErrorReturn { -1L }
                .filter { it > 0 }
                .subscribe {
                    rotateLogs()

                    // Delegate back to caller
                    onComplete()
                }
        }

        flush.onNext(1L)
    }

    /**
     * A utility function to rotate application logs. This function
     * operates in three steps.
     * 1. Compresses the existing log file into a gzip format.
     * 2. Truncate the existing log file to size zero to reset it.
     * 3. Grab all the compressed files that are outside the retention
     *    period and delete them.
     */
    private fun rotateLogs() {
        val file = getLogFile()

        if (!compress(file)) {
            // Unable to compress file
            return
        }

        // Truncate the file to zero.
        PrintWriter(file).close()

        // Iterate over the gzipped files in the directory and delete the files outside the
        // retention period.
        val currentTime = System.currentTimeMillis()
        file.parentFile?.listFiles()
            ?.filter {
                it.extension.lowercase() == "gz" &&
                    it.lastModified() + TimeUnit.DAYS.toMillis(RETENTION_DAYS_DURATION) < currentTime
            }?.map { it.delete() }
    }

    private fun getLogsDirectoryFromPath(path: String): String {
        // in file explorer, this is data/data/com.qmobile.sample4dapp.files.logs.insights.log
        val dir = File(path, "logs")

        if (!dir.exists() && !dir.mkdirs()) {
            throw FileNotFoundException("Unable to create logs file")
        }

        return dir.absolutePath
    }

    private fun getLogFile(): File {
        val file = File(
            filePath,
            LOG_FILE_NAME
        )

        if (!file.exists() && !file.createNewFile()) {
            throw IOException("Unable to load log file")
        }

        if (!file.canWrite()) {
            throw IOException("Log file not writable")
        }

        return file
    }

    private fun compress(file: File): Boolean {
        try {
            // Create a new file for the compressed logs.
            val compressed = File(
                file.parentFile?.absolutePath,
                "${file.name.substringBeforeLast(".")}_${LOG_FILE_TIME_FORMAT.format(Date())}.gz"
            )

            write(file, compressed)
        } catch (e: IOException) {
            Timber.e("An error occurred while compressing log file into gzip format")
            return false
        }
        return true
    }

    private fun write(file: File, compressed: File) {
        FileInputStream(file).use { fis ->
            FileOutputStream(compressed).use { fos ->
                GZIPOutputStream(fos).use { gzos ->

                    val buffer = ByteArray(GZIP_BUFFER_SIZE)
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
