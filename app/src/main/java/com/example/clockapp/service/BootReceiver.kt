package com.example.clockapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 开机启动接收器，用于在设备重启后重新设置所有闹钟
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "设备启动完成，重新设置闹钟")
            
            // 重新设置所有闹钟
            AlarmScheduler.rescheduleAllAlarms(context)
        }
    }
} 