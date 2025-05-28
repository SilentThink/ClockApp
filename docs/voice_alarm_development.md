# 语音设置闹钟功能开发文档

## 功能概述

语音设置闹钟功能允许用户通过语音命令快速创建和设置闹钟，无需手动输入时间、重复规则和标签等信息。该功能利用科大讯飞语音识别SDK实现语音转文字，然后通过自然语言处理解析用户意图和闹钟参数。

## 技术实现

### 1. 依赖配置

项目使用科大讯飞语音识别SDK，相关依赖已在`app/build.gradle.kts`中配置：

```kotlin
dependencies {
    // 其他依赖...
    implementation(files("libs\\Msc.jar"))
}
```

同时在`android`块中配置了ABI过滤器：

```kotlin
android {
    // 其他配置...
    defaultConfig {
        // 其他配置...
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }
    }
    
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }
}
```

### 2. 权限配置

语音识别功能需要以下权限：

- `RECORD_AUDIO`: 录音权限
- `INTERNET`: 网络访问权限
- `ACCESS_NETWORK_STATE`: 网络状态访问权限
- `WRITE_EXTERNAL_STORAGE`: 存储权限（用于保存临时音频文件）

这些权限在`AndroidManifest.xml`中声明，并在运行时动态请求。

### 3. 核心类

#### 3.1 JsonParser

`JsonParser`类负责解析科大讯飞返回的JSON结果，并提供语音命令解析功能：

```kotlin
package com.example.clockapp.util

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

object JsonParser {
    // 解析讯飞语音识别结果
    fun parseIatResult(json: String): String { ... }
    
    // 解析语音闹钟命令
    fun parseAlarmCommand(text: String): AlarmCommand? { ... }
    
    // 辅助方法
    private fun containsAlarmKeyword(text: String): Boolean { ... }
    private fun parseTime(text: String, command: AlarmCommand) { ... }
    private fun parseRepeatDays(text: String, command: AlarmCommand) { ... }
    private fun parseLabel(text: String, command: AlarmCommand) { ... }
    
    // 语音闹钟命令数据类
    data class AlarmCommand(
        var hour: Int = 8,
        var minute: Int = 0,
        var repeatDays: BooleanArray = BooleanArray(7) { false },
        var label: String = ""
    ) { ... }
}
```

#### 3.2 AlarmEditActivity

`AlarmEditActivity`类负责闹钟编辑界面，集成了语音识别功能：

```kotlin
class AlarmEditActivity : AppCompatActivity() {
    // 语音识别相关
    private var mIat: SpeechRecognizer? = null
    private var mIatDialog: RecognizerDialog? = null
    private val mIatResults: MutableMap<String, String> = LinkedHashMap()
    private lateinit var mSharedPreferences: SharedPreferences
    
    // 初始化语音识别
    private fun initVoiceRecognition() { ... }
    
    // 开始语音识别
    private fun startVoiceRecognition() { ... }
    
    // 处理语音识别结果
    private fun processVoiceResult(results: RecognizerResult) { ... }
    
    // 参数设置
    private fun setParam() { ... }
    
    // 动态申请权限
    private fun initPermission() { ... }
}
```

### 4. 用户界面

在闹钟编辑界面添加了语音按钮，点击后弹出语音识别对话框：

```xml
<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:id="@+id/voice_button"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_alignParentEnd="true"
    android:layout_centerVertical="true"
    android:layout_marginEnd="16dp"
    android:contentDescription="语音设置"
    app:srcCompat="@drawable/ic_mic"
    app:fabSize="mini" />
```

## 语音命令解析

### 支持的语音命令格式

系统支持以下几种语音命令格式：

1. **简单设置时间**：
   - "设置闹钟早上7点"
   - "设置闹钟下午3点半"
   - "设置闹钟晚上10点"

2. **带重复规则的设置**：
   - "每天早上7点闹钟"
   - "工作日早上7点闹钟"
   - "周末早上9点闹钟"
   - "周一周三周五下午6点提醒"

3. **带标签的设置**：
   - "明天早上7点提醒我开会"
   - "下午3点半提醒我去健身"
   - "晚上10点闹钟提醒我睡觉"

### 解析逻辑

1. **时间解析**：
   - 识别"早上"、"下午"、"晚上"等时间前缀
   - 识别"X点"、"X点X分"、"X点半"等时间格式
   - 转换为24小时制

2. **重复规则解析**：
   - 识别"每天"、"工作日"、"周末"等常用重复模式
   - 识别"周一"、"周二"等具体星期几

3. **标签解析**：
   - 提取"提醒我XXX"、"闹钟XXX"等模式中的标签内容
   - 过滤掉包含时间关键词的标签内容

## 使用方法

1. 在闹钟编辑界面，点击右侧的麦克风按钮
2. 在弹出的对话框中说出闹钟设置命令，如"设置早上7点的闹钟"
3. 系统会自动识别并设置相应的闹钟参数
4. 用户可以进一步手动调整参数，然后点击保存按钮

## 注意事项

1. 语音识别需要网络连接
2. 首次使用时需要授予录音和存储权限
3. 在嘈杂环境中识别效果可能不佳
4. 部分复杂的语音命令可能无法正确解析，此时会提示用户重试

## 未来优化方向

1. 支持更多语音命令格式
2. 改进自然语言处理算法，提高识别准确率
3. 添加离线语音识别功能
4. 支持多语言语音命令
5. 增加语音反馈，形成对话式交互 