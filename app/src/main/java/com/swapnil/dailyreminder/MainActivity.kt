package com.swapnil.dailyreminder

import android.Manifest
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        Scheduler.rescheduleAll(this)
        val last = CrashLog.take(this)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifOk = nm.areNotificationsEnabled()
        val fsOk = Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent()
        val sp = getSharedPreferences("setup", Context.MODE_PRIVATE)
        val firstTime = !sp.getBoolean("shown", false)
        val pkg = Uri.parse("package:$packageName")
        setContent {
            AppTheme {
                App()
                var report by remember { mutableStateOf(last) }
                var setup by remember { mutableStateOf(last == null && (firstTime || !notifOk || !fsOk)) }
                val r = report
                if (r != null) {
                    AlertDialog(
                        onDismissRequest = { report = null },
                        title = { Text("Error found. Please send this text to the developer") },
                        text = { Text(r, fontSize = 11.sp, modifier = Modifier.verticalScroll(rememberScrollState())) },
                        confirmButton = {
                            TextButton(onClick = {
                                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("error", r))
                                report = null
                            }) { Text("Copy and close") }
                        },
                        dismissButton = { TextButton(onClick = { report = null }) { Text("Close") } }
                    )
                } else if (setup) {
                    AlertDialog(
                        onDismissRequest = { },
                        title = { Text("Show alarms on lock screen") },
                        text = {
                            Column(Modifier.verticalScroll(rememberScrollState())) {
                                Text("To wake the screen and show the alarm when the phone is locked, please allow these:")
                                OutlinedButton(onClick = {
                                    startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                                }) { Text("1. Turn on notifications" + if (notifOk) " (done)" else "") }
                                if (Build.VERSION.SDK_INT >= 34) {
                                    OutlinedButton(onClick = {
                                        startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg))
                                    }) { Text("2. Allow full-screen alarms" + if (fsOk) " (done)" else "") }
                                }
                                OutlinedButton(onClick = {
                                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg))
                                }) { Text("3. Battery: choose Unrestricted") }
                                Text("Also allow 'Display over other apps' or 'Show on lock screen' if your phone has it.", fontSize = 12.sp)
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                sp.edit().putBoolean("shown", true).apply()
                                setup = false
                            }) { Text("Done") }
                        }
                    )
                }
            }
        }
    }
}
