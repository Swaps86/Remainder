package com.swapnil.dailyreminder

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        setContent {
            AppTheme {
                App()
                var report by remember { mutableStateOf(last) }
                val r = report
                if (r != null) {
                    AlertDialog(
                        onDismissRequest = { report = null },
                        title = { Text("Error found. Please send this text to the developer") },
                        text = { Text(r, fontSize = 11.sp, modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())) },
                        confirmButton = {
                            TextButton(onClick = {
                                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("error", r))
                                report = null
                            }) { Text("Copy and close") }
                        },
                        dismissButton = { TextButton(onClick = { report = null }) { Text("Close") } }
                    )
                }
            }
        }
    }
}
