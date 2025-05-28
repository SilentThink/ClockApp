package com.example.clockapp.util

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Json结果解析类
 */
object JsonParser {

    /**
     * 解析讯飞语音识别结果
     */
    fun parseIatResult(json: String): String {
        val ret = StringBuilder()
        try {
            val tokener = JSONTokener(json)
            val joResult = JSONObject(tokener)

            val words = joResult.getJSONArray("ws")
            for (i in 0 until words.length()) {
                // 转写结果词，默认使用第一个结果
                val items = words.getJSONObject(i).getJSONArray("cw")
                val obj = items.getJSONObject(0)
                ret.append(obj.getString("w"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ret.toString()
    }
    
    /**
     * 解析语音闹钟命令
     * 支持格式：
     * 1. "设置闹钟 早上7点"
     * 2. "明天下午3点半提醒我开会"
     * 3. "每天晚上10点闹钟"
     * 返回解析后的闹钟设置
     */
    fun parseAlarmCommand(text: String): AlarmCommand? {
        val command = AlarmCommand()
        
        // 检查是否包含闹钟关键词
        if (!containsAlarmKeyword(text)) {
            return null
        }
        
        // 解析时间
        parseTime(text, command)
        
        // 解析重复
        parseRepeatDays(text, command)
        
        // 解析标签
        parseLabel(text, command)
        
        return command
    }
    
    private fun containsAlarmKeyword(text: String): Boolean {
        val keywords = arrayOf("闹钟", "提醒", "叫我", "叫醒", "起床")
        return keywords.any { text.contains(it) }
    }
    
    private fun parseTime(text: String, command: AlarmCommand) {
        // 解析小时
        val hourPatterns = arrayOf(
            "(上午|早上|早晨|凌晨|中午|下午|晚上|傍晚|晚间)?(\\d+)点",
            "(上午|早上|早晨|凌晨|中午|下午|晚上|傍晚|晚间)?(\\d+)时"
        )
        
        for (pattern in hourPatterns) {
            val regex = Regex(pattern)
            val matchResult = regex.find(text)
            if (matchResult != null) {
                val timePrefix = matchResult.groupValues[1]
                var hour = matchResult.groupValues[2].toInt()
                
                // 处理12小时制
                if (timePrefix == "下午" || timePrefix == "晚上" || timePrefix == "傍晚" || timePrefix == "晚间") {
                    if (hour < 12) hour += 12
                } else if ((timePrefix == "上午" || timePrefix == "早上" || timePrefix == "早晨" || timePrefix == "凌晨") && hour == 12) {
                    hour = 0
                }
                
                command.hour = hour
                break
            }
        }
        
        // 解析分钟
        val minutePatterns = arrayOf(
            "(\\d+)点(\\d+)分",
            "(\\d+)时(\\d+)分",
            "(\\d+)点半"
        )
        
        for (pattern in minutePatterns) {
            val regex = Regex(pattern)
            val matchResult = regex.find(text)
            if (matchResult != null) {
                if (matchResult.groupValues.size > 2 && pattern.contains("半")) {
                    command.minute = 30
                } else {
                    command.minute = matchResult.groupValues[2].toInt()
                }
                break
            }
        }
    }
    
    private fun parseRepeatDays(text: String, command: AlarmCommand) {
        // 检查是否包含重复关键词
        if (text.contains("每天")) {
            for (i in 0..6) {
                command.repeatDays[i] = true
            }
        } else if (text.contains("工作日") || text.contains("周一到周五")) {
            for (i in 1..5) {
                command.repeatDays[i] = true
            }
        } else if (text.contains("周末")) {
            command.repeatDays[0] = true
            command.repeatDays[6] = true
        } else {
            // 检查单独的星期几
            val dayPatterns = arrayOf(
                "周日|星期日|礼拜日|周天|星期天",
                "周一|星期一|礼拜一",
                "周二|星期二|礼拜二",
                "周三|星期三|礼拜三",
                "周四|星期四|礼拜四",
                "周五|星期五|礼拜五",
                "周六|星期六|礼拜六"
            )
            
            for (i in dayPatterns.indices) {
                if (Regex(dayPatterns[i]).find(text) != null) {
                    command.repeatDays[i] = true
                }
            }
        }
    }
    
    private fun parseLabel(text: String, command: AlarmCommand) {
        // 提取可能的标签
        val labelPatterns = arrayOf(
            "提醒我(.+)",
            "闹钟(.+)",
            "叫我(.+)"
        )
        
        for (pattern in labelPatterns) {
            val regex = Regex(pattern)
            val matchResult = regex.find(text)
            if (matchResult != null && matchResult.groupValues.size > 1) {
                val label = matchResult.groupValues[1].trim()
                if (label.isNotEmpty() && !containsTimeKeywords(label)) {
                    command.label = label
                    break
                }
            }
        }
    }
    
    private fun containsTimeKeywords(text: String): Boolean {
        val keywords = arrayOf("点", "分", "时", "早上", "晚上", "下午", "上午", "凌晨")
        return keywords.any { text.contains(it) }
    }
    
    /**
     * 语音闹钟命令数据类
     */
    data class AlarmCommand(
        var hour: Int = 8,
        var minute: Int = 0,
        var repeatDays: BooleanArray = BooleanArray(7) { false },
        var label: String = ""
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AlarmCommand

            if (hour != other.hour) return false
            if (minute != other.minute) return false
            if (!repeatDays.contentEquals(other.repeatDays)) return false
            if (label != other.label) return false

            return true
        }

        override fun hashCode(): Int {
            var result = hour
            result = 31 * result + minute
            result = 31 * result + repeatDays.contentHashCode()
            result = 31 * result + label.hashCode()
            return result
        }
    }
} 