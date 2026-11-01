package com.swapnil.dailyreminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

// Foreground service: keeps the app alive while the phone is idle, wakes the screen,
// shows the alarm screen and repeats Beep Beep until Done / Snooze (max 2 minutes).
class AlarmService : Service() {
    companion object {
        @Volatile var running = false
        @Volatile private var gen = 0
        const val NOTIF_ID = 77
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, AlarmService::class.java))
        }
    }

    private var wl: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getIntExtra("code", 0) ?: 0
        val title = intent?.getStringExtra("title") ?: "Reminder"
        val msg = intent?.getStringExtra("msg") ?: ""

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel("alarm3", "Alarms", NotificationManager.IMPORTANCE_HIGH)
        ch.setSound(null, null)
        ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        nm.createNotificationChannel(ch)

        val open = Intent(this, AlarmActivity::class.java)
            .putExtra("code", code).putExtra("title", title).putExtra("msg", msg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val fl = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getActivity(this, code, open, fl)
        val done = PendingIntent.getBroadcast(this, code + 1,
            Intent(this, AlarmActionReceiver::class.java).setAction("DONE"), fl)
        val snooze = PendingIntent.getBroadcast(this, code + 2,
            Intent(this, AlarmActionReceiver::class.java).setAction("SNOOZE")
                .putExtra("title", title).putExtra("msg", msg), fl)
        val icon = Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel)
        val n = Notification.Builder(this, "alarm3")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title).setContentText(msg)
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(pi).setFullScreenIntent(pi, true)
            .addAction(Notification.Action.Builder(icon, "Done", done).build())
            .addAction(Notification.Action.Builder(icon, "Snooze 5 min", snooze).build())
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, n)
        }

        try {
            wl?.release()
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE, "dailyreminder:alarmservice")
            wl?.acquire(130000)
        } catch (e: Throwable) { }

        try { startActivity(open) } catch (e: Throwable) { }

        running = true
        gen++
        val mine = gen
        Thread {
            var cycles = 0
            while (running && mine == gen && cycles < 15) {
                Beeper.pattern()
                cycles++
                var w = 0
                while (running && mine == gen && w < 5) { Thread.sleep(1000); w++ }
            }
            if (mine == gen) stopSelf()
        }.start()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        try { wl?.release() } catch (e: Throwable) { }
        super.onDestroy()
    }
}

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        AlarmService.stop(ctx)
        if (i.action == "SNOOZE") {
            Scheduler.snooze(ctx, i.getStringExtra("title") ?: "Reminder", i.getStringExtra("msg") ?: "", 5)
        }
    }
}
