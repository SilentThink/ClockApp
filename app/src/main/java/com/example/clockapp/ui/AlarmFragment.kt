package com.example.clockapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clockapp.R
import com.example.clockapp.adapter.AlarmAdapter
import com.example.clockapp.db.AlarmDatabaseHelper
import com.example.clockapp.model.Alarm
import com.example.clockapp.service.AlarmScheduler
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * 闹钟片段
 */
class AlarmFragment : Fragment(), AlarmAdapter.OnAlarmClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var addButton: FloatingActionButton
    private lateinit var adapter: AlarmAdapter
    private lateinit var dbHelper: AlarmDatabaseHelper

    private val editAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AlarmEditActivity.RESULT_ALARM_DELETED) {
            // 闹钟被删除，刷新列表
            loadAlarms()
        } else if (result.resultCode == android.app.Activity.RESULT_OK) {
            // 闹钟被添加或编辑，刷新列表
            loadAlarms()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alarm, container, false)

        dbHelper = AlarmDatabaseHelper(requireContext())

        // 初始化视图
        recyclerView = view.findViewById(R.id.alarm_recycler_view)
        addButton = view.findViewById(R.id.add_alarm_button)

        // 设置RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = AlarmAdapter(mutableListOf(), this)
        recyclerView.adapter = adapter

        // 设置添加按钮点击事件
        addButton.setOnClickListener {
            val intent = Intent(requireContext(), AlarmEditActivity::class.java)
            editAlarmLauncher.launch(intent)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadAlarms()
    }

    /**
     * 加载闹钟列表
     */
    private fun loadAlarms() {
        val alarms = dbHelper.getAllAlarms()
        adapter.updateAlarms(alarms)
    }

    /**
     * 闹钟点击事件
     */
    override fun onAlarmClick(alarm: Alarm) {
        val intent = Intent(requireContext(), AlarmEditActivity::class.java).apply {
            putExtra(AlarmEditActivity.EXTRA_ALARM_ID, alarm.id)
        }
        editAlarmLauncher.launch(intent)
    }

    /**
     * 闹钟开关状态改变事件
     */
    override fun onAlarmSwitchChanged(alarm: Alarm, isEnabled: Boolean) {
        alarm.isEnabled = isEnabled
        dbHelper.updateAlarm(alarm)

        if (isEnabled) {
            AlarmScheduler.scheduleAlarm(requireContext(), alarm)
        } else {
            AlarmScheduler.cancelAlarm(requireContext(), alarm)
        }
    }
} 