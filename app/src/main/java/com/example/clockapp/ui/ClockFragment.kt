package com.example.clockapp.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE)

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
        digitalTimeView.text = timeFormat.format(currentTime)
        dateView.text = dateFormat.format(currentTime)
        clockView.invalidate() // 强制重绘时钟视图
    }
} 