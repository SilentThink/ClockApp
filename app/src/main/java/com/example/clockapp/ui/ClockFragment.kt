package com.example.clockapp.ui

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.clockapp.R
import com.example.clockapp.view.ClockView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 时钟片段
 */
class ClockFragment : Fragment() {

    private lateinit var clockView: ClockView
    private lateinit var digitalTimeView: TextView
    private lateinit var dateView: TextView
    private lateinit var formatSwitch: Switch
    private val handler = Handler(Looper.getMainLooper())
    
    // 24小时制格式
    private val timeFormat24 = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    // 12小时制格式
    private val timeFormat12 = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    // 当前使用的时间格式
    private var currentTimeFormat = timeFormat24
    
    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE)
    
    // 是否使用12小时制
    private var use12HourFormat = false
    
    // 偏好设置键
    private val PREFS_NAME = "clock_preferences"
    private val KEY_USE_12_HOUR = "use_12_hour_format"

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_clock, container, false)

        clockView = view.findViewById(R.id.clock_view)
        digitalTimeView = view.findViewById(R.id.digitalTime)
        dateView = view.findViewById(R.id.dateText)
        formatSwitch = view.findViewById(R.id.format_switch)
        
        // 从偏好设置中读取时间格式
        val prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        use12HourFormat = prefs.getBoolean(KEY_USE_12_HOUR, false)
        formatSwitch.isChecked = use12HourFormat
        currentTimeFormat = if (use12HourFormat) timeFormat12 else timeFormat24
        
        // 设置开关监听器
        formatSwitch.setOnCheckedChangeListener { _, isChecked ->
            use12HourFormat = isChecked
            currentTimeFormat = if (use12HourFormat) timeFormat12 else timeFormat24
            updateTime()
            
            // 保存设置到偏好设置
            val editor = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            editor.putBoolean(KEY_USE_12_HOUR, use12HourFormat)
            editor.apply()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateTimeRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateTimeRunnable)
    }

    private fun updateTime() {
        val currentTime = Date()
        digitalTimeView.text = currentTimeFormat.format(currentTime)
        dateView.text = dateFormat.format(currentTime)
        clockView.invalidate() // 强制重绘时钟视图
    }
} 