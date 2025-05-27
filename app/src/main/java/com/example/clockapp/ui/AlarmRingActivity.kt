package com.example.clockapp.ui

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.clockapp.R
import com.example.clockapp.db.AlarmDatabaseHelper
import com.example.clockapp.service.AlarmReceiver
import com.example.clockapp.service.AlarmScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 闹钟响铃界面
 */
class AlarmRingActivity : AppCompatActivity() {

    private lateinit var alarmTimeText: TextView
    private lateinit var alarmLabelText: TextView
    private lateinit var dismissButton: Button
    private lateinit var snoozeButton: Button
    
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var alarmId: String? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable {
        stopAlarm()
        finish()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置全屏和屏幕常亮
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        
        setContentView(R.layout.activity_alarm_ring)
        
        // 获取闹钟ID
        alarmId = intent.getStringExtra(AlarmReceiver.ALARM_ID)
        if (alarmId == null) {
            finish()
            return
        }
        
        // 初始化视图
        alarmTimeText = findViewById(R.id.alarm_time_text)
        alarmLabelText = findViewById(R.id.alarm_label_text)
        dismissButton = findViewById(R.id.dismiss_button)
        snoozeButton = findViewById(R.id.snooze_button)
        
        // 设置当前时间
        updateCurrentTime()
        
        // 获取闹钟信息
        val dbHelper = AlarmDatabaseHelper(this)
        val alarm = dbHelper.getAlarm(alarmId!!)
        
        if (alarm != null) {
            // 显示闹钟标签
            if (alarm.label.isNotEmpty()) {
                alarmLabelText.text = alarm.label
                alarmLabelText.visibility = View.VISIBLE
            } else {
                alarmLabelText.visibility = View.GONE
            }
            
            // 设置按钮点击事件
            dismissButton.setOnClickListener {
                stopAlarm()
                finish()
            }
            
            snoozeButton.setOnClickListener {
                // 贪睡功能
                val snoozeAlarm = alarm.copy()
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MINUTE, alarm.snoozeMinutes)
                snoozeAlarm.hour = calendar.get(Calendar.HOUR_OF_DAY)
                snoozeAlarm.minute = calendar.get(Calendar.MINUTE)
                
                // 更新闹钟
                dbHelper.updateAlarm(snoozeAlarm)
                
                // 重新设置闹钟
                AlarmScheduler.scheduleAlarm(this, snoozeAlarm)
                
                stopAlarm()
                finish()
            }
        } else {
            finish()
            return
        }
        
        // 获取电源锁
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "ClockApp:AlarmWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 最多持续10分钟
        
        // 播放铃声
        playRingtone()
        
        // 振动
        startVibration()
        
        // 设置自动停止（3分钟后）
        handler.postDelayed(autoStopRunnable, 3 * 60 * 1000L)
    }
    
    private fun updateCurrentTime() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        alarmTimeText.text = timeFormat.format(Date())
    }
    
    private fun playRingtone() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmRingActivity, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun startVibration() {
        val pattern = longArrayOf(0, 500, 500, 500, 500, 500)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorManager.defaultVibrator
            vibrator?.vibrate(
                VibrationEffect.createWaveform(pattern, 0)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
    }
    
    private fun stopAlarm() {
        // 停止铃声
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        
        // 停止振动
        vibrator?.cancel()
        
        // 取消自动停止
        handler.removeCallbacks(autoStopRunnable)
        
        // 取消通知
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(AlarmReceiver.NOTIFICATION_ID)
        
        // 释放电源锁
        wakeLock?.release()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
} 