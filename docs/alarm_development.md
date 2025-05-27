# 闹钟功能开发文档

本文档详细介绍了ClockApp中闹钟功能的实现过程、技术要点和关键代码。

## 功能概述

闹钟功能是ClockApp的核心功能之一，提供了以下特性：

1. 创建、编辑和删除闹钟
2. 设置闹钟重复规则（单次、每天、工作日、周末或自定义）
3. 添加闹钟标签
4. 设置振动提醒
5. 贪睡功能
6. 设备重启后自动恢复闹钟

## 技术架构

闹钟功能采用了MVC架构模式，主要包含以下组件：

### 数据层

- **Alarm**: 闹钟数据模型，定义了闹钟的属性和行为
- **AlarmDatabaseHelper**: SQLite数据库帮助类，负责闹钟数据的持久化存储

### 控制层

- **AlarmScheduler**: 闹钟调度器，负责设置和取消系统闹钟
- **AlarmReceiver**: 闹钟广播接收器，处理闹钟触发事件
- **BootReceiver**: 开机启动接收器，在设备重启后恢复闹钟设置

### 视图层

- **AlarmFragment**: 闹钟列表界面，显示所有闹钟
- **AlarmAdapter**: 闹钟列表适配器，负责闹钟列表项的渲染和交互
- **AlarmEditActivity**: 闹钟编辑界面，用于创建和编辑闹钟
- **AlarmRingActivity**: 闹钟响铃界面，当闹钟触发时显示

## 关键实现

### 1. 闹钟数据模型

```kotlin
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
    // 计算下一次响铃时间
    fun getNextAlarmTime(): Calendar {
        val calendar = Calendar.getInstance()
        
        // 如果不重复且当天时间已过，设置为明天
        if (!hasRepeatDays() && (hour < calendar.get(Calendar.HOUR_OF_DAY) || 
            (hour == calendar.get(Calendar.HOUR_OF_DAY) && minute <= calendar.get(Calendar.MINUTE)))) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        } else if (hasRepeatDays()) {
            // 如果有重复日，找到下一个重复的日子
            // 实现逻辑
        }
        
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        return calendar
    }
    
    // 检查是否有重复日
    fun hasRepeatDays(): Boolean {
        return repeatDays.any { it }
    }
    
    // 获取重复日描述
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
}
```

### 2. 闹钟数据库

使用SQLite数据库存储闹钟数据，主要表结构：

```sql
CREATE TABLE alarms (
    id TEXT PRIMARY KEY,
    hour INTEGER,
    minute INTEGER,
    enabled INTEGER,
    label TEXT,
    repeat_days TEXT,
    vibrate INTEGER,
    sound_uri TEXT,
    snooze_minutes INTEGER
)
```

### 3. 闹钟调度

使用Android的AlarmManager API设置系统闹钟：

```kotlin
fun scheduleAlarm(context: Context, alarm: Alarm) {
    if (!alarm.isEnabled) {
        cancelAlarm(context, alarm)
        return
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra(AlarmReceiver.ALARM_ID, alarm.id)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        alarm.id.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = alarm.getNextAlarmTime()
    val triggerTime = calendar.timeInMillis

    // 根据Android版本选择合适的设置方法
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        // 如果无法精确设置闹钟，使用非精确模式
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        // 使用精确闹钟，并允许在低电量模式下触发
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    } else {
        // 旧版本使用精确闹钟
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }
}
```

### 4. 闹钟触发

当闹钟触发时，AlarmReceiver会接收到广播并执行以下操作：

1. 启动AlarmRingActivity显示闹钟界面
2. 创建通知提醒用户
3. 如果设置了振动，则启动振动
4. 如果是非重复闹钟，则禁用它
5. 重新调度下一次闹钟

### 5. 闹钟响铃界面

闹钟响铃界面包含以下功能：

1. 显示当前时间和闹钟标签
2. 播放闹钟铃声
3. 振动提醒
4. 提供"关闭"和"贪睡"按钮
5. 自动获取电源锁，确保屏幕亮起
6. 设置自动停止计时器（3分钟后）

### 6. 设备重启适配

使用BootReceiver监听设备启动完成事件，在设备重启后恢复所有已启用的闹钟：

```kotlin
override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
        AlarmScheduler.rescheduleAllAlarms(context)
    }
}
```

## UI设计

### 闹钟列表

闹钟列表采用RecyclerView实现，每个列表项包含：

- 闹钟时间
- 上午/下午标识
- 闹钟标签
- 重复规则描述
- 启用/禁用开关

### 闹钟编辑界面

编辑界面包含以下元素：

- 时间选择器
- 标签输入框
- 重复规则选择
- 贪睡时长设置
- 振动开关
- 保存和删除按钮

### 闹钟响铃界面

响铃界面设计简洁明了，包含：

- 大号时间显示
- 闹钟标签
- "关闭"和"贪睡"按钮

## 性能优化

1. 使用单例模式实现AlarmScheduler，避免重复创建对象
2. 在数据库操作中使用事务处理批量操作
3. 在闹钟列表中使用ViewHolder模式提高列表性能
4. 使用Handler延迟处理定时任务，避免频繁操作

## 已知问题和解决方案

1. **问题**: Android 12及以上版本对精确闹钟权限有更严格的限制  
   **解决方案**: 添加SCHEDULE_EXACT_ALARM权限，并在运行时检查权限状态

2. **问题**: 部分设备在低电量模式下可能会延迟闹钟触发  
   **解决方案**: 使用setExactAndAllowWhileIdle方法设置闹钟，允许在低电量模式下触发

3. **问题**: 闹钟音量可能受到系统音量设置的影响  
   **解决方案**: 使用AudioAttributes设置闹钟铃声的音频类型为USAGE_ALARM

4. **问题**: 闹钟编辑界面没有保存按钮  
   **解决方案**: 为AlarmEditActivity设置带有ActionBar的主题，通过在themes.xml中创建Theme.ClockApp.WithActionBar主题并在AndroidManifest.xml中应用

5. **问题**: 导入缺失导致编译错误  
   **解决方案**: 添加必要的导入语句，包括Calendar和AlarmScheduler类

## 主题配置

为了确保闹钟编辑界面正常显示ActionBar和保存按钮，创建了专用主题：

```xml
<!-- 带有ActionBar的主题，用于闹钟编辑活动 -->
<style name="Theme.ClockApp.WithActionBar" parent="Theme.AppCompat.Light.DarkActionBar">
    <item name="colorPrimary">@color/purple_500</item>
    <item name="colorPrimaryDark">@color/purple_700</item>
    <item name="colorAccent">@color/teal_200</item>
    <item name="android:windowBackground">@android:color/white</item>
</style>
```

并在AndroidManifest.xml中应用：

```xml
<activity
    android:name=".ui.AlarmEditActivity"
    android:exported="false"
    android:theme="@style/Theme.ClockApp.WithActionBar"
    android:parentActivityName=".MainActivity" />
```

## 未来改进计划

1. 添加更多铃声选项和自定义铃声功能
2. 实现智能闹钟功能（逐渐增大音量、智能唤醒等）
3. 添加闹钟标签模板
4. 支持位置感知闹钟（基于地理位置触发）
5. 添加闹钟统计和分析功能 