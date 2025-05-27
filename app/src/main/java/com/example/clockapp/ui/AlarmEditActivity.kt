package com.example.clockapp.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.clockapp.R
import com.example.clockapp.db.AlarmDatabaseHelper
import com.example.clockapp.model.Alarm
import com.example.clockapp.service.AlarmScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 闹钟编辑界面
 */
class AlarmEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val RESULT_ALARM_DELETED = 2
    }

    private lateinit var timeText: TextView
    private lateinit var labelEdit: EditText
    private lateinit var repeatLayout: LinearLayout
    private lateinit var repeatText: TextView
    private lateinit var snoozeLayout: LinearLayout
    private lateinit var snoozeText: TextView
    private lateinit var vibrateCheckBox: CheckBox

    private var alarm: Alarm = Alarm()
    private var isNewAlarm = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_edit)

        // 设置标题
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 初始化视图
        timeText = findViewById(R.id.edit_alarm_time)
        labelEdit = findViewById(R.id.edit_alarm_label)
        repeatLayout = findViewById(R.id.repeat_layout)
        repeatText = findViewById(R.id.repeat_text)
        snoozeLayout = findViewById(R.id.snooze_layout)
        snoozeText = findViewById(R.id.snooze_text)
        vibrateCheckBox = findViewById(R.id.vibrate_checkbox)

        // 获取闹钟ID
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        if (alarmId != null) {
            // 编辑现有闹钟
            isNewAlarm = false
            supportActionBar?.title = "编辑闹钟"

            val dbHelper = AlarmDatabaseHelper(this)
            val existingAlarm = dbHelper.getAlarm(alarmId)
            if (existingAlarm != null) {
                alarm = existingAlarm
            }
        } else {
            // 创建新闹钟
            supportActionBar?.title = "添加闹钟"
            
            // 默认设置为当前时间
            val calendar = Calendar.getInstance()
            alarm.hour = calendar.get(Calendar.HOUR_OF_DAY)
            alarm.minute = calendar.get(Calendar.MINUTE)
        }

        // 设置时间点击事件
        timeText.setOnClickListener {
            showTimePickerDialog()
        }

        // 设置重复点击事件
        repeatLayout.setOnClickListener {
            showRepeatDialog()
        }

        // 设置贪睡点击事件
        snoozeLayout.setOnClickListener {
            showSnoozeDialog()
        }

        // 设置振动点击事件
        vibrateCheckBox.setOnCheckedChangeListener { _, isChecked ->
            alarm.vibrate = isChecked
        }

        // 更新UI
        updateUI()
    }

    private fun updateUI() {
        // 更新时间
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
        }
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeText.text = timeFormat.format(calendar.time)

        // 更新标签
        labelEdit.setText(alarm.label)

        // 更新重复
        repeatText.text = alarm.getRepeatDaysDescription()

        // 更新贪睡
        snoozeText.text = "${alarm.snoozeMinutes} 分钟"

        // 更新振动
        vibrateCheckBox.isChecked = alarm.vibrate
    }

    private fun showTimePickerDialog() {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                alarm.hour = hourOfDay
                alarm.minute = minute
                updateUI()
            },
            alarm.hour,
            alarm.minute,
            true
        )
        timePickerDialog.show()
    }

    private fun showRepeatDialog() {
        val days = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val checkedItems = BooleanArray(7) { i -> alarm.repeatDays[i] }

        AlertDialog.Builder(this)
            .setTitle("重复")
            .setMultiChoiceItems(days, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("确定") { _, _ ->
                alarm.repeatDays = checkedItems
                updateUI()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSnoozeDialog() {
        val options = arrayOf("5 分钟", "10 分钟", "15 分钟", "20 分钟", "30 分钟")
        val values = intArrayOf(5, 10, 15, 20, 30)
        val selectedIndex = values.indexOf(alarm.snoozeMinutes).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("贪睡时长")
            .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                alarm.snoozeMinutes = values[which]
                updateUI()
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_alarm_edit, menu)
        // 如果是新闹钟，隐藏删除按钮
        menu.findItem(R.id.action_delete).isVisible = !isNewAlarm
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_save -> {
                saveAlarm()
                true
            }
            R.id.action_delete -> {
                deleteAlarm()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveAlarm() {
        // 更新标签
        alarm.label = labelEdit.text.toString().trim()

        val dbHelper = AlarmDatabaseHelper(this)
        if (isNewAlarm) {
            dbHelper.addAlarm(alarm)
        } else {
            dbHelper.updateAlarm(alarm)
        }

        // 设置闹钟
        alarm.isEnabled = true
        AlarmScheduler.scheduleAlarm(this, alarm)

        // 返回结果
        val resultIntent = Intent().apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun deleteAlarm() {
        AlertDialog.Builder(this)
            .setTitle("删除闹钟")
            .setMessage("确定要删除这个闹钟吗？")
            .setPositiveButton("删除") { _, _ ->
                val dbHelper = AlarmDatabaseHelper(this)
                dbHelper.deleteAlarm(alarm.id)

                // 取消闹钟
                AlarmScheduler.cancelAlarm(this, alarm)

                // 返回结果
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_ALARM_ID, alarm.id)
                }
                setResult(RESULT_ALARM_DELETED, resultIntent)
                finish()
            }
            .setNegativeButton("取消", null)
            .show()
    }
} 