package com.example.clockapp.model

import java.io.Serializable
import java.util.Calendar
import java.util.UUID

/**
 * 闹钟数据模型
 */
data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    var hour: Int = 0,
    var minute: Int = 0,
    var isEnabled: Boolean = true,
    var label: String = "",
    var repeatDays: BooleanArray = BooleanArray(7) { false }, // 周日到周六
    var vibrate: Boolean = true,
    var soundUri: String = "",
    var snoozeMinutes: Int = 5
) : Serializable {

    /**
     * 获取下一次响铃的时间
     */
    fun getNextAlarmTime(): Calendar {
        val calendar = Calendar.getInstance()
        
        // 如果不重复且当天时间已过，设置为明天
        if (!hasRepeatDays() && (hour < calendar.get(Calendar.HOUR_OF_DAY) || 
            (hour == calendar.get(Calendar.HOUR_OF_DAY) && minute <= calendar.get(Calendar.MINUTE)))) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        } else if (hasRepeatDays()) {
            // 如果有重复日，找到下一个重复的日子
            val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 转换为0-6
            var daysToAdd = 0
            
            // 如果今天的时间已经过了，从明天开始查找
            if (hour < calendar.get(Calendar.HOUR_OF_DAY) || 
                (hour == calendar.get(Calendar.HOUR_OF_DAY) && minute <= calendar.get(Calendar.MINUTE))) {
                daysToAdd = 1
            }
            
            // 查找下一个启用的日子
            for (i in 0 until 7) {
                val checkDay = (currentDayOfWeek + daysToAdd) % 7
                if (repeatDays[checkDay]) {
                    break
                }
                daysToAdd++
            }
            
            calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        }
        
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        return calendar
    }
    
    /**
     * 检查是否有重复日
     */
    fun hasRepeatDays(): Boolean {
        return repeatDays.any { it }
    }
    
    /**
     * 获取重复日的描述
     */
    fun getRepeatDaysDescription(): String {
        if (!hasRepeatDays()) {
            return "仅一次"
        }
        
        val days = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val selectedDays = mutableListOf<String>()
        
        for (i in repeatDays.indices) {
            if (repeatDays[i]) {
                selectedDays.add(days[i])
            }
        }
        
        // 检查是否为每天重复
        if (selectedDays.size == 7) {
            return "每天"
        }
        
        // 检查是否为工作日重复
        if (selectedDays.size == 5 && repeatDays[1] && repeatDays[2] && 
            repeatDays[3] && repeatDays[4] && repeatDays[5]) {
            return "工作日"
        }
        
        // 检查是否为周末重复
        if (selectedDays.size == 2 && repeatDays[0] && repeatDays[6]) {
            return "周末"
        }
        
        return selectedDays.joinToString(", ")
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as Alarm
        
        if (id != other.id) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
} 