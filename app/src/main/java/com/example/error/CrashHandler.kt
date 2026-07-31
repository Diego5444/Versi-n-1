package com.example.error

import android.content.Context
import android.content.Intent
import android.os.Process
import com.example.ui.error.ErrorActivity
import java.io.PrintWriter
import java.io.StringWriter

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()
            val message = throwable.localizedMessage ?: throwable.message ?: "Error desconocido"

            val intent = Intent(context, ErrorActivity::class.java).apply {
                putExtra(ErrorActivity.EXTRA_ERROR_MESSAGE, message)
                putExtra(ErrorActivity.EXTRA_ERROR_STACKTRACE, stackTrace)
                putExtra(ErrorActivity.EXTRA_THREAD_NAME, thread.name)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)

            Process.killProcess(Process.myPid())
            System.exit(10)
        } catch (e: Exception) {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        fun init(context: Context) {
            val handler = CrashHandler(context.applicationContext)
            Thread.setDefaultUncaughtExceptionHandler(handler)
        }
    }
}
