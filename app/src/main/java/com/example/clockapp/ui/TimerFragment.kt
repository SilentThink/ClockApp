package com.example.clockapp.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.clockapp.R
import java.util.Locale

/**
 * 倒计时片段
 */
class TimerFragment : Fragment() {

    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var secondPicker: NumberPicker
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var resetButton: Button
    private lateinit var stopAlarmButton: Button
    private lateinit var timerTextView: TextView
    private lateinit var progressBar: ProgressBar

    private var countDownTimer: CountDownTimer? = null
    private var timerRunning = false
    private var timeLeftInMillis: Long = 0
    private var startTimeInMillis: Long = 0
    
    // 媒体播放器
    private var mediaPlayer: MediaPlayer? = null
    // 振动器
    private var vibrator: Vibrator? = null
    // 电源锁
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_timer, container, false)

        hourPicker = view.findViewById(R.id.hourPicker)
        minutePicker = view.findViewById(R.id.minutePicker)
        secondPicker = view.findViewById(R.id.secondPicker)
        startButton = view.findViewById(R.id.startButton)
        pauseButton = view.findViewById(R.id.pauseButton)
        resetButton = view.findViewById(R.id.resetButton)
        stopAlarmButton = view.findViewById(R.id.stopAlarmButton)
        timerTextView = view.findViewById(R.id.timerTextView)
        progressBar = view.findViewById(R.id.progressBar)

        // 初始化振动器
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // 设置时间选择器
        setupTimePickers()

        // 设置按钮点击事件
        startButton.setOnClickListener { startTimer() }
        pauseButton.setOnClickListener { pauseTimer() }
        resetButton.setOnClickListener { resetTimer() }
        stopAlarmButton.setOnClickListener { stopAlarm() }

        updateUI()

        return view
    }

    private fun setupTimePickers() {
        hourPicker.apply {
            minValue = 0
            maxValue = 23
            wrapSelectorWheel = true
        }

        minutePicker.apply {
            minValue = 0
            maxValue = 59
            wrapSelectorWheel = true
        }

        secondPicker.apply {
            minValue = 0
            maxValue = 59
            wrapSelectorWheel = true
        }
    }

    private fun startTimer() {
        if (timerRunning) {
            return
        }

        if (timeLeftInMillis == 0L) {
            // 如果是新的计时器，从选择器获取时间
            val hours = hourPicker.value
            val minutes = minutePicker.value
            val seconds = secondPicker.value

            if (hours == 0 && minutes == 0 && seconds == 0) {
                return // 不允许设置为0时间
            }

            startTimeInMillis = (hours * 3600 + minutes * 60 + seconds) * 1000L
            timeLeftInMillis = startTimeInMillis
        }

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateUI()
            }

            override fun onFinish() {
                timerRunning = false
                timeLeftInMillis = 0
                updateUI()
                timerFinished()
            }
        }.start()

        timerRunning = true
        updateUI()
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        timerRunning = false
        updateUI()
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        timeLeftInMillis = 0
        timerRunning = false
        updateUI()
        
        // 停止响铃和振动
        stopAlarm()
    }

    private fun updateUI() {
        val hours = (timeLeftInMillis / 1000) / 3600
        val minutes = ((timeLeftInMillis / 1000) % 3600) / 60
        val seconds = (timeLeftInMillis / 1000) % 60

        val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        timerTextView.text = timeFormatted

        // 更新进度条
        if (startTimeInMillis > 0) {
            // 对于圆形进度条，进度从0到100
            val progress = ((timeLeftInMillis * 100) / startTimeInMillis).toInt()
            // 使用动画平滑更新进度
            animateProgressBar(progress)
        } else {
            progressBar.progress = 100
        }

        // 检查是否正在播放闹铃
        val isAlarmPlaying = mediaPlayer?.isPlaying == true
        
        // 首先隐藏所有按钮
        startButton.visibility = View.INVISIBLE
        pauseButton.visibility = View.INVISIBLE
        resetButton.visibility = View.INVISIBLE
        stopAlarmButton.visibility = View.INVISIBLE
        
        // 根据状态显示相应按钮
        if (isAlarmPlaying) {
            // 闹铃正在响，只显示停止闹铃按钮
            stopAlarmButton.visibility = View.VISIBLE
            hourPicker.visibility = View.INVISIBLE
            minutePicker.visibility = View.INVISIBLE
            secondPicker.visibility = View.INVISIBLE
            timerTextView.visibility = View.VISIBLE
        } else if (timerRunning) {
            // 倒计时运行中，显示暂停和重置按钮
            pauseButton.visibility = View.VISIBLE
            resetButton.visibility = View.VISIBLE
            hourPicker.visibility = View.INVISIBLE
            minutePicker.visibility = View.INVISIBLE
            secondPicker.visibility = View.INVISIBLE
            timerTextView.visibility = View.VISIBLE
        } else if (timeLeftInMillis > 0) {
            // 倒计时暂停，显示开始和重置按钮
            startButton.visibility = View.VISIBLE
            resetButton.visibility = View.VISIBLE
            hourPicker.visibility = View.INVISIBLE
            minutePicker.visibility = View.INVISIBLE
            secondPicker.visibility = View.INVISIBLE
            timerTextView.visibility = View.VISIBLE
        } else {
            // 初始状态或重置后，只显示开始按钮和时间选择器
            startButton.visibility = View.VISIBLE
            hourPicker.visibility = View.VISIBLE
            minutePicker.visibility = View.VISIBLE
            secondPicker.visibility = View.VISIBLE
            timerTextView.visibility = View.INVISIBLE
        }
    }

    private fun animateProgressBar(targetProgress: Int) {
        val animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, targetProgress)
        animation.duration = 300
        animation.interpolator = DecelerateInterpolator()
        animation.start()
    }

    private fun timerFinished() {
        // 获取电源锁，保持屏幕亮起
        acquireWakeLock()
        
        // 播放提示音
        playAlarmSound()
        
        // 振动提醒
        startVibration()
        
        // 显示提示
        Toast.makeText(requireContext(), R.string.timer_finished, Toast.LENGTH_SHORT).show()
        
        // 更新UI，显示停止按钮
        updateUI()
    }
    
    private fun playAlarmSound() {
        try {
            // 释放之前的MediaPlayer
            mediaPlayer?.release()
            mediaPlayer = null
            
            // 获取默认闹钟铃声
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            
            // 创建新的MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setDataSource(requireContext(), alarmUri)
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
            
            // 打印日志，确认闹铃正在播放
            println("闹铃开始播放")
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果出错，确保MediaPlayer为null
            mediaPlayer = null
        }
    }
    
    private fun startVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 500, 500, 500, 500), // 振动模式：0毫秒延迟，500毫秒振动，500毫秒休息，循环
                    0 // 重复从索引0开始
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 500, 500, 500, 500), 0)
        }
    }
    
    private fun acquireWakeLock() {
        try {
            // 释放之前的WakeLock
            releaseWakeLock()
            
            // 获取新的WakeLock
            val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "ClockApp:TimerWakeLock"
            )
            wakeLock?.acquire(10*60*1000L) // 10分钟超时
            
            // 设置窗口保持屏幕亮起
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
            
            // 移除窗口保持屏幕亮起标志
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopAlarm() {
        // 停止音乐
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            println("闹铃已停止")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 停止振动
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 释放电源锁
        releaseWakeLock()
        
        // 更新UI
        updateUI()
    }

    override fun onPause() {
        super.onPause()
        if (timerRunning) {
            pauseTimer()
        }
        // 停止响铃和振动
        stopAlarm()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        stopAlarm()
    }
} 