package com.swapnil.dailyreminder

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Kind { ROUTINE, MEDICINE, MEETING, NOTE }

// One item for routine / medicine / meeting / note.
// daily = true  -> rings every day at hour:minute
// daily = false -> rings once at atMillis (meeting on a date, or a note reminder)
data class Item(
    val id: Int,
    val kind: Kind,
    val title: String,
    val detail: String = "",
    val hour: Int = 8,
    val minute: Int = 0,
    val endHour: Int = -1,
    val endMinute: Int = 0,
    val beforeMin: Int = 0,
    val daily: Boolean = true,
    val atMillis: Long = 0L,
    val enabled: Boolean = true,
    val created: Long = System.currentTimeMillis()
)

data class WaterCfg(
    val firstH: Int = 7, val firstM: Int = 0,
    val lastH: Int = 21, val lastM: Int = 0,
    val everyH: Int = 2, val on: Boolean = false
)

data class OfficeCfg(
    val inH: Int = 9, val inM: Int = 30,
    val hours: Int = 9, val before: Int = 15, val on: Boolean = false
)

data class Profile(val name: String = "", val y: Int = 0, val m: Int = 0, val d: Int = 0)

fun fmt(h: Int, m: Int): String {
    val ap = if (h % 24 < 12) "AM" else "PM"
    val hh = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d %s".format(hh, m, ap)
}

fun stamp(ms: Long): String =
    SimpleDateFormat("EEE, d MMM yyyy · h:mm a", Locale.getDefault()).format(Date(ms))

object Store {
    val items = mutableStateListOf<Item>()
    var water by mutableStateOf(WaterCfg())
    var office by mutableStateOf(OfficeCfg())
    var profile by mutableStateOf(Profile())
    private var sp: SharedPreferences? = null
    private var loaded = false

    fun init(ctx: Context) {
        if (loaded) return
        sp = ctx.applicationContext.getSharedPreferences("dr", Context.MODE_PRIVATE)
        try {
            sp?.getString("json", null)?.let { importJson(it, true) }
        } catch (e: Exception) { }
        loaded = true
    }

    fun save(ctx: Context) {
        sp?.edit()?.putString("json", toJson())?.apply()
        Scheduler.rescheduleAll(ctx.applicationContext)
    }

    fun nextId(): Int = (items.maxOfOrNull { it.id } ?: 0) + 1

    fun upsert(ctx: Context, item: Item) {
        val idx = items.indexOfFirst { x -> x.id == item.id }
        if (item.id == 0 || idx < 0) items.add(item.copy(id = nextId())) else items[idx] = item
        save(ctx)
    }

    fun remove(ctx: Context, id: Int) {
        items.removeAll { x -> x.id == id }
        save(ctx)
    }

    fun toJson(): String {
        val o = JSONObject()
        o.put("app", "DailyReminder").put("version", "1.0.0")
        val arr = JSONArray()
        for (x in items) {
            arr.put(
                JSONObject().put("id", x.id).put("kind", x.kind.name).put("title", x.title)
                    .put("detail", x.detail).put("hour", x.hour).put("minute", x.minute)
                    .put("endHour", x.endHour).put("endMinute", x.endMinute)
                    .put("before", x.beforeMin).put("daily", x.daily).put("at", x.atMillis)
                    .put("enabled", x.enabled).put("created", x.created)
            )
        }
        o.put("items", arr)
        o.put("water", JSONObject().put("fh", water.firstH).put("fm", water.firstM)
            .put("lh", water.lastH).put("lm", water.lastM).put("ev", water.everyH).put("on", water.on))
        o.put("office", JSONObject().put("ih", office.inH).put("im", office.inM)
            .put("hrs", office.hours).put("bf", office.before).put("on", office.on))
        o.put("profile", JSONObject().put("name", profile.name)
            .put("y", profile.y).put("m", profile.m).put("d", profile.d))
        return o.toString(2)
    }

    // replace = true: replace everything. replace = false: merge (keep current data).
    fun importJson(text: String, replace: Boolean) {
        val o = JSONObject(text)
        val arr = o.optJSONArray("items") ?: JSONArray()
        val list = mutableListOf<Item>()
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            list.add(
                Item(
                    j.optInt("id"), Kind.valueOf(j.optString("kind", "ROUTINE")),
                    j.optString("title"), j.optString("detail"),
                    j.optInt("hour", 8), j.optInt("minute"),
                    j.optInt("endHour", -1), j.optInt("endMinute"),
                    j.optInt("before"), j.optBoolean("daily", true), j.optLong("at"),
                    j.optBoolean("enabled", true), j.optLong("created", System.currentTimeMillis())
                )
            )
        }
        if (replace) {
            items.clear(); items.addAll(list)
            o.optJSONObject("water")?.let {
                water = WaterCfg(it.optInt("fh", 7), it.optInt("fm"), it.optInt("lh", 21),
                    it.optInt("lm"), it.optInt("ev", 2), it.optBoolean("on"))
            }
            o.optJSONObject("office")?.let {
                office = OfficeCfg(it.optInt("ih", 9), it.optInt("im", 30), it.optInt("hrs", 9),
                    it.optInt("bf", 15), it.optBoolean("on"))
            }
            o.optJSONObject("profile")?.let {
                profile = Profile(it.optString("name"), it.optInt("y"), it.optInt("m"), it.optInt("d"))
            }
        } else {
            for (x in list) {
                val dup = items.any { e ->
                    e.kind == x.kind && e.title == x.title && e.hour == x.hour &&
                        e.minute == x.minute && e.created == x.created
                }
                if (!dup) items.add(x.copy(id = nextId()))
            }
        }
    }
}
