package com.example.clockapp.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.clockapp.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 时钟小部件的实现类
 */
class ClockWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 为每个小部件实例执行更新
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // 当小部件的第一个实例被创建时调用
    }

    override fun onDisabled(context: Context) {
        // 当小部件的最后一个实例被删除时调用
    }

    companion object {
        /**
         * 更新小部件的方法
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // 创建一个新的RemoteViews对象
            val views = RemoteViews(context.packageName, R.layout.widget_clock)
            
            // 小部件使用TextClock，它会自动更新，无需手动设置时间

            // 通知AppWidgetManager更新小部件
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
} 