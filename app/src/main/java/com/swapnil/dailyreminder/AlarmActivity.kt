package com.swapnil.dailyreminder

import android.app.NotificationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

class AlarmActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val loop = object : Runnable {
        override fun run() {
            Thread { Beeper.pattern() }.start()
            handler.postDelayed(this, 8000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val code = intent.getIntExtra("code", 0)
        val title = intent.getStringExtra("title") ?: "Reminder"
        val msg = intent.getStringExtra("msg") ?: ""
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        handler.postDelayed(loop, 8000)          // beep again every 8 seconds
        handler.postDelayed({ finish() }, 120000) // stop after 2 minutes
        val c = Calendar.getInstance()
        val now = fmt(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
        setContent {
            Column(
                Modifier.fillMaxSize().background(Color(0xFF0F1B2D)).padding(32.dp),
                Arrangement.Center, Alignment.CenterHorizontally
            ) {
                Text(now, color = Color(0xFFB9C6D8), fontSize = 20.sp)
                Text(title, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(msg, color = Color(0xFFD5DEEA), fontSize = 17.sp)
                Text("Beep Beep", color = Color(0xFFB9C6D8), fontSize = 13.sp)
                Column(Modifier.height(48.dp)) {}
                Button(
                    onClick = { nm.cancel(code); finish() },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("Done", fontSize = 18.sp) }
                Column(Modifier.height(12.dp)) {}
                OutlinedButton(
                    onClick = {
                        nm.cancel(code)
                        Scheduler.snooze(this@AlarmActivity, title, msg, 5)
                        finish()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("Snooze 5 min", color = Color.White, fontSize = 18.sp) }
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
