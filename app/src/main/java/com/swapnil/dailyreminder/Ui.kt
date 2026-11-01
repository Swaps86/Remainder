@file:OptIn(ExperimentalMaterial3Api::class)

package com.swapnil.dailyreminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.util.Calendar
import java.util.Date
import java.util.Locale

val Teal = Color(0xFF0B5D66)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, secondary = Teal), content = content)
}

// ---------- helpers ----------
fun pickTime(ctx: Context, h: Int, m: Int, cb: (Int, Int) -> Unit) {
    TimePickerDialog(ctx, { _, a, b -> cb(a, b) }, h, m, false).show()
}

fun pickDate(ctx: Context, ms: Long, cb: (Long) -> Unit) {
    val c = Calendar.getInstance().apply { timeInMillis = ms }
    DatePickerDialog(ctx, { _, y, mo, d ->
        cb(Calendar.getInstance().apply { set(y, mo, d, 12, 0, 0) }.timeInMillis)
    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
}

fun dateText(ms: Long): String = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(ms))

@Composable
fun Chips(values: List<Int>, sel: Int, label: (Int) -> String, onSel: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (v in values) {
            FilterChip(selected = sel == v, onClick = { onSel(v) }, label = { Text(label(v)) })
        }
    }
}

@Composable
fun AppFooter() {
    Text(
        "© 2026 Shri Swami Samarth\nVersion 1.0.0",
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        textAlign = TextAlign.Center, fontSize = 13.sp, color = Color.Gray
    )
}

// ---------- app shell ----------
@Composable
fun App() {
    var tab by remember { mutableStateOf("Routine") }
    var menu by remember { mutableStateOf(false) }
    val subs = listOf("Profile", "Office time", "Backup", "About us")
    val sub = tab in subs
    val tabs = listOf(
        "Routine" to Icons.Default.WbSunny, "Water" to Icons.Default.WaterDrop,
        "Medicine" to Icons.Default.Medication, "Meetings" to Icons.Default.Event,
        "Notes" to Icons.Default.EditNote
    )
    BackHandler(sub) { tab = "Routine" }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (sub) tab else "Daily Reminder") },
                navigationIcon = {
                    if (sub) IconButton(onClick = { tab = "Routine" }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Menu") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        for (s in subs) {
                            DropdownMenuItem(text = { Text(s) }, onClick = { tab = s; menu = false })
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!sub) NavigationBar {
                for ((name, icon) in tabs) {
                    NavigationBarItem(
                        selected = tab == name, onClick = { tab = name },
                        icon = { Icon(icon, name) }, label = { Text(name, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                "Routine" -> ItemsScreen(Kind.ROUTINE)
                "Water" -> WaterScreen()
                "Medicine" -> ItemsScreen(Kind.MEDICINE)
                "Meetings" -> ItemsScreen(Kind.MEETING)
                "Notes" -> NotesScreen()
                "Profile" -> ProfileScreen()
                "Office time" -> OfficeScreen()
                "Backup" -> BackupScreen()
                else -> AboutScreen()
            }
        }
    }
}

// ---------- routine / medicine / meetings ----------
fun subtitle(x: Item): String {
    val p = mutableListOf<String>()
    if (x.kind == Kind.MEDICINE && x.detail.isNotBlank()) p.add(x.detail)
    p.add(if (x.daily) "Every day" else dateText(x.atMillis))
    if (x.beforeMin > 0) p.add("Alert ${x.beforeMin} min before")
    p.add("Beep Beep")
    return p.joinToString(" · ")
}

@Composable
fun ItemsScreen(kind: Kind) {
    var edit by remember { mutableStateOf<Item?>(null) }
    val list = Store.items.filter { it.kind == kind }.sortedBy { it.hour * 60 + it.minute }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (list.isEmpty()) item { Text("Nothing yet. Tap + to add.", color = Color.Gray) }
            items(list, key = { it.id }) { x -> ItemCard(x) { edit = x } }
        }
        FloatingActionButton(
            onClick = {
                edit = Item(0, kind, "", hour = 8, minute = 0,
                    endHour = if (kind == Kind.MEETING) 9 else -1)
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Icon(Icons.Default.Add, "Add") }
    }
    edit?.let { EditDialog(it) { edit = null } }
}

@Composable
fun ItemCard(x: Item, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(105.dp)) {
                Text(fmt(x.hour, x.minute), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (x.endHour >= 0) Text("to " + fmt(x.endHour, x.endMinute), fontSize = 12.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(x.title, fontWeight = FontWeight.SemiBold)
                Text(subtitle(x), fontSize = 12.sp, color = Color.Gray)
            }
            Switch(checked = x.enabled, onCheckedChange = { on -> Store.upsert(ctx, x.copy(enabled = on)) })
        }
    }
}

@Composable
fun EditDialog(init: Item, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val kind = init.kind
    var title by remember { mutableStateOf(init.title) }
    var detail by remember { mutableStateOf(init.detail) }
    var h by remember { mutableStateOf(init.hour) }
    var m by remember { mutableStateOf(init.minute) }
    var eh by remember { mutableStateOf(if (init.endHour >= 0) init.endHour else 9) }
    var em by remember { mutableStateOf(init.endMinute) }
    var before by remember { mutableStateOf(init.beforeMin) }
    var daily by remember { mutableStateOf(init.daily) }
    var dateMs by remember { mutableStateOf(if (init.atMillis > 0) init.atMillis else System.currentTimeMillis()) }
    val label = when (kind) {
        Kind.ROUTINE -> "Name (example: Wake up)"
        Kind.MEDICINE -> "Medicine name"
        else -> "Meeting title"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (init.id == 0) "Add" else "Edit") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(label) }, singleLine = true)
                if (kind == Kind.MEDICINE) {
                    OutlinedTextField(detail, { detail = it }, label = { Text("Dose (example: 1 tablet after food)") })
                }
                OutlinedButton(onClick = { pickTime(ctx, h, m) { a, b -> h = a; m = b } }) {
                    Text((if (kind == Kind.MEETING) "Start: " else "Time: ") + fmt(h, m))
                }
                if (kind == Kind.MEETING) {
                    OutlinedButton(onClick = { pickTime(ctx, eh, em) { a, b -> eh = a; em = b } }) {
                        Text("End: " + fmt(eh, em))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Every day", Modifier.weight(1f)); Switch(daily, { daily = it })
                    }
                    if (!daily) OutlinedButton(onClick = { pickDate(ctx, dateMs) { dateMs = it } }) {
                        Text("Date: " + dateText(dateMs))
                    }
                }
                Text("Alert before (minutes)", fontSize = 13.sp)
                Chips(listOf(0, 10, 15, 30), before, { it.toString() }) { before = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val at = if (daily) 0L else Calendar.getInstance().apply {
                    timeInMillis = dateMs
                    set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                Store.upsert(ctx, init.copy(
                    title = title.ifBlank { "Reminder" }, detail = detail, hour = h, minute = m,
                    endHour = if (kind == Kind.MEETING) eh else -1, endMinute = em,
                    beforeMin = before, daily = daily, atMillis = at
                ))
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (init.id != 0) TextButton(onClick = { Store.remove(ctx, init.id); onDismiss() }) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

// ---------- water ----------
@Composable
fun WaterScreen() {
    val ctx = LocalContext.current
    val w = Store.water
    fun put(n: WaterCfg) { Store.water = n; Store.save(ctx) }
    val times = Scheduler.waterTimes(w)
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Water reminder", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Switch(w.on, { put(w.copy(on = it)) })
        }
        Text("Set once. Times are made automatically. Alarm: Beep Beep.", color = Color.Gray)
        OutlinedButton(onClick = { pickTime(ctx, w.firstH, w.firstM) { a, b -> put(w.copy(firstH = a, firstM = b)) } },
            modifier = Modifier.fillMaxWidth()) { Text("First glass: " + fmt(w.firstH, w.firstM)) }
        OutlinedButton(onClick = { pickTime(ctx, w.lastH, w.lastM) { a, b -> put(w.copy(lastH = a, lastM = b)) } },
            modifier = Modifier.fillMaxWidth()) { Text("Last glass: " + fmt(w.lastH, w.lastM)) }
        Text("Repeat every (hours)")
        Chips(listOf(1, 2, 3, 4), w.everyH, { "$it hr" }) { put(w.copy(everyH = it)) }
        Text("Auto times (${times.size} glasses)", fontWeight = FontWeight.Bold)
        times.forEachIndexed { i, t ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp)) {
                    Text("Glass ${i + 1}", Modifier.weight(1f))
                    Text(fmt(t.first, t.second), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------- office time ----------
@Composable
fun OfficeScreen() {
    val ctx = LocalContext.current
    val o = Store.office
    fun put(n: OfficeCfg) { Store.office = n; Store.save(ctx) }
    val total = o.inH * 60 + o.inM + o.hours * 60
    val al = total - o.before
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Office time alarm", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Switch(o.on, { put(o.copy(on = it)) })
        }
        OutlinedButton(onClick = { pickTime(ctx, o.inH, o.inM) { a, b -> put(o.copy(inH = a, inM = b)) } },
            modifier = Modifier.fillMaxWidth()) { Text("Punch in time: " + fmt(o.inH, o.inM) + "  (tap to edit)") }
        Button(onClick = {
            val c = Calendar.getInstance()
            put(o.copy(inH = c.get(Calendar.HOUR_OF_DAY), inM = c.get(Calendar.MINUTE)))
        }, modifier = Modifier.fillMaxWidth()) { Text("Punch in now") }
        Text("Work hours required")
        Chips(listOf(9, 10, 12), o.hours, { "$it hrs" }) { put(o.copy(hours = it)) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { if (o.hours > 1) put(o.copy(hours = o.hours - 1)) }) { Text("-") }
            Text("  Custom: ${o.hours} hrs  ")
            OutlinedButton(onClick = { if (o.hours < 16) put(o.copy(hours = o.hours + 1)) }) { Text("+") }
        }
        Text("Alarm before finish (minutes)")
        Chips(listOf(10, 15, 30), o.before, { "$it min" }) { put(o.copy(before = it)) }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Punch in: " + fmt(o.inH, o.inM))
                Text("Alarm rings: " + fmt(((al / 60) % 24 + 24) % 24, ((al % 60) + 60) % 60), fontWeight = FontWeight.Bold)
                Text("Punch out: " + fmt((total / 60) % 24, total % 60))
            }
        }
        Text("Saved automatically. Repeats every day.", color = Color.Gray, fontSize = 12.sp)
    }
}

// ---------- profile and health tips ----------
fun tips(age: Int): List<Pair<String, String>> = when {
    age < 13 -> listOf(
        "Play outside daily" to "At least 1 hour of active play",
        "Sleep 9-12 hours" to "Same bedtime every night",
        "Eat fruits and vegetables" to "Less sweets and packaged snacks",
        "Limit screen time" to "Take breaks and play with friends")
    age < 18 -> listOf(
        "Sleep 8-10 hours" to "Avoid phone late at night",
        "Be active every day" to "About 60 minutes of movement",
        "Eat regular meals" to "Do not skip breakfast",
        "Talk about stress" to "Share worries with a trusted person")
    age < 30 -> listOf(
        "Sleep 7-9 hours" to "Fix one bedtime and wake time",
        "Exercise 3-5 days a week" to "Walking, sports or gym",
        "Drink enough water" to "Around 2-3 litres a day",
        "Limit junk food and screens" to "Take a break every hour")
    age < 40 -> listOf(
        "Sleep 7-9 hours" to "Fix one bedtime and wake time",
        "Walk or exercise daily" to "About 150 minutes a week",
        "Drink enough water" to "Around 2-3 litres a day",
        "Yearly health check-up" to "BP, sugar, cholesterol, weight",
        "Less sugar, salt and screen time" to "Take a break from screens every hour")
    age < 50 -> listOf(
        "Yearly health check-up" to "BP, sugar, cholesterol, eyes",
        "Exercise regularly" to "150 minutes a week plus stretching",
        "Watch your weight and waist" to "Less sugar, oil and salt",
        "Manage stress" to "Rest, hobbies and good sleep")
    age < 60 -> listOf(
        "Regular check-ups" to "Heart, diabetes, eyes, bones",
        "Walk every day" to "30 minutes at an easy pace",
        "Eat more fibre and protein" to "Less fried and salty food",
        "Take medicines on time" to "Use the Medicine tab")
    else -> listOf(
        "Walk and stretch daily" to "Gentle movement keeps joints active",
        "Regular check-ups" to "BP, sugar, eyes, hearing, bones",
        "Take medicines on time" to "Use the Medicine tab",
        "Drink water and eat light meals" to "Stay social and keep your mind active")
}

@Composable
fun ProfileScreen() {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(Store.profile.name) }
    var dob by remember {
        mutableStateOf(
            if (Store.profile.y > 0) LocalDate.of(Store.profile.y, Store.profile.m, Store.profile.d) else null
        )
    }
    val today = LocalDate.now()
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedButton(onClick = {
            val b = dob ?: LocalDate.of(1995, 1, 1)
            DatePickerDialog(ctx, { _, y, mo, d -> dob = LocalDate.of(y, mo + 1, d) },
                b.year, b.monthValue - 1, b.dayOfMonth).show()
        }, modifier = Modifier.fillMaxWidth()) {
            Text(if (dob == null) "Select date of birth" else "Date of birth: " + dob.toString())
        }
        Button(onClick = {
            Store.profile = Profile(name, dob?.year ?: 0, dob?.monthValue ?: 0, dob?.dayOfMonth ?: 0)
            Store.save(ctx)
        }, modifier = Modifier.fillMaxWidth()) { Text("Save profile") }

        val d = dob
        if (d != null && !d.isAfter(today)) {
            val per = Period.between(d, today)
            var nb = d.withYear(today.year)
            if (nb.isBefore(today)) nb = nb.plusYears(1)
            val left = Period.between(today, nb)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (name.isBlank()) "Age today" else "$name, age today", color = Color.Gray)
                    Text("${per.years} years", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Teal)
                    Text("${per.months} months ${per.days} days")
                    Text(
                        if (nb == today) "Happy birthday!" else
                            "Next birthday in ${left.months} months ${left.days} days",
                        color = Color.Gray, fontSize = 13.sp
                    )
                }
            }
            Text("Health tips for your age", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            for ((t, s) in tips(per.years)) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(t, fontWeight = FontWeight.SemiBold)
                        Text(s, fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }
            Text("General tips, not medical advice. They change as your age changes.", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

// ---------- notepad ----------
fun tomorrowNine(): Long = Calendar.getInstance().apply {
    add(Calendar.DAY_OF_YEAR, 1)
    set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun NotesScreen() {
    var edit by remember { mutableStateOf<Item?>(null) }
    val e = edit
    if (e != null) {
        NoteEditor(e) { edit = null }
    } else {
        val list = Store.items.filter { it.kind == Kind.NOTE }.sortedByDescending { it.created }
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (list.isEmpty()) item { Text("No notes yet. Tap + to write one.", color = Color.Gray) }
                items(list, key = { it.id }) { n ->
                    Card(Modifier.fillMaxWidth().clickable { edit = n }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(n.title, fontWeight = FontWeight.Bold)
                            Text(n.detail, maxLines = 2, fontSize = 14.sp)
                            Text(stamp(n.created), fontSize = 12.sp, color = Color.Gray)
                            if (n.enabled && n.atMillis > 0) Text("Reminder: " + stamp(n.atMillis), fontSize = 12.sp, color = Teal)
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = { edit = Item(0, Kind.NOTE, "", daily = false, enabled = false) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
            ) { Icon(Icons.Default.Add, "New note") }
        }
    }
}

@Composable
fun NoteEditor(item: Item, onClose: () -> Unit) {
    val ctx = LocalContext.current
    BackHandler { onClose() }
    var title by remember { mutableStateOf(item.title) }
    var body by remember { mutableStateOf(item.detail) }
    var on by remember { mutableStateOf(item.enabled && item.atMillis > 0) }
    var at by remember { mutableStateOf(if (item.atMillis > 0) item.atMillis else tomorrowNine()) }
    val created = remember { if (item.id == 0) System.currentTimeMillis() else item.created }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("Cancel") }
            Spacer(Modifier.weight(1f))
            if (item.id != 0) TextButton(onClick = { Store.remove(ctx, item.id); onClose() }) { Text("Delete") }
            Button(onClick = {
                Store.upsert(ctx, item.copy(
                    title = title.ifBlank { "Note" }, detail = body, kind = Kind.NOTE,
                    enabled = on, atMillis = if (on) at else 0L, daily = false,
                    hour = 0, minute = 0, created = created
                ))
                onClose()
            }) { Text("Save") }
        }
        Text("Written: " + stamp(created) + " (auto)", fontSize = 12.sp, color = Color.Gray)
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        // Real Android EditText: red spelling underlines, word suggestions and auto-correct
        // come from the phone's keyboard / spell checker.
        AndroidView(
            factory = { c ->
                EditText(c).apply {
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    gravity = Gravity.TOP
                    minLines = 8
                    hint = "Write your note here"
                    setText(body)
                    addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) { body = s?.toString() ?: "" }
                        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c2: Int) {}
                        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c2: Int) {}
                    })
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Reminder alarm", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Switch(on, { on = it })
        }
        if (on) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    pickDate(ctx, at) { d ->
                        val t = Calendar.getInstance().apply { timeInMillis = at }
                        val n = Calendar.getInstance().apply { timeInMillis = d }
                        t.set(n.get(Calendar.YEAR), n.get(Calendar.MONTH), n.get(Calendar.DAY_OF_MONTH))
                        at = t.timeInMillis
                    }
                }) { Text(dateText(at)) }
                OutlinedButton(onClick = {
                    val c = Calendar.getInstance().apply { timeInMillis = at }
                    pickTime(ctx, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)) { h, m ->
                        c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m); c.set(Calendar.SECOND, 0)
                        at = c.timeInMillis
                    }
                }) { Text(fmt(Calendar.getInstance().apply { timeInMillis = at }.get(Calendar.HOUR_OF_DAY),
                    Calendar.getInstance().apply { timeInMillis = at }.get(Calendar.MINUTE))) }
            }
            Text("Rings Beep Beep on this date and time.", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

// ---------- backup and import ----------
@Composable
fun BackupScreen() {
    val ctx = LocalContext.current
    var pending by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf("") }
    val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(Store.toJson().toByteArray()) }
                msg = "Backup saved."
            } catch (e: Exception) { msg = "Backup failed." }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                pending = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            } catch (e: Exception) { msg = "Could not read the file." }
        }
    }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Keep your data safe. Move it to a new phone.", color = Color.Gray)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Backup", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("File: reminder_backup_$day.json", fontSize = 12.sp, color = Color.Gray)
                Button(onClick = { create.launch("reminder_backup_$day.json") }, modifier = Modifier.fillMaxWidth()) { Text("Backup now") }
                OutlinedButton(onClick = {
                    val f = File(ctx.cacheDir, "reminder_backup_$day.json")
                    f.writeText(Store.toJson())
                    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
                    val s = Intent(Intent.ACTION_SEND).setType("application/json")
                        .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    ctx.startActivity(Intent.createChooser(s, "Share backup"))
                }, modifier = Modifier.fillMaxWidth()) { Text("Share backup (Drive, WhatsApp)") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Import", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Choose a backup file from your phone or Drive.", fontSize = 13.sp, color = Color.Gray)
                OutlinedButton(onClick = { open.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    modifier = Modifier.fillMaxWidth()) { Text("Import backup file") }
            }
        }
        if (msg.isNotBlank()) Text(msg, color = Teal, fontWeight = FontWeight.SemiBold)
        AppFooter()
    }
    val p = pending
    if (p != null) {
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Import this backup?") },
            text = { Text("Merge keeps your current data and adds the backup. Replace all deletes current data first.") },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        msg = try { Store.importJson(p, false); Store.save(ctx); "Import complete (merged)." }
                        catch (e: Exception) { "This is not a valid backup file." }
                        pending = null
                    }) { Text("Merge") }
                    TextButton(onClick = {
                        msg = try { Store.importJson(p, true); Store.save(ctx); "Import complete. All alarms are set again." }
                        catch (e: Exception) { "This is not a valid backup file." }
                        pending = null
                    }) { Text("Replace all") }
                }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } }
        )
    }
}

// ---------- about ----------
@Composable
fun AboutScreen() {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.width(96.dp).heightIn(min = 96.dp).background(Teal, androidx.compose.foundation.shape.RoundedCornerShape(27.dp)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.HourglassBottom, null, tint = Color.White, modifier = Modifier.width(56.dp))
        }
        Text("Daily Reminder", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Version 1.0.0", color = Color.Gray)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("About us", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Your daily helper for routine, water, medicine, meetings, office time and notes. Alarms ring on time so you never miss what matters.")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Developed by", fontSize = 13.sp, color = Color.Gray)
                Text("Swapnil Palande", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        AppFooter()
    }
}
