package com.example.clockapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.clockapp.R
import com.example.clockapp.model.Alarm
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 闹钟列表适配器
 */
class AlarmAdapter(
    private var alarms: MutableList<Alarm>,
    private val onAlarmClickListener: OnAlarmClickListener
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    interface OnAlarmClickListener {
        fun onAlarmClick(alarm: Alarm)
        fun onAlarmSwitchChanged(alarm: Alarm, isEnabled: Boolean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarms[position]
        holder.bind(alarm)
    }

    override fun getItemCount(): Int = alarms.size

    /**
     * 更新闹钟列表
     */
    fun updateAlarms(newAlarms: List<Alarm>) {
        alarms.clear()
        alarms.addAll(newAlarms)
        notifyDataSetChanged()
    }

    /**
     * 添加闹钟
     */
    fun addAlarm(alarm: Alarm) {
        alarms.add(alarm)
        notifyItemInserted(alarms.size - 1)
    }

    /**
     * 更新闹钟
     */
    fun updateAlarm(alarm: Alarm) {
        val index = alarms.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            alarms[index] = alarm
            notifyItemChanged(index)
        }
    }

    /**
     * 删除闹钟
     */
    fun removeAlarm(alarm: Alarm) {
        val index = alarms.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            alarms.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeText: TextView = itemView.findViewById(R.id.alarm_time)
        private val amPmText: TextView = itemView.findViewById(R.id.alarm_am_pm)
        private val labelText: TextView = itemView.findViewById(R.id.alarm_label)
        private val repeatText: TextView = itemView.findViewById(R.id.alarm_repeat)
        private val enableSwitch: Switch = itemView.findViewById(R.id.alarm_switch)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onAlarmClickListener.onAlarmClick(alarms[position])
                }
            }

            enableSwitch.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val alarm = alarms[position]
                    if (alarm.isEnabled != isChecked) {
                        alarm.isEnabled = isChecked
                        onAlarmClickListener.onAlarmSwitchChanged(alarm, isChecked)
                    }
                }
            }
        }

        fun bind(alarm: Alarm) {
            // 设置时间
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
            }
            timeText.text = timeFormat.format(calendar.time)

            // 设置上午/下午
            val isAm = alarm.hour < 12
            amPmText.text = if (isAm) "上午" else "下午"

            // 设置标签
            if (alarm.label.isNotEmpty()) {
                labelText.text = alarm.label
                labelText.visibility = View.VISIBLE
            } else {
                labelText.visibility = View.GONE
            }

            // 设置重复
            val repeatDesc = alarm.getRepeatDaysDescription()
            if (repeatDesc != "仅一次") {
                repeatText.text = repeatDesc
                repeatText.visibility = View.VISIBLE
            } else {
                repeatText.visibility = View.GONE
            }

            // 设置开关状态
            enableSwitch.isChecked = alarm.isEnabled
        }
    }
} 