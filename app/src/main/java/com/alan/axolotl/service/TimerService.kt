package com.alan.axolotl.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.alan.axolotl.MainActivity
import com.alan.axolotl.R

class TimerService : Service() {

    private var countDownTimer: CountDownTimer? = null
    private var remainingSeconds: Long = 0
    private var totalSeconds: Long = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 5)
                startCountdown(minutes)
            }
            ACTION_STOP -> {
                stopCountdown()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startCountdown(minutes: Int) {
        totalSeconds = minutes * 60L
        remainingSeconds = totalSeconds
        startForeground(NOTIFICATION_ID, buildNotification(remainingSeconds))

        sendTickBroadcast(remainingSeconds, true)

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(totalSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = millisUntilFinished / 1000
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification(remainingSeconds))
                sendTickBroadcast(remainingSeconds, true)
            }

            override fun onFinish() {
                sendTickBroadcast(0, false)
                launchLockScreen()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }.start()
    }

    private fun stopCountdown() {
        countDownTimer?.cancel()
        countDownTimer = null
        sendTickBroadcast(0, false)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun sendTickBroadcast(seconds: Long, running: Boolean) {
        val intent = Intent(ACTION_TICK).apply {
            setPackage(packageName)
            putExtra(EXTRA_REMAINING, seconds)
            putExtra(EXTRA_RUNNING, running)
        }
        sendBroadcast(intent)
    }

    private fun launchLockScreen() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_TIMER_FINISHED
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun buildNotification(seconds: Long): Notification {
        val minutes = seconds / 60
        val secs = seconds % 60
        val timeText = "%02d:%02d".format(minutes, secs)

        val openIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Axolotl Timer")
            .setContentText("Time remaining: $timeText")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows countdown timer progress"
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "axolotl_timer"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.alan.axolotl.TIMER_START"
        const val ACTION_STOP = "com.alan.axolotl.TIMER_STOP"
        const val ACTION_TICK = "com.alan.axolotl.TIMER_TICK"
        const val ACTION_TIMER_FINISHED = "com.alan.axolotl.TIMER_FINISHED"
        const val EXTRA_MINUTES = "extra_minutes"
        const val EXTRA_REMAINING = "extra_remaining"
        const val EXTRA_RUNNING = "extra_running"

        fun startTimer(context: Context, minutes: Int) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MINUTES, minutes)
            }
            context.startForegroundService(intent)
        }

        fun stopTimer(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
