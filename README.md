# ClockApp - 现代化Android时钟应用

## 项目介绍

ClockApp是一款现代化的Android时钟应用，提供精确的时间显示和丰富的时钟功能。项目采用了最新的Android开发技术，实现了美观的UI设计和流畅的用户体验。

### 项目需求

基本要求:
1. 代码：使用Git管理工程代码，包含commit记录;最终将源代码压缩成zip包（或者提交到Github）发送给导师；
2. 功能：至少包含"时分秒"信息，正常更新时间，时间跟手机系统相同;
3. 界面：清晰可看，跳秒动效自然，UI样式可参考上图，可自由发挥;

自由发挥：
4. 更优雅的交互与设计：UI元素更丰富、动画效果更自然
5. 更完善的功能：支持桌面Widget、支持闹钟、倒计时功能
6. 更牛逼的技术应用：AI智能时钟、3D效果、语音设置闹钟

## 功能特点

### 核心功能

- **模拟时钟**：精美的模拟时钟界面，带有平滑的指针动画
- **数字时钟**：清晰的数字时间显示，支持12小时和24小时格式
- **日期显示**：当前日期和星期信息
- **闹钟功能**：完整的闹钟设置、编辑和管理功能
- **桌面小部件**：支持添加到Android桌面的时钟小部件

### 设计亮点

- **现代化UI**：深色主题设计，具有科技感
- **平滑动画**：60fps的指针移动动画，确保视觉流畅
- **响应式布局**：适配不同尺寸的设备屏幕
- **精美细节**：阴影效果、圆角设计、精致刻度
- **分离式布局**：使用Fragment和ViewPager2实现时钟和闹钟功能的分页展示

## 技术实现

### 主要技术

- **自定义View**：使用Android自定义View实现模拟时钟
- **Canvas绘图**：使用Canvas API绘制时钟元素
- **SQLite数据库**：存储和管理闹钟数据
- **AlarmManager**：实现系统闹钟功能
- **AppWidget**：实现桌面小部件功能
- **动画效果**：实现平滑的指针动画和过渡效果
- **Fragment**：使用Fragment实现界面模块化
- **ViewPager2**：实现页面滑动切换
- **BottomNavigationView**：实现底部导航栏

### 项目结构

```
app/
├── src/main/
│   ├── java/com/example/clockapp/
│   │   ├── MainActivity.kt          # 主活动
│   │   ├── view/
│   │   │   └── ClockView.kt         # 自定义时钟视图
│   │   ├── model/
│   │   │   └── Alarm.kt             # 闹钟数据模型
│   │   ├── db/
│   │   │   └── AlarmDatabaseHelper.kt # 闹钟数据库帮助类
│   │   ├── service/
│   │   │   ├── AlarmReceiver.kt     # 闹钟广播接收器
│   │   │   ├── AlarmScheduler.kt    # 闹钟调度器
│   │   │   └── BootReceiver.kt      # 开机启动接收器
│   │   ├── ui/
│   │   │   ├── ClockFragment.kt     # 时钟片段
│   │   │   ├── AlarmFragment.kt     # 闹钟片段
│   │   │   ├── AlarmEditActivity.kt # 闹钟编辑界面
│   │   │   └── AlarmRingActivity.kt # 闹钟响铃界面
│   │   ├── adapter/
│   │   │   └── AlarmAdapter.kt      # 闹钟列表适配器
│   │   └── widget/
│   │       └── ClockWidget.kt       # 桌面小部件实现
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml    # 主界面布局
│   │   │   ├── fragment_clock.xml   # 时钟片段布局
│   │   │   ├── fragment_alarm.xml   # 闹钟片段布局
│   │   │   ├── item_alarm.xml       # 闹钟列表项布局
│   │   │   ├── activity_alarm_edit.xml # 闹钟编辑界面布局
│   │   │   ├── activity_alarm_ring.xml # 闹钟响铃界面布局
│   │   │   └── widget_clock.xml     # 小部件布局
│   │   ├── menu/
│   │   │   ├── bottom_nav_menu.xml  # 底部导航菜单
│   │   │   └── menu_alarm_edit.xml  # 闹钟编辑菜单
│   │   ├── drawable/
│   │   │   ├── ic_clock.xml         # 时钟图标
│   │   │   ├── ic_alarm.xml         # 闹钟图标
│   │   │   ├── ic_add.xml           # 添加图标
│   │   │   ├── ic_save.xml          # 保存图标
│   │   │   ├── ic_delete.xml        # 删除图标
│   │   │   └── ic_chevron_right.xml # 向右箭头图标
│   │   ├── values/
│   │   │   ├── colors.xml           # 颜色资源
│   │   │   ├── strings.xml          # 字符串资源
│   │   │   └── themes.xml           # 主题资源
│   │   └── xml/
│   │       └── clock_widget_info.xml # 小部件配置
│   └── AndroidManifest.xml          # 应用清单
└── build.gradle.kts                 # 构建配置
```

## 使用说明

### 安装要求

- Android 5.0 (API 21) 或更高版本
- 支持所有Android设备和平板电脑

### 主要功能使用

1. **查看时间**：打开应用即可查看当前时间，包括模拟时钟和数字时钟显示
2. **设置闹钟**：
   - 点击底部导航栏的"闹钟"选项
   - 点击右下角的"+"按钮添加新闹钟
   - 设置闹钟时间、重复规则、标签等信息
   - 点击顶部操作栏的保存按钮完成设置
3. **管理闹钟**：
   - 在闹钟列表中查看所有闹钟
   - 点击闹钟项可编辑闹钟
   - 使用开关按钮可快速启用/禁用闹钟
   - 在编辑界面可删除闹钟
4. **添加桌面小部件**：
   - 长按主屏幕空白处
   - 选择"小部件"选项
   - 找到"时钟"小部件并添加到主屏幕

## 开发文档

详细的开发文档可在以下位置找到：

- [小部件开发文档](docs/widget_development.md)
- [闹钟功能开发文档](docs/alarm_development.md)

## 已修复问题

1. **时钟重叠问题**：修复了时钟界面出现两个表盘重叠的问题，通过移除 `activity_main.xml` 中重复的时钟视图元素解决。
2. **闹钟保存按钮问题**：修复了闹钟编辑界面没有保存按钮的问题，通过为 `AlarmEditActivity` 设置带有 ActionBar 的主题解决。
3. **导入缺失问题**：修复了 `AlarmScheduler` 和 `AlarmRingActivity` 中缺少必要导入的问题。

## 未来计划

- 支持多时区显示
- 添加倒计时和秒表功能
- 实现更多样式的时钟主题
- 支持自定义小部件外观
- 添加智能闹钟功能（逐渐增大音量、智能唤醒等）

## 开发者信息

- 开发语言：Kotlin
- 开发工具：Android Studio
- 最低支持API：21 (Android 5.0)
- 目标API：34 (Android 14)

## 许可证

Copyright © 2025 ClockApp

本项目仅用于教育目的，未经允许不得用于商业用途。