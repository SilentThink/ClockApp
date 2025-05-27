# 倒计时功能开发文档

本文档详细介绍了ClockApp中倒计时功能的实现过程、技术要点和关键代码。

## 功能概述

倒计时功能是ClockApp的核心功能之一，提供了以下特性：

1. 设置小时、分钟和秒的倒计时时间
2. 开始、暂停和重置倒计时
3. 圆形进度条直观显示倒计时进度
4. 倒计时结束时播放提示音和振动
5. 美观的UI设计，包括圆角按钮和动画效果

## 技术架构

倒计时功能主要由以下组件构成：

### 视图层

- **TimerFragment**: 倒计时界面的Fragment，负责UI展示和用户交互
- **fragment_timer.xml**: 倒计时界面的布局文件，定义了界面元素的排列和样式

### 控制层

- **CountDownTimer**: Android系统提供的倒计时计时器，负责倒计时的核心逻辑
- **MediaPlayer**: 用于播放倒计时结束时的提示音
- **Vibrator**: 用于在倒计时结束时提供振动反馈
- **PowerManager.WakeLock**: 用于在倒计时结束时保持屏幕亮起

### 资源层

- **drawable/circular_progress_bar.xml**: 自定义圆形进度条样式
- **drawable/rounded_button*.xml**: 各种按钮的圆角背景样式
- **values/colors.xml**: 颜色资源定义
- **values/strings.xml**: 字符串资源定义

## 关键实现

### 1. 倒计时核心逻辑

```kotlin
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
```

### 2. 圆形进度条实现

圆形进度条通过自定义Drawable实现：

```xml
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 背景圆环 -->
    <item>
        <shape android:shape="ring"
            android:thicknessRatio="16"
            android:useLevel="false">
            <solid android:color="@color/progress_background" />
        </shape>
    </item>
    <!-- 进度圆环 -->
    <item>
        <shape android:shape="ring"
            android:thicknessRatio="16"
            android:useLevel="true">
            <gradient
                android:type="sweep"
                android:startColor="@color/teal_200"
                android:endColor="@color/teal_200" />
        </shape>
    </item>
</layer-list>
```

在布局中使用：

```xml
<ProgressBar
    android:id="@+id/progressBar"
    style="?android:attr/progressBarStyleHorizontal"
    android:layout_width="200dp"
    android:layout_height="200dp"
    android:layout_gravity="center"
    android:progress="100"
    android:progressDrawable="@drawable/circular_progress_bar"
    android:rotation="-90" />
```

### 3. 进度条动画效果

为了使进度条变化更加平滑，添加了动画效果：

```kotlin
private fun animateProgressBar(targetProgress: Int) {
    val animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, targetProgress)
    animation.duration = 300
    animation.interpolator = DecelerateInterpolator()
    animation.start()
}
```

### 4. 倒计时结束提醒

当倒计时结束时，会播放提示音、振动并唤醒屏幕：

```kotlin
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
```

### 5. 播放提示音实现

```kotlin
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
    } catch (e: Exception) {
        e.printStackTrace()
        mediaPlayer = null
    }
}
```

### 6. 振动功能实现

```kotlin
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
```

### 7. 屏幕保持亮起

```kotlin
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
```

## UI设计

倒计时界面的UI设计注重美观和用户体验，主要包括以下元素：

### 时间选择区域

- 三个NumberPicker分别用于选择小时、分钟和秒
- 使用自定义的NumberPickerStyle样式，与应用整体风格保持一致
- 选择器之间使用文本分隔符（时、分）增强可读性

### 倒计时显示

- 大号数字显示剩余时间，格式为"00:00:00"
- 文本添加阴影效果，增强可读性
- 圆形进度条直观显示倒计时进度

### 控制按钮

- 采用圆角设计，增加现代感
- 不同功能的按钮使用不同颜色区分：
  - 开始按钮：青色（teal_200）
  - 暂停按钮：橙色（orange_500）
  - 重置按钮：红色（red_500）
  - 停止闹铃按钮：深红色（red_700）
- 按钮添加波纹效果，增强交互体验
- 根据不同状态动态显示/隐藏相应按钮

## 性能优化

1. **资源管理**：在Fragment生命周期结束时释放资源，避免内存泄漏
   ```kotlin
   override fun onDestroy() {
       super.onDestroy()
       countDownTimer?.cancel()
       stopAlarm()
   }
   ```

2. **动画优化**：使用ObjectAnimator进行进度条动画，提供流畅的视觉效果
   ```kotlin
   private fun animateProgressBar(targetProgress: Int) {
       val animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, targetProgress)
       animation.duration = 300
       animation.interpolator = DecelerateInterpolator()
       animation.start()
   }
   ```

3. **错误处理**：对可能出现异常的操作进行try-catch处理，提高应用稳定性
   ```kotlin
   try {
       // 操作代码
   } catch (e: Exception) {
       e.printStackTrace()
   }
   ```

## 用户体验优化

1. **视觉反馈**：
   - 使用圆形进度条直观显示倒计时进度
   - 按钮状态变化提供明确的视觉反馈
   - 倒计时结束时显示Toast提示

2. **听觉反馈**：
   - 倒计时结束时播放系统默认闹钟铃声
   - 铃声循环播放，确保用户能够注意到

3. **触觉反馈**：
   - 倒计时结束时提供振动提醒
   - 按钮点击时的波纹效果提供触觉反馈

4. **交互设计**：
   - 根据不同状态动态显示相应的控制按钮
   - 倒计时结束时自动亮起屏幕，确保用户能够及时看到

## 已知问题和解决方案

1. **问题**：在某些设备上，圆形进度条可能显示不正常  
   **解决方案**：使用ProgressBar的rotation属性调整进度条方向，确保在所有设备上正确显示

2. **问题**：在低电量模式下，振动和声音可能被系统限制  
   **解决方案**：使用USAGE_ALARM音频属性，提高提示音的优先级

3. **问题**：长时间倒计时可能因系统休眠而不准确  
   **解决方案**：使用CountDownTimer而非Handler实现倒计时，更加可靠

## 未来改进计划

1. 添加自定义铃声选择功能
2. 支持保存常用倒计时预设
3. 添加多个并行倒计时功能
4. 实现倒计时小部件，可以添加到桌面
5. 添加倒计时历史记录功能 