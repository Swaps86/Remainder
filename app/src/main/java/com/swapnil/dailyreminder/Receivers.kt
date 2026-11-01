package com.swapnil.dailyreminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.PowerManager

// "Beep Beep" ... pause ... "Beep Beep"  (two times)
object Beeper {
    fun pattern() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            repeat(2) {
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, 200); Thread.sleep(300)
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, 200); Thread.sleep(300)
                Thread.sleep(500)
            }
            Thread.sleep(300)
            tg.release()
        } catch (e: Exception) { }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        try {
            wake(ctx)
            Store.init(ctx)
            val code = i.getIntExtra("code", 0)
            val title = i.getStringExtra("title") ?: "Reminder"
            val msg = i.getStringExtra("msg") ?: ""
            var started = false
            try {
                ctx.startForegroundService(Intent(ctx, AlarmService::class.java)
                    .putExtra("code", code).putExtra("title", title).putExtra("msg", msg))
                started = true
            } catch (e: Throwable) {
                CrashLog.save(ctx, "SERVICE START ERROR:\n" + android.util.Log.getStackTraceString(e))
            }
            if (!started) {
                try { notify(ctx, code, title, msg) } catch (e: Throwable) { }
                try {
                    ctx.startActivity(Intent(ctx, AlarmActivity::class.java)
                        .putExtra("code", code).putExtra("title", title).putExtra("msg", msg)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Throwable) { }
                val pending = goAsync()
                Thread {
                    try { Beeper.pattern() } finally { pending.finish() }
                }.start()
            }
            Scheduler.rescheduleAll(ctx)
        } catch (e: Throwable) {
            CrashLog.save(ctx, "RECEIVER ERROR:\n" + android.util.Log.getStackTraceString(e))
        }
    }

    @Suppress("DEPRECATION")
    private fun wake(ctx: Context) {
        try {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE, "dailyreminder:alarm")
            wl.acquire(20000)
        } catch (e: Throwable) { }
    }

    private fun notify(ctx: Context, code: Int, title: String, msg: String) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel("alarm2", "Alarms", NotificationManager.IMPORTANCE_HIGH)
        ch.setSound(null, null)
        ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        ch.enableVibration(true)
        nm.createNotificationChannel(ch)
        val open = Intent(ctx, AlarmActivity::class.java)
            .putExtra("code", code).putExtra("title", title).putExtra("msg", msg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = PendingIntent.getActivity(ctx, code, open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = Notification.Builder(ctx, "alarm2")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title).setContentText(msg)
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(pi).setFullScreenIntent(pi, true)
            .setAutoCancel(true).build()
        nm.notify(code, n)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        Store.init(ctx)
        Scheduler.rescheduleAll(ctx)
    }
}
