package com.swapnil.dailyreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object Scheduler {
    data class Slot(val code: Int, val at: Long, val title: String, val msg: String)

    // Next time of day h:m (plus addMin minutes) that is still in the future.
    fun nextDaily(h: Int, m: Int, addMin: Int): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        c.add(Calendar.MINUTE, addMin)
        while (c.timeInMillis <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    fun waterTimes(w: WaterCfg): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        var t = w.firstH * 60 + w.firstM
        val end = w.lastH * 60 + w.lastM
        val step = maxOf(1, w.everyH) * 60
        while (t <= end && out.size < 48) { out.add((t / 60) to (t % 60)); t += step }
        return out
    }

    fun slots(): List<Slot> {
        val out = mutableListOf<Slot>()
        val now = System.currentTimeMillis()
        for (x in Store.items) {
            if (!x.enabled) continue
            val at = if (x.daily) nextDaily(x.hour, x.minute, -x.beforeMin)
            else x.atMillis - x.beforeMin * 60000L
            if (at <= now) continue
            val msg = when (x.kind) {
                Kind.ROUTINE -> "Time for ${x.title}"
                Kind.MEDICINE -> x.detail.ifBlank { "Take your medicine" }
                Kind.MEETING -> "Starts ${fmt(x.hour, x.minute)}" +
                    (if (x.endHour >= 0) ", ends ${fmt(x.endHour, x.endMinute)}" else "")
                Kind.NOTE -> x.detail.take(120)
            }
            out.add(Slot(10000 + x.id, at, x.title.ifBlank { "Reminder" }, msg))
        }
        val w = Store.water
        if (w.on) {
            val times = waterTimes(w)
            times.forEachIndexed { i, t ->
                out.add(Slot(1000 + i, nextDaily(t.first, t.second, 0), "Drink water",
                    "Glass ${i + 1} of ${times.size}"))
            }
        }
        val o = Store.office
        if (o.on) {
            val total = o.inH * 60 + o.inM + o.hours * 60
            out.add(Slot(2000, nextDaily(o.inH, o.inM, o.hours * 60 - o.before), "Office time",
                "${o.before} minutes left. Punch out at ${fmt((total / 60) % 24, total % 60)}"))
        }
        return out
    }

    private fun pi(ctx: Context, code: Int, s: Slot?): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java)
        if (s != null) i.putExtra("code", s.code).putExtra("title", s.title).putExtra("msg", s.msg)
        return PendingIntent.getBroadcast(ctx, code, i,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun set(ctx: Context, am: AlarmManager, s: Slot) {
        val show = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(s.at, show), pi(ctx, s.code, s))
        } catch (e: Throwable) {
            CrashLog.save(ctx, "ALARM SET ERROR:\n" + android.util.Log.getStackTraceString(e))
            try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, s.at, pi(ctx, s.code, s)) } catch (e2: Throwable) { }
        }
    }

    fun rescheduleAll(ctx: Context) {
        try { doReschedule(ctx) } catch (e: Throwable) {
            CrashLog.save(ctx, "SCHEDULE ERROR:\n" + android.util.Log.getStackTraceString(e))
        }
    }

    private fun doReschedule(ctx: Context) {
        val c = ctx.applicationContext
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val sp = c.getSharedPreferences("dr_codes", Context.MODE_PRIVATE)
        for (code in sp.getStringSet("codes", emptySet()) ?: emptySet()) {
            am.cancel(pi(c, code.toInt(), null))
        }
        val s = slots()
        for (x in s) set(c, am, x)
        sp.edit().putStringSet("codes", s.map { it.code.toString() }.toSet()).apply()
    }

    fun snooze(ctx: Context, title: String, msg: String, minutes: Int) {
        val c = ctx.applicationContext
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val at = System.currentTimeMillis() + minutes * 60000L
        set(c, am, Slot(4000 + ((at / 1000) % 1000).toInt(), at, title, msg))
    }
}
