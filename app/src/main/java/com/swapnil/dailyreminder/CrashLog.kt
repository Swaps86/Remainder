package com.swapnil.dailyreminder

import android.app.Application
import android.content.Context
import android.util.Log

object CrashLog {
    fun save(ctx: Context, text: String) {
        try {
            ctx.applicationContext.getSharedPreferences("crash", Context.MODE_PRIVATE)
                .edit().putString("log", text.take(3500)).commit()
        } catch (e: Throwable) { }
    }

    fun take(ctx: Context): String? {
        val sp = ctx.applicationContext.getSharedPreferences("crash", Context.MODE_PRIVATE)
        val s = sp.getString("log", null)
        sp.edit().remove("log").apply()
        return s
    }

    fun install(ctx: Context) {
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            save(ctx, "CRASH:\n" + Log.getStackTraceString(e))
            old?.uncaughtException(t, e)
        }
    }
}

class DailyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }
}
