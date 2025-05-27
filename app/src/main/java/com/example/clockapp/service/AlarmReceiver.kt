package com.example.clockapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.clockapp.R
import com.example.clockapp.db.AlarmDatabaseHelper
import com.example.clockapp.ui.AlarmRingActivity

/**
 * 闹钟广播接收器
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ALARM_ID = "alarm_id"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "alarm_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm received")
        
        val alarmId = intent.getStringExtra(ALARM_ID) ?: return
        Log.d(TAG, "Alarm ID: $alarmId")
        
        // 获取闹钟数据
        val dbHelper = AlarmDatabaseHelper(context)
        val alarm = dbHelper.getAlarm(alarmId)
        
        if (alarm == null) {
            Log.e(TAG, "Alarm not found in database")
            return
        }
        
        if (!alarm.isEnabled) {
            Log.d(TAG, "Alarm is disabled")
            return
        }
        
        // 启动闹钟响铃界面
        val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(ALARM_ID, alarmId)
        }
        context.startActivity(ringIntent)
        
        // 创建通知
        createNotification(context, alarm.label.ifEmpty { "闹钟" }, alarmId)
        
        // 如果需要振动
        if (alarm.vibrate) {
            vibrate(context)
        }
        
        // 如果是非重复闹钟，关闭它
        if (!alarm.hasRepeatDays()) {
            alarm.isEnabled = false
            dbHelper.updateAlarm(alarm)
        }
        
        // 重新设置闹钟
        AlarmScheduler.scheduleAlarm(context, alarm)
    }
    
    /**
     * 创建通知
     */
    private fun createNotification(context: Context, title: String, alarmId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "闹钟通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "闹钟响铃通知"
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // 创建打开应用的Intent
        val intent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(ALARM_ID, alarmId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 创建通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText("点击停止闹钟")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()
        
        // 显示通知
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 振动
     */
    private fun vibrate(context: Context) {
        val pattern = longArrayOf(0, 500, 500, 500, 500, 500)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, 0)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        }
    }
} 